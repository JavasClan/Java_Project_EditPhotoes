package imgedit.ui;

import imgedit.core.ImageOperation;
import imgedit.core.operations.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.geometry.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.animation.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.ArrayList;

/**
 * 对话框管理器
 */
public class DialogManager {

    private final EditorController controller;

    public DialogManager(EditorController controller) {
        this.controller = controller;
    }

    /**
     * 显示主题选择器
     */
    public void showThemeSelector() {
        Dialog<ThemeManager.Theme> dialog = new Dialog<>();
        dialog.setTitle("选择主题");
        dialog.setHeaderText("选择界面主题");

        // 应用主场景样式
        try {
            if (controller.getMainScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🎨 选择主题");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane themeGrid = new GridPane();
        themeGrid.setHgap(15);
        themeGrid.setVgap(15);
        themeGrid.setAlignment(Pos.CENTER);

        ThemeManager.Theme[] themes = ThemeManager.Theme.values();
        for (int i = 0; i < themes.length; i++) {
            ThemeManager.Theme theme = themes[i];
            VBox themeItem = createThemePreview(theme);
            themeItem.setOnMouseClicked(e -> {
                controller.applyTheme(theme);
                dialog.setResult(theme);
                dialog.close();
            });

            themeGrid.add(themeItem, i % 3, i / 3);
        }

        content.getChildren().addAll(titleLabel, themeGrid);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    /**
     * 创建主题预览
     */
    private VBox createThemePreview(ThemeManager.Theme theme) {
        VBox preview = new VBox(10);
        preview.setAlignment(Pos.CENTER);
        preview.setPadding(new Insets(15));
        preview.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 10;");
        preview.setOnMouseEntered(e -> preview.setStyle(
                "-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 10; -fx-cursor: hand;"
        ));
        preview.setOnMouseExited(e -> preview.setStyle(
                "-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 10;"
        ));

        HBox colorSample = new HBox(5);
        colorSample.setAlignment(Pos.CENTER);

        Color[] colors = controller.getThemeManager().getThemeColors(theme);
        for (Color color : colors) {
            Circle colorCircle = new Circle(12);
            colorCircle.setFill(color);
            colorSample.getChildren().add(colorCircle);
        }

        Label themeLabel = new Label(theme.getDisplayName());
        themeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        preview.getChildren().addAll(colorSample, themeLabel);
        return preview;
    }

    /**
     * 显示豆包图生图对话框
     */
    public void showArkImageDialog() {
        if (!controller.getArkManager().isAvailable()) {
            controller.showError("功能未就绪", "请检查config.properties配置");
            return;
        }

        if (controller.getImageManager().getCurrentImageFile() == null) {
            controller.showError("提示", "请先在主界面加载一张参考图片");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("豆包图生图 - AI 创作中心");

        // 应用样式
        try {
            if (controller.getMainScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 自定义头部
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 15, 0));
        Label iconLbl = new Label("🎨");
        iconLbl.setStyle("-fx-font-size: 40px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        Label titleLbl = new Label("AI 灵感创作");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subTitleLbl = new Label("基于 " + controller.getImageManager().getCurrentImageFile().getName() + " 进行再创作");
        subTitleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        headerBox.getChildren().addAll(iconLbl, titleLbl, subTitleLbl);

        // 3. 提示词输入区域
        VBox promptBox = new VBox(8);
        Label pLabel = new Label("✨ 你的创意指令:");
        pLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        TextArea pArea = new TextArea();
        pArea.setPromptText("例如：把背景变成赛博朋克风格的街道，添加霓虹灯光效，保持主体清晰...");
        pArea.setWrapText(true);
        pArea.setPrefRowCount(3);
        pArea.setPrefHeight(80);
        promptBox.getChildren().addAll(pLabel, pArea);

        // 4. 输出设置区域
        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10);
        settingsGrid.setVgap(10);
        settingsGrid.setPadding(new Insets(15));
        settingsGrid.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");

        Label dirLabel = new Label("保存位置:");
        dirLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
        TextField dirField = new TextField("D:/generated_images/");
        Button browseBtn = new Button("📂 浏览");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) dirField.setText(f.getAbsolutePath());
        });

        Label nameLabel = new Label("文件命名:");
        nameLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
        TextField nameField = new TextField("ai_art_" + System.currentTimeMillis());

        settingsGrid.add(dirLabel, 0, 0);
        settingsGrid.add(dirField, 1, 0);
        settingsGrid.add(browseBtn, 2, 0);
        settingsGrid.add(nameLabel, 0, 1);
        settingsGrid.add(nameField, 1, 1);

        // 让输入框自动拉伸
        GridPane.setHgrow(dirField, Priority.ALWAYS);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        // 5. 状态与进度
        VBox statusBox = new VBox(5);
        Label statusLabel = new Label("准备就绪");
        statusLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);

        ProgressBar pBar = new ProgressBar();
        pBar.setVisible(false);
        pBar.setMaxWidth(Double.MAX_VALUE);
        statusBox.getChildren().addAll(statusLabel, pBar);

        // 6. 生成按钮
        Button genBtn = new Button("🚀  立即生成");
        genBtn.setMaxWidth(Double.MAX_VALUE);
        genBtn.setPrefHeight(40);
        genBtn.setStyle("-fx-font-size: 14px;");

        // 组装主内容
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(480);
        content.getChildren().addAll(headerBox, promptBox, settingsGrid, statusBox, genBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // 隐藏默认的关闭按钮
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        // 7. 生成逻辑
        genBtn.setOnAction(e -> {
            String prompt = pArea.getText().trim();
            if (prompt.isEmpty()) {
                pArea.setStyle("-fx-border-color: #ff5252;");
                pArea.setPromptText("⚠️ 请先输入提示词！");
                return;
            }

            // 锁定界面
            pArea.setDisable(true);
            settingsGrid.setDisable(true);
            genBtn.setDisable(true);
            pBar.setVisible(true);
            statusLabel.setText("✨ AI 正在绘图，请稍候 (约5-10秒)...");
            statusLabel.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold;");

            new Thread(() -> {
                try {
                    String saveDir = dirField.getText();
                    String fileName = nameField.getText();

                    // 调用生成接口
                    String resultPath = controller.getArkManager().generateImage(
                            controller.getImageManager().getCurrentImageFile().getAbsolutePath(),
                            prompt, saveDir, fileName);

                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("✅ 生成成功！");
                        pBar.setVisible(false);

                        // 显示成功弹窗
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("创作完成");
                        alert.setHeaderText("您的 AI 作品已生成");
                        alert.setContentText("保存路径: " + resultPath + "\n\n是否立即在编辑器中打开？");

                        try {
                            if (controller.getMainScene() != null) {
                                alert.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
                            }
                        } catch (Exception ex) {}

                        alert.showAndWait().ifPresent(r -> {
                            if (r == ButtonType.OK) {
                                controller.getImageManager().loadImage(new File(resultPath));
                                dialog.close();
                            } else {
                                // 如果不打开，解锁界面允许再次生成
                                pArea.setDisable(false);
                                settingsGrid.setDisable(false);
                                genBtn.setDisable(false);
                                genBtn.setText("🔄  再来一张");
                                nameField.setText("ai_art_" + System.currentTimeMillis());
                            }
                        });
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("❌ 生成失败: " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #ff5252;");
                        pBar.setVisible(false);
                        genBtn.setDisable(false);
                        pArea.setDisable(false);
                        settingsGrid.setDisable(false);
                    });
                }
            }).start();
        });

        dialog.showAndWait();
    }

    /**
     * 显示批量处理对话框
     */
    public void showBatchProcessingDialog() {
        // 弹出文件选择器选择多张图片
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择多张图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(controller.getPrimaryStage());
        if (files != null && !files.isEmpty()) {
            showBatchProcessingDialog(files);
        }
    }

    /**
     * 显示批量处理对话框（传入文件列表）
     */
    private void showBatchProcessingDialog(List<File> files) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("批量工坊");

        try {
            if (controller.getMainScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 头部
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));
        Label icon = new Label("🏭");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label("批量图像处理流水线");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subtitle = new Label("已就绪队列: " + files.size() + " 个文件");
        subtitle.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold; " +
                "-fx-background-color: #f0f4ff; -fx-padding: 4 10; -fx-background-radius: 12;");
        header.getChildren().addAll(icon, title, subtitle);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setPrefWidth(450);

        // 1. 操作选择卡片
        VBox opCard = new VBox(10);
        opCard.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; " +
                "-fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");
        Label opLabel = new Label("选择流水线操作:");
        opLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        ComboBox<String> opCombo = new ComboBox<>();
        opCombo.getItems().addAll("灰度化", "调整亮度", "调整对比度", "调整饱和度", "模糊", "边缘检测", "旋转90度");
        opCombo.setValue("灰度化");
        opCombo.setMaxWidth(Double.MAX_VALUE);

        // 参数滑块（默认隐藏）
        VBox paramBox = new VBox(5);
        paramBox.setVisible(false);
        paramBox.setManaged(false);
        Label paramLbl = new Label("强度参数:");
        paramLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        Slider paramSlider = new Slider(-100, 100, 0);
        paramBox.getChildren().addAll(paramLbl, paramSlider);

        opCombo.setOnAction(e -> {
            String val = opCombo.getValue();
            boolean showSlider = val.contains("亮度") || val.contains("对比度") ||
                    val.contains("饱和度") || val.contains("模糊");
            paramBox.setVisible(showSlider);
            paramBox.setManaged(showSlider);
        });

        opCard.getChildren().addAll(opLabel, opCombo, paramBox);

        // 2. 输出设置卡片
        VBox outCard = new VBox(10);
        outCard.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; " +
                "-fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");
        Label outLabel = new Label("输出命名规则:");
        outLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        TextField suffixField = new TextField("_processed");
        suffixField.setPromptText("例如: _edit, _v2");
        outCard.getChildren().addAll(outLabel, suffixField);

        // 按钮
        Button startBtn = new Button("🚀  启动流水线");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setPrefHeight(45);

        content.getChildren().addAll(header, opCard, outCard, startBtn);
        dialog.getDialogPane().setContent(content);

        // 关闭按钮逻辑
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setVisible(false);
        closeBtn.setManaged(false);

        startBtn.setOnAction(e -> {
            dialog.close();
            executeBatchProcessing(files, opCombo.getValue(), paramSlider.getValue(), suffixField.getText());
        });

        dialog.showAndWait();
    }

    /**
     * 执行批量处理
     */
    private void executeBatchProcessing(List<File> files, String operationType,
                                        double paramValue, String suffix) {
        controller.showProgress("批量处理中...");

        new Thread(() -> {
            try {
                List<BatchOperation.BatchTask> tasks = new ArrayList<>();

                // 加载所有图片
                for (File file : files) {
                    try {
                        BufferedImage img = javax.imageio.ImageIO.read(file);
                        if (img != null) {
                            // 创建批处理任务
                            BatchOperation.BatchConfig config = new BatchOperation.BatchConfig(
                                    BatchOperation.BatchMode.SINGLE_OPERATION,
                                    new ArrayList<>(),
                                    4,  // 线程数
                                    false,
                                    suffix
                            );

                            BatchOperation.BatchTask task = new BatchOperation.BatchTask(
                                    img,
                                    file.getName(),
                                    config
                            );
                            tasks.add(task);
                        }
                    } catch (Exception e) {
                        System.err.println("无法加载图片: " + file.getName() + " - " + e.getMessage());
                    }
                }

                if (tasks.isEmpty()) {
                    javafx.application.Platform.runLater(() -> {
                        controller.hideProgress();
                        controller.showError("批量处理失败", "无法加载任何图片");
                    });
                    return;
                }

                // 创建操作
                ImageOperation operation = createBatchOperation(operationType, paramValue);

                // 创建批量处理操作
                BatchOperation batchOp = BatchOperation.createSingleOperationBatch(tasks, operation);

                // 创建进度监听器
                BatchOperation.BatchProgressListener listener = new BatchOperation.BatchProgressListener() {
                    private int processed = 0;
                    private int total = tasks.size();

                    @Override
                    public void onProgress(String imageName, int processedCount, int totalCount) {
                        javafx.application.Platform.runLater(() -> {
                            controller.updateStatus(String.format("批量处理: %s (%d/%d)",
                                    imageName, processedCount, totalCount));
                        });
                    }

                    @Override
                    public void onTaskComplete(String imageName, boolean success) {
                        processed++;
                        javafx.application.Platform.runLater(() -> {
                            if (success) {
                                // 添加到历史记录
                                // 注意：这里需要ImageManager的addHistory方法
                                // controller.getImageManager().addHistory("批量处理: " + imageName);
                            }
                        });
                    }

                    @Override
                    public void onBatchComplete(int successCount, int totalCount) {
                        javafx.application.Platform.runLater(() -> {
                            controller.hideProgress();
                            if (successCount == totalCount) {
                                controller.showSuccess("批量处理完成",
                                        String.format("成功处理 %d/%d 张图片", successCount, totalCount));
                            } else {
                                controller.showWarning("批量处理完成",
                                        String.format("成功处理 %d/%d 张图片，失败 %d 张",
                                                successCount, totalCount, totalCount - successCount));
                            }
                        });
                    }
                };

                // 执行批量处理
                List<BatchOperation.BatchResult> results = batchOp.executeBatch(listener);

                // 保存处理后的图片
                for (int i = 0; i < results.size(); i++) {
                    BatchOperation.BatchResult result = results.get(i);
                    if (result.isSuccess() && result.getResultImage() != null) {
                        try {
                            String originalName = files.get(i).getName();
                            int dotIndex = originalName.lastIndexOf('.');
                            String baseName = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName;
                            String extension = dotIndex > 0 ? originalName.substring(dotIndex) : ".png";
                            String newName = baseName + suffix + extension;
                            File outputFile = new File(files.get(i).getParent(), newName);

                            String format = extension.substring(1).toUpperCase();
                            if (format.equals("JPG") || format.equals("JPEG")) {
                                format = "JPEG";
                            } else if (format.equals("PNG")) {
                                format = "PNG";
                            } else {
                                format = "PNG";
                            }

                            javax.imageio.ImageIO.write(result.getResultImage(), format, outputFile);
                        } catch (Exception e) {
                            System.err.println("保存失败: " + files.get(i).getName() + " - " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    controller.hideProgress();
                    controller.showError("批量处理失败", e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    /**
     * 根据类型创建批量处理操作
     */
    private ImageOperation createBatchOperation(String operationType, double paramValue) {
        switch (operationType) {
            case "灰度化":
                return GrayscaleOperation.create();
            case "调整亮度":
                BrightnessOperation.BrightnessMode mode = paramValue >= 0 ?
                        BrightnessOperation.BrightnessMode.INCREASE :
                        BrightnessOperation.BrightnessMode.DECREASE;
                float intensity = (float)(Math.abs(paramValue) / 100.0);
                return new BrightnessOperation(mode, intensity);
            case "调整对比度":
                float contrastLevel = (float)(paramValue / 100.0f + 1.0f);
                return new ContrastOperation(contrastLevel);
            case "调整饱和度":
                float saturationFactor = (float)(paramValue / 100.0f + 1.0f);
                return new SaturationOperation(saturationFactor);
            case "模糊":
                BlurOperation.BlurIntensity intensityLevel;
                if (paramValue <= 33) {
                    intensityLevel = BlurOperation.BlurIntensity.LIGHT;
                } else if (paramValue <= 66) {
                    intensityLevel = BlurOperation.BlurIntensity.MEDIUM;
                } else {
                    intensityLevel = BlurOperation.BlurIntensity.STRONG;
                }
                return new BlurOperation(intensityLevel);
            case "边缘检测":
                return EdgeDetectionOperation.createAllEdges();
            case "旋转90度":
                return RotateOperation.create90Degree();
            default:
                return GrayscaleOperation.create();
        }
    }

    /**
     * 显示艺术风格对话框
     */
    public void showArtisticStyleDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("艺术画廊");

        try {
            if (controller.getMainScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 头部设计
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label icon = new Label("🎨");
        icon.setStyle("-fx-font-size: 48px; " +
                "-fx-effect: dropshadow(gaussian, rgba(255, 153, 102, 0.4), 10, 0, 0, 2);");

        Label title = new Label("选择艺术流派");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Give your photo a creative soul");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff9966; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(255, 153, 102, 0.1); " +
                "-fx-padding: 4 12; -fx-background-radius: 20;");

        header.getChildren().addAll(icon, title, subtitle);

        // 风格卡片网格
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));
        grid.setAlignment(Pos.CENTER);

        // 定义所有支持的风格
        addStyleCard(grid, 0, 0, "油画", "Oil Painting", "🖼️",
                "厚重的笔触与质感", dialog, ArtisticStyleOperation.ArtisticStyle.OIL_PAINTING);
        addStyleCard(grid, 1, 0, "水彩", "Watercolor", "💧",
                "清透晕染的效果", dialog, ArtisticStyleOperation.ArtisticStyle.WATERCOLOR);
        addStyleCard(grid, 0, 1, "素描", "Sketch", "✏️",
                "纯粹的黑白线条", dialog, ArtisticStyleOperation.ArtisticStyle.PENCIL_SKETCH);
        addStyleCard(grid, 1, 1, "卡通", "Cartoon", "🦄",
                "二次元明快色彩", dialog, ArtisticStyleOperation.ArtisticStyle.CARTOON);
        addStyleCard(grid, 0, 2, "马赛克", "Mosaic", "🧩",
                "像素化复古风", dialog, ArtisticStyleOperation.ArtisticStyle.MOSAIC);

        // 包装在滚动容器中
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(360);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.getStyleClass().add("edge-to-edge");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(420);
        content.getChildren().addAll(header, scroll);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
    }

    /**
     * 创建艺术风格卡片
     */
    private void addStyleCard(GridPane grid, int col, int row, String name, String enName,
                              String emoji, String desc, Dialog<Void> dialog,
                              ArtisticStyleOperation.ArtisticStyle style) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setPrefWidth(160);

        // 样式定义
        String normalStyle = "-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #e5e7eb; " +
                "-fx-border-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 5, 0, 0, 0);";

        String hoverStyle = "-fx-background-color: linear-gradient(to bottom right, #ff9966, #ff5e62); " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: transparent; " +
                "-fx-border-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(255, 94, 98, 0.4), 10, 0, 0, 2);";

        card.setStyle(normalStyle);

        // 内容构建
        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 28px;");

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        Label enLbl = new Label(enName);
        enLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        card.getChildren().addAll(iconLbl, nameLbl, enLbl, descLbl);

        // 交互事件
        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            enLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-weight: bold;");
            descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);");
            card.setTranslateY(-3);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(normalStyle);
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #374151;");
            enLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
            descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            card.setTranslateY(0);
        });

        card.setOnMouseClicked(e -> {
            dialog.close();
            // 应用风格
            ArtisticStyleOperation operation = new ArtisticStyleOperation(style,
                    new ArtisticStyleOperation.StyleParameters(0.7f, 5, 0.5f));
            controller.getImageManager().applyOperation(operation, "应用艺术风格: " + name);
        });

        grid.add(card, col, row);
    }

    /**
     * 显示文本输入对话框
     */
    public void showTextInputDialog(String title, String header, String defaultValue,
                                    Consumer<String> onAccept) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        // 使用支持中文的字体
        Font chineseFont = Font.font("Microsoft YaHei", 14);
        TextArea textArea = new TextArea(defaultValue);
        textArea.setFont(chineseFont);
        textArea.setPromptText("请输入...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(new Label(header), textArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return textArea.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && !result.trim().isEmpty()) {
                onAccept.accept(result);
            }
        });
    }

    /**
     * 显示错误对话框
     */
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            if (controller.getMainScene() != null) {
                alert.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        alert.showAndWait();
    }

    /**
     * 显示成功对话框
     */
    public void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            if (controller.getMainScene() != null) {
                alert.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        alert.showAndWait();
    }

    /**
     * 显示警告对话框
     */
    public void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            if (controller.getMainScene() != null) {
                alert.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        alert.showAndWait();
    }

    /**
     * 显示帮助对话框
     */
    public void showHelp() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("关于");

        try {
            if (controller.getMainScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setPrefWidth(400);

        // Logo
        StackPane logoPane = new StackPane();
        Circle bg = new Circle(40, Color.web("#667eea"));
        Label icon = new Label("🎨");
        icon.setStyle("-fx-font-size: 40px; -fx-text-fill: white;");
        logoPane.getChildren().addAll(bg, icon);
        logoPane.setEffect(new DropShadow(15, Color.rgb(102, 126, 234, 0.4)));

        Label title = new Label("AI Image Editor Pro");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label ver = new Label("Version 3.1.0 Ultimate");
        ver.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        // 快捷键列表
        VBox keys = new VBox(8);
        keys.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; -fx-background-radius: 8;");
        keys.getChildren().addAll(
                createKeyRow("Ctrl + O", "打开图片"),
                createKeyRow("Ctrl + S", "保存图片"),
                createKeyRow("Ctrl + Z", "撤销操作"),
                createKeyRow("Ctrl + T", "切换主题")
        );

        Button closeBtn = new Button("我知道了");
        closeBtn.setPrefWidth(120);
        closeBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(logoPane, title, ver, keys, closeBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
    }

    /**
     * 创建快捷键行
     */
    private HBox createKeyRow(String key, String desc) {
        HBox row = new HBox(10);
        Label k = new Label(key);
        k.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; " +
                "-fx-text-fill: #667eea; -fx-background-color: rgba(102,126,234,0.1); " +
                "-fx-padding: 2 6; -fx-background-radius: 4;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: #4b5563;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(d, sp, k);
        return row;
    }

    /**
     * 显示裁剪对话框
     */
    public void showCropDialog(int imageWidth, int imageHeight, Consumer<java.awt.Rectangle> onAccept) {
        Dialog<java.awt.Rectangle> dialog = new Dialog<>();
        dialog.setTitle("裁剪图片");
        dialog.setHeaderText("输入裁剪区域");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField xField = new TextField("0");
        TextField yField = new TextField("0");
        TextField widthField = new TextField(String.valueOf(imageWidth / 2));
        TextField heightField = new TextField(String.valueOf(imageHeight / 2));

        grid.add(new Label("X坐标:"), 0, 0);
        grid.add(xField, 1, 0);
        grid.add(new Label("Y坐标:"), 0, 1);
        grid.add(yField, 1, 1);
        grid.add(new Label("宽度:"), 0, 2);
        grid.add(widthField, 1, 2);
        grid.add(new Label("高度:"), 0, 3);
        grid.add(heightField, 1, 3);

        // 添加图片尺寸信息
        Label sizeInfo = new Label(String.format("图片尺寸: %d × %d", imageWidth, imageHeight));
        sizeInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        grid.add(sizeInfo, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());

                    return new java.awt.Rectangle(x, y, width, height);
                } catch (NumberFormatException e) {
                    showError("输入错误", "请输入有效的数字");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cropArea -> {
            if (cropArea.width > 0 && cropArea.height > 0) {
                onAccept.accept(cropArea);
            }
        });
    }
}