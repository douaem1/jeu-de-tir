package Ui;
/*
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class UIFactory {

    private Button createActionButton(String text, String colorKey) {
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

    private TextField createStylizedTextField(String promptText) {
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

    private PasswordField createStylizedPasswordField(String promptText) {
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
}*/