package imgedit.ui;


import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.util.Duration;

/**
 * 动画管理器
 */
public class AnimationManager {

    /**
     * 播放入场动画
     */
    public void playEntryAnimation(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /**
     * 播放图像加载动画
     */
    public void playImageLoadAnimation(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), node);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    /**
     * 播放成功动画
     */
    public void playSuccessAnimation(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    /**
     * 播放主题切换动画
     */
    public void playThemeSwitchAnimation(Node node) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), node);
        fadeIn.setFromValue(0.7);
        fadeIn.setToValue(1.0);

        SequentialTransition sequence = new SequentialTransition(fadeOut, fadeIn);
        sequence.play();
    }

    /**
     * 播放淡入淡出动画
     */
    public static void playFadeAnimation(Node node, double from, double to, int durationMs) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(from);
        fade.setToValue(to);
        fade.play();
    }

    /**
     * 播放缩放动画
     */
    public static void playScaleAnimation(Node node, double fromX, double fromY,
                                          double toX, double toY, int durationMs) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setFromX(fromX);
        scale.setFromY(fromY);
        scale.setToX(toX);
        scale.setToY(toY);
        scale.play();
    }

    /**
     * 播放按钮点击动画
     */
    public void playButtonClickAnimation(Node node) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), node);
        scaleDown.setFromX(1.0);
        scaleDown.setFromY(1.0);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), node);
        scaleUp.setFromX(0.95);
        scaleUp.setFromY(0.95);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(scaleDown, scaleUp);
        sequence.play();
    }

    /**
     * 播放滑动动画
     */
    public void playSlideAnimation(Node node, double fromX, double toX, double fromY, double toY, int durationMs) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(fromX);
        slide.setFromY(fromY);
        slide.setToX(toX);
        slide.setToY(toY);
        slide.play();
    }

    /**
     * 播放旋转动画
     */
    public void playRotateAnimation(Node node, double fromAngle, double toAngle, int durationMs) {
        RotateTransition rotate = new RotateTransition(Duration.millis(durationMs), node);
        rotate.setFromAngle(fromAngle);
        rotate.setToAngle(toAngle);
        rotate.play();
    }

    /**
     * 播放序列动画
     */
//    public void playSequenceAnimation(Node node, Timeline... keyFrames) {
//        Timeline timeline = new Timeline();
//        timeline.getKeyFrames().addAll(keyFrames);
//        timeline.play();
//    }

    /**
     * 播放闪烁动画
     */
//    public void playBlinkAnimation(Node node, int count) {
//        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), node);
//        fadeOut.setFromValue(1.0);
//        fadeOut.setToValue(0.3);
//
//        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), node);
//        fadeIn.setFromValue(0.3);
//        fadeIn.setToValue(1.0);
//
//        SequentialSequence sequence = new SequentialSequence();
//        for (int i = 0; i < count; i++) {
//            sequence.getChildren().add(fadeOut);
//            sequence.getChildren().add(fadeIn);
//        }
//        sequence.play();
//    }

    /**
     * 播放弹跳动画
     */
    public void playBounceAnimation(Node node) {
        TranslateTransition bounce1 = new TranslateTransition(Duration.millis(100), node);
        bounce1.setFromY(0);
        bounce1.setToY(-10);

        TranslateTransition bounce2 = new TranslateTransition(Duration.millis(100), node);
        bounce2.setFromY(-10);
        bounce2.setToY(0);

        TranslateTransition bounce3 = new TranslateTransition(Duration.millis(50), node);
        bounce3.setFromY(0);
        bounce3.setToY(-5);

        TranslateTransition bounce4 = new TranslateTransition(Duration.millis(50), node);
        bounce4.setFromY(-5);
        bounce4.setToY(0);

        SequentialTransition sequence = new SequentialTransition(bounce1, bounce2, bounce3, bounce4);
        sequence.play();
    }

    /**
     * 播放进度条动画
     */
    public void playProgressAnimation(ProgressIndicator progress, double from, double to, int durationMs) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress.progressProperty(), from)),
                new KeyFrame(Duration.millis(durationMs), new KeyValue(progress.progressProperty(), to))
        );
        timeline.play();
    }

    /**
     * 停止所有动画
     */
    public void stopAllAnimations(Node node) {
        node.getTransforms().clear();
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        node.setRotate(0);
        node.setOpacity(1.0);
    }

    /**
     * 创建自定义动画
     */
//    public Timeline createCustomAnimation(Node node, Duration duration,
//                                          KeyFrame... keyFrames) {
//        Timeline timeline = new Timeline(keyFrames);
//        timeline.setNode(node);
//        return timeline;
//    }

    /**
     * 创建并行动画组
     */
    public ParallelTransition createParallelAnimation(Animation... animations) {
        return new ParallelTransition(animations);
    }

    /**
     * 创建序列动画组
     */
    public SequentialTransition createSequentialAnimation(Animation... animations) {
        return new SequentialTransition(animations);
    }

    /**
     * 播放加载动画
     */
    public void playLoadingAnimation(Node loadingNode, Node contentNode) {
        // 淡出内容
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), contentNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.3);

        // 显示加载动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), loadingNode);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);

        ParallelTransition parallel = new ParallelTransition(fadeOut, fadeIn);
        parallel.play();
    }

    /**
     * 停止加载动画
     */
    public void stopLoadingAnimation(Node loadingNode, Node contentNode) {
        // 淡出加载动画
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), loadingNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0);

        // 显示内容
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentNode);
        fadeIn.setFromValue(0.3);
        fadeIn.setToValue(1.0);

        ParallelTransition parallel = new ParallelTransition(fadeOut, fadeIn);
        parallel.play();
    }
}