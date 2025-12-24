package imgedit.ui;

import imgedit.core.operations.CropOperation;
import imgedit.core.operations.DrawingOperation;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具管理器 - 负责绘图、裁剪等工具
 */
public class ToolManager {

    public enum ToolMode {
        SELECT, CROP, DRAW_BRUSH, DRAW_TEXT, DRAW_RECT, DRAW_CIRCLE
    }

    private final EditorController controller;
    private ToolMode currentToolMode = ToolMode.SELECT;

    // 裁剪相关
    private Rectangle cropSelection = null;
    private boolean isSelectingCrop = false;
    private double cropStartX, cropStartY;

    // 绘图相关
    private List<DrawingOperation.DrawingPoint> currentBrushPoints = new ArrayList<>();
    private DrawingOperation.BrushStyle currentBrushStyle = new DrawingOperation.BrushStyle(
            java.awt.Color.BLACK, 3, 1.0f);
    private boolean isDrawing = false;  // 新增：跟踪是否正在绘图

    // 形状绘制
    private double shapeStartX, shapeStartY;

    public ToolManager(EditorController controller) {
        this.controller = controller;
    }

    public ToolMode getCurrentToolMode() {
        return currentToolMode;
    }

    public void setToolMode(ToolMode mode) {
        currentToolMode = mode;
        clearSelection();
        controller.updateStatus("切换到模式: " + mode.toString());
    }

    public void handleMousePressed(double x, double y, Canvas canvas) {
        if (controller.getImageManager().getCurrentImage() == null) return;
        System.out.println("鼠标按下 - 屏幕坐标: (" + x + ", " + y + ")");
        double[] imageCoords = convertToImageCoordinates(x, y);
        System.out.println("转换后 - 图像坐标: (" + imageCoords[0] + ", " + imageCoords[1] + ")");
        switch (currentToolMode) {
            case CROP:
                startCropSelection(imageCoords[0], imageCoords[1]);
                isSelectingCrop = true;
                break;

            case DRAW_BRUSH:
                startDrawing(imageCoords[0], imageCoords[1]);
                isDrawing = true;
                break;

            case DRAW_RECT:
            case DRAW_CIRCLE:
                startShapeDrawing(imageCoords[0], imageCoords[1]);
                isDrawing = true;
                break;
        }
    }

    public void handleMouseDragged(double x, double y, Canvas canvas) {
        if (controller.getImageManager().getCurrentImage() == null) return;

        double[] imageCoords = convertToImageCoordinates(x, y);

        switch (currentToolMode) {
            case CROP:
                if (isSelectingCrop) {
                    updateCropSelection(imageCoords[0], imageCoords[1], canvas);
                }
                break;

            case DRAW_BRUSH:
                if (isDrawing) {
                    continueDrawing(imageCoords[0], imageCoords[1], canvas);
                }
                break;

            case DRAW_RECT:
            case DRAW_CIRCLE:
                if (isDrawing) {
                    updateShapeDrawing(imageCoords[0], imageCoords[1], canvas);
                }
                break;
        }
    }

    // 修改 handleMouseReleased 方法
    public void handleMouseReleased(double x, double y) {
        if (controller.getImageManager().getCurrentImage() == null) return;

        double[] imageCoords = convertToImageCoordinates(x, y);

        switch (currentToolMode) {
            case CROP:
                if (isSelectingCrop) {
                    endCropSelection(imageCoords[0], imageCoords[1]);
                    isSelectingCrop = false;

                    // 自动执行裁剪，而不是显示确认按钮
                    if (cropSelection != null &&
                            cropSelection.getWidth() > 0 &&
                            cropSelection.getHeight() > 0) {
                        applyCrop();
                    } else {
                        controller.updateStatus("裁剪区域无效，请重新选择");
                    }

                    // 重要：立即清理画布
                    clearSelectionCanvasImmediately();
                }
                break;

            case DRAW_BRUSH:
                if (isDrawing) {
                    endDrawing();
                    isDrawing = false;

                    // 重要：立即清理画布
                    clearSelectionCanvasImmediately();
                }
                break;

            case DRAW_RECT:
            case DRAW_CIRCLE:
                if (isDrawing) {
                    endShapeDrawing(imageCoords[0], imageCoords[1]);
                    isDrawing = false;

                    // 重要：立即清理画布
                    clearSelectionCanvasImmediately();
                }
                break;
        }

        // 额外清理：确保所有绘图状态都重置
        resetDrawingStates();
    }

    // 新增：立即清理画布的方法
    private void clearSelectionCanvasImmediately() {
        Platform.runLater(() -> {
            Canvas canvas = controller.getSelectionCanvas();
            if (canvas != null) {
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // 用透明颜色填充整个画布
                gc.setFill(Color.TRANSPARENT);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // 重置画布大小
                canvas.setWidth(0);
                canvas.setHeight(0);

                System.out.println("立即清理画布完成");
            }
        });
    }

    // 新增：重置所有绘图状态
    private void resetDrawingStates() {
        currentBrushPoints.clear();
        isDrawing = false;
        isDrawing = false;
    }

    public void handleTextClick(double x, double y) {
        double[] imageCoords = convertToImageCoordinates(x, y);
        addTextAtPosition((int)imageCoords[0], (int)imageCoords[1]);
    }

    private double[] convertToImageCoordinates(double screenX, double screenY) {
        if (controller.getImageManager().getCurrentImage() == null) {
            return new double[]{screenX, screenY};
        }

        // 使用 ImageManager 的坐标转换方法
        return controller.getImageManager().screenToImageCoordinates(screenX, screenY);
    }

    private void startCropSelection(double x, double y) {
        cropStartX = x;
        cropStartY = y;
        cropSelection = new Rectangle((int)x, (int)y, 0, 0);
    }

    private void updateCropSelection(double x, double y, Canvas canvas) {
        if (cropSelection == null) return;

        double rectX = Math.min(cropStartX, x);
        double rectY = Math.min(cropStartY, y);
        double width = Math.abs(x - cropStartX);
        double height = Math.abs(y - cropStartY);

        cropSelection.setRect(rectX, rectY, width, height);
        drawSelectionRect(canvas, rectX, rectY, width, height);
    }

    private void drawSelectionRect(Canvas canvas, double imageX, double imageY, double imageWidth, double imageHeight) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 获取图像显示区域并设置 Canvas 大小
        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) return;

        canvas.setWidth(displayBounds.getWidth());
        canvas.setHeight(displayBounds.getHeight());

        // 转换图像坐标到 Canvas 坐标
        double[] startCoords = imageToCanvasCoordinates(imageX, imageY, canvas);
        double[] endCoords = imageToCanvasCoordinates(imageX + imageWidth, imageY + imageHeight, canvas);

        double canvasX = startCoords[0];
        double canvasY = startCoords[1];
        double canvasWidth = endCoords[0] - startCoords[0];
        double canvasHeight = endCoords[1] - startCoords[1];

        // 绘制半透明填充
        gc.setFill(Color.rgb(0, 150, 255, 0.1));
        gc.fillRect(canvasX, canvasY, canvasWidth, canvasHeight);

        // 绘制边框
        gc.setStroke(Color.rgb(0, 150, 255, 0.8));
        gc.setLineWidth(2);
        gc.strokeRect(canvasX, canvasY, canvasWidth, canvasHeight);

        // 绘制角点
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.rgb(0, 150, 255, 0.8));

        double cornerSize = 8;

        // 左上角
        gc.fillRect(canvasX - cornerSize/2, canvasY - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(canvasX - cornerSize/2, canvasY - cornerSize/2, cornerSize, cornerSize);

        // 右上角
        gc.fillRect(canvasX + canvasWidth - cornerSize/2, canvasY - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(canvasX + canvasWidth - cornerSize/2, canvasY - cornerSize/2, cornerSize, cornerSize);

        // 左下角
        gc.fillRect(canvasX - cornerSize/2, canvasY + canvasHeight - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(canvasX - cornerSize/2, canvasY + canvasHeight - cornerSize/2, cornerSize, cornerSize);

        // 右下角
        gc.fillRect(canvasX + canvasWidth - cornerSize/2, canvasY + canvasHeight - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(canvasX + canvasWidth - cornerSize/2, canvasY + canvasHeight - cornerSize/2, cornerSize, cornerSize);
    }

    private void endCropSelection(double x, double y) {
        if (cropSelection == null) return;

        double rectX = Math.min(cropStartX, x);
        double rectY = Math.min(cropStartY, y);
        double width = Math.abs(x - cropStartX);
        double height = Math.abs(y - cropStartY);

        cropSelection.setRect(rectX, rectY, width, height);
        controller.updateStatus(String.format("裁剪区域: (%.0f, %.0f) %.0f×%.0f",
                rectX, rectY, width, height));
    }

    public void applyCrop() {
        if (cropSelection == null || controller.getImageManager().getCurrentImage() == null) {
            controller.updateStatus("没有选择裁剪区域");
            return;
        }

        // 转换为整数
        int x = (int) Math.round(cropSelection.getX());
        int y = (int) Math.round(cropSelection.getY());
        int width = (int) Math.round(cropSelection.getWidth());
        int height = (int) Math.round(cropSelection.getHeight());

        // 确保最小尺寸
        if (width <= 2 || height <= 2) {
            controller.showWarning("区域太小", "裁剪区域至少需要3x3像素");
            return;
        }

        // 创建裁剪操作
        try {
            CropOperation operation = new CropOperation(x, y, width, height);
            controller.getImageManager().applyOperation(operation, "裁剪图片");

            // 清除选择
            cropSelection = null;

            // 清理Canvas
            Canvas canvas = controller.getSelectionCanvas();
            if (canvas != null) {
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            }

            controller.updateStatus(String.format("裁剪完成: %dx%d", width, height));

        } catch (Exception e) {
            controller.showError("裁剪失败", e.getMessage());
        }
    }

    private void clearCanvasImmediately(Canvas canvas) {
        if (canvas == null) {
            System.out.println("警告：canvas为null，无法清理");
            return;
        }

        // 在JavaFX应用线程中执行清理
        Platform.runLater(() -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // 方法1：清除整个画布
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // 方法2：用透明颜色填充
            gc.setFill(Color.TRANSPARENT);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // 方法3：重置画布大小（有时需要）
            canvas.setWidth(0);
            canvas.setHeight(0);

            // 方法4：强制重绘画布
            //canvas.requestLayout();

            System.out.println("画布已清理，大小: " + canvas.getWidth() + "x" + canvas.getHeight());
        });
    }
    private void showCropConfirmButton() {
        // 在实际应用中，需要从UI中查找确认按钮并显示
    }

    private void hideCropConfirmButton() {
        // 在实际应用中，需要从UI中查找确认按钮并隐藏
    }

    private void startDrawing(double x, double y) {
        currentBrushPoints.clear();
        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
    }

    private void continueDrawing(double x, double y, Canvas canvas) {
        if (currentBrushPoints.isEmpty()) return;

        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
        drawBrushPreview(canvas);
    }

    private void drawBrushPreview(Canvas canvas) {
        if (currentBrushPoints.size() < 2) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 获取图像显示区域并设置 Canvas 大小
        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) return;

        canvas.setWidth(displayBounds.getWidth());
        canvas.setHeight(displayBounds.getHeight());

        // 设置画笔样式
        java.awt.Color color = currentBrushStyle.getColor();
        gc.setStroke(Color.rgb(color.getRed(), color.getGreen(), color.getBlue(),
                color.getAlpha() / 255.0));

        // 获取图像尺寸以计算缩放比例
        double imageWidth = controller.getImageManager().getCurrentImage().getWidth();
        double imageHeight = controller.getImageManager().getCurrentImage().getHeight();
        double scaleX = displayBounds.getWidth() / imageWidth;
        double scaleY = displayBounds.getHeight() / imageHeight;
        double scale = Math.min(scaleX, scaleY);

        gc.setLineWidth(currentBrushStyle.getThickness() * scale);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // 绘制线条
        for (int i = 0; i < currentBrushPoints.size() - 1; i++) {
            DrawingOperation.DrawingPoint p1 = currentBrushPoints.get(i);
            DrawingOperation.DrawingPoint p2 = currentBrushPoints.get(i + 1);

            // 转换为 Canvas 坐标
            double x1 = p1.getX() * scaleX;
            double y1 = p1.getY() * scaleY;
            double x2 = p2.getX() * scaleX;
            double y2 = p2.getY() * scaleY;

            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    private void endDrawing() {
        if (currentBrushPoints.size() >= 2) {
            applyCurrentDrawing();
        }
        currentBrushPoints.clear();
    }

    private void applyCurrentDrawing() {
        if (currentBrushPoints.size() < 2) {
            controller.showWarning("绘图", "请先绘制一些内容");
            return;
        }

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                DrawingOperation.DrawingType.BRUSH,
                new ArrayList<>(currentBrushPoints),
                null,
                currentBrushStyle,
                null
        );

        DrawingOperation operation = new DrawingOperation(element);
        controller.getImageManager().applyOperation(operation, "画笔绘制");

        currentBrushPoints.clear();
        controller.updateStatus("绘图已应用");
    }

    private void startShapeDrawing(double x, double y) {
        shapeStartX = x;
        shapeStartY = y;
        currentBrushPoints.clear();
        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
    }

    private void updateShapeDrawing(double x, double y, Canvas canvas) {
        if (currentBrushPoints.size() < 2) return;

        currentBrushPoints.set(1, new DrawingOperation.DrawingPoint((int)x, (int)y));
        drawShapePreview(canvas);
    }

    private void drawShapePreview(Canvas canvas) {
        if (currentBrushPoints.size() < 2) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 获取图像显示区域并设置 Canvas 大小
        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) return;

        canvas.setWidth(displayBounds.getWidth());
        canvas.setHeight(displayBounds.getHeight());

        DrawingOperation.DrawingPoint p1 = currentBrushPoints.get(0);
        DrawingOperation.DrawingPoint p2 = currentBrushPoints.get(1);

        // 获取图像尺寸以计算缩放比例
        double imageWidth = controller.getImageManager().getCurrentImage().getWidth();
        double imageHeight = controller.getImageManager().getCurrentImage().getHeight();
        double scaleX = displayBounds.getWidth() / imageWidth;
        double scaleY = displayBounds.getHeight() / imageHeight;

        // 转换为 Canvas 坐标
        double x1 = p1.getX() * scaleX;
        double y1 = p1.getY() * scaleY;
        double x2 = p2.getX() * scaleX;
        double y2 = p2.getY() * scaleY;

        double x = Math.min(x1, x2);
        double y = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);

        // 设置画笔样式
        java.awt.Color color = currentBrushStyle.getColor();
        gc.setStroke(Color.rgb(color.getRed(), color.getGreen(), color.getBlue(),
                color.getAlpha() / 255.0));
        gc.setLineWidth(currentBrushStyle.getThickness() * Math.min(scaleX, scaleY));
        gc.setLineDashes(0);

        switch (currentToolMode) {
            case DRAW_RECT:
                gc.strokeRect(x, y, width, height);
                break;
            case DRAW_CIRCLE:
                double radius = Math.min(width, height) / 2;
                double centerX = x + width / 2;
                double centerY = y + height / 2;
                gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                break;
        }
    }

    private void endShapeDrawing(double x, double y) {
        if (currentBrushPoints.size() >= 2) {
            currentBrushPoints.set(1, new DrawingOperation.DrawingPoint((int)x, (int)y));
            applyCurrentShape();
        }
        currentBrushPoints.clear();
    }

    private void applyCurrentShape() {
        if (currentBrushPoints.size() < 2) return;

        DrawingOperation.DrawingType type;
        switch (currentToolMode) {
            case DRAW_RECT:
                type = DrawingOperation.DrawingType.RECTANGLE;
                break;
            case DRAW_CIRCLE:
                type = DrawingOperation.DrawingType.CIRCLE;
                break;
            default:
                return;
        }

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                type,
                new ArrayList<>(currentBrushPoints),
                null,
                currentBrushStyle,
                null
        );

        DrawingOperation operation = new DrawingOperation(element);
        controller.getImageManager().applyOperation(operation,
                type == DrawingOperation.DrawingType.RECTANGLE ? "绘制矩形" : "绘制圆形");

        currentBrushPoints.clear();
    }

    private void addTextAtPosition(int x, int y) {
        controller.getDialogManager().showTextInputDialog("添加文字", "输入要添加的文字:",
                "", text -> {
                    if (text != null && !text.isEmpty()) {
                        DrawingOperation.TextStyle textStyle = new DrawingOperation.TextStyle(
                                getSystemChineseFont(),
                                24,
                                currentBrushStyle.getColor(),
                                false, false, false);

                        List<DrawingOperation.DrawingPoint> points = new ArrayList<>();
                        points.add(new DrawingOperation.DrawingPoint(x, y));

                        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                                DrawingOperation.DrawingType.TEXT,
                                points,
                                text,
                                null,
                                textStyle);

                        DrawingOperation operation = new DrawingOperation(element);
                        controller.getImageManager().applyOperation(operation, "添加文字");
                    }
                });
    }

    private String getSystemChineseFont() {
        String[] chineseFonts = {
                "Microsoft YaHei",
                "PingFang SC",
                "Noto Sans CJK SC",
                "SimHei",
                "SimSun",
                "NSimSun",
                "KaiTi",
                "FangSong",
                "Microsoft JhengHei",
                "STXihei",
                "STSong",
                "STKaiti",
                "STFangsong"
        };

        // 在实际应用中，需要检查系统字体
        return "Microsoft YaHei";
    }

    private double[] imageToCanvasCoordinates(double imageX, double imageY, Canvas canvas) {
        // 获取图像显示区域
        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();

        // 获取图像原始尺寸
        double imageWidth = controller.getImageManager().getCurrentImage().getWidth();
        double imageHeight = controller.getImageManager().getCurrentImage().getHeight();

        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) {
            return new double[]{imageX, imageY};
        }

        // 计算缩放比例
        double scaleX = displayBounds.getWidth() / imageWidth;
        double scaleY = displayBounds.getHeight() / imageHeight;

        // 转换为 Canvas 坐标
        double canvasX = imageX * scaleX;
        double canvasY = imageY * scaleY;

        return new double[]{canvasX, canvasY};
    }

    public void clearDrawing() {
        currentBrushPoints.clear();
        clearDrawingPreview(controller.getSelectionCanvas());
        controller.updateStatus("当前绘图已清除");
    }

    // 新增：清理绘图预览
    private void clearDrawingPreview(Canvas canvas) {
        if (canvas != null) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }
    // 新增：统一的画布清理方法
    private void clearSelectionCanvas() {
        Canvas canvas = controller.getSelectionCanvas();
        if (canvas != null) {
            System.out.println("清理选择画布 - 大小: " + canvas.getWidth() + "x" + canvas.getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            // 重置画布大小
            canvas.setWidth(0);
            canvas.setHeight(0);
        } else {
            System.out.println("警告：选择画布为null！");
        }
    }

    private void clearSelection() {
        cropSelection = null;
        currentBrushPoints.clear();
        isDrawing = false;
        clearDrawingPreview(controller.getSelectionCanvas());
    }
}