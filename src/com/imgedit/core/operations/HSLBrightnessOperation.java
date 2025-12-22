package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;

/**
 * HSL色彩空间亮度调整操作
 *
 * 优势：
 * 1. 只调整亮度分量(L)，不影响色相(H)和饱和度(S)
 * 2. 避免RGB空间调整导致的色彩失真
 * 3. 支持更大范围的亮度调整而不会丢失信息
 * 4. 符合人眼感知，调整更自然
 */
public class HSLBrightnessOperation implements ImageOperation {

    public enum AdjustmentMode {
        RELATIVE("相对调整"),   // 基于当前亮度的增减
        ABSOLUTE("绝对调整");   // 设置到目标亮度值

        private final String description;

        AdjustmentMode(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    private final AdjustmentMode mode;
    private final float adjustmentValue;  // 范围: [-1.0f, 1.0f] 或 [0.0f, 1.0f]

    /**
     * 构造函数
     *
     * @param mode 调整模式
     * @param adjustmentValue 调整值:
     *                       - RELATIVE模式: [-1.0f, 1.0f] 负值变暗，正值变亮
     *                       - ABSOLUTE模式: [0.0f, 1.0f] 目标亮度值
     */
    public HSLBrightnessOperation(AdjustmentMode mode, float adjustmentValue) {
        this.mode = mode;
        this.adjustmentValue = adjustmentValue;

        switch (mode) {
            case RELATIVE:
                if (adjustmentValue < -1.0f || adjustmentValue > 1.0f) {
                    throw new IllegalArgumentException("相对调整值必须在-1.0到1.0之间");
                }
                break;
            case ABSOLUTE:
                if (adjustmentValue < 0.0f || adjustmentValue > 1.0f) {
                    throw new IllegalArgumentException("绝对亮度值必须在0.0到1.0之间");
                }
                break;
        }
    }

    /**
     * 便捷方法：相对亮度调整
     *
     * @param delta 亮度变化量 (-1.0f 到 1.0f)
     * @return 相对调整操作对象
     */
    public static HSLBrightnessOperation createRelativeAdjustment(float delta) {
        return new HSLBrightnessOperation(AdjustmentMode.RELATIVE, delta);
    }

    /**
     * 便捷方法：设置绝对亮度
     *
     * @param brightness 目标亮度值 (0.0f 到 1.0f)
     * @return 绝对调整操作对象
     */
    public static HSLBrightnessOperation createAbsoluteAdjustment(float brightness) {
        return new HSLBrightnessOperation(AdjustmentMode.ABSOLUTE, brightness);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // 创建结果图像，保持原图像类型
            BufferedImage result = new BufferedImage(
                    width, height, image.getType());

            // 处理每个像素
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);

                    // 提取Alpha通道
                    int alpha = (rgb >> 24) & 0xFF;

                    // 提取RGB分量 (0-255)
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // 将RGB转换为HSL
                    float[] hsl = rgbToHsl(r, g, b);

                    // 调整亮度分量(L)
                    float newL = adjustBrightness(hsl[2]);

                    // 将HSL转换回RGB
                    int[] newRgb = hslToRgb(hsl[0], hsl[1], newL);

                    // 组合新像素值
                    int newPixel = (alpha << 24) | (newRgb[0] << 16) | (newRgb[1] << 8) | newRgb[2];
                    result.setRGB(x, y, newPixel);
                }
            }

            return result;

        } catch (Exception e) {
            throw new ImageProcessingException("HSL亮度调整失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调整亮度分量
     *
     * @param currentL 当前亮度值 (0.0f - 1.0f)
     * @return 调整后的亮度值
     */
    private float adjustBrightness(float currentL) {
        switch (mode) {
            case RELATIVE:
                // 相对调整：在现有亮度基础上增减
                float newL = currentL + adjustmentValue;
                return clamp(newL, 0.0f, 1.0f);

            case ABSOLUTE:
                // 绝对调整：直接设置到目标亮度
                // 可以添加平滑过渡，避免突变
                return adjustmentValue;

            default:
                return currentL;
        }
    }

    /**
     * RGB转HSL转换
     *
     * @param r 红色分量 0-255
     * @param g 绿色分量 0-255
     * @param b 蓝色分量 0-255
     * @return HSL数组 [H, S, L]，范围 [0, 1]
     */
    private float[] rgbToHsl(int r, int g, int b) {
        // 将RGB归一化到 [0, 1]
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));

        // 计算亮度(Lightness)
        float l = (max + min) / 2.0f;

        float h = 0, s = 0;

        if (max != min) {
            // 计算饱和度(Saturation)
            float delta = max - min;
            s = (l > 0.5f) ? delta / (2.0f - max - min) : delta / (max + min);

            // 计算色相(Hue)
            if (max == rf) {
                h = (gf - bf) / delta + (gf < bf ? 6.0f : 0.0f);
            } else if (max == gf) {
                h = (bf - rf) / delta + 2.0f;
            } else {
                h = (rf - gf) / delta + 4.0f;
            }
            h /= 6.0f;
        }

        return new float[] {h, s, l};
    }

    /**
     * HSL转RGB转换
     *
     * @param h 色相 0-1
     * @param s 饱和度 0-1
     * @param l 亮度 0-1
     * @return RGB数组 [R, G, B]，范围 0-255
     */
    private int[] hslToRgb(float h, float s, float l) {
        float r, g, b;

        if (s == 0) {
            // 无饱和度，是灰度
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1.0f + s) : l + s - l * s;
            float p = 2.0f * l - q;

            // 转换色相到RGB
            r = hueToRgb(p, q, h + 1.0f / 3.0f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0f / 3.0f);
        }

        return new int[] {
                Math.round(r * 255),
                Math.round(g * 255),
                Math.round(b * 255)
        };
    }

    /**
     * 辅助函数：将色相分量转换为RGB
     */
    private float hueToRgb(float p, float q, float t) {
        if (t < 0.0f) t += 1.0f;
        if (t > 1.0f) t -= 1.0f;

        if (t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
        if (t < 1.0f / 2.0f) return q;
        if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
        return p;
    }

    /**
     * 值钳位
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String getOperationName() {
        switch (mode) {
            case RELATIVE:
                int percentage = Math.round(adjustmentValue * 100);
                return String.format("HSL亮度调整 %+d%%", percentage);
            case ABSOLUTE:
                return String.format("HSL亮度设置 %.0f%%", adjustmentValue * 100);
            default:
                return "HSL亮度调整";
        }
    }

    // Getter方法
    public AdjustmentMode getMode() { return mode; }
    public float getAdjustmentValue() { return adjustmentValue; }
}