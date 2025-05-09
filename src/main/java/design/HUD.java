
package design;

import Game.GameManager;

import javafx.animation.*;
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
    public void updateScore() {
        if (scoreLabel != null) {
            Platform.runLater(() -> {
                int newScore = gameManager.score;
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
            updateScore();
            updateLives(gameManager.lives);
         //   updateLevel(gameManager.currentLevel);
        }
    }

    public void updateLivesDirect(int newValue) {
        Platform.runLater(() -> {
            livesLabel.setText("VIES: " + newValue);
        });
    }
    /**
     * Méthodes à ajouter à votre classe HUD pour gérer l'indicateur de mode multijoueur
     */

// Attribut à ajouter à la classe HUD
    private Label multiplayerIndicator;

    /**
     * Crée et affiche l'indicateur de mode multijoueur dans le HUD.
     */
    public void showMultiplayerIndicator() {
        if (multiplayerIndicator == null) {
            multiplayerIndicator = new Label("MULTIPLAYER");
            multiplayerIndicator.setTextFill(Color.YELLOW);
            multiplayerIndicator.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            multiplayerIndicator.setPadding(new Insets(5, 10, 5, 10));
            multiplayerIndicator.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 5;");

            // Positionner l'indicateur en haut à droite de l'écran
            StackPane.setAlignment(multiplayerIndicator, Pos.TOP_RIGHT);
            StackPane.setMargin(multiplayerIndicator, new Insets(10, 10, 0, 0));

            // Ajouter un effet de pulsation pour attirer l'attention
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(multiplayerIndicator.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(1), new KeyValue(multiplayerIndicator.opacityProperty(), 0.6)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(multiplayerIndicator.opacityProperty(), 1.0))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        }

        // S'assurer que l'indicateur est visible et ajouté au HUD

        if (hudContainer != null && hudContainer.getChildren().contains(multiplayerIndicator) == false) {
            ((Pane) hudContainer.getCenter()).getChildren().add(multiplayerIndicator);
        }

        multiplayerIndicator.setVisible(true);
    }

    /**
     * Cache l'indicateur de mode multijoueur dans le HUD.
     */
    public void hideMultiplayerIndicator() {
        if (multiplayerIndicator != null) {
            multiplayerIndicator.setVisible(false);
        }
    }

    /**
     * Méthode pour afficher des informations sur les joueurs connectés
     *
     * @param connectedPlayers nombre de joueurs connectés
     */
    public void updateMultiplayerStatus(int connectedPlayers) {
        if (multiplayerIndicator != null) {
            multiplayerIndicator.setText("MULTIPLAYER (" + connectedPlayers + ")");
        }
    }

}
