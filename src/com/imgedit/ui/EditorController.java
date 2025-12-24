package imgedit.ui;

import imgedit.core.operations.CropOperation;
import imgedit.service.ImageEditorService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.util.function.Consumer;

/**
 * 编辑器控制器 - 负责协调各模块
 */
public class EditorController {

    private final Stage primaryStage;
    private Scene mainScene;
    private BorderPane root;
    private StackPane rootContainer;

    // 模块实例
    private ThemeManager themeManager;
    private ImageManager imageManager;
    private UIManager uiManager;
    private ToolManager toolManager;
    private ArkManager arkManager;
    private DialogManager dialogManager;
    private AnimationManager animationManager;
    private ConfigManager configManager;

    // 服务层
    private ImageEditorService imageEditorService;

    // UI状态
    private VBox toastContainer;
    private StackPane loadingOverlay;
    private Label loadingText;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;

    // 新增：直接存储选择画布的引用
    private Canvas selectionCanvas;

    public EditorController(Stage stage) {
        this.primaryStage = stage;
    }
    public void refreshImageInfo() {
        if (uiManager != null) {
            uiManager.refreshImageInfo();
        }
    }

    public void start() {
        // 1. 加载配置
        configManager = new ConfigManager();

        // 2. 显示启动画面
        showSplashScreen(() -> {
            Platform.runLater(() -> {
                // 3. 初始化所有模块
                initializeModules();

                // 4. 初始化主窗口
                initializeMainWindow();

                // 5. 应用默认主题
                applyTheme(themeManager.getCurrentTheme());

                // 6. 显示主窗口
                primaryStage.show();

                // 7. 播放入场动画
                animationManager.playEntryAnimation(root);
            });
        });
    }

    private void showSplashScreen(Runnable onComplete) {
        Stage splashStage = new Stage();

        VBox splashRoot = new VBox(20);
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setPrefSize(550, 380);
        splashRoot.getStyleClass().add("splash-root");

        // Logo区域
        StackPane logoPane = new StackPane();
        Circle bg = new Circle(50);
        bg.getStyleClass().add("splash-logo-bg");
        Label logoIcon = new Label("✨");
        logoIcon.setStyle("-fx-font-size: 55px;");
        logoPane.getChildren().addAll(bg, logoIcon);

        // 标题文字
        Label titleLabel = new Label("Pro Image Editor");
        titleLabel.getStyleClass().add("splash-title");

        Label subtitleLabel = new Label("ULTIMATE EDITION");
        subtitleLabel.getStyleClass().add("splash-subtitle");

        // 进度条
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20, 50, 0, 50));

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("splash-progress-bar");

        Label loadingLabel = new Label("Initializing Core Modules...");
        loadingLabel.getStyleClass().add("splash-loading-text");

        progressBox.getChildren().addAll(progressBar, loadingLabel);
        splashRoot.getChildren().addAll(logoPane, titleLabel, subtitleLabel, progressBox);

        Scene splashScene = new Scene(splashRoot);

        // 加载启动页CSS
        try {
            CSSLoader.loadCSS(splashScene);
        } catch (Exception e) {
            System.err.println("启动页CSS加载失败: " + e.getMessage());
        }

        splashStage.setScene(splashScene);
        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        // 模拟加载动画
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i++) {
                    double progress = i / 100.0;
                    final int step = i;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                        if (step > 30) loadingLabel.setText("Loading UI Components...");
                        if (step > 70) loadingLabel.setText("Starting Application...");
                    });
                    Thread.sleep(20);
                }

                // 淡出动画
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashRoot);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> {
                        splashStage.close();
                        onComplete.run();
                    });
                    fadeOut.play();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initializeModules() {
        // 初始化各模块
        themeManager = new ThemeManager();
        imageManager = new ImageManager(this);
        uiManager = new UIManager(this);
        toolManager = new ToolManager(this);
        arkManager = new ArkManager();
        dialogManager = new DialogManager(this);
        animationManager = new AnimationManager();

        try {
            imageEditorService = new ImageEditorService();
        } catch (Exception e) {
            dialogManager.showError("初始化失败", "无法启动图像编辑服务: " + e.getMessage());
        }
    }

    private void initializeMainWindow() {
        // 创建根布局
        root = uiManager.createRootLayout();

        // 创建根容器（包含Toast层）
        rootContainer = uiManager.createRootContainer(root);

        // 创建场景
        mainScene = new Scene(rootContainer, 1600, 950);

        // 加载CSS
        CSSLoader.loadCSS(mainScene);

        // 设置场景
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Pro Image Editor - Ultimate Edition");
        primaryStage.setMaximized(true);

        // 设置快捷键
        setupShortcuts();

        // 创建选择画布并添加到场景
        createSelectionCanvas();
    }

    // 在 EditorController.java 的 createSelectionCanvas 方法中添加
    private void createSelectionCanvas() {
        // 创建透明的选择画布
        selectionCanvas = new Canvas();
        selectionCanvas.setId("selectionCanvas");

        // 关键设置：让画布可以接收鼠标事件
        selectionCanvas.setMouseTransparent(false);

        // 关键设置：设置透明背景
        selectionCanvas.setStyle("-fx-background-color: transparent;");

        // 关键设置：设置填充颜色为透明
        selectionCanvas.getGraphicsContext2D().setFill(Color.TRANSPARENT);
        selectionCanvas.getGraphicsContext2D().fillRect(0, 0, selectionCanvas.getWidth(), selectionCanvas.getHeight());

        // 设置初始大小
        selectionCanvas.setWidth(0);
        selectionCanvas.setHeight(0);

        // 确保画布在最上层
        selectionCanvas.setViewOrder(-1);

        // 设置鼠标事件
        setupSelectionCanvasEvents(selectionCanvas);

        // 将选择画布添加到根容器的顶部
        if (rootContainer != null) {
            rootContainer.getChildren().add(selectionCanvas);
            selectionCanvas.toFront();
        }

        System.out.println("选择画布已创建并设置事件，ID: " + selectionCanvas.getId());
    }

    // 新增：在图像加载/更新时重置画布
    public void resetSelectionCanvas() {
        if (selectionCanvas != null) {
            clearSelectionCanvas();

            // 重置画布位置和大小
            selectionCanvas.setTranslateX(0);
            selectionCanvas.setTranslateY(0);
            selectionCanvas.setWidth(0);
            selectionCanvas.setHeight(0);
        }
    }
    // 新增：设置选择画布事件
    private void setupSelectionCanvasEvents(Canvas canvas) {
        canvas.setOnMousePressed(event -> {
            toolManager.handleMousePressed(event.getX(), event.getY(), canvas);
            event.consume();
        });

        canvas.setOnMouseDragged(event -> {
            toolManager.handleMouseDragged(event.getX(), event.getY(), canvas);
            event.consume();
        });

        canvas.setOnMouseReleased(event -> {
            toolManager.handleMouseReleased(event.getX(), event.getY());
            event.consume();
        });
    }

    private void setupShortcuts() {
        Scene scene = primaryStage.getScene();

        // Ctrl+T 切换主题
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                this::cycleTheme
        );

        // Ctrl+Shift+T 打开主题选择器
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                () -> dialogManager.showThemeSelector()
        );

        // Ctrl+O 打开图片
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN),
                () -> imageManager.openImage()
        );

        // Ctrl+S 保存图片
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                () -> imageManager.saveImage()
        );

        // Ctrl+Z 撤销
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                () -> imageManager.undo()
        );

        // Ctrl+Y 重做
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN),
                () -> imageManager.redo()
        );
    }

    private void cycleTheme() {
        ThemeManager.Theme nextTheme = themeManager.getNextTheme();
        applyTheme(nextTheme);
    }

    public void applyTheme(ThemeManager.Theme theme) {
        themeManager.setCurrentTheme(theme);
        uiManager.applyTheme(theme);
    }

    // 假设在EditorController中处理鼠标选择
    public void handleCropSelection(double startX, double startY, double endX, double endY) {
        if (imageManager.getCurrentImage() == null) return;

        // 将屏幕坐标转换为图像坐标
        double[] startImageCoords = imageManager.screenToImageCoordinates(startX, startY);
        double[] endImageCoords = imageManager.screenToImageCoordinates(endX, endY);

        // 计算矩形区域（确保正确的宽高）
        int x = (int) Math.min(startImageCoords[0], endImageCoords[0]);
        int y = (int) Math.min(startImageCoords[1], endImageCoords[1]);
        int width = (int) Math.abs(endImageCoords[0] - startImageCoords[0]);
        int height = (int) Math.abs(endImageCoords[1] - startImageCoords[1]);

        // 确保区域有效
        if (width <= 0 || height <= 0) {
            showWarning("无效区域", "请选择有效的裁剪区域");
            return;
        }

        // 创建并应用裁剪操作
        CropOperation cropOp = new CropOperation(x, y, width, height);
        imageManager.applyOperation(cropOp, "裁剪");
    }

    // Getters for other modules
    public Stage getPrimaryStage() { return primaryStage; }
    public Scene getMainScene() { return mainScene; }
    public BorderPane getRoot() { return root; }
    public ImageManager getImageManager() { return imageManager; }
    public ThemeManager getThemeManager() { return themeManager; }
    public UIManager getUIManager() { return uiManager; }
    public ToolManager getToolManager() { return toolManager; }
    public ArkManager getArkManager() { return arkManager; }
    public DialogManager getDialogManager() { return dialogManager; }
    public AnimationManager getAnimationManager() { return animationManager; }
    public ImageEditorService getImageEditorService() { return imageEditorService; }

    // UI状态管理方法
    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public void showProgress(String message) {
        uiManager.showProgress(message);
    }

    public void hideProgress() {
        uiManager.hideProgress();
    }

    public void showToast(String message, String type) {
        uiManager.showToast(message, type);
    }

    public void showSuccess(String title, String message) {
        uiManager.showToast("✅ " + message, "success");
    }

    public void showError(String title, String message) {
        if (message.length() < 30) {
            uiManager.showToast("❌ " + message, "error");
        } else {
            dialogManager.showError(title, message);
        }
    }

    public void showWarning(String title, String message) {
        dialogManager.showWarning(title, message);
    }

    // 修复：直接返回存储的画布引用
    public Canvas getSelectionCanvas() {
        return selectionCanvas;
    }

    // 新增：清理选择画布的方法
    public void clearSelectionCanvas() {

        if (selectionCanvas != null) {
            javafx.scene.canvas.GraphicsContext gc = selectionCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, selectionCanvas.getWidth(), selectionCanvas.getHeight());
            selectionCanvas.setWidth(0);
            selectionCanvas.setHeight(0);
        }
    }

    /**
     * 获取根节点
     */
    public Parent getRootNode() {
        // 返回主场景的根节点
        return primaryStage.getScene().getRoot();
    }
}