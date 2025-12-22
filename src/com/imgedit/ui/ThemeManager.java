package imgedit.ui;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * 主题管理器 - 管理界面主题样式
 */
public class ThemeManager {

    public enum Theme {
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
    private final Map<Theme, String> themeStyles = new HashMap<>();
    private final Map<Theme, Color[]> themeColors = new HashMap<>();

    public ThemeManager() {
        initializeThemes();
    }

    private void initializeThemes() {
        // 浅色模式
        themeStyles.put(Theme.LIGHT_MODE,
                "-fx-background-color: #f5f7fa; " +
                        "-fx-text-fill: #2c3e50;"
        );
        themeColors.put(Theme.LIGHT_MODE,
                new Color[]{Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f5f7fa")});

        // 深色模式
        themeStyles.put(Theme.DARK_MODE,
                "-fx-background-color: #121212; " +
                        "-fx-text-fill: #e0e0e0;"
        );
        themeColors.put(Theme.DARK_MODE,
                new Color[]{Color.web("#7b2cbf"), Color.web("#9d4edd"), Color.web("#121212")});

        // 蓝色之夜主题
        themeStyles.put(Theme.BLUE_NIGHT,
                "-fx-background-color: #0f172a; " +
                        "-fx-text-fill: #e2e8f0;"
        );
        themeColors.put(Theme.BLUE_NIGHT,
                new Color[]{Color.web("#0ea5e9"), Color.web("#3b82f6"), Color.web("#0f172a")});

        // 绿色森林主题
        themeStyles.put(Theme.GREEN_FOREST,
                "-fx-background-color: #022c22; " +
                        "-fx-text-fill: #d1fae5;"
        );
        themeColors.put(Theme.GREEN_FOREST,
                new Color[]{Color.web("#10b981"), Color.web("#059669"), Color.web("#022c22")});

        // 紫色梦幻主题
        themeStyles.put(Theme.PURPLE_DREAM,
                "-fx-background-color: #1e1b4b; " +
                        "-fx-text-fill: #e9d5ff;"
        );
        themeColors.put(Theme.PURPLE_DREAM,
                new Color[]{Color.web("#8b5cf6"), Color.web("#7c3aed"), Color.web("#1e1b4b")});

        // 橙色日落主题
        themeStyles.put(Theme.ORANGE_SUNSET,
                "-fx-background-color: #431407; " +
                        "-fx-text-fill: #fed7aa;"
        );
        themeColors.put(Theme.ORANGE_SUNSET,
                new Color[]{Color.web("#f97316"), Color.web("#ea580c"), Color.web("#431407")});

        // 粉色花语主题
        themeStyles.put(Theme.PINK_BLOSSOM,
                "-fx-background-color: #500724; " +
                        "-fx-text-fill: #fbcfe8;"
        );
        themeColors.put(Theme.PINK_BLOSSOM,
                new Color[]{Color.web("#ec4899"), Color.web("#db2777"), Color.web("#500724")});

        // 赛博朋克主题
        themeStyles.put(Theme.CYBERPUNK,
                "-fx-background-color: #000000; " +
                        "-fx-text-fill: #00ff41;"
        );
        themeColors.put(Theme.CYBERPUNK,
                new Color[]{Color.web("#00ff41"), Color.web("#ff00ff"), Color.web("#000000")});
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void applyTheme(Theme theme, BorderPane root, VBox leftPanel, VBox rightPanel,
                           HBox topBar, HBox bottomBar, ListView<String> historyListView) {
        currentTheme = theme;

        // 获取当前主题的样式
        String style = themeStyles.get(theme);

        // 应用主题到根布局
        if (root != null) {
            root.setStyle(style);
        }

        // 更新各个面板的样式
        updatePanelStyles(theme, leftPanel, rightPanel, topBar, bottomBar, historyListView);

        // 播放主题切换动画
        if (root != null) {
            playThemeSwitchAnimation(root);
        }
    }

    public void cycleTheme(BorderPane root, VBox leftPanel, VBox rightPanel,
                           HBox topBar, HBox bottomBar, ListView<String> historyListView) {
        Theme[] themes = Theme.values();
        int currentIndex = currentTheme.ordinal();
        int nextIndex = (currentIndex + 1) % themes.length;
        applyTheme(themes[nextIndex], root, leftPanel, rightPanel, topBar, bottomBar, historyListView);
    }

    private void updatePanelStyles(Theme theme, VBox leftPanel, VBox rightPanel,
                                   HBox topBar, HBox bottomBar, ListView<String> historyListView) {
        String panelStyle = "";
        String buttonStyle = "";
        String sectionStyle = "";
        String listStyle = "";

        switch (theme) {
            case LIGHT_MODE:
                panelStyle = "-fx-background-color: white;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #2c3e50;";
                listStyle = "-fx-background-color: white; -fx-background-radius: 8;";
                break;
            case DARK_MODE:
                panelStyle = "-fx-background-color: #1e1e1e;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #7b2cbf, #9d4edd); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #ffffff;";
                listStyle = "-fx-background-color: #2d2d2d; -fx-background-radius: 8;";
                break;
            case BLUE_NIGHT:
                panelStyle = "-fx-background-color: #1e293b;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #0ea5e9, #3b82f6); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #38bdf8;";
                listStyle = "-fx-background-color: #1e293b; -fx-background-radius: 8;";
                break;
            case GREEN_FOREST:
                panelStyle = "-fx-background-color: #064e3b;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #10b981, #059669); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #34d399;";
                listStyle = "-fx-background-color: #064e3b; -fx-background-radius: 8;";
                break;
            case PURPLE_DREAM:
                panelStyle = "-fx-background-color: #312e81;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #a78bfa;";
                listStyle = "-fx-background-color: #312e81; -fx-background-radius: 8;";
                break;
            case ORANGE_SUNSET:
                panelStyle = "-fx-background-color: #7c2d12;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #f97316, #ea580c); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #fb923c;";
                listStyle = "-fx-background-color: #7c2d12; -fx-background-radius: 8;";
                break;
            case PINK_BLOSSOM:
                panelStyle = "-fx-background-color: #831843;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #ec4899, #db2777); " +
                        "-fx-text-fill: white;";
                sectionStyle = "-fx-text-fill: #f472b6;";
                listStyle = "-fx-background-color: #831843; -fx-background-radius: 8;";
                break;
            case CYBERPUNK:
                panelStyle = "-fx-background-color: #0f0f23;";
                buttonStyle = "-fx-background-color: linear-gradient(to right, #00ff41, #00cc33); " +
                        "-fx-text-fill: black;";
                sectionStyle = "-fx-text-fill: #00ff41;";
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
        if (topBar != null) {
            topBar.setStyle(panelStyle);
        }
        if (bottomBar != null) {
            bottomBar.setStyle(panelStyle);
        }
        if (historyListView != null) {
            historyListView.setStyle(listStyle);
        }
    }

    private void updatePanelComponents(VBox panel, Theme theme) {
        for (Node node : panel.getChildren()) {
            if (node instanceof Label) {
                updateLabelStyle((Label) node, theme);
            } else if (node instanceof Button) {
                updateButtonStyle((Button) node, theme);
            } else if (node instanceof Separator) {
                updateSeparatorStyle((Separator) node, theme);
            } else if (node instanceof VBox) {
                updatePanelComponents((VBox) node, theme);
            } else if (node instanceof HBox) {
                for (Node child : ((HBox) node).getChildren()) {
                    if (child instanceof Button) {
                        updateButtonStyle((Button) child, theme);
                    }
                }
            }
        }
    }

    private void updateLabelStyle(Label label, Theme theme) {
        String text = label.getText();
        if (text != null && (text.contains("🎛") || text.contains("🔄") || text.contains("✨") ||
                text.contains("🤖") || text.contains("📜") || text.contains("ℹ️") ||
                text.contains("⚡"))) {
            updateSectionLabelStyle(label, theme);
        }
    }

    private void updateSectionLabelStyle(Label label, Theme theme) {
        String style;
        switch (theme) {
            case LIGHT_MODE:
                style = "-fx-text-fill: #2c3e50;";
                break;
            case DARK_MODE:
                style = "-fx-text-fill: #ffffff;";
                break;
            case BLUE_NIGHT:
                style = "-fx-text-fill: #38bdf8;";
                break;
            case GREEN_FOREST:
                style = "-fx-text-fill: #34d399;";
                break;
            case PURPLE_DREAM:
                style = "-fx-text-fill: #a78bfa;";
                break;
            case ORANGE_SUNSET:
                style = "-fx-text-fill: #fb923c;";
                break;
            case PINK_BLOSSOM:
                style = "-fx-text-fill: #f472b6;";
                break;
            case CYBERPUNK:
                style = "-fx-text-fill: #00ff41;";
                break;
            default:
                style = "-fx-text-fill: #2c3e50;";
        }
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " + style);
    }

    private void updateButtonStyle(Button button, Theme theme) {
        String style;
        switch (theme) {
            case LIGHT_MODE:
                style = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-text-fill: white;";
                break;
            case DARK_MODE:
                style = "-fx-background-color: linear-gradient(to right, #7b2cbf, #9d4edd); -fx-text-fill: white;";
                break;
            case BLUE_NIGHT:
                style = "-fx-background-color: linear-gradient(to right, #0ea5e9, #3b82f6); -fx-text-fill: white;";
                break;
            case GREEN_FOREST:
                style = "-fx-background-color: linear-gradient(to right, #10b981, #059669); -fx-text-fill: white;";
                break;
            case PURPLE_DREAM:
                style = "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed); -fx-text-fill: white;";
                break;
            case ORANGE_SUNSET:
                style = "-fx-background-color: linear-gradient(to right, #f97316, #ea580c); -fx-text-fill: white;";
                break;
            case PINK_BLOSSOM:
                style = "-fx-background-color: linear-gradient(to right, #ec4899, #db2777); -fx-text-fill: white;";
                break;
            case CYBERPUNK:
                style = "-fx-background-color: linear-gradient(to right, #00ff41, #00cc33); -fx-text-fill: black;";
                break;
            default:
                style = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-text-fill: white;";
        }
        button.setStyle(style + " -fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
    }

    private void updateSeparatorStyle(Separator separator, Theme theme) {
        String style;
        switch (theme) {
            case LIGHT_MODE:
                style = "-fx-background-color: #dee2e6;";
                break;
            case DARK_MODE:
                style = "-fx-background-color: #404040;";
                break;
            case BLUE_NIGHT:
                style = "-fx-background-color: #475569;";
                break;
            case GREEN_FOREST:
                style = "-fx-background-color: #047857;";
                break;
            case PURPLE_DREAM:
                style = "-fx-background-color: #5b21b6;";
                break;
            case ORANGE_SUNSET:
                style = "-fx-background-color: #9a3412;";
                break;
            case PINK_BLOSSOM:
                style = "-fx-background-color: #9d174d;";
                break;
            case CYBERPUNK:
                style = "-fx-background-color: #00ff41;";
                break;
            default:
                style = "-fx-background-color: #dee2e6;";
        }
        separator.setStyle(style);
    }

    private void playThemeSwitchAnimation(Node node) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), node);
        fadeIn.setFromValue(0.7);
        fadeIn.setToValue(1.0);

        SequentialTransition sequence = new SequentialTransition(fadeOut, fadeIn);
        sequence.play();
    }

    public Color[] getThemeColors(Theme theme) {
        return themeColors.get(theme);
    }

    public Theme[] getAvailableThemes() {
        return Theme.values();
    }
}