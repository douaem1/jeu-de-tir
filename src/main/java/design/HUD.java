package design;

import Game.GameManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HUD {
    private GameManager gameManager;
    public BorderPane hudContainer;
    public Label scoreLabel;
    public Label livesLabel;

    // Référence à la classe HUD depuis GameManager
    public HUD() {
        // Constructeur vide pour la compatibilité existante
    }

    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void setupHUD() {
        hudContainer = new BorderPane();
        hudContainer.setPrefWidth(GameManager.WINDOW_WIDTH);
        hudContainer.setPrefHeight(GameManager.WINDOW_HEIGHT);
        hudContainer.setMouseTransparent(true);

        // Créer le HUD supérieur pour le score
        HBox topHUD = new HBox(20);
        topHUD.setAlignment(Pos.TOP_LEFT);
        topHUD.setPadding(new Insets(15));

        // Label du score avec style amélioré
        scoreLabel = new Label("SCORE: 0");
        scoreLabel.setFont(Font.font("Agency FB", FontWeight.BOLD, 28));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setEffect(new DropShadow(5, Color.BLACK));

        // Label des vies avec style amélioré
        livesLabel = new Label("VIES: 3");
        livesLabel.setFont(Font.font("Agency FB", FontWeight.BOLD, 28));
        livesLabel.setTextFill(Color.WHITE);
        livesLabel.setEffect(new DropShadow(5, Color.BLACK));

        // Ajouter les labels au HUD
        topHUD.getChildren().addAll(scoreLabel, livesLabel);

        // Placer le HUD en haut de l'écran
        hudContainer.setTop(topHUD);
    }

    // Méthode spécifique pour mettre à jour le score
    public void updateScore(int newScore) {
        if (scoreLabel != null) {
            Platform.runLater(() -> {
                scoreLabel.setText("SCORE: " + newScore);
                System.out.println("HUD mis à jour avec score: " + newScore);
            });
        }
    }

    // Méthode spécifique pour mettre à jour les vies
    public void updateLives(int newLives) {
        if (livesLabel != null) {
            Platform.runLater(() -> {
                livesLabel.setText("VIES: " + newLives);
            });
        }
    }

    // Méthode générale pour mettre à jour tout le HUD
    public void updateHUD() {
        if (gameManager != null) {
            updateScore(gameManager.score);
            updateLives(gameManager.lives);
        }
    }
}