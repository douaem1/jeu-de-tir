
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
    public Label levelLabel;
    public HUD() {
        this.gameManager = null;
    }

    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void setupHUD() {
        hudContainer = new BorderPane();
        hudContainer.setPrefWidth(GameManager.WINDOW_WIDTH);
        hudContainer.setPrefHeight(GameManager.WINDOW_HEIGHT);
        hudContainer.setMouseTransparent(true);
        HBox topHUD = new HBox(20);
        topHUD.setAlignment(Pos.TOP_LEFT);
        topHUD.setPadding(new Insets(15));
        levelLabel = new Label("NIVEAU: 1");
        levelLabel.setFont(Font.font("Agency FB", FontWeight.BOLD, 28));
        levelLabel.setTextFill(Color.WHITE);
        levelLabel.setEffect(new DropShadow(5, Color.BLACK));

        topHUD.getChildren().add(levelLabel);
        scoreLabel = new Label("SCORE: 0");
        scoreLabel.setFont(Font.font("Agency FB", FontWeight.BOLD, 28));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setEffect(new DropShadow(5, Color.BLACK));
        livesLabel = new Label("VIES: 3");
        livesLabel.setFont(Font.font("Agency FB", FontWeight.BOLD, 28));
        livesLabel.setTextFill(Color.WHITE);
        livesLabel.setEffect(new DropShadow(5, Color.BLACK));
        topHUD.getChildren().addAll(scoreLabel, livesLabel);
        hudContainer.setTop(topHUD);
    }

    //  mettre à jour le score
    public void updateScore(int newScore) {
        if (scoreLabel != null) {
            Platform.runLater(() -> {
                scoreLabel.setText("SCORE: " + newScore);
                System.out.println("HUD mis à jour avec score: " + newScore);
            });
        }
    }
    public void updateLives(int newLives) {
        Platform.runLater(() -> {
            if (livesLabel != null) {
                livesLabel.setText("VIES: " + newLives);
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
