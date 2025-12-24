package imgedit.ui;

// 【修复1】补充缺失的核心接口导入
import imgedit.core.ImageOperation;

import imgedit.core.operations.*;
// 【修复2】导入风格迁移需要的内部类
import imgedit.core.operations.ArtisticStyleOperation.ArtisticStyle;
import imgedit.core.operations.ArtisticStyleOperation.StyleParameters;

import imgedit.utils.ImageUtils;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图像管理类 - 负责图像的加载、保存、操作
 */
public class ImageManager {

    private final EditorController controller;

    // 图像数据
    private BufferedImage currentBufferedImage;
    private Image currentImage;
    private Image originalImage;
    private File currentImageFile;
    private double currentZoom = 1.0;

    // UI组件引用
    private ImageView imageView;
    private ScrollPane imageScrollPane;
    private ListView<String> historyListView;

    public ImageManager(EditorController controller) {
        this.controller = controller;
    }

    // ==================== UI 设置 ====================

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public void setImageScrollPane(ScrollPane scrollPane) {
        this.imageScrollPane = scrollPane;
    }

    public void setHistoryListView(ListView<String> listView) {
        this.historyListView = listView;
    }

    // ==================== 文件操作 ====================

    public void openImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showOpenDialog(controller.getPrimaryStage());
        if (file != null) {
            loadImage(file);
        }
    }

    public void loadImage(File file) {
        controller.showProgress("正在加载图片...");

        new Thread(() -> {
            try {
                Image image = new Image(file.toURI().toString());

                currentImageFile = file;
                currentImage = image;
                originalImage = image;
                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);

                Platform.runLater(() -> {
                    imageView.setImage(currentImage);

                    if (imageScrollPane.getParent() instanceof StackPane) {
                        StackPane centerPane = (StackPane) imageScrollPane.getParent();
                        Node placeholder = centerPane.lookup("#placeholder");
                        if (placeholder != null) placeholder.setVisible(false);
                    }

                    imageScrollPane.setVisible(true);

                    if (imageScrollPane.getContent() instanceof VBox) {
                        VBox imageContainer = (VBox) imageScrollPane.getContent();
                        Node controlButtons = imageContainer.lookup("#control-buttons");
                        if (controlButtons != null) controlButtons.setVisible(true);
                    }

                    fitToWindow();

                    if (controller.getImageEditorService() != null) {
                        controller.getImageEditorService().initImageProcessor(currentImage);
                    }

                    controller.refreshImageInfo();
                    if (historyListView != null) historyListView.getItems().clear();
                    addHistory("打开图片: " + file.getName());

                    controller.updateStatus("图片已加载: " + file.getName());
                    controller.hideProgress();
                    controller.resetSelectionCanvas();

                    if (controller.getAnimationManager() != null) {
                        controller.getAnimationManager().playImageLoadAnimation(imageView);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("加载失败", "无法加载图片: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    public void saveImage() {
        if (currentImage == null) {
            controller.showWarning("提示", "没有可保存的图片");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp")
        );

        File file = fileChooser.showSaveDialog(controller.getPrimaryStage());
        if (file != null) {
            controller.showProgress("正在保存图片...");

            new Thread(() -> {
                try {
                    BufferedImage bufferedImage;
                    if (controller.getImageEditorService() != null) {
                        bufferedImage = controller.getImageEditorService().getImageProcessor().getCurrentImage();
                    } else {
                        bufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    }

                    String format = getFileExtension(file.getName()).toUpperCase();
                    if (format.equals("JPG")) format = "JPEG";

                    ImageIO.write(bufferedImage, format, file);

                    currentImageFile = file;

                    Platform.runLater(() -> {
                        controller.hideProgress();
                        controller.updateStatus("图片已保存: " + file.getName());
                        controller.showSuccess("保存成功", "图片已保存到: " + file.getAbsolutePath());
                        controller.refreshImageInfo();
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        controller.hideProgress();
                        controller.showError("保存失败", "无法保存图片: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    // ==================== 图像调整 ====================

    public void applyAllAdjustments(double brightness, double contrast, double saturation) {
        if (currentImage == null) {
            controller.showWarning("提示", "请先加载图片");
            return;
        }

        controller.showProgress("正在应用调整...");

        new Thread(() -> {
            try {
                if (brightness != 0) {
                    BrightnessOperation.BrightnessMode mode = brightness >= 0 ?
                            BrightnessOperation.BrightnessMode.INCREASE :
                            BrightnessOperation.BrightnessMode.DECREASE;
                    float intensity = (float)(Math.abs(brightness) / 100.0);
                    applyOperationSync(new BrightnessOperation(mode, intensity));
                }

                if (contrast != 0) {
                    float contrastLevel = (float)(contrast / 100.0f + 1.0f);
                    applyOperationSync(new ContrastOperation(contrastLevel));
                }

                if (saturation != 0) {
                    float saturationFactor = (float)(saturation / 100.0f + 1.0f);
                    applyOperationSync(new SaturationOperation(saturationFactor));
                }

                Platform.runLater(() -> {
                    controller.updateStatus("基础调整已应用");
                    controller.hideProgress();
                    if (controller.getAnimationManager() != null) {
                        controller.getAnimationManager().playSuccessAnimation(imageView);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("调整失败", e.getMessage());
                });
            }
        }).start();
    }

    private void applyOperationSync(ImageOperation operation) throws Exception {
        if (controller.getImageEditorService() != null) {
            controller.getImageEditorService().applyOperation(operation);
            currentImage = controller.getImageEditorService().getCurrentImage();
            currentBufferedImage = controller.getImageEditorService().getImageProcessor().getCurrentImage();

            Platform.runLater(() -> imageView.setImage(currentImage));
        }
    }

    // ==================== 通用操作方法 ====================

    public void applyOperation(ImageOperation operation, String operationName) {
        if (controller.getImageEditorService() == null) return;

        controller.showProgress(operationName + "处理中...");

        controller.getImageEditorService().applyOperationAsync(
                operation,
                resultImage -> Platform.runLater(() -> {
                    currentImage = resultImage;
                    imageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);

                    addHistory(operationName);
                    controller.updateStatus(operationName + "完成");
                    controller.hideProgress();

                    if (controller.getAnimationManager() != null) {
                        controller.getAnimationManager().playSuccessAnimation(imageView);
                    }
                }),
                exception -> Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("操作失败", exception.getMessage());
                })
        );
    }

    // ==================== 具体功能实现 ====================

    public void rotate90() {
        applyOperation(RotateOperation.create90Degree(), "旋转90度");
    }

    public void rotate180() {
        applyOperation(RotateOperation.create180Degree(), "旋转180度");
    }

    public void flipHorizontal() {
        applyOperation(FlipOperation.createHorizontalFlip(), "水平翻转");
    }

    public void flipVertical() {
        applyOperation(FlipOperation.createVerticalFlip(), "垂直翻转");
    }

    public void applyGrayscale() {
        applyOperation(GrayscaleOperation.create(), "灰度化");
    }

    public void applyBlur(double value) {
        if (value == 0) return;
        BlurOperation.BlurIntensity intensity;
        if (value <= 3) intensity = BlurOperation.BlurIntensity.LIGHT;
        else if (value <= 6) intensity = BlurOperation.BlurIntensity.MEDIUM;
        else intensity = BlurOperation.BlurIntensity.STRONG;

        applyOperation(new BlurOperation(intensity), "应用模糊");
    }

    public void detectEdges() {
        applyOperation(EdgeDetectionOperation.createAllEdges(), "边缘检测");
    }

    public void aiEnhance() {
        applyOperation(AIColorEnhancementOperation.createAutoEnhancement(), "AI增强");
    }

    public void removeBackground() {
        applyOperation(BackgroundRemovalOperation.createAutoBackgroundRemoval(), "移除背景");
    }

    // 【修复3】修复后的 applyArtisticStyle 方法
    public void applyArtisticStyle() {
        if (currentImage == null) {
            controller.showError("提示", "请先加载图片");
            return;
        }

        // 使用 lambda 接收回调
        controller.getDialogManager().showArtisticStyleDialog(styleName -> {
            if (styleName != null && !styleName.isEmpty()) {
                try {
                    // 1. 获取风格枚举
                    ArtisticStyle style = getStyleFromDescription(styleName);

                    // 2. 创建默认参数 (解决构造函数参数不匹配问题)
                    // 假设参数含义为: intensity(1.0), iterations(1), smoothing(0.5)
                    StyleParameters params = new StyleParameters(1.0f, 1, 0.5f);

                    // 3. 创建操作对象
                    ArtisticStyleOperation operation = new ArtisticStyleOperation(style, params);

                    applyOperation(operation, "艺术风格: " + styleName);
                } catch (Exception e) {
                    controller.showError("错误", "无法应用风格: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // 【新增】辅助方法：中文名转枚举
    private ArtisticStyle getStyleFromDescription(String desc) {
        ArtisticStyle[] styles = ArtisticStyle.values();
        if (styles.length == 0) return null; // 应添加空检查

        for (ArtisticStyle s : styles) {
            String name = s.name().toUpperCase();
            if (desc.contains("星空") && (name.contains("STARRY") || name.contains("GOGH"))) return s;
            if (desc.contains("立体") && (name.contains("CUBISM") || name.contains("PICASSO"))) return s;
            if (desc.contains("印象") && (name.contains("MONET") || name.contains("IMPRESSION"))) return s;
            if (desc.contains("浮世绘") && (name.contains("UKIYOE") || name.contains("KANAGAWA"))) return s;
            if (desc.contains("赛博") && name.contains("CYBER")) return s;
            if (desc.contains("素描") && name.contains("SKETCH")) return s;
            if (desc.contains("卡通") && name.contains("CARTOON")) return s;
            if (desc.contains("马赛克") && name.contains("MOSAIC")) return s;
            if (desc.contains("水彩") && name.contains("WATER")) return s;
            if (desc.contains("油画") && name.contains("OIL")) return s;
        }

        // 默认返回第一个，防止崩溃
        return styles[0];
    }

    public void undo() {
        if (controller.getImageEditorService() != null && controller.getImageEditorService().canUndo()) {
            Image result = controller.getImageEditorService().undo();
            if (result != null) {
                updateImage(result);
                addHistory("撤销操作");
                controller.updateStatus("撤销完成");
            }
        } else {
            controller.updateStatus("无法撤销");
        }
    }

    public void redo() {
        if (controller.getImageEditorService() != null && controller.getImageEditorService().canRedo()) {
            Image result = controller.getImageEditorService().redo();
            if (result != null) {
                updateImage(result);
                addHistory("重做操作");
                controller.updateStatus("重做完成");
            }
        } else {
            controller.updateStatus("无法重做");
        }
    }

    public void resetImage() {
        if (currentImageFile != null) {
            loadImage(currentImageFile);
        }
    }

    public void clearCanvas() {
        currentImage = null;
        currentImageFile = null;
        currentBufferedImage = null;
        imageView.setImage(null);
        imageScrollPane.setVisible(false);

        if (imageScrollPane.getParent() instanceof StackPane) {
            StackPane centerPane = (StackPane) imageScrollPane.getParent();
            Node placeholder = centerPane.lookup("#placeholder");
            if (placeholder != null) placeholder.setVisible(true);
        }

        if (historyListView != null) historyListView.getItems().clear();
        controller.updateStatus("画布已清空");
    }

    public void fitToWindow() {
        if (currentImage != null) {
            double maxWidth = 1000;
            double maxHeight = 700;
            double imageWidth = currentImage.getWidth();
            double imageHeight = currentImage.getHeight();

            double scaleRatio = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
            scaleRatio = Math.min(scaleRatio, 1.0);

            setZoom(scaleRatio);

            imageView.setFitWidth(imageWidth * scaleRatio);
            imageView.setFitHeight(imageHeight * scaleRatio);
        }
    }

    public void setZoom(double zoom) {
        this.currentZoom = zoom;
        if (imageView != null) {
            imageView.setScaleX(currentZoom);
            imageView.setScaleY(currentZoom);
        }
    }

    public void zoomIn() {
        setZoom(currentZoom * 1.2);
    }

    public void zoomOut() {
        setZoom(currentZoom * 0.8);
    }

    private void addHistory(String operation) {
        if (historyListView != null) {
            historyListView.getItems().add(0, operation);
            if (historyListView.getItems().size() > 20) {
                historyListView.getItems().remove(20);
            }
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "png";
    }

    public void updateImage(Image newImage) {
        currentImage = newImage;
        imageView.setImage(currentImage);
        currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
    }

    public Image getCurrentImage() { return currentImage; }
    public Image getOriginalImage() { return originalImage; }
    public BufferedImage getCurrentBufferedImage() { return currentBufferedImage; }
    public File getCurrentImageFile() { return currentImageFile; }

    public double[] screenToImageCoordinates(double screenX, double screenY) {
        if (imageView == null || imageView.getImage() == null) {
            return new double[]{screenX, screenY};
        }

        Bounds bounds = imageView.getBoundsInParent();
        double scaleX = imageView.getImage().getWidth() / bounds.getWidth();
        double scaleY = imageView.getImage().getHeight() / bounds.getHeight();

        double localX = screenX - bounds.getMinX();
        double localY = screenY - bounds.getMinY();

        return new double[]{localX * scaleX, localY * scaleY};
    }

    public Bounds getImageDisplayBounds() {
        if (imageView == null) return new BoundingBox(0,0,0,0);
        return imageView.localToScene(imageView.getBoundsInLocal());
    }
}