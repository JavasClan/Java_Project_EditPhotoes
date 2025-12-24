package imgedit.ui;

import imgedit.core.ImageOperation;
import imgedit.core.operations.*;
import imgedit.core.operations.ArtisticStyleOperation.ArtisticStyle;
import imgedit.core.operations.RotateOperation.RotationAngle;
import imgedit.core.operations.FlipOperation.FlipDirection;
import imgedit.core.operations.GrayscaleOperation.GrayscaleAlgorithm;
import imgedit.core.operations.BatchOperation.BatchMode;
import imgedit.core.operations.BatchOperation.BatchConfig;
import imgedit.core.operations.BatchOperation.BatchTask;
import imgedit.core.operations.DrawingOperation.DrawingType;
import imgedit.core.operations.DrawingOperation.BrushStyle;
import imgedit.core.operations.DrawingOperation.TextStyle;
import imgedit.core.operations.DrawingOperation.DrawingPoint;
import imgedit.core.operations.DrawingOperation.DrawingElement;

import java.awt.Color;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 图像操作工厂 - 负责创建各种图像处理操作实例
 * 采用工厂模式，提供统一的操作创建接口
 */
public class ImageOperationFactory {

    private static ImageOperationFactory instance;

    // 操作注册表
    private final Map<String, OperationCreator> operationCreators = new HashMap<>();

    private ImageOperationFactory() {
        registerDefaultOperations();
    }

    /**
     * 获取工厂单例实例
     */
    public static synchronized ImageOperationFactory getInstance() {
        if (instance == null) {
            instance = new ImageOperationFactory();
        }
        return instance;
    }

    /**
     * 注册默认操作
     */
    private void registerDefaultOperations() {
        // 几何变换操作
        registerOperation("rotate_90", params -> RotateOperation.create90Degree());
        registerOperation("rotate_180", params -> RotateOperation.create180Degree());
        registerOperation("rotate_270", params -> RotateOperation.create270Degree());
        registerOperation("rotate_custom", params -> {
            int angle = params.containsKey("angle") ?
                    (int) params.get("angle") : 90;
            return RotateOperation.createCustomAngle(angle);
        });

        // 翻转操作
        registerOperation("flip_horizontal", params -> FlipOperation.createHorizontalFlip());
        registerOperation("flip_vertical", params -> FlipOperation.createVerticalFlip());

        // 灰度化操作
        registerOperation("grayscale", params -> {
            if (params.containsKey("algorithm")) {
                GrayscaleAlgorithm algorithm = (GrayscaleAlgorithm) params.get("algorithm");
                return GrayscaleOperation.createWithAlgorithm(algorithm);
            }
            return GrayscaleOperation.create();
        });

        // 艺术风格操作
        registerOperation("artistic_style", params -> {
            ArtisticStyle style = params.containsKey("style") ?
                    (ArtisticStyle) params.get("style") : ArtisticStyle.OIL_PAINTING;
            float intensity = params.containsKey("intensity") ?
                    (float) params.get("intensity") : 0.7f;
            int brushSize = params.containsKey("brushSize") ?
                    (int) params.get("brushSize") : 5;
            float detailPreserve = params.containsKey("detailPreserve") ?
                    (float) params.get("detailPreserve") : 0.5f;

            ArtisticStyleOperation.StyleParameters styleParams =
                    new ArtisticStyleOperation.StyleParameters(intensity, brushSize, detailPreserve);
            return new ArtisticStyleOperation(style, styleParams);
        });

        // 批量处理操作
        registerOperation("batch", params -> {
            BatchMode mode = params.containsKey("mode") ?
                    (BatchMode) params.get("mode") : BatchMode.SINGLE_OPERATION;
            List<ImageOperation> operations = params.containsKey("operations") ?
                    (List<ImageOperation>) params.get("operations") : new ArrayList<>();
            int threadCount = params.containsKey("threadCount") ?
                    (int) params.get("threadCount") : 4;
            boolean preserveOriginal = params.containsKey("preserveOriginal") ?
                    (boolean) params.get("preserveOriginal") : false;
            String outputSuffix = params.containsKey("outputSuffix") ?
                    (String) params.get("outputSuffix") : "_processed";

            BatchConfig config = new BatchConfig(
                    mode, operations, threadCount, preserveOriginal, outputSuffix
            );

            List<BatchTask> tasks = params.containsKey("tasks") ?
                    (List<BatchTask>) params.get("tasks") : new ArrayList<>();

            return new BatchOperation(config, tasks);
        });

        // 绘图操作
        registerOperation("drawing", params -> {
            List<DrawingElement> elements = params.containsKey("elements") ?
                    (List<DrawingElement>) params.get("elements") : new ArrayList<>();

            if (elements.isEmpty()) {
                DrawingElement element = params.containsKey("element") ?
                        (DrawingElement) params.get("element") : null;
                if (element != null) {
                    elements.add(element);
                }
            }

            return new DrawingOperation(elements);
        });

        // 画笔操作
        registerOperation("brush", params -> {
            List<DrawingPoint> points = params.containsKey("points") ?
                    (List<DrawingPoint>) params.get("points") : new ArrayList<>();
            Color color = params.containsKey("color") ?
                    (Color) params.get("color") : Color.BLACK;
            int thickness = params.containsKey("thickness") ?
                    (int) params.get("thickness") : 3;

            return DrawingOperation.createBrushOperation(points, color, thickness);
        });

        // 文字操作
        registerOperation("text", params -> {
            String text = params.containsKey("text") ?
                    (String) params.get("text") : "Hello";
            int x = params.containsKey("x") ? (int) params.get("x") : 50;
            int y = params.containsKey("y") ? (int) params.get("y") : 50;
            String fontName = params.containsKey("fontName") ?
                    (String) params.get("fontName") : "Arial";
            int fontSize = params.containsKey("fontSize") ?
                    (int) params.get("fontSize") : 24;
            Color color = params.containsKey("color") ?
                    (Color) params.get("color") : Color.BLACK;

            return DrawingOperation.createTextOperation(text, x, y, fontName, fontSize, color);
        });
    }

    /**
     * 注册新操作类型
     */
    public void registerOperation(String operationType, OperationCreator creator) {
        operationCreators.put(operationType.toLowerCase(), creator);
    }

    /**
     * 创建图像操作
     */
    public ImageOperation createOperation(String operationType, Map<String, Object> params) {
        OperationCreator creator = operationCreators.get(operationType.toLowerCase());
        if (creator == null) {
            throw new IllegalArgumentException("未知的操作类型: " + operationType);
        }
        return creator.create(params);
    }

    /**
     * 创建图像操作（无参数）
     */
    public ImageOperation createOperation(String operationType) {
        return createOperation(operationType, new HashMap<>());
    }

    /**
     * 创建预设操作
     */
    public ImageOperation createPresetOperation(String presetName) {
        Map<String, Object> params = new HashMap<>();

        switch (presetName.toLowerCase()) {
            // 旋转操作
            case "rotate_90":
                return createOperation("rotate_90", params);
            case "rotate_180":
                return createOperation("rotate_180", params);
            case "rotate_270":
                return createOperation("rotate_270", params);
            case "rotate_90_custom":
                params.put("angle", 90);
                return createOperation("rotate_custom", params);
            case "rotate_45_custom":
                params.put("angle", 45);
                return createOperation("rotate_custom", params);

            // 翻转操作
            case "flip_horizontal":
                return createOperation("flip_horizontal", params);
            case "flip_vertical":
                return createOperation("flip_vertical", params);

            // 灰度化操作
            case "grayscale_average":
                params.put("algorithm", GrayscaleAlgorithm.AVERAGE);
                return createOperation("grayscale", params);
            case "grayscale_luminosity":
                params.put("algorithm", GrayscaleAlgorithm.LUMINOSITY);
                return createOperation("grayscale", params);
            case "grayscale_desaturation":
                params.put("algorithm", GrayscaleAlgorithm.DESATURATION);
                return createOperation("grayscale", params);

            // 艺术风格操作
            case "oil_painting":
                params.put("style", ArtisticStyle.OIL_PAINTING);
                params.put("intensity", 0.7f);
                params.put("brushSize", 5);
                params.put("detailPreserve", 0.5f);
                return createOperation("artistic_style", params);
            case "watercolor":
                params.put("style", ArtisticStyle.WATERCOLOR);
                params.put("intensity", 0.6f);
                params.put("brushSize", 3);
                params.put("detailPreserve", 0.3f);
                return createOperation("artistic_style", params);
            case "pencil_sketch":
                params.put("style", ArtisticStyle.PENCIL_SKETCH);
                params.put("intensity", 0.8f);
                params.put("brushSize", 2);
                params.put("detailPreserve", 0.2f);
                return createOperation("artistic_style", params);
            case "cartoon":
                params.put("style", ArtisticStyle.CARTOON);
                params.put("intensity", 0.9f);
                params.put("brushSize", 4);
                params.put("detailPreserve", 0.6f);
                return createOperation("artistic_style", params);
            case "mosaic_art":
                params.put("style", ArtisticStyle.MOSAIC);
                params.put("intensity", 0.5f);
                params.put("brushSize", 8);
                params.put("detailPreserve", 0.4f);
                return createOperation("artistic_style", params);

            // 绘图操作
            case "draw_red_brush":
                List<DrawingPoint> points = new ArrayList<>();
                points.add(new DrawingPoint(50, 50));
                points.add(new DrawingPoint(150, 150));
                points.add(new DrawingPoint(200, 100));
                params.put("points", points);
                params.put("color", Color.RED);
                params.put("thickness", 5);
                return createOperation("brush", params);

            case "draw_blue_text":
                params.put("text", "Hello World");
                params.put("x", 100);
                params.put("y", 100);
                params.put("fontName", "Microsoft YaHei");
                params.put("fontSize", 36);
                params.put("color", Color.BLUE);
                return createOperation("text", params);

            default:
                throw new IllegalArgumentException("未知的预设操作: " + presetName);
        }
    }

    /**
     * 获取所有支持的操作类型
     */
    public List<String> getSupportedOperations() {
        return new ArrayList<>(operationCreators.keySet());
    }

    /**
     * 获取所有预设操作名称
     */
    public List<String> getPresetOperations() {
        List<String> presets = new ArrayList<>();
        // 旋转操作
        presets.add("rotate_90");
        presets.add("rotate_180");
        presets.add("rotate_270");
        presets.add("rotate_90_custom");
        presets.add("rotate_45_custom");

        // 翻转操作
        presets.add("flip_horizontal");
        presets.add("flip_vertical");

        // 灰度化操作
        presets.add("grayscale_average");
        presets.add("grayscale_luminosity");
        presets.add("grayscale_desaturation");

        // 艺术风格操作
        presets.add("oil_painting");
        presets.add("watercolor");
        presets.add("pencil_sketch");
        presets.add("cartoon");
        presets.add("mosaic_art");

        // 绘图操作
        presets.add("draw_red_brush");
        presets.add("draw_blue_text");

        return presets;
    }

    /**
     * 创建批量处理操作
     */
    public BatchOperation createBatchOperation(List<BatchTask> tasks, BatchConfig config) {
        return new BatchOperation(config, tasks);
    }

    /**
     * 创建单操作批量处理
     */
    public BatchOperation createSingleOperationBatch(List<BatchTask> tasks, ImageOperation operation) {
        return BatchOperation.createSingleOperationBatch(tasks, operation);
    }

    /**
     * 创建多操作批量处理
     */
    public BatchOperation createSequenceBatch(List<BatchTask> tasks, List<ImageOperation> operations) {
        return BatchOperation.createSequenceBatch(tasks, operations);
    }

    /**
     * 创建优化的批量处理操作
     */
    public BatchOperation createOptimizedBatch(List<BatchTask> tasks, List<ImageOperation> operations) {
        return BatchOperation.createOptimizedBatch(tasks, operations);
    }

    /**
     * 创建任务列表
     */
    public List<BatchTask> createTasks(List<java.awt.image.BufferedImage> images,
                                       List<String> imageNames, BatchConfig config) {
        return BatchOperation.createTasks(images, imageNames, config);
    }

    /**
     * 创建画笔操作
     */
    public DrawingOperation createBrushOperation(List<DrawingPoint> points, Color color, int thickness) {
        return DrawingOperation.createBrushOperation(points, color, thickness);
    }

    /**
     * 创建文字操作
     */
    public DrawingOperation createTextOperation(String text, int x, int y,
                                                String fontName, int fontSize, Color color) {
        return DrawingOperation.createTextOperation(text, x, y, fontName, fontSize, color);
    }

    /**
     * 创建灰度化操作
     */
    public GrayscaleOperation createGrayscaleOperation() {
        return GrayscaleOperation.create();
    }

    /**
     * 创建带指定算法的灰度化操作
     */
    public GrayscaleOperation createGrayscaleOperation(GrayscaleAlgorithm algorithm) {
        return GrayscaleOperation.createWithAlgorithm(algorithm);
    }

    /**
     * 创建旋转操作
     */
    public RotateOperation createRotateOperation(RotationAngle angle) {
        return new RotateOperation(angle);
    }

    /**
     * 创建翻转操作
     */
    public FlipOperation createFlipOperation(FlipDirection direction) {
        return new FlipOperation(direction);
    }

    /**
     * 创建艺术风格操作
     */
    public ArtisticStyleOperation createArtisticStyleOperation(ArtisticStyle style,
                                                               ArtisticStyleOperation.StyleParameters parameters) {
        return new ArtisticStyleOperation(style, parameters);
    }

    /**
     * 操作创建器接口
     */
    @FunctionalInterface
    public interface OperationCreator {
        ImageOperation create(Map<String, Object> params);
    }

    /**
     * 操作构建器 - 支持链式调用
     */
    public static class OperationBuilder {
        private final Map<String, Object> params = new HashMap<>();
        private String operationType;

        public OperationBuilder(String operationType) {
            this.operationType = operationType;
        }

        public OperationBuilder setParam(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public ImageOperation build() {
            return getInstance().createOperation(operationType, params);
        }
    }

    /**
     * 批量操作构建器
     */
    public static class BatchOperationBuilder {
        private final List<BatchTask> tasks = new ArrayList<>();
        private BatchConfig config = new BatchConfig(
                BatchMode.SINGLE_OPERATION, new ArrayList<>(), 4, false, "_processed"
        );

        public BatchOperationBuilder addTask(BatchTask task) {
            tasks.add(task);
            return this;
        }

        public BatchOperationBuilder addImage(java.awt.image.BufferedImage image, String name) {
            tasks.add(new BatchTask(image, name, config));
            return this;
        }

        public BatchOperationBuilder setConfig(BatchConfig config) {
            this.config = config;
            return this;
        }

        public BatchOperationBuilder setMode(BatchMode mode) {
            this.config = new BatchConfig(
                    mode, config.getOperations(), config.getThreadCount(),
                    config.isPreserveOriginal(), config.getOutputSuffix()
            );
            return this;
        }

        public BatchOperationBuilder addOperation(ImageOperation operation) {
            List<ImageOperation> operations = new ArrayList<>(config.getOperations());
            operations.add(operation);
            this.config = new BatchConfig(
                    config.getMode(), operations, config.getThreadCount(),
                    config.isPreserveOriginal(), config.getOutputSuffix()
            );
            return this;
        }

        public BatchOperationBuilder setThreadCount(int threadCount) {
            this.config = new BatchConfig(
                    config.getMode(), config.getOperations(), threadCount,
                    config.isPreserveOriginal(), config.getOutputSuffix()
            );
            return this;
        }

        public BatchOperationBuilder setPreserveOriginal(boolean preserveOriginal) {
            this.config = new BatchConfig(
                    config.getMode(), config.getOperations(), config.getThreadCount(),
                    preserveOriginal, config.getOutputSuffix()
            );
            return this;
        }

        public BatchOperationBuilder setOutputSuffix(String outputSuffix) {
            this.config = new BatchConfig(
                    config.getMode(), config.getOperations(), config.getThreadCount(),
                    config.isPreserveOriginal(), outputSuffix
            );
            return this;
        }

        public BatchOperation build() {
            return new BatchOperation(config, tasks);
        }
    }
}