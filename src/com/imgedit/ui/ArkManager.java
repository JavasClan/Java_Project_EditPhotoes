package imgedit.ui;

import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.model.images.generation.ResponseFormat;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.binary.Base64;
import java.util.Properties;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 豆包图生图管理器（增强版，基于真实API）
 */
public class ArkManager {

    private Properties arkConfig;
    private boolean arkAvailable = false;
    private OkHttpClient httpClient;
    private ArkService arkService;

    public ArkManager() {
        loadConfig();
        initializeHttpClient();
        initializeArkService();
    }

    private void loadConfig() {
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

    private void initializeHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void initializeArkService() {
        if (!arkAvailable) {
            return;
        }

        try {
            String apiKey = arkConfig.getProperty("ark.api.key");
            String baseUrl = arkConfig.getProperty("ark.base.url");

            // 创建连接池和调度器
            ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
            Dispatcher dispatcher = new Dispatcher();

            // 构建ArkService实例
            arkService = ArkService.builder()
                    .baseUrl(baseUrl)
                    .dispatcher(dispatcher)
                    .connectionPool(connectionPool)
                    .apiKey(apiKey)
                    .build();

            System.out.println("豆包API服务初始化成功");
        } catch (Exception e) {
            System.err.println("初始化豆包API服务失败: " + e.getMessage());
            arkAvailable = false;
        }
    }

    public boolean isAvailable() {
        return arkAvailable;
    }

    public Properties getConfig() {
        return arkConfig;
    }

    /**
     * 生成豆包AI图像（使用真实API）
     */
    public String generateImage(String imagePath, String prompt, String saveDir, String fileName) throws Exception {
        if (!arkAvailable) {
            throw new Exception("豆包图生图功能未配置或初始化失败");
        }

        // 从配置中获取参数
        String modelId = arkConfig.getProperty("ark.model.id");
        String apiKey = arkConfig.getProperty("ark.api.key");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("API Key 未配置");
        }

        try {
            // 1. 图片转标准Base64
            String imageBase64 = imageToBase64(imagePath);
            System.out.println("图片已转换为Base64格式");

            // 2. 构建图生图请求
            GenerateImagesRequest generateRequest = GenerateImagesRequest.builder()
                    .model(modelId)
                    .prompt(prompt)
                    .image(imageBase64) // 带前缀的Base64
                    .size("2K") // 生成尺寸，可选: 1024x1024, 1024x576, 576x1024
                    .sequentialImageGeneration("disabled")
                    .responseFormat(ResponseFormat.Url) // 返回URL便于下载
                    .stream(false)
                    .watermark(false)
                    .build();

            System.out.println("正在调用豆包图生图API...");
            System.out.println("模型: " + modelId);
            System.out.println("提示词: " + prompt);

            // 3. 调用API
            ImagesResponse imagesResponse = arkService.generateImages(generateRequest);

            // 4. 检查响应
            if (imagesResponse == null) {
                throw new Exception("API响应为空");
            }

            if (imagesResponse.getData() == null || imagesResponse.getData().isEmpty()) {
                throw new Exception("未生成图片，响应: " + imagesResponse);
            }

            // 5. 获取图片URL
            String imageUrl = imagesResponse.getData().get(0).getUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                throw new Exception("生成的图片URL为空");
            }

            System.out.println("图生图成功！生成的图片URL：" + imageUrl);

            // 6. 下载并保存图片
            return downloadImage(imageUrl, saveDir, fileName);

        } catch (Exception e) {
            // 重新抛出异常，携带更多上下文信息
            throw new Exception("豆包图生图失败: " + e.getMessage(), e);
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

        // 拼接标准Base64前缀（API识别关键）
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
    private String downloadImage(String imageUrl, String saveDir, String fileName) throws IOException {
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

        // 3. 发送请求下载图片
        Request request = new Request.Builder()
                .url(imageUrl) // 使用原始URL（含签名参数）
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                System.out.println("下载图片失败，HTTP状态码：" + response.code());
                System.out.println("错误响应：" + errorBody);
                throw new IOException("下载图片失败，HTTP状态码：" + response.code());
            }

            // 4. 提取图片格式
            String imageFormat = pureImageUrl.substring(pureImageUrl.lastIndexOf(".") + 1).toLowerCase();

            // 过滤文件名中的非法字符
            String safeFileName = filterIllegalFileName(fileName);

            // 补全文件名后缀
            String fullFileName = safeFileName.endsWith("." + imageFormat)
                    ? safeFileName
                    : safeFileName + "." + imageFormat;

            // 拼接最终保存路径
            File saveFile = new File(dir, fullFileName);

            // 5. 写入文件
            try (InputStream inputStream = response.body().byteStream()) {
                FileUtils.copyInputStreamToFile(inputStream, saveFile);
            }

            System.out.println("图片保存成功: " + saveFile.getAbsolutePath());
            return saveFile.getAbsolutePath();
        }
    }

    /**
     * 测试API连接
     */
    public boolean testConnection() {
        if (!arkAvailable) {
            return false;
        }

        try {
            // 使用ArkService进行测试连接
            // 这里我们可以尝试调用一个简单的API或检查服务状态
            System.out.println("正在测试豆包API连接...");

            // 模拟一个简单的请求来测试连接
            // 注意：实际API可能没有/health端点，这里仅为示例
            String baseUrl = arkConfig.getProperty("ark.base.url");
            Request request = new Request.Builder()
                    .url(baseUrl + "/health")
                    .addHeader("Authorization", "Bearer " + arkConfig.getProperty("ark.api.key"))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                System.out.println("API连接测试结果: " + (success ? "成功" : "失败"));
                return success;
            }
        } catch (Exception e) {
            System.err.println("API连接测试失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取可用的模型列表
     */
    public String[] getAvailableModels() {
        // 从配置中获取模型ID，或调用API获取可用模型列表
        // 这里返回配置中的模型ID和几个常用模型
        String modelId = arkConfig.getProperty("ark.model.id");
        return new String[] {
                modelId != null ? modelId : "未配置模型",
                "ark-1.0",
                "ark-2.0",
                "creative-v1",
                "realistic-v2"
        };
    }

    /**
     * 获取API状态信息
     */
    public String getApiStatus() {
        if (!arkAvailable) {
            return "未配置或初始化失败";
        }

        try {
            // 测试API连接
            boolean connected = testConnection();
            return connected ? "在线" : "离线";
        } catch (Exception e) {
            return "连接失败: " + e.getMessage();
        }
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }

        if (arkService != null) {
            try {
                arkService.shutdownExecutor();
                System.out.println("豆包API服务已关闭");
            } catch (Exception e) {
                System.err.println("关闭豆包API服务时出错: " + e.getMessage());
            }
        }
    }
}