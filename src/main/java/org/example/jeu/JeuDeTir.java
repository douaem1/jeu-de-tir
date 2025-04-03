package org.example.jeu;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.io.File;

public class JeuDeTir extends Application {
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        afficherEcranDemarrage();
    }

    private void afficherEcranDemarrage() {
        StackPane root = new StackPane();

        // Chargement de l'image de fond
        ImageView background = chargerImage("C:/Users/douae/Downloads/anime-girls-airport-planes-sunset-wallpaper-3de72376c0e02750aacbef6eb2801425.jpg", 1024, 768);

        VBox menu = new VBox(15);
        menu.setAlignment(Pos.CENTER);
        menu.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-padding: 30px; -fx-border-radius: 15px;");

        Label titre = new Label("Jeu de Tir");
        titre.setFont(new Font("Arial", 40));
        titre.setTextFill(Color.WHITE);
        titre.setTextAlignment(TextAlignment.CENTER);
        titre.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 5, 0.5, 2, 2);");

        Label label = new Label("Entrez votre nom :");
        label.setFont(new Font("", 22));
        label.setTextFill(Color.LIGHTGRAY);

        TextField nomJoueur = new TextField();
        nomJoueur.setPromptText("Votre nom...");
        nomJoueur.setStyle("-fx-background-color: white; -fx-border-radius: 15px; -fx-font-size: 18px; -fx-padding: 8px;");

        // Boutons avec des couleurs adaptées à l’image
        Button boutonCommencer = creerBouton("Bataille Rapide", "#FF847C");   // Corail doux
        Button boutonPerso = creerBouton("Partie Personnalisée", "#D291BC");  // Violet rosé
        Button boutonEquipement = creerBouton("Équipement", "#E6AF2E");       // Or doux
        Button boutonClassement = creerBouton("Classement", "#A8C686");       // Vert pastel
        Button boutonQuitter = creerBouton("Quitter", "#CC444B");             // Rouge doux
        boutonQuitter.setOnAction(e -> primaryStage.close());

        // Ajout au menu
        menu.getChildren().addAll(titre, label, nomJoueur, boutonCommencer, boutonPerso, boutonEquipement, boutonClassement, boutonQuitter);

        root.getChildren().addAll(background, menu);

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setTitle("Jeu de Tir - Menu Principal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void lancerJeu(String nom) {
        System.out.println("Le jeu commence pour : " + nom);
    }


    private ImageView chargerImage(String chemin, double largeur, double hauteur) {
        File fichier = new File(chemin);
        ImageView imageView;
        if (fichier.exists()) {
            Image image = new Image(fichier.toURI().toString());
            imageView = new ImageView(image);
        } else {
            System.out.println("⚠️ Image introuvable : " + chemin);
            imageView = new ImageView();
        }
        imageView.setFitWidth(largeur);
        imageView.setFitHeight(hauteur);
        return imageView;
    }

    private Button creerBouton(String texte, String couleur) {
        Button bouton = new Button(texte);
        bouton.setStyle("-fx-background-color: " + couleur + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 20px; " +
                "-fx-padding: 15px 40px; " +
                "-fx-border-radius: 25px; " +
                "-fx-opacity: 0.9;"); // Ajout de transparence légère

        // Effet de survol plus doux
        bouton.setOnMouseEntered(e -> bouton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: " + couleur + "; " +
                        "-fx-font-size: 20px; " +
                        "-fx-padding: 15px 40px; " +
                        "-fx-border-radius: 25px; " +
                        "-fx-border-color: " + couleur + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-opacity: 1;"));

        bouton.setOnMouseExited(e -> bouton.setStyle(
                "-fx-background-color: " + couleur + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 20px; " +
                        "-fx-padding: 15px 40px; " +
                        "-fx-border-radius: 25px; " +
                        "-fx-opacity: 0.9;"));

        return bouton;
    }

}
