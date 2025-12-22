package API;

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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 豆包图生图增强版：控制台输入prompt + 生成图片自动保存到本地（修复非法文件名问题）
 */
public class ImageGenerationsExample {

    // 加载配置文件
    private static Properties loadArkConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream inputStream = ImageGenerationsExample.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (inputStream == null) {
                throw new IOException("配置文件 config.properties 未找到，请检查resources目录");
            }
            props.load(inputStream);
        }
        return props;
    }

    /**
     * 本地图片转标准Base64（带格式前缀，适配豆包API）
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
        // 提取图片格式（jpg/png/webp）
        String imageFormat = getImageFormat(imagePath);
        // 拼接标准Base64前缀（API识别关键）
        return "data:image/" + imageFormat + ";base64," + Base64.encodeBase64String(imageBytes);
    }

    /**
     * 提取图片格式（兼容jpg/jpeg）
     */
    private static String getImageFormat(String imagePath) {
        String suffix = imagePath.substring(imagePath.lastIndexOf(".") + 1).toLowerCase();
        return "jpeg".equals(suffix) ? "jpg" : suffix;
    }

    /**
     * 过滤Windows非法文件名字符（\:*?"<>|）
     */
    private static String filterIllegalFileName(String fileName) {
        // 正则替换所有非法字符为下划线
        String illegalChars = "[\\\\/:*?\"<>|]";
        Pattern pattern = Pattern.compile(illegalChars);
        return pattern.matcher(fileName).replaceAll("_");
    }

    /**
     * 从URL中提取纯图片路径（去掉?及后面的参数）
     */
    private static String getPureImageUrl(String imageUrl) {
        if (imageUrl.contains("?")) {
            return imageUrl.split("\\?")[0]; // 截取?前的纯URL
        }
        return imageUrl;
    }

    /**
     * 下载图片并保存到本地（修复非法文件名问题）
     * @param imageUrl 生成的图片URL（含TOS参数）
     * @param saveDir 保存目录（如D:/generated_images/）
     * @param fileName 保存的文件名（自动补全格式）
     * @return 保存的文件路径
     * @throws IOException 下载/保存失败
     */
    private static String downloadImage(String imageUrl, String saveDir, String fileName) throws IOException {
        // 1. 处理URL：去掉TOS签名参数，只保留纯图片路径
        String pureImageUrl = getPureImageUrl(imageUrl);

        // 2. 创建保存目录（不存在则创建）
        File dir = new File(saveDir);
        if (!dir.exists()) {
            boolean mkdirSuccess = dir.mkdirs();
            if (!mkdirSuccess) {
                throw new IOException("创建保存目录失败：" + saveDir);
            }
        }

        // 3. 构建OkHttpClient（复用项目依赖）
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 4. 发送请求下载图片（使用带参数的原始URL，保证能下载）
        Request request = new Request.Builder()
                .url(imageUrl) // 原始URL（含签名参数，必须用这个下载）
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载图片失败，HTTP状态码：" + response.code());
            }

            // 5. 提取图片格式（从纯URL中提取，避免参数干扰）
            String imageFormat = pureImageUrl.substring(pureImageUrl.lastIndexOf(".") + 1).toLowerCase();
            // 过滤文件名中的非法字符
            String safeFileName = filterIllegalFileName(fileName);
            // 补全文件名后缀
            String fullFileName = safeFileName.endsWith("." + imageFormat)
                    ? safeFileName
                    : safeFileName + "." + imageFormat;
            // 拼接最终保存路径
            File saveFile = new File(dir, fullFileName); // 用File构造器避免路径分隔符问题

            // 6. 写入文件
            try (InputStream inputStream = response.body().byteStream()) {
                FileUtils.copyInputStreamToFile(inputStream, saveFile);
            }
            return saveFile.getAbsolutePath();
        }
    }

    public static void main(String[] args) {
        // ========== 1. 加载配置 ==========
        Properties config;
        String apiKey;
        String baseUrl;
        String modelId;
        try {
            config = loadArkConfig();
            apiKey = config.getProperty("ark.api.key");
            baseUrl = config.getProperty("ark.base.url");
            modelId = config.getProperty("ark.model.id");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.err.println("API Key 未配置，请检查 config.properties");
                return;
            }
        } catch (IOException e) {
            System.err.println("加载配置失败：" + e.getMessage());
            return;
        }

        // ========== 2. 控制台交互输入 ==========
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name()); // 支持中文输入

        // 输入原图路径
        System.out.print("请输入本地原图路径（如D:/test.jpg）：");
        String localImagePath = scanner.nextLine().trim();
        if (localImagePath.isEmpty()) {
            System.err.println("原图路径不能为空！");
            scanner.close();
            return;
        }

        // 输入图生图指令
        System.out.print("请输入图生图创作指令：");
        String prompt = scanner.nextLine().trim();
        if (prompt.isEmpty()) {
            System.err.println("创作指令不能为空！");
            scanner.close();
            return;
        }

        // 输入图片保存目录（默认D:/generated_images/）
        System.out.print("请输入图片保存目录（默认：D:/generated_images/）：");
        String saveDir = scanner.nextLine().trim();
        if (saveDir.isEmpty()) {
            saveDir = "D:/generated_images/"; // 默认保存目录
        }

        // 输入保存的文件名（默认：generated_image）
        System.out.print("请输入图片文件名（无需后缀，默认：generated_image）：");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            fileName = "generated_image"; // 默认文件名
        }
        scanner.close(); // 关闭Scanner

        // ========== 3. 图片转标准Base64 ==========
        String imageBase64;
        try {
            imageBase64 = imageToBase64(localImagePath);
            // 打印Base64前缀（验证格式，可删除）
            System.out.println("\nBase64格式校验：" + imageBase64.substring(0, 50) + "...");
        } catch (IOException e) {
            System.err.println("图片转Base64失败：" + e.getMessage());
            return;
        }

        // ========== 4. 构建ArkService（对齐官方示例） ==========
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        ArkService service = ArkService.builder()
                .baseUrl(baseUrl)
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();

        // ========== 5. 构建图生图请求 ==========
        GenerateImagesRequest generateRequest = GenerateImagesRequest.builder()
                .model(modelId) // 替换为控制台可用的图生图模型ID
                .prompt(prompt) // 控制台输入的指令
                .image(imageBase64) // 带前缀的Base64
                .size("2K") // 生成尺寸
                .sequentialImageGeneration("disabled")
                .responseFormat(ResponseFormat.Url) // 返回URL（便于下载）
                .stream(false)
                .watermark(false)
                .build();

        // ========== 6. 调用API + 下载保存图片 ==========
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
            System.out.println("\n服务已关闭，程序结束。");
        }
    }
}
