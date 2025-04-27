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
    // Référence directe au GameManager parent
    private GameManager gameManager;

    // Constantes
    private final double LASER_SPEED = 10.0;

    public Player(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void fireEnhancedLaser(Pane gamePane, ImageView player) {
        System.out.println("Tentative de tir: " + (gameManager != null ? "Manager OK" : "Manager NULL"));

        // Vérification visuelle dans la console pour s'assurer que le tir est tenté
        System.out.println("Position du laser: X=" + (player.getX() + player.getFitWidth()/2) + ", Y=" + player.getY());

        // Création du laser
        Rectangle laser = new Rectangle(6, 25, Color.LIMEGREEN);
        laser.setX(player.getX() + player.getFitWidth()/2 - 3);
        laser.setY(player.getY() - 10); // Commence juste au-dessus du joueur

        // Ajout d'un effet visuel pour mieux voir le laser
        laser.setArcWidth(4);
        laser.setArcHeight(4);

        // Ajout du laser à la scène
        Platform.runLater(() -> {
            gamePane.getChildren().add(laser);
            System.out.println("Laser ajouté à la scène");
        });

        // Animation du mouvement du laser
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(16), event -> {
                    laser.setY(laser.getY() - LASER_SPEED);

                    // Vérification des collisions
                    if (gameManager != null) {
                        gameManager.checkLaserCollisions(gamePane, laser);
                    }

                    // Suppression du laser s'il sort de l'écran
                    if (laser.getY() < -30) {
                        Platform.runLater(() -> {
                            gamePane.getChildren().remove(laser);
                        });
                    }
                })
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Ajouter aux animations actives pour pouvoir l'arrêter plus tard
        if (gameManager != null) {
            gameManager.activeAnimations.add(timeline);
        }
    }

    public ImageView createPlayer() {
        try {
            // Chargement de l'image
            Image image = new Image(getClass().getResourceAsStream("/airplane.png"));

            // Création du ImageView
            ImageView player = new ImageView(image);

            // Configuration de la taille
            player.setFitWidth(100);
            player.setPreserveRatio(true);

            // Positionnement initial
            player.setX(GameManager.WINDOW_WIDTH / 2 - 50);
            player.setY(GameManager.WINDOW_HEIGHT - 150);

            return player;

        } catch (Exception e) {
            System.err.println("Erreur chargement joueur: " + e.getMessage());
            e.printStackTrace();

            // Fallback si l'image ne charge pas
            Rectangle placeholder = new Rectangle(100, 60, Color.BLUE);
            placeholder.setStroke(Color.WHITE);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            ImageView fallback = new ImageView(placeholder.snapshot(params, null));
            fallback.setX(GameManager.WINDOW_WIDTH / 2 - 50);
            fallback.setY(GameManager.WINDOW_HEIGHT - 150);

            return fallback;
        }
    }
}