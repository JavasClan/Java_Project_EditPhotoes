package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 绘图和文字操作类
 * 功能：在图片上绘制图形和添加文字
 */
public class DrawingOperation implements ImageOperation {

    /**
     * 绘图元素类型
     */
    public enum DrawingType {
        BRUSH("画笔"),
        TEXT("文字"),
        RECTANGLE("矩形"),
        CIRCLE("圆形"),
        LINE("直线"),
        ARROW("箭头");

        private final String description;

        DrawingType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 画笔样式
     */
    public static class BrushStyle {
        private final Color color;
        private final int thickness;
        private final float opacity;

        public BrushStyle(Color color, int thickness, float opacity) {
            this.color = color != null ? color : Color.BLACK;
            this.thickness = Math.max(1, Math.min(50, thickness));
            this.opacity = Math.max(0.1f, Math.min(1.0f, opacity));
        }

        public Color getColor() { return color; }
        public int getThickness() { return thickness; }
        public float getOpacity() { return opacity; }
    }

    /**
     * 文字样式
     */
    public static class TextStyle {
        private final String fontName;
        private final int fontSize;
        private final Color color;
        private final boolean bold;
        private final boolean italic;
        private final boolean underline;

        public TextStyle(String fontName, int fontSize, Color color,
                         boolean bold, boolean italic, boolean underline) {
            this.fontName = fontName != null ? fontName : "Arial";
            this.fontSize = Math.max(8, Math.min(100, fontSize));
            this.color = color != null ? color : Color.BLACK;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
        }

        public String getFontName() { return fontName; }
        public int getFontSize() { return fontSize; }
        public Color getColor() { return color; }
        public boolean isBold() { return bold; }
        public boolean isItalic() { return italic; }
        public boolean isUnderline() { return underline; }
    }

    /**
     * 绘图点
     */
    public static class DrawingPoint {
        private final int x;
        private final int y;

        public DrawingPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    /**
     * 绘图元素
     */
    public static class DrawingElement {
        private final DrawingType type;
        private final List<DrawingPoint> points;
        private final String text;
        private final BrushStyle brushStyle;
        private final TextStyle textStyle;

        public DrawingElement(DrawingType type, List<DrawingPoint> points,
                              String text, BrushStyle brushStyle, TextStyle textStyle) {
            this.type = type;
            this.points = points != null ? new ArrayList<>(points) : new ArrayList<>();
            this.text = text;
            this.brushStyle = brushStyle;
            this.textStyle = textStyle;
        }

        public DrawingType getType() { return type; }
        public List<DrawingPoint> getPoints() { return points; }
        public String getText() { return text; }
        public BrushStyle getBrushStyle() { return brushStyle; }
        public TextStyle getTextStyle() { return textStyle; }
    }

    private final List<DrawingElement> drawingElements;

    /**
     * 创建绘图操作
     */
    public DrawingOperation(List<DrawingElement> drawingElements) {
        this.drawingElements = new ArrayList<>(drawingElements);
    }

    /**
     * 创建单个绘图元素的操作
     */
    public DrawingOperation(DrawingElement element) {
        this.drawingElements = new ArrayList<>();
        this.drawingElements.add(element);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            BufferedImage result = new BufferedImage(
                    image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

            // 绘制原始图像
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(image, 0, 0, null);

            // 应用抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 绘制所有元素
            for (DrawingElement element : drawingElements) {
                drawElement(g2d, element);
            }

            g2d.dispose();
            return result;

        } catch (Exception e) {
            throw new ImageProcessingException("绘图操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 绘制单个元素
     */
    private void drawElement(Graphics2D g2d, DrawingElement element) {
        switch (element.getType()) {
            case BRUSH:
                drawBrush(g2d, element);
                break;
            case TEXT:
                drawText(g2d, element);
                break;
            case RECTANGLE:
                drawRectangle(g2d, element);
                break;
            case CIRCLE:
                drawCircle(g2d, element);
                break;
            case LINE:
                drawLine(g2d, element);
                break;
            case ARROW:
                drawArrow(g2d, element);
                break;
        }
    }

    /**
     * 绘制画笔轨迹
     */
    private void drawBrush(Graphics2D g2d, DrawingElement element) {
        if (element.getPoints().size() < 2) return;

        BrushStyle style = element.getBrushStyle();
        if (style == null) return;

        // 设置画笔样式
        g2d.setColor(style.getColor());
        g2d.setStroke(new BasicStroke(style.getThickness(),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 设置透明度
        g2d.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, style.getOpacity()));

        // 绘制线条
        List<DrawingPoint> points = element.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            DrawingPoint p1 = points.get(i);
            DrawingPoint p2 = points.get(i + 1);
            g2d.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        // 恢复透明度
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * 绘制文字
     */
    private void drawText(Graphics2D g2d, DrawingElement element) {
        if (element.getText() == null || element.getText().isEmpty()) return;
        if (element.getPoints().isEmpty()) return;
        if (element.getTextStyle() == null) return;

        TextStyle style = element.getTextStyle();
        DrawingPoint point = element.getPoints().get(0);

        // 创建字体
        int fontStyle = Font.PLAIN;
        if (style.isBold() && style.isItalic()) {
            fontStyle = Font.BOLD | Font.ITALIC;
        } else if (style.isBold()) {
            fontStyle = Font.BOLD;
        } else if (style.isItalic()) {
            fontStyle = Font.ITALIC;
        }

        Font font = new Font(style.getFontName(), fontStyle, style.getFontSize());
        g2d.setFont(font);
        g2d.setColor(style.getColor());

        // 创建带属性的字符串（用于下划线）
        AttributedString attributedString = new AttributedString(element.getText());
        attributedString.addAttribute(TextAttribute.FONT, font);

        if (style.isUnderline()) {
            attributedString.addAttribute(TextAttribute.UNDERLINE,
                    TextAttribute.UNDERLINE_ON, 0, element.getText().length());
        }

        // 绘制文字
        g2d.drawString(attributedString.getIterator(), point.getX(), point.getY());
    }

    /**
     * 绘制矩形
     */
    private void drawRectangle(Graphics2D g2d, DrawingElement element) {
        if (element.getPoints().size() < 2) return;
        if (element.getBrushStyle() == null) return;

        DrawingPoint p1 = element.getPoints().get(0);
        DrawingPoint p2 = element.getPoints().get(1);

        int x = Math.min(p1.getX(), p2.getX());
        int y = Math.min(p1.getY(), p2.getY());
        int width = Math.abs(p2.getX() - p1.getX());
        int height = Math.abs(p2.getY() - p1.getY());

        BrushStyle style = element.getBrushStyle();
        g2d.setColor(style.getColor());
        g2d.setStroke(new BasicStroke(style.getThickness()));

        g2d.drawRect(x, y, width, height);
    }

    /**
     * 绘制圆形
     */
    private void drawCircle(Graphics2D g2d, DrawingElement element) {
        if (element.getPoints().size() < 2) return;
        if (element.getBrushStyle() == null) return;

        DrawingPoint p1 = element.getPoints().get(0);
        DrawingPoint p2 = element.getPoints().get(1);

        int x = Math.min(p1.getX(), p2.getX());
        int y = Math.min(p1.getY(), p2.getY());
        int width = Math.abs(p2.getX() - p1.getX());
        int height = Math.abs(p2.getY() - p1.getY());

        // 确保圆形
        int size = Math.min(width, height);

        BrushStyle style = element.getBrushStyle();
        g2d.setColor(style.getColor());
        g2d.setStroke(new BasicStroke(style.getThickness()));

        g2d.drawOval(x, y, size, size);
    }

    /**
     * 绘制直线
     */
    private void drawLine(Graphics2D g2d, DrawingElement element) {
        if (element.getPoints().size() < 2) return;
        if (element.getBrushStyle() == null) return;

        DrawingPoint p1 = element.getPoints().get(0);
        DrawingPoint p2 = element.getPoints().get(1);

        BrushStyle style = element.getBrushStyle();
        g2d.setColor(style.getColor());
        g2d.setStroke(new BasicStroke(style.getThickness()));

        g2d.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * 绘制箭头
     */
    private void drawArrow(Graphics2D g2d, DrawingElement element) {
        if (element.getPoints().size() < 2) return;
        if (element.getBrushStyle() == null) return;

        DrawingPoint p1 = element.getPoints().get(0);
        DrawingPoint p2 = element.getPoints().get(1);

        BrushStyle style = element.getBrushStyle();
        g2d.setColor(style.getColor());
        g2d.setStroke(new BasicStroke(style.getThickness()));

        // 绘制直线
        g2d.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());

        // 绘制箭头头部
        drawArrowHead(g2d, p1.getX(), p1.getY(), p2.getX(), p2.getY(),
                style.getThickness() * 2);
    }

    /**
     * 绘制箭头头部
     */
    private void drawArrowHead(Graphics2D g2d, int x1, int y1, int x2, int y2, int size) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        double len = Math.sqrt(dx * dx + dy * dy);

        if (len == 0) return;

        // 计算箭头头部三个点
        double x3 = x2 - size * Math.cos(angle - Math.PI / 6);
        double y3 = y2 - size * Math.sin(angle - Math.PI / 6);
        double x4 = x2 - size * Math.cos(angle + Math.PI / 6);
        double y4 = y2 - size * Math.sin(angle + Math.PI / 6);

        // 填充箭头头部
        int[] xPoints = {x2, (int)x3, (int)x4};
        int[] yPoints = {y2, (int)y3, (int)y4};

        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * 创建画笔操作
     */
    public static DrawingOperation createBrushOperation(List<DrawingPoint> points,
                                                        Color color, int thickness) {
        BrushStyle style = new BrushStyle(color, thickness, 1.0f);
        DrawingElement element = new DrawingElement(DrawingType.BRUSH, points,
                null, style, null);
        return new DrawingOperation(element);
    }

    /**
     * 创建文字操作
     */
    public static DrawingOperation createTextOperation(String text, int x, int y,
                                                       String fontName, int fontSize,
                                                       Color color) {
        List<DrawingPoint> points = new ArrayList<>();
        points.add(new DrawingPoint(x, y));

        TextStyle style = new TextStyle(fontName, fontSize, color, false, false, false);
        DrawingElement element = new DrawingElement(DrawingType.TEXT, points,
                text, null, style);
        return new DrawingOperation(element);
    }

    @Override
    public String getOperationName() {
        if (drawingElements.isEmpty()) {
            return "绘图操作";
        }

        DrawingElement firstElement = drawingElements.get(0);
        return String.format("%s [%d个元素]",
                firstElement.getType().getDescription(), drawingElements.size());
    }
}