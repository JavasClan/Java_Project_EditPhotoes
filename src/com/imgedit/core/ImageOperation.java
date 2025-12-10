package imgedit.core;

import imgedit.core.exceptions.ImageProcessingException;

import java.awt.image.BufferedImage;

/**
 * 图片操作统一接口
 *
 * 设计说明：
 * - 所有图片处理操作都必须实现此接口
 * - 采用命令模式，每个操作都是独立的对象
 * - 便于实现撤销/重做功能
 * - UI层通过此接口调用所有图片处理功能
 *
 * @author 核心处理层开发团队
 */
public interface ImageOperation {

    /**
     * 对图片应用操作
     *
     * @param image 输入的原始图片
     * @return 处理后的新图片（注意：不修改原图，返回新对象）
     * @throws ImageProcessingException 当操作失败时抛出，包含错误信息
     *
     * 注意事项：
     * - 必须保证线程安全
     * - 不应修改输入参数image
     * - 操作失败时应抛出明确的异常信息供UI层显示
     */
    BufferedImage apply(BufferedImage image) throws ImageProcessingException;

    /**
     * 获取操作名称（用于历史记录显示）
     *
     * @return 操作的描述性名称，如"旋转90度"、"裁剪[100,100,200,200]"
     *
     * UI层使用：在历史记录列表中显示此名称
     */
    String getOperationName();
}