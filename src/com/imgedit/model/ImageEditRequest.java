package imgedit.model;

import imgedit.model.enums.OperationType;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class ImageEditRequest {
    private BufferedImage originalImage;
    private OperationType operationType;
    private Map<String, Object> parameters;
    private int historyIndex; // 用于撤销/重做

    // 构造器、getter、setter
    // 构造方法
    public ImageEditRequest(BufferedImage originalImage,OperationType operationType) {
        this.originalImage = originalImage;
        this.operationType = operationType;
        this.parameters = new HashMap<>(); // 初始化参数映射
        this.historyIndex = -1; // -1表示新操作
    }
    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
    }

    // 补充错误提示中缺失的方法
    public OperationType getOperationType() {
        return operationType;
    }
    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public int getHistoryIndex() {
        return historyIndex;
    }

    public void setHistoryIndex(int historyIndex) {
        this.historyIndex = historyIndex;
    }

    // 方便添加参数的方法（前端调用）
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }

    // 便捷方法：获取参数值
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public Object getParameter(String key, Object defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }
}


