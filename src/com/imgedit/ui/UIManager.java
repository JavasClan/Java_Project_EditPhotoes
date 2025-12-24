package imgedit.ui;

import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.scene.input.*;
import javafx.scene.input.KeyCombination;
import javafx.util.Duration;
import javafx.animation.*;
import java.util.function.Consumer;

/**
 * UI组件创建和管理器
 */
public class UIManager {

    private final EditorController controller;
    private VBox toastContainer;
    private StackPane loadingOverlay;
    private Label loadingText;

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
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));

        // Logo区域
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("✨");
        logoIcon.getStyleClass().add("app-logo-icon");

        Label appTitle = new Label("Pro Image Editor");
        appTitle.getStyleClass().add("app-logo-text");

        logoBox.getChildren().addAll(logoIcon, appTitle);

        // 中间占位
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 右侧按钮
        HBox rightActions = new HBox(15);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        // 功能按钮
        Button undoBtn = createIconButton("↩️", "撤销");
        Button redoBtn = createIconButton("↪️", "重做");
        Button openBtn = createIconButton("📂", "打开");
        Button saveBtn = new Button("💾 保存");
        saveBtn.getStyleClass().add("save-btn");
        Button themeBtn = createIconButton("🌗", "主题");
        Button helpBtn = createIconButton("❓", "关于");

        // 设置事件
        undoBtn.setOnAction(e -> controller.getImageManager().undo());
        redoBtn.setOnAction(e -> controller.getImageManager().redo());
        openBtn.setOnAction(e -> controller.getImageManager().openImage());
        saveBtn.setOnAction(e -> controller.getImageManager().saveImage());
        themeBtn.setOnAction(e -> controller.getDialogManager().showThemeSelector());
        helpBtn.setOnAction(e -> controller.getDialogManager().showHelp());

        rightActions.getChildren().addAll(undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                openBtn, saveBtn, new Separator(Orientation.VERTICAL), themeBtn, helpBtn);

        topBar.getChildren().addAll(logoBox, spacer, rightActions);
        return topBar;
    }

    public ScrollPane createLeftPanel() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);

        // 1. 基础调整卡片
        VBox adjustmentPanel = createAdvancedAdjustmentPanel();
        VBox basicCard = createCard("🎛  基础调整", adjustmentPanel);

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

    public StackPane createCenterPanel() {
        return controller.getImageManager().createImageDisplayArea();
    }

    public ScrollPane createRightPanel() {
        return controller.getImageManager().createHistoryPanel();
    }

    public HBox createBottomBar() {
        HBox bottomBar = new HBox(20);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setId("bottom-capsule");

        Label statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label zoomIcon = new Label("🔍");
        zoomIcon.setStyle("-fx-font-size: 14px; -fx-opacity: 0.7;");

        Slider zoomSlider = new Slider(0.1, 3.0, 1.0);
        zoomSlider.setPrefWidth(150);
        zoomSlider.setShowTickLabels(false);
        zoomSlider.setShowTickMarks(false);

        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            controller.getImageManager().setZoom(newVal.doubleValue());
        });

        bottomBar.getChildren().addAll(statusLabel, spacer, zoomIcon, zoomSlider);
        HBox.setMargin(bottomBar, new Insets(0, 20, 20, 20));
        bottomBar.setMaxWidth(800);

        return bottomBar;
    }

    // 其他UI创建方法...
    public Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().add("icon-action-btn");
        return btn;
    }

    public VBox createCard(String title, Node... nodes) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(15));
        card.setId("content-card");

        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.setId("card-title");
            card.getChildren().add(titleLabel);
        }

        for (Node node : nodes) {
            card.getChildren().add(node);
        }

        return card;
    }

    public void loadCSS(Scene scene) {
        // CSS加载逻辑
        // ...
    }

    public void setupShortcuts(Scene scene) {
        // 快捷键设置逻辑
        // ...
    }

    public void applyTheme(ThemeManager.Theme theme) {
        // 主题应用逻辑
        // ...
    }

    public void showToast(String message, String type) {
        // Toast显示逻辑
        // ...
    }

    public void showProgress(String message) {
        // 进度显示逻辑
        // ...
    }

    public void hideProgress() {
        // 隐藏进度逻辑
        // ...
    }
}