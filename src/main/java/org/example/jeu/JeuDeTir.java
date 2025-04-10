package org.example.jeu;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
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
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class JeuDeTir extends Application {

    // Configuration
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final String[] BACKGROUND_PATHS = {"/img.jpg", "/background.jpg", "/backround.jpg"};
    private static final String[] FONT_FAMILIES = {"Agency FB", "Arial", "Bank Gothic"};
    private final Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),  // Ajouté
            "DARK", Color.web("#1A1A2E")
    );

    // Éléments du jeu
    private Stage primaryStage;
    private ImageView player;
    private final CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    private int score = 0;
    private int lives = 3;
    private Pane gamepane;  // Conteneur principal du jeu

    // Dans la section Éléments du jeu

    private BorderPane hudContainer;
    private Label scoreLabel;
    private Label levelLabel;
    private ProgressBar healthBar;
    private Label healthLabel;
    private Label ammoLabel;
    private Label notificationLabel;

    // Variables de contrôle du mouvement
    private volatile boolean movingLeft = false;
    private volatile boolean movingRight = false;
    private volatile boolean movingUp = false;
    private volatile boolean movingDown = false;
    private volatile boolean firing = false;
    private final double PLAYER_SPEED = 5.0;
    private final double LASER_SPEED = 10.0;

    // Threads
    private ExecutorService gameExecutor = Executors.newFixedThreadPool(3);
    private volatile boolean gameRunning = false;

    // Chat
    private Socket socket;
    private PrintWriter out;
    private TextArea chatArea;
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

        // Création de l'UI du chat (masqué par défaut)
        //createChatUI(root);

        // Contenu principal
        VBox mainContainer = createMainContainer();
        root.getChildren().add(mainContainer);

        // Bouton de bascule du chat
        //Button toggleChatBtn = createToggleChatButton();
        //root.getChildren().add(toggleChatBtn);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);

        // Initialisation de la connexion chat APRÈS la création de la scène
        //initializeChat();
        // Gestionnaire d'événements pour le déplacement du chat
        //setupChatDrag();

        primaryStage.show();
    }
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        setupMainMenu();
        primaryStage.setTitle("Jet Fighters ");
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

    // ========================= MENU PRINCIPAL =========================
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

    private void fadeOutAndClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> primaryStage.close());
        fade.play();
        activeAnimations.add(fade);
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
    // ========================= CORE DU JEU =========================
    private void startGame() {
        gamepane = new Pane();
        player = createPlayer();
        gamepane.getChildren().add(player);
        score = 0;
        lives = 3;
        gameRunning = true;

        // Initialisation du HUD
        HUD();
        updateHUD();
        gamepane.getChildren().add(hudContainer);

        setupControls();
        setupEnemySpawning();

        Scene gameScene = new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(gameScene);

        // Donner le focus
        gamepane.requestFocus();

        gameRunning = true;
        startGameThreads();
    }
    private void setupEnemySpawning() {
        Timeline enemySpawner = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    ImageView enemy = createEnemyAirplane();
                    gamepane.getChildren().add(enemy);
                    enemies.add(enemy);
                    animateEnemy(enemy);
                }
                ));
        enemySpawner.setCycleCount(Animation.INDEFINITE);
        enemySpawner.play();
        activeAnimations.add(enemySpawner);
    }

    private void animateEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    enemy.setY(enemy.getY() + 1.5);
                    checkPlayerCollision(enemy); // Vérifie les collisions à chaque frame
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        activeAnimations.add(animation);
    }

    private void checkPlayerCollision(ImageView enemy) {
        if (player != null && enemy.getBoundsInParent().intersects(player.getBoundsInParent())) {
            handlePlayerHit();
            gamepane.getChildren().remove(enemy);
            enemies.remove(enemy);
        }
    }

    private void handlePlayerHit() {
        lives--;
        updateHUD();

        if (lives <= 0) {
            gameOver(gamepane);
        } else {
            // Effet de clignotement
            Timeline blink = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(player.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.seconds(0.1), new KeyValue(player.opacityProperty(), 1.0))
            );
            blink.setCycleCount(6);
            blink.play();
        }
    }

    private void updateHUD() {
        // Version simplifiée du HUD
        if (scoreLabel != null) scoreLabel.setText("SCORE: " + score);

    }

    private void checkLaserCollisions(Pane gamePane, Rectangle laser) {
        enemies.removeIf(enemy -> {
            if (laser.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                Platform.runLater(() -> {
                    gamePane.getChildren().removeAll(laser, enemy);
                    createExplosion(enemy.getX() + enemy.getFitWidth()/2,
                            enemy.getY() + enemy.getFitHeight()/2);

                    // Mettre à jour le score
                    score += 5;
                    scoreLabel.setText("SCORE: " + score); // Mise à jour directe du label
                });
                return true;
            }
            return false;
        });
    }

    private ImageView createEnemyAirplane() {
        try {
            // 1. Essayer de charger l'image depuis les ressources
            InputStream is = getClass().getResourceAsStream("/enemy.png");
            if (is != null) {
                Image enemyImage = new Image(is);
                ImageView enemy = new ImageView(enemyImage);

                // Générer des dimensions aléatoires pour la largeur et la hauteur
                double width = 120; // Largeur entre 80 et 150
                double height = 90;  // Hauteur entre 30 et 60

                // Appliquer les dimensions aléatoires
                enemy.setFitWidth(width);
                enemy.setFitHeight(height);
                enemy.setPreserveRatio(true);

                // Position aléatoire en X, apparition hors écran en Y
                enemy.setX(Math.random() * (WINDOW_WIDTH - width));
                enemy.setY(-height);  // Commence au-dessus de l'écran

                // Effet visuel optionnel
                enemy.setEffect(new DropShadow(10, Color.RED));

                return enemy;
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image ennemi: " + e.getMessage());
        }

        // 2. Fallback graphique si l'image n'est pas trouvée
        Polygon enemyShape = new Polygon(
                0.0, 20.0,
                15.0, 0.0,
                30.0, 20.0,
                25.0, 20.0,
                25.0, 40.0,
                5.0, 40.0,
                5.0, 20.0
        );

        enemyShape.setFill(COLORS.get("DANGER"));  // Rouge
        enemyShape.setStroke(COLORS.get("LIGHT")); // Bordure blanche
        enemyShape.setStrokeWidth(2);

        // Création d'une ImageView à partir du Polygon
        ImageView fallbackEnemy = new ImageView(enemyShape.snapshot(null, null));

        // Générer des dimensions aléatoires pour le fallback
        double fallbackWidth = 80 + Math.random() * 70;  // Largeur entre 80 et 150
        double fallbackHeight = 30 + Math.random() * 30; // Hauteur entre 30 et 60

        // Appliquer les dimensions aléatoires
        fallbackEnemy.setFitWidth(fallbackWidth);
        fallbackEnemy.setFitHeight(fallbackHeight);
        fallbackEnemy.setX(Math.random() * (WINDOW_WIDTH - fallbackWidth));
        fallbackEnemy.setY(-fallbackHeight);

        return fallbackEnemy;
    }


    private void initializeGame() {
        gamepane = new Pane();
        setupGameBackground();

        player=createPlayer();
        gamepane.getChildren().add(player);
        // Vérification de position
        System.out.println("Position joueur - X: " + player.getX() + " Y: " + player.getY());

        setupHUD();
        setupControls();
        primaryStage.setScene(new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT));
        gamepane.getChildren().add(player);
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
    private void fireLaser() {
        Rectangle laser = new Rectangle(5, 20, Color.RED);
        laser.setX(player.getX() + player.getFitWidth()/2 - 2.5);
        laser.setY(player.getY());
        gamepane.getChildren().add(laser);

        // Animation du laser
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    laser.setY(laser.getY() - 10);
                    checkLaserCollisions(gamepane, laser);

                    // Vérifier les collisions
                    enemies.removeIf(enemy -> {
                        if (laser.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                            gamepane.getChildren().removeAll(laser, enemy);
                            return true;
                        }
                        return false;
                    });

                    if (laser.getY() < 0) {
                        gamepane.getChildren().remove(laser);
                        ((Timeline)e.getSource()).stop();
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
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
    private void setupGameBackground() {
        ImageView background = loadBestBackground();
        gamepane.getChildren().add(background);
        animateBackground(background);
    }

    public void HUD() {
        // this.uiFactory = new UIFactory();
        this.hudContainer = new BorderPane();
        initializeHUD();
    }

    private void initializeHUD() {
        // Top HUD (score, level)
        HBox topHUD = new HBox(20);
        topHUD.setPadding(new Insets(10));
        topHUD.setAlignment(Pos.CENTER);

        scoreLabel = createHUDLabel("SCORE: 0");
        levelLabel = createHUDLabel("NIVEAU: 1");

        topHUD.getChildren().addAll(scoreLabel, levelLabel);

        // Bottom HUD (health, ammo)
        HBox bottomHUD = new HBox(20);
        bottomHUD.setPadding(new Insets(10));
        bottomHUD.setAlignment(Pos.CENTER);

        // Barre de santé
        VBox healthBox = new VBox(5);
        healthBox.setAlignment(Pos.CENTER_LEFT);

        Label healthTitle = createHUDLabel("SANTÉ");
        healthTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));

        healthBar = new ProgressBar(1.0);
        healthBar.setPrefWidth(150);
        healthBar.setStyle(
                "-fx-accent: linear-gradient(to right, #ff0000, #00ff00);" +
                        "-fx-control-inner-background: rgba(0, 0, 0, 0.5);" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;"
        );

        healthLabel = createHUDLabel("100%");
        healthLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));

        healthBox.getChildren().addAll(healthTitle, healthBar, healthLabel);

        // Munitions
        VBox ammoBox = new VBox(5);
        ammoBox.setAlignment(Pos.CENTER_RIGHT);

        Label ammoTitle = createHUDLabel("MUNITIONS");
        ammoTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));

        ammoLabel = createHUDLabel("100");
        ammoLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));

        ammoBox.getChildren().addAll(ammoTitle, ammoLabel);

        bottomHUD.getChildren().addAll(healthBox, ammoBox);

        // Notification au centre
        notificationLabel = new Label("");
        notificationLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 24));
        notificationLabel.setTextFill(Color.WHITE);
        notificationLabel.setOpacity(0);
        StackPane centerPane = new StackPane(notificationLabel);

        // Assemblage du HUD
        hudContainer.setTop(topHUD);
        hudContainer.setBottom(bottomHUD);
        hudContainer.setCenter(centerPane);

        // Style global
        hudContainer.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.5),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    private Label createHUDLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        label.setTextFill(Color.WHITE);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(2);
        shadow.setOffsetX(1);
        shadow.setOffsetY(1);
        label.setEffect(shadow);

        return label;
    }

    private void startGameThreads() {
        // Thread de mise à jour du jeu
        gameExecutor.submit(() -> {
            while (gameRunning) {
                Platform.runLater(this::updateGameState);
                try { Thread.sleep(16); }
                catch (InterruptedException e) { break; }
            }
        });

        // Thread d'apparition des ennemis
        gameExecutor.submit(() -> {
            while (gameRunning) {
                Platform.runLater(this::createEnemyAirplane);
                try { Thread.sleep(2000); }
                catch (InterruptedException e) { break; }
            }
        });
    }

    private void updateGameState() {
        updateEnemies();
        //Rectangle laser=null;
        //checkLaserCollisions(gamepane, laser);
        updateHUD();
        try {
            // Code qui pourrait causer un problème
        } catch (Exception e) {
            System.err.println("Erreur détectée: " + e.getMessage());
            e.printStackTrace();
            // Continuer le jeu au lieu de planter
        }
    }

    // ========================= LOGIQUE DU JEU =========================
    private void updateEnemies() {
        enemies.removeIf(enemy -> {
            // Mettre à jour la position de l'ennemi
            enemy.setY(enemy.getY() + 3);

            // Si l'ennemi sort de l'écran
            if (enemy.getY() > WINDOW_HEIGHT) {
                Platform.runLater(() -> gamepane.getChildren().remove(enemy));
                return true; // Supprime l'ennemi de la liste
            }
            return false;
        });
    }


    private void stopAllAnimations() {
        // Arrête toutes les animations et vide la liste
        for (Animation animation : activeAnimations) {
            if (animation != null) {
                animation.stop();
            }
        }
        activeAnimations.clear();  // Nettoie la liste après l'arrêt
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

    // ========================= GRAPHIQUES =========================
    private ImageView createPlayer() {
        try {
            // 1. Chargement de l'image
            Image image = new Image(getClass().getResourceAsStream("/airplane.png"));

            // 2. Création du ImageView
            ImageView player = new ImageView(image);

            // 3. Configuration de la taille
            player.setFitWidth(100);  // Correction de la casse (setFitWidth au lieu de setfitWidth)
            player.setPreserveRatio(true); // Conserve les proportions

            // 4. Positionnement initial
            player.setX(WINDOW_WIDTH / 2 - 100); // Centré horizontalement
            player.setY(WINDOW_HEIGHT - 150);    // 150px du bas

            // 5. Ajout au gamePane (si nécessaire)
            // gamePane.getChildren().add(player); // Décommenter si besoin

            return player; // Retourne l'objet créé

        } catch (Exception e) {
            System.err.println("Erreur chargement joueur: " + e.getMessage());

            // Fallback si l'image ne charge pas
            Rectangle placeholder = new Rectangle(200, 100, Color.BLUE);
            placeholder.setStroke(Color.WHITE);
            return new ImageView(placeholder.snapshot(null, null));
        }
    }


    private void createExplosion(double x, double y) {
        Circle explosion = new Circle(x, y, 0, Color.ORANGERED);
        explosion.setEffect(new Glow(0.8));
        gamepane.getChildren().add(explosion);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(explosion.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(explosion.radiusProperty(), 30)),
                new KeyFrame(Duration.millis(400), new KeyValue(explosion.opacityProperty(), 0))
        );
        timeline.setOnFinished(e -> gamepane.getChildren().remove(explosion));
        timeline.play();
    }

    // ========================= CONTROLES =========================
    private void setupEnhancedControls(Pane gamePane, ImageView player, Pane hud) {
        gamePane.setFocusTraversable(true);

        // Gestion des touches
        gamePane.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT: movingLeft = true; break;
                case RIGHT: movingRight = true; break;
                case UP: movingUp = true; break;
                case DOWN: movingDown = true; break;
                case SPACE: firing = true; break;
            }
        });

        gamePane.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case LEFT: movingLeft = false; break;
                case RIGHT: movingRight = false; break;
                case UP: movingUp = false; break;
                case DOWN: movingDown = false; break;
                case SPACE: firing = false; break;
            }
        });
    }

    private void returnToMenu() {
        stopGame();
        setupMainMenu();
    }


    private void startChatListener() {
        gameExecutor.submit(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    String finalMessage = message;
                    Platform.runLater(() -> chatArea.appendText(finalMessage + "\n"));
                }
            } catch (IOException e) {
                Platform.runLater(() -> chatArea.appendText("Déconnecté du chat\n"));
            }
        });
    }

    // ========================= UTILITAIRES =========================


    private String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    @Override
    public void stop() {
        stopGame();
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erreur fermeture socket: " + e.getMessage());
        }
    }

    private void stopGame() {
        gameRunning = false;
        gameExecutor.shutdownNow();
        try {
            if (!gameExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                gameExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            gameExecutor.shutdownNow();
        }
        Platform.runLater(() -> {
            activeAnimations.forEach(Animation::stop);
            activeAnimations.clear();
            if (gamepane != null) gamepane.getChildren().clear();
        });
    }

    private void setupHUD() {
        hudContainer = new BorderPane();

        // Top HUD
        HBox topHUD = new HBox(20);
        scoreLabel = createHUDLabel("SCORE: 0");
        levelLabel = createHUDLabel("NIVEAU: 1");
        topHUD.getChildren().addAll(scoreLabel, levelLabel);

        // Bottom HUD
        HBox bottomHUD = new HBox(20);
        healthBar = new ProgressBar(1.0);
        healthLabel = createHUDLabel("100%");
        ammoLabel = createHUDLabel("100");
        bottomHUD.getChildren().addAll(healthBar, healthLabel, ammoLabel);

        hudContainer.setTop(topHUD);
        hudContainer.setBottom(bottomHUD);

        gamepane.getChildren().add(hudContainer);
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
        PasswordField confirmPasswordField = createStylizedPasswordField("Confirm your Password");

        Button signupBtn = createActionButton("SIGN UP", "ACCENT");
        signupBtn.setPrefWidth(200);

        Button backBtn = createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        backBtn.setOnAction(e -> {
            playButtonPressAnimation(backBtn);
            transitionToScene(() -> setupMainMenu());
        });

        signupBox.getChildren().addAll(title, usernameField, passwordField, confirmPasswordField, signupBtn, backBtn);
        signupBox.setOpacity(0);
        signupBox.setTranslateY(20);
        root.getChildren().add(signupBox);

        Scene signupScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(signupScene);

        animateFormEntrance(signupBox);
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
    private void animateFormEntrance(VBox form) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), form);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(0.6), form);
        slide.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
        activeAnimations.add(parallel);
    }


    private void setupControls() {
        gamepane.setFocusTraversable(true);
        gamepane.requestFocus();

        gamepane.setOnKeyPressed(e -> {
            if (!gameRunning) return;

            double speed = 8;
            switch (e.getCode()) {
                case LEFT:
                    player.setX(Math.max(0, player.getX() - speed));
                    break;
                case RIGHT:
                    player.setX(Math.min(WINDOW_WIDTH - player.getFitWidth(), player.getX() + speed));
                    break;
                case UP:
                    player.setY(Math.max(0, player.getY() - speed));
                    break;
                case DOWN:
                    player.setY(Math.min(WINDOW_HEIGHT - player.getFitHeight(), player.getY() + speed));
                    break;
                case SPACE:
                    fireEnhancedLaser(gamepane, player, hudContainer); // Utilisez hudContainer ici
                    break;
                case ESCAPE:
                    returnToMenu();
                    break;
            }
        });
    }
    private void fireEnhancedLaser(Pane gamePane, ImageView player, Pane hud) {
        if (!gameRunning) return;

        Rectangle laser = new Rectangle(4, 20, Color.LIMEGREEN);
        laser.setX(player.getX() + player.getFitWidth()/2 - 2);
        laser.setY(player.getY());
        gamePane.getChildren().add(laser);

        AnimationTimer laserAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                laser.setY(laser.getY() - LASER_SPEED);

                // Vérifier les collisions
                checkLaserCollisions(gamePane, laser);

                // Supprimer le laser s'il sort de l'écran
                if (laser.getY() < 0 || !gamePane.getChildren().contains(laser)) {
                    gamePane.getChildren().remove(laser);
                    this.stop();
                }
            }
        };
        laserAnimation.start();
    }
    public static void main(String[] args) {
        launch(args);
    }
}