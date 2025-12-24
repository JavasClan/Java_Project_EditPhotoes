package imgedit.ui;

import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * 主题管理器
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

    // 将默认主题改为 DARK_MODE
    private Theme currentTheme = Theme.DARK_MODE; // 这里改成了 DARK_MODE

    private Map<Theme, String> themeStyles = new HashMap<>();
    private Map<Theme, Color[]> themeColors = new HashMap<>();
    private Map<Theme, String> themeGradients = new HashMap<>();

    public ThemeManager() {
        initializeThemes();
    }

    private void initializeThemes() {
        // 初始化样式 - 确保浅色背景配深色文字，深色背景配浅色文字
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

        // 初始化颜色
        themeColors.put(Theme.LIGHT_MODE, new Color[]{
                Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f5f7fa")
        });

        themeColors.put(Theme.DARK_MODE, new Color[]{
                Color.web("#7b2cbf"), Color.web("#9d4edd"), Color.web("#121212")
        });

        themeColors.put(Theme.BLUE_NIGHT, new Color[]{
                Color.web("#0ea5e9"), Color.web("#3b82f6"), Color.web("#0f172a")
        });

        themeColors.put(Theme.GREEN_FOREST, new Color[]{
                Color.web("#10b981"), Color.web("#059669"), Color.web("#022c22")
        });

        themeColors.put(Theme.PURPLE_DREAM, new Color[]{
                Color.web("#8b5cf6"), Color.web("#7c3aed"), Color.web("#1e1b4b")
        });

        themeColors.put(Theme.ORANGE_SUNSET, new Color[]{
                Color.web("#f97316"), Color.web("#ea580c"), Color.web("#431407")
        });

        themeColors.put(Theme.PINK_BLOSSOM, new Color[]{
                Color.web("#ec4899"), Color.web("#db2777"), Color.web("#500724")
        });

        themeColors.put(Theme.CYBERPUNK, new Color[]{
                Color.web("#00ff41"), Color.web("#ff00ff"), Color.web("#000000")
        });

        // 初始化渐变
        themeGradients.put(Theme.LIGHT_MODE, "linear-gradient(to right, #667eea, #764ba2)");
        themeGradients.put(Theme.DARK_MODE, "linear-gradient(to right, #7b2cbf, #9d4edd)");
        themeGradients.put(Theme.CYBERPUNK, "linear-gradient(to right, #00ff41, #00cc33)");
        themeGradients.put(Theme.BLUE_NIGHT, "linear-gradient(to right, #0ea5e9, #3b82f6)");
        themeGradients.put(Theme.GREEN_FOREST, "linear-gradient(to right, #10b981, #059669)");
        themeGradients.put(Theme.PURPLE_DREAM, "linear-gradient(to right, #8b5cf6, #7c3aed)");
        themeGradients.put(Theme.ORANGE_SUNSET, "linear-gradient(to right, #f97316, #ea580c)");
        themeGradients.put(Theme.PINK_BLOSSOM, "linear-gradient(to right, #ec4899, #db2777)");
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(Theme theme) {
        this.currentTheme = theme;
    }

    public String getThemeStyle(Theme theme) {
        return themeStyles.get(theme);
    }

    public Color[] getThemeColors(Theme theme) {
        return themeColors.get(theme);
    }

    public String getThemeGradient(Theme theme) {
        return themeGradients.get(theme);
    }

    public Theme getNextTheme() {
        Theme[] themes = Theme.values();
        int currentIndex = currentTheme.ordinal();
        int nextIndex = (currentIndex + 1) % themes.length;
        return themes[nextIndex];
    }

    public String getButtonTextColor(Theme theme) {
        switch (theme) {
            case LIGHT_MODE: return "#2c3e50"; // 浅色主题按钮文字用深色
            default: return "#ffffff"; // 深色主题按钮文字用浅色
        }
    }

    public String getCardBackground(Theme theme) {
        switch (theme) {
            case LIGHT_MODE:
                return "rgba(255,255,255,0.9)"; // 浅色主题卡片背景浅色
            case DARK_MODE:
            case CYBERPUNK:
            case BLUE_NIGHT:
            case GREEN_FOREST:
            case PURPLE_DREAM:
            case ORANGE_SUNSET:
            case PINK_BLOSSOM:
                return "rgba(255,255,255,0.08)"; // 深色主题卡片背景深色半透明
            default:
                return "rgba(255,255,255,0.8)";
        }
    }

    public String getTextColor(Theme theme) {
        switch (theme) {
            case LIGHT_MODE:
                return "#2c3e50"; // 浅色主题文字用深色
            case DARK_MODE:
            case CYBERPUNK:
            case BLUE_NIGHT:
            case GREEN_FOREST:
            case PURPLE_DREAM:
            case ORANGE_SUNSET:
            case PINK_BLOSSOM:
                return "#e0e0e0"; // 深色主题文字用浅色
            default:
                return "#333";
        }
    }

    public String getTitleColor(Theme theme) {
        switch (theme) {
            case LIGHT_MODE:
                return "#1a237e"; // 浅色主题标题用深色
            case DARK_MODE:
            case CYBERPUNK:
            case BLUE_NIGHT:
            case GREEN_FOREST:
            case PURPLE_DREAM:
            case ORANGE_SUNSET:
            case PINK_BLOSSOM:
                return "#ffffff"; // 深色主题标题用浅色
            default:
                return "#2c3e50";
        }
    }

    /**
     * 判断主题是否为深色主题
     */
    public boolean isDarkTheme(Theme theme) {
        return theme != Theme.LIGHT_MODE;
    }

    /**
     * 获取对比度合适的文字颜色
     */
    public String getContrastTextColor(String backgroundColor) {
        // 计算背景颜色的亮度
        Color color = Color.web(backgroundColor);
        double brightness = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;

        // 根据亮度返回合适的文字颜色
        return brightness > 0.5 ? "#2c3e50" : "#ffffff";
    }
}