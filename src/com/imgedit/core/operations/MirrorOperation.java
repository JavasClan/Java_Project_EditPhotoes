package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 图片镜像操作实现类
 *
 * 功能说明：
 * - 基于图片边缘创建镜像效果
 * - 支持垂直和水平镜像
 * - 可调节镜像边界的位置
 *
 * 应用场景：
 * - 创建对称效果
 * - 制作艺术效果
 * - 生成倒影效果
 */
public class MirrorOperation implements ImageOperation {

    /**
     * 镜像方向枚举
     */
    public enum MirrorDirection {
        VERTICAL_LEFT("垂直左镜像", true, false, 0.5f),
        VERTICAL_RIGHT("垂直右镜像", true, false, 0.5f),
        HORIZONTAL_TOP("水平上镜像", false, true, 0.5f),
        HORIZONTAL_BOTTOM("水平下镜像", false, true, 0.5f);

        private final String description;
        private final boolean vertical;   // 是否为垂直镜像
        private final boolean horizontal; // 是否为水平镜像
        private final float splitRatio;   // 分割比例

        MirrorDirection(String description, boolean vertical, boolean horizontal, float splitRatio) {
            this.description = description;
            this.vertical = vertical;
            this.horizontal = horizontal;
            this.splitRatio = splitRatio;
        }

        public String getDescription() { return description; }
        public boolean isVertical() { return vertical; }
        public boolean isHorizontal() { return horizontal; }
        public float getSplitRatio() { return splitRatio; }
    }

    private final MirrorDirection direction;
    private final float splitRatio; // 镜像分割比例（0.1-0.9）

    /**
     * 构造函数（使用默认分割比例）
     *
     * @param direction 镜像方向
     */
    public MirrorOperation(MirrorDirection direction) {
        this(direction, direction.getSplitRatio());
    }

    /**
     * 构造函数
     *
     * @param direction 镜像方向
     * @param splitRatio 镜像分割比例（0.1-0.9）
     * @throws IllegalArgumentException 当分割比例无效时抛出
     */
    public MirrorOperation(MirrorDirection direction, float splitRatio) {
        if (splitRatio <= 0 || splitRatio >= 1.0f) {
            throw new IllegalArgumentException("分割比例必须在0.1到0.9之间");
        }
        this.direction = direction;
        this.splitRatio = splitRatio;
    }

    /**
     * 便捷工厂方法
     */
    public static MirrorOperation createVerticalLeftMirror() {
        return new MirrorOperation(MirrorDirection.VERTICAL_LEFT);
    }

    public static MirrorOperation createVerticalRightMirror() {
        return new MirrorOperation(MirrorDirection.VERTICAL_RIGHT);
    }

    public static MirrorOperation createHorizontalTopMirror() {
        return new MirrorOperation(MirrorDirection.HORIZONTAL_TOP);
    }

    public static MirrorOperation createHorizontalBottomMirror() {
        return new MirrorOperation(MirrorDirection.HORIZONTAL_BOTTOM);
    }

    /**
     * 应用镜像操作
     *
     * 实现原理：
     * - 将图片分割为两部分
     * - 复制一部分到另一部分，形成镜像效果
     * - 可以调整分割线位置
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // 创建目标图片
            BufferedImage mirroredImage = new BufferedImage(width, height, image.getType());
            Graphics2D g2d = mirroredImage.createGraphics();

            // 先绘制原始图片
            g2d.drawImage(image, 0, 0, null);

            // 根据镜像方向应用镜像效果
            if (direction.isVertical()) {
                applyVerticalMirror(g2d, image, width, height);
            } else if (direction.isHorizontal()) {
                applyHorizontalMirror(g2d, image, width, height);
            }

            g2d.dispose();
            return mirroredImage;

        } catch (Exception e) {
            throw new ImageProcessingException("镜像操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用垂直镜像
     */
    private void applyVerticalMirror(Graphics2D g2d, BufferedImage image, int width, int height) {
        int splitX = (int)(width * splitRatio);

        if (direction == MirrorDirection.VERTICAL_LEFT) {
            // 垂直左镜像：右侧镜像到左侧
            int sourceWidth = width - splitX;
            g2d.drawImage(image,
                    0, 0, splitX, height,                        // 目标区域：左侧
                    splitX, 0, splitX + sourceWidth, height,    // 源区域：右侧
                    null);
        } else {
            // 垂直右镜像：左侧镜像到右侧
            int sourceWidth = splitX;
            g2d.drawImage(image,
                    splitX, 0, width, height,                   // 目标区域：右侧
                    0, 0, sourceWidth, height,                  // 源区域：左侧
                    null);
        }
    }

    /**
     * 应用水平镜像
     */
    private void applyHorizontalMirror(Graphics2D g2d, BufferedImage image, int width, int height) {
        int splitY = (int)(height * splitRatio);

        if (direction == MirrorDirection.HORIZONTAL_TOP) {
            // 水平上镜像：下部镜像到上部
            int sourceHeight = height - splitY;
            g2d.drawImage(image,
                    0, 0, width, splitY,                        // 目标区域：上部
                    0, splitY, width, splitY + sourceHeight,    // 源区域：下部
                    null);
        } else {
            // 水平下镜像：上部镜像到下部
            int sourceHeight = splitY;
            g2d.drawImage(image,
                    0, splitY, width, height,                   // 目标区域：下部
                    0, 0, width, sourceHeight,                  // 源区域：上部
                    null);
        }
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        return String.format("镜像效果 [%s, 分割比例:%.1f]",
                direction.getDescription(), splitRatio);
    }

    // ========== Getter方法 ==========

    public MirrorDirection getDirection() {
        return direction;
    }

    public float getSplitRatio() {
        return splitRatio;
    }
}