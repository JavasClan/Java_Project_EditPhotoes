package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;

/**
 * 简化修复版的优化HSL亮度调整操作类
 */
public class OptimizedHSLBrightnessOperation implements ImageOperation {

    private static final int LUT_SIZE = 64;
    private static final float LUT_STEP = 1.0f / (LUT_SIZE - 1);

    // HSL -> RGB 查找表: [H][S][L] -> 打包的RGB值 (24位)
    private static final int[][][] HSL_TO_RGB_LUT;

    static {
        System.out.println("正在初始化HSL查找表 (大小: " + LUT_SIZE + "x" + LUT_SIZE + "x" + LUT_SIZE + ")");

        long startTime = System.currentTimeMillis();

        HSL_TO_RGB_LUT = new int[LUT_SIZE][LUT_SIZE][LUT_SIZE];
        initHslToRgbLut();

        long endTime = System.currentTimeMillis();
        System.out.printf("HSL查找表初始化完成，耗时: %dms\n", endTime - startTime);
    }

    private final float brightnessDelta;

    public OptimizedHSLBrightnessOperation(float brightnessDelta) {
        this.brightnessDelta = brightnessDelta;
    }

    /**
     * 初始化HSL到RGB查找表
     */
    private static void initHslToRgbLut() {
        for (int hIdx = 0; hIdx < LUT_SIZE; hIdx++) {
            float h = hIdx * LUT_STEP;
            for (int sIdx = 0; sIdx < LUT_SIZE; sIdx++) {
                float s = sIdx * LUT_STEP;
                for (int lIdx = 0; lIdx < LUT_SIZE; lIdx++) {
                    float l = lIdx * LUT_STEP;
                    HSL_TO_RGB_LUT[hIdx][sIdx][lIdx] = hslToRgbInt(h, s, l);
                }
            }
        }
    }

    /**
     * HSL转RGB，返回打包的int值
     */
    private static int hslToRgbInt(float h, float s, float l) {
        float r, g, b;

        if (s == 0) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1.0f + s) : l + s - l * s;
            float p = 2.0f * l - q;

            r = hueToRgb(p, q, h + 1.0f / 3.0f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0f / 3.0f);
        }

        int ri = Math.round(r * 255);
        int gi = Math.round(g * 255);
        int bi = Math.round(b * 255);

        return (ri << 16) | (gi << 8) | bi;
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            BufferedImage result = new BufferedImage(width, height, image.getType());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    int adjusted = adjustPixel(argb);
                    result.setRGB(x, y, adjusted);
                }
            }

            return result;
        } catch (Exception e) {
            throw new ImageProcessingException("HSL亮度调整失败: " + e.getMessage(), e);
        }
    }

    private int adjustPixel(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        // 直接计算HSL（简化版，不依赖RGB->HSL查找表）
        float[] hsl = rgbToHsl(r, g, b);

        // 转换为查找表索引
        int hIdx = (int)(hsl[0] * (LUT_SIZE - 1));
        int sIdx = (int)(hsl[1] * (LUT_SIZE - 1));
        int lIdx = (int)(hsl[2] * (LUT_SIZE - 1));

        // 调整亮度
        float l = lIdx * LUT_STEP;
        float newL = Math.max(0.0f, Math.min(1.0f, l + brightnessDelta));
        int newLIdx = (int)(newL * (LUT_SIZE - 1));
        newLIdx = Math.min(LUT_SIZE - 1, Math.max(0, newLIdx));

        // 从查找表获取RGB
        int rgbInt = HSL_TO_RGB_LUT[hIdx][sIdx][newLIdx];

        return (a << 24) | rgbInt;
    }

    // 省略 rgbToHsl 和 hueToRgb 方法（与上面相同）

    /**
     * RGB转HSL
     */
    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float l = (max + min) / 2.0f;

        float h = 0, s = 0;
        if (max != min) {
            float delta = max - min;
            s = (l > 0.5f) ? delta / (2.0f - max - min) : delta / (max + min);

            if (max == rf) {
                h = (gf - bf) / delta + (gf < bf ? 6.0f : 0.0f);
            } else if (max == gf) {
                h = (bf - rf) / delta + 2.0f;
            } else {
                h = (rf - gf) / delta + 4.0f;
            }
            h /= 6.0f;
        }

        return new float[]{h, s, l};
    }
    /**
     * 辅助函数：色相分量转换
     */
    private static float hueToRgb(float p, float q, float t) {
        if (t < 0.0f) t += 1.0f;
        if (t > 1.0f) t -= 1.0f;

        if (t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
        if (t < 1.0f / 2.0f) return q;
        if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
        return p;
    }


    @Override
    public String getOperationName() {
        int percentage = Math.round(brightnessDelta * 100);
        return String.format("HSL亮度调整 %+d%% (简化优化版)", percentage);
    }
}