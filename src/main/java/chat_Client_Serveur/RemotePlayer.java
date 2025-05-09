package chat_Client_Serveur;

import Game.GameManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Rectangle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Objects;

public class RemotePlayer {
    private GameManager gameManager;
    private String playerId;
    private ImageView playerSprite;
    private Label nameLabel;
    private Label scoreLabel;
    private int score = 0;

    public RemotePlayer(GameManager gameManager, String playerId, String aircraft, double x, double y) {
        this.gameManager = gameManager;
        this.playerId = playerId;

        // Créer le sprite du joueur distant
        String imagePath = "/images/" + aircraft + ".png";
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            playerSprite = new ImageView(image);
            playerSprite.setFitWidth(64);
            playerSprite.setFitHeight(64);
            playerSprite.setX(x);
            playerSprite.setY(y);
            playerSprite.getStyleClass().add("remote-player");
            playerSprite.setUserData(playerId);

            gameManager.gamepane.getChildren().add(playerSprite);

            // Créer un label pour le nom du joueur
            nameLabel = new Label("Joueur " + playerId.substring(0, 4));
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            nameLabel.setTextFill(Color.WHITE);
            nameLabel.setLayoutX(x);
            nameLabel.setLayoutY(y - 15);
            gameManager.gamepane.getChildren().add(nameLabel);

            // Créer un label pour le score
            scoreLabel = new Label("0");
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            scoreLabel.setTextFill(Color.YELLOW);
            scoreLabel.setLayoutX(x);
            scoreLabel.setLayoutY(y - 30);
            gameManager.gamepane.getChildren().add(scoreLabel);
        } catch (Exception e) {
            System.err.println("Error loading remote player image: " + e.getMessage());
        }
    }

    public void updatePosition(double x, double y) {
        if (playerSprite != null) {
            playerSprite.setX(x);
            playerSprite.setY(y);

            nameLabel.setLayoutX(x);
            nameLabel.setLayoutY(y - 15);

            scoreLabel.setLayoutX(x);
            scoreLabel.setLayoutY(y - 30);
        }
    }

    public void createShot(double x, double y) {
        // Créer un tir provenant du joueur distant
        Rectangle laser = new Rectangle(x + 28, y - 20, 8, 20);
        laser.setFill(Color.RED);
        laser.setOpacity(0.8);
        laser.getStyleClass().add("remote-laser");

        gameManager.gamepane.getChildren().add(laser);

        // Animer le tir
        Timeline shotAnimation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    laser.setY(laser.getY() - 10);

                    // Vérifier les collisions avec les ennemis
                    gameManager.checkLaserCollisions(gameManager.gamepane, laser);

                    // Supprimer le laser s'il sort de l'écran
                    if (laser.getY() < -30) {
                        gameManager.gamepane.getChildren().remove(laser);
                    }
                })
        );
        shotAnimation.setCycleCount(50);  // Limiter la durée de l'animation
        shotAnimation.play();
        gameManager.activeAnimations.add(shotAnimation);
    }

    public void updateScore(int newScore) {
        this.score = newScore;
        scoreLabel.setText(String.valueOf(score));
    }

    public void remove() {
        gameManager.gamepane.getChildren().removeAll(playerSprite, nameLabel, scoreLabel);
    }
}