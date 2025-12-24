package imgedit.ui;

import imgedit.core.ImageOperation;
import imgedit.core.operations.*;
import imgedit.utils.ImageUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
/**
 * 图像管理类 - 负责图像的加载、保存、操作
 */
public class ImageManager {

    private final EditorController controller;

    // 图像数据
    private BufferedImage currentBufferedImage;
    private Image currentImage;

    private File currentImageFile;
    private double currentZoom = 1.0;

    // UI组件引用
    private ImageView imageView;
    private ScrollPane imageScrollPane;
    private ListView<String> historyListView;

    public ImageManager(EditorController controller) {
        this.controller = controller;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public void setImageScrollPane(ScrollPane scrollPane) {
        this.imageScrollPane = scrollPane;
    }

    public void setHistoryListView(ListView<String> listView) {
        this.historyListView = listView;
    }

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
                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);

                Platform.runLater(() -> {
                    imageView.setImage(currentImage);

                    // 隐藏占位符，显示图像区域
                    StackPane centerPane = (StackPane) imageScrollPane.getParent();
                    Node placeholder = centerPane.lookup("#placeholder");
                    if (placeholder != null) {
                        placeholder.setVisible(false);
                    }

                    // 显示图像区域
                    imageScrollPane.setVisible(true);

                    // 显示控制按钮
                    VBox imageContainer = (VBox) imageScrollPane.getContent();
                    if (imageContainer != null) {
                        Node controlButtons = imageContainer.lookup("#control-buttons");
                        if (controlButtons != null) {
                            controlButtons.setVisible(true);
                        }
                    }

                    // 调整图片显示大小
                    if (currentImage.getWidth() > 0 && currentImage.getHeight() > 0) {
                        double imageWidth = currentImage.getWidth();
                        double imageHeight = currentImage.getHeight();
                        double maxWidth = 1000;
                        double maxHeight = 700;

                        double widthRatio = maxWidth / imageWidth;
                        double heightRatio = maxHeight / imageHeight;
                        double scaleRatio = Math.min(widthRatio, heightRatio);

                        scaleRatio = Math.min(scaleRatio, 1.0);

                        imageView.setFitWidth(imageWidth * scaleRatio);
                        imageView.setFitHeight(imageHeight * scaleRatio);

                        currentZoom = 1.0;
                        imageView.setScaleX(currentZoom);
                        imageView.setScaleY(currentZoom);
                    }

                    // 初始化服务
                    if (controller.getImageEditorService() != null) {
                        controller.getImageEditorService().initImageProcessor(currentImage);
                    }

                    addHistory("打开图片: " + file.getName());
                    controller.updateStatus("图片已加载: " + file.getName() + " (" +
                            (int)currentImage.getWidth() + "×" + (int)currentImage.getHeight() + ")");
                    controller.hideProgress();

                    // 播放加载动画
                    controller.getAnimationManager().playImageLoadAnimation(imageView);
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
                    BufferedImage bufferedImage = controller.getImageEditorService()
                            .getImageProcessor().getCurrentImage();
                    String format = getFileExtension(file.getName()).toUpperCase();
                    if (format.equals("JPG")) format = "JPEG";

                    ImageIO.write(bufferedImage, format, file);

                    Platform.runLater(() -> {
                        controller.hideProgress();
                        controller.updateStatus("图片已保存: " + file.getName());
                        controller.showSuccess("保存成功", "图片已保存到: " + file.getAbsolutePath());
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

    public void applyAllAdjustments(double brightness, double contrast, double saturation) {
        if (currentImage == null || controller.getImageEditorService() == null) {
            controller.showWarning("提示", "请先加载图片");
            return;
        }

        controller.showProgress("正在应用调整...");

        new Thread(() -> {
            try {
                // 保存原始图片用于回退
                Image originalImage = currentImage;

                // 依次应用调整
                if (brightness != 0) {
                    BrightnessOperation.BrightnessMode mode = brightness >= 0 ?
                            BrightnessOperation.BrightnessMode.INCREASE :
                            BrightnessOperation.BrightnessMode.DECREASE;
                    float intensity = (float)(Math.abs(brightness) / 100.0);
                    BrightnessOperation brightnessOp = new BrightnessOperation(mode, intensity);

                    applyOperation(brightnessOp, "调整亮度");
                    Thread.sleep(100);
                }

                if (contrast != 0) {
                    float contrastLevel = (float)(contrast / 100.0f + 1.0f);
                    ContrastOperation contrastOp = new ContrastOperation(contrastLevel);

                    applyOperation(contrastOp, "调整对比度");
                    Thread.sleep(100);
                }

                if (saturation != 0) {
                    float saturationFactor = (float)(saturation / 100.0f + 1.0f);
                    SaturationOperation saturationOp = new SaturationOperation(saturationFactor);

                    applyOperation(saturationOp, "调整饱和度");
                    Thread.sleep(100);
                }

                Thread.sleep(300);

                Platform.runLater(() -> {
                    controller.updateStatus("基础调整已应用");
                    controller.hideProgress();
                    controller.getAnimationManager().playSuccessAnimation(imageView);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("调整失败", e.getMessage());
                });
            }
        }).start();
    }

    public void applyOperation(ImageOperation operation, String operationName) {
        controller.showProgress("处理中...");

        new Thread(() -> {
            try {
                controller.getImageEditorService().applyOperationAsync(
                        operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            addHistory(operationName);
                            controller.updateStatus(operationName + "完成");
                            controller.hideProgress();
                            controller.getAnimationManager().playSuccessAnimation(imageView);
                        }),
                        exception -> Platform.runLater(() -> {
                            controller.hideProgress();
                            controller.showError("操作失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("操作失败", e.getMessage());
                });
            }
        }).start();
    }

    public void rotate90() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        RotateOperation operation = RotateOperation.create90Degree();
        applyOperation(operation, "旋转90度");
    }

    public void rotate180() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        RotateOperation operation = RotateOperation.create180Degree();
        applyOperation(operation, "旋转180度");
    }

    public void flipHorizontal() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        FlipOperation operation = FlipOperation.createHorizontalFlip();
        applyOperation(operation, "水平翻转");
    }

    public void flipVertical() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        FlipOperation operation = FlipOperation.createVerticalFlip();
        applyOperation(operation, "垂直翻转");
    }

    public void applyGrayscale() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        GrayscaleOperation operation = GrayscaleOperation.create();
        applyOperation(operation, "灰度化");
    }

    public void applyBlur(double value) {
        if (currentImage == null || controller.getImageEditorService() == null || value == 0) return;

        BlurOperation.BlurIntensity intensity;
        if (value <= 3) {
            intensity = BlurOperation.BlurIntensity.LIGHT;
        } else if (value <= 6) {
            intensity = BlurOperation.BlurIntensity.MEDIUM;
        } else {
            intensity = BlurOperation.BlurIntensity.STRONG;
        }

        BlurOperation operation = new BlurOperation(intensity);
        applyOperation(operation, "应用模糊");
    }

    public void detectEdges() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        EdgeDetectionOperation operation = EdgeDetectionOperation.createAllEdges();
        applyOperation(operation, "边缘检测");
    }

    public void aiEnhance() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        controller.showProgress("AI增强处理中...");

        new Thread(() -> {
            try {
                AIColorEnhancementOperation operation = AIColorEnhancementOperation.createAutoEnhancement();
                controller.getImageEditorService().applyOperationAsync(
                        operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            addHistory("AI增强");
                            controller.updateStatus("AI增强完成");
                            controller.hideProgress();
                            controller.getAnimationManager().playSuccessAnimation(imageView);
                        }),
                        exception -> Platform.runLater(() -> {
                            controller.hideProgress();
                            controller.showError("AI增强失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("AI增强失败", e.getMessage());
                });
            }
        }).start();
    }

    public void removeBackground() {
        if (currentImage == null || controller.getImageEditorService() == null) return;
        controller.showProgress("背景移除中...");

        new Thread(() -> {
            try {
                BackgroundRemovalOperation operation = BackgroundRemovalOperation.createAutoBackgroundRemoval();
                controller.getImageEditorService().applyOperationAsync(
                        operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            addHistory("移除背景");
                            controller.updateStatus("背景移除完成");
                            controller.hideProgress();
                            controller.getAnimationManager().playSuccessAnimation(imageView);
                        }),
                        exception -> Platform.runLater(() -> {
                            controller.hideProgress();
                            controller.showError("背景移除失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("背景移除失败", e.getMessage());
                });
            }
        }).start();
    }

    public void applyArtisticStyle() {
        if (currentImage == null) {
            controller.showError("提示", "请先加载图片");
            return;
        }

        controller.getDialogManager().showArtisticStyleDialog();
    }

    public void undo() {
        if (controller.getImageEditorService() != null && controller.getImageEditorService().canUndo()) {
            try {
                Image result = controller.getImageEditorService().undo();
                if (result != null) {
                    currentImage = result;
                    imageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    controller.updateStatus("撤销完成");
                    addHistory("撤销操作");
                }
            } catch (Exception e) {
                controller.showError("撤销失败", e.getMessage());
            }
        } else {
            controller.updateStatus("无法撤销");
        }
    }

    public void redo() {
        if (controller.getImageEditorService() != null && controller.getImageEditorService().canRedo()) {
            try {
                Image result = controller.getImageEditorService().redo();
                if (result != null) {
                    currentImage = result;
                    imageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    controller.updateStatus("重做完成");
                    addHistory("重做操作");
                }
            } catch (Exception e) {
                controller.showError("重做失败", e.getMessage());
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

        // 隐藏图像区域，显示占位符
        imageScrollPane.setVisible(false);

        // 查找占位符
        StackPane centerPane = (StackPane) imageScrollPane.getParent();
        Node placeholder = centerPane.lookup("#placeholder");
        if (placeholder != null) {
            placeholder.setVisible(true);
        }

        // 隐藏控制按钮
        VBox imageContainer = (VBox) imageScrollPane.getContent();
        if (imageContainer != null) {
            Node controlButtons = imageContainer.lookup("#control-buttons");
            if (controlButtons != null) {
                controlButtons.setVisible(false);
            }
        }

        if (historyListView != null) {
            historyListView.getItems().clear();
        }
        controller.updateStatus("画布已清空");
    }

    public void setZoom(double zoom) {
        currentZoom = zoom;
        if (imageView != null && imageView.getImage() != null) {
            imageView.setScaleX(zoom);
            imageView.setScaleY(zoom);
        }
    }

    public void zoomIn() {
        currentZoom *= 1.2;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
    }

    public void zoomOut() {
        currentZoom *= 0.8;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
    }

    public void fitToWindow() {
        if (currentImage != null) {
            currentZoom = 1.0;
            imageView.setScaleX(currentZoom);
            imageView.setScaleY(currentZoom);

            double maxWidth = 1000;
            double maxHeight = 700;
            double imageWidth = currentImage.getWidth();
            double imageHeight = currentImage.getHeight();

            double widthRatio = maxWidth / imageWidth;
            double heightRatio = maxHeight / imageHeight;
            double scaleRatio = Math.min(widthRatio, heightRatio);

            scaleRatio = Math.min(scaleRatio, 1.0);

            imageView.setFitWidth(imageWidth * scaleRatio);
            imageView.setFitHeight(imageHeight * scaleRatio);
        }
    }

    public void resetZoom() {
        currentZoom = 1.0;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
        if (currentImage != null) {
            imageView.setFitWidth(currentImage.getWidth());
            imageView.setFitHeight(currentImage.getHeight());
        }
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
    public double getImageWidth() {
        return currentImage != null ? currentImage.getWidth() : 0;
    }

    public double getImageHeight() {
        return currentImage != null ? currentImage.getHeight() : 0;
    }

    public int getBufferedImageWidth() {
        return currentBufferedImage != null ? currentBufferedImage.getWidth() : 0;
    }

    public int getBufferedImageHeight() {
        return currentBufferedImage != null ? currentBufferedImage.getHeight() : 0;
    }

    /**
     * 将屏幕坐标转换为图像坐标
     */
    public double[] screenToImageCoordinates(double screenX, double screenY) {
        if (imageView == null || imageView.getImage() == null) {
            return new double[]{screenX, screenY};
        }

        // 获取图像原始尺寸
        double imageWidth = imageView.getImage().getWidth();
        double imageHeight = imageView.getImage().getHeight();

        // 获取ImageView在父容器中的边界
        Bounds viewBounds = imageView.getBoundsInParent();
        double viewX = viewBounds.getMinX();
        double viewY = viewBounds.getMinY();
        double viewWidth = viewBounds.getWidth();
        double viewHeight = viewBounds.getHeight();

        // 计算缩放比例（简化版本）
        double scaleX = imageWidth / viewWidth;
        double scaleY = imageHeight / viewHeight;

        // 计算相对于ImageView的坐标
        double relativeX = screenX - viewX;
        double relativeY = screenY - viewY;

        // 转换为原始图像坐标
        double imageX = relativeX * scaleX;
        double imageY = relativeY * scaleY;

        // 确保坐标在图像范围内
        imageX = Math.max(0, Math.min(imageX, imageWidth - 1));
        imageY = Math.max(0, Math.min(imageY, imageHeight - 1));

        return new double[]{imageX, imageY};
    }

    /**
     * 将图像坐标转换为屏幕坐标
     */
    public double[] imageToScreenCoordinates(double imageX, double imageY) {
        if (imageView == null || imageView.getImage() == null) {
            return new double[]{imageX, imageY};
        }

        double imageWidth = imageView.getImage().getWidth();
        double imageHeight = imageView.getImage().getHeight();
        double viewWidth = imageView.getBoundsInLocal().getWidth();
        double viewHeight = imageView.getBoundsInLocal().getHeight();

        double scaleX = viewWidth / imageWidth;
        double scaleY = viewHeight / imageHeight;

        Bounds viewportBounds = imageView.localToParent(imageView.getBoundsInLocal());
        double offsetX = viewportBounds.getMinX();
        double offsetY = viewportBounds.getMinY();

        double screenX = imageX * scaleX + offsetX;
        double screenY = imageY * scaleY + offsetY;

        return new double[]{screenX, screenY};
    }

    /**
     * 更新当前显示的图像
     */
    public void updateImage(Image newImage) {
        currentImage = newImage;
        imageView.setImage(currentImage);
        currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
    }

    /**
     * 更新当前显示的图像（从BufferedImage）
     */
    public void updateImageFromBufferedImage(BufferedImage bufferedImage) {
        currentBufferedImage = bufferedImage;
        currentImage = ImageUtils.bufferedImageToFxImage(bufferedImage);
        imageView.setImage(currentImage);
    }

    /**
     * 获取图像在屏幕上的显示边界
     */
    public Bounds getImageDisplayBounds() {
        if (imageView == null || imageView.getImage() == null) {
            return new BoundingBox(0, 0, 0, 0);
        }

        double imageWidth = imageView.getImage().getWidth();
        double imageHeight = imageView.getImage().getHeight();
        double viewWidth = imageView.getBoundsInLocal().getWidth();
        double viewHeight = imageView.getBoundsInLocal().getHeight();

        // 计算实际显示区域
        double actualWidth = viewWidth;
        double actualHeight = viewHeight;
        double offsetX = 0;
        double offsetY = 0;

        if (imageView.isPreserveRatio()) {  // 修正这里：使用 isPreserveRatio()
            double imageRatio = imageWidth / imageHeight;
            double viewRatio = viewWidth / viewHeight;

            if (imageRatio > viewRatio) {
                actualHeight = viewWidth / imageRatio;
                offsetY = (viewHeight - actualHeight) / 2;
            } else {
                actualWidth = viewHeight * imageRatio;
                offsetX = (viewWidth - actualWidth) / 2;
            }
        }

        // 转换为场景坐标
        Bounds viewBounds = imageView.getBoundsInLocal();
        Bounds sceneBounds = imageView.localToScene(viewBounds);

        return new BoundingBox(
                sceneBounds.getMinX() + offsetX,
                sceneBounds.getMinY() + offsetY,
                actualWidth,
                actualHeight
        );
    }

    // Getters
    public Image getCurrentImage() { return currentImage; }
    public BufferedImage getCurrentBufferedImage() { return currentBufferedImage; }
    public File getCurrentImageFile() { return currentImageFile; }
    public double getCurrentZoom() { return currentZoom; }

    public ImageView getCurrentImageView() {
        return imageView;
    }
}