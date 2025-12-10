package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 图片旋转操作实现类
 *
 * 功能说明：
 * - 支持90°、180°、270°旋转
 * - 保持图片质量，无压缩损失
 * - 自动调整画布大小适应旋转
 *
 * 算法说明：
 * - 使用Graphics2D的rotate方法实现高质量旋转
 * - 通过数学计算确保旋转中心正确
 * - 为后续自定义角度旋转预留扩展点
 */
public class RotateOperation implements ImageOperation {

    /**
     * 旋转角度枚举
     *
     * 设计考虑：
     * - 使用枚举保证类型安全
     * - 便于UI层通过下拉菜单选择
     * - 后续可扩展其他角度
     */
    public enum RotationAngle {
        DEGREES_90(90, "90°顺时针"),
        DEGREES_180(180, "180°旋转"),
        DEGREES_270(270, "90°逆时针");

        private final int degrees;
        private final String description;

        RotationAngle(int degrees, String description) {
            this.degrees = degrees;
            this.description = description;
        }

        public int getDegrees() { return degrees; }
        public String getDescription() { return description; }
    }

    private final RotationAngle angle;

    /**
     * 构造函数
     *
     * @param angle 旋转角度，使用预定义枚举保证有效性
     */
    public RotateOperation(RotationAngle angle) {
        this.angle = angle;
    }

    // ========== 为UI层提供的工厂方法 ==========
    // 简化UI层创建对象的复杂度

    /**
     * 创建90度顺时针旋转操作
     *
     * @return 配置好的旋转操作对象
     *
     * UI层调用示例：
     * RotateOperation rotateOp = RotateOperation.create90Degree();
     * imageProcessor.applyOperation(rotateOp);
     */
    public static RotateOperation create90Degree() {
        return new RotateOperation(RotationAngle.DEGREES_90);
    }

    public static RotateOperation create180Degree() {
        return new RotateOperation(RotationAngle.DEGREES_180);
    }

    public static RotateOperation create270Degree() {
        return new RotateOperation(RotationAngle.DEGREES_270);
    }

    /**
     * 创建自定义角度旋转操作（为后续功能预留）
     *
     * @param degrees 旋转角度（目前只支持90的倍数）
     * @return 配置好的旋转操作对象
     * @throws IllegalArgumentException 当角度不支持时抛出
     *
     * 扩展说明：
     * - 目前限制为90°倍数以保证实现简单
     * - 后续可修改为支持任意角度，需要处理图像插值等问题
     */
    public static RotateOperation createCustomAngle(int degrees) {
        // 参数验证 - 工具层可提供通用的参数验证工具
        if (degrees % 90 != 0) {
            throw new IllegalArgumentException("目前只支持90度的倍数，输入角度: " + degrees);
        }
        return new RotateOperation(createRotationAngle(degrees));
    }

    /**
     * 应用旋转操作 - 核心算法实现
     *
     * 算法特点：
     * - 不修改原图，返回新图片对象
     * - 90°和270°旋转时会交换宽高
     * - 使用高质量渲染提示（如有需要）
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            BufferedImage rotatedImage;
            Graphics2D g2d;

            switch (angle) {
                case DEGREES_90:
                    // 90°旋转：宽高互换
                    rotatedImage = new BufferedImage(height, width, image.getType());
                    g2d = rotatedImage.createGraphics();
                    // 设置旋转中心和角度，平移确保图片完全显示
                    g2d.translate(height, 0); // 先平移到右侧
                    g2d.rotate(Math.PI / 2);  // 再旋转90°
                    g2d.drawImage(image, 0, 0, null);
                    break;

                case DEGREES_180:
                    // 180°旋转：宽高不变，中心旋转
                    rotatedImage = new BufferedImage(width, height, image.getType());
                    g2d = rotatedImage.createGraphics();
                    g2d.translate(width, height); // 平移到右下角
                    g2d.rotate(Math.PI);         // 旋转180°
                    g2d.drawImage(image, 0, 0, null);
                    break;

                case DEGREES_270:
                    // 270°旋转：宽高互换
                    rotatedImage = new BufferedImage(height, width, image.getType());
                    g2d = rotatedImage.createGraphics();
                    g2d.translate(0, width);     // 平移到下方
                    g2d.rotate(3 * Math.PI / 2); // 旋转270°
                    g2d.drawImage(image, 0, 0, null);
                    break;

                default:
                    throw new ImageProcessingException("不支持的旋转角度: " + angle);
            }

            g2d.dispose(); // 重要：释放Graphics2D资源
            return rotatedImage;

        } catch (Exception e) {
            // 统一异常处理，抛出明确的业务异常
            throw new ImageProcessingException("旋转操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取操作名称 - 用于历史记录显示
     */
    @Override
    public String getOperationName() {
        return "旋转 " + angle.getDescription();
    }

    // ========== 私有辅助方法 ==========

    /**
     * 根据度数创建旋转角度枚举
     * 简化实现，实际可根据需要扩展支持更多角度
     */
    private static RotationAngle createRotationAngle(int degrees) {
        switch (degrees) {
            case 90: return RotationAngle.DEGREES_90;
            case 180: return RotationAngle.DEGREES_180;
            case 270: return RotationAngle.DEGREES_270;
            default: throw new IllegalArgumentException("不支持的旋转角度: " + degrees);
        }
    }

    // ========== Getter方法 ==========
    // 为UI层和测试提供状态查询

    public RotationAngle getAngle() {
        return angle;
    }
}