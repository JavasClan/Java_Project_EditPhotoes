package imgedit.ui;

import imgedit.core.ImageOperation;
import imgedit.core.operations.*;
import imgedit.utils.ImageUtils;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

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

    // UI组件
    private ImageView imageView;
    private ScrollPane imageScrollPane;
    private ListView<String> historyListView;

    public ImageManager(EditorController controller) {
        this.controller = controller;
    }

    public StackPane createImageDisplayArea() {
        return new ImageDisplayArea(this).create();
    }

    public ScrollPane createHistoryPanel() {
        return new HistoryPanel(this).create();
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
        controller.getUIManager().showProgress("正在加载图片...");

        new Thread(() -> {
            try {
                Image image = new Image(file.toURI().toString());
                currentImageFile = file;
                currentImage = image;
                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);

                // 更新UI
                Platform.runLater(() -> {
                    imageView.setImage(currentImage);
                    updateDisplay();
                    controller.getUIManager().hideProgress();
                    addHistory("打开图片: " + file.getName());
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.getUIManager().hideProgress();
                    controller.getDialogManager().showError("加载失败", "无法加载图片: " + e.getMessage());
                });
            }
        }).start();
    }

    public void saveImage() {
        // 保存图像逻辑
        // ...
    }

    public void applyOperation(ImageOperation operation, String operationName) {
        controller.getUIManager().showProgress("处理中...");

        new Thread(() -> {
            try {
                controller.getImageEditorService().applyOperationAsync(
                        operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            addHistory(operationName);
                            controller.getUIManager().hideProgress();
                        }),
                        exception -> Platform.runLater(() -> {
                            controller.getUIManager().hideProgress();
                            controller.getDialogManager().showError("操作失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.getUIManager().hideProgress();
                    controller.getDialogManager().showError("操作失败", e.getMessage());
                });
            }
        }).start();
    }

    public void undo() {
        // 撤销逻辑
        // ...
    }

    public void redo() {
        // 重做逻辑
        // ...
    }

    public void setZoom(double zoom) {
        currentZoom = zoom;
        if (imageView != null && imageView.getImage() != null) {
            imageView.setScaleX(zoom);
            imageView.setScaleY(zoom);
        }
    }

    private void updateDisplay() {
        // 更新图像显示
        // ...
    }

    private void addHistory(String operation) {
        if (historyListView != null) {
            historyListView.getItems().add(0, operation);
            if (historyListView.getItems().size() > 20) {
                historyListView.getItems().remove(20);
            }
        }
    }

    // Getters and Setters
    public Image getCurrentImage() { return currentImage; }
    public BufferedImage getCurrentBufferedImage() { return currentBufferedImage; }
    public File getCurrentImageFile() { return currentImageFile; }
    public void setImageView(ImageView imageView) { this.imageView = imageView; }
    public void setImageScrollPane(ScrollPane scrollPane) { this.imageScrollPane = scrollPane; }
    public void setHistoryListView(ListView<String> listView) { this.historyListView = listView; }
}