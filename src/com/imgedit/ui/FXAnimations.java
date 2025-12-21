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

    public static void fadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1);
        fade.setToValue(0);
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

    public static void slideInFromRight(Node node, Duration duration) {
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromX(50);
        slide.setToX(0);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition parallel = new ParallelTransition(slide, fade);
        parallel.play();
    }

    public static void scaleIn(Node node, Duration duration) {
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition parallel = new ParallelTransition(scale, fade);
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

    public static void pulse(Node node, Duration duration, int cycles) {
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.05);
        scale.setToY(1.05);
        scale.setCycleCount(cycles);
        scale.setAutoReverse(true);
        scale.play();
    }

    public static void rotate(Node node, Duration duration) {
        RotateTransition rotate = new RotateTransition(duration, node);
        rotate.setByAngle(360);
        rotate.setCycleCount(1);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.play();
    }

    public static void shake(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setToX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    public static void bounce(Node node) {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(300), node);
        bounce.setFromX(1.0);
        bounce.setFromY(1.0);
        bounce.setToX(1.2);
        bounce.setToY(1.2);
        bounce.setCycleCount(2);
        bounce.setAutoReverse(true);
        bounce.play();
    }
}