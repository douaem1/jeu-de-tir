package design;

import Game.GameManager;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class HUD {
    private GameManager gameManager;
    public BorderPane hudContainer;
    public Label scoreLabel;
    public Label livesLabel;
    public Label levelLabel;  // Nouveau label pour le niveau

    // Référence à la classe HUD depuis GameManager
    public HUD() {
        this.gameManager = null;
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
//*Ce que j ai ajouté pour levels *********************
        levelLabel = new Label("NIVEAU: 1");
        levelLabel.setFont(Font.font("Agency FB", FontWeight.BOLD, 28));
        levelLabel.setTextFill(Color.WHITE);
        levelLabel.setEffect(new DropShadow(5, Color.BLACK));

        topHUD.getChildren().add(levelLabel); // Ajoute à côté du score
//**********************************************************************************
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
        Platform.runLater(() -> {
            if (livesLabel != null) {
            // Format simple et direct
            livesLabel.setText("VIES: " + newLives);

                // Animation optionnelle mais contrôlée
                try {
                    String currentText = livesLabel.getText().replace("VIES: ", "");
                    int currentLives = Integer.parseInt(currentText);
                    if (newLives > currentLives) {
                        animateLifeGain();
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Erreur de format des vies: " + e.getMessage());
                    livesLabel.setText("VIES: " + newLives);
                }
            }
        });
    }

    private void animateLifeGain() {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), livesLabel);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.3);
        st.setToY(1.3);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        System.out.println("=== COLLISION POWER-UP ===");
        System.out.println("Vies avant: " + gameManager.lives);
        System.out.println("Vies après: " + (gameManager.lives + 1));
    }

    public void updateLevel(int newlevel) {
        if (levelLabel != null) {
            Platform.runLater(() -> levelLabel.setText("NIVEAU: " + newlevel));
        }
    }



    // Méthode générale pour mettre à jour tout le HUD
    public void updateHUD() {
        if (gameManager != null) {
            updateScore(gameManager.score);
            updateLives(gameManager.lives);
            updateLevel(gameManager.currentLevel);
        }
    }

    public void updateLivesDirect(int newValue) {
        Platform.runLater(() -> {
            livesLabel.setText("VIES: " + newValue);
        });
    }

}
