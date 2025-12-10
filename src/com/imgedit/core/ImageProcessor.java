package imgedit.core;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Stack;

/**
 * 图片处理器管理器 - 核心协调类
 *
 * 职责说明：
 * 1. 管理当前图片状态
 * 2. 维护操作历史记录（撤销/重做）
 * 3. 协调所有图片操作
 * 4. 为UI层提供统一的操作入口
 *
 * 协作说明：
 * - UI层通过此类调用所有图片处理功能
 * - 工具层可通过此类获取当前图片状态
 * - 采用外观模式，简化UI层调用复杂度
 */
public class ImageProcessor {
    private BufferedImage currentImage;
    private Stack<BufferedImage> historyStack;
    private Stack<BufferedImage> redoStack;

    /**
     * 构造函数
     *
     * @param initialImage 初始图片，由UI层通过文件加载后传入
     *
     * UI层调用示例：
     * BufferedImage image = ImageIO.read(file);
     * ImageProcessor processor = new ImageProcessor(image);
     */
    public ImageProcessor(BufferedImage initialImage) {
        this.currentImage = initialImage;
        this.historyStack = new Stack<>();
        this.redoStack = new Stack<>();
        saveToHistory(); // 保存初始状态到历史记录
    }

    /**
     * 应用图片操作 - UI层主要调用接口
     *
     * @param operation 要执行的图片操作对象
     *
     * UI层调用流程：
     * 1. 创建具体操作对象（如RotateOperation）
     * 2. 调用此方法应用操作
     * 3. 通过getCurrentImage()获取结果并更新显示
     *
     * 异常处理：
     * - 操作失败时会自动恢复之前状态
     * - 抛出RuntimeException包含详细错误信息供UI层显示
     */
    public void applyOperation(ImageOperation operation) {
        saveToHistory();
        try {
            currentImage = operation.apply(currentImage);
            redoStack.clear(); // 新操作后清空重做栈
        } catch (ImageProcessingException e) {
            restoreFromHistory(); // 操作失败，恢复之前状态
            throw new RuntimeException("图片处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取当前图片 - UI层显示调用接口
     *
     * @return 当前处理后的图片
     *
     * UI层使用说明：
     * - 每次操作后调用此方法获取最新图片
     * - 需要将BufferedImage转换为JavaFX Image进行显示
     * - 工具层提供转换方法：ImageUtils.bufferedImageToFX(image)
     */
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    // ========== 历史记录功能接口 ==========
    // 为后续撤销/重做功能预留，UI层可调用这些方法

    /**
     * 检查是否可以撤销
     *
     * @return true表示有操作可以撤销
     *
     * UI层使用：根据返回值更新撤销按钮的可用状态
     */
    public boolean canUndo() {
        return !historyStack.isEmpty();
    }

    /**
     * 检查是否可以重做
     *
     * @return true表示有操作可以重做
     *
     * UI层使用：根据返回值更新重做按钮的可用状态
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 撤销上一次操作
     *
     * UI层调用：当用户点击撤销按钮时调用
     * 效果：恢复到上一次操作前的状态
     */
    public void undo() {
        if (canUndo()) {
            redoStack.push(currentImage);
            currentImage = historyStack.pop();
        }
    }

    /**
     * 重做上一次撤销的操作
     *
     * UI层调用：当用户点击重做按钮时调用
     * 效果：重新应用上一次撤销的操作
     */
    public void redo() {
        if (canRedo()) {
            historyStack.push(currentImage);
            currentImage = redoStack.pop();
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 保存当前状态到历史记录
     * 内部使用，UI层不需要调用
     */
    private void saveToHistory() {
        historyStack.push(deepCopyImage(currentImage));
        // 限制历史记录数量，防止内存溢出
        if (historyStack.size() > 10) { // 数量可配置，后续可移到配置文件中
            historyStack.remove(0);
        }
    }

    /**
     * 从历史记录恢复状态
     * 内部使用，在操作失败时自动调用
     */
    private void restoreFromHistory() {
        if (!historyStack.isEmpty()) {
            currentImage = historyStack.pop();
        }
    }

    /**
     * 深拷贝图片对象
     *
     * @param image 原图片
     * @return 拷贝后的新图片
     *
     * 工具层协作：如果需要更高效的图片拷贝，可调用工具层的ImageCopyUtils
     */
    private BufferedImage deepCopyImage(BufferedImage image) {
        BufferedImage copy = new BufferedImage(
                image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}