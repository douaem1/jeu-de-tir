package Game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.scene.control.Label; // Import correct pour Label
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.Random;

import java.awt.*;
import java.util.*;

public class PowerUp {
    private GameManager gameManager;
    private ImageView powerUpView;
    private String type;
    private boolean activated = false; // Nouveau flag pour empêcher les doubles activations

    public PowerUp(GameManager gameManager, double x, double y) {
        this.gameManager = gameManager;
        String[] types = {"EXTRA_LIFE", "DOUBLE_SCORE", "SLOW_ENEMIES"};
        this.type = types[new Random().nextInt(types.length)];

        try {
            powerUpView = new ImageView(new Image(getClass().getResourceAsStream(
                    "/powerup_" + type.toLowerCase() + ".png")));
        } catch (Exception e) {
            // Fallback visuel si l'image n'est pas trouvée
            powerUpView = new ImageView();
            Circle circle = new Circle(15);
            powerUpView.setImage(circle.snapshot(null, null));
        }

        powerUpView.setX(x);
        powerUpView.setY(y);
        powerUpView.setFitWidth(30);
        powerUpView.setFitHeight(30);
    }

    public ImageView getView() {
        return powerUpView;
    }

    public void activate() {
        if (activated) return; // Ne pas réactiver si déjà utilisé
        switch(type) {
            case "EXTRA_LIFE":
                activated = true; // Marquer comme utilisé

                // Version ultra-fiable avec vérification
                int oldLives = gameManager.lives;
                if (oldLives < 5) {
                    gameManager.lives = oldLives + 1; // Incrément atomique

                    System.out.println("[POWERUP] Vie ajoutée. Ancien: " + oldLives
                            + ", Nouveau: " + gameManager.lives);

                    // Mise à jour unique du HUD
                    gameManager.hud.updateLivesDirect(gameManager.lives);

                    showLifeGainFeedback();
                }
                break;
            case "DOUBLE_SCORE":
                gameManager.scoreMultiplier = 2;
                Timeline resetMultiplier = new Timeline(
                        new KeyFrame(Duration.seconds(10), e -> gameManager.scoreMultiplier = 1)
                );
                resetMultiplier.play();
                gameManager.activeAnimations.add(resetMultiplier);
                break;
            case "SLOW_ENEMIES":
                  activated = true;
                  gameManager.slowEnemiesTemporarily(8);
                  break;
        }
    }

    public void animate() {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(16), e ->  {
                    powerUpView.setY(powerUpView.getY() + 2);
                    if (powerUpView.getY() > GameManager.WINDOW_HEIGHT) {
                        gameManager.gamepane.getChildren().remove(powerUpView);
                    }

                    if (powerUpView.getBoundsInParent().intersects(gameManager.player.getBoundsInParent())) {
                        activate();
                        remove();
                        ((Timeline)e.getSource()).stop();
                    }

                    if (powerUpView.getY() > GameManager.WINDOW_HEIGHT) {
                        remove();
                        ((Timeline)e.getSource()).stop();
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        gameManager.activeAnimations.add(animation);
    }

    public void remove() {
        Platform.runLater(() -> {
            if (gameManager.gamepane.getChildren().contains(powerUpView)) {
                gameManager.gamepane.getChildren().remove(powerUpView);
            }
        });
    }

    private void showLifeGainFeedback() {
        Label feedback = new Label("+1");
        feedback.setStyle("-fx-font-size: 24; -fx-text-fill: #00FF00;");
        feedback.setLayoutX(gameManager.player.getX() + 20);
        feedback.setLayoutY(gameManager.player.getY() - 20);

        gameManager.gamepane.getChildren().add(feedback);

        FadeTransition ft = new FadeTransition(Duration.seconds(1), feedback);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> gameManager.gamepane.getChildren().remove(feedback));
        ft.play();
    }


}