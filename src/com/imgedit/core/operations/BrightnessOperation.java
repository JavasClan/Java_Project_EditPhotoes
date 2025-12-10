package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

/**
 * 图片亮度调整操作实现类
 *
 * 功能说明：
 * - 支持亮度的增加和减少
 * - 亮度调整范围：-100% 到 +100%
 * - 保持图片的透明度信息
 *
 * 算法说明：
 * - 使用RescaleOp进行高性能像素处理
 * - 通过调整RGB三个通道实现亮度变化
 * - 支持Alpha通道保持原样
 */
public class BrightnessOperation implements ImageOperation {

    /**
     * 亮度调整模式
     */
    public enum BrightnessMode {
        INCREASE("增加亮度"),
        DECREASE("降低亮度");

        private final String description;

        BrightnessMode(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    private final BrightnessMode mode;
    private final float intensity; // 调整强度：0.0f - 1.0f

    /**
     * 构造函数
     *
     * @param mode 亮度调整模式（增加/降低）
     * @param intensity 调整强度（0.0f - 1.0f）
     * @throws IllegalArgumentException 当强度参数无效时抛出
     */
    public BrightnessOperation(BrightnessMode mode, float intensity) {
        if (intensity < 0.0f || intensity > 1.0f) {
            throw new IllegalArgumentException("亮度调整强度必须在0.0到1.0之间");
        }
        this.mode = mode;
        this.intensity = intensity;
    }

    /**
     * 便捷工厂方法 - 增加亮度
     *
     * @param intensity 亮度增加强度（0.0f - 1.0f）
     * @return 配置好的亮度调整操作对象
     */
    public static BrightnessOperation createIncrease(float intensity) {
        return new BrightnessOperation(BrightnessMode.INCREASE, intensity);
    }

    /**
     * 便捷工厂方法 - 降低亮度
     *
     * @param intensity 亮度降低强度（0.0f - 1.0f）
     * @return 配置好的亮度调整操作对象
     */
    public static BrightnessOperation createDecrease(float intensity) {
        return new BrightnessOperation(BrightnessMode.DECREASE, intensity);
    }

    /**
     * 应用亮度调整操作
     *
     * 实现原理：
     * - 使用RescaleOp进行批量像素处理，性能优于逐像素操作
     * - 亮度增加：所有RGB值乘以(1 + intensity)
     * - 亮度降低：所有RGB值乘以(1 - intensity)
     * - Alpha通道保持不变
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            // 计算缩放因子
            float scaleFactor;
            switch (mode) {
                case INCREASE:
                    scaleFactor = 1.0f + intensity;
                    break;
                case DECREASE:
                    scaleFactor = 1.0f - intensity;
                    break;
                default:
                    throw new ImageProcessingException("不支持的亮度调整模式: " + mode);
            }

            // 创建RescaleOp对象进行亮度调整
            // 参数说明：scaleFactor - RGB通道缩放因子, offset - 偏移量, hints - 渲染提示
            RescaleOp rescaleOp = new RescaleOp(scaleFactor, 0, null);

            // 执行亮度调整操作
            BufferedImage resultImage = rescaleOp.filter(image, null);

            // 如果原始图像有透明度，需要恢复Alpha通道
            if (image.getColorModel().hasAlpha()) {
                restoreAlphaChannel(image, resultImage);
            }

            return resultImage;

        } catch (Exception e) {
            throw new ImageProcessingException("亮度调整失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复Alpha通道信息
     *
     * @param source 源图像（包含原始Alpha通道）
     * @param target 目标图像（亮度调整后）
     *
     * 说明：
     * - RescaleOp会修改所有通道，包括Alpha
     * - 此方法将目标图像的Alpha通道恢复为源图像的值
     */
    private void restoreAlphaChannel(BufferedImage source, BufferedImage target) {
        int width = source.getWidth();
        int height = source.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourcePixel = source.getRGB(x, y);
                int targetPixel = target.getRGB(x, y);

                // 提取源图像的Alpha通道
                int alpha = (sourcePixel >> 24) & 0xFF;

                // 提取目标图像的RGB通道
                int red = (targetPixel >> 16) & 0xFF;
                int green = (targetPixel >> 8) & 0xFF;
                int blue = targetPixel & 0xFF;

                // 组合新的像素值（Alpha来自源图像，RGB来自目标图像）
                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                target.setRGB(x, y, newPixel);
            }
        }
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        String direction = mode == BrightnessMode.INCREASE ? "增加" : "降低";
        int percentage = (int)(intensity * 100);
        return String.format("亮度%s %d%%", direction, percentage);
    }

    // ========== Getter方法 ==========

    public BrightnessMode getMode() {
        return mode;
    }

    public float getIntensity() {
        return intensity;
    }
}