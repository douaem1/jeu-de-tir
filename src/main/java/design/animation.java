package design;

import Game.GameManager;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;


import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


public class animation {
    private Stage primaryStage;
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    public Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),
            "DARK", Color.web("#1A1A2E")
    );

    private GameManager gameManager;
    public CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    public static Pane gamepane;
    public void playButtonPressAnimation(Button button) {
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
    public void animateTextGlow(Label label, DropShadow glow) {
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
    public void animateFormEntrance(VBox form) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), form);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(0.6), form);
        slide.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
        activeAnimations.add(parallel);

    }
    public Button createActionButton(String text, String colorKey) {
        Color color = COLORS.get(colorKey);
        String hexColor = toHex(color);

        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + hexColor + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 18; " +
                "-fx-padding: 15 0; " +
                "-fx-background-radius: 30; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
        btn.setAlignment(Pos.CENTER);

        btn.setOnMouseEntered(e -> {
            String lightenedColor = toHex(color.brighter());
            btn.setStyle(btn.getStyle().replace(hexColor, lightenedColor) +
                    "-fx-effect: dropshadow(gaussian, " + hexColor + ", 15, 0.5, 0, 0);");

            ScaleTransition pulse = new ScaleTransition(Duration.millis(300), btn);
            pulse.setToX(1.05);
            pulse.setToY(1.05);
            pulse.play();
            activeAnimations.add(pulse);
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(btn.getStyle()
                    .replace(toHex(color.brighter()), hexColor)
                    .replace("-fx-effect:.*;", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);"));

            ScaleTransition resetScale = new ScaleTransition(Duration.millis(300), btn);
            resetScale.setToX(1.0);
            resetScale.setToY(1.0);
            resetScale.play();
            activeAnimations.add(resetScale);
        });

        return btn;
    }
    public void fadeOutAndClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> primaryStage.close());
        fade.play();
        activeAnimations.add(fade);
    }



    public TextField createStylizedTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-text-fill: white; -fx-prompt-text-fill: lightgray; " +
                "-fx-padding: 12; -fx-background-radius: 8;");
        field.setPrefWidth(300);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() +
                        "-fx-background-color: rgba(255,255,255,0.25); " +
                        "-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);");
            } else {
                field.setStyle(field.getStyle()
                        .replace("-fx-background-color: rgba(255,255,255,0.25);",
                                "-fx-background-color: rgba(255,255,255,0.15);")
                        .replace("-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);", ""));
            }
        });

        return field;
    }

    public PasswordField createStylizedPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-text-fill: white; -fx-prompt-text-fill: lightgray; " +
                "-fx-padding: 12; -fx-background-radius: 8;");
        field.setPrefWidth(300);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() +
                        "-fx-background-color: rgba(255,255,255,0.25); " +
                        "-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);");
            } else {
                field.setStyle(field.getStyle()
                        .replace("-fx-background-color: rgba(255,255,255,0.25);",
                                "-fx-background-color: rgba(255,255,255,0.15);")
                        .replace("-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);", ""));
            }
        });

        return field;
    }
    public String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }


}
