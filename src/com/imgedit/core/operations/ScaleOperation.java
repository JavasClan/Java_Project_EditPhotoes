package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 图片缩放操作实现类
 *
 * 功能说明：
 * - 支持指定目标尺寸进行缩放
 * - 支持保持宽高比
 * - 支持多种缩放质量模式
 *
 * 使用场景：
 * - 制作缩略图
 * - 调整图片尺寸以适应不同需求
 * - 批量处理图片大小
 */
public class ScaleOperation implements ImageOperation {

    /**
     * 缩放模式枚举
     */
    public enum ScaleMode {
        STRETCH("拉伸填充", false),
        KEEP_ASPECT_RATIO("保持比例", true),
        FIT_WIDTH("适应宽度", true),
        FIT_HEIGHT("适应高度", true);

        private final String description;
        private final boolean keepAspectRatio;

        ScaleMode(String description, boolean keepAspectRatio) {
            this.description = description;
            this.keepAspectRatio = keepAspectRatio;
        }

        public String getDescription() { return description; }
        public boolean isKeepAspectRatio() { return keepAspectRatio; }
    }

    /**
     * 缩放质量枚举
     */
    public enum ScaleQuality {
        FAST("快速", RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR),
        BALANCED("平衡", RenderingHints.VALUE_INTERPOLATION_BILINEAR),
        HIGH_QUALITY("高质量", RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        private final String description;
        private final Object renderingHint;

        ScaleQuality(String description, Object renderingHint) {
            this.description = description;
            this.renderingHint = renderingHint;
        }

        public String getDescription() { return description; }
        public Object getRenderingHint() { return renderingHint; }
    }

    private final int targetWidth;
    private final int targetHeight;
    private final ScaleMode scaleMode;
    private final ScaleQuality scaleQuality;

    /**
     * 构造函数
     *
     * @param targetWidth 目标宽度（像素）
     * @param targetHeight 目标高度（像素）
     * @param scaleMode 缩放模式
     * @param scaleQuality 缩放质量
     * @throws IllegalArgumentException 当目标尺寸无效时抛出
     */
    public ScaleOperation(int targetWidth, int targetHeight,
                          ScaleMode scaleMode, ScaleQuality scaleQuality) {
        validateTargetSize(targetWidth, targetHeight);
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.scaleMode = scaleMode;
        this.scaleQuality = scaleQuality;
    }

    /**
     * 便捷构造函数 - 使用默认质量（平衡）
     */
    public ScaleOperation(int targetWidth, int targetHeight, ScaleMode scaleMode) {
        this(targetWidth, targetHeight, scaleMode, ScaleQuality.BALANCED);
    }

    /**
     * 创建保持比例的缩放操作
     */
    public static ScaleOperation createKeepAspectRatio(int targetWidth, int targetHeight) {
        return new ScaleOperation(targetWidth, targetHeight, ScaleMode.KEEP_ASPECT_RATIO);
    }

    /**
     * 创建拉伸填充的缩放操作
     */
    public static ScaleOperation createStretch(int targetWidth, int targetHeight) {
        return new ScaleOperation(targetWidth, targetHeight, ScaleMode.STRETCH);
    }

    /**
     * 应用缩放操作
     *
     * 算法说明：
     * - 根据缩放模式计算实际目标尺寸
     * - 使用Graphics2D进行高质量缩放
     * - 根据质量设置选择合适的插值算法
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            // 计算实际缩放尺寸
            int[] actualSize = calculateActualSize(originalWidth, originalHeight);
            int actualWidth = actualSize[0];
            int actualHeight = actualSize[1];

            // 创建目标图像
            BufferedImage scaledImage = new BufferedImage(actualWidth, actualHeight, image.getType());
            Graphics2D g2d = scaledImage.createGraphics();

            // 设置缩放质量
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scaleQuality.getRenderingHint());
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 执行缩放
            g2d.drawImage(image.getScaledInstance(actualWidth, actualHeight, Image.SCALE_SMOOTH),
                    0, 0, null);

            g2d.dispose();
            return scaledImage;

        } catch (Exception e) {
            throw new ImageProcessingException("缩放操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算实际缩放尺寸
     *
     * @param originalWidth 原始宽度
     * @param originalHeight 原始高度
     * @return 包含[宽度,高度]的数组
     */
    private int[] calculateActualSize(int originalWidth, int originalHeight) {
        int actualWidth = targetWidth;
        int actualHeight = targetHeight;

        switch (scaleMode) {
            case KEEP_ASPECT_RATIO:
                // 保持比例：计算缩放因子，选择较小的那个
                double widthRatio = (double) targetWidth / originalWidth;
                double heightRatio = (double) targetHeight / originalHeight;
                double ratio = Math.min(widthRatio, heightRatio);

                actualWidth = (int) Math.round(originalWidth * ratio);
                actualHeight = (int) Math.round(originalHeight * ratio);
                break;

            case FIT_WIDTH:
                // 适应宽度：高度按比例计算
                double widthScale = (double) targetWidth / originalWidth;
                actualHeight = (int) Math.round(originalHeight * widthScale);
                break;

            case FIT_HEIGHT:
                // 适应高度：宽度按比例计算
                double heightScale = (double) targetHeight / originalHeight;
                actualWidth = (int) Math.round(originalWidth * heightScale);
                break;

            case STRETCH:
                // 拉伸填充：直接使用目标尺寸
                // 不需要额外计算
                break;
        }

        // 确保最小尺寸为1像素
        actualWidth = Math.max(1, actualWidth);
        actualHeight = Math.max(1, actualHeight);

        return new int[]{actualWidth, actualHeight};
    }

    /**
     * 验证目标尺寸
     */
    private void validateTargetSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    String.format("目标尺寸无效: 宽=%d, 高=%d", width, height));
        }

        // 限制最大尺寸，防止内存溢出
        final int MAX_SIZE = 10000;
        if (width > MAX_SIZE || height > MAX_SIZE) {
            throw new IllegalArgumentException(
                    String.format("目标尺寸过大（最大支持%d×%d）", MAX_SIZE, MAX_SIZE));
        }
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        return String.format("缩放 [目标:%d×%d, 模式:%s, 质量:%s]",
                targetWidth, targetHeight,
                scaleMode.getDescription(),
                scaleQuality.getDescription());
    }

    // ========== Getter方法 ==========

    public int getTargetWidth() { return targetWidth; }
    public int getTargetHeight() { return targetHeight; }
    public ScaleMode getScaleMode() { return scaleMode; }
    public ScaleQuality getScaleQuality() { return scaleQuality; }
}