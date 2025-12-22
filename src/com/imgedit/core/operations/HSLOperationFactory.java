package imgedit.core.operations;

import java.util.prefs.Preferences;
import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
/**
 * HSL操作工厂和配置管理
 */
public class HSLOperationFactory {

    public enum Algorithm {
        BASIC_HSL,         // 基础HSL实现
        OPTIMIZED_HSL_LUT, // 优化HSL（查找表）
        OPTIMIZED_HSL_GPU  // GPU加速（预留）
    }

    public enum PerformanceMode {
        QUALITY,    // 质量优先（使用高精度查找表）
        BALANCED,   // 平衡模式
        SPEED       // 速度优先（使用低精度查找表）
    }

    private static Algorithm defaultAlgorithm = Algorithm.OPTIMIZED_HSL_LUT;
    private static PerformanceMode defaultPerformanceMode = PerformanceMode.BALANCED;
    private static boolean useParallelProcessing = true;
    private static int threadThreshold = 10000;

    // 从配置文件加载设置
    static {
        try {
            Preferences prefs = Preferences.userNodeForPackage(HSLOperationFactory.class);

            String algorithmStr = prefs.get("hsl_algorithm", "OPTIMIZED_HSL_LUT");
            defaultAlgorithm = Algorithm.valueOf(algorithmStr);

            String modeStr = prefs.get("hsl_performance_mode", "BALANCED");
            defaultPerformanceMode = PerformanceMode.valueOf(modeStr);

            useParallelProcessing = prefs.getBoolean("hsl_parallel", true);
            threadThreshold = prefs.getInt("hsl_thread_threshold", 10000);

        } catch (Exception e) {
            System.err.println("加载HSL配置失败，使用默认值: " + e.getMessage());
        }
    }

    /**
     * 创建亮度调整操作
     */
    public static ImageOperation createBrightnessOperation(float brightnessDelta) {
        return createBrightnessOperation(brightnessDelta, defaultAlgorithm, defaultPerformanceMode);
    }

    /**
     * 创建亮度调整操作（指定算法）
     */
    public static ImageOperation createBrightnessOperation(float brightnessDelta,
                                                           Algorithm algorithm,
                                                           PerformanceMode mode) {

        switch (algorithm) {
            case BASIC_HSL:
                return HSLBrightnessOperation.createRelativeAdjustment(brightnessDelta);

            case OPTIMIZED_HSL_LUT:
                return createOptimizedOperation(brightnessDelta, mode);

            case OPTIMIZED_HSL_GPU:
                // 预留GPU加速实现
                return createOptimizedOperation(brightnessDelta, mode);

            default:
                throw new IllegalArgumentException("不支持的算法: " + algorithm);
        }
    }

    /**
     * 创建优化的HSL操作
     */
    private static ImageOperation createOptimizedOperation(float brightnessDelta,
                                                           PerformanceMode mode) {

        // 根据性能模式调整参数
        int threshold;
        boolean parallel;

        switch (mode) {
            case QUALITY:
                // 质量模式：使用较小的并行阈值，确保每个线程有足够的工作量
                threshold = 5000;
                parallel = true;
                break;

            case SPEED:
                // 速度模式：增大并行阈值，减少线程开销
                threshold = 50000;
                parallel = useParallelProcessing;
                break;

            case BALANCED:
            default:
                threshold = threadThreshold;
                parallel = useParallelProcessing;
                break;
        }

        return new OptimizedHSLBrightnessOperation(brightnessDelta);
    }

    /**
     * 保存配置到文件
     */
    public static void saveConfiguration(Algorithm algorithm,
                                         PerformanceMode mode,
                                         boolean useParallel,
                                         int threshold) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(HSLOperationFactory.class);

            prefs.put("hsl_algorithm", algorithm.name());
            prefs.put("hsl_performance_mode", mode.name());
            prefs.putBoolean("hsl_parallel", useParallel);
            prefs.putInt("hsl_thread_threshold", threshold);

            prefs.sync();

            // 更新内存中的配置
            defaultAlgorithm = algorithm;
            defaultPerformanceMode = mode;
            useParallelProcessing = useParallel;
            threadThreshold = threshold;

        } catch (Exception e) {
            System.err.println("保存HSL配置失败: " + e.getMessage());
        }
    }

    // Getter方法
    public static Algorithm getDefaultAlgorithm() { return defaultAlgorithm; }
    public static PerformanceMode getDefaultPerformanceMode() { return defaultPerformanceMode; }
    public static boolean isUseParallelProcessing() { return useParallelProcessing; }
    public static int getThreadThreshold() { return threadThreshold; }
}