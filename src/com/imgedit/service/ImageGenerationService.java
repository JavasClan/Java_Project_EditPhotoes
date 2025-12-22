package imgedit.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

/**
 * 豆包图生图服务 - 不依赖任何外部JSON库
 */
public class ImageGenerationService {

    // 您的API配置
    private static final String API_KEY = "4cbf35d4-2bc4-4c2e-8cfc-ea6e430b2f8f";
    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";
    private static final String MODEL_ID = "doubao-seedream-4-5-251128";

    /**
     * 将图片文件转换为Base64
     */
    public String imageToBase64(File imageFile) throws Exception {
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

        // 检查图片大小（豆包API限制10MB）
        if (imageBytes.length > 10 * 1024 * 1024) {
            throw new Exception("图片大小超过10MB限制");
        }

        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        // 获取图片格式
        String fileName = imageFile.getName().toLowerCase();
        String mimeType;
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            mimeType = "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            mimeType = "image/png";
        } else if (fileName.endsWith(".gif")) {
            mimeType = "image/gif";
        } else if (fileName.endsWith(".bmp")) {
            mimeType = "image/bmp";
        } else {
            throw new Exception("不支持的图片格式");
        }

        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * 手动构建JSON字符串
     */
    private String buildJsonRequest(String base64Image, String prompt) {
        return String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"image\":\"%s\",\"size\":\"1024x1024\",\"n\":1}",
                MODEL_ID,
                escapeJson(prompt),
                base64Image
        );
    }

    /**
     * 转义JSON特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 从JSON响应中提取URL（简单解析）
     */
    private String extractUrlFromJson(String jsonResponse) {
        try {
            // 查找"url"字段
            int urlIndex = jsonResponse.indexOf("\"url\":\"");
            if (urlIndex == -1) return null;

            urlIndex += 7; // "\"url\":\""的长度
            int endIndex = jsonResponse.indexOf("\"", urlIndex);
            if (endIndex == -1) return null;

            return jsonResponse.substring(urlIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 调用豆包图生图API
     */
    public String generateImage(File imageFile, String prompt, String saveDir) throws Exception {
        // 1. 转换图片为Base64
        String base64Image = imageToBase64(imageFile);

        // 2. 构建JSON请求
        String jsonRequest = buildJsonRequest(base64Image, prompt);

        // 3. 发送HTTP请求
        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("API调用失败，状态码: " + responseCode);
            }

            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
            }

            // 提取图片URL
            String jsonResponse = response.toString();
            String imageUrl = extractUrlFromJson(jsonResponse);

            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new Exception("无法从响应中获取图片URL: " + jsonResponse);
            }

            // 4. 下载图片
            return downloadImage(imageUrl, saveDir);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 下载图片
     */
    private String downloadImage(String imageUrl, String saveDir) throws Exception {
        // 创建保存目录
        File dir = new File(saveDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new Exception("无法创建目录: " + saveDir);
            }
        }

        // 生成文件名
        String fileName = "generated_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(dir, fileName);

        // 下载图片
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("下载图片失败，状态码: " + responseCode);
            }

            // 保存图片
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return outputFile.getAbsolutePath();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}