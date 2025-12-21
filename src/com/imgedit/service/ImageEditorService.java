package imgedit.service;

import imgedit.core.ImageOperation;
import imgedit.core.ImageProcessor;
import imgedit.core.operations.*;
import imgedit.model.ImageEditRequest;
import imgedit.model.enums.OperationType;
import imgedit.utils.ImageUtils;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 图片编辑服务 - 协调UI层与核心处理层
 * 负责：
 * 1. 管理图片处理器的生命周期
 * 2. 执行同步/异步图片操作
 * 3. 处理撤销/重做
 * 4. 资源清理
 */
public class ImageEditorService {
    // 核心处理器实例
    private ImageProcessor imageProcessor;
    // 线程池用于异步处理
    private final ExecutorService executorService;

    public ImageEditorService() {
        // 使用有界线程池，设置合理的线程命名
        this.executorService = Executors.newFixedThreadPool(
                2,
                new ThreadFactory() {
                    private final AtomicInteger threadCount = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("image-editor-thread-" + threadCount.getAndIncrement());
                        thread.setDaemon(true); // 设置为守护线程，避免阻塞JVM退出
                        return thread;
                    }
                }
        );
    }

    /**
     * 初始化图片处理器
     */
    public void initImageProcessor(BufferedImage initialImage) {
        if (initialImage == null) {
            throw new IllegalArgumentException("初始图片不能为null");
        }

        BufferedImage bufferedImage = ImageUtils.fxImageToBufferedImage(initialImage);
        this.imageProcessor = new ImageProcessor(bufferedImage);
    }

    /**
     * 应用图片操作（同步方法）
     */
    public Image applyOperation(ImageOperation operation) {
        if (imageProcessor == null) {
            throw new IllegalStateException("图片处理器未初始化，请先加载图片");
        }

        if (operation == null) {
            throw new IllegalArgumentException("操作不能为null");
        }

        try {
            imageProcessor.applyOperation(operation);
            BufferedImage processedImage = imageProcessor.getCurrentImage();
            return ImageUtils.bufferedImageToFXImage(processedImage);
        } catch (Exception e) {
            throw new RuntimeException("图片处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用图片操作（异步方法）
     */
    public void applyOperationAsync(ImageOperation operation,
                                    Consumer<Image> onSuccess,
                                    Consumer<Exception> onError) {
        if (operation == null) {
            throw new IllegalArgumentException("操作不能为null");
        }

        executorService.submit(() -> {
            try {
                Image result = applyOperation(operation);
                Platform.runLater(() -> {
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (onError != null) {
                        onError.accept(e);
                    }
                });
            }
        });
    }

    /**
     * 处理图片编辑请求
     */
    public Image processEditRequest(ImageEditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为null");
        }

        ImageOperation operation = createOperationFromRequest(request);
        return applyOperation(operation);
    }

    /**
     * 从请求对象创建操作实例
     */
    public ImageOperation createOperationFromRequest(ImageEditRequest request) {
        OperationType operationType = request.getOperationType();
        Map<String, Object> params = request.getParameters();

        try {
            switch (operationType) {
                case BRIGHTNESS:
                    Object brightnessParam = params.get("brightness");
                    if (brightnessParam == null) {
                        throw new IllegalArgumentException("亮度参数不能为空");
                    }
                    float brightnessValue = ((Number) brightnessParam).floatValue();

                    // 创建BrightnessMode（假设有INCREASE和DECREASE）
                    BrightnessOperation.BrightnessMode mode = brightnessValue >= 0 ?
                            BrightnessOperation.BrightnessMode.INCREASE :
                            BrightnessOperation.BrightnessMode.DECREASE;
                    float intensity = Math.abs(brightnessValue);

                    return new BrightnessOperation(mode, intensity);

                case CONTRAST:
                    float contrast = getFloatParam(params, "contrast");
                    // return new imgedit.core.operations.ContrastOperation(contrast);
                    return createContrastOperation(contrast);

                case CROP:
                    int x = getIntParam(params, "x");
                    int y = getIntParam(params, "y");
                    int width = getIntParam(params, "width");
                    int height = getIntParam(params, "height");
                    return new CropOperation(x, y, width, height);

                case ROTATE:
                    // 根据参数创建旋转操作
                    Object angleObj = params.get("angle");
                    if (angleObj instanceof String) {
                        String angleStr = (String) angleObj;
                        switch (angleStr) {
                            case "90": return RotateOperation.create90Degree();
                            case "180": return RotateOperation.create180Degree();
                            case "270": return RotateOperation.create270Degree();
                            default: throw new IllegalArgumentException("不支持的旋转角度");
                        }
                    }
                    double angle = getDoubleParam(params, "angle");
                    return new RotateOperation(RotateOperation.RotationAngle.valueOf("ANGLE_" + (int)angle));

                case BLUR:
                    // 模糊操作
                    Object intensityObj = params.get("intensity");
                    BlurOperation.BlurIntensity blurIntensity = null;
                    if (intensityObj instanceof String) {
                        blurIntensity = BlurOperation.BlurIntensity.valueOf(
                                ((String) intensityObj).toUpperCase());
                    } else if (intensityObj instanceof Integer) {
                        int level = (int) intensityObj;
                        if (level <= 3) blurIntensity = BlurOperation.BlurIntensity.LIGHT;
                        else if (level <= 6) blurIntensity = BlurOperation.BlurIntensity.MEDIUM;
                        else blurIntensity = BlurOperation.BlurIntensity.STRONG;
                    }
                    return new BlurOperation(blurIntensity);

                // AI操作暂时返回模拟操作或抛出异常
                case AI_ENHANCE:
                    // 如果AIEnhanceOperation不存在或需要参数，这里需要调整
                    // return new imgedit.core.operations.AIEnhanceOperation();
                    throw new UnsupportedOperationException("AI增强功能尚未实现");

                case BACKGROUND_REMOVAL:
                    // return new imgedit.core.operations.BackgroundRemovalOperation();
                    throw new UnsupportedOperationException("背景移除功能尚未实现");

                case ARTISTIC_STYLE:
                    String style = (String) params.get("style");
                    if (style == null || style.trim().isEmpty()) {
                        throw new IllegalArgumentException("艺术风格参数不能为空");
                    }
                    // 假设需要两个参数：风格和强度
                    // return new imgedit.core.operations.ArtisticStyleOperation(style, 1.0f);
                    throw new UnsupportedOperationException("艺术风格功能尚未实现");

                default:
                    throw new IllegalArgumentException("不支持的操作类型: " + operationType);
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("参数类型错误: " + e.getMessage(), e);
        }
    }

    // ========== 辅助方法：参数提取 ==========

    // 添加这个方法到辅助方法区域
    private double getDoubleParam(Map<String, Object> params, String key) {
        return getDoubleParam(params, key, 0.0);
    }

    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("参数'" + key + "'应为数字类型");
    }

    private float getFloatParam(Map<String, Object> params, String key) {
        return getFloatParam(params, key, 0.0f);
    }

    private float getFloatParam(Map<String, Object> params, String key, float defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        throw new IllegalArgumentException("参数'" + key + "'应为数字类型");
    }

    private int getIntParam(Map<String, Object> params, String key) {
        return getIntParam(params, key, 0);
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("参数'" + key + "'应为整数类型");
    }

    // ========== 创建操作实例的方法（适配实际的操作类） ==========

    private ImageOperation createBrightnessOperation(String modeStr, float intensity) {
        // 根据字符串模式创建
        BrightnessOperation.BrightnessMode mode =
                BrightnessOperation.BrightnessMode.valueOf(modeStr.toUpperCase());
        return new BrightnessOperation(mode, intensity);
    }

    private ImageOperation createContrastOperation(float contrast) {
        return new ContrastOperation(contrast);
    }

    private ImageOperation createCropOperation(int x, int y, int width, int height) {
        return new CropOperation(x, y, width, height);
    }

    private ImageOperation createRotateOperation(RotateOperation.RotationAngle angle) {
        // 这里需要根据实际的RotateOperation类来创建
        // 如果RotateOperation需要一个参数：
        return new RotateOperation(angle);
        // 如果需要两个参数（如angle和interpolation）：
        // return new imgedit.core.operations.RotateOperation(angle, "bilinear");
    }

//    private ImageOperation createBlurOperation(int radius, float intensity) {
//        // 这里需要根据实际的BlurOperation类来创建
//        // 根据报错信息，BlurOperation需要两个参数
//        return new imgedit.core.operations.BlurOperation(radius, intensity);
//    }

    // ========== 历史记录管理 ==========

    /**
     * 撤销操作
     */
    public Image undo() {
        if (imageProcessor != null && imageProcessor.canUndo()) {
            imageProcessor.undo();
            BufferedImage bufferedImage = imageProcessor.getCurrentImage();
            return ImageUtils.bufferedImageToFXImage(bufferedImage);
        }
        return null;
    }

    /**
     * 重做操作
     */
    public Image redo() {
        if (imageProcessor != null && imageProcessor.canRedo()) {
            imageProcessor.redo();
            BufferedImage bufferedImage = imageProcessor.getCurrentImage();
            return ImageUtils.bufferedImageToFXImage(bufferedImage);
        }
        return null;
    }

    /**
     * 获取当前图片
     */
    public Image getCurrentImage() {
        if (imageProcessor == null) {
            return null;
        }
        BufferedImage bufferedImage = imageProcessor.getCurrentImage();
        return ImageUtils.bufferedImageToFXImage(bufferedImage);
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // ========== 状态检查方法 ==========

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return imageProcessor != null;
    }

    public boolean canUndo() {
        return imageProcessor != null && imageProcessor.canUndo();
    }

    public boolean canRedo() {
        return imageProcessor != null && imageProcessor.canRedo();
    }

    /**
     * 获取图片处理器（供测试或其他服务使用）
     */
    public ImageProcessor getImageProcessor() {
        return imageProcessor;
    }
}