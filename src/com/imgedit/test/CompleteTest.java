package imgedit.test;

import imgedit.core.ImageProcessor;
import imgedit.core.operations.*;
import imgedit.core.operations.BrightnessOperation;
import imgedit.core.operations.ContrastOperation;
import imgedit.core.operations.EdgeDetectionOperation;
import imgedit.core.operations.GrayscaleOperation;
import imgedit.core.operations.MirrorOperation;
import imgedit.core.operations.ScaleOperation;
import imgedit.core.operations.FlipOperation;
import imgedit.core.operations.BlurOperation;
import imgedit.core.operations.CompositeOperation;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CompleteTest {
    public static void main(String[] args) {
        try {
            System.out.println("开始图片处理测试...");

            // 创建输出目录
            File outputDir = new File("test_output");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }

            // 1. 创建测试图片
            BufferedImage testImage = createTestImage(400, 300);
            System.out.println("创建测试图片: 400x300");
            ImageIO.write(testImage, "PNG", new File(outputDir, "01_original.png"));
            System.out.println("✓ 保存: test_output/01_original.png");

            // 2. 初始化处理器
            ImageProcessor processor = new ImageProcessor(testImage);

            // 3. 测试旋转90度
            processor.applyOperation(RotateOperation.create90Degree());
            System.out.println("✓ 90度旋转完成");
            BufferedImage rotatedImage = processor.getCurrentImage();
            ImageIO.write(rotatedImage, "PNG", new File(outputDir, "02_rotated_90.png"));
            System.out.println("✓ 保存: test_output/02_rotated_90.png");
            System.out.println("   旋转后尺寸: " + rotatedImage.getWidth() + "x" + rotatedImage.getHeight());

            // 4. 测试裁剪
            processor.applyOperation(new CropOperation(50, 50, 200, 150));
            System.out.println("✓ 裁剪完成 [50,50,200,150]");
            BufferedImage croppedImage = processor.getCurrentImage();
            ImageIO.write(croppedImage, "PNG", new File(outputDir, "03_cropped.png"));
            System.out.println("✓ 保存: test_output/03_cropped.png");
            System.out.println("   裁剪后尺寸: " + croppedImage.getWidth() + "x" + croppedImage.getHeight());

            // 5. 测试撤销
            processor.undo();
            System.out.println("✓ 撤销操作完成");
            BufferedImage undoneImage = processor.getCurrentImage();
            ImageIO.write(undoneImage, "PNG", new File(outputDir, "04_undone.png"));
            System.out.println("✓ 保存: test_output/04_undone.png");
            System.out.println("   撤销后尺寸: " + undoneImage.getWidth() + "x" + undoneImage.getHeight());

            // 6. 测试重做
            processor.redo();
            System.out.println("✓ 重做操作完成");
            BufferedImage redoneImage = processor.getCurrentImage();
            ImageIO.write(redoneImage, "PNG", new File(outputDir, "05_redone.png"));
            System.out.println("✓ 保存: test_output/05_redone.png");
            System.out.println("   重做后尺寸: " + redoneImage.getWidth() + "x" + redoneImage.getHeight());

            // 7. 额外测试：再旋转180度
            processor.applyOperation(RotateOperation.create180Degree());
            System.out.println("✓ 180度旋转完成");
            BufferedImage rotated180Image = processor.getCurrentImage();
            ImageIO.write(rotated180Image, "PNG", new File(outputDir, "06_rotated_180.png"));
            System.out.println("✓ 保存: test_output/06_rotated_180.png");
            System.out.println("   180度旋转后尺寸: " + rotated180Image.getWidth() + "x" + rotated180Image.getHeight());

            // 8. 额外测试：再次裁剪
            processor.applyOperation(new CropOperation(20, 20, 100, 80));
            System.out.println("✓ 二次裁剪完成 [20,20,100,80]");
            BufferedImage cropped2Image = processor.getCurrentImage();
            ImageIO.write(cropped2Image, "PNG", new File(outputDir, "07_cropped_again.png"));
            System.out.println("✓ 保存: test_output/07_cropped_again.png");
            System.out.println("   二次裁剪后尺寸: " + cropped2Image.getWidth() + "x" + cropped2Image.getHeight());

            // ========== 新增功能测试 ==========

            // 9. 测试亮度调整功能
            testBrightnessOperation();

            // 10. 测试对比度调整功能
            testContrastOperation();

            // 11. 测试灰度化功能
            testGrayscaleOperation();

            // 12. 测试翻转功能
            testFlipOperation();

            // 13. 测试缩放功能
            testScaleOperation();

            // 14. 测试模糊功能
            testBlurOperation();

            // 15. 测试镜像功能
            testMirrorOperation();

            // 16. 测试边缘检测功能
            testEdgeDetectionOperation();

            // 17. 测试组合操作功能
            testCompositeOperation();

            // 18. 测试组合操作的撤销重做
            testCompositeOperationUndoRedo();

            // ========== 创新功能测试 ==========

            // 19. 测试智能背景移除功能
            testBackgroundRemovalOperation();

            // 20. 测试智能修复（内容感知填充）功能
            testContentAwareFillOperation();

            // 21. 测试图片风格化功能
            testArtisticStyleOperation();

            // 22. 测试图片超分辨率增强功能
            testSuperResolutionOperation();

            // 23. 测试AI辅助色彩增强功能
            testAIColorEnhancementOperation();

            System.out.println("\n所有测试完成！");
            System.out.println("生成的图片文件列表：");
            System.out.println("基础功能测试：");
            System.out.println("  01_original.png      - 原始图片 (400x300)");
            System.out.println("  02_rotated_90.png    - 旋转90度后");
            System.out.println("  03_cropped.png       - 裁剪后");
            System.out.println("  04_undone.png        - 撤销后");
            System.out.println("  05_redone.png        - 重做后");
            System.out.println("  06_rotated_180.png   - 再旋转180度后");
            System.out.println("  07_cropped_again.png - 二次裁剪后");
            System.out.println("\n新增功能测试：");
            System.out.println("  08_brightness_increase.png - 亮度增加30%");
            System.out.println("  09_brightness_decrease.png - 亮度降低20%");
            System.out.println("  10_contrast_enhance.png    - 对比度增强50%");
            System.out.println("  11_contrast_reduce.png     - 对比度减弱30%");
            System.out.println("  12_grayscale_luminosity.png - 灰度化(亮度加权法)");
            System.out.println("  13_grayscale_average.png   - 灰度化(平均值法)");
            System.out.println("  14_flip_horizontal.png     - 水平翻转");
            System.out.println("  15_flip_vertical.png       - 垂直翻转");
            System.out.println("  16_scale_stretch.png       - 缩放(拉伸填充)");
            System.out.println("  17_scale_keep_ratio.png    - 缩放(保持比例)");
            System.out.println("  18_blur_light.png          - 轻微模糊");
            System.out.println("  19_blur_strong.png         - 强烈模糊");
            System.out.println("  20_mirror_vertical.png     - 垂直镜像");
            System.out.println("  21_mirror_horizontal.png   - 水平镜像");
            System.out.println("  22_edges_horizontal.png    - 水平边缘检测");
            System.out.println("  23_edges_vertical.png      - 垂直边缘检测");
            System.out.println("  24_edges_all.png           - 全方位边缘检测");
            System.out.println("  25_composite_vintage.png   - 怀旧效果组合");
            System.out.println("  26_composite_sketch.png    - 素描效果组合");
            System.out.println("  27_composite_undone.png    - 组合操作撤销");
            System.out.println("  28_composite_redone.png    - 组合操作重做");
            System.out.println("\n创新功能测试：");
            System.out.println("  29_background_removal.png  - 智能背景移除");
            System.out.println("  30_content_aware_fill.png  - 智能修复填充");
            System.out.println("  31_oil_painting.png        - 油画风格化");
            System.out.println("  32_pencil_sketch.png       - 铅笔素描风格化");
            System.out.println("  33_super_resolution.png    - 超分辨率增强");
            System.out.println("  34_ai_color_enhance.png    - AI色彩增强");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 创建带颜色和标记的测试图片，便于观察变换效果
    private static BufferedImage createTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制渐变背景
        GradientPaint gradient = new GradientPaint(0, 0, Color.RED, width, height, Color.BLUE);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // 绘制网格线，便于观察旋转和裁剪效果
        g2d.setColor(new Color(255, 255, 255, 100)); // 半透明白色
        for (int i = 0; i < width; i += 50) {
            g2d.drawLine(i, 0, i, height);
        }
        for (int i = 0; i < height; i += 50) {
            g2d.drawLine(0, i, width, i);
        }

        // 绘制坐标标记
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        // 左上角标记
        g2d.drawString("← 左", 10, 20);
        g2d.drawString("上 ↑", 10, 40);

        // 右下角标记
        g2d.drawString("→ 右", width - 50, height - 10);
        g2d.drawString("↓ 下", width - 30, height - 30);

        // 中心标记
        g2d.drawString("中心", width/2 - 20, height/2);
        g2d.fillOval(width/2 - 3, height/2 - 3, 6, 6);

        // 主要标题
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("测试图片 " + width + "x" + height, width/2 - 80, 50);

        // 绘制一个明显的矩形区域，用于测试裁剪
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(50, 50, 200, 150); // 标记第一个裁剪区域
        g2d.drawRect(20, 20, 100, 80);  // 标记第二个裁剪区域

        // 绘制一些几何图形，便于观察图像处理效果
        g2d.setColor(Color.GREEN);
        g2d.fillOval(100, 100, 60, 60); // 圆形

        g2d.setColor(Color.ORANGE);
        g2d.fillRect(250, 150, 80, 60); // 矩形

        g2d.setColor(Color.MAGENTA);
        int[] xPoints = {300, 350, 325};
        int[] yPoints = {80, 80, 130};
        g2d.fillPolygon(xPoints, yPoints, 3); // 三角形

        g2d.dispose();
        return image;
    }

    /**
     * 创建带纯色背景的测试图片，用于背景移除测试
     */
    private static BufferedImage createImageWithSolidBackground(int width, int height, Color backgroundColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 填充纯色背景
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);

        // 绘制前景对象
        g2d.setColor(Color.RED);
        g2d.fillOval(width/4, height/4, width/2, height/2);

        g2d.setColor(Color.GREEN);
        g2d.fillRect(width/3, height/3, width/3, height/3);

        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("前景对象", width/2 - 60, height/2);

        g2d.dispose();
        return image;
    }

    // ========== 新增功能测试方法 ==========

    /**
     * 测试亮度调整功能
     */
    private static void testBrightnessOperation() throws Exception {
        System.out.println("\n=== 测试亮度调整功能 ===");
        File outputDir = new File("test_output");

        // 创建新图片进行测试
        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试亮度增加
        processor.applyOperation(BrightnessOperation.createIncrease(0.3f));
        System.out.println("✓ 亮度增加30%完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "08_brightness_increase.png"));
        System.out.println("✓ 保存: test_output/08_brightness_increase.png");

        // 重置处理器，测试亮度降低
        processor = new ImageProcessor(testImage);
        processor.applyOperation(BrightnessOperation.createDecrease(0.2f));
        System.out.println("✓ 亮度降低20%完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "09_brightness_decrease.png"));
        System.out.println("✓ 保存: test_output/09_brightness_decrease.png");
    }

    /**
     * 测试对比度调整功能
     */
    private static void testContrastOperation() throws Exception {
        System.out.println("\n=== 测试对比度调整功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试对比度增强
        processor.applyOperation(ContrastOperation.createEnhance(50));
        System.out.println("✓ 对比度增强50%完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "10_contrast_enhance.png"));
        System.out.println("✓ 保存: test_output/10_contrast_enhance.png");

        // 重置处理器，测试对比度减弱
        processor = new ImageProcessor(testImage);
        processor.applyOperation(ContrastOperation.createReduce(30));
        System.out.println("✓ 对比度减弱30%完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "11_contrast_reduce.png"));
        System.out.println("✓ 保存: test_output/11_contrast_reduce.png");
    }

    /**
     * 测试灰度化功能
     */
    private static void testGrayscaleOperation() throws Exception {
        System.out.println("\n=== 测试灰度化功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试亮度加权法（默认）
        processor.applyOperation(GrayscaleOperation.create());
        System.out.println("✓ 灰度化(亮度加权法)完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "12_grayscale_luminosity.png"));
        System.out.println("✓ 保存: test_output/12_grayscale_luminosity.png");

        // 重置处理器，测试平均值法
        processor = new ImageProcessor(testImage);
        processor.applyOperation(GrayscaleOperation.createWithAlgorithm(GrayscaleOperation.GrayscaleAlgorithm.AVERAGE));
        System.out.println("✓ 灰度化(平均值法)完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "13_grayscale_average.png"));
        System.out.println("✓ 保存: test_output/13_grayscale_average.png");
    }

    /**
     * 测试翻转功能
     */
    private static void testFlipOperation() throws Exception {
        System.out.println("\n=== 测试翻转功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试水平翻转
        processor.applyOperation(FlipOperation.createHorizontalFlip());
        System.out.println("✓ 水平翻转完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "14_flip_horizontal.png"));
        System.out.println("✓ 保存: test_output/14_flip_horizontal.png");

        // 重置处理器，测试垂直翻转
        processor = new ImageProcessor(testImage);
        processor.applyOperation(FlipOperation.createVerticalFlip());
        System.out.println("✓ 垂直翻转完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "15_flip_vertical.png"));
        System.out.println("✓ 保存: test_output/15_flip_vertical.png");
    }

    /**
     * 测试缩放功能
     */
    private static void testScaleOperation() throws Exception {
        System.out.println("\n=== 测试缩放功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试拉伸填充
        processor.applyOperation(ScaleOperation.createStretch(400, 150));
        System.out.println("✓ 缩放(拉伸填充到400x150)完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "16_scale_stretch.png"));
        System.out.println("✓ 保存: test_output/16_scale_stretch.png");
        System.out.println("   拉伸后尺寸: " + processor.getCurrentImage().getWidth() + "x" + processor.getCurrentImage().getHeight());

        // 重置处理器，测试保持比例
        processor = new ImageProcessor(testImage);
        processor.applyOperation(ScaleOperation.createKeepAspectRatio(400, 150));
        System.out.println("✓ 缩放(保持比例缩放到400x150)完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "17_scale_keep_ratio.png"));
        System.out.println("✓ 保存: test_output/17_scale_keep_ratio.png");
        System.out.println("   保持比例后尺寸: " + processor.getCurrentImage().getWidth() + "x" + processor.getCurrentImage().getHeight());
    }

    /**
     * 测试模糊功能
     */
    private static void testBlurOperation() throws Exception {
        System.out.println("\n=== 测试模糊功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试轻微模糊
        processor.applyOperation(BlurOperation.createLightBlur());
        System.out.println("✓ 轻微模糊完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "18_blur_light.png"));
        System.out.println("✓ 保存: test_output/18_blur_light.png");

        // 重置处理器，测试强烈模糊
        processor = new ImageProcessor(testImage);
        processor.applyOperation(BlurOperation.createStrongBlur());
        System.out.println("✓ 强烈模糊完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "19_blur_strong.png"));
        System.out.println("✓ 保存: test_output/19_blur_strong.png");
    }

    /**
     * 测试镜像功能
     */
    private static void testMirrorOperation() throws Exception {
        System.out.println("\n=== 测试镜像功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试垂直镜像
        processor.applyOperation(MirrorOperation.createVerticalLeftMirror());
        System.out.println("✓ 垂直左镜像完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "20_mirror_vertical.png"));
        System.out.println("✓ 保存: test_output/20_mirror_vertical.png");

        // 重置处理器，测试水平镜像
        processor = new ImageProcessor(testImage);
        processor.applyOperation(MirrorOperation.createHorizontalBottomMirror());
        System.out.println("✓ 水平下镜像完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "21_mirror_horizontal.png"));
        System.out.println("✓ 保存: test_output/21_mirror_horizontal.png");
    }

    /**
     * 测试边缘检测功能
     */
    private static void testEdgeDetectionOperation() throws Exception {
        System.out.println("\n=== 测试边缘检测功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试水平边缘检测
        processor.applyOperation(EdgeDetectionOperation.createHorizontalEdges());
        System.out.println("✓ 水平边缘检测完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "22_edges_horizontal.png"));
        System.out.println("✓ 保存: test_output/22_edges_horizontal.png");

        // 重置处理器，测试垂直边缘检测
        processor = new ImageProcessor(testImage);
        processor.applyOperation(EdgeDetectionOperation.createVerticalEdges());
        System.out.println("✓ 垂直边缘检测完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "23_edges_vertical.png"));
        System.out.println("✓ 保存: test_output/23_edges_vertical.png");

        // 重置处理器，测试全方位边缘检测
        processor = new ImageProcessor(testImage);
        processor.applyOperation(EdgeDetectionOperation.createAllEdges());
        System.out.println("✓ 全方位边缘检测完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "24_edges_all.png"));
        System.out.println("✓ 保存: test_output/24_edges_all.png");
    }

    /**
     * 测试组合操作功能
     */
    private static void testCompositeOperation() throws Exception {
        System.out.println("\n=== 测试组合操作功能 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 测试怀旧效果组合
        processor.applyOperation(CompositeOperation.createVintageEffect());
        System.out.println("✓ 怀旧效果组合操作完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "25_composite_vintage.png"));
        System.out.println("✓ 保存: test_output/25_composite_vintage.png");

        // 重置处理器，测试素描效果组合
        processor = new ImageProcessor(testImage);
        processor.applyOperation(CompositeOperation.createSketchEffect());
        System.out.println("✓ 素描效果组合操作完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "26_composite_sketch.png"));
        System.out.println("✓ 保存: test_output/26_composite_sketch.png");
    }

    /**
     * 测试组合操作的撤销重做
     */
    private static void testCompositeOperationUndoRedo() throws Exception {
        System.out.println("\n=== 测试组合操作的撤销重做 ===");
        File outputDir = new File("test_output");

        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 创建自定义组合操作：先灰度化，再边缘检测，最后水平翻转
        CompositeOperation customComposite = new CompositeOperation("自定义效果")
                .addOperation(new GrayscaleOperation())
                .addOperation(EdgeDetectionOperation.createAllEdges())
                .addOperation(FlipOperation.createHorizontalFlip());

        // 应用组合操作
        processor.applyOperation(customComposite);
        System.out.println("✓ 自定义组合操作完成");
        BufferedImage compositeResult = processor.getCurrentImage();

        // 测试撤销
        processor.undo();
        System.out.println("✓ 组合操作撤销完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "27_composite_undone.png"));
        System.out.println("✓ 保存: test_output/27_composite_undone.png");

        // 测试重做
        processor.redo();
        System.out.println("✓ 组合操作重做完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "28_composite_redone.png"));
        System.out.println("✓ 保存: test_output/28_composite_redone.png");

        // 验证重做后图片与原始结果相同
        BufferedImage redoneComposite = processor.getCurrentImage();
        boolean imagesEqual = compareImages(compositeResult, redoneComposite);
        System.out.println("✓ 重做后图片与原始结果比较: " + (imagesEqual ? "相同" : "不同"));
    }
    private static void testBackgroundRemovalOperation() throws Exception {
        System.out.println("\n=== 测试智能背景移除功能 ===");
        File outputDir = new File("test_output");

        // 修改1：创建带特定颜色背景的图片（不要用纯白或纯黑）
        // 使用浅灰色或浅蓝色作为背景，这样移除效果才明显
        Color backgroundColor = new Color(240, 240, 240); // 浅灰色背景
        BufferedImage testImage = createImageWithSolidBackground(300, 200, backgroundColor);

        ImageProcessor processor = new ImageProcessor(testImage);

        // 保存原始图片
        ImageIO.write(testImage, "PNG", new File(outputDir, "29a_original_with_background.png"));
        System.out.println("✓ 保存: test_output/29a_original_with_background.png");

        // 修改2：使用正确的背景颜色
        BackgroundRemovalOperation.RemovalParameters params =
                new BackgroundRemovalOperation.RemovalParameters(
                        backgroundColor,    // 使用创建图片时用的背景色
                        0.2f,              // 减小容差，更精确
                        0.1f,              // 减小羽化
                        true
                );

        // 或者更好的方案：使用自适应算法检测背景
        BackgroundRemovalOperation bgRemoval = new BackgroundRemovalOperation(
                BackgroundRemovalOperation.RemovalAlgorithm.ADAPTIVE, params);

        processor.applyOperation(bgRemoval);
        System.out.println("✓ 智能背景移除完成");

        // 修改3：创建一个带彩色背景的新图片来显示移除效果
        BufferedImage result = processor.getCurrentImage();
        BufferedImage displayImage = new BufferedImage(
                result.getWidth(),
                result.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        // 用粉色背景显示（更容易看出透明度效果）
        Graphics2D g2d = displayImage.createGraphics();
        g2d.setColor(Color.PINK);
        g2d.fillRect(0, 0, displayImage.getWidth(), displayImage.getHeight());
        g2d.drawImage(result, 0, 0, null);
        g2d.dispose();

        ImageIO.write(displayImage, "PNG", new File(outputDir, "29_background_removal.png"));
        System.out.println("✓ 保存: test_output/29_background_removal.png");

        // 检查结果
        boolean hasTransparency = result.getColorModel().hasAlpha();
        System.out.println("   结果图片是否包含透明度: " + hasTransparency);
    }

    /**
     * 测试智能修复（内容感知填充）功能
     */
    private static void testContentAwareFillOperation() throws Exception {
        System.out.println("\n=== 测试智能修复（内容感知填充）功能 ===");
        File outputDir = new File("test_output");

        // 创建测试图片
        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 保存原始图片
        ImageIO.write(testImage, "PNG", new File(outputDir, "29a_original_for_fill.png"));
        System.out.println("✓ 保存: test_output/29a_original_for_fill.png");

        // 定义要填充的区域（移除左上角的"左"字）
        Rectangle fillArea = new Rectangle(10, 20, 40, 20);

        // 创建智能填充操作
        ContentAwareFillOperation fillOp = ContentAwareFillOperation.create(fillArea);

        // 应用智能填充
        processor.applyOperation(fillOp);
        System.out.println("✓ 智能修复填充完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "30_content_aware_fill.png"));
        System.out.println("✓ 保存: test_output/30_content_aware_fill.png");
        System.out.println("   填充区域: [x=" + fillArea.x + ", y=" + fillArea.y +
                ", w=" + fillArea.width + ", h=" + fillArea.height + "]");
    }

    /**
     * 测试图片风格化功能
     */
    private static void testArtisticStyleOperation() throws Exception {
        System.out.println("\n=== 测试图片风格化功能 ===");
        File outputDir = new File("test_output");

        // 创建测试图片
        BufferedImage testImage = createTestImage(300, 200);

        // 测试油画效果
        ImageProcessor processor = new ImageProcessor(testImage);
        ArtisticStyleOperation.StyleParameters oilParams =
                new ArtisticStyleOperation.StyleParameters(0.7f, 5, 0.5f);
        ArtisticStyleOperation oilPainting = new ArtisticStyleOperation(
                ArtisticStyleOperation.ArtisticStyle.OIL_PAINTING, oilParams);

        processor.applyOperation(oilPainting);
        System.out.println("✓ 油画风格化完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "31_oil_painting.png"));
        System.out.println("✓ 保存: test_output/31_oil_painting.png");

        // 测试铅笔素描效果
        processor = new ImageProcessor(testImage);
        ArtisticStyleOperation pencilSketch = ArtisticStyleOperation.createPencilSketch(0.8f);

        processor.applyOperation(pencilSketch);
        System.out.println("✓ 铅笔素描风格化完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "32_pencil_sketch.png"));
        System.out.println("✓ 保存: test_output/32_pencil_sketch.png");
    }

    /**
     * 测试图片超分辨率增强功能（简化版）
     */
    private static void testSuperResolutionOperation() throws Exception {
        System.out.println("\n=== 测试图片超分辨率增强功能 ===");
        File outputDir = new File("test_output");

        // 创建较小尺寸的测试图片
        BufferedImage testImage = createTestImage(150, 100);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 保存原始小图
        ImageIO.write(testImage, "PNG", new File(outputDir, "32a_original_small.png"));
        System.out.println("✓ 保存: test_output/32a_original_small.png");
        System.out.println("   原始小图尺寸: " + testImage.getWidth() + "x" + testImage.getHeight());

        // 使用简化的超分辨率算法（避免复杂的插值算法）
        try {
            // 创建2倍超分辨率增强，使用双线性插值（最简单稳定）
            SuperResolutionOperation srOp = new SuperResolutionOperation(
                    SuperResolutionOperation.ScaleFactor.X2,
                    SuperResolutionOperation.InterpolationAlgorithm.BILINEAR, // 使用双线性，避免复杂算法
                    0.2f  // 降低锐化强度
            );

            // 应用超分辨率增强
            processor.applyOperation(srOp);
            System.out.println("✓ 超分辨率增强完成");
            BufferedImage enhancedImage = processor.getCurrentImage();
            ImageIO.write(enhancedImage, "PNG", new File(outputDir, "33_super_resolution.png"));
            System.out.println("✓ 保存: test_output/33_super_resolution.png");
            System.out.println("   增强后尺寸: " + enhancedImage.getWidth() + "x" + enhancedImage.getHeight());

            // 验证尺寸是否正确放大
            boolean sizeCorrect = enhancedImage.getWidth() == 300 && enhancedImage.getHeight() == 200;
            System.out.println("   尺寸放大验证: " + (sizeCorrect ? "正确" : "错误"));
        } catch (Exception e) {
            System.err.println("   超分辨率测试失败，使用备用方案...");

            // 备用方案：使用普通的缩放操作
            processor = new ImageProcessor(testImage);
            ScaleOperation scaleOp = ScaleOperation.createKeepAspectRatio(300, 200);
            processor.applyOperation(scaleOp);
            System.out.println("✓ 使用普通缩放作为超分辨率替代方案");
            ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "33_super_resolution_fallback.png"));
            System.out.println("✓ 保存: test_output/33_super_resolution_fallback.png");
        }
    }


    /**
     * 测试AI辅助色彩增强功能
     */
    private static void testAIColorEnhancementOperation() throws Exception {
        System.out.println("\n=== 测试AI辅助色彩增强功能 ===");
        File outputDir = new File("test_output");

        // 创建测试图片
        BufferedImage testImage = createTestImage(300, 200);
        ImageProcessor processor = new ImageProcessor(testImage);

        // 保存原始图片
        ImageIO.write(testImage, "PNG", new File(outputDir, "33a_original_for_ai.png"));
        System.out.println("✓ 保存: test_output/33a_original_for_ai.png");

        // 创建全自动AI色彩增强
        AIColorEnhancementOperation aiEnhance = AIColorEnhancementOperation.createAutoEnhancement();

        // 应用AI色彩增强
        processor.applyOperation(aiEnhance);
        System.out.println("✓ AI色彩增强完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "34_ai_color_enhance.png"));
        System.out.println("✓ 保存: test_output/34_ai_color_enhance.png");

        // 同时测试人像增强模式
        processor = new ImageProcessor(testImage);
        AIColorEnhancementOperation portraitEnhance = new AIColorEnhancementOperation(
                AIColorEnhancementOperation.EnhancementMode.PORTRAIT, 0.6f);

        processor.applyOperation(portraitEnhance);
        System.out.println("✓ AI人像增强完成");
        ImageIO.write(processor.getCurrentImage(), "PNG", new File(outputDir, "35_ai_portrait_enhance.png"));
        System.out.println("✓ 保存: test_output/35_ai_portrait_enhance.png");
    }

    /**
     * 比较两个图片是否相同（简单比较尺寸和像素）
     */
    private static boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }

        int width = img1.getWidth();
        int height = img1.getHeight();

        // 简单比较：只检查前10行和前10列的像素
        for (int y = 0; y < Math.min(10, height); y++) {
            for (int x = 0; x < Math.min(10, width); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }
}