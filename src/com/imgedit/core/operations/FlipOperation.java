package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * 图片翻转操作实现类
 *
 * 功能说明：
 * - 支持水平翻转（镜像）
 * - 支持垂直翻转
 * - 保持图片质量
 *
 * 数学原理：
 * - 水平翻转：x' = width - x - 1
 * - 垂直翻转：y' = height - y - 1
 */
public class FlipOperation implements ImageOperation {

    /**
     * 翻转方向枚举
     */
    public enum FlipDirection {
        HORIZONTAL("水平翻转"),
        VERTICAL("垂直翻转");

        private final String description;

        FlipDirection(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    private final FlipDirection direction;

    /**
     * 构造函数
     *
     * @param direction 翻转方向
     */
    public FlipOperation(FlipDirection direction) {
        this.direction = direction;
    }

    /**
     * 创建水平翻转操作
     */
    public static FlipOperation createHorizontalFlip() {
        return new FlipOperation(FlipDirection.HORIZONTAL);
    }

    /**
     * 创建垂直翻转操作
     */
    public static FlipOperation createVerticalFlip() {
        return new FlipOperation(FlipDirection.VERTICAL);
    }

    /**
     * 应用翻转操作
     *
     * 实现说明：
     * - 使用AffineTransform进行高效变换
     * - 通过缩放因子-1实现翻转效果
     * - 配合平移确保图片在正确位置显示
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // 创建相同大小和类型的空白图片
            BufferedImage flippedImage = new BufferedImage(width, height, image.getType());
            Graphics2D g2d = flippedImage.createGraphics();

            // 创建仿射变换对象
            AffineTransform transform = new AffineTransform();

            switch (direction) {
                case HORIZONTAL:
                    // 水平翻转：x方向缩放-1，然后平移宽度
                    transform.scale(-1, 1);
                    transform.translate(-width, 0);
                    break;

                case VERTICAL:
                    // 垂直翻转：y方向缩放-1，然后平移高度
                    transform.scale(1, -1);
                    transform.translate(0, -height);
                    break;

                default:
                    throw new ImageProcessingException("不支持的翻转方向: " + direction);
            }

            // 应用变换并绘制图像
            g2d.transform(transform);
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            return flippedImage;

        } catch (Exception e) {
            throw new ImageProcessingException("翻转操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        return "图片" + direction.getDescription();
    }

    // ========== Getter方法 ==========

    public FlipDirection getDirection() {
        return direction;
    }
}