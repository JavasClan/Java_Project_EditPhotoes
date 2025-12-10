package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * 图片裁剪操作实现类
 *
 * 功能说明：
 * - 支持矩形区域裁剪
 * - 自动处理边界情况（区域超出图片范围）
 * - 保持图片原始质量
 *
 * 协作说明：
 * - UI层通过鼠标选择区域后调用此操作
 * - 工具层可提供区域验证工具方法
 */
public class CropOperation implements ImageOperation {

    private final Rectangle cropArea;

    /**
     * 构造函数
     *
     * @param cropArea 裁剪区域，包含x,y坐标和宽高
     * @throws IllegalArgumentException 当区域无效时抛出
     *
     * UI层调用说明：
     * - 通常通过鼠标拖拽获取裁剪区域
     * - 坐标从图片左上角开始计算
     */
    public CropOperation(Rectangle cropArea) {
        validateCropArea(cropArea);
        this.cropArea = new Rectangle(cropArea); // 防御性拷贝
    }

    /**
     * 便捷构造函数 - UI层推荐使用
     *
     * @param x 区域左上角x坐标
     * @param y 区域左上角y坐标
     * @param width 区域宽度
     * @param height 区域高度
     */
    public CropOperation(int x, int y, int width, int height) {
        this(new Rectangle(x, y, width, height));
    }

    /**
     * 应用裁剪操作
     *
     * 实现说明：
     * - 自动处理区域超出图片边界的情况
     * - 使用Graphics2D的drawImage进行高质量裁剪
     * - 返回的新图片大小等于有效裁剪区域
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            // 获取在图片范围内的有效裁剪区域
            Rectangle validArea = getValidCropArea(image, cropArea);

            // 创建目标图片
            BufferedImage croppedImage = new BufferedImage(
                    validArea.width, validArea.height, image.getType());

            // 使用Graphics2D进行图片裁剪
            Graphics2D g2d = croppedImage.createGraphics();

            // 从原图指定区域绘制到新图片
            g2d.drawImage(image,
                    0, 0, validArea.width, validArea.height,           // 目标区域：整个新图片
                    validArea.x, validArea.y,                          // 源区域起点
                    validArea.x + validArea.width, validArea.y + validArea.height, // 源区域终点
                    null);

            g2d.dispose(); // 重要：释放资源
            return croppedImage;

        } catch (Exception e) {
            throw new ImageProcessingException("裁剪操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取操作名称 - 用于历史记录显示
     */
    @Override
    public String getOperationName() {
        return String.format("裁剪 [x=%d, y=%d, w=%d, h=%d]",
                cropArea.x, cropArea.y, cropArea.width, cropArea.height);
    }

    // ========== 参数验证方法 ==========

    /**
     * 验证裁剪区域有效性
     *
     * @param area 待验证的区域
     * @throws IllegalArgumentException 当区域无效时抛出
     *
     * 验证规则：
     * - 宽度和高度必须为正数
     * - 坐标不能为负数（但允许部分超出图片范围，会在apply中自动修正）
     */
    private void validateCropArea(Rectangle area) {
        if (area.width <= 0 || area.height <= 0) {
            throw new IllegalArgumentException(
                    String.format("裁剪区域尺寸无效: 宽=%d, 高=%d", area.width, area.height));
        }
    }

    /**
     * 获取有效的裁剪区域（处理边界情况）
     *
     * @param image 原图片
     * @param desiredArea 期望的裁剪区域
     * @return 调整后确保在图片范围内的有效区域
     *
     * 边界处理逻辑：
     * - 如果区域部分超出图片，自动调整到图片边界内
     * - 保证返回的区域完全在图片范围内
     */
    private Rectangle getValidCropArea(BufferedImage image, Rectangle desiredArea) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // 调整起点坐标，确保在图片范围内
        int x = Math.max(0, Math.min(desiredArea.x, imageWidth - 1));
        int y = Math.max(0, Math.min(desiredArea.y, imageHeight - 1));

        // 调整宽高，确保不超出图片边界
        int width = Math.min(desiredArea.width, imageWidth - x);
        int height = Math.min(desiredArea.height, imageHeight - y);

        return new Rectangle(x, y, width, height);
    }

    // ========== Getter方法 ==========
    // 为UI层和测试提供状态查询

    public Rectangle getCropArea() {
        return new Rectangle(cropArea); // 返回拷贝，保护内部状态
    }

    public int getX() { return cropArea.x; }
    public int getY() { return cropArea.y; }
    public int getWidth() { return cropArea.width; }
    public int getHeight() { return cropArea.height; }
}