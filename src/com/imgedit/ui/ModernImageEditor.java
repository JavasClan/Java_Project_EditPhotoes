package imgedit.ui;

import imgedit.core.operations.*;
import imgedit.service.ImageEditorService;
import imgedit.utils.ImageUtils;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;

/**
 * 现代化图像编辑器 - 全新Material Design风格UI
 * 特点: 玻璃态效果、流畅动画、直观交互
 */
public class ModernImageEditor extends Application {

    // 服务层
    private ImageEditorService imageEditorService;

    // 数据层
    private BufferedImage currentBufferedImage;
    private Image currentImage;
    private File currentImageFile;

    // UI组件
    private Stage primaryStage;
    private Scene mainScene;
    private ImageView imageView;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private VBox leftPanel;
    private VBox rightPanel;
    private ScrollPane imageScrollPane;
    private ListView<String> historyListView;

    // 状态
    private double currentZoom = 1.0;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // 显示启动动画
        showSplashScreen(() -> {
            Platform.runLater(this::initializeMainWindow);
        });
    }

    /**
     * 启动画面
     */
    private void showSplashScreen(Runnable onComplete) {
        Stage splashStage = new Stage();

        VBox splashRoot = new VBox(30);
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setPrefSize(500, 350);
        splashRoot.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #667eea 0%, #764ba2 100%);");

        // Logo图标
        Circle logoCircle = new Circle(50);
        logoCircle.setFill(Color.WHITE);
        logoCircle.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.3)));

        Label logoIcon = new Label("🎨");
        logoIcon.setStyle("-fx-font-size: 60px;");

        StackPane logoPane = new StackPane(logoCircle, logoIcon);

        // 标题
        Label titleLabel = new Label("AI Image Editor Pro");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Professional Image Processing Suite");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.8);");

        // 加载进度条
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: white;");

        Label loadingLabel = new Label("Loading...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        splashRoot.getChildren().addAll(logoPane, titleLabel, subtitleLabel, progressBar, loadingLabel);

        Scene splashScene = new Scene(splashRoot);
        splashStage.setScene(splashScene);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        // 动画效果
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), splashRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // 模拟加载
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(2.5), e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashRoot);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(ev -> {
                        splashStage.close();
                        onComplete.run();
                    });
                    fadeOut.play();
                })
        );
        timeline.play();
    }

    /**
     * 初始化主窗口
     */
    private void initializeMainWindow() {
        // 初始化服务
        try {
            imageEditorService = new ImageEditorService();
        } catch (Exception e) {
            showError("初始化失败", "无法启动图像编辑服务: " + e.getMessage());
        }

        // 创建主布局
        BorderPane root = new BorderPane();

        // 设置背景色
        root.setStyle("-fx-background-color: #f5f7fa;");

        // 创建所有组件
        root.setTop(createTopBar());
        root.setLeft(createLeftPanel());
        root.setCenter(createCenterPanel());
        root.setRight(createRightPanel());
        root.setBottom(createBottomBar());

        // 创建场景
        mainScene = new Scene(root, 1600, 900);

        // 设置舞台
        primaryStage.setTitle("AI Image Editor Pro");
        primaryStage.setScene(mainScene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        // 入场动画
        playEntryAnimation(root);
    }

    // 修改 createTopBar() 方法，移除CSS类名，只用内联样式
    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // Logo和标题
        Label logo = new Label("🎨");
        logo.setStyle("-fx-font-size: 28px;");

        Label title = new Label("AI Image Editor");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // 文件操作按钮
        Button openBtn = new Button("📁 打开");
        openBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                "-fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 20; -fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        openBtn.setOnAction(e -> openImage());

        Button saveBtn = new Button("💾 保存");
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c); " +
                "-fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 20; -fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        saveBtn.setOnAction(e -> saveImage());

        // 编辑操作按钮
        Button undoBtn = new Button("↶");
        undoBtn.setTooltip(new Tooltip("撤销"));
        undoBtn.setStyle("-fx-background-color: #ecf0f1; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand;");
        undoBtn.setOnAction(e -> undo());

        Button redoBtn = new Button("↷");
        redoBtn.setTooltip(new Tooltip("重做"));
        redoBtn.setStyle("-fx-background-color: #ecf0f1; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand;");
        redoBtn.setOnAction(e -> redo());

        // 帮助按钮
        Button helpBtn = new Button("❓");
        helpBtn.setTooltip(new Tooltip("帮助"));
        helpBtn.setStyle("-fx-background-color: #ecf0f1; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand;");
        helpBtn.setOnAction(e -> showHelp());

        topBar.getChildren().addAll(logo, title, spacer1, openBtn, saveBtn,
                new Separator(), undoBtn, redoBtn, helpBtn);

        return topBar;
    }

    private void testImageDisplay() {
        System.out.println("=== 测试图片显示 ===");
        System.out.println("currentImage: " + (currentImage != null ? "已加载" : "null"));
        System.out.println("imageView图片: " + (imageView.getImage() != null ? "已设置" : "null"));
        System.out.println("imageView可见: " + imageView.isVisible());
        System.out.println("imageScrollPane可见: " + imageScrollPane.isVisible());

        // 测试强制显示
        if (currentImage != null) {
            Platform.runLater(() -> {
                // 强制重新设置图片
                imageView.setImage(currentImage);
                imageView.setVisible(true);
                imageScrollPane.setVisible(true);

                // 确保占位符隐藏
                StackPane centerPane = (StackPane) imageScrollPane.getParent();
                Node placeholder = centerPane.lookup("#placeholder");
                if (placeholder != null) {
                    placeholder.setVisible(false);
                }

                updateStatus("测试: 强制显示图片");
            });
        } else {
            showWarning("测试", "没有图片可显示");
        }
    }

    /**
     * 创建左侧工具面板
     */
    private ScrollPane createLeftPanel() {
        leftPanel = new VBox(20);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setPrefWidth(280);
        leftPanel.setStyle("-fx-background-color: white;");

        // 基础调整
        Label basicLabel = createSectionLabel("🎛 基础调整");

        VBox brightnessControl = createSliderControl("亮度", -100, 100, 0, value -> {
            adjustBrightness(value);
        });

        VBox contrastControl = createSliderControl("对比度", -100, 100, 0, value -> {
            adjustContrast(value);
        });

        VBox saturationControl = createSliderControl("饱和度", -100, 100, 0, value -> {
            adjustSaturation(value);
        });

        Separator sep1 = new Separator();

        // 变换操作
        Label transformLabel = createSectionLabel("🔄 变换");

        FlowPane transformButtons = new FlowPane(10, 10);
        transformButtons.setAlignment(Pos.CENTER_LEFT);

        Button rotate90 = createOperationButton("⟳ 90°");
        rotate90.setOnAction(e -> rotate90());

        Button rotate180 = createOperationButton("⟳ 180°");
        rotate180.setOnAction(e -> rotate180());

        Button flipH = createOperationButton("⇄ 水平");
        flipH.setOnAction(e -> flipHorizontal());

        Button flipV = createOperationButton("⇅ 垂直");
        flipV.setOnAction(e -> flipVertical());

        transformButtons.getChildren().addAll(rotate90, rotate180, flipH, flipV);

        Separator sep2 = new Separator();

        // 滤镜效果
        Label filterLabel = createSectionLabel("✨ 滤镜");

        VBox blurControl = createSliderControl("模糊", 0, 10, 0, value -> {
            applyBlur(value);
        });

        Button grayscaleBtn = createOperationButton("⚫ 灰度");
        grayscaleBtn.setPrefWidth(Double.MAX_VALUE);
        grayscaleBtn.setOnAction(e -> applyGrayscale());

        Button edgeDetectBtn = createOperationButton("🔲 边缘检测");
        edgeDetectBtn.setPrefWidth(Double.MAX_VALUE);
        edgeDetectBtn.setOnAction(e -> detectEdges());

        Separator sep3 = new Separator();

        // AI功能
        Label aiLabel = createSectionLabel("🤖 AI增强");

        Button aiEnhanceBtn = createGradientButton("✨ AI增强", "#4facfe", "#00f2fe");
        aiEnhanceBtn.setPrefWidth(Double.MAX_VALUE);
        aiEnhanceBtn.setOnAction(e -> aiEnhance());

        Button removeBackground = createGradientButton("🖼 移除背景", "#fa709a", "#fee140");
        removeBackground.setPrefWidth(Double.MAX_VALUE);
        removeBackground.setOnAction(e -> removeBackground());

        Button artisticStyle = createGradientButton("🎨 艺术风格", "#a8edea", "#fed6e3");
        artisticStyle.setPrefWidth(Double.MAX_VALUE);
        artisticStyle.setOnAction(e -> applyArtisticStyle());

        leftPanel.getChildren().addAll(
                basicLabel, brightnessControl, contrastControl, saturationControl,
                sep1, transformLabel, transformButtons,
                sep2, filterLabel, blurControl, grayscaleBtn, edgeDetectBtn,
                sep3, aiLabel, aiEnhanceBtn, removeBackground, artisticStyle
        );

        ScrollPane scrollPane = new ScrollPane(leftPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 0 0;");

        return scrollPane;
    }

    /**
     * 创建中心图像显示区域
     */
    private StackPane createCenterPanel() {
        StackPane centerPane = new StackPane();
        centerPane.setStyle("-fx-background-color: #f5f7fa;");

        // 图像容器
        VBox imageContainer = new VBox(20);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(30));

        // 图像视图
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);

        // 图像背景（棋盘格）
        StackPane imagePane = new StackPane();
        imagePane.setStyle("-fx-background-color: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5); " +
                "-fx-background-radius: 10;");
        imagePane.setPadding(new Insets(20));
        imagePane.getChildren().add(imageView);

        // 图像控制按钮
        HBox controlButtons = new HBox(15);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setId("control-buttons"); // 添加ID以便查找

        Button zoomIn = createIconButton("➕", "放大");
        zoomIn.setOnAction(e -> zoomIn());

        Button zoomOut = createIconButton("➖", "缩小");
        zoomOut.setOnAction(e -> zoomOut());

        Button zoomFit = createIconButton("⛶", "适应");
        zoomFit.setOnAction(e -> fitToWindow());

        Button zoom100 = createIconButton("1:1", "原始");
        zoom100.setOnAction(e -> resetZoom());

        controlButtons.getChildren().addAll(zoomIn, zoomOut, zoomFit, zoom100);

        imageContainer.getChildren().addAll(imagePane, controlButtons);

        // 滚动面板
        imageScrollPane = new ScrollPane(imageContainer);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background-color: transparent;");
        imageScrollPane.setId("image-scroll-pane"); // 添加ID

        // 占位符 - 直接创建为VBox
        VBox placeholder = new VBox(20);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setId("placeholder");

        Label placeholderIcon = new Label("📷");
        placeholderIcon.setStyle("-fx-font-size: 80px; -fx-opacity: 0.3;");

        Label placeholderText = new Label("点击打开按钮选择图片");
        placeholderText.setStyle("-fx-font-size: 18px; -fx-text-fill: #95a5a6;");

        Button quickOpenBtn = createGradientButton("📁 打开图片", "#667eea", "#764ba2");
        quickOpenBtn.setOnAction(e -> openImage());

        placeholder.getChildren().addAll(placeholderIcon, placeholderText, quickOpenBtn);

        // 初始状态：显示占位符，隐藏图像区域
        imageScrollPane.setVisible(false);
        controlButtons.setVisible(false);
        placeholder.setVisible(true);

        centerPane.getChildren().addAll(imageScrollPane, placeholder);

        return centerPane;
    }

    /**
     * 创建右侧面板
     */
    private ScrollPane createRightPanel() {
        rightPanel = new VBox(20);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setPrefWidth(280);
        rightPanel.setStyle("-fx-background-color: white;");

        // 历史记录
        Label historyLabel = createSectionLabel("📜 操作历史");

        historyListView = new ListView<>();
        historyListView.setPrefHeight(300);
        historyListView.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");

        Separator sep1 = new Separator();

        // 图像信息
        Label infoLabel = createSectionLabel("ℹ️ 图像信息");

        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");

        Label sizeLabel = new Label("尺寸: --");
        sizeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");

        Label formatLabel = new Label("格式: --");
        formatLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");

        Label fileSizeLabel = new Label("大小: --");
        fileSizeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");

        infoBox.getChildren().addAll(sizeLabel, formatLabel, fileSizeLabel);

        Separator sep2 = new Separator();

        // 快捷操作
        Label quickLabel = createSectionLabel("⚡ 快捷操作");

        Button resetBtn = createOperationButton("🔄 重置图片");
        resetBtn.setPrefWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> resetImage());

        Button clearBtn = createOperationButton("🗑️ 清空画布");
        clearBtn.setPrefWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearCanvas());

        rightPanel.getChildren().addAll(
                historyLabel, historyListView,
                sep1, infoLabel, infoBox,
                sep2, quickLabel, resetBtn, clearBtn
        );

        ScrollPane scrollPane = new ScrollPane(rightPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 0 1;");

        return scrollPane;
    }

    /**
     * 创建底部状态栏
     */
    private HBox createBottomBar() {
        HBox bottomBar = new HBox(20);
        bottomBar.setPadding(new Insets(10, 20, 10, 20));
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);

        Label versionLabel = new Label("v2.0 Pro");
        versionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #adb5bd;");

        bottomBar.getChildren().addAll(statusLabel, spacer, progressIndicator, versionLabel);

        return bottomBar;
    }

    // ==================== UI辅助方法 ====================

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return label;
    }

    private Button createGradientButton(String text, String color1, String color2) {
        Button btn = new Button(text);
        btn.setStyle(String.format(
                "-fx-background-color: linear-gradient(to right, %s, %s); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 20; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);",
                color1, color2
        ));

        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.05);
            btn.setScaleY(1.05);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });

        return btn;
    }

    private Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: #ecf0f1; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #d5dbdb; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 8 12; " +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #ecf0f1; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 8 12; " +
                        "-fx-cursor: hand;"
        ));

        return btn;
    }

    private Button createOperationButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #dee2e6; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #e9ecef; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 8 12; " +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 8 12; " +
                        "-fx-cursor: hand;"
        ));

        return btn;
    }

    private VBox createSliderControl(String label, double min, double max, double value,
                                     SliderChangeListener listener) {
        VBox box = new VBox(8);

        HBox labelBox = new HBox(10);
        labelBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(String.format("%.0f", value));
        valueLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; " +
                "-fx-background-color: #e9ecef; " +
                "-fx-background-radius: 4; " +
                "-fx-padding: 2 8;");

        labelBox.getChildren().addAll(nameLabel, spacer, valueLabel);

        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setStyle("-fx-control-inner-background: #e9ecef;");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format("%.0f", newVal.doubleValue()));
            listener.onChange(newVal.doubleValue());
        });

        box.getChildren().addAll(labelBox, slider);

        return box;
    }

    // ==================== 图像操作方法 ====================

    private void openImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            loadImage(file);
        }
    }

    private void loadImage(File file) {
        showProgress("正在加载图片...");

        new Thread(() -> {
            try {
                Image image = new Image(file.toURI().toString());
                currentImageFile = file;
                currentImage = image;
                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);

                Platform.runLater(() -> {
                    // 设置图片
                    imageView.setImage(currentImage);

                    // 隐藏占位符，显示图像区域
                    StackPane centerPane = (StackPane) imageScrollPane.getParent();

                    // 查找占位符
                    Node placeholder = centerPane.lookup("#placeholder");
                    if (placeholder != null) {
                        placeholder.setVisible(false);
                        System.out.println("隐藏占位符");
                    }

                    // 显示图像区域
                    imageScrollPane.setVisible(true);

                    // 显示控制按钮
                    VBox imageContainer = (VBox) imageScrollPane.getContent();
                    if (imageContainer != null) {
                        Node controlButtons = imageContainer.lookup("#control-buttons");
                        if (controlButtons != null) {
                            controlButtons.setVisible(true);
                            System.out.println("显示控制按钮");
                        }
                    }

                    // 调整图片显示大小
                    if (currentImage.getWidth() > 0 && currentImage.getHeight() > 0) {
                        double aspectRatio = currentImage.getWidth() / currentImage.getHeight();
                        if (aspectRatio > 1) {
                            // 宽大于高
                            imageView.setFitWidth(800);
                            imageView.setFitHeight(800 / aspectRatio);
                        } else {
                            // 高大于宽
                            imageView.setFitHeight(600);
                            imageView.setFitWidth(600 * aspectRatio);
                        }
                    }

                    // 初始化服务
                    if (imageEditorService != null) {
                        imageEditorService.initImageProcessor(currentImage);
                    }

                    updateHistory("打开图片: " + file.getName());
                    updateStatus("图片已加载: " + file.getName());
                    hideProgress();

                    // 播放加载动画
                    playImageLoadAnimation();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    showError("加载失败", "无法加载图片: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void saveImage() {
        if (currentImage == null) {
            showWarning("提示", "没有可保存的图片");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            showProgress("正在保存图片...");

            new Thread(() -> {
                try {
                    BufferedImage bufferedImage = imageEditorService.getImageProcessor().getCurrentImage();
                    String format = getFileExtension(file.getName()).toUpperCase();
                    if (format.equals("JPG")) format = "JPEG";

                    javax.imageio.ImageIO.write(bufferedImage, format, file);

                    Platform.runLater(() -> {
                        hideProgress();
                        updateStatus("图片已保存: " + file.getName());
                        showSuccess("保存成功", "图片已保存到: " + file.getAbsolutePath());
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        hideProgress();
                        showError("保存失败", "无法保存图片: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void adjustBrightness(double value) {
        if (currentImage == null || imageEditorService == null) return;

        BrightnessOperation.BrightnessMode mode = value >= 0 ?
                BrightnessOperation.BrightnessMode.INCREASE :
                BrightnessOperation.BrightnessMode.DECREASE;
        float intensity = (float)(Math.abs(value) / 100.0);

        BrightnessOperation operation = new BrightnessOperation(mode, intensity);
        applyOperation(operation, "调整亮度");
    }

    private void adjustContrast(double value) {
        if (currentImage == null || imageEditorService == null) return;

        float contrastLevel = (float)(value / 100.0f + 1.0f);
        ContrastOperation operation = new ContrastOperation(contrastLevel);
        applyOperation(operation, "调整对比度");
    }

    private void adjustSaturation(double value) {
        // 饱和度调整暂时使用对比度模拟
        adjustContrast(value * 0.5);
    }

    private void applyBlur(double value) {
        if (currentImage == null || imageEditorService == null || value == 0) return;

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

    private void rotate90() {
        if (currentImage == null || imageEditorService == null) return;
        RotateOperation operation = RotateOperation.create90Degree();
        applyOperation(operation, "旋转90度");
    }

    private void rotate180() {
        if (currentImage == null || imageEditorService == null) return;
        RotateOperation operation = RotateOperation.create180Degree();
        applyOperation(operation, "旋转180度");
    }

    private void flipHorizontal() {
        if (currentImage == null || imageEditorService == null) return;
        FlipOperation operation = FlipOperation.createHorizontalFlip();
        applyOperation(operation, "水平翻转");
    }

    private void flipVertical() {
        if (currentImage == null || imageEditorService == null) return;
        FlipOperation operation = FlipOperation.createVerticalFlip();
        applyOperation(operation, "垂直翻转");
    }

    private void applyGrayscale() {
        if (currentImage == null || imageEditorService == null) return;
        GrayscaleOperation operation = GrayscaleOperation.create();
        applyOperation(operation, "灰度化");
    }

    private void detectEdges() {
        if (currentImage == null || imageEditorService == null) return;
        EdgeDetectionOperation operation = EdgeDetectionOperation.createAllEdges();
        applyOperation(operation, "边缘检测");
    }

    private void aiEnhance() {
        if (currentImage == null || imageEditorService == null) return;
        showProgress("AI增强处理中...");

        new Thread(() -> {
            try {
                AIColorEnhancementOperation operation = AIColorEnhancementOperation.createAutoEnhancement();
                imageEditorService.applyOperationAsync(
                        operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            updateHistory("AI增强");
                            updateStatus("AI增强完成");
                            hideProgress();
                            playSuccessAnimation();
                        }),
                        exception -> Platform.runLater(() -> {
                            hideProgress();
                            showError("AI增强失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    showError("AI增强失败", e.getMessage());
                });
            }
        }).start();
    }

    private void removeBackground() {
        if (currentImage == null || imageEditorService == null) return;
        showProgress("背景移除中...");

        new Thread(() -> {
            try {
                BackgroundRemovalOperation operation = BackgroundRemovalOperation.createAutoBackgroundRemoval();
                imageEditorService.applyOperationAsync(
                        operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            updateHistory("移除背景");
                            updateStatus("背景移除完成");
                            hideProgress();
                            playSuccessAnimation();
                        }),
                        exception -> Platform.runLater(() -> {
                            hideProgress();
                            showError("背景移除失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    showError("背景移除失败", e.getMessage());
                });
            }
        }).start();
    }

    private void applyArtisticStyle() {
        if (currentImage == null || imageEditorService == null) return;

        // 创建选择对话框
        List<String> styles = new ArrayList<>();
        styles.add("油画");
        styles.add("水彩");
        styles.add("素描");
        styles.add("卡通");
        styles.add("马赛克");
        ChoiceDialog<String> dialog = new ChoiceDialog<>(styles.get(0), styles);
        dialog.setTitle("选择艺术风格");
        dialog.setHeaderText("选择要应用的艺术风格");
        dialog.setContentText("风格:");

        dialog.showAndWait().ifPresent(style -> {
            showProgress("应用艺术风格中...");

            new Thread(() -> {
                try {
                    ArtisticStyleOperation.ArtisticStyle selectedStyle;
                    switch (style) {
                        case "油画": selectedStyle = ArtisticStyleOperation.ArtisticStyle.OIL_PAINTING; break;
                        case "水彩": selectedStyle = ArtisticStyleOperation.ArtisticStyle.WATERCOLOR; break;
                        case "素描": selectedStyle = ArtisticStyleOperation.ArtisticStyle.PENCIL_SKETCH; break;
                        case "卡通": selectedStyle = ArtisticStyleOperation.ArtisticStyle.CARTOON; break;
                        default: selectedStyle = ArtisticStyleOperation.ArtisticStyle.MOSAIC; break;
                    }

                    ArtisticStyleOperation.StyleParameters params =
                            new ArtisticStyleOperation.StyleParameters(0.7f, 5, 0.5f);
                    ArtisticStyleOperation operation = new ArtisticStyleOperation(selectedStyle, params);

                    imageEditorService.applyOperationAsync(
                            operation,
                            resultImage -> Platform.runLater(() -> {
                                currentImage = resultImage;
                                imageView.setImage(currentImage);
                                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                                updateHistory("艺术风格: " + style);
                                updateStatus("艺术风格应用完成");
                                hideProgress();
                                playSuccessAnimation();
                            }),
                            exception -> Platform.runLater(() -> {
                                hideProgress();
                                showError("艺术风格应用失败", exception.getMessage());
                            })
                    );
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        hideProgress();
                        showError("艺术风格应用失败", e.getMessage());
                    });
                }
            }).start();
        });
    }

    private void applyOperation(Object operation, String operationName) {
        showProgress("处理中...");

        new Thread(() -> {
            try {
                imageEditorService.applyOperationAsync(
                        (imgedit.core.ImageOperation) operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            updateHistory(operationName);
                            updateStatus(operationName + "完成");
                            hideProgress();
                        }),
                        exception -> Platform.runLater(() -> {
                            hideProgress();
                            showError("操作失败", exception.getMessage());
                        })
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    showError("操作失败", e.getMessage());
                });
            }
        }).start();
    }

    private void undo() {
        if (imageEditorService != null && imageEditorService.canUndo()) {
            try {
                Image result = imageEditorService.undo();
                if (result != null) {
                    currentImage = result;
                    imageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("撤销完成");
                    updateHistory("撤销操作");
                }
            } catch (Exception e) {
                showError("撤销失败", e.getMessage());
            }
        } else {
            updateStatus("无法撤销");
        }
    }

    private void redo() {
        if (imageEditorService != null && imageEditorService.canRedo()) {
            try {
                Image result = imageEditorService.redo();
                if (result != null) {
                    currentImage = result;
                    imageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("重做完成");
                    updateHistory("重做操作");
                }
            } catch (Exception e) {
                showError("重做失败", e.getMessage());
            }
        } else {
            updateStatus("无法重做");
        }
    }

    private void resetImage() {
        if (currentImageFile != null) {
            loadImage(currentImageFile);
        }
    }

    private void clearCanvas() {
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

        historyListView.getItems().clear();
        updateStatus("画布已清空");
    }

    private void zoomIn() {
        currentZoom *= 1.2;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
    }

    private void zoomOut() {
        currentZoom *= 0.8;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
    }

    private void fitToWindow() {
        currentZoom = 1.0;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);
    }

    private void resetZoom() {
        currentZoom = 1.0;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
        if (currentImage != null) {
            imageView.setFitWidth(currentImage.getWidth());
            imageView.setFitHeight(currentImage.getHeight());
        }
    }

    // ==================== UI更新方法 ====================

    private void updateHistory(String operation) {
        historyListView.getItems().add(0, operation);
        if (historyListView.getItems().size() > 20) {
            historyListView.getItems().remove(20);
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showProgress(String message) {
        statusLabel.setText(message);
        progressIndicator.setVisible(true);
    }

    private void hideProgress() {
        progressIndicator.setVisible(false);
    }

    // ==================== 对话框方法 ====================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("帮助");
        alert.setHeaderText("AI Image Editor Pro - 使用指南");
        alert.setContentText(
                "1. 点击打开按钮加载图片\n" +
                        "2. 使用左侧面板调整图片参数\n" +
                        "3. 点击各种效果按钮应用处理\n" +
                        "4. 使用撤销/重做按钮管理历史\n" +
                        "5. 完成后点击保存导出图片\n\n" +
                        "快捷键:\n" +
                        "Ctrl+O - 打开图片\n" +
                        "Ctrl+S - 保存图片\n" +
                        "Ctrl+Z - 撤销\n" +
                        "Ctrl+Y - 重做"
        );
        alert.showAndWait();
    }

    // ==================== 动画效果 ====================

    private void playEntryAnimation(BorderPane root) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void playImageLoadAnimation() {
        // 使用你的 FXAnimations 类
        FXAnimations.fadeIn(imageView, Duration.millis(400));

        // 或者使用组合动画
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), imageView);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    private void playSuccessAnimation() {
        // 使用你的 FXAnimations 类
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), imageView);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    // ==================== 工具方法 ====================

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "png";
    }

    // ==================== 内部接口 ====================

    @FunctionalInterface
    interface SliderChangeListener {
        void onChange(double value);
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        launch(args);
    }
}