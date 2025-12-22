package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;

public class SaturationOperation implements ImageOperation {
    private final float saturationFactor; // 饱和度因子：1.0为原始饱和度

    public SaturationOperation(float saturationFactor) {
        if (saturationFactor < 0) {
            throw new IllegalArgumentException("饱和度因子不能小于0");
        }
        this.saturationFactor = saturationFactor;
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // 提取ARGB通道
                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // RGB转HSL（简化算法）
                float[] hsl = rgbToHsl(red, green, blue);

                // 调整饱和度
                hsl[1] = Math.max(0, Math.min(1.0f, hsl[1] * saturationFactor));

                // HSL转回RGB
                int[] newRgb = hslToRgb(hsl[0], hsl[1], hsl[2]);

                // 重新组合像素
                int newPixel = (alpha << 24) | (newRgb[0] << 16) | (newRgb[1] << 8) | newRgb[2];
                result.setRGB(x, y, newPixel);
            }
        }

        return result;
    }

    private float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0, s = 0, l = (max + min) / 2;

        if (delta > 0) {
            s = delta / (1 - Math.abs(2 * l - 1));

            if (max == rf) {
                h = ((gf - bf) / delta) % 6;
            } else if (max == gf) {
                h = (bf - rf) / delta + 2;
            } else {
                h = (rf - gf) / delta + 4;
            }

            h = (h * 60) % 360;
            if (h < 0) h += 360;
        }

        return new float[]{h, s, l};
    }

    private int[] hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l - c / 2;

        float r = 0, g = 0, b = 0;

        if (0 <= h && h < 60) {
            r = c; g = x; b = 0;
        } else if (60 <= h && h < 120) {
            r = x; g = c; b = 0;
        } else if (120 <= h && h < 180) {
            r = 0; g = c; b = x;
        } else if (180 <= h && h < 240) {
            r = 0; g = x; b = c;
        } else if (240 <= h && h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }

        return new int[]{
                Math.round((r + m) * 255),
                Math.round((g + m) * 255),
                Math.round((b + m) * 255)
        };
    }

    @Override
    public String getOperationName() {
        int percentage = (int)((saturationFactor - 1.0f) * 100);
        if (percentage > 0) {
            return String.format("饱和度增加 %d%%", percentage);
        } else if (percentage < 0) {
            return String.format("饱和度减少 %d%%", -percentage);
        } else {
            return "饱和度调整";
        }
    }

    public float getSaturationFactor() {
        return saturationFactor;
    }
}