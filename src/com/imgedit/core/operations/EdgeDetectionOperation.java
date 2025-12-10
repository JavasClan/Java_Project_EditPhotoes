package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;

/**
 * 图片边缘检测操作实现类
 *
 * 功能说明：
 * - 使用Sobel算子进行边缘检测
 * - 支持水平、垂直和全方位边缘检测
 * - 可调节边缘检测阈值
 *
 * 算法原理：
 * - Sobel算子使用3x3卷积核
 * - 计算图像梯度的近似值
 * - 通过阈值控制边缘检测灵敏度
 */
public class EdgeDetectionOperation implements ImageOperation {

    /**
     * 边缘检测模式枚举
     */
    public enum EdgeDetectionMode {
        HORIZONTAL("水平边缘", true, false),
        VERTICAL("垂直边缘", false, true),
        BOTH("全方位边缘", true, true);

        private final String description;
        private final boolean detectHorizontal;
        private final boolean detectVertical;

        EdgeDetectionMode(String description, boolean detectHorizontal, boolean detectVertical) {
            this.description = description;
            this.detectHorizontal = detectHorizontal;
            this.detectVertical = detectVertical;
        }

        public String getDescription() { return description; }
        public boolean isDetectHorizontal() { return detectHorizontal; }
        public boolean isDetectVertical() { return detectVertical; }
    }

    private final EdgeDetectionMode mode;
    private final int threshold; // 边缘检测阈值（0-255）

    /**
     * 构造函数（使用默认阈值）
     *
     * @param mode 边缘检测模式
     */
    public EdgeDetectionOperation(EdgeDetectionMode mode) {
        this(mode, 128);
    }

    /**
     * 构造函数
     *
     * @param mode 边缘检测模式
     * @param threshold 边缘检测阈值（0-255）
     * @throws IllegalArgumentException 当阈值无效时抛出
     */
    public EdgeDetectionOperation(EdgeDetectionMode mode, int threshold) {
        if (threshold < 0 || threshold > 255) {
            throw new IllegalArgumentException("阈值必须在0-255之间");
        }
        this.mode = mode;
        this.threshold = threshold;
    }

    /**
     * 便捷工厂方法
     */
    public static EdgeDetectionOperation createHorizontalEdges() {
        return new EdgeDetectionOperation(EdgeDetectionMode.HORIZONTAL);
    }

    public static EdgeDetectionOperation createVerticalEdges() {
        return new EdgeDetectionOperation(EdgeDetectionMode.VERTICAL);
    }

    public static EdgeDetectionOperation createAllEdges() {
        return new EdgeDetectionOperation(EdgeDetectionMode.BOTH);
    }

    /**
     * 应用边缘检测操作
     *
     * 实现步骤：
     * 1. 将彩色图片转换为灰度图
     * 2. 应用Sobel算子进行卷积
     * 3. 根据阈值二值化边缘
     * 4. 返回边缘检测结果
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            // 第一步：转换为灰度图
            BufferedImage grayImage = convertToGrayscale(image);

            // 第二步：应用Sobel算子
            BufferedImage edgeImage = applySobelOperator(grayImage);

            // 第三步：二值化处理
            BufferedImage binaryImage = applyThreshold(edgeImage);

            return binaryImage;

        } catch (Exception e) {
            throw new ImageProcessingException("边缘检测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换为灰度图
     */
    private BufferedImage convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 使用亮度加权法
                int gray = (int)(red * 0.299 + green * 0.587 + blue * 0.114);

                int grayPixel = (255 << 24) | (gray << 16) | (gray << 8) | gray;
                grayImage.setRGB(x, y, grayPixel);
            }
        }

        return grayImage;
    }

    /**
     * 应用Sobel算子
     */
    private BufferedImage applySobelOperator(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Sobel算子卷积核
        int[][] sobelX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        int[][] sobelY = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // 从第1行到倒数第1行，第1列到倒数第1列（跳过边缘）
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                int gradientX = 0;
                int gradientY = 0;

                // 应用3x3卷积核
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky);
                        int gray = pixel & 0xFF; // 提取灰度值

                        // 计算X方向和Y方向梯度
                        gradientX += gray * sobelX[ky + 1][kx + 1];
                        gradientY += gray * sobelY[ky + 1][kx + 1];
                    }
                }

                // 计算梯度幅值
                int gradient;
                if (mode.isDetectHorizontal() && mode.isDetectVertical()) {
                    // 全方位边缘：两个方向的组合
                    gradient = (int) Math.sqrt(gradientX * gradientX + gradientY * gradientY);
                } else if (mode.isDetectHorizontal()) {
                    // 水平边缘：只考虑X方向
                    gradient = Math.abs(gradientX);
                } else {
                    // 垂直边缘：只考虑Y方向
                    gradient = Math.abs(gradientY);
                }

                // 限制梯度值在0-255范围内
                gradient = Math.min(255, Math.max(0, gradient));

                // 创建边缘像素
                int edgePixel = (255 << 24) | (gradient << 16) | (gradient << 8) | gradient;
                result.setRGB(x, y, edgePixel);
            }
        }

        return result;
    }

    /**
     * 应用阈值二值化
     */
    private BufferedImage applyThreshold(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int gray = pixel & 0xFF;

                // 二值化：大于阈值设为白色，否则设为黑色
                int binaryGray = (gray > threshold) ? 255 : 0;

                int binaryPixel = (255 << 24) | (binaryGray << 16) | (binaryGray << 8) | binaryGray;
                binaryImage.setRGB(x, y, binaryPixel);
            }
        }

        return binaryImage;
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        return String.format("边缘检测 [%s, 阈值:%d]", mode.getDescription(), threshold);
    }

    // ========== Getter方法 ==========

    public EdgeDetectionMode getMode() {
        return mode;
    }

    public int getThreshold() {
        return threshold;
    }
}