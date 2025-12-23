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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import javax.imageio.ImageIO;

// 添加豆包图生图API相关导入
import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.binary.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.io.IOException;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.ImagePattern;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Orientation;
import javafx.stage.StageStyle;

/**
 * 现代化图像编辑器 - 支持多种高级主题 + 豆包图生图功能
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

    // 交互状态
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

    // 豆包图生图配置
    private Properties arkConfig;
    private boolean arkAvailable = false;

    private StackPane loadingOverlay; // 全局加载层
    private Label loadingText;        // 加载提示文字

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // 1. 加载配置和主题
        loadArkConfig();
        initializeThemes();

        // 2. 显示启动画面
        showSplashScreen(() -> {
            Platform.runLater(() -> {
                // 3. 初始化主界面
                initializeMainWindow();

                // 4. [修正] 加载 CSS (路径: src/resources/styles/main.css)
                try {
                    // 定义硬盘上的源码路径 (用于开发环境调试)
                    String localPath = "src/resources/styles/main.css";

                    // 方案 A: 优先尝试直接读取硬盘文件 (最稳妥，所见即所得)
                    java.io.File cssFile = new java.io.File(localPath);

                    if (cssFile.exists()) {
                        // 如果硬盘上文件存在，直接加载
                        String uri = cssFile.toURI().toString();
                        mainScene.getStylesheets().add(uri);
                        System.out.println("✅ (硬盘模式) CSS 加载成功: " + cssFile.getAbsolutePath());
                    } else {
                        // 方案 B: 如果硬盘找不到，尝试从 classpath 加载 (用于打包后的环境)
                        // 通常 resources 目录被标记为资源根目录后，读取时不需要带 "resources/" 前缀
                        String[] resourcePaths = {
                                "/styles/main.css",           // 标准 Maven/Gradle 结构
                                "/resources/styles/main.css", // 如果 resources 只是普通包
                                "styles/main.css"             // 相对路径尝试
                        };

                        boolean loaded = false;
                        for (String path : resourcePaths) {
                            java.net.URL url = getClass().getResource(path);
                            if (url == null) url = getClass().getClassLoader().getResource(path);

                            if (url != null) {
                                mainScene.getStylesheets().add(url.toExternalForm());
                                System.out.println("✅ (资源模式) CSS 加载成功: " + path);
                                loaded = true;
                                break;
                            }
                        }

                        if (!loaded) {
                            System.err.println("❌ 错误: 找不到 CSS 文件！");
                            System.err.println("   请确认文件路径是: " + cssFile.getAbsolutePath());
                        }
                    }

                } catch (Exception e) {
                    System.err.println("❌ CSS 加载异常: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * 加载豆包图生图配置
     */
    private void loadArkConfig() {
        try {
            arkConfig = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (is != null) {
                arkConfig.load(is);

                // 检查配置是否可用
                String apiKey = arkConfig.getProperty("ark.api.key");
                String baseUrl = arkConfig.getProperty("ark.base.url");
                String modelId = arkConfig.getProperty("ark.model.id");

                arkAvailable = apiKey != null && !apiKey.trim().isEmpty() &&
                        baseUrl != null && !baseUrl.trim().isEmpty() &&
                        modelId != null && !modelId.trim().isEmpty();

                if (arkAvailable) {
                    System.out.println("豆包图生图配置加载成功");
                } else {
                    System.err.println("豆包图生图配置不完整");
                }
            } else {
                System.err.println("未找到config.properties文件");
                arkAvailable = false;
            }
        } catch (Exception e) {
            System.err.println("加载豆包图生图配置失败: " + e.getMessage());
            arkAvailable = false;
        }
    }

    /**
     * 初始化所有主题样式
     */
    private void initializeThemes() {
        // ... 现有代码保持不变 ...
        themeStyles.put(Theme.LIGHT_MODE,
                "-fx-background-color: #f5f7fa; " +
                        "-fx-text-fill: #2c3e50;"
        );

        themeStyles.put(Theme.DARK_MODE,
                "-fx-background-color: #121212; " +
                        "-fx-text-fill: #e0e0e0;"
        );

        themeStyles.put(Theme.BLUE_NIGHT,
                "-fx-background-color: #0f172a; " +
                        "-fx-text-fill: #e2e8f0;"
        );

        themeStyles.put(Theme.GREEN_FOREST,
                "-fx-background-color: #022c22; " +
                        "-fx-text-fill: #d1fae5;"
        );

        themeStyles.put(Theme.PURPLE_DREAM,
                "-fx-background-color: #1e1b4b; " +
                        "-fx-text-fill: #e9d5ff;"
        );

        themeStyles.put(Theme.ORANGE_SUNSET,
                "-fx-background-color: #431407; " +
                        "-fx-text-fill: #fed7aa;"
        );

        themeStyles.put(Theme.PINK_BLOSSOM,
                "-fx-background-color: #500724; " +
                        "-fx-text-fill: #fbcfe8;"
        );

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
        String style = themeStyles.get(theme);

        if (root != null) {
            root.setStyle(style);
            // [新增] 美化滚动条
            root.lookupAll(".scroll-bar").forEach(node ->
                    node.setStyle("-fx-background-color: transparent; -fx-block-increment: 0;"));
            root.lookupAll(".scroll-bar .thumb").forEach(node ->
                    node.setStyle("-fx-background-color: derive(-fx-base, -20%); -fx-background-radius: 5em;"));

            updatePanelStyles(theme);
            // [新增] 调用背景更新
            updateCenterPanelStyle(theme);
        }
        updateStatus("已切换主题: " + theme.getDisplayName());
    }

    private void updateCenterPanelStyle(Theme theme) {
        // 查找 createCenterPanel 中定义的 StackPane
        Node centerNode = root.getCenter();
        if (centerNode instanceof StackPane) {
            StackPane centerPane = (StackPane) centerNode;

            String color = "#e3e6ea"; // 默认浅色背景基色
            if (theme == Theme.DARK_MODE || theme == Theme.CYBERPUNK || theme == Theme.BLUE_NIGHT) {
                color = "#1e1e1e";
            } else if (theme == Theme.ORANGE_SUNSET) {
                color = "#431407";
            } else if (theme == Theme.GREEN_FOREST) {
                color = "#022c22";
            }
            // 应用动态生成的棋盘格
            centerPane.setBackground(createCheckerboardBackground(color));
        }
    }

    /**
     * 更新所有面板的样式
     */
    private void updatePanelStyles(Theme theme) {
        // 定义主题颜色
        String mainBg, cardBg, textColor, titleColor;
        String sliderTrack, sliderThumb;

        switch (theme) {
            case DARK_MODE:
            case CYBERPUNK:
            case BLUE_NIGHT:
            case GREEN_FOREST:
            case PURPLE_DREAM:
                mainBg = themeStyles.get(theme).split(";")[0].split(":")[1]; // 简单提取背景色
                cardBg = "rgba(255,255,255,0.08)"; // 深色模式下的半透明卡片
                textColor = "#e0e0e0";
                titleColor = "#ffffff";
                sliderTrack = "#555";
                sliderThumb = "#ccc";
                break;
            default: // Light Mode & others
                mainBg = themeStyles.get(theme).split(";")[0].split(":")[1];
                cardBg = "rgba(255,255,255,0.8)"; // 浅色模式下的白卡片
                textColor = "#333";
                titleColor = "#2c3e50";
                sliderTrack = "#e0e0e0";
                sliderThumb = "#667eea";
                break;
        }

        // 应用全局背景
        if (root != null) root.setStyle("-fx-background-color: " + mainBg + ";");

        // 递归更新所有节点样式
        updateRecursiveStyle(root, cardBg, textColor, titleColor, theme);
    }

    private void updateRecursiveStyle(Node node, String cardBg, String textColor, String titleColor, Theme theme) {
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;

            // ============================================================
            // 1. [新增] 底部悬浮胶囊 (Bottom Capsule)
            // ============================================================
            if ("bottom-capsule".equals(node.getId())) {
                if (theme == Theme.LIGHT_MODE) {
                    // 粉紫模式：磨砂白 + 淡淡的粉紫光晕
                    node.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(160, 100, 200, 0.2), 20, 0, 0, 5);");
                } else if (theme == Theme.DARK_MODE) {
                    // 橘色模式：磨砂暖白 + 橙色光晕
                    node.setStyle("-fx-background-color: rgba(255, 252, 245, 0.9); -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(255, 100, 50, 0.3), 20, 0, 0, 5);");
                } else {
                    // 深色模式：深黑磨砂
                    node.setStyle("-fx-background-color: rgba(30, 30, 30, 0.85); -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 5);");
                }
            }

            // ============================================================
            // 2. [新增] 侧边栏标题 (Sidebar Header)
            // ============================================================
            else if (node.getStyleClass().contains("sidebar-header")) {
                Label title = (Label) node;
                if (theme == Theme.LIGHT_MODE) {
                    // 粉紫模式：深紫色渐变字
                    title.setStyle("-fx-text-fill: linear-gradient(to right, #667eea, #764ba2); -fx-font-size: 16px; -fx-font-weight: bold;");
                } else if (theme == Theme.DARK_MODE) {
                    // 橘色模式：深棕色字 (为了清晰)
                    title.setStyle("-fx-text-fill: #5c4033; -fx-font-size: 16px; -fx-font-weight: bold;");
                } else {
                    // 深色模式：荧光色或亮白
                    String color = (theme == Theme.CYBERPUNK) ? "#00ff41" : "#e2e8f0";
                    title.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: bold;");
                }
            }

            // ============================================================
            // 3. 上传占位符 (Placeholder) - 保持之前的完美配色
            // ============================================================
            else if ("placeholder".equals(node.getId())) {
                if (theme == Theme.LIGHT_MODE) {
                    node.setStyle("-fx-background-color: rgba(255, 235, 242, 0.7); -fx-border-color: rgba(255, 192, 203, 0.8); -fx-border-width: 2; -fx-border-style: dashed; -fx-background-radius: 24; -fx-border-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(255, 105, 180, 0.3), 15, 0, 0, 0);");
                } else if (theme == Theme.DARK_MODE) {
                    node.setStyle("-fx-background-color: rgba(255, 255, 255, 0.25); -fx-border-color: rgba(255, 230, 200, 0.8); -fx-border-width: 3; -fx-border-style: dashed; -fx-background-radius: 24; -fx-border-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(255, 100, 50, 0.4), 15, 0, 0, 0);");
                } else {
                    node.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 2; -fx-border-style: dashed; -fx-background-radius: 24; -fx-border-radius: 24;");
                }
            }

            // --- 4. 卡片背景 ---
            else if ("content-card".equals(node.getId())) {
                node.setStyle("-fx-background-color: " + cardBg + "; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 0); -fx-padding: 20;");
            }

            // --- 5. 标签 (Label) ---
            if (node instanceof Label) {
                Label l = (Label) node;
                if (l.getStyleClass().contains("sidebar-header") || l.getStyleClass().contains("app-logo-text") || l.getStyleClass().contains("app-logo-icon")) {
                    // 已处理或Logo，跳过
                } else if (l.getStyleClass().contains("upload-hint-title")) {
                    String hintColor = (theme == Theme.LIGHT_MODE ? "#5c5c8a" : (theme == Theme.DARK_MODE ? "#5c4033" : "#94a3b8"));
                    l.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + hintColor + ";");
                } else if (l.getStyleClass().contains("upload-hint-sub")) {
                    String subColor = (theme == Theme.LIGHT_MODE ? "#8c8ca0" : (theme == Theme.DARK_MODE ? "#8c6b5e" : "#64748b"));
                    l.setStyle("-fx-font-size: 14px; -fx-text-fill: " + subColor + ";");
                } else if (l.getStyleClass().contains("upload-icon")) {
                    String iconColor = (theme == Theme.LIGHT_MODE ? "rgba(102, 126, 234, 0.5)" : (theme == Theme.DARK_MODE ? "rgba(255, 153, 51, 0.6)" : "#475569"));
                    l.setStyle("-fx-font-size: 80px; -fx-text-fill: " + iconColor + ";");
                } else if ("card-title".equals(l.getId())) {
                    l.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-weight: bold; -fx-font-size: 15px;");
                } else if (l.getId() != null && l.getId().contains("value")) {
                    l.getStyleClass().add("value-label");
                    if (theme == Theme.LIGHT_MODE || theme == Theme.DARK_MODE) {
                        String bg = (theme == Theme.LIGHT_MODE) ? "rgba(0,0,0,0.06)" : "rgba(255,235,200,0.5)";
                        l.setStyle("-fx-text-fill: #333333; -fx-background-color: " + bg + "; -fx-background-radius: 4; -fx-padding: 2 6; -fx-font-family: 'Consolas', monospace;");
                    } else {
                        l.setStyle("");
                    }
                } else {
                    String finalColor = (theme == Theme.LIGHT_MODE || theme == Theme.DARK_MODE) ? "#333333" : textColor;
                    l.setStyle("-fx-text-fill: " + finalColor + ";");
                }
            }

            // --- 6. 按钮 (Button) ---
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn.getStyleClass().contains("icon-action-btn")) {
                    String iconColor = (theme == Theme.LIGHT_MODE ? "#4a5568" : (theme == Theme.DARK_MODE ? "#5c4033" : "#cbd5e1"));
                    btn.setStyle("-fx-text-fill: " + iconColor + "; -fx-background-color: transparent;");
                    if (theme == Theme.LIGHT_MODE || theme == Theme.DARK_MODE) {
                        String hoverColor = (theme == Theme.LIGHT_MODE) ? "#667eea" : "#ff6b35";
                        btn.setOnMouseEntered(e -> btn.setStyle("-fx-text-fill: " + hoverColor + "; -fx-background-color: rgba(255,255,255,0.5);"));
                        btn.setOnMouseExited(e -> btn.setStyle("-fx-text-fill: " + iconColor + "; -fx-background-color: transparent;"));
                    } else {
                        btn.setOnMouseEntered(null); btn.setOnMouseExited(null);
                    }
                } else if (btn.getStyleClass().contains("save-btn")) {
                    // css
                } else {
                    String commonLayout = "-fx-padding: 8 8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-radius: 8;";
                    if (theme == Theme.LIGHT_MODE || theme == Theme.DARK_MODE) {
                        String accentColor = (theme == Theme.LIGHT_MODE) ? "#667eea" : "#ff6b35";
                        String shadowColor = (theme == Theme.LIGHT_MODE) ? "rgba(102, 126, 234, 0.3)" : "rgba(255, 107, 53, 0.3)";
                        String normalStyle = "-fx-background-color: white; -fx-text-fill: #333333; -fx-border-color: #d1d5db; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 3, 0, 0, 0); " + commonLayout;
                        String hoverStyle = "-fx-background-color: #fff5f0; -fx-text-fill: " + accentColor + "; -fx-border-color: " + accentColor + "; -fx-effect: dropshadow(three-pass-box, " + shadowColor + ", 5, 0, 0, 0); " + commonLayout;
                        btn.setStyle(normalStyle);
                        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
                        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
                    } else {
                        btn.setStyle(commonLayout); btn.setOnMouseEntered(null); btn.setOnMouseExited(null);
                    }
                }
            }

            // --- 7. 开关 & 8. 滑块 (保持之前逻辑) ---
            if (node instanceof ToggleButton) {
                ToggleButton tb = (ToggleButton) node;
                if (theme == Theme.LIGHT_MODE || theme == Theme.DARK_MODE) {
                    String accentColor = (theme == Theme.LIGHT_MODE) ? "#e5e7eb" : "#ffe0b2";
                    String layout = "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6 10; -fx-font-weight: bold;";
                    String selectedStyle = "-fx-background-color: " + accentColor + "; -fx-text-fill: #333333; -fx-border-color: #d1d5db; " + layout;
                    String normalStyle   = "-fx-background-color: transparent; -fx-text-fill: #333333; -fx-border-color: #d1d5db; " + layout;
                    tb.setStyle(tb.isSelected() ? selectedStyle : normalStyle);
                    tb.selectedProperty().addListener((o, old, isSelected) -> {
                        if (currentTheme == Theme.LIGHT_MODE || currentTheme == Theme.DARK_MODE) tb.setStyle(isSelected ? selectedStyle : normalStyle);
                    });
                    tb.setOnMouseEntered(e -> {
                        String hoverBorder = (theme == Theme.LIGHT_MODE) ? "#667eea" : "#ff6b35";
                        if (!tb.isSelected()) tb.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-text-fill: " + hoverBorder + "; -fx-border-color: " + hoverBorder + "; " + layout);
                    });
                    tb.setOnMouseExited(e -> { if (!tb.isSelected()) tb.setStyle(normalStyle); });
                } else {
                    tb.setStyle(""); tb.setOnMouseEntered(null); tb.setOnMouseExited(null);
                }
            }
            if (node instanceof Slider) {
                Slider s = (Slider) node;
                String accent = (theme == Theme.LIGHT_MODE) ? "#7f5af0" : (theme == Theme.DARK_MODE ? "#ff6b35" : "#00ffc8");
                s.setStyle("-fx-base: " + accent + ";");
                String tickColor = (theme == Theme.LIGHT_MODE || theme == Theme.DARK_MODE) ? "#333333" : "white";
                javafx.application.Platform.runLater(() -> {
                    Node axis = s.lookup(".axis");
                    if (axis != null) axis.setStyle("-fx-tick-label-fill: " + tickColor + ";");
                });
            }

            for (Node child : parent.getChildrenUnmodifiable()) {
                updateRecursiveStyle(child, cardBg, textColor, titleColor, theme);
            }
        }
    }

    /**
     * 更新面板内的组件样式
     */
    private void updatePanelComponents(VBox panel, Theme theme) {
        // ... 现有代码保持不变 ...
        for (Node node : panel.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                String text = label.getText();
                if (text.contains("🎛") || text.contains("🔄") || text.contains("✨") ||
                        text.contains("🤖") || text.contains("📜") || text.contains("ℹ️") ||
                        text.contains("⚡") || text.contains("✏️") || text.contains("✂️")) {
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
        // ... 现有代码保持不变 ...
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
        // 跳过控制条的小按钮
        if (button.getParent() != null && "control-buttons".equals(button.getParent().getId())) return;
        // 跳过功能图标按钮（那些 createIconButton 创建的）
        if (button.getText() != null && (button.getText().equals(" ➕ ") || button.getText().equals(" ➖ "))) return;

        String gradient;
        switch (theme) {
            case LIGHT_MODE: gradient = "linear-gradient(to right, #667eea, #764ba2)"; break;
            case DARK_MODE: gradient = "linear-gradient(to right, #7b2cbf, #9d4edd)"; break;
            case CYBERPUNK: gradient = "linear-gradient(to right, #00ff41, #00cc33)"; break;
            case BLUE_NIGHT: gradient = "linear-gradient(to right, #0ea5e9, #3b82f6)"; break;
            case GREEN_FOREST: gradient = "linear-gradient(to right, #10b981, #059669)"; break;
            case PURPLE_DREAM: gradient = "linear-gradient(to right, #8b5cf6, #7c3aed)"; break;
            case ORANGE_SUNSET: gradient = "linear-gradient(to right, #f97316, #ea580c)"; break;
            case PINK_BLOSSOM: gradient = "linear-gradient(to right, #ec4899, #db2777)"; break;
            default: gradient = "linear-gradient(to right, #667eea, #764ba2)";
        }

        String textColor = (theme == Theme.CYBERPUNK) ? "black" : "white";
        // 如果是图标按钮（createIconButton创建的），使用浅色背景
        if (button.getStyle().contains("-fx-border-color")) {
            // 保持 createIconButton 的逻辑，或者在这里统一覆盖
            // 为了保持 new ui 的逻辑，这里我们只覆盖普通的长按钮（如“应用”、“批量处理”）
            if (button.getPrefWidth() == Double.MAX_VALUE || button.getText().contains("打开") || button.getText().contains("保存")) {
                button.setStyle("-fx-background-color: " + gradient + "; -fx-text-fill: " + textColor + "; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
            }
        } else {
            // 普通功能按钮
            button.setStyle("-fx-background-color: " + gradient + "; -fx-text-fill: " + textColor + "; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        }
    }

    /**
     * 更新分隔符样式
     */
    private void updateSeparatorStyle(Separator separator, Theme theme) {
        // ... 现有代码保持不变 ...
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
        // ... 现有代码保持不变 ...
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

        // 1. 构建根容器
        VBox splashRoot = new VBox(20);
        splashRoot.getStyleClass().add("splash-root"); // CSS 类
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setPrefSize(550, 380);

        // 2. Logo 图标 (圆圈背景 + Emoji)
        StackPane logoPane = new StackPane();
        Circle bg = new Circle(50);
        bg.getStyleClass().add("splash-logo-bg");
        Label logoIcon = new Label("✨");
        logoIcon.setStyle("-fx-font-size: 55px;");
        logoPane.getChildren().addAll(bg, logoIcon);

        // 3. 标题文字
        Label titleLabel = new Label("Pro Image Editor");
        titleLabel.getStyleClass().add("splash-title");

        Label subtitleLabel = new Label("ULTIMATE EDITION");
        subtitleLabel.getStyleClass().add("splash-subtitle");

        // 4. 进度条
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20, 50, 0, 50));

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("splash-progress-bar"); // 专用样式

        Label loadingLabel = new Label("Initializing Core Modules...");
        loadingLabel.getStyleClass().add("splash-loading-text");

        progressBox.getChildren().addAll(progressBar, loadingLabel);
        splashRoot.getChildren().addAll(logoPane, titleLabel, subtitleLabel, progressBox);

        Scene splashScene = new Scene(splashRoot);

        // 5. [关键] 为启动页单独加载 CSS
        // 我们直接复用之前写的硬盘查找逻辑，确保 CSS 绝对能加载上
        try {
            String localPath = "src/resources/styles/main.css";
            java.io.File cssFile = new java.io.File(localPath);
            if (cssFile.exists()) {
                splashScene.getStylesheets().add(cssFile.toURI().toString());
            } else {
                java.net.URL url = getClass().getResource("/styles/main.css");
                if (url != null) splashScene.getStylesheets().add(url.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("启动页 CSS 加载失败: " + e.getMessage());
        }

        // 去掉窗口边框，背景透明
        splashStage.setScene(splashScene);
        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        // 6. 简单的模拟加载动画
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
                    Thread.sleep(20); // 模拟耗时
                }

                // 结束后淡出
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
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * 初始化主窗口
     */
    private void initializeMainWindow() {
        // 1. 初始化后端服务
        try {
            imageEditorService = new ImageEditorService();
        } catch (Exception e) {
            showError("初始化失败", "无法启动图像编辑服务: " + e.getMessage());
        }

        // 2. 创建核心布局
        root = new BorderPane();
        root.setTop(createTopBar());
        root.setLeft(createLeftPanel());
        root.setCenter(createCenterPanel());
        root.setRight(createRightPanel());
        root.setBottom(createBottomBar());

        // 3. [关键修复] 创建层叠根容器 (StackPane)
        // 注意：这里先只放入 root，不要放 null 的 loadingOverlay
        StackPane rootContainer = new StackPane(root);

        // 4. 初始化 Toast 容器 (确保不为空)
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.BOTTOM_CENTER);
        toastContainer.setPadding(new Insets(0, 0, 80, 0)); // 距离底部 80px
        toastContainer.setMouseTransparent(true); // 允许鼠标穿透点击下方内容

        // 将 Toast 容器添加到最上层
        rootContainer.getChildren().add(toastContainer);

        // 5. 创建场景并显示
        mainScene = new Scene(rootContainer, 1600, 950);

        // 尝试加载 CSS (如果有的话)
        try {
            // 定义硬盘上的源码路径 (用于开发环境调试)
            String localPath = "src/resources/styles/main.css";
            java.io.File cssFile = new java.io.File(localPath);
            if (cssFile.exists()) {
                mainScene.getStylesheets().add(cssFile.toURI().toString());
            } else {
                // 备用：尝试从 classpath 加载
                java.net.URL url = getClass().getResource("/styles/main.css");
                if (url != null) mainScene.getStylesheets().add(url.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("CSS 加载警告: " + e.getMessage());
        }

        primaryStage.setScene(mainScene);

        // 应用默认主题
        Platform.runLater(() -> applyTheme(Theme.LIGHT_MODE));

        // 设置舞台
        primaryStage.setTitle("Pro Image Editor - Ultimate Edition");
        primaryStage.setMaximized(true);

        // 添加快捷键
        setupShortcuts(root); // 注意这里传 root (BorderPane) 还是 rootContainer 都可以，主要是为了获取 Scene

        primaryStage.show();

        // 入场动画
        playEntryAnimation(root);
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts(BorderPane root) {
        // ... 现有代码保持不变 ...
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
        // ... 现有代码保持不变 ...
        Dialog<Theme> dialog = new Dialog<>();
        dialog.setTitle("选择主题");
        dialog.setHeaderText("选择界面主题");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🎨 选择主题");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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
        // ... 现有代码保持不变 ...
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
        // ... 现有代码保持不变 ...
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

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));

        // --- Logo 区域 ---
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        // 图标
        Label logoIcon = new Label("✨");
        logoIcon.getStyleClass().add("app-logo-icon"); // 添加 CSS 类

        // 标题
        Label appTitle = new Label("Pro Image Editor");
        appTitle.getStyleClass().add("app-logo-text"); // 添加 CSS 类

        logoBox.getChildren().addAll(logoIcon, appTitle);

        // --- 中间占位 ---
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- 右侧按钮 ---
        HBox rightActions = new HBox(15);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        // 功能按钮 (保持原有逻辑)
        Button undoBtn = createIconButton("↩️", "撤销");   undoBtn.setOnAction(e -> undo());
        Button redoBtn = createIconButton("↪️", "重做");   redoBtn.setOnAction(e -> redo());
        Button openBtn = createIconButton("📂", "打开");   openBtn.setOnAction(e -> openImage());

        Button saveBtn = new Button("💾 保存");
        saveBtn.getStyleClass().add("save-btn"); // 专门的保存按钮样式
        saveBtn.setOnAction(e -> saveImage());

        Button themeBtn = createIconButton("🌗", "主题");  themeBtn.setOnAction(e -> showThemeSelectionDialog());
        Button helpBtn = createIconButton("❓", "关于");   helpBtn.setOnAction(e -> showHelp());

        // 应用图标按钮通用样式
        for(Button b : new Button[]{undoBtn, redoBtn, openBtn, themeBtn, helpBtn}) {
            b.getStyleClass().add("icon-action-btn");
        }

        rightActions.getChildren().addAll(undoBtn, redoBtn, new Separator(Orientation.VERTICAL), openBtn, saveBtn, new Separator(Orientation.VERTICAL), themeBtn, helpBtn);
        topBar.getChildren().addAll(logoBox, spacer, rightActions);

        return topBar;
    }

    private void showThemeSelectionDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("主题工坊");
        try { if (mainScene != null) dialog.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets()); } catch (Exception e) {}

        // 1. 头部设计
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));
        Label icon = new Label("🎨");
        icon.setStyle("-fx-font-size: 40px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        Label title = new Label("界面风格");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subtitle = new Label("选择最适合你心情的配色方案");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        header.getChildren().addAll(icon, title, subtitle);

        // 2. 主题网格列表
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));
        grid.setAlignment(Pos.CENTER);

        Theme[] themes = Theme.values();
        for (int i = 0; i < themes.length; i++) {
            Theme theme = themes[i];
            Node card = createThemeCard(theme, () -> {
                applyTheme(theme);
                dialog.close(); // 选择后关闭弹窗
            });
            grid.add(card, i % 2, i / 2); // 每行显示2个
        }

        // 包装在一个滚动容器里，防止主题太多显示不下
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(320); // 限制高度
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        // 隐藏滚动条背景
        scroll.getStyleClass().add("edge-to-edge");

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setPrefWidth(400);
        content.getChildren().addAll(header, scroll);

        dialog.getDialogPane().setContent(content);

        // 关闭按钮
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setVisible(false); closeBtn.setManaged(false);

        dialog.showAndWait();
    }

    /**
     * [辅助] 创建单个主题预览卡片
     */
    private Node createThemeCard(Theme theme, Runnable onSelect) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setPrefWidth(160);
        // 默认样式
        String normalStyle = "-fx-background-color: #f9fafb; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #667eea; -fx-border-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(102,126,234,0.2), 10, 0, 0, 0);";

        card.setStyle(normalStyle);

        // 颜色预览圆点 (获取该主题的代表色)
        HBox colors = new HBox(-5); // 负间距实现重叠效果
        Color[] themeColors = getThemeColors(theme); // 确保你类里有 getThemeColors 方法
        for (Color c : themeColors) {
            Circle circle = new Circle(8, c);
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(2);
            colors.getChildren().add(circle);
        }

        Label name = new Label(theme.getDisplayName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151; -fx-font-size: 13px;");

        card.getChildren().addAll(colors, name);

        // 交互事件
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));
        card.setOnMouseClicked(e -> onSelect.run());

        return card;
    }

    private ScrollPane createLeftPanel() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300); // //稍微加宽一点

        // 1. 基础调整卡片
        VBox adjustmentPanel = createAdvancedAdjustmentPanel();
        // 注意：createAdvancedAdjustmentPanel 内部原本有 title，建议修改该方法去掉内部的 title，或者在这里忽略
        VBox basicCard = createCard("🎛  基础调整", adjustmentPanel);

        // 2. 交互工具卡片
        ToggleGroup toolGroup = new ToggleGroup();

        // 使用网格布局让工具按钮更整齐
        GridPane toolGrid = new GridPane();
        toolGrid.setHgap(10);
        toolGrid.setVgap(10);

        ToggleButton selectTool = createToolButton("👆 选择", ToolMode.SELECT, toolGroup);
        ToggleButton cropTool = createToolButton("✂️ 裁剪", ToolMode.CROP, toolGroup);
        ToggleButton brushTool = createToolButton("🖌️ 画笔", ToolMode.DRAW_BRUSH, toolGroup);
        ToggleButton textTool = createToolButton("A  文字", ToolMode.DRAW_TEXT, toolGroup);
        ToggleButton rectTool = createToolButton("⬜ 矩形", ToolMode.DRAW_RECT, toolGroup);
        ToggleButton circleTool = createToolButton("⭕ 圆形", ToolMode.DRAW_CIRCLE, toolGroup);

        toolGrid.add(selectTool, 0, 0); toolGrid.add(cropTool, 1, 0);
        toolGrid.add(brushTool, 0, 1);  toolGrid.add(textTool, 1, 1);
        toolGrid.add(rectTool, 0, 2);   toolGrid.add(circleTool, 1, 2);

        // 绘图设置面板 (默认隐藏)
        VBox drawingSettings = createDrawingSettingsPanel();
        drawingSettings.setVisible(false);
        toolGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDrawingTool = newVal == brushTool || newVal == rectTool || newVal == circleTool || newVal == textTool;
            drawingSettings.setVisible(isDrawingTool);
            // 动态调整布局，避免留白
            if (!isDrawingTool) {
                drawingSettings.setManaged(false);
            } else {
                drawingSettings.setManaged(true);
            }
        });
        drawingSettings.setManaged(false); // 初始状态不占位

        VBox toolsCard = createCard("🛠️  交互工具", toolGrid, drawingSettings);

        // 3. 变换与批量卡片
        // 变换按钮
        GridPane transGrid = new GridPane();
        transGrid.setHgap(10); transGrid.setVgap(10);
        transGrid.add(createOperationButton("⟳ 90°", e->rotate90()), 0, 0);
        transGrid.add(createOperationButton("⟳ 180°", e->rotate180()), 1, 0);
        transGrid.add(createOperationButton("⇄ 水平", e->flipHorizontal()), 0, 1);
        transGrid.add(createOperationButton("⇅ 垂直", e->flipVertical()), 1, 1);

        // 批量按钮
        Button batchBtn = new Button("批量处理图片");
        batchBtn.setPrefWidth(Double.MAX_VALUE);
        batchBtn.setOnAction(e -> startBatchProcessing());
        // 给批量按钮特殊样式（稍微显眼点）
        batchBtn.setStyle("-fx-background-color: linear-gradient(to right, #4facfe, #00f2fe); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        VBox transCard = createCard("🔄  变换 & 批量", transGrid, new Separator(), batchBtn);

        // 4. 滤镜卡片
        VBox blurControl = createSliderControl("模糊程度", 0, 10, 0, this::applyBlur);
        Button grayscaleBtn = createOperationButton("⚫  灰度化", e->applyGrayscale());
        Button edgeDetectBtn = createOperationButton("🔲  边缘检测", e->detectEdges());

        // 将按钮横向排列节省空间
        HBox filterBtns = new HBox(10, grayscaleBtn, edgeDetectBtn);
        HBox.setHgrow(grayscaleBtn, Priority.ALWAYS);
        HBox.setHgrow(edgeDetectBtn, Priority.ALWAYS);
        grayscaleBtn.setMaxWidth(Double.MAX_VALUE);
        edgeDetectBtn.setMaxWidth(Double.MAX_VALUE);

        VBox filterCard = createCard("✨  滤镜特效", blurControl, filterBtns);

        // 5. AI 增强卡片
        Button aiEnhanceBtn = createAIButton("✨  AI 智能增强", e->aiEnhance(), "#845ec2");
        Button removeBgBtn = createAIButton("🖼  一键移除背景", e->removeBackground(), "#ff9671");
        Button styleBtn = createAIButton("🎨  艺术风格迁移", e->applyArtisticStyle(), "#ffc75f");

        VBox aiCard = createCard("🤖  AI 实验室", aiEnhanceBtn, removeBgBtn, styleBtn);

        // 豆包 AI
        if (arkAvailable) {
            Button arkBtn = createAIButton("🌌  豆包图生图", e->showArkImageGenerationDialog(), "#0081cf");
            aiCard.getChildren().add(arkBtn);
        }

        // 将所有卡片添加到左侧面板
        content.getChildren().addAll(basicCard, toolsCard, transCard, filterCard, aiCard);

        leftPanel = content; // 更新成员变量
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        // 隐藏滚动条背景，使其更自然
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    // [新增辅助] 创建工具栏 ToggleButton
    private ToggleButton createToolButton(String text, ToolMode mode, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setPrefWidth(110); // 固定宽度让网格整齐
        btn.setOnAction(e -> setToolMode(mode));
        if (mode == ToolMode.SELECT) btn.setSelected(true);
        return btn;
    }

    // [新增辅助] 创建普通操作按钮的简写
    private Button createOperationButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = createOperationButton(text); // 调用原有的样式方法
        btn.setOnAction(action);
        btn.setMaxWidth(Double.MAX_VALUE); // 自动填满
        return btn;
    }

    // [新增辅助] 创建 AI 专用多彩按钮
    private Button createAIButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action, String colorHex) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(action);
        // 默认样式，会被 Theme 覆盖，但我们可以给 AI 按钮保留一点特殊色
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 10;");
        return btn;
    }

    /**
     * 显示豆包图生图对话框
     */
    private void showArkImageGenerationDialog() {
        if (!arkAvailable) {
            showError("功能未就绪", "请检查 config.properties 配置");
            return;
        }
        if (currentImageFile == null) {
            showError("提示", "请先在主界面加载一张参考图片");
            return;
        }

        // 1. 创建对话框
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("豆包图生图 - AI 创作中心");

        // [关键] 获取主场景的样式表，应用到弹窗
        try {
            if (mainScene != null) {
                dialog.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets());
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. 自定义头部 (替代默认 Header)
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 15, 0));
        Label iconLbl = new Label("🎨");
        iconLbl.setStyle("-fx-font-size: 40px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        Label titleLbl = new Label("AI 灵感创作");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subTitleLbl = new Label("基于 " + currentImageFile.getName() + " 进行再创作");
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

        // 4. 输出设置区域 (使用 GridPane 对齐)
        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10); settingsGrid.setVgap(10);
        settingsGrid.setPadding(new Insets(15));
        // 给设置区域加个浅色背景框
        settingsGrid.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");

        Label dirLabel = new Label("保存位置:");
        dirLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
        TextField dirField = new TextField("D:/generated_images/");
        Button browseBtn = new Button("📂 浏览");
        browseBtn.getStyleClass().add("small-action"); // 应用 CSS 小按钮样式
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if(f != null) dirField.setText(f.getAbsolutePath());
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
        genBtn.setStyle("-fx-font-size: 14px;"); // 基础样式由 CSS .button 控制

        // 组装主内容
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(480);
        content.getChildren().addAll(headerBox, promptBox, settingsGrid, statusBox, genBtn);

        dialog.getDialogPane().setContent(content);
        // 添加关闭按钮类型 (虽然我们主要用自定义界面，但需要这个来支持右上角X)
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // 隐藏默认的底部按钮栏，因为我们自己画了按钮
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        // 7. 生成逻辑
        genBtn.setOnAction(e -> {
            String prompt = pArea.getText().trim();
            if(prompt.isEmpty()) {
                pArea.setStyle("-fx-border-color: #ff5252;"); // 错误红框
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
                    String url = generateArkImage(currentImageFile.getAbsolutePath(), prompt, saveDir, fileName);

                    Platform.runLater(() -> {
                        statusLabel.setText("✅ 生成成功！");
                        pBar.setVisible(false);

                        // 显示成功弹窗 (这里也可以美化，暂且用 Alert)
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("创作完成");
                        alert.setHeaderText("您的 AI 作品已生成");
                        alert.setContentText("保存路径: " + url + "\n\n是否立即在编辑器中打开？");
                        // 尝试给 Alert 也加样式
                        try { alert.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets()); } catch(Exception ex){}

                        alert.showAndWait().ifPresent(r -> {
                            if(r == ButtonType.OK) {
                                loadImage(new File(url));
                                dialog.close();
                            } else {
                                //如果不打开，解锁界面允许再次生成
                                pArea.setDisable(false);
                                settingsGrid.setDisable(false);
                                genBtn.setDisable(false);
                                genBtn.setText("🔄  再来一张");
                                nameField.setText("ai_art_" + System.currentTimeMillis());
                            }
                        });
                    });
                } catch(Exception ex) {
                    Platform.runLater(() -> {
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
     * 执行豆包图生图生成
     */
    private String generateArkImage(String imagePath, String prompt, String saveDir, String fileName) throws Exception {
        // 从配置中获取参数
        String apiKey = arkConfig.getProperty("ark.api.key");
        String baseUrl = arkConfig.getProperty("ark.base.url");
        String modelId = arkConfig.getProperty("ark.model.id");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("API Key 未配置");
        }

        // 1. 图片转标准Base64
        String imageBase64 = imageToBase64(imagePath);

        // 2. 构建ArkService
        okhttp3.ConnectionPool connectionPool = new okhttp3.ConnectionPool(5, 1, TimeUnit.SECONDS);
        okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher();
        com.volcengine.ark.runtime.service.ArkService service = com.volcengine.ark.runtime.service.ArkService.builder()
                .baseUrl(baseUrl)
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();

        try {
            // 3. 构建图生图请求
            com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest generateRequest =
                    com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest.builder()
                            .model(modelId)
                            .prompt(prompt)
                            .image(imageBase64)
                            .size("2K")
                            .sequentialImageGeneration("disabled")
                            .responseFormat(com.volcengine.ark.runtime.model.images.generation.ResponseFormat.Url)
                            .stream(false)
                            .watermark(false)
                            .build();

            // 4. 调用API
            System.out.println("正在调用豆包图生图API...");
            com.volcengine.ark.runtime.model.images.generation.ImagesResponse imagesResponse =
                    service.generateImages(generateRequest);

            if (imagesResponse.getData() != null && !imagesResponse.getData().isEmpty()) {
                String imageUrl = imagesResponse.getData().get(0).getUrl();
                System.out.println("图生图成功！生成的图片URL：" + imageUrl);

                // 5. 下载并保存图片
                return downloadArkImage(imageUrl, saveDir, fileName);
            } else {
                throw new Exception("生成结果为空");
            }
        } finally {
            service.shutdownExecutor();
        }
    }

    /**
     * 本地图片转标准Base64
     */
    private String imageToBase64(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("图片文件不存在：" + imagePath);
        }

        // 校验图片大小（≤10MB）
        long fileSizeMB = imageFile.length() / (1024 * 1024);
        if (fileSizeMB > 10) {
            throw new IOException("图片大小超过10MB限制，当前：" + fileSizeMB + "MB");
        }

        // 读取图片字节
        byte[] imageBytes = FileUtils.readFileToByteArray(imageFile);
        // 提取图片格式
        String imageFormat = getImageFormat(imagePath);
        // 拼接标准Base64前缀
        return "data:image/" + imageFormat + ";base64," + Base64.encodeBase64String(imageBytes);
    }

    /**
     * 提取图片格式
     */
    private String getImageFormat(String imagePath) {
        String suffix = imagePath.substring(imagePath.lastIndexOf(".") + 1).toLowerCase();
        return "jpeg".equals(suffix) ? "jpg" : suffix;
    }

    /**
     * 过滤非法文件名字符
     */
    private String filterIllegalFileName(String fileName) {
        String illegalChars = "[\\\\/:*?\"<>|]";
        Pattern pattern = Pattern.compile(illegalChars);
        return pattern.matcher(fileName).replaceAll("_");
    }

    /**
     * 从URL中提取纯图片路径
     */
    private String getPureImageUrl(String imageUrl) {
        if (imageUrl.contains("?")) {
            return imageUrl.split("\\?")[0];
        }
        return imageUrl;
    }

    /**
     * 下载图片并保存到本地
     */
    private String downloadArkImage(String imageUrl, String saveDir, String fileName) throws IOException {
        // 1. 处理URL：去掉TOS签名参数
        String pureImageUrl = getPureImageUrl(imageUrl);

        // 2. 创建保存目录
        File dir = new File(saveDir);
        if (!dir.exists()) {
            boolean mkdirSuccess = dir.mkdirs();
            if (!mkdirSuccess) {
                throw new IOException("创建保存目录失败：" + saveDir);
            }
        }

        // 3. 构建OkHttpClient
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 4. 发送请求下载图片
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载图片失败，HTTP状态码：" + response.code());
            }

            // 5. 提取图片格式
            String imageFormat = pureImageUrl.substring(pureImageUrl.lastIndexOf(".") + 1).toLowerCase();
            // 过滤文件名中的非法字符
            String safeFileName = filterIllegalFileName(fileName);
            // 补全文件名后缀
            String fullFileName = safeFileName.endsWith("." + imageFormat)
                    ? safeFileName
                    : safeFileName + "." + imageFormat;
            // 拼接最终保存路径
            File saveFile = new File(dir, fullFileName);

            // 6. 写入文件
            try (java.io.InputStream inputStream = response.body().byteStream()) {
                FileUtils.copyInputStreamToFile(inputStream, saveFile);
            }
            return saveFile.getAbsolutePath();
        }
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
        dialog.setTitle("批量工坊");
        try { if (mainScene != null) dialog.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets()); } catch (Exception e) {}

        // 头部
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0,0,20,0));
        Label icon = new Label("🏭");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label("批量图像处理流水线");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subtitle = new Label("已就绪队列: " + files.size() + " 个文件");
        subtitle.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold; -fx-background-color: #f0f4ff; -fx-padding: 4 10; -fx-background-radius: 12;");
        header.getChildren().addAll(icon, title, subtitle);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setPrefWidth(450);

        // 1. 操作选择卡片
        VBox opCard = new VBox(10);
        opCard.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");
        Label opLabel = new Label("选择流水线操作:");
        opLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        ComboBox<String> opCombo = new ComboBox<>();
        opCombo.getItems().addAll("灰度化", "调整亮度", "调整对比度", "调整饱和度", "模糊", "边缘检测", "旋转90度");
        opCombo.setValue("灰度化");
        opCombo.setMaxWidth(Double.MAX_VALUE);

        // 参数滑块 (默认隐藏)
        VBox paramBox = new VBox(5);
        paramBox.setVisible(false);
        paramBox.setManaged(false); // 不占位
        Label paramLbl = new Label("强度参数:");
        paramLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        Slider paramSlider = new Slider(-100, 100, 0);
        paramBox.getChildren().addAll(paramLbl, paramSlider);

        opCombo.setOnAction(e -> {
            String val = opCombo.getValue();
            boolean showSlider = val.contains("亮度") || val.contains("对比度") || val.contains("饱和度") || val.contains("模糊");
            paramBox.setVisible(showSlider);
            paramBox.setManaged(showSlider);
        });

        opCard.getChildren().addAll(opLabel, opCombo, paramBox);

        // 2. 输出设置卡片
        VBox outCard = new VBox(10);
        outCard.setStyle("-fx-background-color: #f9fafb; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");
        Label outLabel = new Label("输出命名规则:");
        outLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        TextField suffixField = new TextField("_processed");
        suffixField.setPromptText("例如: _edit, _v2");
        outCard.getChildren().addAll(outLabel, suffixField);

        // 按钮
        Button startBtn = new Button("🚀  启动流水线");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setPrefHeight(45);
        // 基础样式由CSS控制

        content.getChildren().addAll(header, opCard, outCard, startBtn);
        dialog.getDialogPane().setContent(content);

        // 关闭按钮逻辑
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setVisible(false); closeBtn.setManaged(false);

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
        centerPane.setId("center-pane");

        // 1. 图像容器 (和之前保持一致)
        VBox imageContainer = new VBox(20);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(30));

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.3)));

        Pane interactionOverlay = new Pane();
        interactionOverlay.setStyle("-fx-background-color: transparent;");

        Canvas selectionCanvas = new Canvas();
        selectionCanvas.setMouseTransparent(true);
        selectionCanvas.setId("selection-canvas");

        StackPane imagePane = new StackPane(imageView, selectionCanvas, interactionOverlay);
        setupMouseInteraction(interactionOverlay, selectionCanvas);

        // 控制按钮条
        HBox controlButtons = new HBox(15);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setId("control-buttons");
        controlButtons.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 30; -fx-padding: 8 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0, 5, 0, 0);");

        // ... 按钮创建代码保持不变 ...
        Button zoomIn = createIconButton("➕", "放大"); zoomIn.setOnAction(e -> zoomIn());
        Button zoomOut = createIconButton("➖", "缩小"); zoomOut.setOnAction(e -> zoomOut());
        Button zoomFit = createIconButton("⛶", "适应窗口"); zoomFit.setOnAction(e -> fitToWindow());
        Button zoom100 = createIconButton("1:1", "原始大小"); zoom100.setOnAction(e -> resetZoom());
        Button confirmCropBtn = createIconButton("✓", "确认裁剪");
        confirmCropBtn.setVisible(false);
        confirmCropBtn.setOnAction(e -> applyCropSelection());
        confirmCropBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 50;"); // 特殊绿色

        controlButtons.getChildren().addAll(zoomIn, zoomOut, zoomFit, zoom100, confirmCropBtn);
        imageContainer.getChildren().addAll(imagePane, controlButtons);

        imageScrollPane = new ScrollPane(imageContainer);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        imageScrollPane.setId("image-scroll-pane");

        // 2. [关键修改] 美化的上传占位符
        VBox placeholder = new VBox(15);
        placeholder.setId("placeholder");
        placeholder.getStyleClass().add("upload-zone"); // 应用 CSS 虚线框样式
        placeholder.setMaxSize(500, 350); // 限制最大尺寸

        Label icon = new Label("☁️"); // 或者用 "📷"
        icon.getStyleClass().add("upload-icon");

        Label text = new Label("拖放图片到此处");
        text.getStyleClass().add("upload-hint-title");

        Label subText = new Label("或者点击此区域打开文件");
        subText.getStyleClass().add("upload-hint-sub");

        Button openBtn = new Button("📂 选择文件");
        openBtn.getStyleClass().add("save-btn"); // 复用之前的紫色按钮样式
        openBtn.setMouseTransparent(true); // 让点击事件穿透给 VBox

        placeholder.getChildren().addAll(icon, text, subText, openBtn);

        // 让整个区域都能点击打开图片
        placeholder.setOnMouseClicked(e -> openImage());

        // 添加拖拽支持
        placeholder.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                // 拖拽悬浮时的临时样式
                placeholder.setStyle("-fx-border-color: #00ffc8; -fx-background-color: rgba(0, 255, 200, 0.1);");
            }
            event.consume();
        });

        placeholder.setOnDragExited(event -> {
            // 恢复默认 CSS 样式 (清除 inline style)
            placeholder.setStyle("");
            event.consume();
        });

        placeholder.setOnDragDropped(event -> {
            var db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                loadImage(file);
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        // 初始状态
        imageScrollPane.setVisible(false);
        controlButtons.setVisible(false);

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
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);

        // 1. 操作历史卡片
        // 优化 ListView 样式，使其融入卡片
        historyListView = new ListView<>();
        historyListView.setPrefHeight(250);
        historyListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        // 给 ListView 加个 ID，方便 CSS 进一步去除默认边框
        historyListView.setId("history-list");

        // 清空历史按钮 (放在标题栏旁边或底部，这里放在底部)
        Button clearHistoryBtn = new Button("清空记录");
        clearHistoryBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 5 0;");
        clearHistoryBtn.setOnAction(e -> {
            historyListView.getItems().clear();
            updateStatus("历史记录已清空");
        });

        VBox historyCard = createCard("📜  操作时光机", historyListView, clearHistoryBtn);

        // 2. 图像信息卡片 (使用 GridPane 对齐)
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);

        // 创建信息标签的辅助方法
        addInfoRow(infoGrid, 0, "📏 尺寸", "size-label", "-- x --");
        addInfoRow(infoGrid, 1, "📁 格式", "format-label", "--");
        addInfoRow(infoGrid, 2, "💾 大小", "filesize-label", "-- MB");

        VBox infoCard = createCard("ℹ️  图像档案", infoGrid);

        // 3. 快捷操作卡片
        Button resetBtn = createOperationButton("🔄  重置图片", e -> resetImage());
        // 给重置按钮一个警示色（淡红）
        resetBtn.setStyle("-fx-background-color: rgba(255, 82, 82, 0.1); -fx-text-fill: #ff5252; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");

        Button clearBtn = createOperationButton("🗑️  清空画布", e -> clearCanvas());

        VBox quickCard = createCard("⚡  快捷指令", resetBtn, clearBtn);

        // 添加所有卡片
        content.getChildren().addAll(historyCard, infoCard, quickCard);

        // 赋值给成员变量
        rightPanel = content;

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    // [新增辅助] 快速添加信息行
    private void addInfoRow(GridPane grid, int row, String title, String valueId, String defaultValue) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        Label v = new Label(defaultValue);
        v.setId(valueId); // 务必设置ID，方便 updateCenterPanelStyle 或其他逻辑更新数值
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        grid.add(t, 0, row);
        grid.add(v, 1, row);
    }

    /**
     * 创建底部状态栏
     */
    private HBox createBottomBar() {
        HBox bottomBar = new HBox(20);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.getStyleClass().add("floating-bottom-bar"); // 应用胶囊样式

        // 这里的颜色会在 updateRecursiveStyle 中动态控制，为了透明度效果
        bottomBar.setId("bottom-capsule");

        // 1. 状态信息
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. 缩放滑块 (增加一个小图标)
        Label zoomIcon = new Label("🔍");
        zoomIcon.setStyle("-fx-font-size: 14px; -fx-opacity: 0.7;");

        Slider zoomSlider = new Slider(0.1, 3.0, 1.0);
        zoomSlider.setPrefWidth(150);
        zoomSlider.setShowTickLabels(false);
        zoomSlider.setShowTickMarks(false);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (imageView.getImage() != null) {
                imageView.setScaleX(newVal.doubleValue());
                imageView.setScaleY(newVal.doubleValue());
                statusLabel.setText(String.format("缩放: %.0f%%", newVal.doubleValue() * 100));
            }
        });

        bottomBar.getChildren().addAll(statusLabel, spacer, zoomIcon, zoomSlider);

        // 为了让它悬浮，我们给它加一点 margin，不要贴底
        HBox.setMargin(bottomBar, new Insets(0, 20, 20, 20)); // 下边距 20px
        bottomBar.setMaxWidth(800); // 限制最大宽度，显得更精致

        return bottomBar;
    }

    // ==================== UI辅助方法 ====================

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

    /**
     * [新增] 创建卡片式容器
     * 用于将功能分组，提供圆角、背景色和阴影，提升视觉层次感
     */
    private VBox createCard(String title, Node... nodes) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(15));
        // 初始样式（稍后会被 updatePanelStyles 覆盖以适应主题）
        card.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");
        // 给卡片打上标签，方便主题切换时识别
        card.setId("content-card");

        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            // 使用更现代的标题样式
            titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-opacity: 0.8;");
            // 标记为标题标签，方便主题更新颜色
            titleLabel.setId("card-title");
            card.getChildren().add(titleLabel);
        }

        // 如果传入的是节点数组，添加到卡片中
        if (nodes != null) {
            for (Node node : nodes) {
                card.getChildren().add(node);
            }
        }
        return card;
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
        if (currentImage == null) {
            showError("提示", "请先加载图片");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("艺术画廊");
        try { if (mainScene != null) dialog.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets()); } catch (Exception e) {}

        // --- 1. 头部设计 (橙色主题) ---
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label icon = new Label("🎨");
        icon.setStyle("-fx-font-size: 48px; -fx-effect: dropshadow(gaussian, rgba(255, 153, 102, 0.4), 10, 0, 0, 2);"); // 橙色光晕

        Label title = new Label("选择艺术流派");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Give your photo a creative soul");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff9966; -fx-font-weight: bold; -fx-background-color: rgba(255, 153, 102, 0.1); -fx-padding: 4 12; -fx-background-radius: 20;");

        header.getChildren().addAll(icon, title, subtitle);

        // --- 2. 风格卡片网格 ---
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));
        grid.setAlignment(Pos.CENTER);

        // 定义所有支持的风格 (名称 + 描述 + 图标/Emoji)
        // 你可以根据需要扩展这个列表
        addStyleCard(grid, 0, 0, "油画", "Oil Painting", "🖼️", "厚重的笔触与质感", dialog, ArtisticStyleOperation.ArtisticStyle.OIL_PAINTING);
        addStyleCard(grid, 1, 0, "水彩", "Watercolor", "💧", "清透晕染的效果", dialog, ArtisticStyleOperation.ArtisticStyle.WATERCOLOR);
        addStyleCard(grid, 0, 1, "素描", "Sketch", "✏️", "纯粹的黑白线条", dialog, ArtisticStyleOperation.ArtisticStyle.PENCIL_SKETCH);
        addStyleCard(grid, 1, 1, "卡通", "Cartoon", "🦄", "二次元明快色彩", dialog, ArtisticStyleOperation.ArtisticStyle.CARTOON);
        addStyleCard(grid, 0, 2, "马赛克", "Mosaic", "🧩", "像素化复古风", dialog, ArtisticStyleOperation.ArtisticStyle.MOSAIC);

        // 包装在滚动容器中
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(360);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.getStyleClass().add("edge-to-edge"); // 利用之前隐藏滚动条背景的类

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
     * [新增辅助] 创建橙色风格的艺术卡片
     */
    private void addStyleCard(GridPane grid, int col, int row, String name, String enName, String emoji, String desc, Dialog<Void> dialog, ArtisticStyleOperation.ArtisticStyle style) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setPrefWidth(160);

        // --- 样式定义 ---
        // 默认：白底灰边
        String normalStyle =
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #e5e7eb; " +
                        "-fx-border-radius: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 5, 0, 0, 0);";

        // 悬浮/激活：橙色渐变背景 + 白字
        String hoverStyle =
                "-fx-background-color: linear-gradient(to bottom right, #ff9966, #ff5e62); " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: transparent; " +
                        "-fx-border-radius: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255, 94, 98, 0.4), 10, 0, 0, 2);";

        card.setStyle(normalStyle);

        // --- 内容构建 ---
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

        // --- 交互事件 ---
        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            // 变色逻辑：文字变白
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            enLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-weight: bold;");
            descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);");
            // 简单的放大动效
            card.setTranslateY(-3);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(normalStyle);
            // 恢复颜色
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #374151;");
            enLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
            descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            card.setTranslateY(0);
        });

        card.setOnMouseClicked(e -> {
            dialog.close();
            // 应用风格
            applyOp(new ArtisticStyleOperation(style, new ArtisticStyleOperation.StyleParameters(0.7f, 5, 0.5f)));
        });

        grid.add(card, col, row);
    }

    private void applyOp(ImageOperation op) {
        // 1. 显示进度条
        showProgress("正在处理...");

        // 2. 开启新线程执行耗时操作，避免卡死 UI
        new Thread(() -> {
            try {
                // 调用 Service 层进行异步处理
                imageEditorService.applyOperationAsync(
                        op,
                        // 成功回调
                        resultImage -> Platform.runLater(() -> {
                            // 更新当前图片引用
                            currentImage = resultImage;
                            imageView.setImage(resultImage);

                            // 尝试同步更新 BufferedImage (如果你的项目架构需要)
                            try {
                                currentBufferedImage = ImageUtils.fxImageToBufferedImage(resultImage);
                            } catch (Exception ignored) {}

                            // 隐藏进度条并播放成功动画
                            hideProgress();
                            playSuccessAnimation(); // 如果你没有这个方法，可以删掉这行

                            // 记录历史 (可选)
                            // updateHistory("应用操作");
                        }),
                        // 失败回调
                        error -> Platform.runLater(() -> {
                            hideProgress();
                            showError("操作失败", error.getMessage());
                        })
                );
            } catch(Exception e) {
                // 捕获线程启动异常
                Platform.runLater(() -> {
                    hideProgress();
                    showError("系统错误", e.getMessage());
                });
            }
        }).start();
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

    private void showProgress(String message) {
        if (loadingOverlay == null) {
            // 1. 懒加载创建遮罩
            loadingOverlay = new StackPane();
            loadingOverlay.getStyleClass().add("loading-overlay");
            loadingOverlay.setVisible(false);

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);

            // 大号进度圈
            ProgressIndicator pi = new ProgressIndicator();
            pi.setPrefSize(60, 60);

            loadingText = new Label(message);
            loadingText.getStyleClass().add("loading-text");

            content.getChildren().addAll(pi, loadingText);
            loadingOverlay.getChildren().add(content);

            // 2. 智能挂载遮罩 (修复报错的核心逻辑)
            // 尝试挂载到 root 的 Center 区域 (即中间的画板 StackPane)
            if (root != null && root.getCenter() instanceof StackPane) {
                StackPane centerStack = (StackPane) root.getCenter();
                if (!centerStack.getChildren().contains(loadingOverlay)) {
                    centerStack.getChildren().add(loadingOverlay);
                }
            } else {
                // 如果找不到中间区域，尝试挂载到 Scene 的根节点
                if (mainScene != null && mainScene.getRoot() instanceof Pane) {
                    Pane sceneRoot = (Pane) mainScene.getRoot();
                    if (!sceneRoot.getChildren().contains(loadingOverlay)) {
                        sceneRoot.getChildren().add(loadingOverlay);
                    }
                }
            }
        }

        // 3. 显示遮罩
        if (loadingText != null) loadingText.setText(message);
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.toFront(); // 确保在最上层
        }
    }

    private void hideProgress() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        }
        // 同时隐藏底部的小圈（为了兼容旧代码）
        if (progressIndicator != null) progressIndicator.setVisible(false);
    }

    private void showToast(String message, String type) {
        // 延迟初始化容器（防空指针）
        if (toastContainer == null) {
            // 如果上面初始化没成功，这里做个兜底，尝试挂载到 Scene 根节点
            if (mainScene != null && mainScene.getRoot() instanceof StackPane) {
                toastContainer = new VBox(10);
                toastContainer.setAlignment(Pos.BOTTOM_CENTER);
                toastContainer.setPadding(new Insets(0, 0, 50, 0));
                toastContainer.setMouseTransparent(true);
                ((StackPane) mainScene.getRoot()).getChildren().add(toastContainer);
            } else {
                return; // 无法显示
            }
        }

        // 创建 Toast 气泡
        Label toast = new Label(message);
        toast.getStyleClass().add("toast-message");
        toast.getStyleClass().add("toast-" + type); // toast-success, toast-error

        // 初始透明
        toast.setOpacity(0);

        // 添加入队
        toastContainer.getChildren().add(toast);

        // 动画序列：淡入 -> 停留 -> 淡出 -> 移除
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.5)); // 停留 2.5 秒
        fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));

        SequentialTransition seq = new SequentialTransition(fadeIn, fadeOut);
        seq.play();
    }
    private VBox toastContainer;
    private void showSuccess(String title, String message) {
        showToast("✅ " + message, "success");
    }

    // [修改] 替换原有的 updateStatus，让重要操作也弹 Toast
    private void updateStatus(String message) {
        statusLabel.setText(message);
        // 如果是保存、处理完成等消息，顺便弹个 Toast
        if (message.contains("完成") || message.contains("成功") || message.contains("已保存")) {
            showToast(message, "info");
        }
    }

    // [修改] 简化 showError，小错误用 Toast，大错误才弹窗
    private void showError(String title, String message) {
        // 如果消息很短，用红色 Toast
        if (message.length() < 30) {
            showToast("❌ " + message, "error");
        } else {
            // 长错误信息还是弹窗，方便用户看
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            try { alert.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets()); } catch(Exception e){}
            alert.showAndWait();
        }
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showHelp() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("关于");
        try { if (mainScene != null) dialog.getDialogPane().getStylesheets().addAll(mainScene.getStylesheets()); } catch (Exception e) {}

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

    // 辅助方法：创建快捷键行
    private HBox createKeyRow(String key, String desc) {
        HBox row = new HBox(10);
        Label k = new Label(key);
        k.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-text-fill: #667eea; -fx-background-color: rgba(102,126,234,0.1); -fx-padding: 2 6; -fx-background-radius: 4;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: #4b5563;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(d, sp, k);
        return row;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "png";
    }

    private Background createCheckerboardBackground(String baseColorHex) {
        Color baseColor = Color.web(baseColorHex);
        int size = 20;
        Canvas canvas = new Canvas(size * 2, size * 2);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(baseColor);
        gc.fillRect(0, 0, size * 2, size * 2);

        // 绘制淡淡的格纹
        Color checkColor = baseColor.grayscale().getBrightness() > 0.5 ?
                baseColor.darker() : baseColor.brighter();
        gc.setFill(Color.color(checkColor.getRed(), checkColor.getGreen(), checkColor.getBlue(), 0.05));
        gc.fillRect(0, 0, size, size);
        gc.fillRect(size, size, size, size);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage patternImage = canvas.snapshot(params, null);

        return new Background(new BackgroundFill(new ImagePattern(patternImage, 0, 0, size * 2, size * 2, false), CornerRadii.EMPTY, Insets.EMPTY));
    }

    @FunctionalInterface
    interface SliderChangeListener {
        void onChange(double value);
    }

    public static void main(String[] args) {
        launch(args);
    }
}