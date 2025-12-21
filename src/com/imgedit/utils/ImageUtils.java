package imgedit.utils;

import java.awt.image.BufferedImage;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;


public class ImageUtils {
    public static BufferedImage fxImageToBufferedImage(Image fxImage) {
        if (fxImage == null) {
            return null;
        }
        // 使用JavaFX提供的转换工具
        return SwingFXUtils.fromFXImage(fxImage, null);
    }

    public static Image bufferedImageToFXImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }

        // 创建可写图像并转换
        WritableImage fxImage = new WritableImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight()
        );
        return SwingFXUtils.toFXImage(bufferedImage, fxImage);
    }
}

