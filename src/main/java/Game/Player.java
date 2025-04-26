package Game;


import javafx.animation.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.Stage;

import static Game.GameManager.*;

import java.util.concurrent.CopyOnWriteArrayList;
  // Package correct

import javafx.scene.image.ImageView;
import Game.GameManager;

public class Player {

    public Stage primaryStage;
    public ImageView player;
    public final CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    public int score = 0;
    public int lives = 3;
    public Pane gamepane;
    public final double LASER_SPEED = 10.0;
    public void fireEnhancedLaser(Pane gamePane, ImageView player, Pane hud) {
        GameManager gameManager= new GameManager();
        if (!gameManager.gameRunning) return;

        Rectangle laser = new Rectangle(4, 20, Color.LIMEGREEN);
        laser.setX(player.getX() + player.getFitWidth()/2 - 2);
        laser.setY(player.getY());
        gamePane.getChildren().add(laser);

        AnimationTimer laserAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                laser.setY(laser.getY() - LASER_SPEED);
                GameManager gameManager = new GameManager();

                // Vérifier les collisions
                gameManager.checkLaserCollisions(gamePane, laser);

                // Supprimer le laser s'il sort de l'écran
                if (laser.getY() < 0 || !gamePane.getChildren().contains(laser)) {
                    gamePane.getChildren().remove(laser);
                    this.stop();
                }
            }
        };
        laserAnimation.start();
    }

    public ImageView createPlayer() {
        try {
            // 1. Chargement de l'image
            Image image = new Image(getClass().getResourceAsStream("/airplane.png"));

            // 2. Création du ImageView
            ImageView player = new ImageView(image);

            // 3. Configuration de la taille
            player.setFitWidth(100);  // Correction de la casse (setFitWidth au lieu de setfitWidth)
            player.setPreserveRatio(true); // Conserve les proportions

            // 4. Positionnement initial
            player.setX(WINDOW_WIDTH / 2 - 100); // Centré horizontalement
            player.setY(WINDOW_HEIGHT - 150);    // 150px du bas

            // 5. Ajout au gamePane (si nécessaire)


            return player; // Retourne l'objet créé

        } catch (Exception e) {
            System.err.println("Erreur chargement joueur: " + e.getMessage());

            // Fallback si l'image ne charge pas
            Rectangle placeholder = new Rectangle(200, 100, Color.BLUE);
            placeholder.setStroke(Color.WHITE);
            return new ImageView(placeholder.snapshot(null, null));
        }
    }

}
