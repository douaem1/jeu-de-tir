package Ui;
/*
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class TransitionManager {
    private void transitionToScene(Runnable sceneSetup) {
        StackPane root = (StackPane) primaryStage.getScene().getRoot();

        Rectangle transitionRect = new Rectangle(0, 0, Color.BLACK);
        transitionRect.widthProperty().bind(root.widthProperty());
        transitionRect.heightProperty().bind(root.heightProperty());
        transitionRect.setOpacity(0);
        root.getChildren().add(transitionRect);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), transitionRect);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            sceneSetup.run();
            root.getChildren().remove(transitionRect);
        });

        fadeIn.play();
        activeAnimations.add(fadeIn);
    }

    private void fadeOutAndClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> primaryStage.close());
        fade.play();
        activeAnimations.add(fade);
    }

    private void playButtonPressAnimation(Button button) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(scaleDown, scaleUp);
        sequence.play();
        activeAnimations.add(sequence);
    }

    private void showNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: rgba(0,0,0,0.8); " +
                "-fx-text-fill: white; -fx-padding: 12 25; " +
                "-fx-background-radius: 20; -fx-font-size: 16;");
        notification.setEffect(new DropShadow(10, COLORS.get("PRIMARY")));

        StackPane root = (StackPane) primaryStage.getScene().getRoot();
        root.getChildren().add(notification);

        StackPane.setAlignment(notification, Pos.BOTTOM_CENTER);
        StackPane.setMargin(notification, new Insets(0, 0, 40, 0));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notification);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), notification);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), notification);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), notification);
        fadeOut.setDelay(Duration.seconds(2));
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(notification));

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(fadeIn, scaleIn),
                pulse,
                fadeOut
        );
        sequence.play();
        activeAnimations.add(sequence);
    }

    private void animateTextGlow(Label label, DropShadow glow) {
        Timeline glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 15)),
                new KeyFrame(Duration.seconds(2), new KeyValue(glow.radiusProperty(), 25)),
                new KeyFrame(Duration.seconds(4), new KeyValue(glow.radiusProperty(), 15))
        );
        glowTimeline.setCycleCount(Animation.INDEFINITE);

        Timeline colorTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.colorProperty(), COLORS.get("PRIMARY"))),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(glow.colorProperty(), COLORS.get("SECONDARY"))),
                new KeyFrame(Duration.seconds(6),
                        new KeyValue(glow.colorProperty(), COLORS.get("ACCENT"))),
                new KeyFrame(Duration.seconds(9),
                        new KeyValue(glow.colorProperty(), COLORS.get("PRIMARY")))
        );
        colorTimeline.setCycleCount(Animation.INDEFINITE);

        ParallelTransition parallel = new ParallelTransition(glowTimeline, colorTimeline);
        parallel.play();
        activeAnimations.add(parallel);
    }


}*/