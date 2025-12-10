package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * 图片灰度化操作实现类
 *
 * 功能说明：
 * - 将彩色图片转换为灰度图片
 * - 支持多种灰度转换算法
 * - 保持Alpha通道信息
 *
 * 灰度算法对比：
 * - 平均值法：简单快速，但效果一般
 * - 加权平均法（亮度法）：人眼感知更自然
 * - 去饱和度法：保留彩色图片的亮度信息
 */
public class GrayscaleOperation implements ImageOperation {

    /**
     * 灰度转换算法枚举
     */
    public enum GrayscaleAlgorithm {
        AVERAGE("平均值法", 0.3333f, 0.3333f, 0.3333f),
        LUMINOSITY("亮度加权法", 0.299f, 0.587f, 0.114f),
        DESATURATION("去饱和度法", 0.0f, 0.0f, 0.0f); // 特殊处理

        private final String description;
        private final float redWeight;
        private final float greenWeight;
        private final float blueWeight;

        GrayscaleAlgorithm(String description, float redWeight, float greenWeight, float blueWeight) {
            this.description = description;
            this.redWeight = redWeight;
            this.greenWeight = greenWeight;
            this.blueWeight = blueWeight;
        }

        public String getDescription() { return description; }
        public float getRedWeight() { return redWeight; }
        public float getGreenWeight() { return greenWeight; }
        public float getBlueWeight() { return blueWeight; }
    }

    private final GrayscaleAlgorithm algorithm;

    /**
     * 构造函数（使用默认算法：亮度加权法）
     */
    public GrayscaleOperation() {
        this(GrayscaleAlgorithm.LUMINOSITY);
    }

    /**
     * 构造函数
     *
     * @param algorithm 灰度转换算法
     */
    public GrayscaleOperation(GrayscaleAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * 便捷工厂方法
     */
    public static GrayscaleOperation create() {
        return new GrayscaleOperation();
    }

    public static GrayscaleOperation createWithAlgorithm(GrayscaleAlgorithm algorithm) {
        return new GrayscaleOperation(algorithm);
    }

    /**
     * 应用灰度化操作
     *
     * 实现说明：
     * - 遍历每个像素，计算灰度值
     * - 保持Alpha通道不变
     * - 支持多种灰度算法
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // 创建灰度图片（保持原始图片类型）
            BufferedImage grayscaleImage = new BufferedImage(
                    width, height, BufferedImage.TYPE_BYTE_GRAY);

            // 根据算法类型选择处理方法
            switch (algorithm) {
                case DESATURATION:
                    applyDesaturation(image, grayscaleImage);
                    break;
                default:
                    applyWeightedAverage(image, grayscaleImage);
                    break;
            }

            return grayscaleImage;

        } catch (Exception e) {
            throw new ImageProcessingException("灰度化操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用加权平均法
     */
    private void applyWeightedAverage(BufferedImage source, BufferedImage target) {
        int width = source.getWidth();
        int height = source.getHeight();

        float redWeight = algorithm.getRedWeight();
        float greenWeight = algorithm.getGreenWeight();
        float blueWeight = algorithm.getBlueWeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getRGB(x, y);

                // 提取ARGB通道
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 计算灰度值（加权平均）
                int gray;
                if (algorithm == GrayscaleAlgorithm.AVERAGE) {
                    // 平均值法
                    gray = (red + green + blue) / 3;
                } else {
                    // 亮度加权法
                    gray = (int)(red * redWeight + green * greenWeight + blue * blueWeight);
                }

                // 组合灰度像素（RGB通道都设为灰度值）
                int grayPixel = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
                target.setRGB(x, y, grayPixel);
            }
        }
    }

    /**
     * 应用去饱和度法
     */
    private void applyDesaturation(BufferedImage source, BufferedImage target) {
        int width = source.getWidth();
        int height = source.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getRGB(x, y);

                // 提取ARGB通道
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 去饱和度法：取最大值和最小值，然后取中间值
                int max = Math.max(red, Math.max(green, blue));
                int min = Math.min(red, Math.min(green, blue));
                int gray = (max + min) / 2;

                // 组合灰度像素
                int grayPixel = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
                target.setRGB(x, y, grayPixel);
            }
        }
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        return "灰度化处理 [" + algorithm.getDescription() + "]";
    }

    // ========== Getter方法 ==========

    public GrayscaleAlgorithm getAlgorithm() {
        return algorithm;
    }
}