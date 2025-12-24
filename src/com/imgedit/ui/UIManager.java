package imgedit.ui;

import imgedit.core.operations.*;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.paint.ImagePattern;
import java.io.File;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;

/**
 * UI组件创建和管理器
 */
public class UIManager {

    private final EditorController controller;
    private VBox toastContainer;
    private StackPane loadingOverlay;
    private Label loadingText;
    private ProgressIndicator progressIndicator;

    // 滑动条值缓存
    private double brightnessValue = 0.0;
    private double contrastValue = 0.0;
    private double saturationValue = 0.0;

    // 添加深色模式标志
    private boolean isDarkMode = true; // 默认深色模式

    public UIManager(EditorController controller) {
        this.controller = controller;
    }

    public BorderPane createRootLayout() {
        BorderPane root = new BorderPane();
        root.setTop(createTopBar());
        root.setLeft(createLeftPanel());
        root.setCenter(createCenterPanel());
        root.setRight(createRightPanel());
        root.setBottom(createBottomBar());

        // 设置初始为深色模式
        applyTheme(ThemeManager.Theme.DARK_MODE);

        return root;
    }

    public StackPane createRootContainer(BorderPane root) {
        StackPane container = new StackPane(root);

        // 初始化Toast容器
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.BOTTOM_CENTER);
        toastContainer.setPadding(new Insets(0, 0, 80, 0));
        toastContainer.setMouseTransparent(true);

        container.getChildren().add(toastContainer);
        return container;
    }

    public HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));

        // Logo区域
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("✨");
        logoIcon.getStyleClass().add("app-logo-icon");
        logoIcon.setStyle("-fx-font-size: 20px;");

        Label appTitle = new Label("Pro Image Editor");
        appTitle.getStyleClass().add("app-logo-text");
        appTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        logoBox.getChildren().addAll(logoIcon, appTitle);

        // 中间占位
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 右侧按钮 - 简化样式
        HBox rightActions = new HBox(15);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        // 功能按钮
        Button undoBtn = createTopBarIconButton("↩", "撤销");
        undoBtn.setOnAction(e -> controller.getImageManager().undo());
        undoBtn.setStyle("-fx-text-fill: #333333;"); // 强制深色字体

        Button redoBtn = createTopBarIconButton("↪", "重做");
        redoBtn.setOnAction(e -> controller.getImageManager().redo());
        redoBtn.setStyle("-fx-text-fill: #333333;"); // 强制深色字体

        Button openBtn = createTopBarIconButton("📂", "打开");
        openBtn.setOnAction(e -> controller.getImageManager().openImage());

        Button saveBtn = new Button("💾 保存");
        saveBtn.setTooltip(new Tooltip("保存图片"));
        saveBtn.getStyleClass().add("save-btn");
        saveBtn.setOnAction(e -> controller.getImageManager().saveImage());
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #4CAF50, #8BC34A); " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 8 15;");

        Button themeBtn = createTopBarIconButton("🌗", "主题");
        themeBtn.setOnAction(e -> controller.getDialogManager().showThemeSelector());

        Button helpBtn = createTopBarIconButton("?", "关于");
        helpBtn.setOnAction(e -> controller.getDialogManager().showHelp());

        // 应用图标按钮样式
        for (Button b : new Button[]{undoBtn, redoBtn, openBtn, themeBtn, helpBtn}) {
            b.setMinSize(40, 40);
            b.setMaxSize(40, 40);
        }

        rightActions.getChildren().addAll(
                undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                openBtn, saveBtn, new Separator(Orientation.VERTICAL),
                themeBtn, helpBtn
        );

        topBar.getChildren().addAll(logoBox, spacer, rightActions);
        return topBar;
    }

    private Button createTopBarIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-font-size: 18px; " +
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 8; " +
                "-fx-cursor: hand; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6;");

        // 悬停效果
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-font-size: 18px; " +
                    "-fx-background-color: rgba(255,255,255,0.1); " +
                    "-fx-border-color: rgba(255,255,255,0.2); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 8; " +
                    "-fx-cursor: hand; " +
                    "-fx-border-radius: 6; " +
                    "-fx-background-radius: 6;");
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-font-size: 18px; " +
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: transparent; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 8; " +
                    "-fx-cursor: hand; " +
                    "-fx-border-radius: 6; " +
                    "-fx-background-radius: 6;");
        });

        return btn;
    }

    public ScrollPane createLeftPanel() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);

        // 1. 基础调整卡片
        VBox adjustmentPanel = createAdvancedAdjustmentPanel();
        VBox basicCard = createCard("🎛 基础调整", adjustmentPanel);

        // 2. 交互工具卡片
        VBox toolsCard = createToolsCard();

        // 3. 变换与批量卡片
        VBox transCard = createTransformCard();

        // 4. 滤镜卡片
        VBox filterCard = createFilterCard();

        // 5. AI增强卡片
        VBox aiCard = createAICard();

        content.getChildren().addAll(basicCard, toolsCard, transCard, filterCard, aiCard);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private VBox createAdvancedAdjustmentPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Label title = new Label("🔧 基础调整");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 亮度调节滑块
        VBox brightnessControl = createAdvancedSlider("亮度", -50, 50, brightnessValue, (value) -> {
            brightnessValue = value;
            controller.updateStatus(String.format("亮度: %.0f", value));
        });

        // 对比度调节滑块
        VBox contrastControl = createAdvancedSlider("对比度", -50, 50, contrastValue, (value) -> {
            contrastValue = value;
            controller.updateStatus(String.format("对比度: %.0f", value));
        });

        // 饱和度调节滑块
        VBox saturationControl = createAdvancedSlider("饱和度", -50, 50, saturationValue, (value) -> {
            saturationValue = value;
            controller.updateStatus(String.format("饱和度: %.0f", value));
        });

        Separator separator = new Separator();

        // 应用所有调整按钮
        HBox buttonBox = createAdjustmentButtons();

        panel.getChildren().addAll(
                title, brightnessControl, contrastControl, saturationControl,
                separator, buttonBox
        );

        return panel;
    }

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

    private HBox createAdjustmentButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button applyBtn = new Button("应用调整");
        applyBtn.setOnAction(e -> applyAllAdjustments());

        Button resetBtn = new Button("重置");
        resetBtn.setOnAction(e -> resetAllAdjustments());

        buttonBox.getChildren().addAll(applyBtn, resetBtn);
        return buttonBox;
    }

    private VBox createToolsCard() {
        ToggleGroup toolGroup = new ToggleGroup();

        GridPane toolGrid = new GridPane();
        toolGrid.setHgap(10);
        toolGrid.setVgap(10);

        ToggleButton selectTool = createToolButton("👆 选择", ToolManager.ToolMode.SELECT, toolGroup);
        ToggleButton cropTool = createToolButton("✂️ 裁剪", ToolManager.ToolMode.CROP, toolGroup);
        ToggleButton brushTool = createToolButton("🖌️ 画笔", ToolManager.ToolMode.DRAW_BRUSH, toolGroup);
        ToggleButton textTool = createToolButton("A 文字", ToolManager.ToolMode.DRAW_TEXT, toolGroup);
        ToggleButton rectTool = createToolButton("⬜ 矩形", ToolManager.ToolMode.DRAW_RECT, toolGroup);
        ToggleButton circleTool = createToolButton("⭕ 圆形", ToolManager.ToolMode.DRAW_CIRCLE, toolGroup);

        toolGrid.add(selectTool, 0, 0);
        toolGrid.add(cropTool, 1, 0);
        toolGrid.add(brushTool, 0, 1);
        toolGrid.add(textTool, 1, 1);
        toolGrid.add(rectTool, 0, 2);
        toolGrid.add(circleTool, 1, 2);

        VBox drawingSettings = createDrawingSettingsPanel();
        drawingSettings.setVisible(false);

        toolGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDrawingTool = newVal == brushTool || newVal == rectTool ||
                    newVal == circleTool || newVal == textTool;
            drawingSettings.setVisible(isDrawingTool);
            drawingSettings.setManaged(isDrawingTool);
        });

        drawingSettings.setManaged(false);

        return createCard("🛠️ 交互工具", toolGrid, drawingSettings);
    }

    private ToggleButton createToolButton(String text, ToolManager.ToolMode mode, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setPrefWidth(110);
        btn.setOnAction(e -> controller.getToolManager().setToolMode(mode));
        if (mode == ToolManager.ToolMode.SELECT) {
            btn.setSelected(true);
        }
        return btn;
    }

    // === 修复与美化后的画笔设置面板 ===
    private VBox createDrawingSettingsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        // 添加 CSS 类以便美化
        panel.getStyleClass().add("settings-panel");
        // 保留一个默认样式作为兜底
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 8;");

        Label settingsLabel = new Label("画笔/文字设置");
        settingsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // 颜色选择
        HBox colorBox = new HBox(10);
        colorBox.setAlignment(Pos.CENTER_LEFT);

        Label colorLabel = new Label("颜色:");
        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.getStyleClass().add("color-picker"); // CSS类

        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            controller.getToolManager().setBrushColor(newVal);
        });

        colorBox.getChildren().addAll(colorLabel, colorPicker);

        // 大小选择
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("大小:");

        // 【关键修复】将最大值从 50 改为 300，解决大图文字太小的问题
        Spinner<Integer> brushSizeSpinner = new Spinner<>(1, 300, 24);
        brushSizeSpinner.setEditable(true);
        brushSizeSpinner.getStyleClass().add("spinner"); // CSS类

        // 监听器必须在定义之后
        brushSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            controller.getToolManager().setBrushSize(newVal);
        });

        sizeBox.getChildren().addAll(sizeLabel, brushSizeSpinner);

        panel.getChildren().addAll(settingsLabel, colorBox, sizeBox);
        return panel;
    }

    private VBox createTransformCard() {
        GridPane transGrid = new GridPane();
        transGrid.setHgap(10);
        transGrid.setVgap(10);

        Button rotate90Btn = createOperationButton("⟳ 90°");
        rotate90Btn.setOnAction(e -> controller.getImageManager().rotate90());

        Button rotate180Btn = createOperationButton("⟳ 180°");
        rotate180Btn.setOnAction(e -> controller.getImageManager().rotate180());

        Button flipHBtn = createOperationButton("⇄ 水平");
        flipHBtn.setOnAction(e -> controller.getImageManager().flipHorizontal());

        Button flipVBtn = createOperationButton("⇅ 垂直");
        flipVBtn.setOnAction(e -> controller.getImageManager().flipVertical());

        transGrid.add(rotate90Btn, 0, 0);
        transGrid.add(rotate180Btn, 1, 0);
        transGrid.add(flipHBtn, 0, 1);
        transGrid.add(flipVBtn, 1, 1);

        Button batchBtn = new Button("批量处理图片");
        batchBtn.setPrefWidth(Double.MAX_VALUE);
        batchBtn.setOnAction(e -> controller.getDialogManager().showBatchProcessingDialog());
        batchBtn.setStyle("-fx-background-color: linear-gradient(to right, #4facfe, #00f2fe); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; " +
                "-fx-cursor: hand;");

        return createCard("🔄 变换 & 批量", transGrid, new Separator(), batchBtn);
    }

    private VBox createFilterCard() {
        VBox blurControl = createSliderControl("模糊程度", 0, 10, 0,
                value -> controller.getImageManager().applyBlur(value));

        Button grayscaleBtn = createOperationButton("⚫ 灰度化");
        grayscaleBtn.setOnAction(e -> controller.getImageManager().applyGrayscale());

        Button edgeDetectBtn = createOperationButton("🔲 边缘检测");
        edgeDetectBtn.setOnAction(e -> controller.getImageManager().detectEdges());

        HBox filterBtns = new HBox(10, grayscaleBtn, edgeDetectBtn);
        HBox.setHgrow(grayscaleBtn, Priority.ALWAYS);
        HBox.setHgrow(edgeDetectBtn, Priority.ALWAYS);
        grayscaleBtn.setMaxWidth(Double.MAX_VALUE);
        edgeDetectBtn.setMaxWidth(Double.MAX_VALUE);

        return createCard("✨ 滤镜特效", blurControl, filterBtns);
    }

    private VBox createAICard() {
        Button aiEnhanceBtn = createAIButton("✨ AI 智能增强",
                e -> controller.getImageManager().aiEnhance(), "#845ec2");

        Button removeBgBtn = createAIButton("🖼 一键移除背景",
                e -> controller.getImageManager().removeBackground(), "#ff9671");

        Button styleBtn = createAIButton("🎨 艺术风格迁移",
                e -> controller.getImageManager().applyArtisticStyle(), "#ffc75f");

        VBox aiCard = createCard("🤖 AI 实验室", aiEnhanceBtn, removeBgBtn, styleBtn);

        if (controller.getArkManager().isAvailable()) {
            Button arkBtn = createAIButton("🌌 豆包图生图",
                    e -> controller.getDialogManager().showArkImageDialog(), "#0081cf");
            aiCard.getChildren().add(arkBtn);
        }

        return aiCard;
    }

    private Button createAIButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action, String colorHex) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(action);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-padding: 10;");
        return btn;
    }

    public StackPane createCenterPanel() {
        StackPane centerPane = new StackPane();
        centerPane.setId("center-pane");

        // 图像容器
        VBox imageContainer = new VBox(20);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(30));

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.3)));

        // 设置给ImageManager
        controller.getImageManager().setImageView(imageView);

        Canvas selectionCanvas = new Canvas();
        selectionCanvas.setMouseTransparent(true);
        selectionCanvas.setId("selection-canvas");

        Pane interactionOverlay = new Pane();
        interactionOverlay.setStyle("-fx-background-color: transparent;");

        // 设置鼠标交互
        setupMouseInteraction(interactionOverlay, selectionCanvas);

        StackPane imagePane = new StackPane(imageView, selectionCanvas, interactionOverlay);

        // 控制按钮条
        HBox controlButtons = createControlButtons(imageView);

        imageContainer.getChildren().addAll(imagePane, controlButtons);

        ScrollPane imageScrollPane = new ScrollPane(imageContainer);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        imageScrollPane.setId("image-scroll-pane");

        // 设置给ImageManager
        controller.getImageManager().setImageScrollPane(imageScrollPane);

        // 上传占位符
        VBox placeholder = createUploadPlaceholder();

        // 初始状态
        imageScrollPane.setVisible(false);
        controlButtons.setVisible(false);

        centerPane.getChildren().addAll(imageScrollPane, placeholder);
        return centerPane;
    }

    private void setupMouseInteraction(Pane overlay, Canvas canvas) {
        overlay.setOnMousePressed(e -> {
            controller.getToolManager().handleMousePressed(e.getX(), e.getY(), canvas);
        });

        overlay.setOnMouseDragged(e -> {
            controller.getToolManager().handleMouseDragged(e.getX(), e.getY(), canvas);
        });

        overlay.setOnMouseReleased(e -> {
            controller.getToolManager().handleMouseReleased(e.getX(), e.getY());
        });

        overlay.setOnMouseClicked(e -> {
            if (controller.getToolManager().getCurrentToolMode() == ToolManager.ToolMode.DRAW_TEXT) {
                controller.getToolManager().handleTextClick(e.getX(), e.getY());
            }
        });
    }

    private HBox createControlButtons(ImageView imageView) {
        HBox controlButtons = new HBox(15);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setId("control-buttons");
        controlButtons.setStyle("-fx-background-color: rgba(255,255,255,0.9); " +
                "-fx-background-radius: 30; -fx-padding: 8 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0, 5, 0, 0);");

        Button zoomIn = createIconButton("➕", "放大");
        zoomIn.setOnAction(e -> controller.getImageManager().zoomIn());

        Button zoomOut = createIconButton("➖", "缩小");
        zoomOut.setOnAction(e -> controller.getImageManager().zoomOut());

        Button zoomFit = createIconButton("⛶", "适应窗口");
        zoomFit.setOnAction(e -> controller.getImageManager().fitToWindow());

        // 对比按钮
        Button compareBtn = createIconButton(" 👁 ", "长按对比");
        compareBtn.setOnMousePressed(e -> {
            Image original = controller.getImageManager().getOriginalImage();
            if (original != null) {
                imageView.setImage(original);
            }
        });

        compareBtn.setOnMouseReleased(e -> {
            Image current = controller.getImageManager().getCurrentImage();
            if (current != null) {
                imageView.setImage(current);
            }
        });

        Button confirmCropBtn = createIconButton("✓", "确认裁剪");
        confirmCropBtn.setVisible(false);
        confirmCropBtn.setOnAction(e -> controller.getToolManager().applyCrop());
        confirmCropBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                "-fx-background-radius: 50;");

        controlButtons.getChildren().addAll(zoomIn, zoomOut, zoomFit, compareBtn, confirmCropBtn);
        return controlButtons;
    }

    private VBox createUploadPlaceholder() {
        VBox placeholder = new VBox(15);
        placeholder.setId("placeholder");
        placeholder.getStyleClass().add("upload-zone");
        placeholder.setMaxSize(500, 350);

        Label icon = new Label("☁️");
        icon.getStyleClass().add("upload-icon");
        icon.setStyle("-fx-font-size: 80px;");

        Label text = new Label("拖放图片到此处");
        text.getStyleClass().add("upload-hint-title");
        text.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subText = new Label("或者点击此区域打开文件");
        subText.getStyleClass().add("upload-hint-sub");
        subText.setStyle("-fx-font-size: 14px;");

        Button openBtn = new Button("📂 选择文件");
        openBtn.getStyleClass().add("save-btn");
        openBtn.setMouseTransparent(true);
        openBtn.setStyle("-fx-background-color: linear-gradient(to right, #4CAF50, #8BC34A); " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 10 20;");

        placeholder.getChildren().addAll(icon, text, subText, openBtn);

        // 点击事件
        placeholder.setOnMouseClicked(e -> controller.getImageManager().openImage());

        // 拖拽支持
        placeholder.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                placeholder.setStyle("-fx-border-color: #00ffc8; " +
                        "-fx-background-color: rgba(0, 255, 200, 0.1);");
            }
            event.consume();
        });

        placeholder.setOnDragExited(event -> {
            placeholder.setStyle("");
            event.consume();
        });

        placeholder.setOnDragDropped(event -> {
            var db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                controller.getImageManager().loadImage(file);
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        return placeholder;
    }

    public ScrollPane createRightPanel() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);

        // 操作历史卡片
        ListView<String> historyListView = new ListView<>();
        historyListView.setPrefHeight(250);
        historyListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        historyListView.setId("history-list");

        controller.getImageManager().setHistoryListView(historyListView);

        Button clearHistoryBtn = new Button("清空记录");
        clearHistoryBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; " +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 5 0;");
        clearHistoryBtn.setOnAction(e -> {
            historyListView.getItems().clear();
            controller.updateStatus("历史记录已清空");
        });

        VBox historyCard = createCard("📜 操作时光机", historyListView, clearHistoryBtn);

        // 图像信息卡片
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);

        // 显示图像信息
        addInfoRow(infoGrid, 0, "📏 尺寸", "size-label", "-- x --");
        addInfoRow(infoGrid, 1, "📁 格式", "format-label", "--");
        addInfoRow(infoGrid, 2, "💾 大小", "filesize-label", "-- KB");

        VBox infoCard = createCard("ℹ️ 图像档案", infoGrid);

        // 快捷操作卡片
        Button resetBtn = createOperationButton("🔄 重置图片");
        resetBtn.setOnAction(e -> controller.getImageManager().resetImage());
        resetBtn.setStyle("-fx-background-color: rgba(255, 82, 82, 0.1); " +
                "-fx-text-fill: #ff5252; -fx-background-radius: 6; " +
                "-fx-cursor: hand; -fx-font-weight: bold;");

        Button clearBtn = createOperationButton("🗑️ 清空画布");
        clearBtn.setOnAction(e -> controller.getImageManager().clearCanvas());

        VBox quickCard = createCard("⚡ 快捷指令", resetBtn, clearBtn);

        content.getChildren().addAll(historyCard, infoCard, quickCard);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private void addInfoRow(GridPane grid, int row, String title, String valueId, String defaultValue) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        Label v = new Label(defaultValue);
        v.setId(valueId);
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        grid.add(t, 0, row);
        grid.add(v, 1, row);
    }

    // 更新图像信息
    public void updateImageInfo() {
        if (controller.getImageManager().getCurrentImage() == null) return;

        Image img = controller.getImageManager().getCurrentImage();
        Label sizeLbl = (Label) controller.getRoot().lookup("#size-label");
        Label formatLbl = (Label) controller.getRoot().lookup("#format-label");
        Label fileLbl = (Label) controller.getRoot().lookup("#filesize-label");

        if (sizeLbl != null) {
            sizeLbl.setText((int)img.getWidth() + " x " + (int)img.getHeight());
        }

        try {
            File file = controller.getImageManager().getCurrentImageFile();
            if (file != null) {
                if (formatLbl != null) {
                    String name = file.getName();
                    String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toUpperCase() : "UNK";
                    formatLbl.setText(ext);
                }
                if (fileLbl != null) {
                    long sizeKB = file.length() / 1024;
                    fileLbl.setText(sizeKB + " KB");
                }
            }
        } catch (Exception e) {
            // 忽略文件信息错误
        }
    }

    public HBox createBottomBar() {
        HBox bottomBar = new HBox(20);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.getStyleClass().add("floating-bottom-bar");
        bottomBar.setId("bottom-capsule");

        // 状态信息
        Label statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 缩放滑块
        Label zoomIcon = new Label("🔍");
        zoomIcon.setStyle("-fx-font-size: 14px; -fx-opacity: 0.7;");

        Slider zoomSlider = new Slider(0.1, 3.0, 1.0);
        zoomSlider.setPrefWidth(150);
        zoomSlider.setShowTickLabels(false);
        zoomSlider.setShowTickMarks(false);

        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            controller.getImageManager().setZoom(newVal.doubleValue());
            statusLabel.setText(String.format("缩放: %.0f%%", newVal.doubleValue() * 100));
        });

        bottomBar.getChildren().addAll(statusLabel, spacer, zoomIcon, zoomSlider);
        HBox.setMargin(bottomBar, new Insets(0, 20, 20, 20));
        bottomBar.setMaxWidth(800);

        return bottomBar;
    }

    // UI辅助方法
    public Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));

        btn.setStyle("-fx-font-size: 16px; " +
                "-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-border-color: rgba(255,255,255,0.2); " +
                "-fx-text-fill: white; " +
                "-fx-padding: 8 12; " +
                "-fx-cursor: hand; " +
                "-fx-border-radius: 50%; " +
                "-fx-background-radius: 50%;");

        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-font-size: 16px; " +
                    "-fx-background-color: rgba(255,255,255,0.2); " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 8 12; " +
                    "-fx-cursor: hand; " +
                    "-fx-border-radius: 50%; " +
                    "-fx-background-radius: 50%;");
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-font-size: 16px; " +
                    "-fx-background-color: rgba(255,255,255,0.1); " +
                    "-fx-border-color: rgba(255,255,255,0.2); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 8 12; " +
                    "-fx-cursor: hand; " +
                    "-fx-border-radius: 50%; " +
                    "-fx-background-radius: 50%;");
        });

        return btn;
    }

    public Button createOperationButton(String text) {
        Button btn = new Button(text);

        btn.getStyleClass().add("operation-button");

        String backgroundColor = "#2d2d2d";
        String hoverColor = "#3d3d3d";
        String borderColor = "#444";
        String textColor = "#ffffff";

        btn.setStyle("-fx-background-color: " + backgroundColor + "; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 8 12; -fx-cursor: hand; " +
                "-fx-text-fill: " + textColor + ";");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverColor + "; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 8 12; -fx-cursor: hand; " +
                        "-fx-text-fill: " + textColor + ";"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + backgroundColor + "; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 8 12; -fx-cursor: hand; " +
                        "-fx-text-fill: " + textColor + ";"
        ));

        return btn;
    }

    public VBox createCard(String title, Node... nodes) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.6); " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");
        card.setId("content-card");

        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-opacity: 0.8;");
            titleLabel.setId("card-title");
            card.getChildren().add(titleLabel);
        }

        for (Node node : nodes) {
            card.getChildren().add(node);
        }

        return card;
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

    public void setupShortcuts(Scene scene) {
        // 快捷键设置已经在EditorController中完成
    }

    public void applyTheme(ThemeManager.Theme theme) {
        // 更新深色模式标志
        isDarkMode = (theme == ThemeManager.Theme.DARK_MODE ||
                theme == ThemeManager.Theme.BLUE_NIGHT ||
                theme == ThemeManager.Theme.CYBERPUNK);

        if (controller.getRoot() != null) {
            String style = controller.getThemeManager().getThemeStyle(theme);
            controller.getRoot().setStyle(style);

            controller.getRoot().lookupAll(".scroll-bar").forEach(node ->
                    node.setStyle("-fx-background-color: transparent; -fx-block-increment: 0;"));

            controller.getRoot().lookupAll(".scroll-bar .thumb").forEach(node ->
                    node.setStyle("-fx-background-color: derive(-fx-base, -20%); " +
                            "-fx-background-radius: 5em;"));

            updatePanelStyles(theme);
            updateCenterPanelStyle(theme);
        }

        controller.updateStatus("已切换主题: " + theme.getDisplayName());
    }

    private void updateCenterPanelStyle(ThemeManager.Theme theme) {
        Node centerNode = controller.getRoot().getCenter();
        if (centerNode instanceof StackPane) {
            StackPane centerPane = (StackPane) centerNode;

            String color = "#e3e6ea"; // 默认浅色背景基色
            if (theme == ThemeManager.Theme.DARK_MODE ||
                    theme == ThemeManager.Theme.CYBERPUNK ||
                    theme == ThemeManager.Theme.BLUE_NIGHT) {
                color = "#1e1e1e";
            } else if (theme == ThemeManager.Theme.ORANGE_SUNSET) {
                color = "#431407";
            } else if (theme == ThemeManager.Theme.GREEN_FOREST) {
                color = "#022c22";
            }

            centerPane.setBackground(createCheckerboardBackground(color));
        }
    }

    private void updatePanelStyles(ThemeManager.Theme theme) {
        String mainBg = extractBackgroundColor(controller.getThemeManager().getThemeStyle(theme));
        String cardBg = controller.getThemeManager().getCardBackground(theme);
        String textColor = controller.getThemeManager().getTextColor(theme);
        String titleColor = controller.getThemeManager().getTitleColor(theme);

        if (controller.getRoot() != null) {
            controller.getRoot().setStyle("-fx-background-color: " + mainBg + ";");
        }

        updateRecursiveStyle(controller.getRoot(), cardBg, textColor, titleColor, theme);
    }

    private String extractBackgroundColor(String style) {
        if (style == null || !style.contains("-fx-background-color:")) {
            return "#f5f7fa";
        }

        String[] parts = style.split("-fx-background-color:");
        if (parts.length > 1) {
            String colorPart = parts[1].split(";")[0].trim();
            return colorPart;
        }

        return "#f5f7fa";
    }

    private void updateRecursiveStyle(Node node, String cardBg, String textColor,
                                      String titleColor, ThemeManager.Theme theme) {
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;

            if ("bottom-capsule".equals(node.getId())) {
                if (theme == ThemeManager.Theme.LIGHT_MODE) {
                    node.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); " +
                            "-fx-background-radius: 30; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(160, 100, 200, 0.2), 20, 0, 0, 5);");
                } else if (theme == ThemeManager.Theme.DARK_MODE) {
                    node.setStyle("-fx-background-color: rgba(255, 252, 245, 0.9); " +
                            "-fx-background-radius: 30; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(255, 100, 50, 0.3), 20, 0, 0, 5);");
                } else {
                    node.setStyle("-fx-background-color: rgba(30, 30, 30, 0.85); " +
                            "-fx-background-radius: 30; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 5);");
                }
            }

            else if (node.getStyleClass().contains("sidebar-header")) {
                if (node instanceof Label) {
                    Label title = (Label) node;
                    if (theme == ThemeManager.Theme.LIGHT_MODE) {
                        title.setStyle("-fx-text-fill: linear-gradient(to right, #667eea, #764ba2); " +
                                "-fx-font-size: 16px; -fx-font-weight: bold;");
                    } else if (theme == ThemeManager.Theme.DARK_MODE) {
                        title.setStyle("-fx-text-fill: #5c4033; -fx-font-size: 16px; -fx-font-weight: bold;");
                    } else {
                        String color = (theme == ThemeManager.Theme.CYBERPUNK) ? "#00ff41" : "#e2e8f0";
                        title.setStyle("-fx-text-fill: " + color + "; " +
                                "-fx-font-size: 16px; -fx-font-weight: bold;");
                    }
                }
            }

            else if ("placeholder".equals(node.getId())) {
                if (theme == ThemeManager.Theme.LIGHT_MODE) {
                    node.setStyle("-fx-background-color: rgba(255, 235, 242, 0.7); " +
                            "-fx-border-color: rgba(255, 192, 203, 0.8); " +
                            "-fx-border-width: 2; -fx-border-style: dashed; " +
                            "-fx-background-radius: 24; -fx-border-radius: 24; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(255, 105, 180, 0.3), 15, 0, 0, 0);");
                } else if (theme == ThemeManager.Theme.DARK_MODE) {
                    node.setStyle("-fx-background-color: rgba(255, 255, 255, 0.25); " +
                            "-fx-border-color: rgba(255, 230, 200, 0.8); " +
                            "-fx-border-width: 3; -fx-border-style: dashed; " +
                            "-fx-background-radius: 24; -fx-border-radius: 24; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(255, 100, 50, 0.4), 15, 0, 0, 0);");
                } else {
                    node.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); " +
                            "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                            "-fx-border-width: 2; -fx-border-style: dashed; " +
                            "-fx-background-radius: 24; -fx-border-radius: 24;");
                }
            }

            else if ("content-card".equals(node.getId())) {
                node.setStyle("-fx-background-color: " + cardBg + "; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 0); " +
                        "-fx-padding: 20;");
            }

            if (node instanceof Label) {
                updateLabelStyle((Label) node, theme, textColor, titleColor);
            }

            if (node instanceof Button) {
                updateButtonStyle((Button) node, theme);
            }

            if (node instanceof Separator) {
                updateSeparatorStyle((Separator) node, theme);
            }

            for (Node child : parent.getChildrenUnmodifiable()) {
                updateRecursiveStyle(child, cardBg, textColor, titleColor, theme);
            }
        }
    }

    private void updateLabelStyle(Label label, ThemeManager.Theme theme, String textColor, String titleColor) {
        String themeTextColor = controller.getThemeManager().getTextColor(theme);
        String themeTitleColor = controller.getThemeManager().getTitleColor(theme);

        String styleClass = label.getStyleClass().toString();

        if (label.getStyleClass().contains("sidebar-header") ||
                label.getStyleClass().contains("app-logo-text") ||
                label.getStyleClass().contains("app-logo-icon")) {
            label.setStyle("-fx-text-fill: " + themeTitleColor + "; -fx-font-weight: bold;");
        } else if (label.getStyleClass().contains("upload-hint-title")) {
            label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + themeTitleColor + ";");
        } else if (label.getStyleClass().contains("upload-hint-sub")) {
            label.setStyle("-fx-font-size: 14px; -fx-text-fill: " + themeTextColor + "; -fx-opacity: 0.8;");
        } else if (label.getStyleClass().contains("upload-icon")) {
            label.setStyle("-fx-font-size: 80px; -fx-text-fill: " + themeTitleColor + "; -fx-opacity: 0.6;");
        } else if ("card-title".equals(label.getId())) {
            label.setStyle("-fx-text-fill: " + themeTitleColor + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        } else if (label.getId() != null && label.getId().contains("value")) {
            String bgColor = controller.getThemeManager().isDarkTheme(theme) ?
                    "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.06)";
            label.setStyle("-fx-text-fill: " + themeTextColor + "; -fx-background-color: " + bgColor + "; " +
                    "-fx-background-radius: 4; -fx-padding: 2 6; " +
                    "-fx-font-family: 'Consolas', monospace;");
        } else {
            label.setStyle("-fx-text-fill: " + themeTextColor + ";");
        }
    }

    private void updateButtonStyle(Button button, ThemeManager.Theme theme) {
        if (button.getParent() != null && "control-buttons".equals(button.getParent().getId())) {
            return;
        }

        String buttonText = button.getText();

        if (buttonText != null && (buttonText.contains("↩") || buttonText.contains("↪") ||
                buttonText.contains("📂") || buttonText.contains("🌗") || buttonText.contains("?"))) {

            String backgroundColor = isDarkMode ? "rgba(40, 40, 40, 0.9)" : "rgba(255, 255, 255, 0.9)";
            String borderColor = isDarkMode ? "#666" : "#dee2e6";
            String textColor = isDarkMode ? "white" : "#2c3e50";

            button.setStyle("-fx-font-size: 18px; " +
                    "-fx-background-color: " + backgroundColor + "; " +
                    "-fx-border-color: " + borderColor + "; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 8 12; -fx-cursor: hand; " +
                    "-fx-text-fill: " + textColor + ";");

            return;
        }

        String gradient = controller.getThemeManager().getThemeGradient(theme);
        String btnTextColor = controller.getThemeManager().getButtonTextColor(theme);

        if (button.getStyleClass().contains("operation-button")) {
            String backgroundColor = isDarkMode ? "#2d2d2d" : "#f8f9fa";
            String hoverColor = isDarkMode ? "#3d3d3d" : "#e9ecef";
            String borderColor = isDarkMode ? "#444" : "#dee2e6";

            button.setStyle("-fx-background-color: " + backgroundColor + "; " +
                    "-fx-border-color: " + borderColor + "; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-padding: 8 12; -fx-cursor: hand; " +
                    "-fx-text-fill: " + btnTextColor + ";");

            button.setOnMouseEntered(e -> {
                button.setStyle("-fx-background-color: " + hoverColor + "; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 8 12; -fx-cursor: hand; " +
                        "-fx-text-fill: " + btnTextColor + ";");
            });

            button.setOnMouseExited(e -> {
                button.setStyle("-fx-background-color: " + backgroundColor + "; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 8 12; -fx-cursor: hand; " +
                        "-fx-text-fill: " + btnTextColor + ";");
            });

            return;
        }

        button.setStyle("-fx-background-color: " + gradient + "; " +
                "-fx-text-fill: " + btnTextColor + "; " +
                "-fx-background-radius: 6; -fx-font-weight: bold; " +
                "-fx-cursor: hand; -fx-padding: 8 15;");
    }

    private void updateSeparatorStyle(Separator separator, ThemeManager.Theme theme) {
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

    private Background createCheckerboardBackground(String baseColorHex) {
        Color baseColor = Color.web(baseColorHex);
        int size = 20;
        Canvas canvas = new Canvas(size * 2, size * 2);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(baseColor);
        gc.fillRect(0, 0, size * 2, size * 2);

        Color checkColor = baseColor.grayscale().getBrightness() > 0.5 ?
                baseColor.darker() : baseColor.brighter();
        gc.setFill(Color.color(checkColor.getRed(), checkColor.getGreen(), checkColor.getBlue(), 0.05));
        gc.fillRect(0, 0, size, size);
        gc.fillRect(size, size, size, size);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage patternImage = canvas.snapshot(params, null);

        return new Background(new BackgroundFill(
                new ImagePattern(patternImage, 0, 0, size * 2, size * 2, false),
                CornerRadii.EMPTY,
                Insets.EMPTY
        ));
    }

    // 进度显示
    public void showProgress(String message) {
        if (loadingOverlay == null) {
            loadingOverlay = new StackPane();
            loadingOverlay.getStyleClass().add("loading-overlay");
            loadingOverlay.setVisible(false);

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);

            ProgressIndicator pi = new ProgressIndicator();
            pi.setPrefSize(60, 60);

            loadingText = new Label(message);
            loadingText.getStyleClass().add("loading-text");

            content.getChildren().addAll(pi, loadingText);
            loadingOverlay.getChildren().add(content);

            if (controller.getMainScene() != null && controller.getMainScene().getRoot() instanceof Pane) {
                Pane root = (Pane) controller.getMainScene().getRoot();
                if (!root.getChildren().contains(loadingOverlay)) {
                    root.getChildren().add(loadingOverlay);
                }
            }
        }

        if (loadingText != null) loadingText.setText(message);
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.toFront();
        }
    }

    public void hideProgress() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        }
    }

    // Toast显示
    public void showToast(String message, String type) {
        if (toastContainer == null) {
            if (controller.getMainScene() != null && controller.getMainScene().getRoot() instanceof StackPane) {
                toastContainer = new VBox(10);
                toastContainer.setAlignment(Pos.BOTTOM_CENTER);
                toastContainer.setPadding(new Insets(0, 0, 50, 0));
                toastContainer.setMouseTransparent(true);
                ((StackPane) controller.getMainScene().getRoot()).getChildren().add(toastContainer);
            } else {
                return;
            }
        }

        Label toast = new Label(message);
        toast.getStyleClass().add("toast-message");
        toast.getStyleClass().add("toast-" + type);

        toast.setOpacity(0);

        toastContainer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.5));
        fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));

        SequentialTransition seq = new SequentialTransition(fadeIn, fadeOut);
        seq.play();
    }

    private void applyAllAdjustments() {
        if (brightnessValue == 0 && contrastValue == 0 && saturationValue == 0) {
            controller.showWarning("提示", "请先调整滑块参数");
            return;
        }

        controller.getImageManager().applyAllAdjustments(brightnessValue, contrastValue, saturationValue);
    }

    private void resetAllAdjustments() {
        brightnessValue = 0.0;
        contrastValue = 0.0;
        saturationValue = 0.0;

        Node brightnessSlider = controller.getRoot().lookup("#亮度-slider");
        Node contrastSlider = controller.getRoot().lookup("#对比度-slider");
        Node saturationSlider = controller.getRoot().lookup("#饱和度-slider");

        if (brightnessSlider instanceof Slider) {
            ((Slider) brightnessSlider).setValue(0);
        }

        if (contrastSlider instanceof Slider) {
            ((Slider) contrastSlider).setValue(0);
        }

        if (saturationSlider instanceof Slider) {
            ((Slider) saturationSlider).setValue(0);
        }

        controller.getImageManager().resetImage();
        controller.updateStatus("调整已重置");
        controller.showSuccess("重置完成", "所有调整已重置为默认值");
    }

    @FunctionalInterface
    interface SliderChangeListener {
        void onChange(double value);
    }
}