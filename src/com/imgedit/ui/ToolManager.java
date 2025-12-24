package imgedit.ui;

import imgedit.core.operations.CropOperation;
import imgedit.core.operations.DrawingOperation;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
    // 默认画笔样式
    private DrawingOperation.BrushStyle currentBrushStyle = new DrawingOperation.BrushStyle(
            java.awt.Color.BLACK, 3, 1.0f);

    // 【新增】独立存储文字大小，默认 40
    private int currentTextSize = 40;

    private boolean isDrawing = false;

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
        clearSelection(); // 切换模式时清理
        controller.updateStatus("切换到模式: " + mode.toString());
    }

    // ==================== 鼠标事件处理 ====================

    public void handleMousePressed(double x, double y, Canvas canvas) {
        if (controller.getImageManager().getCurrentImage() == null) return;

        double[] imageCoords = convertToImageCoordinates(x, y);

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

    // 【重点修复】使用了 try-finally 确保画布一定会清理
    public void handleMouseReleased(double x, double y) {
        if (controller.getImageManager().getCurrentImage() == null) return;

        double[] imageCoords = convertToImageCoordinates(x, y);

        try {
            switch (currentToolMode) {
                case CROP:
                    if (isSelectingCrop) {
                        endCropSelection(imageCoords[0], imageCoords[1]);
                        isSelectingCrop = false;
                        // 自动执行裁剪
                        if (cropSelection != null &&
                                cropSelection.getWidth() > 0 &&
                                cropSelection.getHeight() > 0) {
                            applyCrop();
                        } else {
                            controller.updateStatus("裁剪区域无效，请重新选择");
                        }
                    }
                    break;

                case DRAW_BRUSH:
                    if (isDrawing) {
                        endDrawing();
                        isDrawing = false;
                    }
                    break;

                case DRAW_RECT:
                case DRAW_CIRCLE:
                    if (isDrawing) {
                        endShapeDrawing(imageCoords[0], imageCoords[1]);
                        isDrawing = false;
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            controller.showError("操作错误", e.getMessage());
        } finally {
            // 【关键】无论发生什么，强制清理预览画布，防止出现两个圈
            clearSelectionCanvasImmediately();
            resetDrawingStates();
        }
    }

    public void handleTextClick(double x, double y) {
        double[] imageCoords = convertToImageCoordinates(x, y);
        addTextAtPosition((int)imageCoords[0], (int)imageCoords[1]);
    }

    // ==================== 辅助方法 ====================

    // 【修复】更彻底的清理画布方法
    private void clearSelectionCanvasImmediately() {
        Canvas canvas = controller.getSelectionCanvas();
        if (canvas != null) {
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // 1. 清除内容 (清除比画布大一点的区域，防止边缘残留)
            gc.clearRect(-10, -10, canvas.getWidth() + 20, canvas.getHeight() + 20);

            // 2. 填充透明
            gc.setFill(Color.TRANSPARENT);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // 3. 重置路径 (防止路径残留)
            gc.beginPath();

            // 4. 重置大小
            canvas.setWidth(0);
            canvas.setHeight(0);
        }
    }

    private void resetDrawingStates() {
        currentBrushPoints.clear();
        isDrawing = false;
        isSelectingCrop = false;
    }

    private double[] convertToImageCoordinates(double screenX, double screenY) {
        if (controller.getImageManager().getCurrentImage() == null) {
            return new double[]{screenX, screenY};
        }
        return controller.getImageManager().screenToImageCoordinates(screenX, screenY);
    }

    // ==================== 裁剪逻辑 ====================

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

        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) return;

        canvas.setWidth(displayBounds.getWidth());
        canvas.setHeight(displayBounds.getHeight());

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
    }

    private void endCropSelection(double x, double y) {
        if (cropSelection == null) return;
        double rectX = Math.min(cropStartX, x);
        double rectY = Math.min(cropStartY, y);
        double width = Math.abs(x - cropStartX);
        double height = Math.abs(y - cropStartY);
        cropSelection.setRect(rectX, rectY, width, height);
    }

    public void applyCrop() {
        if (cropSelection == null || controller.getImageManager().getCurrentImage() == null) {
            return;
        }

        int x = (int) Math.round(cropSelection.getX());
        int y = (int) Math.round(cropSelection.getY());
        int width = (int) Math.round(cropSelection.getWidth());
        int height = (int) Math.round(cropSelection.getHeight());

        if (width <= 2 || height <= 2) {
            controller.showWarning("区域太小", "裁剪区域至少需要3x3像素");
            return;
        }

        try {
            CropOperation operation = new CropOperation(x, y, width, height);
            controller.getImageManager().applyOperation(operation, "裁剪图片");
            cropSelection = null;

            // 【关键修改】强制立即清理画布，防止裁剪后蓝框残留在新图上
            clearSelectionCanvasImmediately();

            // 清理动作在 handleMouseReleased 的 finally 块中也会处理，但这里显式调用更安全
            controller.updateStatus(String.format("裁剪完成: %dx%d", width, height));
        } catch (Exception e) {
            controller.showError("裁剪失败", e.getMessage());
        }
    }

    // ==================== 绘图逻辑 (画笔) ====================

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

        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) return;

        canvas.setWidth(displayBounds.getWidth());
        canvas.setHeight(displayBounds.getHeight());

        java.awt.Color color = currentBrushStyle.getColor();
        gc.setStroke(Color.rgb(color.getRed(), color.getGreen(), color.getBlue(),
                color.getAlpha() / 255.0));

        // 计算缩放比例
        double imageWidth = controller.getImageManager().getCurrentImage().getWidth();
        double scaleX = displayBounds.getWidth() / imageWidth;

        gc.setLineWidth(currentBrushStyle.getThickness() * scaleX);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // 绘制线条
        gc.beginPath();
        if (!currentBrushPoints.isEmpty()) {
            DrawingOperation.DrawingPoint start = currentBrushPoints.get(0);
            double[] startPt = imageToCanvasCoordinates(start.getX(), start.getY(), canvas);
            gc.moveTo(startPt[0], startPt[1]);

            for (int i = 1; i < currentBrushPoints.size(); i++) {
                DrawingOperation.DrawingPoint p = currentBrushPoints.get(i);
                double[] pt = imageToCanvasCoordinates(p.getX(), p.getY(), canvas);
                gc.lineTo(pt[0], pt[1]);
            }
        }
        gc.stroke();
    }

    private void endDrawing() {
        if (currentBrushPoints.size() >= 2) {
            applyCurrentDrawing();
        }
        currentBrushPoints.clear();
    }

    private void applyCurrentDrawing() {
        if (currentBrushPoints.size() < 2) return;

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                DrawingOperation.DrawingType.BRUSH,
                new ArrayList<>(currentBrushPoints),
                null,
                currentBrushStyle,
                null
        );

        DrawingOperation operation = new DrawingOperation(element);
        controller.getImageManager().applyOperation(operation, "画笔绘制");
        controller.updateStatus("绘图已应用");
    }

    // ==================== 绘图逻辑 (形状) ====================

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

        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) return;

        canvas.setWidth(displayBounds.getWidth());
        canvas.setHeight(displayBounds.getHeight());

        DrawingOperation.DrawingPoint p1 = currentBrushPoints.get(0);
        DrawingOperation.DrawingPoint p2 = currentBrushPoints.get(1);

        double[] pt1 = imageToCanvasCoordinates(p1.getX(), p1.getY(), canvas);
        double[] pt2 = imageToCanvasCoordinates(p2.getX(), p2.getY(), canvas);

        double x = Math.min(pt1[0], pt2[0]);
        double y = Math.min(pt1[1], pt2[1]);
        double width = Math.abs(pt2[0] - pt1[0]);
        double height = Math.abs(pt2[1] - pt1[1]);

        java.awt.Color color = currentBrushStyle.getColor();
        gc.setStroke(Color.rgb(color.getRed(), color.getGreen(), color.getBlue(),
                color.getAlpha() / 255.0));

        // 计算粗细的显示比例
        double imageWidth = controller.getImageManager().getCurrentImage().getWidth();
        double scaleX = displayBounds.getWidth() / imageWidth;
        gc.setLineWidth(currentBrushStyle.getThickness() * scaleX);

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
            case DRAW_RECT: type = DrawingOperation.DrawingType.RECTANGLE; break;
            case DRAW_CIRCLE: type = DrawingOperation.DrawingType.CIRCLE; break;
            default: return;
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
    }

    // ==================== 文字工具 ====================

    private void addTextAtPosition(int x, int y) {
        // 调用 DialogManager 显示输入框
        controller.getDialogManager().showTextInputDialog("添加文字", "输入要添加的文字:",
                "", text -> {
                    if (text != null && !text.isEmpty()) {

                        // 【修改】使用独立的文字大小变量，不再依赖画笔粗细
                        int fontSize = currentTextSize;
                        if (fontSize < 10) fontSize = 10;

                        DrawingOperation.TextStyle textStyle = new DrawingOperation.TextStyle(
                                getSystemChineseFont(),
                                fontSize,
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
        return "Microsoft YaHei";
    }

    // ==================== 其他工具 ====================

    public void clearDrawing() {
        currentBrushPoints.clear();
        clearSelectionCanvasImmediately();
        controller.updateStatus("当前绘图已清除");
    }

    private void clearSelection() {
        cropSelection = null;
        currentBrushPoints.clear();
        isDrawing = false;
        clearSelectionCanvasImmediately();
    }

    private double[] imageToCanvasCoordinates(double imageX, double imageY, Canvas canvas) {
        javafx.geometry.Bounds displayBounds = controller.getImageManager().getImageDisplayBounds();
        double imageWidth = controller.getImageManager().getCurrentImage().getWidth();
        double imageHeight = controller.getImageManager().getCurrentImage().getHeight();

        if (displayBounds.getWidth() <= 0 || displayBounds.getHeight() <= 0) {
            return new double[]{imageX, imageY};
        }

        double scaleX = displayBounds.getWidth() / imageWidth;
        double scaleY = displayBounds.getHeight() / imageHeight;

        return new double[]{imageX * scaleX, imageY * scaleY};
    }

    /**
     * 设置画笔颜色
     */
    public void setBrushColor(Color fxColor) {
        java.awt.Color awtColor = new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity()
        );

        this.currentBrushStyle = new DrawingOperation.BrushStyle(
                awtColor,
                this.currentBrushStyle.getThickness(),
                this.currentBrushStyle.getOpacity()
        );
    }

    /**
     * 设置画笔粗细
     */
    public void setBrushSize(Number size) {
        this.currentBrushStyle = new DrawingOperation.BrushStyle(
                this.currentBrushStyle.getColor(),
                size.intValue(),
                this.currentBrushStyle.getOpacity()
        );
    }

    /**
     * 【新增】设置文字大小
     */
    public void setTextSize(int size) {
        this.currentTextSize = size;
    }

    public DrawingOperation.BrushStyle getCurrentBrushStyle() {
        return currentBrushStyle;
    }
}