package imgedit.core.exceptions;

/**
 * 图片处理异常类
 *
 * 设计目的：
 * - 统一处理图片操作中的各种错误
 * - 提供明确的错误信息供UI层显示给用户
 * - 区分系统异常和业务逻辑异常
 *
 * 使用场景：
 * - 文件格式不支持
 * - 参数验证失败
 * - 图像处理算法错误
 * - 内存不足等系统问题
 */
public class ImageProcessingException extends Exception {


    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 获取用户友好的错误信息
     *
     * @return 适合显示给用户的错误描述
     *
     * UI层使用：可直接调用此方法显示错误信息
     */
    public String getUserFriendlyMessage() {
        return "图片处理错误: " + getMessage();
    }

    public enum ErrorCode {
        IMAGE_NOT_LOADED,
        OPERATION_NOT_SUPPORTED,
        INVALID_PARAMETERS,
        PROCESSING_TIMEOUT,
        OUT_OF_MEMORY
    }
}
