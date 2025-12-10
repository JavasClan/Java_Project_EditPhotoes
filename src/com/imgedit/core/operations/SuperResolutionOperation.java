package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * 图片超分辨率增强操作实现类
 *
 * 功能说明：
 * - 提升图片分辨率（2x、4x等）
 * - 使用插值算法增强细节
 * - 减少放大时的模糊和锯齿
 *
 * 算法原理：
 * - 基于插值算法：双线性、双三次、Lanczos等
 * - 边缘增强和抗锯齿
 * - 智能锐化以恢复细节
 */
public class SuperResolutionOperation implements ImageOperation {

    /**
     * 放大因子枚举
     */
    public enum ScaleFactor {
        X2("2倍放大", 2),
        X3("3倍放大", 3),
        X4("4倍放大", 4);

        private final String description;
        private final int factor;

        ScaleFactor(String description, int factor) {
            this.description = description;
            this.factor = factor;
        }

        public String getDescription() { return description; }
        public int getFactor() { return factor; }
    }

    /**
     * 插值算法枚举
     */
    public enum InterpolationAlgorithm {
        BILINEAR("双线性插值"),
        BICUBIC("双三次插值"),
        LANCZOS("Lanczos插值"),
        SMART("智能混合插值");

        private final String description;

        InterpolationAlgorithm(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    private final ScaleFactor scaleFactor;
    private final InterpolationAlgorithm algorithm;
    private final float sharpeningStrength; // 锐化强度 (0.0-1.0)

    /**
     * 构造函数
     */
    public SuperResolutionOperation(ScaleFactor scaleFactor,
                                    InterpolationAlgorithm algorithm,
                                    float sharpeningStrength) {
        this.scaleFactor = scaleFactor;
        this.algorithm = algorithm;
        this.sharpeningStrength = Math.max(0.0f, Math.min(1.0f, sharpeningStrength));
    }

    /**
     * 便捷工厂方法
     */
    public static SuperResolutionOperation create2xEnhancement() {
        return new SuperResolutionOperation(ScaleFactor.X2,
                InterpolationAlgorithm.SMART, 0.3f);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();
            int factor = scaleFactor.getFactor();

            int targetWidth = originalWidth * factor;
            int targetHeight = originalHeight * factor;

            // 第一步：基于选定算法进行缩放
            BufferedImage scaledImage = applyInterpolation(image, targetWidth, targetHeight);

            // 第二步：应用边缘增强
            BufferedImage enhancedImage = applyEdgeEnhancement(scaledImage);

            // 第三步：智能锐化
            BufferedImage sharpenedImage = applySmartSharpening(enhancedImage);

            return sharpenedImage;

        } catch (Exception e) {
            throw new ImageProcessingException("超分辨率增强失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用插值算法
     */
    private BufferedImage applyInterpolation(BufferedImage source,
                                             int targetWidth, int targetHeight) {
        BufferedImage result = new BufferedImage(
                targetWidth, targetHeight, source.getType());

        switch (algorithm) {
            case BILINEAR:
                applyBilinearInterpolation(source, result);
                break;
            case BICUBIC:
                applyBicubicInterpolation(source, result);
                break;
            case LANCZOS:
                applyLanczosInterpolation(source, result);
                break;
            case SMART:
                applySmartInterpolation(source, result);
                break;
        }

        return result;
    }

    /**
     * 双线性插值（修复边界问题）
     */
    private void applyBilinearInterpolation(BufferedImage source, BufferedImage target) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int dstWidth = target.getWidth();
        int dstHeight = target.getHeight();

        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                // 计算源图像坐标
                float srcX = (x + 0.5f) * srcWidth / dstWidth - 0.5f;
                float srcY = (y + 0.5f) * srcHeight / dstHeight - 0.5f;

                // 确保坐标在有效范围内
                srcX = Math.max(0, Math.min(srcX, srcWidth - 1));
                srcY = Math.max(0, Math.min(srcY, srcHeight - 1));

                int x1 = (int) Math.floor(srcX);
                int y1 = (int) Math.floor(srcY);
                int x2 = Math.min(x1 + 1, srcWidth - 1);
                int y2 = Math.min(y1 + 1, srcHeight - 1);

                float xWeight = srcX - x1;
                float yWeight = srcY - y1;

                // 获取四个邻域像素
                int q11 = source.getRGB(x1, y1);
                int q12 = source.getRGB(x1, y2);
                int q21 = source.getRGB(x2, y1);
                int q22 = source.getRGB(x2, y2);

                // 双线性插值
                int interpolated = bilinearInterpolate(q11, q12, q21, q22, xWeight, yWeight);
                target.setRGB(x, y, interpolated);
            }
        }
    }

    /**
     * 双线性插值计算
     */
    private int bilinearInterpolate(int q11, int q12, int q21, int q22,
                                    float xWeight, float yWeight) {
        int r1 = interpolateChannel(q11, q21, xWeight, 16);
        int r2 = interpolateChannel(q12, q22, xWeight, 16);
        int r = interpolateValue(r1, r2, yWeight);

        int g1 = interpolateChannel(q11, q21, xWeight, 8);
        int g2 = interpolateChannel(q12, q22, xWeight, 8);
        int g = interpolateValue(g1, g2, yWeight);

        int b1 = interpolateChannel(q11, q21, xWeight, 0);
        int b2 = interpolateChannel(q12, q22, xWeight, 0);
        int b = interpolateValue(b1, b2, yWeight);

        int a1 = interpolateChannel(q11, q21, xWeight, 24);
        int a2 = interpolateChannel(q12, q22, xWeight, 24);
        int a = interpolateValue(a1, a2, yWeight);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 插值单个通道
     */
    private int interpolateChannel(int color1, int color2, float weight, int shift) {
        int value1 = (color1 >> shift) & 0xFF;
        int value2 = (color2 >> shift) & 0xFF;
        return (int)(value1 * (1 - weight) + value2 * weight);
    }

    /**
     * 插值数值
     */
    private int interpolateValue(int value1, int value2, float weight) {
        return (int)(value1 * (1 - weight) + value2 * weight);
    }

    /**
     * 智能混合插值
     */
    private void applySmartInterpolation(BufferedImage source, BufferedImage target) {
        // 结合多种插值算法的优势
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int dstWidth = target.getWidth();
        int dstHeight = target.getHeight();

        // 先使用双三次插值
        applyBicubicInterpolation(source, target);

        // 对边缘区域进行额外处理
        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                // 检测边缘区域
                if (isEdgePixel(source, x * srcWidth / dstWidth, y * srcHeight / dstHeight)) {
                    // 对边缘像素使用更精确的插值
                    int improvedPixel = improveEdgePixel(source, x, y, dstWidth, dstHeight);
                    target.setRGB(x, y, improvedPixel);
                }
            }
        }
    }

    /**
     * 检测是否为边缘像素
     */
    private boolean isEdgePixel(BufferedImage image, int x, int y) {
        if (x <= 0 || x >= image.getWidth() - 1 || y <= 0 || y >= image.getHeight() - 1) {
            return false;
        }

        int centerColor = image.getRGB(x, y);
        int centerLuminance = calculateLuminance(centerColor);

        // 检查周围像素的亮度差异
        int maxDiff = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;

                int neighborColor = image.getRGB(x + dx, y + dy);
                int neighborLuminance = calculateLuminance(neighborColor);
                maxDiff = Math.max(maxDiff, Math.abs(centerLuminance - neighborLuminance));
            }
        }

        return maxDiff > 20; // 阈值
    }

    /**
     * 计算亮度
     */
    private int calculateLuminance(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return (int)(r * 0.299 + g * 0.587 + b * 0.114);
    }

    /**
     * 改进边缘像素插值
     */
    private int improveEdgePixel(BufferedImage source, int dstX, int dstY,
                                 int dstWidth, int dstHeight) {
        // 使用更精确的插值方法处理边缘
        // 这里使用Lanczos插值
        return applyLanczosPixel(source, dstX, dstY, dstWidth, dstHeight);
    }

    /**
     * 应用Lanczos插值
     */
    private void applyLanczosInterpolation(BufferedImage source, BufferedImage target) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int dstWidth = target.getWidth();
        int dstHeight = target.getHeight();

        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                int pixel = applyLanczosPixel(source, x, y, dstWidth, dstHeight);
                target.setRGB(x, y, pixel);
            }
        }
    }

    /**
     * 单个像素的Lanczos插值
     */
    private int applyLanczosPixel(BufferedImage source, int dstX, int dstY,
                                  int dstWidth, int dstHeight) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        float srcX = (dstX + 0.5f) * srcWidth / dstWidth - 0.5f;
        float srcY = (dstY + 0.5f) * srcHeight / dstHeight - 0.5f;

        int a = 2; // Lanczos窗口大小

        float totalWeightR = 0, totalWeightG = 0, totalWeightB = 0, totalWeightA = 0;
        float totalWeightSum = 0;

        int startX = (int) Math.floor(srcX) - a + 1;
        int endX = (int) Math.floor(srcX) + a;
        int startY = (int) Math.floor(srcY) - a + 1;
        int endY = (int) Math.floor(srcY) + a;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (x < 0 || x >= srcWidth || y < 0 || y >= srcHeight) {
                    continue;
                }

                float dx = srcX - x;
                float dy = srcY - y;

                // Lanczos核函数
                float weightX = lanczosKernel(dx, a);
                float weightY = lanczosKernel(dy, a);
                float weight = weightX * weightY;

                if (Math.abs(weight) > 0.0001f) {
                    int pixel = source.getRGB(x, y);

                    totalWeightR += ((pixel >> 16) & 0xFF) * weight;
                    totalWeightG += ((pixel >> 8) & 0xFF) * weight;
                    totalWeightB += (pixel & 0xFF) * weight;
                    totalWeightA += ((pixel >> 24) & 0xFF) * weight;
                    totalWeightSum += weight;
                }
            }
        }

        if (Math.abs(totalWeightSum) > 0.0001f) {
            int r = clamp((int)(totalWeightR / totalWeightSum));
            int g = clamp((int)(totalWeightG / totalWeightSum));
            int b = clamp((int)(totalWeightB / totalWeightSum));
            int aChannel = clamp((int)(totalWeightA / totalWeightSum));

            return (aChannel << 24) | (r << 16) | (g << 8) | b;
        }

        // 回退到双线性插值
        return bilinearInterpolateForPixel(source, srcX, srcY);
    }

    /**
     * Lanczos核函数
     */
    private float lanczosKernel(float x, int a) {
        if (x == 0) return 1;
        if (Math.abs(x) >= a) return 0;

        float piX = (float) (Math.PI * x);
        float piXOverA = (float) (Math.PI * x / a);

        return (float) (Math.sin(piX) * Math.sin(piXOverA) / (piX * piXOverA));
    }

    /**
     * 为单个像素进行双线性插值
     */
    private int bilinearInterpolateForPixel(BufferedImage source, float srcX, float srcY) {
        int x1 = (int) Math.floor(srcX);
        int y1 = (int) Math.floor(srcY);
        int x2 = Math.min(x1 + 1, source.getWidth() - 1);
        int y2 = Math.min(y1 + 1, source.getHeight() - 1);

        float xWeight = srcX - x1;
        float yWeight = srcY - y1;

        int q11 = source.getRGB(x1, y1);
        int q12 = source.getRGB(x1, y2);
        int q21 = source.getRGB(x2, y1);
        int q22 = source.getRGB(x2, y2);

        return bilinearInterpolate(q11, q12, q21, q22, xWeight, yWeight);
    }

    /**
     * 应用边缘增强
     */
    private BufferedImage applyEdgeEnhancement(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage enhanced = new BufferedImage(width, height, image.getType());

        // 使用拉普拉斯算子进行边缘增强
        float[] laplacianKernel = {
                -1, -1, -1,
                -1,  8, -1,
                -1, -1, -1
        };

        Kernel kernel = new Kernel(3, 3, laplacianKernel);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        BufferedImage edges = convolveOp.filter(image, null);

        // 将边缘叠加到原图
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int original = image.getRGB(x, y);
                int edge = edges.getRGB(x, y);

                // 提取边缘强度
                int edgeStrength = (edge & 0xFF); // 假设灰度图

                // 增强原图
                int enhancedPixel = addEdgeToPixel(original, edgeStrength, 0.1f);
                enhanced.setRGB(x, y, enhancedPixel);
            }
        }

        return enhanced;
    }

    /**
     * 将边缘添加到像素
     */
    private int addEdgeToPixel(int pixel, int edgeStrength, float strengthFactor) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        int a = (pixel >> 24) & 0xFF;

        int enhancement = (int)(edgeStrength * strengthFactor);

        r = clamp(r + enhancement);
        g = clamp(g + enhancement);
        b = clamp(b + enhancement);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 应用智能锐化
     */
    private BufferedImage applySmartSharpening(BufferedImage image) {
        if (sharpeningStrength <= 0) return image;

        int width = image.getWidth();
        int height = image.getHeight();

        // USM锐化算法
        float radius = 1.0f;
        float amount = sharpeningStrength * 100; // 转换为百分比

        BufferedImage blurred = applyGaussianBlur(image, radius);
        BufferedImage sharpened = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int original = image.getRGB(x, y);
                int blur = blurred.getRGB(x, y);

                // USM公式：sharp = original + (original - blur) * amount
                int sharpenedPixel = usmSharpen(original, blur, amount);
                sharpened.setRGB(x, y, sharpenedPixel);
            }
        }

        return sharpened;
    }

    /**
     * USM锐化
     */
    private int usmSharpen(int original, int blur, float amount) {
        int or = (original >> 16) & 0xFF;
        int og = (original >> 8) & 0xFF;
        int ob = original & 0xFF;
        int oa = (original >> 24) & 0xFF;

        int br = (blur >> 16) & 0xFF;
        int bg = (blur >> 8) & 0xFF;
        int bb = blur & 0xFF;

        int sr = clamp((int)(or + (or - br) * amount / 100.0f));
        int sg = clamp((int)(og + (og - bg) * amount / 100.0f));
        int sb = clamp((int)(ob + (ob - bb) * amount / 100.0f));

        return (oa << 24) | (sr << 16) | (sg << 8) | sb;
    }

    /**
     * 应用高斯模糊
     */
    private BufferedImage applyGaussianBlur(BufferedImage image, float radius) {
        int size = (int)(radius * 2) * 2 + 1;
        float[] kernel = createGaussianKernel(size, radius);

        Kernel gaussianKernel = new Kernel(size, size, kernel);
        ConvolveOp convolveOp = new ConvolveOp(gaussianKernel, ConvolveOp.EDGE_NO_OP, null);

        return convolveOp.filter(image, null);
    }

    /**
     * 创建高斯核
     */
    private float[] createGaussianKernel(int size, float sigma) {
        float[] kernel = new float[size * size];
        float sum = 0;
        int center = size / 2;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - center;
                int dy = y - center;
                float value = (float) Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
                kernel[y * size + x] = value;
                sum += value;
            }
        }

        // 归一化
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }

    /**
     * 双三次插值（简化实现）
     */
    private void applyBicubicInterpolation(BufferedImage source, BufferedImage target) {
        // 由于双三次插值实现较复杂，这里暂时使用双线性插值代替
        // 实际项目中应实现完整的双三次插值算法
        applyBilinearInterpolation(source, target);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public String getOperationName() {
        return String.format("超分辨率增强 [%s, 算法:%s, 锐化:%.1f]",
                scaleFactor.getDescription(),
                algorithm.getDescription(),
                sharpeningStrength);
    }
}