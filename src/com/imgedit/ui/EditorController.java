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
    private Label statusLabel;

    // 【修改】只保留引用，不再自己创建
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
        configManager = new ConfigManager();
        showSplashScreen(() -> {
            Platform.runLater(() -> {
                initializeModules();
                initializeMainWindow();
                applyTheme(themeManager.getCurrentTheme());
                primaryStage.show();
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

        StackPane logoPane = new StackPane();
        Circle bg = new Circle(50);
        bg.getStyleClass().add("splash-logo-bg");
        Label logoIcon = new Label("✨");
        logoIcon.setStyle("-fx-font-size: 55px;");
        logoPane.getChildren().addAll(bg, logoIcon);

        Label titleLabel = new Label("Pro Image Editor");
        titleLabel.getStyleClass().add("splash-title");
        Label subtitleLabel = new Label("ULTIMATE EDITION");
        subtitleLabel.getStyleClass().add("splash-subtitle");

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
        root = uiManager.createRootLayout();
        rootContainer = uiManager.createRootContainer(root);
        mainScene = new Scene(rootContainer, 1600, 950);
        CSSLoader.loadCSS(mainScene);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Pro Image Editor - Ultimate Edition");
        primaryStage.setMaximized(true);
        setupShortcuts();

        // 【关键修改】删除了 createSelectionCanvas() 调用
        // 画布现在由 UIManager 创建并通过 setSelectionCanvas 传入
    }

    // 【关键新增】允许 UIManager 注入正确的画布
    public void setSelectionCanvas(Canvas canvas) {
        this.selectionCanvas = canvas;
        System.out.println("选择画布已注册: " + canvas);
    }

    // 新增：在图像加载/更新时重置画布
    public void resetSelectionCanvas() {
        if (selectionCanvas != null) {
            clearSelectionCanvas();
            // 重置画布位置和大小 (这些由 UIManager 的布局管理，通常不需要手动重置位置，只需清空内容)
            selectionCanvas.setWidth(0);
            selectionCanvas.setHeight(0);
        }
    }

    private void setupShortcuts() {
        Scene scene = primaryStage.getScene();
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                this::cycleTheme
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                () -> dialogManager.showThemeSelector()
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN),
                () -> imageManager.openImage()
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                () -> imageManager.saveImage()
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                () -> imageManager.undo()
        );
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
        uiManager.showToast(" ✅  " + message, "success");
    }

    public void showError(String title, String message) {
        if (message.length() < 30) {
            uiManager.showToast(" ❌  " + message, "error");
        } else {
            dialogManager.showError(title, message);
        }
    }

    public void showWarning(String title, String message) {
        dialogManager.showWarning(title, message);
    }

    public Canvas getSelectionCanvas() {
        return selectionCanvas;
    }

    public void clearSelectionCanvas() {
        if (selectionCanvas != null) {
            javafx.scene.canvas.GraphicsContext gc = selectionCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, selectionCanvas.getWidth(), selectionCanvas.getHeight());
            // 清除内容后，将尺寸重置，防止阻挡鼠标事件（虽然已设置MouseTransparent，但双重保险）
            selectionCanvas.setWidth(0);
            selectionCanvas.setHeight(0);
        }
    }

    public Parent getRootNode() {
        return primaryStage.getScene().getRoot();
    }
}