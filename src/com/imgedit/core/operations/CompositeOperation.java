package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 组合操作功能实现类
 *
 * 功能说明：
 * - 将多个操作组合成一个复合操作
 * - 按顺序应用所有子操作
 * - 支持操作的嵌套组合
 *
 * 设计模式：
 * - 组合模式：将操作组织成树形结构
 * - 支持复杂的操作流程定义
 */
public class CompositeOperation implements ImageOperation {

    private final List<ImageOperation> operations;
    private final String name;

    /**
     * 构造函数
     *
     * @param name 组合操作的名称
     */
    public CompositeOperation(String name) {
        this.name = name;
        this.operations = new ArrayList<>();
    }

    /**
     * 添加子操作
     *
     * @param operation 要添加的图片操作
     * @return 当前组合操作（支持链式调用）
     */
    public CompositeOperation addOperation(ImageOperation operation) {
        operations.add(operation);
        return this;
    }

    /**
     * 批量添加操作
     *
     * @param operations 要添加的图片操作数组
     * @return 当前组合操作
     */
    public CompositeOperation addOperations(ImageOperation... operations) {
        for (ImageOperation op : operations) {
            this.operations.add(op);
        }
        return this;
    }

    /**
     * 应用组合操作
     *
     * 实现说明：
     * - 按顺序逐个应用所有子操作
     * - 每个操作的输出作为下一个操作的输入
     * - 如果某个操作失败，整个组合操作失败
     */
    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            BufferedImage result = image;

            // 按顺序应用所有操作
            for (ImageOperation operation : operations) {
                result = operation.apply(result);
            }

            return result;

        } catch (ImageProcessingException e) {
            // 重新包装异常，包含更多上下文信息
            throw new ImageProcessingException(
                    String.format("组合操作'%s'执行失败: %s", name, e.getMessage()), e);
        } catch (Exception e) {
            throw new ImageProcessingException(
                    String.format("组合操作'%s'执行失败", name), e);
        }
    }

    /**
     * 创建预设组合操作：旧照片效果
     */
    public static CompositeOperation createVintageEffect() {
        return new CompositeOperation("怀旧效果")
                .addOperation(new GrayscaleOperation()) // 先灰度化
                .addOperation(new ContrastOperation(1.3f)) // 增加对比度
                .addOperation(BrightnessOperation.createDecrease(0.1f)); // 稍微降低亮度
    }

    /**
     * 创建预设组合操作：素描效果
     */
    public static CompositeOperation createSketchEffect() {
        return new CompositeOperation("素描效果")
                .addOperation(new GrayscaleOperation()) // 灰度化
                .addOperation(EdgeDetectionOperation.createAllEdges()) // 边缘检测
                .addOperation(new ContrastOperation(2.0f)); // 增强对比度
    }

    /**
     * 获取操作名称
     */
    @Override
    public String getOperationName() {
        StringBuilder sb = new StringBuilder();
        sb.append("组合操作: ").append(name).append(" [");

        for (int i = 0; i < operations.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(operations.get(i).getOperationName());
        }

        sb.append("]");
        return sb.toString();
    }

    // ========== Getter方法 ==========

    public String getName() {
        return name;
    }

    public List<ImageOperation> getOperations() {
        return new ArrayList<>(operations); // 返回拷贝，保护内部状态
    }

    public int getOperationCount() {
        return operations.size();
    }
}