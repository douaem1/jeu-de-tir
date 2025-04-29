package Menu;

import design.animation;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import Game.GameManager;
import javafx.application.Platform;

public class MenuManager {
    private Stage primaryStage;
    private Authentification authentification;
    private GameManager gamemanager;
    private animation animation;

    public MenuManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.gamemanager = new GameManager();
        this.gamemanager.setPrimaryStage(primaryStage);
        this.animation = new animation();
        this.animation.setPrimaryStage(primaryStage);
        this.authentification = new Authentification(primaryStage);
    }

    public VBox createMainContainer() {
        VBox container = new VBox(25);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40, 60, 40, 60));
        container.setMaxWidth(750);

        Label title = createTitleLabel();
        Label subtitle = createSubtitleLabel();
        VBox actionButtons = createActionButtons();

        container.getChildren().addAll(title, subtitle, actionButtons);
        animateMenuEntrance(container);

        return container;
    }

    private Label createTitleLabel() {
        Label label = new Label("JET FIGHTERS");
        label.setFont(Font.font(gamemanager.FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 74));
        label.setTextFill(gamemanager.COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, gamemanager.COLORS.get("PRIMARY"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);

        label.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animation.animateTextGlow(label, glow);

        return label;
    }

    public Label createSubtitleLabel() {
        Label label = new Label("Only the Fastest Survive the Sky");
        label.setFont(Font.font(gamemanager.FONT_FAMILIES[2], FontWeight.SEMI_BOLD, 35));
        label.setTextFill(gamemanager.COLORS.get("LIGHT"));

        FadeTransition fade = new FadeTransition(Duration.seconds(3), label);
        fade.setFromValue(0.7);
        fade.setToValue(1.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(2), label);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);

        ParallelTransition parallel = new ParallelTransition(fade, scale);
        parallel.play();
        animation.activeAnimations.add(parallel);

        return label;
    }

    public VBox createActionButtons() {
        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setFillWidth(true);

        Button startBtn = animation.createActionButton("START AS A GUEST", "PRIMARY");
        Button signUpBtn = animation.createActionButton("SIGN UP", "ACCENT");
        Button signInBtn = animation.createActionButton("SIGN IN", "SECONDARY");
        Button quitBtn = animation.createActionButton("EXIT", "DANGER");

        startBtn.setPrefSize(300, 60);
        signUpBtn.setPrefSize(300, 60);
        signInBtn.setPrefSize(300, 60);
        quitBtn.setPrefSize(300, 60);

        startBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        quitBtn.setMaxWidth(Double.MAX_VALUE);

        // Corrected implementation for "START AS A GUEST"
        startBtn.setOnAction(e -> {
            System.out.println("Bouton START AS A GUEST cliqué");
            try {
                // Animation de bouton
                animation.playButtonPressAnimation(startBtn);

                // Create and show the player selection interface
                PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);

                // We need to pass the GameManager to the selection interface
                // so that when aircraft is selected, it can properly start the game
                selectionInterface.setGameManager(gamemanager);

                // Show the selection interface - this will let the player choose aircraft and difficulty
                selectionInterface.showSelectionInterface();
            } catch (Exception ex) {
                System.err.println("Erreur lors de l'affichage de l'interface de sélection: " + ex.getMessage());
                ex.printStackTrace();
                showNotification("Erreur de chargement de l'interface");
            }
        });

        signUpBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(signUpBtn);
            transitionToScene(() -> authentification.showSignUpScene());
        });

        signInBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(signInBtn);
            transitionToScene(() -> authentification.showSignInScene());
        });

        quitBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(quitBtn);
            animation.fadeOutAndClose();
        });

        buttonBox.getChildren().addAll(startBtn, signUpBtn, signInBtn, quitBtn);
        return buttonBox;
    }

    public void animateMenuEntrance(VBox container) {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.2), container);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(1), container);
        slide.setFromY(30);
        slide.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(1.2), container);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition parallel = new ParallelTransition(fade, slide, scale);
        parallel.play();
        animation.activeAnimations.add(parallel);
    }

    public void transitionToScene(Runnable sceneSetup) {
        try {
            sceneSetup.run();
        } catch (Exception e) {
            System.err.println("Erreur lors de la transition de scène: " + e.getMessage());
            e.printStackTrace();
            showNotification("Erreur de chargement");
        }
    }

    public void showNotification(String message) {
        try {
            Label notification = new Label(message);
            notification.setStyle("-fx-background-color: rgba(0,0,0,0.8); " +
                    "-fx-text-fill: white; -fx-padding: 12 25; " +
                    "-fx-background-radius: 20; -fx-font-size: 16;");
            notification.setEffect(new DropShadow(10, gamemanager.COLORS.get("PRIMARY")));

            // S'assurer que la scène et le root existent
            if (primaryStage.getScene() != null && primaryStage.getScene().getRoot() instanceof StackPane) {
                StackPane root = (StackPane) primaryStage.getScene().getRoot();
                root.getChildren().add(notification);

                StackPane.setAlignment(notification, Pos.BOTTOM_CENTER);
                StackPane.setMargin(notification, new Insets(0, 0, 40, 0));

                // Animations
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notification);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), notification);
                scaleIn.setFromX(0.8);
                scaleIn.setFromY(0.8);
                scaleIn.setToX(1);
                scaleIn.setToY(1);

                ScaleTransition pulse = new ScaleTransition(Duration.millis(800), notification);
                pulse.setFromX(1);
                pulse.setFromY(1);
                pulse.setToX(1.05);
                pulse.setToY(1.05);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);

                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), notification);
                fadeOut.setDelay(Duration.seconds(2));
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> root.getChildren().remove(notification));

                SequentialTransition sequence = new SequentialTransition(
                        new ParallelTransition(fadeIn, scaleIn),
                        pulse,
                        fadeOut
                );
                sequence.play();
                animation.activeAnimations.add(sequence);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage de la notification: " + e.getMessage());
        }
    }

    public void returnToMenu() {
        gamemanager.stopGame();
        gamemanager.setupMainMenu();
    }
}