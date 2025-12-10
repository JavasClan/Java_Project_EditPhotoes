package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

/**
 * 图片对比度调整操作实现类
 *
 * 功能说明：
 * - 支持对比度的增强和减弱
 * - 调整范围：-100% 到 +200%
 * - 基于线性插值算法实现
 *
 * 算法原理：
 * - 对比度增强：扩大像素值分布范围
 * - 对比度减弱：压缩像素值分布范围
 * - 使用RescaleOp的offset参数实现中心调整
 */
public class ContrastOperation implements ImageOperation {

    private final float contrastLevel; // 对比度等级：1.0为原始对比度

    /**
     * 构造函数
     *
     * @param contrastLevel 对比度等级
     *                      - 1.0: 原始对比度
     *                      - 0.5: 降低50%对比度
     *                      - 2.0: 增加100%对比度
     * @throws IllegalArgumentException 当对比度等级无效时抛出
     */
    public ContrastOperation(float contrastLevel) {
        if (contrastLevel <= 0) {
            throw new IllegalArgumentException("对比度等级必须大于0");
        }
        if (contrastLevel > 5.0f) {
            throw new IllegalArgumentException("对比度等级不能超过5.0");
        }
        this.contrastLevel = contrastLevel;
    }

    /**
     * 创建对比度增强操作
     *
     * @param percentage 增强百分比（0-200）
     * @return 配置好的对比度调整操作
     */
    public static ContrastOperation createEnhance(int percentage) {
        if (percentage < 0 || percentage > 200) {
            throw new IllegalArgumentException("增强百分比必须在0-200之间");
        }
        float level = 1.0f + (percentage / 100.0f);
        return new ContrastOperation(level);
    }

    /**
     * 创建对比度减弱操作
     *
     * @param percentage 减弱百分比（0-100）
     * @return 配置好的对比度调整操作
     */
    public static ContrastOperation createReduce(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("减弱百分比必须在0-100之间");
        }
        float level = 1.0f - (percentage / 100.0f);
        return new ContrastOperation(level);
    }

    /**
     * 应用对比度调整操作
     *
     * 算法实现：
     * - 使用公式：output = (input - 128) * contrastLevel + 128
     * - 通过RescaleOp的scale和offset参数实现
     * - 128是中灰色值，作为调整中心点
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            // 计算偏移量，使调整围绕中灰色(128)进行
            float offset = 128 * (1 - contrastLevel);

            // 创建RescaleOp对象
            RescaleOp rescaleOp = new RescaleOp(contrastLevel, offset, null);

            // 应用对比度调整
            BufferedImage resultImage = rescaleOp.filter(image, null);

            // 限制像素值在0-255范围内（RescaleOp可能产生超出范围的像素）
            clampPixelValues(resultImage);

            return resultImage;

        } catch (Exception e) {
            throw new ImageProcessingException("对比度调整失败: " + e.getMessage(), e);
        }
    }

    /**
     * 限制像素值在有效范围内
     *
     * @param image 需要处理的图片
     */
    private void clampPixelValues(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                // 提取ARGB通道
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 限制RGB值在0-255范围内
                red = Math.max(0, Math.min(255, red));
                green = Math.max(0, Math.min(255, green));
                blue = Math.max(0, Math.min(255, blue));

                // 重新组合像素
                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, newPixel);
            }
        }
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        if (contrastLevel > 1.0f) {
            int percentage = (int)((contrastLevel - 1.0f) * 100);
            return String.format("对比度增强 %d%%", percentage);
        } else if (contrastLevel < 1.0f) {
            int percentage = (int)((1.0f - contrastLevel) * 100);
            return String.format("对比度减弱 %d%%", percentage);
        } else {
            return "对比度调整";
        }
    }

    // ========== Getter方法 ==========

    public float getContrastLevel() {
        return contrastLevel;
    }
}