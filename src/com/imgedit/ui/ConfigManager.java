package imgedit.ui;

import java.util.Properties;
import java.io.*;

/**
 * 配置管理器
 */
public class ConfigManager {

    private Properties properties;
    private File configFile;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        try {
            // 1. 尝试从类路径加载
            InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (is != null) {
                properties.load(is);
                is.close();
                System.out.println("从类路径加载配置成功");
                return;
            }

            // 2. 尝试从当前目录加载
            configFile = new File("config.properties");
            if (configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                properties.load(fis);
                fis.close();
                System.out.println("从当前目录加载配置成功: " + configFile.getAbsolutePath());
                return;
            }

            // 3. 尝试从用户目录加载
            String userHome = System.getProperty("user.home");
            configFile = new File(userHome + "/.image-editor/config.properties");
            if (configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                properties.load(fis);
                fis.close();
                System.out.println("从用户目录加载配置成功: " + configFile.getAbsolutePath());
                return;
            }

            // 4. 创建默认配置
            createDefaultConfig();
            System.out.println("创建默认配置");

        } catch (Exception e) {
            System.err.println("加载配置失败: " + e.getMessage());
            createDefaultConfig();
        }
    }

    /**
     * 创建默认配置
     */
    private void createDefaultConfig() {
        // 应用设置
        properties.setProperty("app.name", "Pro Image Editor");
        properties.setProperty("app.version", "3.1.0");
        properties.setProperty("app.theme", "LIGHT_MODE");
        properties.setProperty("app.language", "zh_CN");

        // 窗口设置
        properties.setProperty("window.width", "1600");
        properties.setProperty("window.height", "950");
        properties.setProperty("window.maximized", "true");
        properties.setProperty("window.fullscreen", "false");

        // 图像设置
        properties.setProperty("image.default_format", "PNG");
        properties.setProperty("image.quality", "90");
        properties.setProperty("image.max_size", "8192");
        properties.setProperty("image.auto_save", "false");
        properties.setProperty("image.backup_enabled", "true");

        // 编辑器设置
        properties.setProperty("editor.undo_limit", "50");
        properties.setProperty("editor.auto_enhance", "false");
        properties.setProperty("editor.preview_enabled", "true");
        properties.setProperty("editor.grid_enabled", "false");
        properties.setProperty("editor.grid_size", "20");

        // 工具设置
        properties.setProperty("tool.default", "SELECT");
        properties.setProperty("tool.brush_size", "3");
        properties.setProperty("tool.brush_color", "#000000");
        properties.setProperty("tool.text_font", "Microsoft YaHei");
        properties.setProperty("tool.text_size", "24");

        // 快捷键设置
        properties.setProperty("shortcut.open", "Ctrl+O");
        properties.setProperty("shortcut.save", "Ctrl+S");
        properties.setProperty("shortcut.undo", "Ctrl+Z");
        properties.setProperty("shortcut.redo", "Ctrl+Y");
        properties.setProperty("shortcut.copy", "Ctrl+C");
        properties.setProperty("shortcut.paste", "Ctrl+V");
        properties.setProperty("shortcut.cut", "Ctrl+X");
        properties.setProperty("shortcut.select_all", "Ctrl+A");

        // 豆包AI设置
        properties.setProperty("ark.api.key", "");
        properties.setProperty("ark.base.url", "https://ark.cn-beijing.volces.com/api/v3");
        properties.setProperty("ark.model.id", "ark-1.0");
        properties.setProperty("ark.enabled", "false");
        properties.setProperty("ark.timeout", "30");

        // 批量处理设置
        properties.setProperty("batch.thread_count", "4");
        properties.setProperty("batch.output_suffix", "_processed");
        properties.setProperty("batch.overwrite", "false");

        // 高级设置
        properties.setProperty("advanced.gpu_acceleration", "true");
        properties.setProperty("advanced.memory_limit", "2048");
        properties.setProperty("advanced.cache_enabled", "true");
        properties.setProperty("advanced.cache_size", "500");
        properties.setProperty("advanced.log_level", "INFO");

        // 保存配置
        saveConfig();
    }

    /**
     * 保存配置
     */
    public void saveConfig() {
        try {
            if (configFile == null) {
                String userHome = System.getProperty("user.home");
                File configDir = new File(userHome + "/.image-editor");
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }
                configFile = new File(configDir, "config.properties");
            }

            FileOutputStream fos = new FileOutputStream(configFile);
            properties.store(fos, "Pro Image Editor Configuration");
            fos.close();

            System.out.println("配置保存成功: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key) {
        return properties.getProperty(key);
    }

    /**
     * 获取字符串配置（带默认值）
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取整数配置
     */
    public int getInt(String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取整数配置（带默认值）
     */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String key) {
        try {
            return Boolean.parseBoolean(properties.getProperty(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取布尔配置（带默认值）
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取双精度配置
     */
    public double getDouble(String key) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 获取双精度配置（带默认值）
     */
    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 设置配置值
     */
    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * 设置整数配置
     */
    public void setInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    /**
     * 设置布尔配置
     */
    public void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }

    /**
     * 设置双精度配置
     */
    public void setDouble(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
    }

    /**
     * 删除配置
     */
    public void remove(String key) {
        properties.remove(key);
    }

    /**
     * 检查配置是否存在
     */
    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    /**
     * 获取所有配置键
     */
    public java.util.Set<String> getKeys() {
        return properties.stringPropertyNames();
    }

    /**
     * 获取所有配置
     */
    public Properties getAllProperties() {
        return new Properties(properties);
    }

    /**
     * 导入配置
     */
    public boolean importConfig(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            Properties newProperties = new Properties();
            newProperties.load(fis);
            fis.close();

            // 合并配置
            for (String key : newProperties.stringPropertyNames()) {
                properties.setProperty(key, newProperties.getProperty(key));
            }

            saveConfig();
            System.out.println("配置导入成功: " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("配置导入失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 导出配置
     */
    public boolean exportConfig(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            properties.store(fos, "Pro Image Editor Configuration - Export");
            fos.close();

            System.out.println("配置导出成功: " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("配置导出失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 重置配置
     */
    public void reset() {
        properties.clear();
        createDefaultConfig();
        System.out.println("配置已重置为默认值");
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        properties.clear();
        loadConfig();
        System.out.println("配置重新加载完成");
    }

    /**
     * 获取配置文件路径
     */
    public String getConfigFilePath() {
        return configFile != null ? configFile.getAbsolutePath() : "内存配置";
    }
}