package org.example.jeu;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.io.InputStream;

public class JeuDeTir extends Application {
    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private static final String COLOR_PRIMARY = "#2E86AB";
    private static final String COLOR_SECONDARY = "#F18F01";
    private static final String COLOR_ACCENT = "#A23B72";
    private static final String COLOR_DANGER = "#C73E1D";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Jet Fighters - Édition Premium");
        setupMainMenu();
    }

    private void setupMainMenu() {
        StackPane root = new StackPane();
        ImageView background = loadBackgroundImage();
        root.getChildren().add(background);

        animateBackground(background);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.4)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        Pane particleLayer = createParticleEffect();
        root.getChildren().add(particleLayer);

        VBox mainContainer = createMainContainer();
        root.getChildren().add(mainContainer);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private ImageView loadBackgroundImage() {
        try {
            String[] paths = {"/background.jpg", "/background.png", "/img.jpg", "/img.png", "/image.jpg", "/image.png"};
            for (String path : paths) {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image image = new Image(is);
                    ImageView view = new ImageView(image);
                    setupBackgroundImage(view);
                    return view;
                }
            }
            throw new RuntimeException("Aucune image de fond trouvée");
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            return createDefaultBackground();
        }
    }

    private void setupBackgroundImage(ImageView imageView) {
        imageView.setFitWidth(WINDOW_WIDTH);
        imageView.setFitHeight(WINDOW_HEIGHT);
        imageView.setPreserveRatio(false);
        ColorAdjust adjust = new ColorAdjust();
        adjust.setBrightness(-0.1);
        adjust.setContrast(0.1);
        imageView.setEffect(adjust);
    }

    private void animateBackground(ImageView background) {
        ScaleTransition zoom = new ScaleTransition(Duration.seconds(30), background);
        zoom.setFromX(1.0);
        zoom.setFromY(1.0);
        zoom.setToX(1.05);
        zoom.setToY(1.05);
        zoom.setCycleCount(Animation.INDEFINITE);
        zoom.setAutoReverse(true);

        TranslateTransition pan = new TranslateTransition(Duration.seconds(40), background);
        pan.setFromX(0);
        pan.setToX(-20);
        pan.setCycleCount(Animation.INDEFINITE);
        pan.setAutoReverse(true);

        ParallelTransition parallelTransition = new ParallelTransition(zoom, pan);
        parallelTransition.play();
    }

    private ImageView createDefaultBackground() {
        Rectangle rect = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        rect.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(0.5, Color.web("#b21f1f")),
                new Stop(1, Color.web("#fdbb2d"))));
        return new ImageView(rect.snapshot(null, null));
    }

    private Pane createParticleEffect() {
        Pane particlePane = new Pane();
        particlePane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        for (int i = 0; i < 50; i++) {
            Circle particle = new Circle(Math.random() * 2 + 0.5);
            particle.setFill(Color.rgb(255, 255, 255, Math.random() * 0.6 + 0.2));
            particle.setCenterX(Math.random() * WINDOW_WIDTH);
            particle.setCenterY(Math.random() * WINDOW_HEIGHT);

            Glow glow = new Glow(0.8);
            particle.setEffect(glow);

            particlePane.getChildren().add(particle);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(particle.opacityProperty(), Math.random() * 0.5 + 0.2)),
                    new KeyFrame(Duration.seconds(Math.random() * 3 + 1),
                            new KeyValue(particle.opacityProperty(), Math.random() * 0.8 + 0.5))
            );
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setAutoReverse(true);
            timeline.play();
        }

        return particlePane;
    }

    private VBox createMainContainer() {
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
        label.setFont(Font.font("Agency FB", FontWeight.EXTRA_BOLD, 74));
        label.setTextFill(Color.WHITE);

        DropShadow glow = new DropShadow(15, Color.web(COLOR_PRIMARY));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);

        label.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animateTextGlow(label, glow);

        return label;
    }

    private void animateTextGlow(Label label, DropShadow glow) {
        Timeline glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 15)),
                new KeyFrame(Duration.seconds(2), new KeyValue(glow.radiusProperty(), 25)),
                new KeyFrame(Duration.seconds(4), new KeyValue(glow.radiusProperty(), 15))
        );
        glowTimeline.setCycleCount(Animation.INDEFINITE);

        Timeline colorTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.colorProperty(), Color.web(COLOR_PRIMARY))),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(glow.colorProperty(), Color.web(COLOR_SECONDARY))),
                new KeyFrame(Duration.seconds(6),
                        new KeyValue(glow.colorProperty(), Color.web(COLOR_ACCENT))),
                new KeyFrame(Duration.seconds(9),
                        new KeyValue(glow.colorProperty(), Color.web(COLOR_PRIMARY)))
        );
        colorTimeline.setCycleCount(Animation.INDEFINITE);

        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(6), label);
        rotateTransition.setByAngle(2);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setAutoReverse(true);

        new ParallelTransition(glowTimeline, colorTimeline, rotateTransition).play();
    }

    private Label createSubtitleLabel() {
        Label label = new Label("Only the Fastest Survive the Sky");
        label.setFont(Font.font("Bank Gothic", FontWeight.SEMI_BOLD, 26));
        label.setTextFill(Color.web("#CCCCCC"));

        FadeTransition fade = new FadeTransition(Duration.seconds(3), label);
        fade.setFromValue(0.7);
        fade.setToValue(1.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(4), label);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);

        new ParallelTransition(fade, scale).play();
        return label;
    }

    private VBox createActionButtons() {
        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setFillWidth(true);

        Button startBtn = createActionButton("GUEST", COLOR_PRIMARY);
        Button signUpBtn = createActionButton("SIGN UP", COLOR_ACCENT);
        Button signInBtn = createActionButton("SIGN IN", COLOR_SECONDARY);
        Button quitBtn = createActionButton("QUITTER", COLOR_DANGER);

        startBtn.setPrefSize(300, 60);
        signUpBtn.setPrefSize(300, 60);
        signInBtn.setPrefSize(300, 60);
        quitBtn.setPrefSize(300, 60);

        startBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        quitBtn.setMaxWidth(Double.MAX_VALUE);

        startBtn.setOnAction(e -> {
            playButtonPressAnimation(startBtn);
            showNotification("Initialisation du système...");
        });

        signUpBtn.setOnAction(e -> {
            playButtonPressAnimation(signUpBtn);
            transitionToScene(() -> ShowSignUpScene());
        });

        signInBtn.setOnAction(e -> {
            playButtonPressAnimation(signInBtn);
            transitionToScene(() -> showSignInScene());
        });

        quitBtn.setOnAction(e -> {
            playButtonPressAnimation(quitBtn);
            fadeOutAndClose();
        });

        buttonBox.getChildren().addAll(startBtn, signUpBtn, signInBtn, quitBtn);
        return buttonBox;
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 18; " +
                "-fx-padding: 15 0; " +
                "-fx-background-radius: 30; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
        btn.setAlignment(Pos.CENTER);

        btn.setOnMouseEntered(e -> {
            String lightenedColor = lightenColor(color, 0.1);
            btn.setStyle(btn.getStyle().replace(color, lightenedColor) +
                    "-fx-effect: dropshadow(gaussian, " + color + ", 15, 0.5, 0, 0);");

            ScaleTransition pulse = new ScaleTransition(Duration.millis(300), btn);
            pulse.setToX(1.05);
            pulse.setToY(1.05);
            pulse.play();
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(btn.getStyle()
                    .replace(lightenColor(color, 0.1), color)
                    .replace("-fx-effect:.*;", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);"));

            ScaleTransition resetScale = new ScaleTransition(Duration.millis(300), btn);
            resetScale.setToX(1.0);
            resetScale.setToY(1.0);
            resetScale.play();
        });

        return btn;
    }

    private void playButtonPressAnimation(Button button) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(scaleDown, scaleUp);
        sequence.play();
    }

    private void fadeOutAndClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> primaryStage.close());
        fade.play();
    }

    private void transitionToScene(Runnable sceneSetup) {
        StackPane root = (StackPane) primaryStage.getScene().getRoot();

        Rectangle transitionRect = new Rectangle(0, 0, Color.BLACK);
        transitionRect.widthProperty().bind(root.widthProperty());
        transitionRect.heightProperty().bind(root.heightProperty());
        transitionRect.setOpacity(0);
        root.getChildren().add(transitionRect);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), transitionRect);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            sceneSetup.run();
        });

        fadeIn.play();
    }

    private String darkenColor(String hexColor, double factor) {
        Color color = Color.web(hexColor);
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255 * (1 - factor)),
                (int)(color.getGreen() * 255 * (1 - factor)),
                (int)(color.getBlue() * 255 * (1 - factor)));
    }

    private String lightenColor(String hexColor, double factor) {
        Color color = Color.web(hexColor);
        return String.format("#%02x%02x%02x",
                clamp((int)(color.getRed() * 255 * (1 + factor))),
                clamp((int)(color.getGreen() * 255 * (1 + factor))),
                clamp((int)(color.getBlue() * 255 * (1 + factor))));
    }

    private int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }

    private void animateMenuEntrance(VBox container) {
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

        new ParallelTransition(fade, slide, scale).play();
    }

    private void showNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 20; -fx-font-size: 16;");
        notification.setEffect(new DropShadow(10, Color.web(COLOR_PRIMARY)));

        StackPane root = (StackPane) primaryStage.getScene().getRoot();
        root.getChildren().add(notification);

        StackPane.setAlignment(notification, Pos.BOTTOM_CENTER);
        StackPane.setMargin(notification, new Insets(0, 0, 40, 0));

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

        new SequentialTransition(
                new ParallelTransition(fadeIn, scaleIn),
                pulse,
                fadeOut
        ).play();
    }

    public void showSignInScene() {
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));
        loginBox.setMaxWidth(500);
        loginBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");

        // Création du fond spécifique pour Sign In
        StackPane root = new StackPane();
        ImageView background = loadBackgroundImage("/img.jpg");
        setupBackgroundImage(background);
        animateBackground(background);
        root.getChildren().add(background);

        // Overlay semi-transparent
        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.5)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        Label title = new Label("Connexion");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web(COLOR_PRIMARY));

        TextField usernameField = createStylizedTextField("Nom d'utilisateur");
        PasswordField passwordField = createStylizedPasswordField("Mot de passe");

        Button loginBtn = createActionButton("Se connecter", COLOR_PRIMARY);
        loginBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Retour", "gray");
        backBtn.setPrefWidth(200);

        // CORRECTION: Ajout de l'action pour le bouton Retour
        backBtn.setOnAction(e -> {
            playButtonPressAnimation(backBtn);
            transitionToScene(() -> setupMainMenu());
        });

        loginBox.getChildren().addAll(title, usernameField, passwordField, loginBtn, backBtn);

        // Animation d'entrée
        loginBox.setOpacity(0);
        loginBox.setTranslateY(20);
        root.getChildren().add(loginBox);

        Scene loginScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(loginScene);

        animateFormEntrance(loginBox);
    }

    public void ShowSignUpScene() {
        VBox signupBox = new VBox(20);
        signupBox.setAlignment(Pos.CENTER);
        signupBox.setPadding(new Insets(40));
        signupBox.setMaxWidth(500);
        signupBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");

        // Création du fond spécifique pour Sign Up
        StackPane root = new StackPane();
        ImageView background = loadBackgroundImage("/img.jpg");
        setupBackgroundImage(background);
        animateBackground(background);
        root.getChildren().add(background);

        // Overlay semi-transparent
        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.5)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        Label title = new Label("SIGN UP");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web(COLOR_ACCENT));

        TextField usernameField = createStylizedTextField("Enter your Username");
        PasswordField passwordField = createStylizedPasswordField("Enter your Password");
        PasswordField passwordField1 = createStylizedPasswordField("Confirm your Password");

        Button signupBtn = createActionButton("Sign Up", COLOR_ACCENT);
        signupBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Return", "gray");
        backBtn.setPrefWidth(200);

        // CORRECTION: Ajout de l'action pour le bouton Return
        backBtn.setOnAction(e -> {
            playButtonPressAnimation(backBtn);
            transitionToScene(() -> setupMainMenu());
        });

        signupBox.getChildren().addAll(title, usernameField, passwordField, passwordField1, signupBtn, backBtn);

        // Animation d'entrée
        signupBox.setOpacity(0);
        signupBox.setTranslateY(20);
        root.getChildren().add(signupBox);

        Scene signupScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(signupScene);

        animateFormEntrance(signupBox);
    }

    // Nouvelle méthode pour charger des images de fond spécifiques
    private ImageView loadBackgroundImage(String... paths) {
        try {
            for (String path : paths) {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image image = new Image(is);
                    return new ImageView(image);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image de fond: " + e.getMessage());
        }
        return createDefaultBackground();
    }

    // Méthode pour animer l'entrée du formulaire
    private void animateFormEntrance(VBox form) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), form);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(0.6), form);
        slide.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }

    private TextField createStylizedTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                "-fx-prompt-text-fill: lightgray; -fx-padding: 12; -fx-background-radius: 8;");
        field.setPrefWidth(300);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() + "-fx-background-color: rgba(255,255,255,0.25); -fx-effect: dropshadow(gaussian, " + COLOR_PRIMARY + ", 5, 0.5, 0, 0);");
            } else {
                field.setStyle(field.getStyle().replace("-fx-background-color: rgba(255,255,255,0.25);", "-fx-background-color: rgba(255,255,255,0.15);")
                        .replace("-fx-effect: dropshadow(gaussian, " + COLOR_PRIMARY + ", 5, 0.5, 0, 0);", ""));
            }
        });

        return field;
    }

    private PasswordField createStylizedPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                "-fx-prompt-text-fill: lightgray; -fx-padding: 12; -fx-background-radius: 8;");
        field.setPrefWidth(300);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() + "-fx-background-color: rgba(255,255,255,0.25); -fx-effect: dropshadow(gaussian, " + COLOR_PRIMARY + ", 5, 0.5, 0, 0);");
            } else {
                field.setStyle(field.getStyle().replace("-fx-background-color: rgba(255,255,255,0.25);", "-fx-background-color: rgba(255,255,255,0.15);")
                        .replace("-fx-effect: dropshadow(gaussian, " + COLOR_PRIMARY + ", 5, 0.5, 0, 0);", ""));
            }
        });

        return field;
    }

    private StackPane createAnimatedBackground() {
        StackPane background = new StackPane();
        background.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2a6c, #b21f1f);");

        Pane particleLayer = new Pane();
        for (int i = 0; i < 60; i++) {
            Circle particle = new Circle(Math.random() * 2 + 1);
            particle.setFill(Color.rgb(255, 255, 255, Math.random() * 0.5 + 0.2));
            particle.setCenterX(Math.random() * WINDOW_WIDTH);
            particle.setCenterY(Math.random() * WINDOW_HEIGHT);

            particleLayer.getChildren().add(particle);

            TranslateTransition float2 = new TranslateTransition(Duration.seconds(Math.random() * 5 + 3), particle);
            float2.setByX(20 * (Math.random() - 0.5));
            float2.setCycleCount(Animation.INDEFINITE);
            float2.setAutoReverse(true);
            float2.play();
        }

        background.getChildren().add(particleLayer);
        return background;
    }

    public static void main(String[] args) {
        launch(args);
    }
}