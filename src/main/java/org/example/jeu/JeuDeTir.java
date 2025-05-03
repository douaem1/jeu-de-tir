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
        JeuDeTir.primaryStage = stage;
        StackPane root = new StackPane();
        Scene scene = new Scene(root, GameManager.WINDOW_WIDTH, GameManager.WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("Jet Fighters");
        stage.setResizable(false);
        gameManager = new GameManager();
        gameManager.setPrimaryStage(stage);
        gameManager.setupMainMenu();
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
    public static void main(String[] args) {
        launch(args);
    }
}