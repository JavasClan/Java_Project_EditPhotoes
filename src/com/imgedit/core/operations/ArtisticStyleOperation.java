package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.Color;
import java.awt.Graphics2D;

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
 * - 卡通效果：颜色量化 + 边缘增强
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
        // Sobel算子（水平和垂直）
        float[] kernelX = {
                -1, 0, 1,
                -2, 0, 2,
                -1, 0, 1
        };

        float[] kernelY = {
                -1, -2, -1,
                0,  0,  0,
                1,  2,  1
        };

        Kernel sobelKernelX = new Kernel(3, 3, kernelX);
        Kernel sobelKernelY = new Kernel(3, 3, kernelY);

        ConvolveOp convolveOpX = new ConvolveOp(sobelKernelX, ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp convolveOpY = new ConvolveOp(sobelKernelY, ConvolveOp.EDGE_NO_OP, null);

        BufferedImage edgeX = convolveOpX.filter(image, null);
        BufferedImage edgeY = convolveOpY.filter(image, null);

        // 合并X和Y方向的边缘
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelX = edgeX.getRGB(x, y);
                int pixelY = edgeY.getRGB(x, y);

                // 提取灰度值
                int grayX = (pixelX >> 16) & 0xFF;
                int grayY = (pixelY >> 16) & 0xFF;

                // 计算梯度幅度
                int gradient = (int)Math.sqrt(grayX * grayX + grayY * grayY);
                gradient = Math.min(255, gradient);

                int edgePixel = (255 << 24) | (gradient << 16) | (gradient << 8) | gradient;
                result.setRGB(x, y, edgePixel);
            }
        }

        return result;
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

    /**
     * 应用水彩效果
     */
    private BufferedImage applyWatercolorEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 步骤1：创建画布并复制原始图像
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // 步骤2：应用高斯模糊模拟水彩扩散
        BufferedImage blurred = applyGaussianBlur(result, 2);

        // 步骤3：边缘检测（获取边缘信息）
        BufferedImage grayImage = applyGrayscale(result);
        BufferedImage edges = applyEdgeDetection(grayImage);

        // 步骤4：边缘保留混合
        result = blendWatercolorWithEdges(blurred, edges);

        // 步骤5：增加饱和度模拟水彩鲜艳效果
        result = enhanceSaturation(result, 1.3f);

        // 步骤6：轻微噪点模拟水彩画纸纹理
        result = addWatercolorTexture(result);

        return result;
    }

    /**
     * 高斯模糊
     */
    private BufferedImage applyGaussianBlur(BufferedImage image, int radius) {
        // 创建高斯核
        int size = radius * 2 + 1;
        float[] kernel = new float[size * size];
        float sigma = radius / 2.0f;
        float sum = 0.0f;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float value = (float)Math.exp(-(x*x + y*y) / (2 * sigma * sigma));
                kernel[(y+radius) * size + (x+radius)] = value;
                sum += value;
            }
        }

        // 归一化
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        Kernel gaussianKernel = new Kernel(size, size, kernel);
        ConvolveOp convolveOp = new ConvolveOp(gaussianKernel, ConvolveOp.EDGE_NO_OP, null);

        return convolveOp.filter(image, null);
    }

    /**
     * 水彩边缘保留混合
     */
    private BufferedImage blendWatercolorWithEdges(BufferedImage colorImage, BufferedImage edgeImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = colorImage.getRGB(x, y);
                int edge = edgeImage.getRGB(x, y);

                // 获取边缘强度
                int edgeIntensity = (edge >> 16) & 0xFF;
                float edgeFactor = edgeIntensity / 255.0f;

                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                // 根据边缘强度调整颜色
                if (edgeFactor > 0.2f) { // 边缘区域
                    // 边缘稍微加深
                    r = Math.max(0, r - 30);
                    g = Math.max(0, g - 30);
                    b = Math.max(0, b - 30);
                } else { // 非边缘区域
                    // 稍微提亮和柔化
                    r = Math.min(255, r + 20);
                    g = Math.min(255, g + 20);
                    b = Math.min(255, b + 20);
                }

                int alpha = (color >> 24) & 0xFF;
                result.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 增加饱和度
     */
    private BufferedImage enhanceSaturation(BufferedImage image, float saturationFactor) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getRGB(x, y);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                // 转换为HSB颜色空间
                float[] hsb = Color.RGBtoHSB(r, g, b, null);

                // 增加饱和度
                hsb[1] = Math.min(1.0f, hsb[1] * saturationFactor);

                // 转换回RGB
                int enhancedColor = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                int alpha = (color >> 24) & 0xFF;
                result.setRGB(x, y, (alpha << 24) | (enhancedColor & 0x00FFFFFF));
            }
        }

        return result;
    }

    /**
     * 添加水彩纹理
     */
    private BufferedImage addWatercolorTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getRGB(x, y);

                // 添加轻微的不规则噪点，模拟水彩画纸
                double noise = (Math.random() - 0.5) * 15;

                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                r = (int)Math.max(0, Math.min(255, r + noise));
                g = (int)Math.max(0, Math.min(255, g + noise));
                b = (int)Math.max(0, Math.min(255, b + noise));

                int alpha = (color >> 24) & 0xFF;
                result.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 应用卡通效果 - 修复版本
     */
    private BufferedImage applyCartoonEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 步骤1：颜色量化（减少颜色数量）
        BufferedImage quantized = quantizeColors(image, 8);

        // 步骤2：边缘检测（使用更精细的边缘检测）
        BufferedImage edges = detectCartoonEdges(image);

        // 步骤3：将量化图像与边缘混合
        BufferedImage result = blendCartoonEffect(quantized, edges);

        return result;
    }

    /**
     * 检测卡通效果边缘 - 使用自适应阈值
     */
    private BufferedImage detectCartoonEdges(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 转为灰度
        BufferedImage grayImage = applyGrayscale(image);

        // 应用高斯模糊减少噪声
        BufferedImage blurred = applyGaussianBlur(grayImage, 1);

        // 应用边缘检测
        BufferedImage edges = applyEdgeDetection(blurred);

        // 自适应阈值处理 - 只保留较强的边缘
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // 计算边缘强度的平均值
        long totalEdgeStrength = 0;
        int pixelCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = edges.getRGB(x, y);
                int edgeStrength = (pixel >> 16) & 0xFF;
                totalEdgeStrength += edgeStrength;
                pixelCount++;
            }
        }

        int averageEdgeStrength = (int)(totalEdgeStrength / pixelCount);
        // 使用更高的阈值，避免太多像素被标记为边缘
        int threshold = Math.min(100, averageEdgeStrength * 2);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = edges.getRGB(x, y);
                int edgeStrength = (pixel >> 16) & 0xFF;

                // 只有较强的边缘才被保留
                if (edgeStrength > threshold) {
                    // 黑色边缘
                    result.setRGB(x, y, 0xFF000000);
                } else {
                    // 透明（无边缘）
                    result.setRGB(x, y, 0x00000000);
                }
            }
        }

        return result;
    }

    /**
     * 颜色量化
     */
    private BufferedImage quantizeColors(BufferedImage image, int colorLevels) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // 计算量化步长
        int step = 256 / colorLevels;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getRGB(x, y);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                // 量化每个颜色通道
                r = (r / step) * step;
                g = (g / step) * step;
                b = (b / step) * step;

                // 确保在0-255范围内
                r = Math.min(255, r);
                g = Math.min(255, g);
                b = Math.min(255, b);

                // 稍微增加对比度使颜色更鲜艳
                r = enhanceContrast(r);
                g = enhanceContrast(g);
                b = enhanceContrast(b);

                int alpha = (color >> 24) & 0xFF;
                result.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 增强对比度
     */
    private int enhanceContrast(int value) {
        // 简单的对比度增强：将值映射到更宽的动态范围
        float factor = 1.2f;
        int result = (int)((value - 128) * factor + 128);
        return Math.max(0, Math.min(255, result));
    }

    /**
     * 混合卡通效果
     */
    private BufferedImage blendCartoonEffect(BufferedImage colorImage, BufferedImage edgeImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = colorImage.getRGB(x, y);
                int edge = edgeImage.getRGB(x, y);

                // 检查边缘像素（alpha通道不为0表示有边缘）
                int alpha = (edge >> 24) & 0xFF;

                if (alpha > 0) {
                    // 有边缘：使用黑色但稍微透明
                    result.setRGB(x, y, 0x80000000); // 半透明黑色
                } else {
                    // 无边缘：使用量化颜色
                    result.setRGB(x, y, color);
                }
            }
        }

        return result;
    }

    /**
     * 应用马赛克艺术效果
     */
    private BufferedImage applyMosaicArtEffect(BufferedImage image) {
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