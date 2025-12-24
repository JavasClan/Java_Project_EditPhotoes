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

    private Theme currentTheme = Theme.LIGHT_MODE;
    private Map<Theme, String> themeStyles = new HashMap<>();
    private Map<Theme, Color[]> themeColors = new HashMap<>();

    public ThemeManager() {
        initializeThemes();
    }

    private void initializeThemes() {
        // 初始化样式
        themeStyles.put(Theme.LIGHT_MODE,
                "-fx-background-color: #f5f7fa; " +
                        "-fx-text-fill: #2c3e50;");

        themeStyles.put(Theme.DARK_MODE,
                "-fx-background-color: #121212; " +
                        "-fx-text-fill: #e0e0e0;");

        // ... 其他主题样式

        // 初始化颜色
        themeColors.put(Theme.LIGHT_MODE, new Color[]{
                Color.web("#667eea"), Color.web("#764ba2"), Color.web("#f5f7fa")
        });

        themeColors.put(Theme.DARK_MODE, new Color[]{
                Color.web("#7b2cbf"), Color.web("#9d4edd"), Color.web("#121212")
        });

        // ... 其他主题颜色
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

    public Theme getNextTheme() {
        Theme[] themes = Theme.values();
        int currentIndex = currentTheme.ordinal();
        int nextIndex = (currentIndex + 1) % themes.length;
        return themes[nextIndex];
    }
}