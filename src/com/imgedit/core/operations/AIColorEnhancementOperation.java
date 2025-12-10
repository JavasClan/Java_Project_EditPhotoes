package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * AI辅助色彩增强操作实现类
 *
 * 功能说明：
 * - 基于统计分析和AI启发式算法智能调整色彩
 * - 自动识别并修正白平衡
 * - 智能增强颜色饱和度和对比度
 * - 自适应曝光校正
 *
 * 算法原理：
 * - 直方图均衡化和匹配
 * - 自动白平衡（灰度世界算法）
 * - 自适应对比度增强（CLAHE变种）
 * - 肤色保护和记忆色增强
 */
public class AIColorEnhancementOperation implements ImageOperation {

    /**
     * 增强模式枚举
     */
    public enum EnhancementMode {
        AUTO("全自动增强"),
        PORTRAIT("人像增强"),
        LANDSCAPE("风景增强"),
        LOW_LIGHT("低光增强");

        private final String description;

        EnhancementMode(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    private final EnhancementMode mode;
    private final float enhancementStrength; // 增强强度 (0.0-1.0)

    /**
     * 构造函数
     */
    public AIColorEnhancementOperation(EnhancementMode mode, float enhancementStrength) {
        this.mode = mode;
        this.enhancementStrength = Math.max(0.0f, Math.min(1.0f, enhancementStrength));
    }

    /**
     * 创建全自动增强
     */
    public static AIColorEnhancementOperation createAutoEnhancement() {
        return new AIColorEnhancementOperation(EnhancementMode.AUTO, 0.5f);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            // 分析图像特征
            ImageAnalysis analysis = analyzeImage(image);

            // 根据模式和图像特征应用增强
            BufferedImage enhanced = image;

            // 步骤1：自动白平衡
            if (analysis.needsWhiteBalance) {
                enhanced = applyWhiteBalance(enhanced, analysis);
            }

            // 步骤2：对比度增强
            if (analysis.needsContrastEnhancement) {
                enhanced = applyContrastEnhancement(enhanced, analysis);
            }

            // 步骤3：饱和度调整
            enhanced = applySaturationAdjustment(enhanced, analysis);

            // 步骤4：曝光校正
            if (Math.abs(analysis.exposureBias) > 0.1) {
                enhanced = applyExposureCorrection(enhanced, analysis);
            }

            // 步骤5：模式特定的增强
            enhanced = applyModeSpecificEnhancement(enhanced, analysis);

            // 步骤6：细节增强
            enhanced = applyDetailEnhancement(enhanced);

            return enhanced;

        } catch (Exception e) {
            throw new ImageProcessingException("AI色彩增强失败: " + e.getMessage(), e);
        }
    }

    /**
     * 图像分析结果
     */
    private static class ImageAnalysis {
        float averageBrightness;     // 平均亮度
        float contrastRatio;         // 对比度比例
        float colorVariance;         // 颜色方差
        float exposureBias;          // 曝光偏差 (-1.0 到 1.0)
        boolean needsWhiteBalance;   // 是否需要白平衡
        boolean needsContrastEnhancement; // 是否需要对比度增强
        float skinTonePercentage;    // 肤色区域百分比
        boolean isLowLight;          // 是否为低光环境

        // 颜色通道统计
        float avgRed, avgGreen, avgBlue;
        float maxRed, maxGreen, maxBlue;
        float minRed, minGreen, minBlue;
    }

    /**
     * 分析图像特征
     */
    private ImageAnalysis analyzeImage(BufferedImage image) {
        ImageAnalysis analysis = new ImageAnalysis();
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        // 初始化统计变量
        float sumRed = 0, sumGreen = 0, sumBlue = 0;
        float sumRedSq = 0, sumGreenSq = 0, sumBlueSq = 0;
        float sumLuminance = 0;
        int skinToneCount = 0;

        analysis.maxRed = analysis.maxGreen = analysis.maxBlue = 0;
        analysis.minRed = analysis.minGreen = analysis.minBlue = 255;

        // 遍历所有像素进行统计
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 更新统计
                sumRed += red;
                sumGreen += green;
                sumBlue += blue;

                sumRedSq += red * red;
                sumGreenSq += green * green;
                sumBlueSq += blue * blue;

                // 更新最大值和最小值
                analysis.maxRed = Math.max(analysis.maxRed, red);
                analysis.maxGreen = Math.max(analysis.maxGreen, green);
                analysis.maxBlue = Math.max(analysis.maxBlue, blue);

                analysis.minRed = Math.min(analysis.minRed, red);
                analysis.minGreen = Math.min(analysis.minGreen, green);
                analysis.minBlue = Math.min(analysis.minBlue, blue);

                // 计算亮度
                float luminance = calculateLuminance(red, green, blue);
                sumLuminance += luminance;

                // 检测肤色
                if (isSkinTone(red, green, blue)) {
                    skinToneCount++;
                }
            }
        }

        // 计算平均值
        analysis.avgRed = sumRed / totalPixels;
        analysis.avgGreen = sumGreen / totalPixels;
        analysis.avgBlue = sumBlue / totalPixels;
        analysis.averageBrightness = sumLuminance / totalPixels;

        // 计算方差
        analysis.colorVariance = (
                (sumRedSq / totalPixels - analysis.avgRed * analysis.avgRed) +
                        (sumGreenSq / totalPixels - analysis.avgGreen * analysis.avgGreen) +
                        (sumBlueSq / totalPixels - analysis.avgBlue * analysis.avgBlue)
        ) / 3;

        // 计算对比度
        analysis.contrastRatio = (analysis.maxRed - analysis.minRed +
                analysis.maxGreen - analysis.minGreen +
                analysis.maxBlue - analysis.minBlue) / (3.0f * 255);

        // 计算曝光偏差（以0.5为中性）
        analysis.exposureBias = analysis.averageBrightness - 0.5f;

        // 判断是否为低光环境
        analysis.isLowLight = analysis.averageBrightness < 0.3f;

        // 计算肤色百分比
        analysis.skinTonePercentage = (float) skinToneCount / totalPixels;

        // 判断是否需要白平衡（基于灰度世界假设）
        float maxAvg = Math.max(analysis.avgRed, Math.max(analysis.avgGreen, analysis.avgBlue));
        float minAvg = Math.min(analysis.avgRed, Math.min(analysis.avgGreen, analysis.avgBlue));
        analysis.needsWhiteBalance = (maxAvg - minAvg) / maxAvg > 0.2f;

        // 判断是否需要对比度增强
        analysis.needsContrastEnhancement = analysis.contrastRatio < 0.3f;

        return analysis;
    }

    /**
     * 计算亮度
     */
    private float calculateLuminance(int red, int green, int blue) {
        return (red * 0.299f + green * 0.587f + blue * 0.114f) / 255.0f;
    }

    /**
     * 检测是否为肤色
     */
    private boolean isSkinTone(int red, int green, int blue) {
        // 简单的肤色检测规则
        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;

        // 转换到YCbCr色彩空间进行肤色检测
        float y = 0.299f * r + 0.587f * g + 0.114f * b;
        float cb = -0.1687f * r - 0.3313f * g + 0.5f * b + 0.5f;
        float cr = 0.5f * r - 0.4187f * g - 0.0813f * b + 0.5f;

        // 肤色在YCbCr空间中的典型范围
        return (y > 0.3 && y < 0.9) &&
                (cb > 0.4 && cb < 0.6) &&
                (cr > 0.4 && cr < 0.6);
    }

    /**
     * 应用白平衡
     */
    private BufferedImage applyWhiteBalance(BufferedImage image, ImageAnalysis analysis) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage balanced = new BufferedImage(width, height, image.getType());

        // 灰度世界算法：假设整幅图像的颜色平均值应该是灰色
        float avgGray = (analysis.avgRed + analysis.avgGreen + analysis.avgBlue) / 3.0f;

        // 计算每个通道的增益
        float gainRed = avgGray / analysis.avgRed;
        float gainGreen = avgGray / analysis.avgGreen;
        float gainBlue = avgGray / analysis.avgBlue;

        // 限制增益范围，避免过度调整
        gainRed = Math.max(0.5f, Math.min(2.0f, gainRed));
        gainGreen = Math.max(0.5f, Math.min(2.0f, gainGreen));
        gainBlue = Math.max(0.5f, Math.min(2.0f, gainBlue));

        // 应用白平衡
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 应用增益
                int newRed = clamp((int)(red * gainRed));
                int newGreen = clamp((int)(green * gainGreen));
                int newBlue = clamp((int)(blue * gainBlue));

                int newPixel = (alpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
                balanced.setRGB(x, y, newPixel);
            }
        }

        return balanced;
    }

    /**
     * 应用对比度增强
     */
    private BufferedImage applyContrastEnhancement(BufferedImage image, ImageAnalysis analysis) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage enhanced = new BufferedImage(width, height, image.getType());

        // 自适应对比度增强
        float contrastFactor = 1.0f + (0.5f - analysis.contrastRatio) * enhancementStrength;

        // 计算直方图
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int luminance = (int)(calculateLuminance(
                        (pixel >> 16) & 0xFF,
                        (pixel >> 8) & 0xFF,
                        pixel & 0xFF
                ) * 255);
                histogram[luminance]++;
            }
        }

        // 计算累积分布函数
        int totalPixels = width * height;
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }

        // 计算查找表
        int[] lookupTable = new int[256];
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] > 0) {
                cdfMin = cdf[i];
                break;
            }
        }

        for (int i = 0; i < 256; i++) {
            lookupTable[i] = clamp((int)(((cdf[i] - cdfMin) / (float)(totalPixels - cdfMin)) * 255));
        }

        // 应用对比度增强
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 计算亮度
                int luminance = (int)(calculateLuminance(red, green, blue) * 255);

                // 获取增强后的亮度
                int enhancedLuminance = lookupTable[luminance];

                // 保持色度，调整亮度
                float ratio = enhancedLuminance / (float) Math.max(luminance, 1);

                int newRed = clamp((int)(red * ratio));
                int newGreen = clamp((int)(green * ratio));
                int newBlue = clamp((int)(blue * ratio));

                int newPixel = (alpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
                enhanced.setRGB(x, y, newPixel);
            }
        }

        return enhanced;
    }

    /**
     * 应用饱和度调整
     */
    private BufferedImage applySaturationAdjustment(BufferedImage image, ImageAnalysis analysis) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage saturated = new BufferedImage(width, height, image.getType());

        // 根据图像特征计算饱和度调整量
        float saturationBoost;

        if (mode == EnhancementMode.PORTRAIT) {
            // 人像模式：轻微降低饱和度，使肤色更自然
            saturationBoost = 0.8f + 0.4f * enhancementStrength;
        } else if (mode == EnhancementMode.LANDSCAPE) {
            // 风景模式：增强饱和度
            saturationBoost = 1.0f + 0.8f * enhancementStrength;
        } else if (analysis.isLowLight) {
            // 低光环境：轻微降低饱和度，减少噪点
            saturationBoost = 0.7f + 0.3f * enhancementStrength;
        } else {
            // 自动模式：根据颜色方差决定
            saturationBoost = 1.0f + (0.3f - analysis.colorVariance) * enhancementStrength * 3;
        }

        // 限制范围
        saturationBoost = Math.max(0.5f, Math.min(2.0f, saturationBoost));

        // 应用饱和度调整
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int enhancedPixel = adjustSaturation(pixel, saturationBoost);
                saturated.setRGB(x, y, enhancedPixel);
            }
        }

        return saturated;
    }

    /**
     * 调整单个像素的饱和度
     */
    private int adjustSaturation(int pixel, float saturationFactor) {
        int alpha = (pixel >> 24) & 0xFF;
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;

        // 转换到HSL色彩空间
        float[] hsl = rgbToHsl(red, green, blue);

        // 调整饱和度
        hsl[1] = Math.max(0.0f, Math.min(1.0f, hsl[1] * saturationFactor));

        // 转换回RGB
        int[] rgb = hslToRgb(hsl[0], hsl[1], hsl[2]);

        return (alpha << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /**
     * RGB转HSL
     */
    private float[] rgbToHsl(int r, int g, int b) {
        float red = r / 255.0f;
        float green = g / 255.0f;
        float blue = b / 255.0f;

        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;

        float hue, saturation, lightness;

        lightness = (max + min) / 2.0f;

        if (delta == 0) {
            hue = 0;
            saturation = 0;
        } else {
            saturation = delta / (1 - Math.abs(2 * lightness - 1));

            if (max == red) {
                hue = (green - blue) / delta + (green < blue ? 6 : 0);
            } else if (max == green) {
                hue = (blue - red) / delta + 2;
            } else {
                hue = (red - green) / delta + 4;
            }

            hue /= 6.0f;
        }

        return new float[]{hue, saturation, lightness};
    }

    /**
     * HSL转RGB
     */
    private int[] hslToRgb(float h, float s, float l) {
        float r, g, b;

        if (s == 0) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;

            r = hueToRgb(p, q, h + 1.0f/3.0f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0f/3.0f);
        }

        return new int[]{
                clamp((int)(r * 255)),
                clamp((int)(g * 255)),
                clamp((int)(b * 255))
        };
    }

    private float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;

        if (t < 1.0f/6.0f) return p + (q - p) * 6 * t;
        if (t < 1.0f/2.0f) return q;
        if (t < 2.0f/3.0f) return p + (q - p) * (2.0f/3.0f - t) * 6;

        return p;
    }

    /**
     * 应用曝光校正
     */
    private BufferedImage applyExposureCorrection(BufferedImage image, ImageAnalysis analysis) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage corrected = new BufferedImage(width, height, image.getType());

        // 根据曝光偏差计算调整量
        float exposureCorrection = -analysis.exposureBias * enhancementStrength;

        // 限制调整范围
        exposureCorrection = Math.max(-0.5f, Math.min(0.5f, exposureCorrection));

        // 应用曝光校正
        float factor = 1.0f + exposureCorrection;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 使用gamma校正调整曝光
                float gamma = exposureCorrection > 0 ?
                        1.0f / (1.0f + exposureCorrection * 2) :
                        1.0f - exposureCorrection * 2;

                int newRed = clamp((int)(255 * Math.pow(red / 255.0f, gamma)));
                int newGreen = clamp((int)(255 * Math.pow(green / 255.0f, gamma)));
                int newBlue = clamp((int)(255 * Math.pow(blue / 255.0f, gamma)));

                int newPixel = (alpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
                corrected.setRGB(x, y, newPixel);
            }
        }

        return corrected;
    }

    /**
     * 应用模式特定的增强
     */
    private BufferedImage applyModeSpecificEnhancement(BufferedImage image, ImageAnalysis analysis) {
        switch (mode) {
            case PORTRAIT:
                return applyPortraitEnhancement(image, analysis);
            case LANDSCAPE:
                return applyLandscapeEnhancement(image, analysis);
            case LOW_LIGHT:
                return applyLowLightEnhancement(image, analysis);
            default:
                return image;
        }
    }

    /**
     * 应用人像增强
     */
    private BufferedImage applyPortraitEnhancement(BufferedImage image, ImageAnalysis analysis) {
        // 人像增强：肤色优化、背景虚化模拟等
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage enhanced = new BufferedImage(width, height, image.getType());

        // 复制原始图像
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                enhanced.setRGB(x, y, image.getRGB(x, y));
            }
        }

        // 如果有明显的肤色区域，应用肤色优化
        if (analysis.skinTonePercentage > 0.1f) {
            enhanced = optimizeSkinTones(enhanced, analysis);
        }

        return enhanced;
    }

    /**
     * 优化肤色
     */
    private BufferedImage optimizeSkinTones(BufferedImage image, ImageAnalysis analysis) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage optimized = new BufferedImage(width, height, image.getType());

        // 目标肤色范围（更健康的肤色）
        float targetHue = 0.05f; // 轻微偏黄
        float targetSaturation = 0.3f;
        float targetLightness = 0.7f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 检测是否为肤色
                if (isSkinTone(red, green, blue)) {
                    // 转换到HSL
                    float[] hsl = rgbToHsl(red, green, blue);

                    // 向目标肤色调整
                    float hue = hsl[0];
                    float saturation = hsl[1];
                    float lightness = hsl[2];

                    // 混合当前肤色和目标肤色
                    float blendFactor = 0.3f * enhancementStrength;
                    hue = hue * (1 - blendFactor) + targetHue * blendFactor;
                    saturation = saturation * (1 - blendFactor) + targetSaturation * blendFactor;
                    lightness = lightness * (1 - blendFactor) + targetLightness * blendFactor;

                    // 转换回RGB
                    int[] rgb = hslToRgb(hue, saturation, lightness);

                    pixel = (pixel & 0xFF000000) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
                }

                optimized.setRGB(x, y, pixel);
            }
        }

        return optimized;
    }

    /**
     * 应用风景增强
     */
    private BufferedImage applyLandscapeEnhancement(BufferedImage image, ImageAnalysis analysis) {
        // 风景增强：增强天空蓝色、草地绿色等
        return image; // 简化实现
    }

    /**
     * 应用低光增强
     */
    private BufferedImage applyLowLightEnhancement(BufferedImage image, ImageAnalysis analysis) {
        // 低光增强：降噪、提亮阴影、减少噪点
        return image; // 简化实现
    }

    /**
     * 应用细节增强
     */
    private BufferedImage applyDetailEnhancement(BufferedImage image) {
        // 细节增强：使用USM锐化等
        return image; // 简化实现
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public String getOperationName() {
        return String.format("AI色彩增强 [模式:%s, 强度:%.1f]",
                mode.getDescription(), enhancementStrength);
    }
}