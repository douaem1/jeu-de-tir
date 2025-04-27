package org.example.jeu;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import Game.GameManager;

public class JeuDeTir extends Application {
    private static Stage primaryStage;
    private GameManager gameManager;

    @Override
    public void start(Stage stage) {
        // Stockage de référence au primaryStage
        JeuDeTir.primaryStage = stage;

        // Configuration initiale de la scène avec une racine vide
        StackPane root = new StackPane();
        Scene scene = new Scene(root, GameManager.WINDOW_WIDTH, GameManager.WINDOW_HEIGHT);
        stage.setScene(scene);

        // Configuration du stage
        stage.setTitle("Jet Fighters");
        stage.setResizable(false);

        // Création du GameManager
        gameManager = new GameManager();
        gameManager.setPrimaryStage(stage);

        // Configuration du menu principal
        gameManager.setupMainMenu();

        // Affichage du stage
        stage.show();

        System.out.println("Application démarrée avec succès!");
    }

    @Override
    public void stop() {
        if (gameManager != null) {
            gameManager.stopGame();
        }
        System.out.println("Application arrêtée");
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}