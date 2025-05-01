package Game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import java.io.*;
import java.util.concurrent.*;
import Game.GameManager;

import static Game.GameManager.*;

public class Enemy {
    private Stage primaryStage;
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    public final CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Rectangle> enemyLasers = new CopyOnWriteArrayList<>();
    private GameManager gameManager;
    public Enemy(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    public  Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),
            "DARK", Color.web("#1A1A2E")
    );

    public void updateEnemies() {
        enemies.removeIf(enemy -> {
            // Mettre à jour la position de l'ennemi
            enemy.setY(enemy.getY() + gameManager.enemySpeed);

            // Si l'ennemi sort de l'écran
            if (enemy.getY() > GameManager.WINDOW_HEIGHT) {
                Platform.runLater(() -> gameManager.gamepane.getChildren().remove(enemy));
                return true; // Supprime l'ennemi de la liste
            }
            return false;
        });

        // Mettre à jour les lasers ennemis
        updateEnemyLasers();
    }

    // ========================= GRAPHIQUES =========================

   public void spawnEnemy() {
        try {
            Image image = new Image(getClass().getResourceAsStream("/enemy.png"));
            ImageView enemy = new ImageView(image);
            enemy.setFitWidth(150);
            enemy.setX(Math.random() * (WINDOW_WIDTH - 150));
            enemies.add(enemy);

            gamepane.getChildren().add(enemy);
            gameManager.animateEnemy(enemy);
        } catch (Exception e) {
            System.err.println("Erreur chargement ennemi: " + e.getMessage());
        }
    }
    public ImageView createEnemyAirplane() {
        try {
            Image enemyImage = new Image(getClass().getResourceAsStream("/enemy.png"));
            ImageView enemy = new ImageView(enemyImage);

            double width = 100;
            double height = 50;
            enemy.setFitWidth(width);
            enemy.setFitHeight(height);
            enemy.setPreserveRatio(true);
            enemy.setX(Math.random() * (GameManager.WINDOW_WIDTH - width));
            enemy.setY(-height);
            enemy.setEffect(new DropShadow(10, Color.RED));

            // Démarrer le tir si niveau >= 5
            if (gameManager.currentLevel >=6) {
                scheduleEnemyShots(enemy);
            }

            return enemy;
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image ennemi: " + e.getMessage());
            return createFallbackEnemy();
        }
    }
    private ImageView createFallbackEnemy() {
        // 2. Fallback graphique si l'image n'est pas trouvée
        Polygon enemyShape = new Polygon(
                0.0, 20.0,
                15.0, 0.0,
                30.0, 20.0,
                25.0, 20.0,
                25.0, 40.0,
                5.0, 40.0,
                5.0, 20.0
        );

        enemyShape.setFill(COLORS.get("DANGER"));  // Rouge
        enemyShape.setStroke(COLORS.get("LIGHT")); // Bordure blanche
        enemyShape.setStrokeWidth(2);

        // Création d'une ImageView à partir du Polygon
        ImageView fallbackEnemy = new ImageView(enemyShape.snapshot(null, null));

        // Générer des dimensions aléatoires pour le fallback
        double fallbackWidth = 80 + Math.random() * 70;  // Largeur entre 80 et 150
        double fallbackHeight = 30 + Math.random() * 30; // Hauteur entre 30 et 60

        // Appliquer les dimensions aléatoires
        fallbackEnemy.setFitWidth(fallbackWidth);
        fallbackEnemy.setFitHeight(fallbackHeight);
        fallbackEnemy.setX(Math.random() * (WINDOW_WIDTH - fallbackWidth));
        fallbackEnemy.setY(-fallbackHeight);

        return fallbackEnemy;
    }

    // ******Méthode de planification des tirs *********
    private void scheduleEnemyShots(ImageView enemy) {
        Random random = new Random();
        Timeline shotTimer = new Timeline(
                new KeyFrame(Duration.seconds(2 + random.nextDouble() * 3), e -> {
                    if (gameManager.gameRunning && gameManager.gamepane.getChildren().contains(enemy)) {
                        fireEnemyLaser(enemy);  // <-- Tir effectif
                        scheduleEnemyShots(enemy); // Planifier le prochain tir (Répétition)
                    }
                })
        );
        shotTimer.play();
        gameManager.activeAnimations.add(shotTimer);
    }

    // ***********Méthode de création du laser *************
    public void fireEnemyLaser(ImageView enemy) {
        if (gameManager.currentLevel >=6 && gameManager.gameRunning) {
            Rectangle laser = new Rectangle(4, 15, Color.RED);
            laser.setX(enemy.getX() + enemy.getFitWidth() / 2 - 2);
            laser.setY(enemy.getY() + enemy.getFitHeight());
            if (gameManager.gamepane != null) {
                gameManager.gamepane.getChildren().add(laser);
                enemyLasers.add(laser);

                Timeline laserAnimation = new Timeline(
                        new KeyFrame(Duration.millis(16), e -> {
                            if (!gameManager.gameRunning) {
                                ((Timeline) e.getSource()).stop();
                                return;
                            }

                            laser.setY(laser.getY() + 5);

                            if (gameManager.lives > 0
                                    && gameManager.player != null
                                    && laser.getBoundsInParent().intersects(gameManager.player.getBoundsInParent())) {
                                gameManager.handlePlayerHit();
                                removeLaser(laser);
                                ((Timeline) e.getSource()).stop();
                            }

                            // Vérifier collision avec joueur
                            if (gameManager.player != null && laser.getBoundsInParent().intersects(gameManager.player.getBoundsInParent())) {
                                gameManager.handlePlayerHit();
                                removeLaser(laser);
                                ((Timeline) e.getSource()).stop();
                                System.out.println("Collision laser-joueur à X:" + laser.getX() + " Y:" + laser.getY());
                                System.out.println("Position joueur: " + gameManager.player.getBoundsInParent());
                            }

                            // Supprimer si hors écran
                            if (laser.getY() > GameManager.WINDOW_HEIGHT) {
                                removeLaser(laser);
                                ((Timeline) e.getSource()).stop();
                            }
                        })
                );
                laserAnimation.setCycleCount(Animation.INDEFINITE);
                laserAnimation.play();
                gameManager.activeAnimations.add(laserAnimation);
            }
        }
    }

    private void updateEnemyLasers() {
        enemyLasers.removeIf(laser -> {
            if (!gameManager.gamepane.getChildren().contains(laser)) {
                return true;
            }
            return false;
        });
    }

    private void removeLaser(Rectangle laser) {
        Platform.runLater(() -> {
            gameManager.gamepane.getChildren().remove(laser);
            enemyLasers.remove(laser);
        });
    }

}
