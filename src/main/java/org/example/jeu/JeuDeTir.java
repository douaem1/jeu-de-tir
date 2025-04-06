package org.example.jeu;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.InputStream;
import javafx.scene.input.KeyCode;
import javafx.scene.Node;
import java.util.*;

public class JeuDeTir extends Application {
    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    // Constantes améliorées
    private static final String[] BACKGROUND_PATHS = {"/img.jpg", "/background.jpg", "/backround.jpg"};
    private static final String[] FONT_FAMILIES = {"Agency FB", "Arial", "Bank Gothic"};
    private ImageView player;
    private List<ImageView> enemies = new ArrayList<>();
    private Pane gamePane;
    private static final String GAME_BACKGROUND_PATH = "/background.jpg";
    // Couleurs avec palette étendue
    private static final Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "ACCENT", Color.web("#A23B72"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "DARK", Color.web("#121212")
    );

    private int score = 0;
    private int lives = 3;
    private boolean gameRunning = true;
    private List<Animation> activeAnimations = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Jet Fighters Premium Deluxe");
        setupMainMenu();
        player = null;
        enemies.clear();
        gamePane = null;

        // Configuration supplémentaire de la fenêtre
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setOnCloseRequest(e -> {
            stopAllAnimations();
            primaryStage.close();
        });
    }

    private void stopAllAnimations() {
        activeAnimations.forEach(Animation::stop);
    }

    private void setupMainMenu() {
        StackPane root = new StackPane();

        // Chargement optimisé du fond
        ImageView background = loadBestBackground();
        root.getChildren().add(background);
        animateBackground(background);

        // Overlay avec effet de dégradé amélioré
        Rectangle overlay = createOverlay();
        root.getChildren().add(overlay);

        // Particules optimisées
        root.getChildren().add(createParticleEffect());

        // Contenu principal
        VBox mainContainer = createMainContainer();
        root.getChildren().add(mainContainer);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private ImageView loadBestBackground() {
        for (String path : BACKGROUND_PATHS) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    Image image = new Image(is);
                    ImageView view = new ImageView(image);
                    setupBackgroundImage(view);
                    return view;
                }
            } catch (Exception e) {
                System.err.println("Erreur de chargement de l'image: " + path);
            }
        }
        return createDefaultBackground();
    }

    private Rectangle createOverlay() {
        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.5)),
                new Stop(1, Color.rgb(0, 0, 0, 0.8))
        ));
        return overlay;
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
        activeAnimations.add(parallelTransition);
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
            activeAnimations.add(timeline);
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
        label.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 74));
        label.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
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
                        new KeyValue(glow.colorProperty(), COLORS.get("PRIMARY"))),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(glow.colorProperty(), COLORS.get("SECONDARY"))),
                new KeyFrame(Duration.seconds(6),
                        new KeyValue(glow.colorProperty(), COLORS.get("ACCENT"))),
                new KeyFrame(Duration.seconds(9),
                        new KeyValue(glow.colorProperty(), COLORS.get("PRIMARY")))
        );
        colorTimeline.setCycleCount(Animation.INDEFINITE);

        ParallelTransition parallel = new ParallelTransition(glowTimeline, colorTimeline);
        parallel.play();
        activeAnimations.add(parallel);
    }

    private Label createSubtitleLabel() {
        Label label = new Label("Only the Fastest Survive the Sky");
        label.setFont(Font.font(FONT_FAMILIES[2], FontWeight.SEMI_BOLD, 35));
        label.setTextFill(COLORS.get("LIGHT"));

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
        activeAnimations.add(parallel);

        return label;
    }

    private VBox createActionButtons() {
        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setFillWidth(true);

        Button startBtn = createActionButton("START AS A GUEST", "PRIMARY");
        Button signUpBtn = createActionButton("SIGN UP", "ACCENT");
        Button signInBtn = createActionButton("SIGN IN", "SECONDARY");
        Button quitBtn = createActionButton("EXIT", "DANGER");

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

    private Button createActionButton(String text, String colorKey) {
        Color color = COLORS.get(colorKey);
        String hexColor = toHex(color);

        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + hexColor + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 18; " +
                "-fx-padding: 15 0; " +
                "-fx-background-radius: 30; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
        btn.setAlignment(Pos.CENTER);

        btn.setOnMouseEntered(e -> {
            String lightenedColor = toHex(color.brighter());
            btn.setStyle(btn.getStyle().replace(hexColor, lightenedColor) +
                    "-fx-effect: dropshadow(gaussian, " + hexColor + ", 15, 0.5, 0, 0);");

            ScaleTransition pulse = new ScaleTransition(Duration.millis(300), btn);
            pulse.setToX(1.05);
            pulse.setToY(1.05);
            pulse.play();
            activeAnimations.add(pulse);
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(btn.getStyle()
                    .replace(toHex(color.brighter()), hexColor)
                    .replace("-fx-effect:.*;", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);"));

            ScaleTransition resetScale = new ScaleTransition(Duration.millis(300), btn);
            resetScale.setToX(1.0);
            resetScale.setToY(1.0);
            resetScale.play();
            activeAnimations.add(resetScale);
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
        activeAnimations.add(sequence);
    }

    private void fadeOutAndClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> primaryStage.close());
        fade.play();
        activeAnimations.add(fade);
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
            root.getChildren().remove(transitionRect);
        });

        fadeIn.play();
        activeAnimations.add(fadeIn);
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
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

        ParallelTransition parallel = new ParallelTransition(fade, slide, scale);
        parallel.play();
        activeAnimations.add(parallel);
    }

    private void showNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: rgba(0,0,0,0.8); " +
                "-fx-text-fill: white; -fx-padding: 12 25; " +
                "-fx-background-radius: 20; -fx-font-size: 16;");
        notification.setEffect(new DropShadow(10, COLORS.get("PRIMARY")));

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

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(fadeIn, scaleIn),
                pulse,
                fadeOut
        );
        sequence.play();
        activeAnimations.add(sequence);
    }

    private void showSignInScene() {
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));
        loginBox.setMaxWidth(500);
        loginBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");

        StackPane root = new StackPane();
        ImageView background = loadBestBackground();
        setupBackgroundImage(background);
        animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = createOverlay();
        root.getChildren().add(overlay);

        Label title = new Label("SIGN IN");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animateTextGlow(title, glow);

        TextField usernameField = createStylizedTextField("Username");
        PasswordField passwordField = createStylizedPasswordField("Password");

        Button loginBtn = createActionButton("SIGN IN", "PRIMARY");
        loginBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Return", "DARK");
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
        ImageView background = loadBestBackground();
        setupBackgroundImage(background);
        animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = createOverlay();
        root.getChildren().add(overlay);

        Label title = new Label("SIGN UP");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("ACCENT"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animateTextGlow(title, glow);

        TextField usernameField = createStylizedTextField("Enter your Username");
        PasswordField passwordField = createStylizedPasswordField("Enter your Password");
        PasswordField passwordField1 = createStylizedPasswordField("Confirm your Password");

        Button signupBtn = createActionButton("SIGN UP", "ACCENT");
        signupBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Return", "DARK");
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

    private void animateFormEntrance(VBox form) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), form);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(0.6), form);
        slide.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
        activeAnimations.add(parallel);
    }

    private TextField createStylizedTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-text-fill: white; -fx-prompt-text-fill: lightgray; " +
                "-fx-padding: 12; -fx-background-radius: 8;");
        field.setPrefWidth(300);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() +
                        "-fx-background-color: rgba(255,255,255,0.25); " +
                        "-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);");
            } else {
                field.setStyle(field.getStyle()
                        .replace("-fx-background-color: rgba(255,255,255,0.25);",
                                "-fx-background-color: rgba(255,255,255,0.15);")
                        .replace("-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);", ""));
            }
        });

        return field;
    }

    private PasswordField createStylizedPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-text-fill: white; -fx-prompt-text-fill: lightgray; " +
                "-fx-padding: 12; -fx-background-radius: 8;");
        field.setPrefWidth(300);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() +
                        "-fx-background-color: rgba(255,255,255,0.25); " +
                        "-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);");
            } else {
                field.setStyle(field.getStyle()
                        .replace("-fx-background-color: rgba(255,255,255,0.25);",
                                "-fx-background-color: rgba(255,255,255,0.15);")
                        .replace("-fx-effect: dropshadow(gaussian, " + toHex(COLORS.get("PRIMARY")) + ", 5, 0.5, 0, 0);", ""));
            }
        });

        return field;
    }

    private void startGame() {
        Pane gamePane = new Pane();
        gamePane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        ImageView gameBackground = loadBestBackground();
        gameBackground.setCache(true);
        gamePane.getChildren().add(gameBackground);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(1, Color.rgb(0, 0, 0, 0.3))
        ));
        gamePane.getChildren().add(overlay);

        player = createPlayerAirplane();
        player.setCache(true);
        gamePane.getChildren().add(player);

        EnhancedHUD hud = new EnhancedHUD();
        gamePane.getChildren().add(hud);

        setupEnhancedControls(gamePane, player, hud);
        setupEnemySpawning(gamePane, hud);

        Scene gameScene = new Scene(gamePane, WINDOW_WIDTH, WINDOW_HEIGHT);
        gameScene.setOnMouseClicked(event -> gamePane.requestFocus());
        primaryStage.setScene(gameScene);
        gamePane.requestFocus();
    }

    private ImageView createPlayerAirplane() {
        try (InputStream is = getClass().getResourceAsStream("/airplane.png")) {
            if (is != null) {
                Image image = new Image(is);
                ImageView airplane = new ImageView(image);
                airplane.setFitWidth(200);
                airplane.setFitHeight(60);
                airplane.setPreserveRatio(true);
                airplane.setX(WINDOW_WIDTH / 2 - 40);
                airplane.setY(WINDOW_HEIGHT - 100);
                return airplane;
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement du vaisseau: " + e.getMessage());
        }

        // Fallback si l'image n'est pas trouvée
        Polygon airplane = new Polygon(0.0, 20.0, 15.0, 0.0, 30.0, 20.0,
                25.0, 20.0, 25.0, 40.0, 5.0, 40.0, 5.0, 20.0);
        airplane.setFill(COLORS.get("PRIMARY"));
        airplane.setStroke(COLORS.get("LIGHT"));
        ImageView fallback = new ImageView(airplane.snapshot(null, null));
        fallback.setX(WINDOW_WIDTH / 2 - 15);
        fallback.setY(WINDOW_HEIGHT - 100);
        return fallback;
    }

    private void setupEnhancedControls(Pane gamePane, ImageView player, EnhancedHUD hud) {
        final double[] speed = {8}; // Variable modifiable

        gamePane.setOnKeyPressed(e -> {
            if (!gameRunning) return;

            switch (e.getCode()) {
                case LEFT:
                    player.setX(Math.max(0, player.getX() - speed[0]));
                    break;
                case RIGHT:
                    player.setX(Math.min(WINDOW_WIDTH - player.getFitWidth(), player.getX() + speed[0]));
                    break;
                case UP:
                    player.setY(Math.max(WINDOW_HEIGHT / 2, player.getY() - speed[0]));
                    break;
                case DOWN:
                    player.setY(Math.min(WINDOW_HEIGHT - player.getFitHeight(), player.getY() + speed[0]));
                    break;
                case SPACE:
                    fireEnhancedLaser(gamePane, player, hud);
                    break;
                case P:
                    togglePause();
                    break;
                case ESCAPE:
                    returnToMenu();
                    break;
            }
        });
    }

    private void fireEnhancedLaser(Pane gamePane, ImageView player, EnhancedHUD hud) {
        Rectangle bullet = new Rectangle(4, 20, Color.LIMEGREEN);
        bullet.setX(player.getX() + player.getFitWidth() / 2 - 2);
        bullet.setY(player.getY());
        bullet.setArcWidth(5);
        bullet.setArcHeight(5);
        bullet.setEffect(new Glow(0.8));
        gamePane.getChildren().add(bullet);

        Timeline bulletMovement = new Timeline(
                new KeyFrame(Duration.millis(16), event -> {
                    bullet.setY(bullet.getY() - 12);

                    checkLaserCollisions(gamePane, bullet, hud);

                    if (bullet.getY() < 0) {
                        gamePane.getChildren().remove(bullet);
                        ((Timeline)event.getSource()).stop();
                    }
                })
        );
        bulletMovement.setCycleCount(Animation.INDEFINITE);
        bulletMovement.play();
        activeAnimations.add(bulletMovement);
    }

    private void checkLaserCollisions(Pane gamePane, Rectangle bullet, EnhancedHUD hud) {
        Iterator<ImageView> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            ImageView enemy = enemyIterator.next();
            if (bullet.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                gamePane.getChildren().removeAll(bullet, enemy);
                enemyIterator.remove();

                score += 10;
                hud.updateScore(score);
                createExplosion(enemy.getX() + enemy.getFitWidth()/2,
                        enemy.getY() + enemy.getFitHeight()/2);

                // Arrêter l'animation du laser
                ((Timeline)bullet.getProperties().get("animation")).stop();
                return;
            }
        }
    }

    private void setupEnemySpawning(Pane gamePane, EnhancedHUD hud) {
        Timeline enemySpawner = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    ImageView enemy = createEnemyAirplane();
                    gamePane.getChildren().add(enemy);
                    enemies.add(enemy);

                    Timeline enemyMovement = new Timeline(
                            new KeyFrame(Duration.millis(16), e -> {
                                enemy.setY(enemy.getY() + 3);

                                if (enemy.getBoundsInParent().intersects(player.getBoundsInParent())) {
                                    handlePlayerHit(gamePane, hud);
                                    gamePane.getChildren().remove(enemy);
                                    enemies.remove(enemy);
                                    ((Timeline)e.getSource()).stop();
                                }

                                if (enemy.getY() > WINDOW_HEIGHT) {
                                    gamePane.getChildren().remove(enemy);
                                    enemies.remove(enemy);
                                    ((Timeline)e.getSource()).stop();
                                }
                            })
                    );
                    enemyMovement.setCycleCount(Animation.INDEFINITE);
                    enemyMovement.play();
                    activeAnimations.add(enemyMovement);
                })
        );
        enemySpawner.setCycleCount(Animation.INDEFINITE);
        enemySpawner.play();
        activeAnimations.add(enemySpawner);
    }

    private ImageView createEnemyAirplane() {
        try (InputStream is = getClass().getResourceAsStream("/enemy_airplane.png")) {
            if (is != null) {
                Image image = new Image(is);
                ImageView enemy = new ImageView(image);
                enemy.setFitWidth(200);
                enemy.setFitHeight(45);
                enemy.setPreserveRatio(true);
                enemy.setX(Math.random() * (WINDOW_WIDTH - 60));
                enemy.setY(-60);
                return enemy;
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'ennemi: " + e.getMessage());
        }

        // Fallback si l'image n'est pas trouvée
        Polygon enemy = new Polygon(0.0, 0.0, 15.0, 20.0, 30.0, 0.0,
                25.0, 0.0, 25.0, -20.0, 5.0, -20.0, 5.0, 0.0);
        enemy.setFill(COLORS.get("DANGER"));
        enemy.setStroke(COLORS.get("LIGHT"));
        ImageView fallback = new ImageView(enemy.snapshot(null, null));
        fallback.setX(Math.random() * (WINDOW_WIDTH - 30));
        fallback.setY(-30);
        return fallback;
    }

    private void handlePlayerHit(Pane gamePane, EnhancedHUD hud) {
        lives--;
        hud.updateLives(lives);

        if (lives <= 0) {
            gameOver(gamePane);
        } else {
            // Effet de clignotement
            Timeline blink = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(player.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.seconds(0.1), new KeyValue(player.opacityProperty(), 1.0))
            );
            blink.setCycleCount(6);
            blink.play();
            activeAnimations.add(blink);

            createExplosion(player.getX() + player.getFitWidth()/2,
                    player.getY() + player.getFitHeight()/2);
        }
    }

    private void createExplosion(double x, double y) {
        Circle explosion = new Circle(0, Color.ORANGE);
        explosion.setCenterX(x);
        explosion.setCenterY(y);
        explosion.setEffect(new Glow(0.9));
        gamePane.getChildren().add(explosion);

        Timeline explosionAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(explosion.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(explosion.radiusProperty(), 30)),
                new KeyFrame(Duration.millis(400), new KeyValue(explosion.radiusProperty(), 50)),
                new KeyFrame(Duration.millis(600), new KeyValue(explosion.opacityProperty(), 0))
        );
        explosionAnim.setOnFinished(e -> gamePane.getChildren().remove(explosion));
        explosionAnim.play();
        activeAnimations.add(explosionAnim);
    }

    private void togglePause() {
        gameRunning = !gameRunning;
        if (gameRunning) {
            activeAnimations.forEach(Animation::play);
            showNotification("Game Resumed");
        } else {
            activeAnimations.forEach(Animation::pause);
            showNotification("Game Paused - Press P to continue");
        }
    }

    private void returnToMenu() {
        stopAllAnimations();
        setupMainMenu();
    }

    private void gameOver(Pane gamePane) {
        gameRunning = false;
        stopAllAnimations();

        // Création du conteneur Game Over
        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 15;");
        gameOverBox.setPadding(new Insets(40));

        // Titre Game Over
        Label title = new Label("GAME OVER");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 56));
        title.setTextFill(COLORS.get("LIGHT"));

        // Affichage du score
        Label scoreLabel = new Label("Final Score: " + score);
        scoreLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 24));
        scoreLabel.setTextFill(COLORS.get("LIGHT"));

        // Bouton Play Again
        Button restartBtn = createActionButton("PLAY AGAIN", "PRIMARY");
        restartBtn.setPrefWidth(200);
        restartBtn.setOnAction(e -> {
            // Nettoyage complet avant de recommencer
            gamePane.getChildren().clear();
            enemies.clear();
            activeAnimations.clear();
            score = 0;
            lives = 3;
            gameRunning = true;

            // Transition fluide
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), gameOverBox);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                startGame();
            });
            fadeOut.play();
        });

        // Bouton Main Menu
        Button menuBtn = createActionButton("MAIN MENU", "SECONDARY");
        menuBtn.setPrefWidth(200);
        menuBtn.setOnAction(e -> {
            // Nettoyage complet avant de retourner au menu
            gamePane.getChildren().clear();
            enemies.clear();
            activeAnimations.clear();
            score = 0;
            lives = 3;

            // Transition fluide
            // Transition fluide
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), gameOverBox);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                setupMainMenu();
            });
            fadeOut.play();

            // Assemblage des éléments
            gameOverBox.getChildren().addAll(title, scoreLabel, restartBtn, menuBtn);
            gameOverBox.setOpacity(0);
            fadeOut.setOnFinished(event -> {
                setupMainMenu();
            });
            fadeOut.play();
        });

        // Assemblage des éléments
        gameOverBox.getChildren().addAll(title, scoreLabel, restartBtn, menuBtn);
        gameOverBox.setOpacity(0);

        // Création de l'overlay
        StackPane overlay = new StackPane(gameOverBox);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        gamePane.getChildren().add(overlay);

        // Animation d'apparition
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), gameOverBox);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.seconds(0.5), gameOverBox);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        ParallelTransition entrance = new ParallelTransition(fadeIn, scaleIn);
        entrance.play();
        activeAnimations.add(entrance);
    }

    class EnhancedHUD extends HBox {
        private Label scoreLabel;
        private Label livesLabel;
        private ProgressBar healthBar;

        public EnhancedHUD() {
            setStyle("-fx-background-color: rgba(0,0,0,0.7);");
            setPadding(new Insets(10));
            setSpacing(20);
            setAlignment(Pos.TOP_LEFT);

            scoreLabel = createHudLabel("Score: 0", COLORS.get("LIGHT"));
            livesLabel = createHudLabel("Lives: 3", COLORS.get("LIGHT"));

            healthBar = new ProgressBar(1.0);
            healthBar.setStyle("-fx-accent: " + toHex(COLORS.get("DANGER")) + ";");
            healthBar.setPrefWidth(200);

            getChildren().addAll(scoreLabel, livesLabel, healthBar);
            StackPane.setAlignment(this, Pos.TOP_LEFT);
        }

        private Label createHudLabel(String text, Color color) {
            Label label = new Label(text);
            label.setTextFill(color);
            label.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 18));
            label.setEffect(new DropShadow(5, Color.BLACK));
            return label;
        }

        public void updateScore(int score) {
            scoreLabel.setText("Score: " + score);
        }

        public void updateLives(int lives) {
            livesLabel.setText("Lives: " + lives);
            healthBar.setProgress(lives / 3.0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}