package Game;

import javafx.animation.*;
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
import design.design;
import design.*;
import Menu.MenuManager;
import Menu.Authentification;

public class GameManager {
    public Stage primaryStage;
    private MenuManager menuManager;
    private Authentification auth;

    // Configuration
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public String[] BACKGROUND_PATHS = {"/img.jpg", "/background.jpg", "/backround.jpg"};
    public String[] FONT_FAMILIES = {"Agency FB", "Arial", "Bank Gothic"};
    public Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),
            "DARK", Color.web("#1A1A2E")
    );

    // Éléments du jeu
    public ImageView player;
    public CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    public int score = 0;
    public int lives = 3;
    public static Pane gamepane;  // Conteneur principal du jeu

    // Éléments du HUD
    public BorderPane hudContainer;
    public Label scoreLabel;
    public Label levelLabel;
    public ProgressBar healthBar;
    public Label healthLabel;
    public Label ammoLabel;
    public Label notificationLabel;

    // Variables de contrôle du mouvement
    public volatile boolean movingLeft = false;
    public volatile boolean movingRight = false;
    public volatile boolean movingUp = false;
    public volatile boolean movingDown = false;
    public volatile boolean firing = false;
    public final double PLAYER_SPEED = 5.0;
    public final double LASER_SPEED = 10.0;

    // Threads
    public ExecutorService gameExecutor = Executors.newFixedThreadPool(3);
    public volatile boolean gameRunning = false;

    // Chat
    public Socket socket;
    public PrintWriter out;
    public TextArea chatArea;
    public String selectedAircraft;

    public GameManager() {
        this.primaryStage = primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public  void setupMainMenu() {
        try {
            // Création d'un nouveau root
            StackPane root = new StackPane();
            design design = new design();
            animation animation = new animation();
            animation.setPrimaryStage(primaryStage);

            // Chargement du fond
            ImageView background = design.loadBestBackground();
            root.getChildren().add(background);
            design.animateBackground(background);

            // Overlay avec effet de dégradé
            Rectangle overlay = design.createOverlay();
            root.getChildren().add(overlay);

            // Effet de particules
            root.getChildren().add(design.createParticleEffect());

            // Contenu principal
            MenuManager menumanager = new MenuManager(primaryStage);
            VBox mainContainer = menumanager.createMainContainer();
            root.getChildren().add(mainContainer);

            // Création et application de la scène
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setScene(scene);

            System.out.println("Menu principal configuré avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration du menu principal: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public HUD hud;

    public void setupHUD() {
        hud = new HUD();
        hud.setupHUD();
        gamepane.getChildren().add(hud.hudContainer);
    }

    // In GameManager.java, modify the startGame method:
// Dans le GameManager, remplacez la méthode startGame() comme ceci:
    public void startGame(String selectedAircraft) {
        this.selectedAircraft = selectedAircraft;

        Platform.runLater(() -> {
            try {
                // Réinitialiser l'état du jeu
                gamepane = new Pane();
                gameRunning = true;
                enemies.clear();
                activeAnimations.forEach(Animation::stop);
                activeAnimations.clear();
                score = 0;
                lives = 3;

                // Setup de base
                ImageView background = new design().loadBestBackground();
                gamepane.getChildren().add(background);

                // Créer le joueur
                Player playerClass = new Player(this);
                player = playerClass.createPlayer();
                gamepane.getChildren().add(player);

                // IMPORTANT: Initialiser le HUD avec une référence à ce GameManager
                hud = new HUD(this);
                hud.setupHUD();

                // S'assurer que la référence au scoreLabel est disponible
                scoreLabel = hud.scoreLabel;

                // Ajouter le HUD à la scène
                gamepane.getChildren().add(hud.hudContainer);

                // Setup des contrôles
                setupControls();

                // Créer la scène
                Scene gameScene = new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT);
                primaryStage.setScene(gameScene);
                gamepane.requestFocus();

                // Démarrer le jeu
                setupEnemySpawning();
                startGameThreads();

                System.out.println("Game started successfully");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Critical error launching game: " + e.getMessage());
                setupMainMenu();
            }
        });
    }
    // Et remplacez la méthode setupControls() comme ceci:
    public void setupControls() {
        gamepane.setFocusTraversable(true);
        gamepane.requestFocus();

        // Création d'une instance de Player unique pour gérer les tirs
        Player playerClass = new Player(this);  // Passer THIS comme référence

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
                    System.out.println("Space key pressed - firing laser");
                    // Use the playerClass instance instead of creating a null one
                    playerClass.fireEnhancedLaser(gamepane, player);
                    break;
                case ESCAPE:
                    stopGame();
                    setupMainMenu();
                    break;
            }
        });
    }

// Also modify the setupEnemySpawning method to add CSS class to enemies:

    private void setupEnemySpawning() {
        Timeline enemySpawner = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    if (!gameRunning) return;

                    Enemy enemy1 = new Enemy();
                    ImageView enemy = enemy1.createEnemyAirplane();
                    if (gamepane != null && enemy != null) {
                        // Add CSS class for identification
                        enemy.getStyleClass().add("enemy-ship");

                        gamepane.getChildren().add(enemy);
                        enemies.add(enemy);
                        animateEnemy(enemy);
                    }
                }
                ));
        enemySpawner.setCycleCount(Animation.INDEFINITE);
        enemySpawner.play();
        activeAnimations.add(enemySpawner);
    }
    public void animateEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(8), e -> {
                    if (!gameRunning) return;

                    enemy.setY(enemy.getY() + 0.5);
                    if (enemy.getY() > WINDOW_HEIGHT) {
                        Platform.runLater(() -> {
                            if (gamepane != null && gamepane.getChildren().contains(enemy)) {
                                gamepane.getChildren().remove(enemy);
                                enemies.remove(enemy);
                            }
                        });
                    } else {
                        checkPlayerCollision(enemy);
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        activeAnimations.add(animation);
    }

    private void checkPlayerCollision(ImageView enemy) {
        if (player != null && enemy.getBoundsInParent().intersects(player.getBoundsInParent())) {
            handlePlayerHit();
            Platform.runLater(() -> {
                if (gamepane != null && gamepane.getChildren().contains(enemy)) {
                    gamepane.getChildren().remove(enemy);
                    enemies.remove(enemy);
                }
            });
        }
    }

    private void handlePlayerHit() {
        lives--;
        HUD hud = new HUD();
        hud.updateHUD();

        if (lives <= 0) {
            gameOver(gamepane);
        } else {
            Timeline blink = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(player.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.seconds(0.1), new KeyValue(player.opacityProperty(), 1.0))
            );
            blink.setCycleCount(6);
            blink.play();
        }
    }

    private void gameOver(Pane gamePane) {
        gameRunning = false;
        design design = new design();
        animation animation = new animation();
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
        Button restartBtn = animation.createActionButton("PLAY AGAIN", "PRIMARY");
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

                startGame(selectedAircraft);
            });
            fadeOut.play();
        });

        // Bouton Main Menu
        Button menuBtn = animation.createActionButton("MAIN MENU", "SECONDARY");
        menuBtn.setPrefWidth(200);
        menuBtn.setOnAction(e -> {
            // Nettoyage complet avant de retourner au menu
            gamePane.getChildren().clear();
            enemies.clear();
            activeAnimations.clear();
            score = 0;
            lives = 3;

            // Transition fluide
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), gameOverBox);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
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

    public void checkLaserCollisions(Pane gamePane, Rectangle laser) {
        enemies.removeIf(enemy -> {
            if (laser.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                Platform.runLater(() -> {
                    gamePane.getChildren().removeAll(laser, enemy);
                    createExplosion(enemy.getX() + enemy.getFitWidth()/2,
                            enemy.getY() + enemy.getFitHeight()/2);

                    // Mettre à jour le score
                    score += 10;
                    if (scoreLabel != null) {
                        scoreLabel.setText("SCORE: " + score);
                    }
                });
                return true;
            }
            return false;
        });
    }

    public void updateGameState() {
        try {
            Enemy enemy = new Enemy();
            enemy.updateEnemies();
            HUD hud = new HUD();
            hud.updateHUD();
        } catch (Exception e) {
            System.err.println("Erreur dans updateGameState: " + e.getMessage());
        }
    }

    private void stopAllAnimations() {
        for (Animation animation : activeAnimations) {
            if (animation != null) {
                animation.stop();
            }
        }
        activeAnimations.clear();
    }

    public void stopGame() {
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

    private void createExplosion(double x, double y) {
        Circle explosion = new Circle(x, y, 0, Color.ORANGERED);
        explosion.setEffect(new Glow(0.8));
        gamepane.getChildren().add(explosion);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(explosion.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(500), new KeyValue(explosion.radiusProperty(), 30)),
                new KeyFrame(Duration.millis(500), new KeyValue(explosion.opacityProperty(), 0))
        );
        timeline.setOnFinished(e -> gamepane.getChildren().remove(explosion));
        timeline.play();
    }

    public void startGameThreads() {
        // S'assurer que l'exécuteur n'est pas déjà fermé
        if (gameExecutor.isShutdown()) {
            gameExecutor = Executors.newFixedThreadPool(3);
        }

        // Thread de mise à jour du jeu
        gameExecutor.submit(() -> {
            while (gameRunning) {
                try {
                    Platform.runLater(this::updateGameState);
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Erreur dans le thread de mise à jour: " + e.getMessage());
                }
            }
        });
    }
}