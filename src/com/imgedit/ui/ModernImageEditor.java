package imgedit.ui;

import imgedit.ui.FXAnimations;
import imgedit.model.ImageEditRequest;
import imgedit.model.enums.OperationType;
import imgedit.service.ImageEditorService;
import imgedit.utils.ImageUtils;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 现代化UI主界面 - 集成AI对话、图生图、图片编辑功能
 * 采用现代化设计风格，支持主题切换和动画效果
 */
public class ModernImageEditor extends Application {

    private ImageEditorService imageEditorService;
    private BufferedImage currentBufferedImage;

    // 一个列表来记录操作历史
    private List<String> operationHistory = new ArrayList<>();

    // 配置
    private static Properties config;
    
    // UI组件
    private ImageView currentImageView;
    private TextArea chatTextArea;
    private TextField chatInputField;
    private ProgressIndicator progressIndicator;
    private ToggleButton themeToggle;
    
    // 状态
    private boolean darkMode = false;
    private File currentImageFile;
    private Image currentImage;
    private Scene mainScene;
    private Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        // 显示启动画面
        Stage splashStage = showSplashScreen();  // 修改为返回启动窗口引用
        
        // 在后台加载资源
        new Thread(() -> {
            try {
                // 模拟加载过程
                Thread.sleep(1500);
                
                // 加载配置
                loadConfig();
                
                // 切换到主界面
                javafx.application.Platform.runLater(() -> {
                    showMainWindow();
                });
            } catch (InterruptedException e) {
                Logger logger = Logger.getLogger(ModernImageEditor.class.getName());
                logger.log(Level.SEVERE, "加载失败", e);
            }
        }).start();
    }

    /**
     * 显示启动画面
     */
    private Stage showSplashScreen() {
        Stage splashStage = new Stage(StageStyle.UNDECORATED);

        VBox splashLayout = new VBox(20);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-background-color: #2196F3; -fx-padding: 40;");
        splashLayout.getStyleClass().add("fade-in");

        // 应用图标
        Label logo = new Label("🤖");
        logo.setStyle("-fx-font-size: 48px;");
        logo.getStyleClass().add("spin-slow");

        Label title = new Label("AI智能图像编辑器");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        title.getStyleClass().add("fade-in-delay-1");

        Label subtitle = new Label("正在加载...");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        subtitle.getStyleClass().add("fade-in-delay-2");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.getStyleClass().add("fade-in-delay-3");
        progressBar.setProgress(-1); // 无限进度

        splashLayout.getChildren().addAll(logo, title, subtitle, progressBar);

        Scene splashScene = new Scene(splashLayout, 400, 300);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(splashStage::close);
            } catch (InterruptedException e) {
                Logger logger = Logger.getLogger(ModernImageEditor.class.getName());
                logger.log(Level.SEVERE, "加载失败", e);
            }
        }).start();

        return splashStage;  // 返回启动窗口
        // 保存引用，稍后关

    }
    
    /**
     * 显示主窗口
     */
    private void showMainWindow() {
        // 初始化图片编辑服务
        try {
            imageEditorService = new ImageEditorService();
            System.out.println("ImageEditorService 初始化成功");
        } catch (Exception e) {
            System.err.println("ImageEditorService 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 创建主容器
        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("root", "fade-in");

        root.getStyleClass().add("show");  // 关键修复：触发淡入动画的完成状态

        // 创建顶部工具栏
        root.setTop(createTopToolbar());
        
        // 创建中心区域
        root.setCenter(createMainContent());
        
        // 创建底部状态栏
        root.setBottom(createStatusBar());
        
        // 应用CSS样式
        mainScene = new Scene(root, 1400, 900);
        loadStyles();
        
        // 设置窗口
        primaryStage.setTitle("AI智能图像编辑器 v2.0");
        primaryStage.setScene(mainScene);
        primaryStage.show();
        
        // 显示欢迎消息
        showWelcomeMessage();


    }
    
    /**
     * 加载样式表
     */
    private void loadStyles() {
        try {
            // 先清除所有样式
            mainScene.getStylesheets().clear();

            // 确保路径正确
            String basePath = "/imgedit/resources/styles/";

            // 加载主样式表
            URL mainCssUrl = getClass().getResource(basePath + "main.css");
            if (mainCssUrl != null) {
                mainScene.getStylesheets().add(mainCssUrl.toExternalForm());
                System.out.println("成功加载主样式表");
            } else {
                System.err.println("未找到main.css，使用后备样式");
                applyInlineStyles();
            }

            // 根据主题加载深色样式
            if (darkMode) {
                URL darkCssUrl = getClass().getResource(basePath + "dark.css");
                if (darkCssUrl != null) {
                    mainScene.getStylesheets().add(darkCssUrl.toExternalForm());
                    System.out.println("应用深色主题");
                }
            }
        } catch (Exception e) {
            System.err.println("加载样式表失败: " + e.getMessage());
            e.printStackTrace();
            applyInlineStyles(); // 使用后备样式
        }
    }
    /**
     * 应用内联样式作为备份
     */
    private void applyInlineStyles() {
        if (mainScene == null) return;

        // 应用基本的内联样式确保所有控件可见
        String inlineCss =
                ".root { -fx-background-color: #f0f0f0; }" +
                        ".toolbar { -fx-background-color: #ffffff; -fx-padding: 10; -fx-border-color: #cccccc; }" +
                        ".button { -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 15; }" +
                        ".toggle-button { -fx-background-color: #e0e0e0; -fx-padding: 8 15; }" +
                        ".label { -fx-text-fill: #333333; }" +
                        ".text-area, .text-field { -fx-background-color: white; -fx-border-color: #cccccc; }" +
                        ".tab-pane { -fx-background-color: white; }" +
                        ".tab { -fx-background-color: #e0e0e0; -fx-padding: 5 10; }" +
                        ".tab:selected { -fx-background-color: #4CAF50; -fx-text-fill: white; }" +
                        ".image-view { -fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: white; }";

        mainScene.getRoot().setStyle(inlineCss);
    }

    /**
     * 应用最小样式
     */
    private void applyMinimalStyles() {
        BorderPane root = (BorderPane) mainScene.getRoot();
        if (root != null) {
            root.setStyle("-fx-background-color: #f0f0f0;");
        }
    }

    /**
     * 切换主题
     */
    private void toggleTheme() {
        darkMode = !darkMode;
        
        // 更新配置
        if (config != null) {
            config.setProperty("app.theme", darkMode ? "dark" : "light");
        }
        
        // 重新加载样式
        loadStyles();
        
        // 更新主题按钮状态
        if (themeToggle != null) {
            themeToggle.setSelected(darkMode);
            themeToggle.setText(darkMode ? "☀️" : "🌙");
            themeToggle.setTooltip(new Tooltip(darkMode ? "切换到亮色主题" : "切换到深色主题"));
        }
        
        // 保存配置
        saveConfig();
        
        // 添加主题切换动画
        if (primaryStage != null) {
            primaryStage.getScene().getRoot().getStyleClass().add("flip");
            PauseTransition flipTransition = new PauseTransition(Duration.millis(500));
            flipTransition.setOnFinished(e -> primaryStage.getScene().getRoot().getStyleClass().remove("flip"));
            flipTransition.play();
        }
    }
    
    /**
     * 检查是否为深色主题
     */
    private boolean isDarkTheme() {
        if (config != null) {
            return "dark".equalsIgnoreCase(config.getProperty("app.theme", "light"));
        }
        return false;
    }
    
    /**
     * 创建顶部工具栏
     */
    private HBox createTopToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(15));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().addAll("toolbar", "slide-in-top");
        // 使用JavaFX动画
        FXAnimations.slideInFromLeft(toolbar, Duration.millis(500));


        // Logo
        Label logo = new Label("🤖 AI图像编辑器");
        logo.getStyleClass().addAll("logo", "gradient-text", "fade-in-delay-1");
        
        // 分隔线
        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        separator.getStyleClass().add("fade-in-delay-2");
        
        // 文件按钮
        Button openBtn = createIconButton("📁", "打开图片", this::openImage);
        openBtn.getStyleClass().add("fade-in-delay-2");
        
        Button saveBtn = createIconButton("💾", "保存图片", this::saveImage);
        saveBtn.getStyleClass().add("fade-in-delay-2");
        
        // AI功能按钮
        Button aiChatBtn = createIconButton("💬", "AI对话", this::openAIChat);
        aiChatBtn.getStyleClass().add("fade-in-delay-3");
        
        Button aiImageBtn = createIconButton("🎨", "AI图生图", this::openAIImageGen);
        aiImageBtn.getStyleClass().add("fade-in-delay-3");
        
        Button enhanceBtn = createIconButton("✨", "AI增强", this::enhanceImage);
        enhanceBtn.getStyleClass().addAll("fade-in-delay-3", "pulse");
        
        // 编辑功能按钮组
        ToggleButton cropBtn = createToggleButton("✂️", "裁剪");
        cropBtn.getStyleClass().add("fade-in-delay-4");
//        ToggleButton cropBtn = createToggleButton("✂️", "裁剪");
//        cropBtn.setOnAction(e -> handleCropOperation());

        MenuButton rotateMenuBtn = new MenuButton("🔄 旋转");
        rotateMenuBtn.getStyleClass().addAll("icon-button", "fade-in-delay-4");

        // 添加菜单项
        MenuItem rotate90 = new MenuItem("旋转90度");
        rotate90.setOnAction(e -> rotate90());

        MenuItem rotate180 = new MenuItem("旋转180度");
        rotate180.setOnAction(e -> rotate180());

        MenuItem rotate270 = new MenuItem("旋转270度");
        rotate270.setOnAction(e -> rotate270());

        rotateMenuBtn.getItems().addAll(rotate90, rotate180, rotate270);

        ToggleButton filterBtn = createToggleButton("🎨", "滤镜");
        filterBtn.getStyleClass().add("fade-in-delay-4");
        
        // 工具按钮组
        Button undoBtn = createIconButton("↩️", "撤销", this::undo);
        undoBtn.getStyleClass().add("fade-in-delay-5");
        
        Button redoBtn = createIconButton("↪️", "重做", this::redo);
        redoBtn.getStyleClass().add("fade-in-delay-5");
        
        Button resetBtn = createIconButton("🔄", "重置", this::resetImage);
        resetBtn.getStyleClass().add("fade-in-delay-5");
        
        // 主题切换
        themeToggle = createToggleButton("🌙", "切换主题");
        themeToggle.setSelected(isDarkTheme());
        themeToggle.setOnAction(e -> toggleTheme());
        themeToggle.getStyleClass().addAll("fade-in-delay-6", "breath");
        
        // 帮助按钮
        Button helpBtn = createIconButton("❓", "帮助", this::showHelp);
        helpBtn.getStyleClass().add("fade-in-delay-6");
        
        // 组合工具栏
        toolbar.getChildren().addAll(
            logo, separator,
            openBtn, saveBtn, new Separator(),
            aiChatBtn, aiImageBtn, enhanceBtn, new Separator(),
            cropBtn, filterBtn, new Separator(),
            undoBtn, redoBtn, resetBtn, new Separator(),
            themeToggle, helpBtn
        );
        
        return toolbar;
    }
    
    /**
     * 创建主内容区域
     */
    private TabPane createMainContent() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().addAll("main-tabs", "fade-in-delay-1");

        // 图片编辑标签页
        Tab editTab = new Tab("图片编辑");
        editTab.setContent(createImageEditPane());
        editTab.setGraphic(new Label("🖼️"));

        // AI对话标签页
        Tab chatTab = new Tab("AI对话");
        chatTab.setContent(createChatPane());
        chatTab.setGraphic(new Label("💬"));

        // 图生图标签页
        Tab genTab = new Tab("图生图");
        genTab.setContent(createImageGenPane());
        genTab.setGraphic(new Label("🎨"));

        // 批量处理标签页
        Tab batchTab = new Tab("批量处理");
        batchTab.setContent(createBatchPane());
        batchTab.setGraphic(new Label("📚"));

        tabPane.getTabs().addAll(editTab, chatTab, genTab, batchTab);
        return tabPane;
    }
    
    /**
     * 创建图片编辑面板
     */
    private BorderPane createImageEditPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));
        
        // 左侧工具栏 - 添加动画效果
        VBox leftToolbar = new VBox(15);
        leftToolbar.setPadding(new Insets(20));
        leftToolbar.setPrefWidth(250);
        leftToolbar.getStyleClass().addAll("side-toolbar", "fade-in", "slide-in-left");
        
        // 基本调整 - 添加动画延迟
        Label basicLabel = new Label("基本调整");
        basicLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        VBox brightnessSlider = createSlider("亮度", -100, 100, 0);
        brightnessSlider.getStyleClass().add("fade-in-delay-2");
        
        VBox contrastSlider = createSlider("对比度", -100, 100, 0);
        contrastSlider.getStyleClass().add("fade-in-delay-3");
        
        VBox saturationSlider = createSlider("饱和度", -100, 100, 0);
        saturationSlider.getStyleClass().add("fade-in-delay-4");
        
        // 高级滤镜 - 添加动画效果
        Label filterLabel = new Label("高级滤镜");
        filterLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        // 模糊效果
        Label blurLabel = new Label("模糊效果");
        blurLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");

        VBox blurSlider = createSlider("模糊", 0, 10, 0);
        blurSlider.getStyleClass().add("fade-in-delay-2");


        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("怀旧", "黑白", "素描", "油画", "水彩", "卡通");
        filterCombo.setPromptText("选择滤镜");
        filterCombo.getStyleClass().addAll("fade-in-delay-2", "hover-scale");
        
        // AI功能 - 添加动画效果
        Label aiLabel = new Label("AI功能");
        aiLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        Button enhanceBtn = new Button("✨ 一键增强");
        enhanceBtn.getStyleClass().addAll("ai-button", "fade-in-delay-2", "hover-scale", "pulse");
        enhanceBtn.setOnAction(e -> enhanceImage());
        
        Button removeBgBtn = new Button("🔲 移除背景");
        removeBgBtn.getStyleClass().addAll("ai-button", "fade-in-delay-3", "hover-scale");
        removeBgBtn.setOnAction(e -> removeBackground());
        
        Button styleBtn = new Button("🎭 艺术风格");
        styleBtn.getStyleClass().addAll("ai-button", "fade-in-delay-4", "hover-scale");
        styleBtn.setOnAction(e -> applyArtisticStyle());
        
        leftToolbar.getChildren().addAll(
            basicLabel, brightnessSlider, contrastSlider, saturationSlider,
            new Separator(),
            filterLabel, filterCombo,
            new Separator(),
            aiLabel, enhanceBtn, removeBgBtn, styleBtn,
            new Separator(),
                blurLabel, blurSlider
        );
        
        // 中心图片显示 - 添加动画效果
        VBox centerPane = new VBox(20);
        centerPane.setAlignment(Pos.CENTER);
        centerPane.setPadding(new Insets(20));
        centerPane.getStyleClass().add("fade-in");
        
        currentImageView = new ImageView();
        currentImageView.setPreserveRatio(true);
        currentImageView.setFitWidth(600);
        currentImageView.setFitHeight(400);

        // 设置默认占位符（可选）
        Rectangle placeholder = new Rectangle(600, 400, Color.LIGHTGRAY);
        placeholder.setArcWidth(10);
        placeholder.setArcHeight(10);
        placeholder.setStroke(Color.DARKGRAY);
        placeholder.setStrokeWidth(1);

        // 使用StackPane包装ImageView
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(600, 400);
        imageContainer.getStyleClass().add("image-view");
        imageContainer.getChildren().addAll(placeholder, currentImageView);

        // 图片控制按钮 - 添加动画效果
        HBox imageControls = new HBox(15);
        imageControls.setAlignment(Pos.CENTER);
        imageControls.getStyleClass().add("fade-in-delay-1");
        
        Button zoomInBtn = createIconButton("➕", "放大", this::zoomIn);
        Button zoomOutBtn = createIconButton("➖", "缩小", this::zoomOut);
        Button fitBtn = createIconButton("↔️", "适应窗口", this::fitToWindow);
        Button originalBtn = createIconButton("📏", "原始尺寸", this::resetZoom);
        
        imageControls.getChildren().addAll(zoomInBtn, zoomOutBtn, fitBtn, originalBtn);
        
        // 编辑历史 - 添加动画效果
        Label historyLabel = new Label("编辑历史");
        historyLabel.getStyleClass().addAll("section-label", "fade-in-delay-2");
        
        ListView<String> historyList = new ListView<>();
        historyList.setPrefHeight(150);
        historyList.getItems().addAll("打开图片", "调整亮度", "应用滤镜");
        historyList.getStyleClass().addAll("fade-in-delay-3", "hover-lift");
        
        centerPane.getChildren().addAll(
            currentImageView, 
            imageControls, 
            historyLabel, 
            historyList
        );
        
        pane.setLeft(leftToolbar);
        pane.setCenter(centerPane);
        
        return pane;
    }
    
    /**
     * 创建AI对话面板
     */
    private BorderPane createChatPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));
        
        // 聊天历史区域 - 添加动画效果
        VBox chatHistoryPane = new VBox(10);
        chatHistoryPane.setPrefWidth(300);
        chatHistoryPane.getStyleClass().addAll("card", "fade-in", "slide-in-left");
        
        Label historyLabel = new Label("聊天历史");
        historyLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        ListView<String> chatHistory = new ListView<>();
        chatHistory.setPrefHeight(600);
        chatHistory.getStyleClass().addAll("fade-in-delay-2", "hover-lift");
        
        Button newChatBtn = new Button("🆕 新对话");
        newChatBtn.setMaxWidth(Double.MAX_VALUE);
        newChatBtn.getStyleClass().addAll("secondary-button", "fade-in-delay-3", "hover-scale");
        
        chatHistoryPane.getChildren().addAll(historyLabel, chatHistory, newChatBtn);
        
        // 主聊天区域 - 添加动画效果
        VBox mainChatPane = new VBox(10);
        mainChatPane.getStyleClass().add("fade-in");
        
        chatTextArea = new TextArea();
        chatTextArea.setPrefHeight(500);
        chatTextArea.setEditable(false);
        chatTextArea.getStyleClass().addAll("chat-area", "fade-in-delay-1");
        chatTextArea.setWrapText(true);
        
        // 输入区域 - 添加动画效果
        HBox inputPane = new HBox(10);
        inputPane.getStyleClass().add("fade-in-delay-2");
        
        chatInputField = new TextField();
        chatInputField.setPromptText("输入您的问题...");
        chatInputField.setPrefWidth(400);
        chatInputField.getStyleClass().add("hover-scale");
        
        Button sendBtn = new Button("发送");
        sendBtn.getStyleClass().addAll("primary-button", "hover-scale");
        sendBtn.setOnAction(e -> sendMessage());
        
        inputPane.getChildren().addAll(chatInputField, sendBtn);
        
        // AI模型选择 - 添加动画效果
        HBox modelPane = new HBox(10);
        modelPane.setAlignment(Pos.CENTER_LEFT);
        modelPane.getStyleClass().add("fade-in-delay-3");
        
        Label modelLabel = new Label("AI模型:");
        modelLabel.getStyleClass().add("fade-in-delay-3");
        
        ComboBox<String> modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll("DeepSeek", "GPT-4", "Claude", "本地模型");
        modelCombo.setValue("DeepSeek");
        modelCombo.getStyleClass().addAll("fade-in-delay-3", "hover-scale");
        
        VBox tempSlider = createSlider("创意度", 0, 100, 70);
        tempSlider.getStyleClass().add("fade-in-delay-4");
        
        VBox lengthSlider = createSlider("回答长度", 100, 2000, 1000);
        lengthSlider.getStyleClass().add("fade-in-delay-4");
        
        modelPane.getChildren().addAll(modelLabel, modelCombo, tempSlider, lengthSlider);
        
        mainChatPane.getChildren().addAll(chatTextArea, inputPane, modelPane);
        
        pane.setLeft(chatHistoryPane);
        pane.setCenter(mainChatPane);
        
        return pane;
    }
    
    /**
     * 创建图生图面板
     */
    private BorderPane createImageGenPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));
        
        // 左侧控制面板 - 添加动画效果
        VBox controlPane = new VBox(20);
        controlPane.setPrefWidth(400);
        controlPane.getStyleClass().addAll("card", "fade-in", "slide-in-left");
        
        // 模型选择 - 添加动画效果
        Label modelLabel = new Label("AI模型设置");
        modelLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        ComboBox<String> aiModelCombo = new ComboBox<>();
        aiModelCombo.getItems().addAll("豆包图生图", "Stable Diffusion", "DALL-E", "Midjourney");
        aiModelCombo.setValue("豆包图生图");
        aiModelCombo.getStyleClass().addAll("fade-in-delay-2", "hover-scale");
        
        // 图片上传 - 添加动画效果
        Label uploadLabel = new Label("上传原始图片");
        uploadLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        HBox uploadPane = new HBox(10);
        uploadPane.getStyleClass().add("fade-in-delay-2");
        
        Button uploadBtn = new Button("📁 选择图片");
        uploadBtn.getStyleClass().addAll("secondary-button", "hover-scale");
        uploadBtn.setOnAction(e -> uploadImageForGen());
        
        TextField imagePathField = new TextField();
        imagePathField.setPromptText("图片路径");
        imagePathField.getStyleClass().add("hover-scale");
        
        uploadPane.getChildren().addAll(uploadBtn, imagePathField);
        
        // 提示词输入 - 添加动画效果
        Label promptLabel = new Label("生成提示词");
        promptLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        TextArea promptArea = new TextArea();
        promptArea.setPrefHeight(150);
        promptArea.setPromptText("描述您想要生成的图像...");
        promptArea.getStyleClass().addAll("fade-in-delay-2", "hover-scale");
        
        // 参数设置 - 添加动画效果
        Label paramLabel = new Label("生成参数");
        paramLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        VBox qualitySlider = createSlider("质量", 0, 100, 85);
        qualitySlider.getStyleClass().add("fade-in-delay-2");
        
        VBox styleSlider = createSlider("创意度", 0, 100, 70);
        styleSlider.getStyleClass().add("fade-in-delay-3");
        
        ComboBox<String> sizeCombo = new ComboBox<>();
        sizeCombo.getItems().addAll("512x512", "768x768", "1024x1024", "2K", "4K");
        sizeCombo.setValue("1024x1024");
        sizeCombo.getStyleClass().addAll("fade-in-delay-4", "hover-scale");
        
        // 生成按钮 - 添加动画效果
        Button generateBtn = new Button("✨ 生成图片");
        generateBtn.getStyleClass().addAll("primary-button", "fade-in-delay-5", "hover-scale", "pulse");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> generateImage());
        
        controlPane.getChildren().addAll(
            modelLabel, aiModelCombo,
            new Separator(),
            uploadLabel, uploadPane,
            new Separator(),
            promptLabel, promptArea,
            new Separator(),
            paramLabel, qualitySlider, styleSlider, sizeCombo,
            new Separator(),
            generateBtn
        );
        
        // 右侧预览区域 - 添加动画效果
        VBox previewPane = new VBox(20);
        previewPane.setAlignment(Pos.CENTER);
        previewPane.getStyleClass().add("fade-in");
        
        ImageView genImageView = new ImageView();
        genImageView.setFitWidth(500);
        genImageView.setFitHeight(500);
        genImageView.setPreserveRatio(true);
        genImageView.getStyleClass().addAll("gen-image-view", "hover-scale");
        
        // 生成历史 - 添加动画效果
        Label genHistoryLabel = new Label("生成历史");
        genHistoryLabel.getStyleClass().addAll("section-label", "fade-in-delay-1");
        
        FlowPane historyPane = new FlowPane();
        historyPane.setHgap(10);
        historyPane.setVgap(10);
        historyPane.getStyleClass().add("fade-in-delay-2");
        
        // 添加一些示例历史图片
        for (int i = 0; i < 6; i++) {
            Pane thumb = new Pane();
            thumb.setPrefSize(100, 100);
            thumb.getStyleClass().addAll("thumb-pane", "hover-scale");
            thumb.getStyleClass().add("fade-in-delay-" + (i + 3));
            historyPane.getChildren().add(thumb);
        }
        
        previewPane.getChildren().addAll(genImageView, genHistoryLabel, historyPane);
        
        pane.setLeft(controlPane);
        pane.setCenter(previewPane);
        
        return pane;
    }
    
    /**
     * 创建批量处理面板
     */
    private ScrollPane createBatchPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("fade-in");
        
        Label titleLabel = new Label("批量图片处理");
        titleLabel.getStyleClass().addAll("title-label", "fade-in-delay-1");
        
        // 文件选择区域 - 添加动画效果
        Label selectLabel = new Label("选择图片文件夹");
        selectLabel.getStyleClass().addAll("section-label", "fade-in-delay-2");
        
        HBox selectPane = new HBox(10);
        selectPane.getStyleClass().add("fade-in-delay-3");
        
        TextField folderField = new TextField();
        folderField.setPromptText("文件夹路径");
        folderField.setPrefWidth(400);
        folderField.getStyleClass().add("hover-scale");
        
        Button browseBtn = new Button("📁 浏览");
        browseBtn.getStyleClass().addAll("secondary-button", "hover-scale");
        
        Button scanBtn = new Button("🔍 扫描图片");
        scanBtn.getStyleClass().addAll("secondary-button", "hover-scale");
        
        selectPane.getChildren().addAll(folderField, browseBtn, scanBtn);
        
        // 文件列表 - 添加动画效果
        Label listLabel = new Label("图片列表");
        listLabel.getStyleClass().addAll("section-label", "fade-in-delay-4");
        
        TableView<File> fileTable = new TableView<>();
        TableColumn<File, String> nameCol = new TableColumn<>("文件名");
        TableColumn<File, String> sizeCol = new TableColumn<>("大小");
        TableColumn<File, String> statusCol = new TableColumn<>("状态");
        fileTable.getColumns().addAll(nameCol, sizeCol, statusCol);
        fileTable.setPrefHeight(300);
        fileTable.getStyleClass().addAll("fade-in-delay-5", "hover-lift");
        
        // 批量操作设置 - 添加动画效果
        Label opsLabel = new Label("批量操作设置");
        opsLabel.getStyleClass().addAll("section-label", "fade-in-delay-6");
        
        HBox opsPane = new HBox(10);
        opsPane.getStyleClass().add("fade-in-delay-7");
        
        ComboBox<String> operationCombo = new ComboBox<>();
        operationCombo.getItems().addAll("调整大小", "格式转换", "添加水印", "批量滤镜", "AI增强");
        operationCombo.setValue("调整大小");
        operationCombo.getStyleClass().add("hover-scale");
        
        Button addOpBtn = new Button("➕ 添加操作");
        addOpBtn.getStyleClass().addAll("secondary-button", "hover-scale");
        
        Button clearOpsBtn = new Button("🗑️ 清空操作");
        clearOpsBtn.getStyleClass().addAll("secondary-button", "hover-scale");
        
        opsPane.getChildren().addAll(operationCombo, addOpBtn, clearOpsBtn);
        
        // 操作队列 - 添加动画效果
        ListView<String> opsList = new ListView<>();
        opsList.setPrefHeight(150);
        opsList.getStyleClass().addAll("fade-in-delay-8", "hover-lift");
        
        // 开始处理 - 添加动画效果
        HBox processPane = new HBox(10);
        processPane.getStyleClass().add("fade-in-delay-9");
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.getStyleClass().add("spin");
        
        Button startBtn = new Button("🚀 开始批量处理");
        startBtn.getStyleClass().addAll("primary-button", "hover-scale", "pulse");
        
        processPane.getChildren().addAll(progressIndicator, startBtn);
        
        pane.getChildren().addAll(
            titleLabel,
            selectLabel, selectPane,
            listLabel, fileTable,
            opsLabel, opsPane, opsList,
            processPane
        );
        
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("fade-in");
        return scrollPane;
    }
    
    /**
     * 创建状态栏
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10, 20, 10, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().addAll("status-bar", "slide-in-bottom");
        
        Label statusLabel = new Label("就绪");
        statusLabel.getStyleClass().add("fade-in-delay-1");
        
        Label imageInfoLabel = new Label("未加载图片");
        imageInfoLabel.getStyleClass().add("fade-in-delay-2");
        
        Label aiStatusLabel = new Label("AI服务: 可用");
        aiStatusLabel.getStyleClass().add("fade-in-delay-3");
        
        statusBar.getChildren().addAll(statusLabel, new Separator(), imageInfoLabel, new Separator(), aiStatusLabel);
        return statusBar;
    }
    
    /**
     * 创建图标按钮
     */
    private Button createIconButton(String icon, String tooltip, Runnable action) {
        Button button = new Button(icon);
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().addAll("icon-button", "hover-scale");
        button.setOnAction(e -> {
            action.run();
            // 添加点击动画
            button.getStyleClass().add("pulse");
            // 按钮点击动画
            PauseTransition btnPulseTransition = new PauseTransition(Duration.millis(300));
            btnPulseTransition.setOnFinished(event -> button.getStyleClass().remove("pulse"));
            btnPulseTransition.play();
        });
        return button;
    }
    
    /**
     * 创建切换按钮
     */
    private ToggleButton createToggleButton(String icon, String tooltip) {
        ToggleButton button = new ToggleButton(icon);
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().addAll("toggle-button", "hover-scale");
        return button;
    }
    
    /**
     * 创建滑块
     */
    // 绑定实际操作很重要
    private VBox createSlider(String label, double min, double max, double value) {
        VBox sliderBox = new VBox(5);
        sliderBox.getStyleClass().add("hover-scale");
        
        Label sliderLabel = new Label(label);
        sliderLabel.getStyleClass().add("label-light");
        
        Slider slider = new Slider(min, max, value);
        
        Label valueLabel = new Label(String.valueOf((int)value));
        valueLabel.getStyleClass().add("label");



        
        slider.valueProperty().addListener((obs, old, newVal) -> {
            valueLabel.setText(String.valueOf(newVal.intValue()));

                    // 根据标签调用不同的处理方法
                    switch (label) {
                        case "亮度":
                            adjustBrightness(newVal.doubleValue());
                            break;
                        case "对比度":
                            // 类似实现对比度调整
                            adjustContrast(newVal.doubleValue());
                            break;
                        case "模糊":
                            // 类似实现模糊调整
                            adjustBlur(newVal.doubleValue());

                            break;
                    }
            
            // 添加值变化动画
            valueLabel.getStyleClass().add("pulse");
            PauseTransition pulseTransition = new PauseTransition(Duration.millis(200));
            pulseTransition.setOnFinished(e -> valueLabel.getStyleClass().remove("pulse"));
            pulseTransition.play();
        });
        
        sliderBox.getChildren().addAll(sliderLabel, slider, valueLabel);
        return sliderBox;
    }

    private void adjustBrightness(double sliderValue) {
        imgedit.core.operations.BrightnessOperation.BrightnessMode mode;
        float intensity;

        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        if (sliderValue >= 0) {
            // 增加亮度
            mode = imgedit.core.operations.BrightnessOperation.BrightnessMode.INCREASE;
            intensity = (float)(sliderValue / 100.0); // 转换为0.0-1.0
        } else {
            // 降低亮度
            mode = imgedit.core.operations.BrightnessOperation.BrightnessMode.DECREASE;
            intensity = (float)(-sliderValue / 100.0); // 转换为0.0-1.0
        }

        // ★★★ 关键：不要使用ImageEditRequest，直接创建Operation对象 ★★★
        imgedit.core.operations.BrightnessOperation operation =
                new imgedit.core.operations.BrightnessOperation(mode, intensity);

        // 异步执行
        imageEditorService.applyOperationAsync(
                operation,
                resultImage -> {
                    currentImage = resultImage;
                    currentImageView.setImage(currentImage);
                    // 更新BufferedImage
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("亮度调整完成");
                },
                exception -> {
                    showError("操作失败", exception.getMessage());
                    updateStatus("操作失败: " + exception.getMessage());
                }
        );
    }
    private void rotate180() {
        if (currentImage == null || imageEditorService == null) return;

        imgedit.core.operations.RotateOperation operation =
                imgedit.core.operations.RotateOperation.create180Degree();

        imageEditorService.applyOperationAsync(
                operation,
                resultImage -> {
                    currentImage = resultImage;
                    currentImageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("旋转180度完成");
                },
                exception -> showError("旋转失败", exception.getMessage())
        );
    }

    private void rotate270() {
        if (currentImage == null || imageEditorService == null) return;

        imgedit.core.operations.RotateOperation operation =
                imgedit.core.operations.RotateOperation.create270Degree();

        imageEditorService.applyOperationAsync(
                operation,
                resultImage -> {
                    currentImage = resultImage;
                    currentImageView.setImage(currentImage);
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("旋转270度完成");
                },
                exception -> showError("旋转失败", exception.getMessage())
        );
    }

    private void rotate90() {
        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        if (imageEditorService == null) {
            showAlert("提示", "服务未初始化");
            return;
        }

        // ★★★ 直接使用RotateOperation的工厂方法 ★★★
        imgedit.core.operations.RotateOperation operation =
                imgedit.core.operations.RotateOperation.create90Degree();

        // 异步执行
        imageEditorService.applyOperationAsync(
                operation,
                resultImage -> {
                    currentImage = resultImage;
                    currentImageView.setImage(currentImage);
                    // 更新BufferedImage
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("旋转90度完成");

                    // 添加旋转动画
                    currentImageView.getStyleClass().add("rotate-90");
                    PauseTransition transition = new PauseTransition(Duration.millis(500));
                    transition.setOnFinished(e -> currentImageView.getStyleClass().remove("rotate-90"));
                    transition.play();
                },
                exception -> {
                    showError("旋转失败", exception.getMessage());
                    updateStatus("旋转失败: " + exception.getMessage());
                }
        );
    }

    // 对比度调整方法
    private void adjustContrast(double value) {
        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        if (imageEditorService == null) {
            showAlert("提示", "服务未初始化");
            return;
        }

        // 创建请求
        ImageEditRequest request = new ImageEditRequest(currentBufferedImage, OperationType.CONTRAST);
        // 转换值范围：假设滑块-100到100，对应对比度0.0到2.0
        float contrastValue = (float)(value / 100.0f + 1.0f);
        request.addParameter("contrast", contrastValue);

        // 异步处理
        imageEditorService.applyOperationAsync(
                imageEditorService.createOperationFromRequest(request),
                resultImage -> {
                    currentImage = resultImage;
                    currentImageView.setImage(currentImage);
                    // 更新BufferedImage
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("对比度调整完成");
                },
                exception -> {
                    showError("操作失败", exception.getMessage());
                    updateStatus("操作失败: " + exception.getMessage());
                }
        );
    }

    // 添加模糊方法
    private void adjustBlur(double sliderValue) {
        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        if (imageEditorService == null) {
            showAlert("提示", "服务未初始化");
            return;
        }

        // 根据滑块值选择模糊强度
        imgedit.core.operations.BlurOperation.BlurIntensity intensity;

        // 假设滑块0-10，映射到三种强度
        if (sliderValue <= 3) {
            intensity = imgedit.core.operations.BlurOperation.BlurIntensity.LIGHT;
        } else if (sliderValue <= 6) {
            intensity = imgedit.core.operations.BlurOperation.BlurIntensity.MEDIUM;
        } else {
            intensity = imgedit.core.operations.BlurOperation.BlurIntensity.STRONG;
        }

        // ★★★ 直接创建BlurOperation ★★★
        imgedit.core.operations.BlurOperation operation =
                new imgedit.core.operations.BlurOperation(intensity);

        // 异步执行
        imageEditorService.applyOperationAsync(
                operation,
                resultImage -> {
                    currentImage = resultImage;
                    currentImageView.setImage(currentImage);
                    // 更新BufferedImage
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("模糊效果应用完成");
                },
                exception -> {
                    showError("操作失败", exception.getMessage());
                    updateStatus("操作失败: " + exception.getMessage());
                }
        );
    }

    // ========== 业务逻辑方法 ==========
    
    private void loadConfig() {
        config = new Properties();
        try {
            // 从外部文件加载
            try (FileInputStream input = new FileInputStream("config.properties")) {
                config.load(input);
                System.out.println("从外部文件加载配置");
            } catch (Exception e) {
                // 从资源文件加载
                try (java.io.InputStream input = getClass().getResourceAsStream("/config.properties")) {
                    if (input != null) {
                        config.load(input);
                        System.out.println("从资源文件加载配置");
                    }
                }
            }
            
            // 读取主题设置
            darkMode = "dark".equalsIgnoreCase(config.getProperty("app.theme", "light"));
            
        } catch (Exception e) {
            System.err.println("加载配置失败: " + e.getMessage());
            config = new Properties();
        }
    }
    
    private void saveConfig() {
        try (java.io.FileOutputStream output = new java.io.FileOutputStream("config.properties")) {
            config.store(output, "AI Image Editor Configuration");
            System.out.println("配置已保存");
        } catch (Exception e) {
            System.err.println("保存配置失败: " + e.getMessage());
        }
    }

    private void openImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                // 显示加载状态
                updateStatus("正在加载图片...");

                // 简化加载逻辑
                Image image = new Image(file.toURI().toString());

                if (image.isError()) {
                    throw new RuntimeException("图片加载失败");
                }

                // 更新UI组件
                currentImageFile = file;
                currentImage = image;
                currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);

                // 在UI线程中更新ImageView
                Platform.runLater(() -> {
                    currentImageView.setImage(currentImage);

                    // 添加加载动画
                    ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), currentImageView);
                    scaleTransition.setFromX(0.8);
                    scaleTransition.setFromY(0.8);
                    scaleTransition.setToX(1.0);
                    scaleTransition.setToY(1.0);
                    scaleTransition.play();

                    updateStatus("已加载图片: " + file.getName());

                    try {
                        if (imageEditorService == null) {
                            imageEditorService = new ImageEditorService();
                        }

                        // 初始化图片处理器（传递BufferedImage）
                        if (currentBufferedImage != null) {
                            imageEditorService.initImageProcessor(currentBufferedImage);
                            System.out.println("图片处理器初始化成功");
                        } else {
                            System.err.println("currentBufferedImage为null，无法初始化处理器");
                        }
                    } catch (Exception e) {
                        System.err.println("初始化ImageEditorService失败: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // 更新图片信息显示
                    updateImageInfo();
                });

            } catch (Exception e) {
                showError("图片加载失败", e.getMessage());
                e.printStackTrace();
            }

        }
    }
    
    private void saveImage() {
        if (currentImage == null) {
            showAlert("提示", "没有图片可保存");
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
            showLoadingAnimation("正在保存图片...", () -> {
                try {
                    // 获取当前处理后的BufferedImage
                    BufferedImage bufferedImage = imageEditorService.getImageProcessor().getCurrentImage();
                    if (bufferedImage != null) {
                        // 根据文件扩展名选择保存格式
                        String fileName = file.getName().toLowerCase();
                        String format = "png"; // 默认
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                            format = "jpg";
                        } else if (fileName.endsWith(".bmp")) {
                            format = "bmp";
                        }

                        // 保存图片
                        javax.imageio.ImageIO.write(bufferedImage, format, file);

                        showAlert("保存成功", "图片已保存到: " + file.getAbsolutePath());
                        updateStatus("图片已保存");
                    } else {
                        showError("保存失败", "无法获取当前图片");
                    }
                } catch (Exception e) {
                    showError("保存失败", e.getMessage());
                }
            });
        }
    }
    
    private void openAIChat() {
        TabPane tabPane = (TabPane) ((BorderPane) mainScene.getRoot()).getCenter();
        tabPane.getSelectionModel().select(1); // 切换到聊天标签页
        updateStatus("打开AI对话界面");
        
        // 添加切换动画
        tabPane.getStyleClass().add("flip");
        // 标签页切换动画
        PauseTransition chatTabTransition = new PauseTransition(Duration.millis(500));
        chatTabTransition.setOnFinished(e -> tabPane.getStyleClass().remove("flip"));
        chatTabTransition.play();
    }
    
    private void openAIImageGen() {
        TabPane tabPane = (TabPane) ((BorderPane) mainScene.getRoot()).getCenter();
        tabPane.getSelectionModel().select(2); // 切换到图生图标签页
        updateStatus("打开AI图生图界面");
        
        // 添加切换动画
        tabPane.getStyleClass().add("flip");
        // 标签页切换动画
        PauseTransition genTabTransition = new PauseTransition(Duration.millis(500));
        genTabTransition.setOnFinished(e -> tabPane.getStyleClass().remove("flip"));
        genTabTransition.play();
    }
    
    private void enhanceImage() {
        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        
        updateStatus("正在使用AI增强图片...");
        
        // 显示加载动画
        showLoadingAnimation("AI正在增强图片...", () -> {
            progressIndicator.setVisible(false);
            
            // 显示成功动画
            Label successLabel = new Label("✨ 增强完成！");
            successLabel.getStyleClass().addAll("fade-in", "pulse");
            successLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50;");
            
            showAlert("AI增强完成", "图片已成功增强！");
            updateStatus("AI增强完成");
        });
    }
    
    private void removeBackground() {
        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        
        showLoadingAnimation("正在移除背景...", () -> {
            showAlert("背景移除完成", "背景已成功移除！");
            updateStatus("背景移除完成");
        });
    }
    
    private void applyArtisticStyle() {
        if (currentImage == null) {
            showAlert("提示", "请先加载图片");
            return;
        }
        
        showLoadingAnimation("正在应用艺术风格...", () -> {
            showAlert("艺术风格应用完成", "艺术风格已成功应用！");
            updateStatus("艺术风格应用完成");
        });
    }
    
    private void sendMessage() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            // 添加用户消息动画
            HBox userBubble = new HBox();
            Label userText = new Label("你: " + message);
            userText.getStyleClass().addAll("chat-bubble-user", "fade-in", "slide-in-right");
            userBubble.getChildren().add(userText);
            userBubble.setAlignment(Pos.CENTER_RIGHT);
            
            // 添加到聊天区域（这里简化处理）
            chatTextArea.appendText("你: " + message + "\n\n");
            chatInputField.clear();
            
            // 添加发送动画
            chatInputField.getStyleClass().add("pulse");
            // 发送动画
            PauseTransition sendAnimTransition = new PauseTransition(Duration.millis(300));
            sendAnimTransition.setOnFinished(e -> chatInputField.getStyleClass().remove("pulse"));
            sendAnimTransition.play();
            
            // 模拟AI回复
            showLoadingAnimation("AI正在思考...", () -> {
                String reply = "AI: 这是一个模拟回复。实际应该调用您的DeepSeek API。\n\n";
                chatTextArea.appendText(reply);
                updateStatus("AI回复完成");
            });
        }
    }
    
    private void generateImage() {
        showLoadingAnimation("正在生成图片...", () -> {
            showAlert("生成成功", "AI图片已生成！");
            updateStatus("图片生成完成");
        });
    }
    
    private void uploadImageForGen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择原始图片");
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            updateStatus("已选择原始图片: " + file.getName());
            
            // 添加上传成功动画
            Label successLabel = new Label("✅ 上传成功");
            successLabel.getStyleClass().addAll("fade-in", "pulse");
            successLabel.setStyle("-fx-font-size: 12px;");
            
            // 临时显示
            // 上传成功动画（临时显示）
            PauseTransition uploadSuccessTransition = new PauseTransition(Duration.seconds(2));
            uploadSuccessTransition.setOnFinished(e -> successLabel.setVisible(false));
            uploadSuccessTransition.play();
        }
    }
    
    private void zoomIn() {
        if (currentImageView != null && currentImage != null) {
            currentImageView.setFitWidth(currentImageView.getFitWidth() * 1.2);
            currentImageView.setFitHeight(currentImageView.getFitHeight() * 1.2);
            currentImageView.getStyleClass().add("zoom-in");
            // 放大动画
            PauseTransition zoomInAnimTransition = new PauseTransition(Duration.millis(300));
            zoomInAnimTransition.setOnFinished(e -> currentImageView.getStyleClass().remove("zoom-in"));
            zoomInAnimTransition.play();
        }
    }
    
    private void zoomOut() {
        if (currentImageView != null && currentImage != null) {
            currentImageView.setFitWidth(currentImageView.getFitWidth() * 0.8);
            currentImageView.setFitHeight(currentImageView.getFitHeight() * 0.8);
            currentImageView.getStyleClass().add("zoom-in");
            // 放大动画
            PauseTransition zoomInTransition = new PauseTransition(Duration.millis(300));
            zoomInTransition.setOnFinished(e -> currentImageView.getStyleClass().remove("zoom-in"));
            zoomInTransition.play();
        }
    }
    
    private void fitToWindow() {
        // 适应窗口逻辑
        updateStatus("图片已适应窗口");
    }
    
    private void resetZoom() {
        if (currentImage != null) {
            currentImageView.setFitWidth(currentImage.getWidth());
            currentImageView.setFitHeight(currentImage.getHeight());
            currentImageView.getStyleClass().add("zoom-in");
            // 缩小动画
            PauseTransition zoomOutTransition = new PauseTransition(Duration.millis(300));
            zoomOutTransition.setOnFinished(e -> currentImageView.getStyleClass().remove("zoom-in"));
            zoomOutTransition.play();
            updateStatus("图片已重置为原始尺寸");
        }
    }
    
    private void undo() {
        if (imageEditorService != null && imageEditorService.canUndo()) {
            try {
                Image result = imageEditorService.undo();
                if (result != null) {
                    currentImage = result;
                    currentImageView.setImage(currentImage);
                    // 更新BufferedImage引用
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("撤销完成");
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
                    currentImageView.setImage(currentImage);
                    // 更新BufferedImage引用
                    currentBufferedImage = ImageUtils.fxImageToBufferedImage(currentImage);
                    updateStatus("重做完成");
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
            try {
                currentImage = new Image(new FileInputStream(currentImageFile));
                currentImageView.setImage(currentImage);
                updateStatus("图片已重置");
                
                // 添加重置动画
                currentImageView.getStyleClass().add("flip");
                // 图片重置动画
                PauseTransition resetImageTransition = new PauseTransition(Duration.millis(500));
                resetImageTransition.setOnFinished(e -> currentImageView.getStyleClass().remove("flip"));
                resetImageTransition.play();
            } catch (Exception e) {
                showError("重置失败", e.getMessage());
            }
        }
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("帮助");
        alert.setHeaderText("AI智能图像编辑器 - 使用帮助");
        alert.setContentText(
            "功能说明：\n" +
            "1. 图片编辑：提供裁剪、旋转、滤镜等基本编辑功能\n" +
            "2. AI对话：与AI进行文字交流，获取图像处理建议\n" +
            "3. 图生图：使用AI生成或修改图片\n" +
            "4. 批量处理：一次性处理多张图片\n\n" +
            "提示：\n" +
            "- 请先加载图片再进行编辑操作\n" +
            "- AI功能需要网络连接\n" +
            "- 支持多种图片格式"
        );
        alert.showAndWait();
        updateStatus("查看帮助文档");
    }
    
    private void showWelcomeMessage() {
        updateStatus("欢迎使用AI智能图像编辑器！");
        
        // 显示欢迎弹窗
        Stage welcomeStage = new Stage();
        welcomeStage.initStyle(StageStyle.UTILITY);
        welcomeStage.setTitle("欢迎");
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.getStyleClass().addAll("card", "fade-in");
        
        Label icon = new Label("✨");
        icon.getStyleClass().addAll("fade-in", "spin-slow");
        icon.setStyle("-fx-font-size: 48px;");
        
        Label welcomeLabel = new Label("欢迎使用AI智能图像编辑器！");
        welcomeLabel.getStyleClass().addAll("title-label", "fade-in-delay-1", "slide-in-top");
        
        Label subtitle = new Label("开始您的创意之旅吧！");
        subtitle.getStyleClass().addAll("label-light", "fade-in-delay-2");
        
        content.getChildren().addAll(icon, welcomeLabel, subtitle);
        
        Scene scene = new Scene(content, 400, 200);
        scene.getStylesheets().addAll(
            getClass().getResource("/styles/main.css").toExternalForm()
            // getClass().getResource("/styles/animations.css").toExternalForm()
        );
        
        welcomeStage.setScene(scene);
        welcomeStage.show();

        // 欢迎窗口自动关闭动画
        PauseTransition welcomeCloseTransition = new PauseTransition(Duration.seconds(3));
        welcomeCloseTransition.setOnFinished(e -> welcomeStage.close());
        welcomeCloseTransition.play();
    }
    
    /**
     * 显示加载动画
     */
    private void showLoadingAnimation(String message, Runnable onComplete) {
        StackPane loadingPane = new StackPane();
        loadingPane.setAlignment(Pos.CENTER);
        loadingPane.getStyleClass().add("loading-overlay");
        loadingPane.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");
        
        VBox loadingContent = new VBox(20);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.getStyleClass().add("fade-in");
        
        // 旋转动画
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("spin");
        
        Label loadingText = new Label(message);
        loadingText.getStyleClass().addAll("fade-in-delay-1", "breath");
        
        loadingContent.getChildren().addAll(spinner, loadingText);
        loadingPane.getChildren().add(loadingContent);
        
        // 添加到场景
        BorderPane root = (BorderPane) mainScene.getRoot();
        StackPane originalCenter = new StackPane(root.getCenter());
        root.setCenter(loadingPane);
        
        // 模拟加载过程
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 模拟加载时间
                
                javafx.application.Platform.runLater(() -> {
                    root.setCenter(originalCenter);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            } catch (InterruptedException e) {
                Logger logger = Logger.getLogger(ModernImageEditor.class.getName());
                logger.log(Level.SEVERE, "加载失败", e);
            }
        }).start();
    }
    
    private void updateStatus(String message) {
        // 更新状态栏（这里简化处理）
        System.out.println("状态: " + message);
    }
    private void updateImageInfo() {
        if (currentImage != null) {
            // 更新状态栏图片信息
            BorderPane root = (BorderPane) mainScene.getRoot();
            HBox statusBar = (HBox) root.getBottom();

            // 更新图片信息标签
            for (Node node : statusBar.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    if (label.getText().contains("未加载图片")) {
                        label.setText(String.format("图片: %dx%d | %.1fMB",
                                (int)currentImage.getWidth(),
                                (int)currentImage.getHeight(),
                                currentImageFile.length() / (1024.0 * 1024.0)));
                        break;
                    }
                }
            }
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
