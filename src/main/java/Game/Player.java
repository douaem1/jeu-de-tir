package Game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;


public class Player {
    public  GameManager gameManager;
    private String selectedAircraft;
    private final double LASER_SPEED = 10.0;

    public Player(GameManager gameManager) {
        this.gameManager = gameManager;
        if (gameManager != null) {
            this.selectedAircraft = gameManager.selectedAircraft;
        }
    }



    public void fireEnhancedLaser(Pane gamePane, ImageView player) {
        System.out.println("Tentative de tir: " + (gameManager != null ? "Manager OK" : "Manager NULL"));
        System.out.println("Position du laser: X=" + (player.getX() + player.getFitWidth()/2) + ", Y=" + player.getY());
        Rectangle laser = new Rectangle(6, 25, Color.LIMEGREEN);
        laser.setX(player.getX() + player.getFitWidth()/2 - 3);
        laser.setY(player.getY() - 10);
        laser.setArcWidth(4);
        laser.setArcHeight(4);
        Platform.runLater(() -> {
            gamePane.getChildren().add(laser);
            System.out.println("Laser ajouté à la scène");
        });
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(16), event -> {
                    laser.setY(laser.getY() - LASER_SPEED);
                    if (gameManager != null) {
                        gameManager.checkLaserCollisions(gamePane, laser);
                    }
                    if (laser.getY() < -30) {
                        Platform.runLater(() -> {
                            gamePane.getChildren().remove(laser);
                        });
                    }
                })
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        if (gameManager != null) {
            gameManager.activeAnimations.add(timeline);
        }
    }

    public ImageView createPlayer() {
        try {
            String aircraftImagePath = getAircraftImagePath(selectedAircraft);
            Image image = new Image(getClass().getResourceAsStream(aircraftImagePath));

            ImageView player = new ImageView(image);
            player.setFitWidth(100);
            player.setPreserveRatio(true);
            player.setX(GameManager.WINDOW_WIDTH / 2 - 50);
            player.setY(GameManager.WINDOW_HEIGHT - 150);

            return player;

        } catch (Exception e) {
            System.err.println("Erreur chargement joueur: " + e.getMessage());
            Rectangle fallbackPlayer = new Rectangle(100, 80, Color.BLUE);
            fallbackPlayer.setX(GameManager.WINDOW_WIDTH / 2 - 50);
            fallbackPlayer.setY(GameManager.WINDOW_HEIGHT - 150);
            ImageView fallbackView = new ImageView();
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            Image fallbackImage = fallbackPlayer.snapshot(params, null);
            fallbackView.setImage(fallbackImage);
            return fallbackView;
        }
    }

    private String getAircraftImagePath(String aircraftName) {
        if (aircraftName == null) {
            System.out.println("Warning: No aircraft selected, using default");
            return "/Spaceship_02_ORANGE.png";
        }
        switch (aircraftName) {
            case "F-22 Raptor":
                return "/Spaceship_04_RED.png";
            case "Eurofighter Typhoon":
                return "/Spaceship_05_BLUE.png";
            case "Sukhoi Su-57":
                return "/Spaceship_02_ORANGE.png";
            default:
                System.out.println("Unknown aircraft: " + aircraftName + ", using default");
                return "/Spaceship_02_ORANGE.png";
        }
    }
}