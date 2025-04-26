package org.example.jeu;

import Menu.Authentification;
import javafx.application.Application;
import javafx.stage.Stage;
import Game.GameManager;

public class JeuDeTir extends Application {
    private static Stage primaryStage;// Correction ici

    private GameManager gameManager = new GameManager();

    @Override // Ajouté
    public void start(Stage primaryStage) {
        gameManager.setPrimaryStage(primaryStage); // Transmettez le Stage
        gameManager.setupMainMenu();
        primaryStage.setTitle("Jet Fighters");
        primaryStage.show();
    }

    @Override
    public void stop() {
        gameManager.stopGame();
    }
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}