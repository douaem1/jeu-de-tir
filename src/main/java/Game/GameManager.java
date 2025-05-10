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
import chat_Client_Serveur.NetworkManager;

public class GameManager {
    public Stage primaryStage;
    private MenuManager menuManager;
    private Authentification auth;
    private NetworkManager networkManager;
    private int gameMode = GameConstants.SINGLE_PLAYER; // Default mode

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
    public ImageView opponentPlayer; // For multiplayer
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
    public Label multiplayerStatusLabel; // NEW: Status for multiplayer

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

    // Game components
    public Socket socket;
    public PrintWriter out;
    public String selectedAircraft;

    public GameManager() {
        // Default constructor
    }

    public GameManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public void setGameMode(int gameMode) {
        this.gameMode = gameMode;
    }

    public void setupMainMenu() {
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

    // Modified startGame method to support multiplayer
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

                // Setup multiplayer if needed
                if (gameMode != GameConstants.SINGLE_PLAYER) {
                    setupMultiplayerComponents();
                }

                // IMPORTANT: Initialiser le HUD avec une référence à ce GameManager
                hud = new HUD();
                hud.setupHUD();

                // Add multiplayer status to HUD if in multiplayer mode
                if (gameMode != GameConstants.SINGLE_PLAYER) {
                    multiplayerStatusLabel = new Label(gameMode == GameConstants.MULTIPLAYER_HOST ?
                            "HOSTING" : "CONNECTED");
                    multiplayerStatusLabel.setTextFill(COLORS.get("ACCENT"));
                    multiplayerStatusLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 18));
                    hud.hudContainer.setRight(multiplayerStatusLabel);
                    BorderPane.setAlignment(multiplayerStatusLabel, Pos.TOP_RIGHT);
                    BorderPane.setMargin(multiplayerStatusLabel, new Insets(15, 20, 0, 0));
                }

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

                // Start network listening thread if in multiplayer mode
                if (gameMode != GameConstants.SINGLE_PLAYER && networkManager != null) {
                    startNetworkThread();
                }

                System.out.println("Game started successfully in mode: " +
                        (gameMode == GameConstants.SINGLE_PLAYER ? "Single Player" :
                                gameMode == GameConstants.MULTIPLAYER_HOST ? "Multiplayer (Host)" : "Multiplayer (Client)"));

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error starting game: " + e.getMessage());

                // Notify the user
                showError("Game Error", "Could not start the game", e.getMessage());
            }
        });
    }

    private void showError(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void setupMultiplayerComponents() {
        // Create opponent player representation
        opponentPlayer = new ImageView();
        opponentPlayer.setImage(new Image(getClass().getResourceAsStream("/opponent_aircraft.png")));
        opponentPlayer.setFitWidth(80);
        opponentPlayer.setFitHeight(80);
        opponentPlayer.setPreserveRatio(true);
        opponentPlayer.setLayoutX(WINDOW_WIDTH - 200);
        opponentPlayer.setLayoutY(WINDOW_HEIGHT / 2);
        opponentPlayer.setVisible(false); // Initially hidden until sync

        gamepane.getChildren().add(opponentPlayer);

        System.out.println("Multiplayer components initialized");
    }

    private void setupControls() {
        gamepane.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT:  case A: movingLeft = true; break;
                case RIGHT: case D: movingRight = true; break;
                case UP:    case W: movingUp = true; break;
                case DOWN:  case S: movingDown = true; break;
                case SPACE: firing = true; break;
                case ESCAPE: pauseGame(); break;
                default: break;
            }
        });

        gamepane.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case LEFT:  case A: movingLeft = false; break;
                case RIGHT: case D: movingRight = false; break;
                case UP:    case W: movingUp = false; break;
                case DOWN:  case S: movingDown = false; break;
                case SPACE: firing = false; break;
                default: break;
            }
        });
    }

    private void setupEnemySpawning() {
        gameExecutor.submit(() -> {
            Random random = new Random();
            try {
                while (gameRunning) {
                    if (enemies.size() < 10) { // Limit the number of enemies
                        Platform.runLater(() -> {
                            try {
                                // Create a new enemy
                                Enemy enemyClass = new Enemy(this);
                                ImageView enemy = enemyClass.createEnemyAirplane();

                                // Add enemy to scene and list
                                gamepane.getChildren().add(enemy);
                                enemies.add(enemy);

                                // Animate the enemy
                                TranslateTransition transition = animateEnemy(enemy);
                                activeAnimations.add(transition);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }

                    // Delay between enemy spawns - random between 1-3 seconds
                    Thread.sleep(1000 + random.nextInt(2000));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    public TranslateTransition animateEnemy(ImageView enemy) {
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
        return null;
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


    private void startGameThreads() {
        // Movement thread
        gameExecutor.submit(() -> {
            try {
                while (gameRunning) {
                    updatePlayerPosition();
                    // Send position to network in multiplayer mode
                    if (gameMode != GameConstants.SINGLE_PLAYER && networkManager != null) {
                        sendPositionUpdate();
                    }
                    Thread.sleep(16); // ~60 FPS
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Firing thread
        gameExecutor.submit(() -> {
            try {
                while (gameRunning) {
                    if (firing) {
                        Platform.runLater(this::fireLaser);
                    }
                    Thread.sleep(200); // Fire rate limiter
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Collision detection thread
        gameExecutor.submit(() -> {
            try {
                while (gameRunning) {
                    Platform.runLater(this::detectCollisions);
                    Thread.sleep(32); // 30 times per second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void startNetworkThread() {
        gameExecutor.submit(() -> {
            try {
                while (gameRunning && networkManager != null && networkManager.isConnected()) {
                    Object data = networkManager.receiveData();
                    if (data != null) {
                        handleNetworkData(data);
                    }
                    Thread.sleep(16); // Check for updates ~60 times per second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void sendPositionUpdate() {
        if (player != null && networkManager != null) {
            PlayerData data = new PlayerData();
            data.x = player.getLayoutX();
            data.y = player.getLayoutY();
            data.isFiring = firing;

            networkManager.sendData(data);
        }
    }

    private void handleNetworkData(Object data) {
        if (data instanceof PlayerData) {
            Platform.runLater(() -> {
                PlayerData playerData = (PlayerData) data;
                if (opponentPlayer != null) {
                    opponentPlayer.setLayoutX(playerData.x);
                    opponentPlayer.setLayoutY(playerData.y);
                    opponentPlayer.setVisible(true);

                    if (playerData.isFiring) {
                        fireOpponentLaser();
                    }
                }
            });
        }
    }

    private void updatePlayerPosition() {
        if (!gameRunning) return;

        Platform.runLater(() -> {
            if (player == null) return;

            double newX = player.getLayoutX();
            double newY = player.getLayoutY();

            if (movingLeft) newX -= PLAYER_SPEED;
            if (movingRight) newX += PLAYER_SPEED;
            if (movingUp) newY -= PLAYER_SPEED;
            if (movingDown) newY += PLAYER_SPEED;

            // Keep player within bounds
            newX = Math.max(0, Math.min(WINDOW_WIDTH - player.getFitWidth(), newX));
            newY = Math.max(0, Math.min(WINDOW_HEIGHT - player.getFitHeight(), newY));

            player.setLayoutX(newX);
            player.setLayoutY(newY);
        });
    }

    private void fireLaser() {
        if (player == null) return;

        // Create laser visual
        Rectangle laser = new Rectangle(5, 20, COLORS.get("DANGER"));
        laser.setArcWidth(5);
        laser.setArcHeight(5);

        // Position at player's position
        laser.setLayoutX(player.getLayoutX() + player.getFitWidth()/2);
        laser.setLayoutY(player.getLayoutY());

        // Add glow effect
        Glow glow = new Glow();
        glow.setLevel(0.8);
        laser.setEffect(glow);

        // Add to scene
        gamepane.getChildren().add(laser);

        // Animate the laser
        TranslateTransition laserAnimation = new TranslateTransition(Duration.seconds(1), laser);
        laserAnimation.setByY(-WINDOW_HEIGHT);
        laserAnimation.setOnFinished(e -> {
            gamepane.getChildren().remove(laser);
        });

        laserAnimation.play();
        activeAnimations.add(laserAnimation);
    }

    private void fireOpponentLaser() {
        if (opponentPlayer == null) return;

        // Create laser visual
        Rectangle laser = new Rectangle(5, 20, COLORS.get("ACCENT"));
        laser.setArcWidth(5);
        laser.setArcHeight(5);

        // Position at opponent's position
        laser.setLayoutX(opponentPlayer.getLayoutX() + opponentPlayer.getFitWidth()/2);
        laser.setLayoutY(opponentPlayer.getLayoutY() + opponentPlayer.getFitHeight());

        // Add glow effect
        Glow glow = new Glow();
        glow.setLevel(0.8);
        laser.setEffect(glow);

        // Add to scene
        gamepane.getChildren().add(laser);

        // Animate the laser
        TranslateTransition laserAnimation = new TranslateTransition(Duration.seconds(1), laser);
        laserAnimation.setByY(WINDOW_HEIGHT);
        laserAnimation.setOnFinished(e -> {
            gamepane.getChildren().remove(laser);
        });

        laserAnimation.play();
        activeAnimations.add(laserAnimation);
    }

    private void detectCollisions() {
        // Implementation of collision detection
        // Would check for intersection between lasers and enemies/players
    }

    public void updateScore(int points) {
        score += points;
        Platform.runLater(() -> {
            if (scoreLabel != null) {
                scoreLabel.setText("SCORE: " + score);
            }
        });
    }

    public void updateLives(int change) {
        lives += change;

        Platform.runLater(() -> {
            if (healthLabel != null) {
                healthLabel.setText("LIVES: " + lives);
            }

            if (healthBar != null) {
                healthBar.setProgress(lives / 3.0);
            }

            if (lives <= 0) {
                gameOver(gamepane);
            }
        });
    }

    public void pauseGame() {
        // Implementation of pause functionality
        gameRunning = false;

        Platform.runLater(() -> {
            // Create pause menu
            VBox pauseMenu = new VBox(10);
            pauseMenu.setAlignment(Pos.CENTER);
            pauseMenu.setBackground(new Background(new BackgroundFill(
                    Color.rgb(0, 0, 0, 0.7), CornerRadii.EMPTY, Insets.EMPTY)));
            pauseMenu.setPrefSize(300, 200);
            pauseMenu.setLayoutX((WINDOW_WIDTH - 300) / 2);
            pauseMenu.setLayoutY((WINDOW_HEIGHT - 200) / 2);

            Label pauseLabel = new Label("GAME PAUSED");
            pauseLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 24));
            pauseLabel.setTextFill(COLORS.get("LIGHT"));

            Button resumeButton = new Button("RESUME");
            resumeButton.setOnAction(e -> resumeGame());

            Button exitButton = new Button("EXIT TO MENU");
            exitButton.setOnAction(e -> exitToMenu());

            pauseMenu.getChildren().addAll(pauseLabel, resumeButton, exitButton);
            gamepane.getChildren().add(pauseMenu);
        });
    }

    public void resumeGame() {
        gameRunning = true;

        Platform.runLater(() -> {
            // Remove pause menu
            gamepane.getChildren().removeIf(node -> node instanceof VBox &&
                    ((VBox) node).getChildren().stream()
                            .anyMatch(child -> child instanceof Label &&
                                    ((Label) child).getText().equals("GAME PAUSED")));
            gamepane.requestFocus();
        });
    }

    public void gameOver(Pane gamepane) {
        gameRunning = false;

        Platform.runLater(() -> {
            // Create game over screen
            VBox gameOverMenu = new VBox(10);
            gameOverMenu.setAlignment(Pos.CENTER);
            gameOverMenu.setBackground(new Background(new BackgroundFill(
                    Color.rgb(0, 0, 0, 0.8), CornerRadii.EMPTY, Insets.EMPTY)));
            gameOverMenu.setPrefSize(400, 300);
            gameOverMenu.setLayoutX((WINDOW_WIDTH - 400) / 2);
            gameOverMenu.setLayoutY((WINDOW_HEIGHT - 300) / 2);

            Label gameOverLabel = new Label("GAME OVER");
            gameOverLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 36));
            gameOverLabel.setTextFill(COLORS.get("DANGER"));

            Label finalScoreLabel = new Label("FINAL SCORE: " + score);
            finalScoreLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 24));
            finalScoreLabel.setTextFill(COLORS.get("LIGHT"));

            Button restartButton = new Button("RESTART");
            restartButton.setOnAction(e -> restartGame());

            Button exitButton = new Button("EXIT TO MENU");
            exitButton.setOnAction(e -> exitToMenu());

            gameOverMenu.getChildren().addAll(gameOverLabel, finalScoreLabel, restartButton, exitButton);
            GameManager.gamepane.getChildren().add(gameOverMenu);
        });
    }

    private void restartGame() {
        // Clean up
        cleanup();

        // Start a new game
        startGame(selectedAircraft);
    }

    private void exitToMenu() {
        // Clean up
        cleanup();

        // Go back to main menu
        setupMainMenu();
    }

    public void cleanup() {
        // Stop all game threads
        gameRunning = false;

        // Stop and clear all animations
        Platform.runLater(() -> {
            activeAnimations.forEach(Animation::stop);
            activeAnimations.clear();
        });

        // Close network connection if in multiplayer mode
        if (gameMode != GameConstants.SINGLE_PLAYER && networkManager != null) {
            networkManager.close();
        }
    }


    // Inner class for network data
    public static class PlayerData implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x;
        public double y;
        public boolean isFiring;
    }
}