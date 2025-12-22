package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 批量处理操作类
 * 功能：对多个图像应用相同的操作或操作序列
 */
public class BatchOperation implements ImageOperation {

    /**
     * 批量处理模式
     */
    public enum BatchMode {
        SINGLE_OPERATION("单个操作"),      // 所有图片应用同一个操作
        OPERATION_SEQUENCE("操作序列"),    // 所有图片应用相同的操作序列
        CUSTOM_PROCESSING("自定义处理");   // 每张图片自定义处理

        private final String description;

        BatchMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 批量处理配置
     */
    public static class BatchConfig {
        private final BatchMode mode;
        private final List<ImageOperation> operations;
        private final int threadCount;
        private final boolean preserveOriginal;
        private final String outputSuffix;

        public BatchConfig(BatchMode mode, List<ImageOperation> operations,
                           int threadCount, boolean preserveOriginal, String outputSuffix) {
            this.mode = mode;
            this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
            this.threadCount = Math.max(1, Math.min(8, threadCount));
            this.preserveOriginal = preserveOriginal;
            this.outputSuffix = outputSuffix != null ? outputSuffix : "_processed";
        }

        public BatchMode getMode() { return mode; }
        public List<ImageOperation> getOperations() { return operations; }
        public int getThreadCount() { return threadCount; }
        public boolean isPreserveOriginal() { return preserveOriginal; }
        public String getOutputSuffix() { return outputSuffix; }
    }

    /**
     * 批量处理任务
     */
    public static class BatchTask {
        private final BufferedImage inputImage;
        private final String imageName;
        private final BatchConfig config;

        public BatchTask(BufferedImage inputImage, String imageName, BatchConfig config) {
            this.inputImage = inputImage;
            this.imageName = imageName;
            this.config = config;
        }

        public BufferedImage getInputImage() { return inputImage; }
        public String getImageName() { return imageName; }
        public BatchConfig getConfig() { return config; }
    }

    /**
     * 批量处理结果
     */
    public static class BatchResult {
        private final boolean success;
        private final String imageName;
        private final BufferedImage resultImage;
        private final String message;
        private final Throwable error;

        public BatchResult(boolean success, String imageName,
                           BufferedImage resultImage, String message, Throwable error) {
            this.success = success;
            this.imageName = imageName;
            this.resultImage = resultImage;
            this.message = message;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getImageName() { return imageName; }
        public BufferedImage getResultImage() { return resultImage; }
        public String getMessage() { return message; }
        public Throwable getError() { return error; }
    }

    /**
     * 批量处理进度监听器
     */
    public interface BatchProgressListener {
        void onProgress(String imageName, int processed, int total);
        void onTaskComplete(String imageName, boolean success);
        void onBatchComplete(int successCount, int total);
    }

    private final BatchConfig config;
    private final List<BatchTask> tasks;

    /**
     * 创建批量处理操作
     */
    public BatchOperation(BatchConfig config, List<BatchTask> tasks) {
        this.config = config;
        this.tasks = new ArrayList<>(tasks);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        // 单个图像批量处理（主要用于测试）
        if (tasks.size() == 1 && tasks.get(0).getInputImage() == image) {
            return processSingleImage(tasks.get(0));
        }

        throw new ImageProcessingException("批量处理需要多个图像");
    }

    /**
     * 执行批量处理
     */
    public List<BatchResult> executeBatch(BatchProgressListener listener) {
        List<BatchResult> results = new ArrayList<>();

        if (tasks.isEmpty()) {
            return results;
        }

        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadCount());
        List<Future<BatchResult>> futures = new ArrayList<>();

        try {
            // 提交所有任务
            for (int i = 0; i < tasks.size(); i++) {
                final int index = i;
                final BatchTask task = tasks.get(i);

                Callable<BatchResult> callable = () -> {
                    try {
                        BufferedImage result = processSingleImage(task);

                        // 通知进度
                        if (listener != null) {
                            listener.onProgress(task.getImageName(), index + 1, tasks.size());
                            listener.onTaskComplete(task.getImageName(), true);
                        }

                        return new BatchResult(true, task.getImageName(),
                                result, "处理成功", null);
                    } catch (Exception e) {
                        // 通知失败
                        if (listener != null) {
                            listener.onTaskComplete(task.getImageName(), false);
                        }

                        return new BatchResult(false, task.getImageName(),
                                null, "处理失败: " + e.getMessage(), e);
                    }
                };

                futures.add(executor.submit(callable));
            }

            // 等待所有任务完成
            for (Future<BatchResult> future : futures) {
                try {
                    BatchResult result = future.get();
                    results.add(result);
                } catch (InterruptedException | ExecutionException e) {
                    results.add(new BatchResult(false, "未知", null,
                            "任务执行异常: " + e.getMessage(), e));
                }
            }

            // 计算成功数量
            int successCount = (int) results.stream().filter(BatchResult::isSuccess).count();

            // 通知批量完成
            if (listener != null) {
                listener.onBatchComplete(successCount, tasks.size());
            }

        } finally {
            executor.shutdown();
        }

        return results;
    }

    /**
     * 处理单个图像
     */
    private BufferedImage processSingleImage(BatchTask task) throws ImageProcessingException {
        BufferedImage image = task.getInputImage();
        BatchConfig config = task.getConfig();

        try {
            BufferedImage result = image;

            // 根据模式处理图像
            switch (config.getMode()) {
                case SINGLE_OPERATION:
                    if (!config.getOperations().isEmpty()) {
                        result = config.getOperations().get(0).apply(result);
                    }
                    break;

                case OPERATION_SEQUENCE:
                    for (ImageOperation operation : config.getOperations()) {
                        result = operation.apply(result);
                    }
                    break;

                case CUSTOM_PROCESSING:
                    // 自定义处理需要子类实现
                    result = applyCustomProcessing(result, task.getImageName());
                    break;
            }

            return result;

        } catch (Exception e) {
            throw new ImageProcessingException("处理图像失败: " + task.getImageName(), e);
        }
    }

    /**
     * 自定义处理（子类可以重写）
     */
    protected BufferedImage applyCustomProcessing(BufferedImage image, String imageName)
            throws ImageProcessingException {
        // 默认实现：直接返回原图
        return image;
    }

    /**
     * 创建单操作批量处理
     */
    public static BatchOperation createSingleOperationBatch(List<BatchTask> tasks,
                                                            ImageOperation operation) {
        List<ImageOperation> operations = new ArrayList<>();
        operations.add(operation);

        BatchConfig config = new BatchConfig(BatchMode.SINGLE_OPERATION,
                operations, 4, false, "_processed");

        return new BatchOperation(config, tasks);
    }

    /**
     * 创建多操作批量处理
     */
    public static BatchOperation createSequenceBatch(List<BatchTask> tasks,
                                                     List<ImageOperation> operations) {
        BatchConfig config = new BatchConfig(BatchMode.OPERATION_SEQUENCE,
                operations, 4, false, "_processed");

        return new BatchOperation(config, tasks);
    }

    /**
     * 创建任务列表
     */
    public static List<BatchTask> createTasks(List<BufferedImage> images,
                                              List<String> imageNames, BatchConfig config) {
        List<BatchTask> tasks = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            BufferedImage image = images.get(i);
            String name = i < imageNames.size() ? imageNames.get(i) : "image_" + i;
            tasks.add(new BatchTask(image, name, config));
        }

        return tasks;
    }

    /**
     * 创建优化的批量处理操作（自动选择线程数）
     */
    public static BatchOperation createOptimizedBatch(List<BatchTask> tasks,
                                                      List<ImageOperation> operations) {
        // 根据任务数量自动选择线程数
        int threadCount = Math.min(4, Math.max(1, tasks.size() / 10 + 1));

        BatchConfig config = new BatchConfig(BatchMode.OPERATION_SEQUENCE,
                operations, threadCount, true, "_batch");

        return new BatchOperation(config, tasks);
    }

    @Override
    public String getOperationName() {
        return String.format("批量处理 [%d个任务, %s]",
                tasks.size(), config.getMode().getDescription());
    }
}