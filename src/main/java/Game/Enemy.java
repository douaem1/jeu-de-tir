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
    private GameManager gameManager;
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
            enemy.setY(enemy.getY() + 3);

            // Si l'ennemi sort de l'écran
            if (enemy.getY() > WINDOW_HEIGHT) {
                Platform.runLater(() -> gamepane.getChildren().remove(enemy));
                return true; // Supprime l'ennemi de la liste
            }
            return false;
        });
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
            //  charger l'image depuis les ressources
            InputStream is = getClass().getResourceAsStream("/enemy.png");
            if (is != null) {
                Image enemyImage = new Image(is);
                ImageView enemy = new ImageView(enemyImage);

                // Générer des dimensions aléatoires pour la largeur et la hauteur
                double width = 100; // Largeur entre 80 et 150
                double height = 50;  // Hauteur entre 30 et 60

                // Appliquer les dimensions aléatoires
                enemy.setFitWidth(width);
                enemy.setFitHeight(height);
                enemy.setPreserveRatio(true);

                // Position aléatoire en X, apparition hors écran en Y
                enemy.setX(Math.random() * (WINDOW_WIDTH - width));
                enemy.setY(-height);  // Commence au-dessus de l'écran

                // Effet visuel optionnel
                enemy.setEffect(new DropShadow(10, Color.RED));

                return enemy;
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image ennemi: " + e.getMessage());
        }

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
}
