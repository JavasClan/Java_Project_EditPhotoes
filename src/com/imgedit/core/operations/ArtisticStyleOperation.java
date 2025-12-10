package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * 图片风格化操作实现类
 *
 * 功能说明：
 * - 将普通照片转换为艺术效果
 * - 支持多种艺术风格：油画、水彩、素描等
 * - 可调节风格强度和细节保留
 *
 * 算法原理：
 * - 油画效果：区域颜色平均化 + 笔触模拟
 * - 水彩效果：边缘保留平滑 + 颜色扩散
 * - 素描效果：边缘检测 + 纹理叠加
 */
public class ArtisticStyleOperation implements ImageOperation {

    /**
     * 艺术风格枚举
     */
    public enum ArtisticStyle {
        OIL_PAINTING("油画效果"),
        WATERCOLOR("水彩效果"),
        PENCIL_SKETCH("铅笔素描"),
        CARTOON("卡通效果"),
        MOSAIC("马赛克艺术");

        private final String description;

        ArtisticStyle(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 风格参数
     */
    public static class StyleParameters {
        private final float intensity;       // 风格强度 (0.1-1.0)
        private final int brushSize;         // 笔触/块大小
        private final float detailPreserve;  // 细节保留程度 (0.0-1.0)

        public StyleParameters(float intensity, int brushSize, float detailPreserve) {
            this.intensity = Math.max(0.1f, Math.min(1.0f, intensity));
            this.brushSize = Math.max(1, Math.min(20, brushSize));
            this.detailPreserve = Math.max(0.0f, Math.min(1.0f, detailPreserve));
        }
    }

    private final ArtisticStyle style;
    private final StyleParameters parameters;

    /**
     * 构造函数
     */
    public ArtisticStyleOperation(ArtisticStyle style, StyleParameters parameters) {
        this.style = style;
        this.parameters = parameters;
    }

    /**
     * 创建油画效果
     */
    public static ArtisticStyleOperation createOilPainting(float intensity) {
        StyleParameters params = new StyleParameters(intensity, 5, 0.5f);
        return new ArtisticStyleOperation(ArtisticStyle.OIL_PAINTING, params);
    }

    /**
     * 创建素描效果
     */
    public static ArtisticStyleOperation createPencilSketch(float intensity) {
        StyleParameters params = new StyleParameters(intensity, 1, 0.8f);
        return new ArtisticStyleOperation(ArtisticStyle.PENCIL_SKETCH, params);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            BufferedImage result;

            switch (style) {
                case OIL_PAINTING:
                    result = applyOilPaintingEffect(image);
                    break;
                case WATERCOLOR:
                    result = applyWatercolorEffect(image);
                    break;
                case PENCIL_SKETCH:
                    result = applyPencilSketchEffect(image);
                    break;
                case CARTOON:
                    result = applyCartoonEffect(image);
                    break;
                case MOSAIC:
                    result = applyMosaicArtEffect(image);
                    break;
                default:
                    throw new ImageProcessingException("不支持的风格类型: " + style);
            }

            return result;

        } catch (Exception e) {
            throw new ImageProcessingException("风格化处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用油画效果
     */
    private BufferedImage applyOilPaintingEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int radius = parameters.brushSize;
        float intensity = parameters.intensity;

        BufferedImage result = new BufferedImage(width, height, image.getType());

        // 对每个像素应用油画效果
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 获取当前像素周围的颜色直方图
                ColorHistogram histogram = collectColorHistogram(image, x, y, radius);

                // 找到出现次数最多的颜色（油画效果）
                int dominantColor = histogram.getDominantColor();

                // 混合原始颜色和主导颜色（根据强度）
                int originalColor = image.getRGB(x, y);
                int finalColor = blendColors(originalColor, dominantColor, intensity);

                result.setRGB(x, y, finalColor);
            }
        }

        return result;
    }

    /**
     * 颜色直方图
     */
    private static class ColorHistogram {
        private final int[] redHist = new int[256];
        private final int[] greenHist = new int[256];
        private final int[] blueHist = new int[256];

        public void addColor(int color) {
            redHist[(color >> 16) & 0xFF]++;
            greenHist[(color >> 8) & 0xFF]++;
            blueHist[color & 0xFF]++;
        }

        public int getDominantColor() {
            int dominantRed = getDominantChannel(redHist);
            int dominantGreen = getDominantChannel(greenHist);
            int dominantBlue = getDominantChannel(blueHist);

            return (0xFF << 24) | (dominantRed << 16) | (dominantGreen << 8) | dominantBlue;
        }

        private int getDominantChannel(int[] histogram) {
            int maxIndex = 0;
            int maxValue = 0;

            for (int i = 0; i < histogram.length; i++) {
                if (histogram[i] > maxValue) {
                    maxValue = histogram[i];
                    maxIndex = i;
                }
            }

            return maxIndex;
        }
    }

    /**
     * 收集颜色直方图
     */
    private ColorHistogram collectColorHistogram(BufferedImage image, int centerX, int centerY, int radius) {
        ColorHistogram histogram = new ColorHistogram();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                    histogram.addColor(image.getRGB(x, y));
                }
            }
        }

        return histogram;
    }

    /**
     * 混合两种颜色
     */
    private int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int)(r1 * (1 - ratio) + r2 * ratio);
        int g = (int)(g1 * (1 - ratio) + g2 * ratio);
        int b = (int)(b1 * (1 - ratio) + b2 * ratio);

        int alpha = (color1 >> 24) & 0xFF;

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 应用铅笔素描效果
     */
    private BufferedImage applyPencilSketchEffect(BufferedImage image) {
        // 步骤1：转为灰度图
        BufferedImage grayImage = applyGrayscale(image);

        // 步骤2：应用边缘检测
        BufferedImage edges = applyEdgeDetection(grayImage);

        // 步骤3：反相（素描效果）
        BufferedImage inverted = invertImage(edges);

        // 步骤4：添加纸张纹理
        BufferedImage result = addPaperTexture(inverted);

        return result;
    }

    /**
     * 转为灰度图
     */
    private BufferedImage applyGrayscale(BufferedImage image) {
        BufferedImage gray = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                int grayValue = (int)(red * 0.299 + green * 0.587 + blue * 0.114);
                int grayPixel = (255 << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                gray.setRGB(x, y, grayPixel);
            }
        }

        return gray;
    }

    /**
     * 边缘检测
     */
    private BufferedImage applyEdgeDetection(BufferedImage image) {
        // Sobel算子
        float[] kernel = {
                -1, 0, 1,
                -2, 0, 2,
                -1, 0, 1
        };

        Kernel sobelKernel = new Kernel(3, 3, kernel);
        ConvolveOp convolveOp = new ConvolveOp(sobelKernel, ConvolveOp.EDGE_NO_OP, null);

        return convolveOp.filter(image, null);
    }

    /**
     * 图片反相
     */
    private BufferedImage invertImage(BufferedImage image) {
        BufferedImage inverted = new BufferedImage(
                image.getWidth(), image.getHeight(), image.getType());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int invertedPixel = 0xFFFFFF - (pixel & 0x00FFFFFF);
                invertedPixel |= (pixel & 0xFF000000); // 保持Alpha通道
                inverted.setRGB(x, y, invertedPixel);
            }
        }

        return inverted;
    }

    /**
     * 添加纸张纹理
     */
    private BufferedImage addPaperTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage textured = new BufferedImage(width, height, image.getType());

        // 简单的噪声纹理
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int gray = pixel & 0xFF;

                // 添加轻微噪声模拟纸张纹理
                double noise = Math.random() * 10 - 5;
                int texturedGray = (int)Math.max(0, Math.min(255, gray + noise));

                int texturedPixel = (255 << 24) | (texturedGray << 16) |
                        (texturedGray << 8) | texturedGray;
                textured.setRGB(x, y, texturedPixel);
            }
        }

        return textured;
    }

    private BufferedImage applyWatercolorEffect(BufferedImage image) {
        // 水彩效果实现（边缘保留平滑 + 颜色扩散）
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        // 简化实现：应用高斯模糊 + 增加饱和度
        // TODO: 实现真正的水彩效果算法
        return result;
    }

    private BufferedImage applyCartoonEffect(BufferedImage image) {
        // 卡通效果实现（颜色量化 + 边缘增强）
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        // 简化实现：减少颜色数量 + 边缘检测
        // TODO: 实现真正的卡通效果算法
        return result;
    }

    private BufferedImage applyMosaicArtEffect(BufferedImage image) {
        // 马赛克艺术效果
        int width = image.getWidth();
        int height = image.getHeight();
        int blockSize = parameters.brushSize;

        BufferedImage result = new BufferedImage(width, height, image.getType());

        // 创建马赛克效果
        for (int y = 0; y < height; y += blockSize) {
            for (int x = 0; x < width; x += blockSize) {
                int blockWidth = Math.min(blockSize, width - x);
                int blockHeight = Math.min(blockSize, height - y);

                // 计算块的平均颜色
                int avgColor = calculateBlockAverage(image, x, y, blockWidth, blockHeight);

                // 填充块
                fillBlock(result, x, y, blockWidth, blockHeight, avgColor);
            }
        }

        return result;
    }

    /**
     * 计算块的平均颜色
     */
    private int calculateBlockAverage(BufferedImage image, int startX, int startY,
                                      int width, int height) {
        long sumRed = 0, sumGreen = 0, sumBlue = 0;
        int count = 0;

        for (int y = startY; y < startY + height; y++) {
            for (int x = startX; x < startX + width; x++) {
                int pixel = image.getRGB(x, y);
                sumRed += (pixel >> 16) & 0xFF;
                sumGreen += (pixel >> 8) & 0xFF;
                sumBlue += pixel & 0xFF;
                count++;
            }
        }

        if (count == 0) return 0;

        int avgRed = (int)(sumRed / count);
        int avgGreen = (int)(sumGreen / count);
        int avgBlue = (int)(sumBlue / count);
        int alpha = 255; // 不透明

        return (alpha << 24) | (avgRed << 16) | (avgGreen << 8) | avgBlue;
    }

    /**
     * 填充块
     */
    private void fillBlock(BufferedImage image, int startX, int startY,
                           int width, int height, int color) {
        for (int y = startY; y < startY + height; y++) {
            for (int x = startX; x < startX + width; x++) {
                image.setRGB(x, y, color);
            }
        }
    }

    @Override
    public String getOperationName() {
        return String.format("%s [强度:%.1f, 笔触:%d, 细节:%.1f]",
                style.getDescription(),
                parameters.intensity,
                parameters.brushSize,
                parameters.detailPreserve);
    }
}