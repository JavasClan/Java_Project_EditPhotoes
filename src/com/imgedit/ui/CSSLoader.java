package imgedit.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import java.io.File;
import java.net.URL;

/**
 * CSS样式加载器
 */
public class CSSLoader {

    /**
     * 加载CSS到场景
     */
    public static void loadCSS(Scene scene) {
        try {
            // 定义硬盘上的源码路径
            String localPath = "src/resources/styles/main.css";
            File cssFile = new File(localPath);

            if (cssFile.exists()) {
                // 硬盘模式
                String uri = cssFile.toURI().toString();
                scene.getStylesheets().add(uri);
                System.out.println("✅ (硬盘模式) CSS 加载成功: " + cssFile.getAbsolutePath());
            } else {
                // 资源模式
                String[] resourcePaths = {
                        "/styles/main.css",
                        "/resources/styles/main.css",
                        "styles/main.css"
                };

                boolean loaded = false;
                for (String path : resourcePaths) {
                    URL url = CSSLoader.class.getResource(path);
                    if (url == null) {
                        url = CSSLoader.class.getClassLoader().getResource(path);
                    }

                    if (url != null) {
                        scene.getStylesheets().add(url.toExternalForm());
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
    }

    /**
     * 为对话框加载CSS
     */
    public static void loadCSSForDialog(DialogPane dialogPane, Scene mainScene) {
        try {
            if (mainScene != null) {
                dialogPane.getStylesheets().addAll(mainScene.getStylesheets());
            }
        } catch (Exception e) {
            System.err.println("对话框CSS加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载自定义CSS文件
     */
    public static void loadCustomCSS(Scene scene, String cssPath) {
        try {
            File cssFile = new File(cssPath);
            if (cssFile.exists()) {
                scene.getStylesheets().add(cssFile.toURI().toString());
                System.out.println("自定义CSS加载成功: " + cssFile.getAbsolutePath());
            } else {
                System.err.println("自定义CSS文件不存在: " + cssPath);
            }
        } catch (Exception e) {
            System.err.println("自定义CSS加载异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 移除所有CSS样式
     */
    public static void clearCSS(Scene scene) {
        scene.getStylesheets().clear();
        System.out.println("CSS样式已清除");
    }

    /**
     * 重新加载CSS
     */
    public static void reloadCSS(Scene scene) {
        clearCSS(scene);
        loadCSS(scene);
    }

    /**
     * 检查CSS文件是否存在
     */
    public static boolean checkCSSExists() {
        String[] paths = {
                "src/resources/styles/main.css",
                "resources/styles/main.css",
                "styles/main.css"
        };

        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                System.out.println("找到CSS文件: " + file.getAbsolutePath());
                return true;
            }
        }

        // 检查类路径
        String[] resourcePaths = {
                "/styles/main.css",
                "/resources/styles/main.css"
        };

        for (String path : resourcePaths) {
            URL url = CSSLoader.class.getResource(path);
            if (url != null) {
                System.out.println("找到资源CSS文件: " + path);
                return true;
            }
        }

        System.err.println("未找到CSS文件");
        return false;
    }

    /**
     * 获取CSS文件路径列表
     */
    public static String[] getAvailableCSSPaths() {
        java.util.List<String> paths = new java.util.ArrayList<>();

        // 硬盘路径
        String[] localPaths = {
                "src/resources/styles/main.css",
                "resources/styles/main.css",
                "styles/main.css"
        };

        for (String path : localPaths) {
            File file = new File(path);
            if (file.exists()) {
                paths.add(file.getAbsolutePath());
            }
        }

        // 资源路径
        String[] resourcePaths = {
                "/styles/main.css",
                "/resources/styles/main.css"
        };

        for (String path : resourcePaths) {
            URL url = CSSLoader.class.getResource(path);
            if (url != null) {
                paths.add(url.toExternalForm());
            }
        }

        return paths.toArray(new String[0]);
    }

    /**
     * 加载所有可用的CSS文件
     */
    public static void loadAllAvailableCSS(Scene scene) {
        String[] paths = getAvailableCSSPaths();
        for (String path : paths) {
            try {
                if (path.startsWith("file:")) {
                    scene.getStylesheets().add(path);
                } else if (path.startsWith("jar:") || path.startsWith("http")) {
                    scene.getStylesheets().add(path);
                } else {
                    File file = new File(path);
                    scene.getStylesheets().add(file.toURI().toString());
                }
                System.out.println("加载CSS: " + path);
            } catch (Exception e) {
                System.err.println("加载CSS失败: " + path + " - " + e.getMessage());
            }
        }
    }

    /**
     * 创建内联CSS样式
     */
    public static String createInlineCSS(String backgroundColor, String textColor,
                                         String fontSize, String fontFamily) {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: %s; " +
                        "-fx-font-size: %s; " +
                        "-fx-font-family: '%s';",
                backgroundColor, textColor, fontSize, fontFamily
        );
    }

    /**
     * 创建按钮CSS样式
     */
    public static String createButtonCSS(String normalColor, String hoverColor,
                                         String pressedColor, String textColor) {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: %s; " +
                        "-fx-background-radius: 5; " +
                        "-fx-padding: 8 16; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); " +
                        "-fx-border-color: transparent; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 5; " +
                        "-fx-font-weight: bold;",
                normalColor, textColor
        );
    }

    /**
     * 创建卡片CSS样式
     */
    public static String createCardCSS(String backgroundColor, String borderColor,
                                       double borderRadius, double shadowRadius) {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-background-radius: %.0f; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: %.0f; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), %.0f, 0, 0, 0); " +
                        "-fx-padding: 16;",
                backgroundColor, borderRadius, borderColor, borderRadius, shadowRadius
        );
    }
}