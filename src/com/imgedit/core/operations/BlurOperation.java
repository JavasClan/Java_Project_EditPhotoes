package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * 图片模糊操作实现类
 *
 * 功能说明：
 * - 支持高斯模糊效果
 * - 可调节模糊半径
 * - 保持图片边缘处理
 *
 * 算法原理：
 * - 使用卷积核（Kernel）对图像进行滤波
 * - 高斯模糊使用正态分布权重
 * - 边缘使用边缘复制策略处理
 */
public class BlurOperation implements ImageOperation {

    /**
     * 模糊强度枚举
     */
    public enum BlurIntensity {
        LIGHT("轻微模糊", 3),
        MEDIUM("中等模糊", 5),
        STRONG("强烈模糊", 7);

        private final String description;
        private final int kernelSize; // 卷积核大小

        BlurIntensity(String description, int kernelSize) {
            this.description = description;
            this.kernelSize = kernelSize;
        }

        public String getDescription() { return description; }
        public int getKernelSize() { return kernelSize; }
    }

    private final BlurIntensity intensity;

    /**
     * 构造函数
     *
     * @param intensity 模糊强度
     */
    public BlurOperation(BlurIntensity intensity) {
        this.intensity = intensity;
    }

    /**
     * 便捷工厂方法
     */
    public static BlurOperation createLightBlur() {
        return new BlurOperation(BlurIntensity.LIGHT);
    }

    public static BlurOperation createMediumBlur() {
        return new BlurOperation(BlurIntensity.MEDIUM);
    }

    public static BlurOperation createStrongBlur() {
        return new BlurOperation(BlurIntensity.STRONG);
    }

    /**
     * 应用模糊操作
     *
     * 实现说明：
     * - 创建高斯模糊卷积核
     * - 使用ConvolveOp进行卷积运算
     * - 边缘区域使用复制边缘像素策略
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int kernelSize = intensity.getKernelSize();

            // 创建高斯模糊卷积核
            float[] kernelData = createGaussianKernel(kernelSize);
            Kernel kernel = new Kernel(kernelSize, kernelSize, kernelData);

            // 创建卷积操作对象
            // 使用边缘复制策略，避免边缘出现黑色边框
            ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

            // 应用模糊效果
            BufferedImage blurredImage = convolveOp.filter(image, null);

            return blurredImage;

        } catch (OutOfMemoryError e) {
            throw new ImageProcessingException("内存不足，无法处理大图片的模糊操作", e);
        } catch (Exception e) {
            throw new ImageProcessingException("模糊操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建高斯模糊卷积核
     *
     * @param size 卷积核大小（必须是奇数）
     * @return 卷积核数据数组
     *
     * 数学原理：
     * - 高斯函数：G(x,y) = (1/(2πσ²)) * e^(-(x²+y²)/(2σ²))
     * - 这里简化实现，使用近似高斯权重
     */
    private float[] createGaussianKernel(int size) {
        if (size % 2 == 0) {
            throw new IllegalArgumentException("卷积核大小必须是奇数");
        }

        float[] kernel = new float[size * size];
        float sigma = size / 3.0f; // 标准差
        float sum = 0.0f;

        int center = size / 2;

        // 计算高斯权重
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - center;
                int dy = y - center;

                // 二维高斯函数
                float value = (float) Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
                value /= (2 * Math.PI * sigma * sigma);

                kernel[y * size + x] = value;
                sum += value;
            }
        }

        // 归一化，确保所有权重之和为1
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }

    /**
     * 创建简单均值模糊卷积核（替代方案，性能更好）
     */
    private float[] createSimpleKernel(int size) {
        float[] kernel = new float[size * size];
        float value = 1.0f / (size * size);

        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = value;
        }

        return kernel;
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        return "模糊处理 [" + intensity.getDescription() + "]";
    }

    // ========== Getter方法 ==========

    public BlurIntensity getIntensity() {
        return intensity;
    }
}