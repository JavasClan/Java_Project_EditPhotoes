package imgedit.ui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class FXAnimations {

    public static void fadeIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public static void slideInFromLeft(Node node, Duration duration) {
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromX(-50);
        slide.setToX(0);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition parallel = new ParallelTransition(slide, fade);
        parallel.play();
    }

    public static void pulse(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.05);
        scale.setToY(1.05);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);
        scale.play();
    }
}