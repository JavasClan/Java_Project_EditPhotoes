package imgedit.core.operations;

import imgedit.core.ImageOperation;

import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;

/**
 * 自适应HSL操作 - 根据图像大小自动选择最佳实现
 */
public class AdaptiveHSLBrightnessOperation implements ImageOperation {

    private final float brightnessDelta;

    public AdaptiveHSLBrightnessOperation(float brightnessDelta) {
        this.brightnessDelta = brightnessDelta;
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelCount = width * height;

        // 根据图像大小选择最佳实现
        if (pixelCount < 10000) {
            // 小图像：使用基础实现，避免查找表开销
            try {
                return new HSLBrightnessOperation(
                        HSLBrightnessOperation.AdjustmentMode.RELATIVE,
                        brightnessDelta
                ).apply(image);
            } catch (ImageProcessingException e) {
                throw new RuntimeException(e);
            }

        } else if (pixelCount < 1000000) {
            // 中等图像：使用优化的查找表实现
            try {
                return new OptimizedHSLBrightnessOperation(brightnessDelta).apply(image);
            } catch (ImageProcessingException e) {
                throw new RuntimeException(e);
            }

        } else {
            // 大图像：使用最大并行化
            try {
                return new OptimizedHSLBrightnessOperation(
                        brightnessDelta
                ).apply(image);
            } catch (ImageProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getOperationName() {
        int percentage = Math.round(brightnessDelta * 100);
        return String.format("HSL亮度调整 %+d%% (自适应)", percentage);
    }
}