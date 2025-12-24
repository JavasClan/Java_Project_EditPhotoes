package imgedit.ui;

import imgedit.core.ImageOperation;
import imgedit.core.operations.*;
import imgedit.core.operations.ArtisticStyleOperation.ArtisticStyle;
import imgedit.core.operations.ArtisticStyleOperation.StyleParameters;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.scene.effect.DropShadow;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 对话框管理器 - 终极完整版 (包含所有修复和功能)
 */
public class DialogManager {

    private final EditorController controller;

    public DialogManager(EditorController controller) {
        this.controller = controller;
    }

    // ==================== 1. 深度美化的通用弹窗 ====================

    public void showSuccess(String title, String message) {
        showStyledMessage(title, message, "success");
    }

    public void showWarning(String title, String message) {
        showStyledMessage(title, message, "warning");
    }

    public void showError(String title, String message) {
        showStyledMessage(title, message, "error");
    }

    private void showStyledMessage(String title, String message, String type) {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.TRANSPARENT);

            String iconChar;
            String colorHex;
            String headerColor;

            switch (type) {
                case "success":
                    iconChar = "✅"; colorHex = "#10b981"; headerColor = "#ecfdf5"; break;
                case "error":
                    iconChar = "❌"; colorHex = "#ef4444"; headerColor = "#fef2f2"; break;
                case "warning":
                default:
                    iconChar = "⚠️"; colorHex = "#f59e0b"; headerColor = "#fffbeb"; break;
            }

            VBox root = new VBox(0);
            root.setAlignment(Pos.CENTER);
            root.setPrefWidth(360);
            root.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: rgba(0,0,0,0.1); -fx-border-width: 1;");
            root.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.15)));

            VBox headerBox = new VBox(10);
            headerBox.setAlignment(Pos.CENTER);
            headerBox.setPadding(new Insets(20, 20, 10, 20));
            headerBox.setStyle("-fx-background-color: " + headerColor + "; -fx-background-radius: 12 12 0 0;");

            Label icon = new Label(iconChar);
            icon.setStyle("-fx-font-size: 40px; -fx-font-family: 'Segoe UI Emoji';");
            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
            headerBox.getChildren().addAll(icon, titleLbl);

            VBox bodyBox = new VBox(20);
            bodyBox.setAlignment(Pos.CENTER);
            bodyBox.setPadding(new Insets(20));
            bodyBox.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;");

            Label msgLbl = new Label(message);
            msgLbl.setWrapText(true);
            msgLbl.setAlignment(Pos.CENTER);
            msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563; -fx-text-alignment: CENTER;");

            Button okBtn = new Button("我知道了");
            okBtn.setPrefWidth(120);
            String btnStyle = "-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20;";
            okBtn.setStyle(btnStyle);
            okBtn.setOnMouseEntered(e -> okBtn.setOpacity(0.9));
            okBtn.setOnMouseExited(e -> okBtn.setOpacity(1.0));
            okBtn.setOnAction(e -> dialog.close());

            bodyBox.getChildren().addAll(msgLbl, okBtn);
            root.getChildren().addAll(headerBox, bodyBox);

            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().setStyle("-fx-background-color: transparent;");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            if (closeButton != null) {
                closeButton.setVisible(false);
                closeButton.setManaged(false);
            }

            dialog.showAndWait();
        });
    }

    // ==================== 2. 批量处理模块 (功能全开) ====================

    public void showBatchProcessingDialog() {
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

    private void showBatchProcessingDialog(List<File> files) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("批量工坊");
        applyMainStyles(dialog);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(480);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("-fx-background-color:transparent;");

        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        Label icon = new Label("🏭");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label("批量图像处理流水线");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subtitle = new Label("待处理: " + files.size() + " 个文件");
        subtitle.setStyle("-fx-text-fill: #667eea;");
        header.getChildren().addAll(icon, title, subtitle);

        VBox optionsBox = new VBox(12);
        optionsBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e5e7eb;");

        // 1. 旋转
        CheckBox rotateCb = new CheckBox("旋转图片");
        ComboBox<String> rotateCombo = new ComboBox<>();
        rotateCombo.getItems().addAll("90度 (顺时针)", "180度", "270度 (逆时针)");
        rotateCombo.getSelectionModel().selectFirst();
        HBox rotateBox = new HBox(10, rotateCb, rotateCombo);
        rotateBox.setAlignment(Pos.CENTER_LEFT);
        bindFieldStyle(rotateCombo, rotateCb);

        // 2. 翻转
        CheckBox flipCb = new CheckBox("翻转图片");
        ComboBox<String> flipCombo = new ComboBox<>();
        flipCombo.getItems().addAll("水平翻转", "垂直翻转");
        flipCombo.getSelectionModel().selectFirst();
        HBox flipBox = new HBox(10, flipCb, flipCombo);
        flipBox.setAlignment(Pos.CENTER_LEFT);
        bindFieldStyle(flipCombo, flipCb);

        // 3. 调整大小
        CheckBox resizeCb = new CheckBox("调整大小");
        TextField wField = new TextField(""); wField.setPromptText("宽 (px)"); wField.setPrefWidth(80);
        TextField hField = new TextField(""); hField.setPromptText("高 (px)"); hField.setPrefWidth(80);
        HBox resizeBox = new HBox(10, resizeCb, new Label("W:"), wField, new Label("H:"), hField);
        resizeBox.setAlignment(Pos.CENTER_LEFT);
        bindFieldStyle(wField, resizeCb);
        bindFieldStyle(hField, resizeCb);

        // 4. 艺术风格 (已加回！)
        CheckBox artisticCb = new CheckBox("艺术风格");
        ComboBox<String> styleCombo = new ComboBox<>();
        styleCombo.getItems().addAll("油画", "素描", "马赛克", "卡通", "老照片");
        styleCombo.getSelectionModel().selectFirst();
        HBox artisticBox = new HBox(10, artisticCb, styleCombo);
        artisticBox.setAlignment(Pos.CENTER_LEFT);
        bindFieldStyle(styleCombo, artisticCb);

        // 5. 滤镜
        CheckBox filterCb = new CheckBox("应用滤镜");
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("灰度化");
        filterCombo.getSelectionModel().selectFirst();
        HBox filterBox = new HBox(10, filterCb, filterCombo);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        bindFieldStyle(filterCombo, filterCb);

        // 6. 格式转换
        CheckBox formatCb = new CheckBox("格式转换");
        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("png", "jpg");
        formatCombo.getSelectionModel().selectFirst();
        HBox formatBox = new HBox(10, formatCb, formatCombo);
        formatBox.setAlignment(Pos.CENTER_LEFT);
        bindFieldStyle(formatCombo, formatCb);

        optionsBox.getChildren().addAll(
                new Label("处理选项:"), rotateBox, flipBox, resizeBox, artisticBox, filterBox, formatBox
        );

        VBox outBox = new VBox(10);
        outBox.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e5e7eb;");

        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        TextField dirField = new TextField(files.get(0).getParent());
        HBox.setHgrow(dirField, Priority.ALWAYS);
        Button dirBtn = new Button("📂 浏览");
        dirBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) dirField.setText(f.getAbsolutePath());
        });
        dirBox.getChildren().addAll(new Label("输出目录:"), dirField, dirBtn);

        HBox suffixBox = new HBox(10);
        suffixBox.setAlignment(Pos.CENTER_LEFT);
        TextField suffixField = new TextField("_processed");
        suffixBox.getChildren().addAll(new Label("文件名后缀:"), suffixField);

        outBox.getChildren().addAll(dirBox, suffixBox);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        Label statusLabel = new Label("准备就绪");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: grey;");

        Button startBtn = new Button("🚀  启动处理");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");

        root.getChildren().addAll(header, optionsBox, outBox, progressBar, statusLabel, startBtn);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        startBtn.setOnAction(e -> {
            boolean doRotate = rotateCb.isSelected();
            int rotIdx = rotateCombo.getSelectionModel().getSelectedIndex();
            int rotateAngle = (rotIdx == 0) ? 90 : (rotIdx == 1 ? 180 : 270);

            boolean doFlip = flipCb.isSelected();
            boolean flipHorz = flipCombo.getSelectionModel().getSelectedIndex() == 0;

            boolean doResize = resizeCb.isSelected();
            int w = 0, h = 0;
            if (doResize) {
                if (wField.getText().trim().isEmpty() || hField.getText().trim().isEmpty()) {
                    showError("参数错误", "请填写调整大小的宽度和高度！");
                    return;
                }
                try {
                    w = Integer.parseInt(wField.getText().trim());
                    h = Integer.parseInt(hField.getText().trim());
                } catch (Exception ex) {
                    showError("错误", "宽高必须为有效的整数"); return;
                }
            }
            final int finalW = w;
            final int finalH = h;

            boolean doArtistic = artisticCb.isSelected();
            String artisticStyle = styleCombo.getValue();

            boolean doFilter = filterCb.isSelected();
            boolean isGray = filterCombo.getValue().equals("灰度化");

            boolean doFormat = formatCb.isSelected();
            String targetFormat = doFormat ? formatCombo.getValue() : "png";

            String outDir = dirField.getText();
            String suffix = suffixField.getText();

            startBtn.setDisable(true);
            controller.showProgress("批量处理中...");

            new Thread(() -> {
                int total = files.size();
                AtomicInteger count = new AtomicInteger(0);
                int success = 0;

                for (File file : files) {
                    try {
                        Platform.runLater(() -> statusLabel.setText("正在处理: " + file.getName()));

                        BufferedImage image = ImageIO.read(file);
                        if (image != null) {
                            if (doRotate) image = processRotate(image, rotateAngle);
                            if (doFlip) image = processFlip(image, flipHorz);
                            if (doResize) image = processResize(image, finalW, finalH);

                            // 艺术风格处理 (已加回！)
                            if (doArtistic) image = processArtisticStyle(image, artisticStyle);

                            if (doFilter && isGray) image = processGrayscale(image);

                            String name = file.getName();
                            int dot = name.lastIndexOf('.');
                            String base = dot > 0 ? name.substring(0, dot) : name;
                            String ext = doFormat ? targetFormat : (dot > 0 ? name.substring(dot + 1) : "png");

                            File dest = new File(outDir, base + suffix + "." + ext);
                            ImageIO.write(image, ext, dest);
                            success++;
                        }
                    } catch (Exception ex) {
                        System.err.println("处理失败: " + file.getName());
                    }

                    int current = count.incrementAndGet();
                    Platform.runLater(() -> progressBar.setProgress((double) current / total));
                }

                int finalSuccess = success;
                Platform.runLater(() -> {
                    controller.hideProgress();
                    startBtn.setDisable(false);
                    statusLabel.setText("完成！");
                    dialog.close();
                    showSuccess("批量完成", "成功处理 " + finalSuccess + " / " + total + " 张图片");
                });

            }).start();
        });

        dialog.showAndWait();
    }

    private void bindFieldStyle(Control field, CheckBox checkBox) {
        field.disableProperty().bind(checkBox.selectedProperty().not());
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateDisabledStyle(field, !newVal));
        updateDisabledStyle(field, !checkBox.isSelected());
    }

    private void updateDisabledStyle(Control field, boolean isDisabled) {
        if (isDisabled) {
            // 纯黑字，深灰提示，浅灰底 -> 清晰可见
            field.setStyle("-fx-opacity: 1.0; -fx-background-color: #eaeaea; -fx-text-fill: #000000; -fx-prompt-text-fill: #555555;");
        } else {
            field.setStyle("-fx-opacity: 1.0; -fx-background-color: white; -fx-text-fill: #1f2937;");
        }
    }

    // --- AWT 底层算法 ---
    private BufferedImage processRotate(BufferedImage src, int angle) {
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = w, newH = h;
        if (angle == 90 || angle == 270) { newW = h; newH = w; }
        BufferedImage res = new BufferedImage(newW, newH, src.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : src.getType());
        Graphics2D g = res.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newW - w) / 2.0, (newH - h) / 2.0);
        at.rotate(Math.toRadians(angle), w / 2.0, h / 2.0);
        g.setTransform(at);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return res;
    }

    private BufferedImage processFlip(BufferedImage src, boolean horizontal) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage res = new BufferedImage(w, h, src.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : src.getType());
        Graphics2D g = res.createGraphics();
        int sx1 = horizontal ? w : 0; int sy1 = horizontal ? 0 : h;
        int sx2 = horizontal ? 0 : w; int sy2 = horizontal ? h : 0;
        g.drawImage(src, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
        g.dispose();
        return res;
    }

    private BufferedImage processResize(BufferedImage src, int targetW, int targetH) {
        BufferedImage res = new BufferedImage(targetW, targetH, src.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : src.getType());
        Graphics2D g = res.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return res;
    }

    private BufferedImage processGrayscale(BufferedImage src) {
        BufferedImage res = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = res.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return res;
    }

    /**
     * 原生实现的简易艺术风格滤镜
     */
    private BufferedImage processArtisticStyle(BufferedImage src, String styleName) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = res.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        switch (styleName) {
            case "马赛克":
                int blockSize = 15;
                for (int y = 0; y < h; y += blockSize) {
                    for (int x = 0; x < w; x += blockSize) {
                        int pixel = res.getRGB(x, y);
                        for (int by = 0; by < blockSize && y + by < h; by++) {
                            for (int bx = 0; bx < blockSize && x + bx < w; bx++) {
                                res.setRGB(x + bx, y + by, pixel);
                            }
                        }
                    }
                }
                break;
            case "素描":
                BufferedImage gray = processGrayscale(src);
                float[] edgeKernel = { 0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f };
                BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, edgeKernel));
                res = op.filter(gray, null);
                break;
            case "油画":
                float[] blurKernel = { 0.1f, 0.1f, 0.1f, 0.1f, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f };
                BufferedImageOp blur = new ConvolveOp(new Kernel(3, 3, blurKernel));
                res = blur.filter(src, null);
                break;
            case "卡通":
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = src.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int gVal = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        r = (r / 64) * 64;
                        gVal = (gVal / 64) * 64;
                        b = (b / 64) * 64;
                        int newPixel = (r << 16) | (gVal << 8) | b;
                        res.setRGB(x, y, newPixel);
                    }
                }
                break;
            case "老照片":
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = src.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int gVal = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        int tr = (int)(0.393*r + 0.769*gVal + 0.189*b);
                        int tg = (int)(0.349*r + 0.686*gVal + 0.168*b);
                        int tb = (int)(0.272*r + 0.534*gVal + 0.131*b);
                        if(tr > 255) tr = 255; if(tg > 255) tg = 255; if(tb > 255) tb = 255;
                        res.setRGB(x, y, (tr << 16) | (tg << 8) | tb);
                    }
                }
                break;
        }
        return res;
    }

    // ==================== 3. 豆包图生图 (完整逻辑) ====================

    public void showArkImageDialog() {
        if (!controller.getArkManager().isAvailable()) {
            showError("功能未就绪", "请检查config.properties配置");
            return;
        }
        if (controller.getImageManager().getCurrentImageFile() == null) {
            showError("提示", "请先在主界面加载一张参考图片");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("豆包图生图");
        applyMainStyles(dialog);

        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 15, 0));
        Label iconLbl = new Label("🎨");
        iconLbl.setStyle("-fx-font-size: 40px;");
        Label titleLbl = new Label("AI 灵感创作");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        headerBox.getChildren().addAll(iconLbl, titleLbl);

        VBox promptBox = new VBox(8);
        Label pLabel = new Label("✨ 你的创意指令:");
        TextArea pArea = new TextArea();
        pArea.setPromptText("例如：把背景变成赛博朋克风格的街道...");
        pArea.setWrapText(true);
        pArea.setPrefHeight(80);
        promptBox.getChildren().addAll(pLabel, pArea);

        GridPane settings = new GridPane();
        settings.setHgap(10); settings.setVgap(10);
        TextField dirField = new TextField("D:/generated_images/");
        Button browseBtn = new Button("📂");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) dirField.setText(f.getAbsolutePath());
        });
        TextField nameField = new TextField("ai_" + System.currentTimeMillis());

        settings.add(new Label("保存位置:"), 0, 0);
        settings.add(dirField, 1, 0);
        settings.add(browseBtn, 2, 0);
        settings.add(new Label("文件命名:"), 0, 1);
        settings.add(nameField, 1, 1);
        GridPane.setHgrow(dirField, Priority.ALWAYS);

        VBox statusBox = new VBox(5);
        Label statusLabel = new Label("准备就绪");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        ProgressBar pBar = new ProgressBar();
        pBar.setVisible(false);
        pBar.setMaxWidth(Double.MAX_VALUE);
        statusBox.getChildren().addAll(statusLabel, pBar);

        Button genBtn = new Button("🚀 立即生成");
        genBtn.setMaxWidth(Double.MAX_VALUE);
        genBtn.setPrefHeight(40);
        genBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(450);
        content.getChildren().addAll(headerBox, promptBox, settings, statusBox, genBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        genBtn.setOnAction(e -> {
            String prompt = pArea.getText().trim();
            if (prompt.isEmpty()) { showError("提示", "请输入提示词"); return; }

            pArea.setDisable(true); genBtn.setDisable(true); pBar.setVisible(true);
            statusLabel.setText("AI 正在绘图...");

            new Thread(() -> {
                try {
                    String path = controller.getArkManager().generateImage(
                            controller.getImageManager().getCurrentImageFile().getAbsolutePath(),
                            prompt, dirField.getText(), nameField.getText());

                    Platform.runLater(() -> {
                        statusLabel.setText("生成成功！"); pBar.setVisible(false);
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("创作完成");
                        alert.setHeaderText("生成成功");
                        alert.setContentText("是否立即打开？\n" + path);
                        applyMainStyles(alert);
                        alert.showAndWait().ifPresent(r -> {
                            if(r == ButtonType.OK) {
                                controller.getImageManager().loadImage(new File(path));
                                dialog.close();
                            } else {
                                pArea.setDisable(false); genBtn.setDisable(false);
                                nameField.setText("ai_" + System.currentTimeMillis());
                            }
                        });
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("失败: " + ex.getMessage());
                        pBar.setVisible(false);
                        pArea.setDisable(false); genBtn.setDisable(false);
                    });
                }
            }).start();
        });

        dialog.showAndWait();
    }

    // ==================== 4. 其他对话框 (已修复关于页面) ====================

    public void showThemeSelector() {
        Dialog<ThemeManager.Theme> dialog = new Dialog<>();
        dialog.setTitle("选择主题");
        applyMainStyles(dialog);
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

    private VBox createThemePreview(ThemeManager.Theme theme) {
        VBox preview = new VBox(10);
        preview.setAlignment(Pos.CENTER);
        preview.setPadding(new Insets(15));
        preview.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 10;");
        preview.setOnMouseEntered(e -> preview.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 10; -fx-cursor: hand;"));
        preview.setOnMouseExited(e -> preview.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 10;"));
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

    public void showTextInputDialog(String title, String header, String defaultValue,
                                    Consumer<String> onAccept) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        applyMainStyles(dialog);
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(350);
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        TextArea textArea = new TextArea(defaultValue);
        textArea.setPromptText("请输入文字...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.setPrefHeight(80);
        content.getChildren().addAll(headerLabel, textArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(dialogButton -> dialogButton == ButtonType.OK ? textArea.getText() : null);
        Platform.runLater(textArea::requestFocus);
        dialog.showAndWait().ifPresent(result -> {
            if (result != null && !result.trim().isEmpty()) onAccept.accept(result);
        });
    }

    public void showArtisticStyleDialog(Consumer<String> callback) {
        List<String> styles = List.of("油画", "水彩", "素描", "卡通", "马赛克");
        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("油画", styles);
        choiceDialog.setTitle("艺术风格");
        choiceDialog.setHeaderText("选择风格");
        choiceDialog.setContentText("风格:");
        applyMainStyles(choiceDialog);
        choiceDialog.showAndWait().ifPresent(callback);
    }

    public void showHelp() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("关于");
        applyMainStyles(dialog);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setPrefWidth(400);

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
        closeBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(logoPane, title, ver, keys, closeBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
    }

    private HBox createKeyRow(String key, String desc) {
        HBox row = new HBox(10);
        Label k = new Label(key);
        k.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-text-fill: #667eea; -fx-background-color: rgba(102,126,234,0.1); -fx-padding: 2 6; -fx-background-radius: 4;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: #4b5563;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(d, sp, k);
        return row;
    }

    public void showCropDialog(int imageWidth, int imageHeight, Consumer<java.awt.Rectangle> onAccept) {
        Dialog<java.awt.Rectangle> dialog = new Dialog<>();
        dialog.setTitle("裁剪图片");
        applyMainStyles(dialog);
    }

    private void applyMainStyles(Dialog<?> dialog) {
        try {
            if (controller.getMainScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(controller.getMainScene().getStylesheets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}