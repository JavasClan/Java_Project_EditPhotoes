package API;

import com.google.gson.Gson;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class DeepSeekClient {

    //加载配置文件，读取key和url
    private static String API_KEY;
    private static String API_URL;

    static{
        Properties properties=new Properties();
        try{
            InputStream is=DeepSeekClient.class.getResourceAsStream("/config.properties");
            properties.load(is);
            API_KEY=properties.getProperty("key");
            API_URL=properties.getProperty("url");

        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    //内部类
    //消息类
    static class Message{
        private String role;
        private String content;

        public Message(String role, String content){
            this.role=role;
            this.content=content;
        }

        public String getContent(){
            return content;
        }
    }

    //请求体类(请求体的数据内部结构，用于构建请求JSON)
    static class ChatRequest{
        private String model;
        private List<Message> messages;
        private double temperature;
        private int max_tokens;

        public ChatRequest(String model, List<Message> messages, double temperature, int max_tokens){
            this.model=model;
            this.messages=messages;
            this.temperature=temperature;
            this.max_tokens=max_tokens;
        }
    }

    //响应体类(按照响应的JSON格式来设计响应体，用来封装响应体字符)
    static class ChatResponse{
        private List<Choice> choices;
        public List<Choice> getChoices(){
            return choices;
        }

        static class Choice{
            private Message message;

            public Message getMessage(){
                return message;
            }
        }
    }

    //发送请求的方法
    public static String sendRequest(ChatRequest requestBody){
        HttpClient client= HttpClient.newHttpClient();
        Gson gson=new Gson();

        String requestBodyJson=gson.toJson(requestBody);
        try{
            HttpRequest request= HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type","application/json")
                    .header("Authorization","Bearer " + API_KEY)  // 注意空格
                    .POST(BodyPublishers.ofString(requestBodyJson))
                    .build();

            System.out.println(">>>已提交问题，正在思考中....");

            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());

            if(response.statusCode()==200){
                ChatResponse chatResponse=gson.fromJson(response.body(),ChatResponse.class);
                return chatResponse.getChoices().get(0).getMessage().getContent();
            }else{
                return "请求失败，状态码: "+response.statusCode()+", 响应： "+response.body();
            }
        } catch (Exception e) {
            return "请求异常: " + e.getMessage();
        }
    }

    //提问问题及打印响应
    public static void ask(String content){
        List<Message> messages=new ArrayList<>();
        messages.add(new Message("user",content));

        ChatRequest requestBody=new ChatRequest(
                "deepseek-chat",
                messages,
                0.7,
                1000
        );
        System.out.println(">>>正在提交问题....");
        long startTime=System.currentTimeMillis();

        String response=sendRequest(requestBody);
        long endTime=System.currentTimeMillis();
        System.out.println("思考用时： "+(endTime-startTime)/1000+"秒");

        // 检查TypewriterEffect类是否存在
        try {
            // 假设TypewriterEffect在同一包内
            TypewriterEffect.printWord(response, 20);
        } catch (Exception e) {
            // 如果TypewriterEffect不存在，直接打印
            System.out.println(response);
        }
    }

    //调用测试
    public static void main(String args[])  {
        Scanner scanner=new Scanner(System.in);
        System.out.println("*** 我是DeepSeek ,很高兴见到您哈哈哈 ***");
        while(true){
            System.out.println("---请告诉我您的问题: ---");
            String question=scanner.nextLine();  // 使用nextLine而不是next
            if("bye".equalsIgnoreCase(question.trim())){
                break;
            }
            ask(question);
            System.out.println();
        }
        System.out.println("拜拜~欢迎下次使用");
        scanner.close();
    }
}