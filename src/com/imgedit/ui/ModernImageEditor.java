package imgedit.ui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * 现代化图像编辑器 - 主启动类
 */
public class ModernImageEditor extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 创建编辑器控制器
        EditorController controller = new EditorController(primaryStage);
        // 启动编辑器
        controller.start();
    }

    public static void main(String[] args) {
        launch(args);
    }

}