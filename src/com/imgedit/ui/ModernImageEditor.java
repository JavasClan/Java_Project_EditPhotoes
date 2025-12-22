package imgedit.ui;

import imgedit.core.ImageOperation;
import imgedit.core.operations.*;
import imgedit.service.ImageEditorService;
import imgedit.utils.ImageUtils;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import javax.imageio.ImageIO;

/**
 * 现代化图像编辑器 - 支持多种高级主题
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
    private BorderPane root;

    // 调整值缓存
    private double brightnessValue = 0.0;
    private double contrastValue = 0.0;
    private double saturationValue = 0.0;

    // 状态
    private double currentZoom = 1.0;


    // 添加交互状态
    private enum ToolMode {
        SELECT,       // 选择模式
        CROP,         // 裁剪模式
        DRAW_BRUSH,   // 画笔模式
        DRAW_TEXT,    // 文字模式
        DRAW_RECT,    // 矩形模式
        DRAW_CIRCLE   // 圆形模式
    }

    private ToolMode currentToolMode = ToolMode.SELECT;

    // 裁剪相关变量
    private Rectangle cropSelection = null;
    private boolean isSelectingCrop = false;
    private double cropStartX, cropStartY;

    // 绘图相关变量
    private List<DrawingOperation.DrawingPoint> currentBrushPoints = new ArrayList<>();
    private DrawingOperation.BrushStyle currentBrushStyle = new DrawingOperation.BrushStyle(
            java.awt.Color.BLACK, 3, 1.0f);

    // 颜色选择
    private ColorPicker colorPicker;

    // 画笔粗细
    private Spinner<Integer> brushSizeSpinner;

    // 主题管理
    private enum Theme {
        LIGHT_MODE("浅色模式"),
        DARK_MODE("深色模式"),
        BLUE_NIGHT("蓝色之夜"),
        GREEN_FOREST("绿色森林"),
        PURPLE_DREAM("紫色梦幻"),
        ORANGE_SUNSET("橙色日落"),
        PINK_BLOSSOM("粉色花语"),
        CYBERPUNK("赛博朋克");

        private final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private Theme currentTheme = Theme.LIGHT_MODE;
    private Map<Theme, String> themeStyles = new HashMap<>();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        initializeThemes();

        // 显示启动动画
        showSplashScreen(() -> {
            Platform.runLater(this::initializeMainWindow);
        });
    }

    /**
     * 初始化所有主题样式
     */
    private void initializeThemes() {
        // 浅色模式
        themeStyles.put(Theme.LIGHT_MODE,
                "-fx-background-color: #f5f7fa; " +
                        "-fx-text-fill: #2c3e50;"
        );

        // 深色模式
        themeStyles.put(Theme.DARK_MODE,
                "-fx-background-color: #121212; " +
                        "-fx-text-fill: #e0e0e0;"
        );

        // 蓝色之夜主题
        themeStyles.put(Theme.BLUE_NIGHT,
                "-fx-background-color: #0f172a; " +
                        "-fx-text-fill: #e2e8f0;"
        );

        // 绿色森林主题
        themeStyles.put(Theme.GREEN_FOREST,
                "-fx-background-color: #022c22; " +
                        "-fx-text-fill: #d1fae5;"
        );

        // 紫色梦幻主题
        themeStyles.put(Theme.PURPLE_DREAM,
                "-fx-background-color: #1e1b4b; " +
                        "-fx-text-fill: #e9d5ff;"
        );

        // 橙色日落主题
        themeStyles.put(Theme.ORANGE_SUNSET,
                "-fx-background-color: #431407; " +
                        "-fx-text-fill: #fed7aa;"
        );

        // 粉色花语主题
        themeStyles.put(Theme.PINK_BLOSSOM,
                "-fx-background-color: #500724; " +
                        "-fx-text-fill: #fbcfe8;"
        );

        // 赛博朋克主题
        themeStyles.put(Theme.CYBERPUNK,
                "-fx-background-color: #000000; " +
                        "-fx-text-fill: #00ff41;"
        );
    }

    /**
     * 应用当前主题
     */
    private void applyTheme(Theme theme) {
        currentTheme = theme;

        // 获取当前主题的样式
        String style = themeStyles.get(theme);

        // 应用主题到根布局
        root.setStyle(style);

        // 更新各个面板的样式
        updatePanelStyles(theme);

        updateStatus("已切换主题: " + theme.getDisplayName());

        // 播放主题切换动画
        playThemeSwitchAnimation();
    }

    /**
     * 更新所有面板的样式
     */
    private void updatePanelStyles(Theme theme) {
        String panelStyle = "";
        String buttonStyle = "";
        String sectionStyle = "";
        String infoBoxStyle = "";
        String listStyle = "";

        // 根据主题设置不同的样式
        switch (theme) {
            case LIGHT_MODE:
                panelStyle = "-fx-background-color: white;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #2c3e50;";
                infoBoxStyle = "-fx-background-color: #f8f9fa; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: white; -fx-background-radius: 8;";
                break;

            case DARK_MODE:
                panelStyle = "-fx-background-color: #1e1e1e;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #7b2cbf, #9d4edd); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #ffffff;";
                infoBoxStyle = "-fx-background-color: #2d2d2d; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #2d2d2d; -fx-background-radius: 8;";
                break;

            case BLUE_NIGHT:
                panelStyle = "-fx-background-color: #1e293b;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #0ea5e9, #3b82f6); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #38bdf8;";
                infoBoxStyle = "-fx-background-color: #0f172a; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #1e293b; -fx-background-radius: 8;";
                break;

            case GREEN_FOREST:
                panelStyle = "-fx-background-color: #064e3b;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #10b981, #059669); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #34d399;";
                infoBoxStyle = "-fx-background-color: #022c22; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #064e3b; -fx-background-radius: 8;";
                break;

            case PURPLE_DREAM:
                panelStyle = "-fx-background-color: #312e81;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #a78bfa;";
                infoBoxStyle = "-fx-background-color: #1e1b4b; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #312e81; -fx-background-radius: 8;";
                break;

            case ORANGE_SUNSET:
                panelStyle = "-fx-background-color: #7c2d12;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #f97316, #ea580c); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #fb923c;";
                infoBoxStyle = "-fx-background-color: #431407; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #7c2d12; -fx-background-radius: 8;";
                break;

            case PINK_BLOSSOM:
                panelStyle = "-fx-background-color: #831843;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #ec4899, #db2777); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #f472b6;";
                infoBoxStyle = "-fx-background-color: #500724; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #831843; -fx-background-radius: 8;";
                break;

            case CYBERPUNK:
                panelStyle = "-fx-background-color: #0f0f23;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #00ff41, #00cc33); " +
                        "-fx-text-fill: black;";
                sectionStyle = "-fx-text-fill: #00ff41;";
                infoBoxStyle = "-fx-background-color: #000000; -fx-background-radius: 8;";
                listStyle = "-fx-background-color: #0f0f23; -fx-background-radius: 8;";
                break;
        }

        // 应用样式到各个面板
        if (leftPanel != null) {
            leftPanel.setStyle(panelStyle);
            updatePanelComponents(leftPanel, theme);
        }
        if (rightPanel != null) {
            rightPanel.setStyle(panelStyle);
            updatePanelComponents(rightPanel, theme);
        }

        // 更新历史列表
        if (historyListView != null) {
            historyListView.setStyle(listStyle);
        }

        // 更新状态栏
        HBox bottomBar = (HBox) root.getBottom();
        if (bottomBar != null) {
            bottomBar.setStyle(panelStyle);
        }

        // 更新顶部工具栏
        HBox topBar = (HBox) root.getTop();
        if (topBar != null) {
            topBar.setStyle(panelStyle);
        }
    }

    /**
     * 更新面板内的组件样式
     */
    private void updatePanelComponents(VBox panel, Theme theme) {
        for (Node node : panel.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                String text = label.getText();
                if (text.contains("🎛") || text.contains("🔄") || text.contains("✨") ||
                        text.contains("🤖") || text.contains("📜") || text.contains("ℹ️") ||
                        text.contains("⚡") || text.contains("✏️") || text.contains("✂️")) {
                    // 这是section label
                    updateSectionLabelStyle(label, theme);
                }
            } else if (node instanceof Button) {
                updateButtonStyle((Button) node, theme);
            } else if (node instanceof Separator) {
                updateSeparatorStyle((Separator) node, theme);
            } else if (node instanceof VBox) {
                updatePanelComponents((VBox) node, theme);
            }
        }
    }

    /**
     * 更新分段标签样式
     */
    private void updateSectionLabelStyle(Label label, Theme theme) {
        String style;
        switch (theme) {
            case LIGHT_MODE: style = "-fx-text-fill: #2c3e50;"; break;
            case DARK_MODE: style = "-fx-text-fill: #ffffff;"; break;
            case BLUE_NIGHT: style = "-fx-text-fill: #38bdf8;"; break;
            case GREEN_FOREST: style = "-fx-text-fill: #34d399;"; break;
            case PURPLE_DREAM: style = "-fx-text-fill: #a78bfa;"; break;
            case ORANGE_SUNSET: style = "-fx-text-fill: #fb923c;"; break;
            case PINK_BLOSSOM: style = "-fx-text-fill: #f472b6;"; break;
            case CYBERPUNK: style = "-fx-text-fill: #00ff41;"; break;
            default: style = "-fx-text-fill: #2c3e50;";
        }
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " + style);
    }

    /**
     * 更新按钮样式
     */
    private void updateButtonStyle(Button button, Theme theme) {
        String style;
        switch (theme) {
            case LIGHT_MODE:
                style = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white;";
                break;
            case DARK_MODE:
                style = "-fx-background-color: linear-gradient(to right, #7b2cbf, #9d4edd); " +
                        "-fx-text-fill: white;";
                break;
            case BLUE_NIGHT:
                style = "-fx-background-color: linear-gradient(to right, #0ea5e9, #3b82f6); " +
                        "-fx-text-fill: white;";
                break;
            case GREEN_FOREST:
                style = "-fx-background-color: linear-gradient(to right, #10b981, #059669); " +
                        "-fx-text-fill: white;";
                break;
            case PURPLE_DREAM:
                style = "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed); " +
                        "-fx-text-fill: white;";
                break;
            case ORANGE_SUNSET:
                style = "-fx-background-color: linear-gradient(to right, #f97316, #ea580c); " +
                        "-fx-text-fill: white;";
                break;
            case PINK_BLOSSOM:
                style = "-fx-background-color: linear-gradient(to right, #ec4899, #db2777); " +
                        "-fx-text-fill: white;";
                break;
            case CYBERPUNK:
                style = "-fx-background-color: linear-gradient(to right, #00ff41, #00cc33); " +
                        "-fx-text-fill: black;";
                break;
            default:
                style = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white;";
        }
        button.setStyle(style + " -fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
    }

    /**
     * 更新分隔符样式
     */
    private void updateSeparatorStyle(Separator separator, Theme theme) {
        String style;
        switch (theme) {
            case LIGHT_MODE: style = "-fx-background-color: #dee2e6;"; break;
            case DARK_MODE: style = "-fx-background-color: #404040;"; break;
            case BLUE_NIGHT: style = "-fx-background-color: #475569;"; break;
            case GREEN_FOREST: style = "-fx-background-color: #047857;"; break;
            case PURPLE_DREAM: style = "-fx-background-color: #5b21b6;"; break;
            case ORANGE_SUNSET: style = "-fx-background-color: #9a3412;"; break;
            case PINK_BLOSSOM: style = "-fx-background-color: #9d174d;"; break;
            case CYBERPUNK: style = "-fx-background-color: #00ff41;"; break;
            default: style = "-fx-background-color: #dee2e6;";
        }
        separator.setStyle(style);
    }

    /**
     * 播放主题切换动画
     */
    private void playThemeSwitchAnimation() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), root);
        fadeIn.setFromValue(0.7);
        fadeIn.setToValue(1.0);

        SequentialTransition sequence = new SequentialTransition(fadeOut, fadeIn);
        sequence.play();
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
        root = new BorderPane();

        // 创建所有组件
        root.setTop(createTopBar());
        root.setLeft(createLeftPanel());
        root.setCenter(createCenterPanel());
        root.setRight(createRightPanel());
        root.setBottom(createBottomBar());

        // 创建场景
        mainScene = new Scene(root, 1600, 900);
        primaryStage.setScene(mainScene);

        // 应用默认主题
        applyTheme(Theme.LIGHT_MODE);

        // 设置舞台
        primaryStage.setTitle("AI Image Editor Pro");
        primaryStage.setMaximized(true);

        // 添加快捷键
        setupShortcuts(root);

        primaryStage.show();

        // 入场动画
        playEntryAnimation(root);
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts(BorderPane root) {
        // 主题切换快捷键
        Scene scene = primaryStage.getScene();

        // Ctrl+T 切换主题
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                this::cycleTheme
        );

        // Ctrl+Shift+T 打开主题选择器
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::showThemeSelector
        );
    }

    /**
     * 循环切换主题
     */
    private void cycleTheme() {
        Theme[] themes = Theme.values();
        int currentIndex = currentTheme.ordinal();
        int nextIndex = (currentIndex + 1) % themes.length;
        applyTheme(themes[nextIndex]);
    }

    /**
     * 显示主题选择器
     */
    private void showThemeSelector() {
        Dialog<Theme> dialog = new Dialog<>();
        dialog.setTitle("选择主题");
        dialog.setHeaderText("选择界面主题");

        // 创建主题选择器
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🎨 选择主题");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 创建主题网格
        GridPane themeGrid = new GridPane();
        themeGrid.setHgap(15);
        themeGrid.setVgap(15);
        themeGrid.setAlignment(Pos.CENTER);

        Theme[] themes = Theme.values();
        for (int i = 0; i < themes.length; i++) {
            Theme theme = themes[i];
            VBox themeItem = createThemePreview(theme);
            themeItem.setOnMouseClicked(e -> {
                applyTheme(theme);
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
    private VBox createThemePreview(Theme theme) {
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

        // 主题颜色示例
        HBox colorSample = new HBox(5);
        colorSample.setAlignment(Pos.CENTER);

        // 根据主题类型显示不同颜色
        Color[] colors = getThemeColors(theme);
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
     * 获取主题颜色
     */
    private Color[] getThemeColors(Theme theme) {
        switch (theme) {
            case LIGHT_MODE:
                return new Color[]{
                        Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f5f7fa")
                };
            case DARK_MODE:
                return new Color[]{
                        Color.web("#7b2cbf"), Color.web("#9d4edd"), Color.web("#121212")
                };
            case BLUE_NIGHT:
                return new Color[]{
                        Color.web("#0ea5e9"), Color.web("#3b82f6"), Color.web("#0f172a")
                };
            case GREEN_FOREST:
                return new Color[]{
                        Color.web("#10b981"), Color.web("#059669"), Color.web("#022c22")
                };
            case PURPLE_DREAM:
                return new Color[]{
                        Color.web("#8b5cf6"), Color.web("#7c3aed"), Color.web("#1e1b4b")
                };
            case ORANGE_SUNSET:
                return new Color[]{
                        Color.web("#f97316"), Color.web("#ea580c"), Color.web("#431407")
                };
            case PINK_BLOSSOM:
                return new Color[]{
                        Color.web("#ec4899"), Color.web("#db2777"), Color.web("#500724")
                };
            case CYBERPUNK:
                return new Color[]{
                        Color.web("#00ff41"), Color.web("#ff00ff"), Color.web("#000000")
                };
            default:
                return new Color[]{Color.GRAY, Color.DARKGRAY, Color.LIGHTGRAY};
        }
    }

    // 修改 createTopBar() 方法，添加主题选择器
    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        // 初始样式将在主题应用时设置

        // Logo和标题
        Label logo = new Label("🎨");
        logo.setStyle("-fx-font-size: 28px;");

        Label title = new Label("AI Image Editor");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // 文件操作按钮
        Button openBtn = new Button("📁 打开");
        openBtn.setOnAction(e -> openImage());

        Button saveBtn = new Button("💾 保存");
        saveBtn.setOnAction(e -> saveImage());

        // 主题选择器
        MenuButton themeMenu = new MenuButton("🎨 主题");
        themeMenu.setStyle("-fx-background-color: rgba(0,0,0,0.05); " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand;");

        for (Theme theme : Theme.values()) {
            MenuItem item = new MenuItem(theme.getDisplayName());
            item.setOnAction(e -> applyTheme(theme));
            themeMenu.getItems().add(item);
        }

        // 编辑操作按钮
        Button undoBtn = createIconButton("↶", "撤销");
        undoBtn.setOnAction(e -> undo());

        Button redoBtn = createIconButton("↷", "重做");
        redoBtn.setOnAction(e -> redo());

        // 帮助按钮
        Button helpBtn = createIconButton("❓", "帮助");
        helpBtn.setOnAction(e -> showHelp());

        topBar.getChildren().addAll(logo, title, spacer1, openBtn, saveBtn, themeMenu,
                new Separator(), undoBtn, redoBtn, helpBtn);

        return topBar;
    }

    /**
     * 创建左侧工具面板 - 增强交互功能
     */
    private ScrollPane createLeftPanel() {
        leftPanel = new VBox(20);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setPrefWidth(280);

        // 基础调整
        Label basicLabel = createSectionLabel("🎛 基础调整");
        VBox adjustmentPanel = createAdvancedAdjustmentPanel();

        Separator sep1 = new Separator();

        // 交互工具选择
        Label toolsLabel = createSectionLabel("🛠️ 交互工具");

        // 工具选择按钮组
        ToggleGroup toolGroup = new ToggleGroup();

        ToggleButton selectTool = new ToggleButton("👆 选择");
        selectTool.setToggleGroup(toolGroup);
        selectTool.setSelected(true);
        selectTool.setOnAction(e -> setToolMode(ToolMode.SELECT));

        ToggleButton cropTool = new ToggleButton("✂️ 裁剪");
        cropTool.setToggleGroup(toolGroup);
        cropTool.setOnAction(e -> setToolMode(ToolMode.CROP));

        ToggleButton brushTool = new ToggleButton("🖌️ 画笔");
        brushTool.setToggleGroup(toolGroup);
        brushTool.setOnAction(e -> setToolMode(ToolMode.DRAW_BRUSH));

        ToggleButton textTool = new ToggleButton("A 文字");
        textTool.setToggleGroup(toolGroup);
        textTool.setOnAction(e -> setToolMode(ToolMode.DRAW_TEXT));

        ToggleButton rectTool = new ToggleButton("⬜ 矩形");
        rectTool.setToggleGroup(toolGroup);
        rectTool.setOnAction(e -> setToolMode(ToolMode.DRAW_RECT));

        ToggleButton circleTool = new ToggleButton("⭕ 圆形");
        circleTool.setToggleGroup(toolGroup);
        circleTool.setOnAction(e -> setToolMode(ToolMode.DRAW_CIRCLE));

        FlowPane toolButtons = new FlowPane(10, 10);
        toolButtons.setAlignment(Pos.CENTER_LEFT);
        toolButtons.getChildren().addAll(selectTool, cropTool, brushTool, textTool, rectTool, circleTool);

        Separator sep2 = new Separator();

        // 绘图工具设置面板
        VBox drawingSettings = createDrawingSettingsPanel();
        drawingSettings.setVisible(false); // 默认隐藏

        // 监听工具切换，显示/隐藏设置面板
        toolGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDrawingTool = newVal == brushTool || newVal == rectTool || newVal == circleTool;
            drawingSettings.setVisible(isDrawingTool);
        });

        Separator sep3 = new Separator();

        // 批量处理
        Label batchLabel = createSectionLabel("🔄 批量处理");
        Button batchBtn = new Button("批量处理图片");
        batchBtn.setPrefWidth(Double.MAX_VALUE);
        batchBtn.setOnAction(e -> startBatchProcessing());

        Separator sep4 = new Separator();

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

        Separator sep5 = new Separator();

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

        Separator sep6 = new Separator();

        // AI功能
//        Label aiLabel = createSectionLabel("🤖 AI增强");

//        Button aiEnhanceBtn = new Button("✨ AI增强");
//        aiEnhanceBtn.setPrefWidth(Double.MAX_VALUE);
//        aiEnhanceBtn.setOnAction(e -> aiEnhance());

//        Button removeBackground = new Button("🖼 移除背景");
//        removeBackground.setPrefWidth(Double.MAX_VALUE);
//        removeBackground.setOnAction(e -> removeBackground());

        Button artisticStyle = new Button("🎨 艺术风格");
        artisticStyle.setPrefWidth(Double.MAX_VALUE);
        artisticStyle.setOnAction(e -> applyArtisticStyle());

        leftPanel.getChildren().addAll(
                basicLabel, adjustmentPanel,
                sep1, toolsLabel, toolButtons, drawingSettings,
                sep2, batchLabel, batchBtn,
                sep3, transformLabel, transformButtons,
                sep4, filterLabel, blurControl, grayscaleBtn, edgeDetectBtn,
                sep5, artisticStyle
        );

        ScrollPane scrollPane = new ScrollPane(leftPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        return scrollPane;
    }

    /**
     * 创建绘图设置面板 - 修复清除按钮问题
     */
    private VBox createDrawingSettingsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 8;");

        Label settingsLabel = new Label("画笔设置");
        settingsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // 颜色选择
        HBox colorBox = new HBox(10);
        colorBox.setAlignment(Pos.CENTER_LEFT);

        Label colorLabel = new Label("颜色:");
        colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setOnAction(e -> {
            Color selectedColor = colorPicker.getValue();
            currentBrushStyle = new DrawingOperation.BrushStyle(
                    new java.awt.Color(
                            (float) selectedColor.getRed(),
                            (float) selectedColor.getGreen(),
                            (float) selectedColor.getBlue(),
                            (float) selectedColor.getOpacity()
                    ),
                    currentBrushStyle.getThickness(),
                    currentBrushStyle.getOpacity()
            );
        });

        colorBox.getChildren().addAll(colorLabel, colorPicker);

        // 画笔大小
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabel = new Label("粗细:");
        brushSizeSpinner = new Spinner<>(1, 50, 3);
        brushSizeSpinner.setEditable(true);
        brushSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentBrushStyle = new DrawingOperation.BrushStyle(
                    currentBrushStyle.getColor(),
                    newVal,
                    currentBrushStyle.getOpacity()
            );
        });

        sizeBox.getChildren().addAll(sizeLabel, brushSizeSpinner);

        // 清除当前绘图按钮 - 修复版本
        Button clearDrawingBtn = new Button("🗑️ 清除当前绘图");
        clearDrawingBtn.setOnAction(e -> {
            // 清除内存中的点
            currentBrushPoints.clear();

            // 清除画布预览
            clearCanvasPreview();

            updateStatus("当前绘图已清除");
        });

        // 应用绘图按钮
//        Button applyDrawingBtn = new Button("✅ 应用绘图");
//        applyDrawingBtn.setOnAction(e -> {
//            if (currentBrushPoints.size() >= 2) {
//                applyCurrentDrawing();
//            } else {
//                showWarning("绘图", "请先绘制一些内容");
//            }
//        });

        panel.getChildren().addAll(settingsLabel, colorBox, sizeBox, clearDrawingBtn);

        return panel;
    }

    /**
     * 清除画布预览
     */
    private void clearCanvasPreview() {
        // 在 createCenterPanel() 方法中需要给画布设置ID，以便这里能找到
        StackPane centerPane = (StackPane) imageScrollPane.getParent();
        if (centerPane != null) {
            // 查找画布
            Node canvasNode = centerPane.lookup("#selection-canvas");
            if (canvasNode instanceof Canvas) {
                Canvas canvas = (Canvas) canvasNode;
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            }
        }
    }

    // ==================== 绘图、裁剪、批量处理方法 ====================

    /**
     * 非交互式文字添加方法也需要修复
     */
    private void addText() {
        if (currentImage == null) {
            showWarning("提示", "请先加载图片");
            return;
        }

        // 创建自定义对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("添加文字");
        dialog.setHeaderText("输入要添加的文字");

        // 使用支持中文的字体
        Font chineseFont = Font.font("Microsoft YaHei", 14);
        TextArea textArea = new TextArea();
        textArea.setFont(chineseFont);
        textArea.setPromptText("请输入文字...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(new Label("文字:"), textArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 验证输入
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            okButton.setDisable(newText.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return textArea.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(text -> {
            // 创建文字样式
            DrawingOperation.TextStyle textStyle = new DrawingOperation.TextStyle(
                    getSystemChineseFont(),  // 使用系统中文字体
                    24,
                    java.awt.Color.BLACK,
                    false, false, false);

            // 创建绘图元素
            List<DrawingOperation.DrawingPoint> points = new ArrayList<>();
            points.add(new DrawingOperation.DrawingPoint(50, 50));

            DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                    DrawingOperation.DrawingType.TEXT,
                    points,
                    text,
                    null,
                    textStyle);

            // 创建绘图操作
            DrawingOperation operation = new DrawingOperation(element);
            applyOperation(operation, "添加文字");
        });
    }

    /**
     * 开始绘制
     */
    private void startDrawing() {
        showWarning("功能提示", "画笔功能需要在图像上直接绘制\n请等待后续版本实现交互式绘图");
    }

    /**
     * 绘制矩形
     */
    private void drawRectangle() {
        if (currentImage == null) {
            showWarning("提示", "请先加载图片");
            return;
        }

        // 创建画笔样式
        DrawingOperation.BrushStyle brushStyle = new DrawingOperation.BrushStyle(
                java.awt.Color.RED, 3, 1.0f);

        // 创建绘图点（示例位置）
        List<DrawingOperation.DrawingPoint> points = new ArrayList<>();
        points.add(new DrawingOperation.DrawingPoint(50, 50));
        points.add(new DrawingOperation.DrawingPoint(200, 150));

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                DrawingOperation.DrawingType.RECTANGLE, points, null, brushStyle, null);

        DrawingOperation operation = new DrawingOperation(element);
        applyOperation(operation, "绘制矩形");
    }

    /**
     * 绘制圆形
     */
    private void drawCircle() {
        if (currentImage == null) {
            showWarning("提示", "请先加载图片");
            return;
        }

        // 创建画笔样式
        DrawingOperation.BrushStyle brushStyle = new DrawingOperation.BrushStyle(
                java.awt.Color.BLUE, 3, 1.0f);

        // 创建绘图点（示例位置）
        List<DrawingOperation.DrawingPoint> points = new ArrayList<>();
        points.add(new DrawingOperation.DrawingPoint(100, 100));
        points.add(new DrawingOperation.DrawingPoint(200, 200));

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                DrawingOperation.DrawingType.CIRCLE, points, null, brushStyle, null);

        DrawingOperation operation = new DrawingOperation(element);
        applyOperation(operation, "绘制圆形");
    }

    /**
     * 开始裁剪
     */
    private void startCrop() {
        if (currentImage == null) {
            showWarning("提示", "请先加载图片");
            return;
        }

        // 创建裁剪对话框
        Dialog<Rectangle> dialog = new Dialog<>();
        dialog.setTitle("裁剪图片");
        dialog.setHeaderText("输入裁剪区域");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        int imageWidth = (int) currentImage.getWidth();
        int imageHeight = (int) currentImage.getHeight();

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

                    return new Rectangle(x, y, width, height);
                } catch (NumberFormatException e) {
                    showError("输入错误", "请输入有效的数字");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cropArea -> {
            if (cropArea.width > 0 && cropArea.height > 0) {
                CropOperation operation = new CropOperation(cropArea);
                applyOperation(operation, "裁剪图片");
            }
        });
    }

    /**
     * 开始批量处理
     */
    private void startBatchProcessing() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择多张图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        if (files != null && !files.isEmpty()) {
            showBatchProcessingDialog(files);
        }
    }

    /**
     * 显示批量处理对话框
     */
    private void showBatchProcessingDialog(List<File> files) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("批量处理");
        dialog.setHeaderText("选择要应用的操作");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        Label infoLabel = new Label("已选择 " + files.size() + " 张图片");
        infoLabel.setStyle("-fx-font-weight: bold;");

        // 选择操作类型
        ComboBox<String> operationCombo = new ComboBox<>();
        operationCombo.getItems().addAll("灰度化", "调整亮度", "调整对比度", "调整饱和度", "模糊", "边缘检测", "旋转90度");
        operationCombo.setValue("灰度化");

        // 参数控制
        VBox paramBox = new VBox(10);
        paramBox.setVisible(false);

        Slider paramSlider = new Slider(-100, 100, 0);
        paramSlider.setShowTickLabels(true);
        paramSlider.setShowTickMarks(true);

        operationCombo.setOnAction(e -> {
            paramBox.setVisible(!operationCombo.getValue().equals("灰度化") &&
                    !operationCombo.getValue().equals("边缘检测") &&
                    !operationCombo.getValue().equals("旋转90度"));
        });

        paramBox.getChildren().addAll(new Label("参数值:"), paramSlider);

        // 输出设置
        TextField suffixField = new TextField("_processed");
        suffixField.setPromptText("输出文件后缀");

        Button startBtn = new Button("开始批量处理");
        startBtn.setOnAction(e -> {
            executeBatchProcessing(files, operationCombo.getValue(),
                    paramSlider.getValue(), suffixField.getText());
            dialog.close();
        });

        content.getChildren().addAll(infoLabel,
                new Label("选择操作:"), operationCombo,
                paramBox,
                new Label("输出文件后缀:"), suffixField,
                startBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    /**
     * 执行批量处理
     */
    private void executeBatchProcessing(List<File> files, String operationType,
                                        double paramValue, String suffix) {
        showProgress("批量处理中...");

        new Thread(() -> {
            try {
                List<BufferedImage> images = new ArrayList<>();
                List<String> imageNames = new ArrayList<>();

                // 加载所有图片
                for (File file : files) {
                    try {
                        BufferedImage img = ImageIO.read(file);
                        if (img != null) {
                            images.add(img);
                            imageNames.add(file.getName());
                        }
                    } catch (Exception e) {
                        System.err.println("无法加载图片: " + file.getName() + " - " + e.getMessage());
                    }
                }

                if (images.isEmpty()) {
                    Platform.runLater(() -> {
                        hideProgress();
                        showError("批量处理失败", "无法加载任何图片");
                    });
                    return;
                }

                // 创建操作
                ImageOperation operation = createBatchOperation(operationType, paramValue);

                // 创建批量处理配置
                List<BatchOperation.BatchTask> tasks = new ArrayList<>();
                for (int i = 0; i < images.size(); i++) {
                    List<ImageOperation> operations = new ArrayList<>();
                    operations.add(operation);

                    BatchOperation.BatchConfig config = new BatchOperation.BatchConfig(
                            BatchOperation.BatchMode.SINGLE_OPERATION,
                            operations,
                            Math.min(4, Runtime.getRuntime().availableProcessors()),
                            false,
                            suffix
                    );

                    tasks.add(new BatchOperation.BatchTask(
                            images.get(i),
                            imageNames.get(i),
                            config
                    ));
                }

                // 执行批量处理
                BatchOperation batchOp = BatchOperation.createSingleOperationBatch(tasks, operation);

                // 创建进度监听器
                BatchOperation.BatchProgressListener listener = new BatchOperation.BatchProgressListener() {
                    private int processed = 0;

                    @Override
                    public void onProgress(String imageName, int processedCount, int total) {
                        Platform.runLater(() -> {
                            updateStatus(String.format("批量处理: %s (%d/%d)",
                                    imageName, processedCount, total));
                        });
                    }

                    @Override
                    public void onTaskComplete(String imageName, boolean success) {
                        processed++;
                        Platform.runLater(() -> {
                            if (success) {
                                updateHistory("批量处理: " + imageName);
                            }
                        });
                    }

                    @Override
                    public void onBatchComplete(int successCount, int total) {
                        Platform.runLater(() -> {
                            hideProgress();
                            if (successCount == total) {
                                showSuccess("批量处理完成",
                                        String.format("成功处理 %d/%d 张图片", successCount, total));
                            } else {
                                showWarning("批量处理完成",
                                        String.format("成功处理 %d/%d 张图片，失败 %d 张",
                                                successCount, total, total - successCount));
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
                            String originalName = imageNames.get(i);
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

                            ImageIO.write(result.getResultImage(), format, outputFile);
                        } catch (Exception e) {
                            System.err.println("保存失败: " + imageNames.get(i) + " - " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    showError("批量处理失败", e.getMessage());
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
     * 创建高级调整面板
     */
    private VBox createAdvancedAdjustmentPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        // 初始样式将在主题应用时设置

        Label title = new Label("🔧 基础调整");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 亮度调节滑块
        VBox brightnessControl = createAdvancedSlider("亮度", -50, 50, brightnessValue, (value) -> {
            brightnessValue = value;
            updateStatus(String.format("亮度: %.0f", value));
        });

        // 对比度调节滑块
        VBox contrastControl = createAdvancedSlider("对比度", -50, 50, contrastValue, (value) -> {
            contrastValue = value;
            updateStatus(String.format("对比度: %.0f", value));
        });

        // 饱和度调节滑块
        VBox saturationControl = createAdvancedSlider("饱和度", -50, 50, saturationValue, (value) -> {
            saturationValue = value;
            updateStatus(String.format("饱和度: %.0f", value));
        });

        Separator separator = new Separator();

        // 应用所有调整按钮
        HBox buttonBox = createAdjustmentButtons();

        panel.getChildren().addAll(
                title,
                brightnessControl,
                contrastControl,
                saturationControl,
                separator,
                buttonBox
        );

        return panel;
    }

    /**
     * 创建高级滑块控件
     */
    private VBox createAdvancedSlider(String label, double min, double max, double initialValue,
                                      SliderChangeListener listener) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(5));

        HBox labelBox = new HBox(10);
        labelBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(String.format("%.0f", initialValue));
        valueLabel.setId(label + "-value");
        valueLabel.setStyle("-fx-font-size: 12px; " +
                "-fx-background-color: rgba(0,0,0,0.1); " +
                "-fx-background-radius: 4; " +
                "-fx-padding: 2 8;");

        labelBox.getChildren().addAll(nameLabel, spacer, valueLabel);

        Slider slider = new Slider(min, max, initialValue);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(5);
        slider.setSnapToTicks(false);
        slider.setId(label + "-slider");
        slider.setStyle("-fx-control-inner-background: #e9ecef;");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int intValue = newVal.intValue();
            valueLabel.setText(String.format("%d", intValue));
            if (listener != null) {
                listener.onChange(newVal.doubleValue());
            }
        });

        box.getChildren().addAll(labelBox, slider);

        return box;
    }

    /**
     * 创建调整按钮组
     */
    private HBox createAdjustmentButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // 应用按钮
        Button applyBtn = new Button("✅ 应用调整");
        applyBtn.setOnAction(e -> applyAllAdjustments());

        // 重置按钮
        Button resetBtn = new Button("🔄 重置");
        resetBtn.setOnAction(e -> resetAllAdjustments());

        buttonBox.getChildren().addAll(applyBtn, resetBtn);

        return buttonBox;
    }
    /**
     * 创建中心图像显示区域 - 增强交互功能
     */
    private StackPane createCenterPanel() {
        StackPane centerPane = new StackPane();

        // 图像容器
        VBox imageContainer = new VBox(20);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(20));

        // 图像视图
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // 创建交互覆盖层
        Pane interactionOverlay = new Pane();
        interactionOverlay.setMouseTransparent(false);
        interactionOverlay.setStyle("-fx-background-color: transparent;");


        // 创建用于显示选择框的画布
        Canvas selectionCanvas = new Canvas();
        selectionCanvas.setMouseTransparent(true); // 画布不接收鼠标事件
        selectionCanvas.setId("selection-canvas");  // 设置ID
        GraphicsContext gc = selectionCanvas.getGraphicsContext2D();

        StackPane imagePane = new StackPane();
        imagePane.setStyle("-fx-background-color: transparent;");
        imagePane.getChildren().addAll(imageView, selectionCanvas, interactionOverlay);

        // 为覆盖层添加鼠标事件监听
        setupMouseInteraction(interactionOverlay, selectionCanvas);

        // 图像控制按钮
        HBox controlButtons = new HBox(15);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setId("control-buttons");
        controlButtons.setStyle("-fx-background-color: rgba(255,255,255,0.9); " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 8 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Button zoomIn = createIconButton("➕", "放大");
        zoomIn.setOnAction(e -> zoomIn());

        Button zoomOut = createIconButton("➖", "缩小");
        zoomOut.setOnAction(e -> zoomOut());

        Button zoomFit = createIconButton("⛶", "适应");
        zoomFit.setOnAction(e -> fitToWindow());

        Button zoom100 = createIconButton("1:1", "原始");
        zoom100.setOnAction(e -> resetZoom());

        // 添加确认裁剪按钮
        Button confirmCropBtn = createIconButton("✓", "确认裁剪");
        confirmCropBtn.setVisible(false);
        confirmCropBtn.setOnAction(e -> applyCropSelection());

        controlButtons.getChildren().addAll(zoomIn, zoomOut, zoomFit, zoom100, confirmCropBtn);

        imageContainer.getChildren().addAll(imagePane, controlButtons);

        // 滚动面板
        imageScrollPane = new ScrollPane(imageContainer);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background-color: transparent;");
        imageScrollPane.setId("image-scroll-pane");

        // 占位符
        VBox placeholder = new VBox(20);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setId("placeholder");
        placeholder.setStyle("-fx-background-color: transparent;");

        Label placeholderIcon = new Label("📷");
        placeholderIcon.setStyle("-fx-font-size: 80px; -fx-opacity: 0.3;");

        Label placeholderText = new Label("点击打开按钮选择图片");
        placeholderText.setStyle("-fx-font-size: 18px; -fx-opacity: 0.6;");

        Button quickOpenBtn = new Button("📁 打开图片");
        quickOpenBtn.setOnAction(e -> openImage());

        placeholder.getChildren().addAll(placeholderIcon, placeholderText, quickOpenBtn);

        // 初始状态
        imageScrollPane.setVisible(false);
        controlButtons.setVisible(false);
        placeholder.setVisible(true);

        centerPane.getChildren().addAll(imageScrollPane, placeholder);

        return centerPane;
    }

    /**
     * 设置鼠标交互
     */
    private void setupMouseInteraction(Pane overlay, Canvas selectionCanvas) {
        overlay.setOnMousePressed(e -> {
            if (currentImage == null) return;

            double mouseX = e.getX();
            double mouseY = e.getY();

            // 转换为图像原始坐标
            double[] imageCoords = convertToImageCoordinates(mouseX, mouseY);

            switch (currentToolMode) {
                case CROP:
                    startCropSelection(imageCoords[0], imageCoords[1]);
                    isSelectingCrop = true;
                    break;

                case DRAW_BRUSH:
                    startDrawing(imageCoords[0], imageCoords[1]);
                    break;

                case DRAW_RECT:
                case DRAW_CIRCLE:
                    startShapeDrawing(imageCoords[0], imageCoords[1]);
                    break;
            }
        });

        overlay.setOnMouseDragged(e -> {
            if (currentImage == null) return;

            double mouseX = e.getX();
            double mouseY = e.getY();
            double[] imageCoords = convertToImageCoordinates(mouseX, mouseY);

            switch (currentToolMode) {
                case CROP:
                    if (isSelectingCrop) {
                        updateCropSelection(imageCoords[0], imageCoords[1], selectionCanvas);
                    }
                    break;

                case DRAW_BRUSH:
                    continueDrawing(imageCoords[0], imageCoords[1], selectionCanvas);
                    break;

                case DRAW_RECT:
                case DRAW_CIRCLE:
                    updateShapeDrawing(imageCoords[0], imageCoords[1], selectionCanvas);
                    break;
            }
        });

        overlay.setOnMouseReleased(e -> {
            if (currentImage == null) return;

            double mouseX = e.getX();
            double mouseY = e.getY();
            double[] imageCoords = convertToImageCoordinates(mouseX, mouseY);

            switch (currentToolMode) {
                case CROP:
                    if (isSelectingCrop) {
                        endCropSelection(imageCoords[0], imageCoords[1]);
                        isSelectingCrop = false;
                        // 显示确认按钮
                        HBox controlButtons = (HBox) imageScrollPane.getContent().lookup("#control-buttons");
                        if (controlButtons != null) {
                            Button confirmCropBtn = (Button) controlButtons.getChildren().get(4);
                            confirmCropBtn.setVisible(cropSelection != null);
                        }
                    }
                    break;

                case DRAW_BRUSH:
                    endDrawing();
                    break;

                case DRAW_RECT:
                case DRAW_CIRCLE:
                    endShapeDrawing(imageCoords[0], imageCoords[1]);
                    break;
            }
        });

        // 文字工具：点击时添加文字
        overlay.setOnMouseClicked(e -> {
            if (currentImage == null) return;

            if (currentToolMode == ToolMode.DRAW_TEXT) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                double[] imageCoords = convertToImageCoordinates(mouseX, mouseY);

                addTextAtPosition((int)imageCoords[0], (int)imageCoords[1]);
            }
        });
    }

    /**
     * 转换屏幕坐标到图像原始坐标
     */
    private double[] convertToImageCoordinates(double screenX, double screenY) {
        if (currentImage == null) return new double[]{0, 0};

        // 获取ImageView的边界
        double viewX = imageView.getBoundsInParent().getMinX();
        double viewY = imageView.getBoundsInParent().getMinY();
        double viewWidth = imageView.getBoundsInParent().getWidth();
        double viewHeight = imageView.getBoundsInParent().getHeight();

        // 获取原始图像尺寸
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();

        // 计算缩放比例
        double scaleX = imageWidth / viewWidth;
        double scaleY = imageHeight / viewHeight;

        // 计算相对于ImageView的坐标
        double relativeX = screenX - viewX;
        double relativeY = screenY - viewY;

        // 转换为原始图像坐标
        double imageX = relativeX * scaleX;
        double imageY = relativeY * scaleY;

        // 确保坐标在图像范围内
        imageX = Math.max(0, Math.min(imageX, imageWidth));
        imageY = Math.max(0, Math.min(imageY, imageHeight));

        return new double[]{imageX, imageY};
    }

    /**
     * 设置工具模式
     */
    private void setToolMode(ToolMode mode) {
        currentToolMode = mode;

        // 清除当前选择
        cropSelection = null;
        currentBrushPoints.clear();

        // 隐藏确认裁剪按钮
        if (mode != ToolMode.CROP) {
            HBox controlButtons = (HBox) imageScrollPane.getContent().lookup("#control-buttons");
            if (controlButtons != null && controlButtons.getChildren().size() > 4) {
                Button confirmCropBtn = (Button) controlButtons.getChildren().get(4);
                confirmCropBtn.setVisible(false);
            }
        }

        updateStatus("切换到模式: " + mode.toString());
    }

    /**
     * 开始选择裁剪区域
     */
    private void startCropSelection(double startX, double startY) {
        cropStartX = startX;
        cropStartY = startY;
        cropSelection = new Rectangle((int)startX, (int)startY, 0, 0);
    }

    /**
     * 更新裁剪选择区域
     */
    private void updateCropSelection(double endX, double endY, Canvas canvas) {
        if (cropSelection == null) return;

        double x = Math.min(cropStartX, endX);
        double y = Math.min(cropStartY, endY);
        double width = Math.abs(endX - cropStartX);
        double height = Math.abs(endY - cropStartY);

        cropSelection.setRect(x, y, width, height);

        // 在画布上绘制选择框
        drawSelectionRect(canvas, x, y, width, height);
    }

    /**
     * 结束裁剪选择
     */
    private void endCropSelection(double endX, double endY) {
        if (cropSelection == null) return;

        double x = Math.min(cropStartX, endX);
        double y = Math.min(cropStartY, endY);
        double width = Math.abs(endX - cropStartX);
        double height = Math.abs(endY - cropStartY);

        cropSelection.setRect(x, y, width, height);

        updateStatus(String.format("裁剪区域: (%.0f, %.0f) %.0f×%.0f", x, y, width, height));
    }

    /**
     * 应用裁剪选择
     */
    private void applyCropSelection() {
        if (cropSelection == null || currentImage == null) return;

        // 转换为整数
        int x = (int) Math.round(cropSelection.getX());
        int y = (int) Math.round(cropSelection.getY());
        int width = (int) Math.round(cropSelection.getWidth());
        int height = (int) Math.round(cropSelection.getHeight());

        // 确保在图像范围内
        int imageWidth = (int) currentImage.getWidth();
        int imageHeight = (int) currentImage.getHeight();

        x = Math.max(0, Math.min(x, imageWidth - 1));
        y = Math.max(0, Math.min(y, imageHeight - 1));
        width = Math.min(width, imageWidth - x);
        height = Math.min(height, imageHeight - y);

        if (width <= 0 || height <= 0) {
            showWarning("无效区域", "裁剪区域太小或无效");
            return;
        }

        CropOperation operation = new CropOperation(x, y, width, height);
        applyOperation(operation, "裁剪图片");

        // 清除选择
        cropSelection = null;

        // 隐藏确认按钮
        HBox controlButtons = (HBox) imageScrollPane.getContent().lookup("#control-buttons");
        if (controlButtons != null && controlButtons.getChildren().size() > 4) {
            Button confirmCropBtn = (Button) controlButtons.getChildren().get(4);
            confirmCropBtn.setVisible(false);
        }
    }

    /**
     * 在画布上绘制选择框
     */
    private void drawSelectionRect(Canvas canvas, double x, double y, double width, double height) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 清除画布
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 设置画布大小与ImageView相同
        canvas.setWidth(imageView.getBoundsInParent().getWidth());
        canvas.setHeight(imageView.getBoundsInParent().getHeight());

        // 计算屏幕坐标
        double viewWidth = imageView.getBoundsInParent().getWidth();
        double viewHeight = imageView.getBoundsInParent().getHeight();
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();

        double scaleX = viewWidth / imageWidth;
        double scaleY = viewHeight / imageHeight;

        double screenX = x * scaleX;
        double screenY = y * scaleY;
        double screenWidth = width * scaleX;
        double screenHeight = height * scaleY;

        // 绘制半透明填充
        gc.setFill(Color.rgb(0, 150, 255, 0.1));
        gc.fillRect(screenX, screenY, screenWidth, screenHeight);

        // 绘制边框
        gc.setStroke(Color.rgb(0, 150, 255, 0.8));
        gc.setLineWidth(2);
        gc.strokeRect(screenX, screenY, screenWidth, screenHeight);

        // 绘制角点
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.rgb(0, 150, 255, 0.8));

        double cornerSize = 8;

        // 左上角
        gc.fillRect(screenX - cornerSize/2, screenY - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(screenX - cornerSize/2, screenY - cornerSize/2, cornerSize, cornerSize);

        // 右上角
        gc.fillRect(screenX + screenWidth - cornerSize/2, screenY - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(screenX + screenWidth - cornerSize/2, screenY - cornerSize/2, cornerSize, cornerSize);

        // 左下角
        gc.fillRect(screenX - cornerSize/2, screenY + screenHeight - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(screenX - cornerSize/2, screenY + screenHeight - cornerSize/2, cornerSize, cornerSize);

        // 右下角
        gc.fillRect(screenX + screenWidth - cornerSize/2, screenY + screenHeight - cornerSize/2, cornerSize, cornerSize);
        gc.strokeRect(screenX + screenWidth - cornerSize/2, screenY + screenHeight - cornerSize/2, cornerSize, cornerSize);
    }

    /**
     * 开始绘图
     */
    private void startDrawing(double x, double y) {
        currentBrushPoints.clear();
        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
    }

    /**
     * 继续绘图
     */
    private void continueDrawing(double x, double y, Canvas canvas) {
        if (currentBrushPoints.isEmpty()) return;

        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
        drawBrushPreview(canvas);
    }

    /**
     * 结束绘图
     */
    private void endDrawing() {
        if (currentBrushPoints.size() >= 2) {
            applyCurrentDrawing();
        }
        currentBrushPoints.clear();
    }

    /**
     * 应用当前绘图
     */
    private void applyCurrentDrawing() {
        if (currentBrushPoints.size() < 2) {
            showWarning("绘图", "请先绘制一些内容");
            return;
        }

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                DrawingOperation.DrawingType.BRUSH,
                new ArrayList<>(currentBrushPoints),
                null,
                currentBrushStyle,
                null
        );

        DrawingOperation operation = new DrawingOperation(element);
        applyOperation(operation, "画笔绘制");

        currentBrushPoints.clear();
        updateStatus("绘图已应用");
    }

    /**
     * 在画布上绘制画笔预览
     */
    private void drawBrushPreview(Canvas canvas) {
        if (currentBrushPoints.size() < 2) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 设置画布大小
        canvas.setWidth(imageView.getBoundsInParent().getWidth());
        canvas.setHeight(imageView.getBoundsInParent().getHeight());

        // 转换为屏幕坐标
        double viewWidth = imageView.getBoundsInParent().getWidth();
        double viewHeight = imageView.getBoundsInParent().getHeight();
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();

        double scaleX = viewWidth / imageWidth;
        double scaleY = viewHeight / imageHeight;

        // 设置画笔样式
        java.awt.Color color = currentBrushStyle.getColor();
        gc.setStroke(Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255.0));
        gc.setLineWidth(currentBrushStyle.getThickness() * Math.min(scaleX, scaleY));
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        // 绘制线条
        for (int i = 0; i < currentBrushPoints.size() - 1; i++) {
            DrawingOperation.DrawingPoint p1 = currentBrushPoints.get(i);
            DrawingOperation.DrawingPoint p2 = currentBrushPoints.get(i + 1);

            double x1 = p1.getX() * scaleX;
            double y1 = p1.getY() * scaleY;
            double x2 = p2.getX() * scaleX;
            double y2 = p2.getY() * scaleY;

            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    /**
     * 在指定位置添加文字 - 修复中文乱码问题
     */
    private void addTextAtPosition(int x, int y) {
        // 创建自定义的文本输入对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("添加文字");
        dialog.setHeaderText("输入要添加的文字");

        // 使用支持中文的字体
        Font chineseFont = Font.font("Microsoft YaHei", 14);

        // 创建文本输入区域
        TextArea textArea = new TextArea();
        textArea.setFont(chineseFont);
        textArea.setPromptText("请输入文字...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.setPrefColumnCount(20);

        // 设置对话框内容
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(new Label("文字:"), textArea);

        dialog.getDialogPane().setContent(content);

        // 添加按钮
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 验证输入
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            okButton.setDisable(newText.trim().isEmpty());
        });

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return textArea.getText().trim();
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(text -> {
            if (text.isEmpty()) {
                showWarning("输入错误", "请输入有效的文字");
                return;
            }

            // 创建文字样式 - 使用支持中文的字体
            DrawingOperation.TextStyle textStyle = new DrawingOperation.TextStyle(
                    getSystemChineseFont(),  // 获取系统中文字体
                    24,
                    currentBrushStyle.getColor(),
                    false, false, false);

            // 创建绘图元素
            List<DrawingOperation.DrawingPoint> points = new ArrayList<>();
            points.add(new DrawingOperation.DrawingPoint(x, y));

            DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                    DrawingOperation.DrawingType.TEXT,
                    points,
                    text,
                    null,
                    textStyle);

            // 创建绘图操作
            DrawingOperation operation = new DrawingOperation(element);
            applyOperation(operation, "添加文字");
        });
    }

    /**
     * 获取系统可用的中文字体
     */
    private String getSystemChineseFont() {
        // 优先使用常见的中文字体
        String[] chineseFonts = {
                "Microsoft YaHei",      // Windows
                "PingFang SC",         // macOS
                "Noto Sans CJK SC",    // Linux/通用
                "SimHei",              // 黑体
                "SimSun",              // 宋体
                "NSimSun",             // 新宋体
                "KaiTi",               // 楷体
                "FangSong",            // 仿宋
                "Microsoft JhengHei",  // 繁体
                "STXihei",             // 华文细黑
                "STSong",              // 华文宋体
                "STKaiti",             // 华文楷体
                "STFangsong"          // 华文仿宋
        };

        // 检查系统字体
        List<String> systemFonts = javafx.scene.text.Font.getFamilies();

        for (String font : chineseFonts) {
            if (systemFonts.contains(font)) {
                return font;
            }
        }

        // 如果没有找到中文字体，使用默认字体并尝试加载
        return "Microsoft YaHei";
    }

    /**
     * 开始形状绘制
     */
    private void startShapeDrawing(double x, double y) {
        currentBrushPoints.clear();
        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
        currentBrushPoints.add(new DrawingOperation.DrawingPoint((int)x, (int)y));
    }

    /**
     * 更新形状绘制
     */
    private void updateShapeDrawing(double x, double y, Canvas canvas) {
        if (currentBrushPoints.size() < 2) return;

        currentBrushPoints.set(1, new DrawingOperation.DrawingPoint((int)x, (int)y));
        drawShapePreview(canvas);
    }

    /**
     * 结束形状绘制
     */
    private void endShapeDrawing(double x, double y) {
        if (currentBrushPoints.size() >= 2) {
            currentBrushPoints.set(1, new DrawingOperation.DrawingPoint((int)x, (int)y));
            applyCurrentShape();
        }
        currentBrushPoints.clear();
    }

    /**
     * 应用当前形状
     */
    private void applyCurrentShape() {
        if (currentBrushPoints.size() < 2) return;

        DrawingOperation.DrawingType type;
        switch (currentToolMode) {
            case DRAW_RECT:
                type = DrawingOperation.DrawingType.RECTANGLE;
                break;
            case DRAW_CIRCLE:
                type = DrawingOperation.DrawingType.CIRCLE;
                break;
            default:
                return;
        }

        DrawingOperation.DrawingElement element = new DrawingOperation.DrawingElement(
                type,
                new ArrayList<>(currentBrushPoints),
                null,
                currentBrushStyle,
                null
        );

        DrawingOperation operation = new DrawingOperation(element);
        applyOperation(operation, type == DrawingOperation.DrawingType.RECTANGLE ? "绘制矩形" : "绘制圆形");

        currentBrushPoints.clear();
    }

    /**
     * 在画布上绘制形状预览
     */
    private void drawShapePreview(Canvas canvas) {
        if (currentBrushPoints.size() < 2) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 设置画布大小
        canvas.setWidth(imageView.getBoundsInParent().getWidth());
        canvas.setHeight(imageView.getBoundsInParent().getHeight());

        // 转换为屏幕坐标
        double viewWidth = imageView.getBoundsInParent().getWidth();
        double viewHeight = imageView.getBoundsInParent().getHeight();
        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();

        double scaleX = viewWidth / imageWidth;
        double scaleY = viewHeight / imageHeight;

        DrawingOperation.DrawingPoint p1 = currentBrushPoints.get(0);
        DrawingOperation.DrawingPoint p2 = currentBrushPoints.get(1);

        double x1 = p1.getX() * scaleX;
        double y1 = p1.getY() * scaleY;
        double x2 = p2.getX() * scaleX;
        double y2 = p2.getY() * scaleY;

        double x = Math.min(x1, x2);
        double y = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);

        // 设置画笔样式
        java.awt.Color color = currentBrushStyle.getColor();
        gc.setStroke(Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255.0));
        gc.setLineWidth(currentBrushStyle.getThickness() * Math.min(scaleX, scaleY));
        gc.setLineDashes(0);

        switch (currentToolMode) {
            case DRAW_RECT:
                gc.strokeRect(x, y, width, height);
                break;
            case DRAW_CIRCLE:
                double radius = Math.min(width, height) / 2;
                double centerX = x + width / 2;
                double centerY = y + height / 2;
                gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                break;
        }
    }
    /**
     * 创建右侧面板
     */
    private ScrollPane createRightPanel() {
        rightPanel = new VBox(20);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setPrefWidth(280);
        // 初始样式将在主题应用时设置

        // 历史记录
        Label historyLabel = createSectionLabel("📜 操作历史");

        historyListView = new ListView<>();
        historyListView.setPrefHeight(300);
        // 初始样式将在主题应用时设置

        Separator sep1 = new Separator();

        // 图像信息
        Label infoLabel = createSectionLabel("ℹ️ 图像信息");

        VBox infoBox = new VBox(10);
        // 初始样式将在主题应用时设置

        Label sizeLabel = new Label("尺寸: --");
        sizeLabel.setStyle("-fx-font-size: 13px;");

        Label formatLabel = new Label("格式: --");
        formatLabel.setStyle("-fx-font-size: 13px;");

        Label fileSizeLabel = new Label("大小: --");
        fileSizeLabel.setStyle("-fx-font-size: 13px;");

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
        scrollPane.setStyle("-fx-background-color: transparent;");

        return scrollPane;
    }

    /**
     * 创建底部状态栏
     */
    private HBox createBottomBar() {
        HBox bottomBar = new HBox(20);
        bottomBar.setPadding(new Insets(10, 20, 10, 20));
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        // 初始样式将在主题应用时设置

        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);

        Label versionLabel = new Label("v2.0 Pro | 主题: " + currentTheme.getDisplayName());
        versionLabel.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");

        bottomBar.getChildren().addAll(statusLabel, spacer, progressIndicator, versionLabel);

        return bottomBar;
    }

    // ==================== UI辅助方法 ====================

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

    private Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.8); " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand; " +
                "-fx-border-color: #dee2e6; " +
                "-fx-border-width: 1;");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(255,255,255,1); " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 8 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: #adb5bd; " +
                        "-fx-border-width: 1;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.8); " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 8 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1;"
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
        nameLabel.setStyle("-fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(String.format("%.0f", value));
        valueLabel.setStyle("-fx-font-size: 12px; " +
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

    // ==================== 动画效果 ====================

    private void playEntryAnimation(BorderPane root) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void playImageLoadAnimation() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), imageView);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    private void playSuccessAnimation() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), imageView);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
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
                    if (imageEditorService != null) {
                        imageEditorService.initImageProcessor(currentImage);
                    }

                    updateHistory("打开图片: " + file.getName());
                    updateStatus("图片已加载: " + file.getName() + " (" +
                            (int)currentImage.getWidth() + "×" + (int)currentImage.getHeight() + ")");
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

                    ImageIO.write(bufferedImage, format, file);

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

    private void applyAllAdjustments() {
        if (currentImage == null || imageEditorService == null) {
            showWarning("提示", "请先加载图片");
            return;
        }

        // 检查是否有调整需要应用
        if (brightnessValue == 0 && contrastValue == 0 && saturationValue == 0) {
            showWarning("提示", "请先调整滑块参数");
            return;
        }

        showProgress("正在应用调整...");

        new Thread(() -> {
            try {
                // 保存原始图片用于回退
                Image originalImage = currentImage;

                // 依次应用调整
                if (brightnessValue != 0) {
                    BrightnessOperation.BrightnessMode mode = brightnessValue >= 0 ?
                            BrightnessOperation.BrightnessMode.INCREASE :
                            BrightnessOperation.BrightnessMode.DECREASE;
                    float intensity = (float)(Math.abs(brightnessValue) / 100.0);
                    BrightnessOperation brightnessOp = new BrightnessOperation(mode, intensity);

                    imageEditorService.applyOperationAsync(
                            brightnessOp,
                            resultImage -> Platform.runLater(() -> {
                                currentImage = resultImage;
                                imageView.setImage(currentImage);
                                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            }),
                            exception -> Platform.runLater(() -> {
                                showError("亮度调整失败", exception.getMessage());
                            })
                    );

                    Thread.sleep(100);
                }

                if (contrastValue != 0) {
                    float contrastLevel = (float)(contrastValue / 100.0f + 1.0f);
                    ContrastOperation contrastOp = new ContrastOperation(contrastLevel);

                    imageEditorService.applyOperationAsync(
                            contrastOp,
                            resultImage -> Platform.runLater(() -> {
                                currentImage = resultImage;
                                imageView.setImage(currentImage);
                                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            }),
                            exception -> Platform.runLater(() -> {
                                showError("对比度调整失败", exception.getMessage());
                            })
                    );

                    Thread.sleep(100);
                }

                if (saturationValue != 0) {
                    float saturationFactor = (float)(saturationValue / 100.0f + 1.0f);
                    SaturationOperation saturationOp = new SaturationOperation(saturationFactor);

                    imageEditorService.applyOperationAsync(
                            saturationOp,
                            resultImage -> Platform.runLater(() -> {
                                currentImage = resultImage;
                                imageView.setImage(currentImage);
                                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            }),
                            exception -> Platform.runLater(() -> {
                                showError("饱和度调整失败", exception.getMessage());
                            })
                    );

                    Thread.sleep(100);
                }

                Thread.sleep(300);

                Platform.runLater(() -> {
                    imageView.setImage(currentImage);
                    updateHistory("基础调整");
                    updateStatus("基础调整已应用");
                    hideProgress();
                    playSuccessAnimation();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    showError("调整失败", e.getMessage());
                });
            }
        }).start();
    }

    private void resetAllAdjustments() {
        // 重置缓存值
        brightnessValue = 0.0;
        contrastValue = 0.0;
        saturationValue = 0.0;

        // 更新滑块显示
        Slider brightnessSlider = (Slider) leftPanel.lookup("#亮度-slider");
        Slider contrastSlider = (Slider) leftPanel.lookup("#对比度-slider");
        Slider saturationSlider = (Slider) leftPanel.lookup("#饱和度-slider");

        if (brightnessSlider != null) {
            brightnessSlider.setValue(0);
            Label brightnessValueLabel = (Label) leftPanel.lookup("#亮度-value");
            if (brightnessValueLabel != null) {
                brightnessValueLabel.setText("0");
            }
        }

        if (contrastSlider != null) {
            contrastSlider.setValue(0);
            Label contrastValueLabel = (Label) leftPanel.lookup("#对比度-value");
            if (contrastValueLabel != null) {
                contrastValueLabel.setText("0");
            }
        }

        if (saturationSlider != null) {
            saturationSlider.setValue(0);
            Label saturationValueLabel = (Label) leftPanel.lookup("#饱和度-value");
            if (saturationValueLabel != null) {
                saturationValueLabel.setText("0");
            }
        }

        // 如果已加载图片，重置到原始状态
        if (currentImageFile != null) {
            loadImage(currentImageFile);
        }

        updateStatus("调整已重置");
        showSuccess("重置完成", "所有调整已重置为默认值");
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
                        (ImageOperation) operation,
                        resultImage -> Platform.runLater(() -> {
                            currentImage = resultImage;
                            imageView.setImage(currentImage);
                            currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                            updateHistory(operationName);
                            updateStatus(operationName + "完成");
                            hideProgress();
                            playSuccessAnimation();
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

    private void resetZoom() {
        currentZoom = 1.0;
        imageView.setScaleX(currentZoom);
        imageView.setScaleY(currentZoom);
        if (currentImage != null) {
            imageView.setFitWidth(currentImage.getWidth());
            imageView.setFitHeight(currentImage.getHeight());
        }
    }

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
                        "Ctrl+Y - 重做\n" +
                        "Ctrl+T - 切换主题\n" +
                        "Ctrl+Shift+T - 打开主题选择器"
        );
        alert.showAndWait();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "png";
    }

    @FunctionalInterface
    interface SliderChangeListener {
        void onChange(double value);
    }

    public static void main(String[] args) {
        launch(args);
    }
}