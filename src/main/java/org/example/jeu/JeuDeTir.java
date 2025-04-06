
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
import javafx.scene.input.KeyCode;
import javafx.scene.Node;

public class JeuDeTir extends Application {
    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private static final String COLOR_PRIMARY = "#2E86AB";
    private static final String COLOR_SECONDARY = "#F18F01";
    private static final String COLOR_ACCENT = "#A23B72";
    private static final String COLOR_DANGER = "#C73E1D";
    private int score = 0;
    private int lives = 3;

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
            String[] paths = {"/img.jpg"};
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
                new Stop(1, Color.web("#fdbb2d")))
        );
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

        new ParallelTransition(glowTimeline, colorTimeline).play();
    }

    private Label createSubtitleLabel() {
        Label label = new Label("Only the Fastest Survive the Sky");
        label.setFont(Font.font("Bank Gothic", FontWeight.SEMI_BOLD, 35));
        label.setTextFill(Color.web("white", 1.0));

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

        new ParallelTransition(fade, scale).play();
        return label;
    }

    private VBox createActionButtons() {
        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setFillWidth(true);

        Button startBtn = createActionButton("START AS A GUEST", COLOR_PRIMARY);
        Button signUpBtn = createActionButton("SIGN UP", COLOR_ACCENT);
        Button signInBtn = createActionButton("SIGN IN", COLOR_SECONDARY);
        Button quitBtn = createActionButton("EXIT", COLOR_DANGER);

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
            transitionToScene(() -> startGame());
        });

        signUpBtn.setOnAction(e -> {
            playButtonPressAnimation(signUpBtn);
            transitionToScene(() -> showSignUpScene());
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

    private void showSignInScene() {
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));
        loginBox.setMaxWidth(500);
        loginBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");

        StackPane root = new StackPane();
        ImageView background = loadBackgroundImage("/img.jpg");
        setupBackgroundImage(background);
        animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.5)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        Label title = new Label("SIGN IN");
        title.setFont(Font.font("Agency FB", FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(Color.WHITE);

        DropShadow glow = new DropShadow(15, Color.web(COLOR_PRIMARY));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animateTextGlow(title, glow);

        TextField usernameField = createStylizedTextField("Username");
        PasswordField passwordField = createStylizedPasswordField("Password");

        Button loginBtn = createActionButton("SIGN IN", COLOR_PRIMARY);
        loginBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Return", "gray");
        backBtn.setPrefWidth(200);

        backBtn.setOnAction(e -> {
            playButtonPressAnimation(backBtn);
            transitionToScene(() -> setupMainMenu());
        });

        loginBox.getChildren().addAll(title, usernameField, passwordField, loginBtn, backBtn);

        loginBox.setOpacity(0);
        loginBox.setTranslateY(20);
        root.getChildren().add(loginBox);

        Scene loginScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(loginScene);

        animateFormEntrance(loginBox);
    }

    private void showSignUpScene() {
        VBox signupBox = new VBox(20);
        signupBox.setAlignment(Pos.CENTER);
        signupBox.setPadding(new Insets(40));
        signupBox.setMaxWidth(500);
        signupBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");

        StackPane root = new StackPane();
        ImageView background = loadBackgroundImage("/img.jpg");
        setupBackgroundImage(background);
        animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.5)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        Label title = new Label("SIGN UP");
        title.setFont(Font.font("Agency FB", FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(Color.WHITE);

        DropShadow glow = new DropShadow(15, Color.web(COLOR_ACCENT));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animateTextGlow(title, glow);

        TextField usernameField = createStylizedTextField("Enter your Username");
        PasswordField passwordField = createStylizedPasswordField("Enter your Password");
        PasswordField passwordField1 = createStylizedPasswordField("Confirm your Password");

        Button signupBtn = createActionButton("SIGN UP", COLOR_ACCENT);
        signupBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Return", "gray");
        backBtn.setPrefWidth(200);

        backBtn.setOnAction(e -> {
            playButtonPressAnimation(backBtn);
            transitionToScene(() -> setupMainMenu());
        });

        signupBox.getChildren().addAll(title, usernameField, passwordField, passwordField1, signupBtn, backBtn);

        signupBox.setOpacity(0);
        signupBox.setTranslateY(20);
        root.getChildren().add(signupBox);

        Scene signupScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(signupScene);

        animateFormEntrance(signupBox);
    }

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
    private void startGame() {
        Pane gamePane = new Pane();
        gamePane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        ImageView gameBackground = loadBackgroundImage("/backround.jpg");
        setupBackgroundImage(gameBackground);
        animateBackground(gameBackground);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(1, Color.rgb(0, 0, 0, 0.3))
        ));

        gamePane.getChildren().addAll(gameBackground, overlay);

        ImageView player = createPlayerAirplane();
        gamePane.getChildren().add(player);

        HUD hud = new HUD();
        gamePane.getChildren().add(hud);
        StackPane.setAlignment(hud, Pos.TOP_LEFT);

        setupPlayerMovement(gamePane, player);
        setupShootingMechanics(gamePane, player, hud);
        setupEnemySpawning(gamePane, hud);

        Scene gameScene = new Scene(gamePane, WINDOW_WIDTH, WINDOW_HEIGHT);
        gameScene.setOnMouseClicked(event -> gamePane.requestFocus());
        primaryStage.setScene(gameScene);
        gamePane.requestFocus();
    }

    private ImageView createPlayerAirplane() {
        try {
            InputStream is = getClass().getResourceAsStream("/airplane.png");
            Image image = new Image(is);
            ImageView airplane = new ImageView(image);
            airplane.setFitWidth(200);
            airplane.setFitHeight(60);
            airplane.setPreserveRatio(true);
            airplane.setX(WINDOW_WIDTH / 2 - 40);
            airplane.setY(WINDOW_HEIGHT - 100);
            return airplane;
        } catch (Exception e) {
            Polygon airplane = new Polygon(0.0, 20.0, 15.0, 0.0, 30.0, 20.0, 25.0, 20.0, 25.0, 40.0, 5.0, 40.0, 5.0, 20.0);
            airplane.setFill(Color.BLUE);
            airplane.setStroke(Color.WHITE);
            airplane.setLayoutX(WINDOW_WIDTH / 2 - 15);
            airplane.setLayoutY(WINDOW_HEIGHT - 100);
            return new ImageView(airplane.snapshot(null, null));
        }
    }

    private void setupPlayerMovement(Pane gamePane, ImageView player) {
        gamePane.setOnKeyPressed(e -> {
            double speed = 8;
            switch (e.getCode()) {
                case LEFT: if (player.getX() > 0) player.setX(player.getX() - speed); break;
                case RIGHT: if (player.getX() < WINDOW_WIDTH - player.getFitWidth()) player.setX(player.getX() + speed); break;
                case UP: if (player.getY() > WINDOW_HEIGHT / 2) player.setY(player.getY() - speed); break;
                case DOWN: if (player.getY() < WINDOW_HEIGHT - player.getFitHeight()) player.setY(player.getY() + speed); break;
            }
        });
    }

    private void setupShootingMechanics(Pane gamePane, ImageView player, HUD hud) {
        gamePane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                Rectangle bullet = new Rectangle(4, 15, Color.YELLOW);
                bullet.setX(player.getX() + player.getFitWidth() / 2 - 2);
                bullet.setY(player.getY());
                gamePane.getChildren().add(bullet);

                Timeline bulletMovement = new Timeline(
                        new KeyFrame(Duration.millis(16), event -> {
                            bullet.setY(bullet.getY() - 10);
                            checkCollisions(gamePane, bullet, hud);
                            if (bullet.getY() < 0) {
                                gamePane.getChildren().remove(bullet);
                                ((Timeline)event.getSource()).stop();
                            }
                        })
                );
                bulletMovement.setCycleCount(Animation.INDEFINITE);
                bulletMovement.play();
            }
        });
    }

    private void setupEnemySpawning(Pane gamePane, HUD hud) {
        Timeline enemySpawner = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    ImageView enemy = createEnemyAirplane();
                    gamePane.getChildren().add(enemy);

                    Timeline enemyMovement = new Timeline(
                            new KeyFrame(Duration.millis(16), e -> {
                                enemy.setY(enemy.getY() + 3);
                                checkPlayerCollision(gamePane, enemy, hud);
                                if (enemy.getY() > WINDOW_HEIGHT) {
                                    gamePane.getChildren().remove(enemy);
                                    ((Timeline)e.getSource()).stop();
                                }
                            })
                    );
                    enemyMovement.setCycleCount(Animation.INDEFINITE);
                    enemyMovement.play();
                })
        );
        enemySpawner.setCycleCount(Animation.INDEFINITE);
        enemySpawner.play();
    }

    private ImageView createEnemyAirplane() {
        try {
            InputStream is = getClass().getResourceAsStream("/enemy_airplane.png");
            Image image = new Image(is);
            ImageView enemy = new ImageView(image);
            enemy.setFitWidth(200);
            enemy.setFitHeight(45);
            enemy.setPreserveRatio(true);
            enemy.setX(Math.random() * (WINDOW_WIDTH - 60));
            enemy.setY(-60);
            return enemy;
        } catch (Exception e) {
            Polygon enemy = new Polygon(0.0, 0.0, 15.0, 20.0, 30.0, 0.0, 25.0, 0.0, 25.0, -20.0, 5.0, -20.0, 5.0, 0.0);
            enemy.setFill(Color.RED);
            enemy.setStroke(Color.WHITE);
            enemy.setLayoutX(Math.random() * (WINDOW_WIDTH - 30));
            enemy.setLayoutY(-30);
            return new ImageView(enemy.snapshot(null, null));
        }
    }

    private void checkCollisions(Pane gamePane, Rectangle bullet, HUD hud) {
        for (Node node : gamePane.getChildren()) {
            if (node instanceof ImageView && node != gamePane.getChildren().get(0)) {
                ImageView enemy = (ImageView) node;
                if (bullet.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                    gamePane.getChildren().removeAll(bullet, enemy);
                    score += 10;
                    ((Label)hud.getChildren().get(0)).setText("Score: " + score);
                    createExplosion(gamePane, enemy.getX(), enemy.getY());
                    return;
                }
            }
        }
    }

    private void checkPlayerCollision(Pane gamePane, ImageView enemy, HUD hud) {
        ImageView player = (ImageView) gamePane.getChildren().get(2); // Index du joueur
        if (enemy.getBoundsInParent().intersects(player.getBoundsInParent())) {
            gamePane.getChildren().remove(enemy);
            lives--;
            ((Label)hud.getChildren().get(1)).setText("Lives: " + lives);
            createExplosion(gamePane, player.getX(), player.getY());
            if (lives <= 0) gameOver(gamePane);
        }
    }

    private void createExplosion(Pane gamePane, double x, double y) {
        Circle explosion = new Circle(0, Color.ORANGE);
        explosion.setCenterX(x + 30);
        explosion.setCenterY(y + 30);
        explosion.setEffect(new Glow(0.8));
        gamePane.getChildren().add(explosion);

        Timeline explosionAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(explosion.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(explosion.radiusProperty(), 30)),
                new KeyFrame(Duration.millis(400), new KeyValue(explosion.radiusProperty(), 50)),
                new KeyFrame(Duration.millis(600), new KeyValue(explosion.opacityProperty(), 0))
        );
        explosionAnim.setOnFinished(e -> gamePane.getChildren().remove(explosion));
        explosionAnim.play();
    }

    private void gameOver(Pane gamePane) {
        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 15;");
        gameOverBox.setPadding(new Insets(40));

        Label title = new Label("GAME OVER");
        title.setFont(Font.font("Agency FB", FontWeight.EXTRA_BOLD, 56));
        title.setTextFill(Color.WHITE);

        Label scoreLabel = new Label("Final Score: " + score);
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        scoreLabel.setTextFill(Color.WHITE);

        Button restartBtn = createActionButton("PLAY AGAIN", COLOR_PRIMARY);
        Button menuBtn = createActionButton("MAIN MENU", COLOR_SECONDARY);

        restartBtn.setOnAction(e -> {
            score = 0;
            lives = 3;
            transitionToScene(() -> startGame());
        });

        menuBtn.setOnAction(e -> {
            score = 0;
            lives = 3;
            transitionToScene(() -> setupMainMenu());
        });

        gameOverBox.getChildren().addAll(title, scoreLabel, restartBtn, menuBtn);
        gameOverBox.setOpacity(0);
        gamePane.getChildren().add(gameOverBox);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), gameOverBox);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    class HUD extends HBox {
        public HUD() {
            setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            setPadding(new Insets(10));
            setSpacing(20);

            Label scoreLabel = new Label("Score: 0");
            scoreLabel.setTextFill(Color.WHITE);
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

            Label livesLabel = new Label("Lives: 3");
            livesLabel.setTextFill(Color.WHITE);
            livesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

            getChildren().addAll(scoreLabel, livesLabel);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
