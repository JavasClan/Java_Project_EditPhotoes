package API;

import java.util.Scanner;
import java.io.InputStream;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.File;
import org.apache.commons.io.FileUtils;
import com.google.gson.Gson;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.model.images.generation.ResponseFormat;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 集成客户端：结合DeepSeek文字问答和豆包图生图功能
 */
public class IntegratedClient {

    // 消息类（与DeepSeekClient一致）
    static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    // 请求体类
    static class ChatRequest {
        private String model;
        private List<Message> messages;
        private double temperature;
        private int max_tokens;

        public ChatRequest(String model, List<Message> messages, double temperature, int max_tokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = max_tokens;
        }
    }

    // 响应体类
    static class ChatResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        static class Choice {
            private Message message;

            public Message getMessage() {
                return message;
            }
        }
    }

    // 加载配置文件
    private static Properties loadConfig() {
        Properties props = new Properties();
        try {
            InputStream is = IntegratedClient.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            if (is == null) {
                System.err.println("配置文件 config.properties 未找到，请检查resources目录");
                return null;
            }
            props.load(is);
        } catch (Exception e) {
            System.err.println("加载配置失败：" + e.getMessage());
            return null;
        }
        return props;
    }

    /**
     * 显示主菜单
     */
    private static void showMainMenu() {
        System.out.println("\n=======================================");
        System.out.println("      AI集成客户端 - 主菜单");
        System.out.println("=======================================");
        System.out.println("1. DeepSeek 文字问答");
        System.out.println("2. 豆包 图生图");
        System.out.println("0. 退出");
        System.out.println("=======================================");
        System.out.print("请选择功能 (0-2): ");
    }

    /**
     * DeepSeek 文字问答模式
     */
    private static void deepSeekMode(Properties config) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        System.out.println("\n*** 进入 DeepSeek 文字问答模式 ***");
        System.out.println("（输入 'bye' 返回主菜单）");

        while (true) {
            System.out.println("\n--- 请输入您的问题: ---");
            String question = scanner.nextLine().trim();

            if ("bye".equalsIgnoreCase(question) || "0".equals(question)) {
                System.out.println("返回主菜单...");
                break;
            }

            if (question.isEmpty()) {
                System.out.println("问题不能为空！");
                continue;
            }

            // 调用DeepSeek API
            askDeepSeek(question, config);
        }
    }

    /**
     * 豆包图生图模式
     */
    private static void imageGenerationMode(Properties config) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        System.out.println("\n*** 进入 豆包图生图模式 ***");

        try {
            // 输入原图路径
            System.out.print("请输入本地原图路径（如D:/test.jpg）：");
            String localImagePath = scanner.nextLine().trim();
            if (localImagePath.isEmpty()) {
                System.err.println("原图路径不能为空！");
                return;
            }

            // 输入图生图指令
            System.out.print("请输入图生图创作指令：");
            String prompt = scanner.nextLine().trim();
            if (prompt.isEmpty()) {
                System.err.println("创作指令不能为空！");
                return;
            }

            // 输入图片保存目录
            System.out.print("请输入图片保存目录（默认：D:/generated_images/）：");
            String saveDir = scanner.nextLine().trim();
            if (saveDir.isEmpty()) {
                saveDir = "D:/generated_images/";
            }

            // 输入保存的文件名
            System.out.print("请输入图片文件名（无需后缀，默认：generated_image）：");
            String fileName = scanner.nextLine().trim();
            if (fileName.isEmpty()) {
                fileName = "generated_image";
            }

            // 调用豆包图生图API
            generateImage(localImagePath, prompt, saveDir, fileName, config);

        } catch (Exception e) {
            System.err.println("图生图功能出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 调用DeepSeek API
     */
    private static void askDeepSeek(String content, Properties config) {
        String apiKey = config.getProperty("key");
        String apiUrl = config.getProperty("url");

        if (apiKey == null || apiUrl == null) {
            System.err.println("DeepSeek配置缺失，请检查config.properties文件");
            return;
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", content));

        ChatRequest requestBody = new ChatRequest(
                "deepseek-chat",
                messages,
                0.7,
                1000
        );

        System.out.println(">>>正在提交问题....");
        long startTime = System.currentTimeMillis();

        String response = sendDeepSeekRequest(requestBody, apiKey, apiUrl);
        long endTime = System.currentTimeMillis();
        System.out.println("思考用时： " + (endTime - startTime) / 1000 + "秒");

        // 逐字打印效果
        printWithTypewriterEffect(response, 20);
    }

    /**
     * 发送DeepSeek请求
     */
    private static String sendDeepSeekRequest(ChatRequest requestBody, String apiKey, String apiUrl) {
        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();

        String requestBodyJson = gson.toJson(requestBody);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(BodyPublishers.ofString(requestBodyJson))
                    .build();

            System.out.println(">>>已提交问题，正在思考中....");

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
                return chatResponse.getChoices().get(0).getMessage().getContent();
            } else {
                return "请求失败，状态码: " + response.statusCode() + ", 响应： " + response.body();
            }
        } catch (Exception e) {
            return "请求异常: " + e.getMessage();
        }
    }

    /**
     * 逐字打印效果
     */
    private static void printWithTypewriterEffect(String text, int delayMs) {
        if (text == null || text.isEmpty()) {
            System.out.println("(无响应)");
            return;
        }

        try {
            for (char c : text.toCharArray()) {
                System.out.print(c);
                Thread.sleep(delayMs);
            }
            System.out.println();
        } catch (InterruptedException e) {
            System.out.println(text);
        }
    }

    /**
     * 豆包图生图功能 - 完整静态方法实现
     */
    public static void generateImage(String localImagePath, String prompt, String saveDir, String fileName, Properties config) {
        String apiKey = config.getProperty("ark.api.key");
        String baseUrl = config.getProperty("ark.base.url");
        String modelId = config.getProperty("ark.model.id");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("API Key 未配置，请检查 config.properties");
            return;
        }

        // 1. 图片转标准Base64
        String imageBase64;
        try {
            imageBase64 = imageToBase64(localImagePath);
            System.out.println("\n图片Base64转换成功！");
        } catch (IOException e) {
            System.err.println("图片转Base64失败：" + e.getMessage());
            return;
        }

        // 2. 构建ArkService
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        ArkService service = ArkService.builder()
                .baseUrl(baseUrl)
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();

        // 3. 构建图生图请求
        GenerateImagesRequest generateRequest = GenerateImagesRequest.builder()
                .model(modelId)
                .prompt(prompt)
                .image(imageBase64)
                .size("2K")
                .sequentialImageGeneration("disabled")
                .responseFormat(ResponseFormat.Url)
                .stream(false)
                .watermark(false)
                .build();

        // 4. 调用API + 下载保存图片
        try {
            System.out.println("\n正在调用豆包图生图API，请稍候...");
            ImagesResponse imagesResponse = service.generateImages(generateRequest);

            if (imagesResponse.getData() != null && !imagesResponse.getData().isEmpty()) {
                String imageUrl = imagesResponse.getData().get(0).getUrl();
                System.out.println("图生图成功！生成的图片URL：" + imageUrl);

                // 下载并保存图片
                System.out.println("正在下载图片到本地...");
                String savePath = downloadImage(imageUrl, saveDir, fileName);
                System.out.println("图片保存成功！路径：" + savePath);
            } else {
                System.err.println("生成结果为空，响应详情：" + imagesResponse);
            }
        } catch (Exception e) {
            System.err.println("调用API/下载图片失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            service.shutdownExecutor();
            System.out.println("\n图生图处理完成。");
        }
    }

    /**
     * 本地图片转标准Base64
     */
    private static String imageToBase64(String imagePath) throws IOException {
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
    private static String getImageFormat(String imagePath) {
        String suffix = imagePath.substring(imagePath.lastIndexOf(".") + 1).toLowerCase();
        return "jpeg".equals(suffix) ? "jpg" : suffix;
    }

    /**
     * 过滤非法文件名字符
     */
    private static String filterIllegalFileName(String fileName) {
        String illegalChars = "[\\\\/:*?\"<>|]";
        Pattern pattern = Pattern.compile(illegalChars);
        return pattern.matcher(fileName).replaceAll("_");
    }

    /**
     * 从URL中提取纯图片路径
     */
    private static String getPureImageUrl(String imageUrl) {
        if (imageUrl.contains("?")) {
            return imageUrl.split("\\?")[0];
        }
        return imageUrl;
    }

    /**
     * 下载图片并保存到本地
     */
    private static String downloadImage(String imageUrl, String saveDir, String fileName) throws IOException {
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
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 4. 发送请求下载图片
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
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
            try (InputStream inputStream = response.body().byteStream()) {
                FileUtils.copyInputStreamToFile(inputStream, saveFile);
            }
            return saveFile.getAbsolutePath();
        }
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());

        System.out.println("=======================================");
        System.out.println("       欢迎使用AI集成客户端");
        System.out.println("   结合DeepSeek问答与豆包图生图");
        System.out.println("=======================================");

        // 加载配置
        Properties config = loadConfig();
        if (config == null) {
            System.err.println("无法加载配置文件，程序退出。");
            return;
        }

        // 检查服务可用性
        boolean deepSeekAvailable = config.getProperty("key") != null && config.getProperty("url") != null;
        boolean arkAvailable = config.getProperty("ark.api.key") != null &&
                config.getProperty("ark.base.url") != null &&
                config.getProperty("ark.model.id") != null;

        System.out.println("服务状态检查:");
        System.out.println("  DeepSeek 文字问答: " + (deepSeekAvailable ? "✓ 可用" : "✗ 不可用"));
        System.out.println("  豆包图生图: " + (arkAvailable ? "✓ 可用" : "✗ 不可用"));
        System.out.println("=======================================");

        if (!deepSeekAvailable && !arkAvailable) {
            System.err.println("所有服务都不可用，请检查配置文件！");
            return;
        }

        while (true) {
            showMainMenu();

            try {
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        if (deepSeekAvailable) {
                            deepSeekMode(config);
                        } else {
                            System.err.println("DeepSeek服务不可用，请检查配置文件！");
                        }
                        break;

                    case "2":
                        if (arkAvailable) {
                            imageGenerationMode(config);
                        } else {
                            System.err.println("豆包图生图服务不可用，请检查配置文件！");
                        }
                        break;

                    case "0":
                        System.out.println("\n感谢使用AI集成客户端，再见！");
                        scanner.close();
                        return;

                    default:
                        System.out.println("无效的选择，请重新输入！");
                }
            } catch (Exception e) {
                System.err.println("发生错误: " + e.getMessage());
            }
        }
    }
}