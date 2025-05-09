package Game;

import Menu.MenuManager;
import chat_Client_Serveur.GameServer;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import design.design;
import design.animation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class MultiplayerManager {
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;

    // Network components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread networkThread;
    private String serverAddress = "localhost";
    private int serverPort = 5555;
    private boolean isHost = false;

    // Styling elements
    public String[] FONT_FAMILIES = {"Agency FB", "Arial", "Bank Gothic"};
    public Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),
            "DARK", Color.web("#1A1A2E")
    );

    // Core components
    private Stage primaryStage;
    private String username;
    private String opponentUsername = "OPPONENT";
    private Pane gamePane;
    private boolean gameRunning = false;

    // Game elements
    private ImageView localPlayer;
    private ImageView remotePlayer;
    private CopyOnWriteArrayList<Rectangle> playerLasers = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Rectangle> opponentLasers = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();

    // Game state
    private int score = 0;
    private int lives = 3;
    private int opponentScore = 0;
    private int opponentLives = 3;

    // HUD elements
    private Label scoreLabel;
    private Label enemyScoreLabel;
    private ProgressBar localHealthBar;
    private ProgressBar remoteHealthBar;
    private Label statusLabel;

    // Movement tracking
    private AtomicLong lastPositionUpdate = new AtomicLong(0);
    private final Set<KeyCode> keysPressed = new HashSet<>();
    private AnimationTimer movementLoop;

    public MultiplayerManager(Stage primaryStage, String username) {
        this.primaryStage = primaryStage;
        this.username = username;
    }

    public void startMultiplayerGame() {
        Platform.runLater(() -> {
            try {
                System.out.println("[STARTING] Setting up multiplayer game...");

                // Setup game scene first
                setupGameScene();

                // Connect to server if not already connected
                if (socket == null && !isHost) {
                    System.out.println("[NETWORK] Connecting to server...");
                    if (!connectToServer()) {
                        System.err.println("[ERROR] Failed to connect to server");
                        return;
                    }
                }

                // Start game loop
                startGameLoop();

                // Start periodic checks
                startPeriodicVisibilityCheck();
                startConnectionHealthCheck();

                System.out.println("[SUCCESS] Multiplayer game started successfully");

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[FAILURE] Failed to start multiplayer game: " + e.getMessage());
                showError("Game Error", "Failed to start multiplayer game: " + e.getMessage());
            }
        });
    }

    private boolean connectToServer() {
        try {
            System.out.println("Connecting to server at " + serverAddress + ":" + serverPort);
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send join message
            out.println("JOIN:" + username);
            System.out.println("Sent JOIN message");

            // Wait for server response
            String response = in.readLine();
            if (response == null || !response.startsWith("WELCOME")) {
                throw new IOException("Invalid server response: " + response);
            }

            System.out.println("Server response: " + response);

            // Start network listener
            startNetworkListener();
            return true;
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            showError("Connection Error", "Could not connect to game server: " + e.getMessage());
            return false;
        }
    }

    private void startNetworkListener() {
        networkThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String msg = message;
                    Platform.runLater(() -> processServerMessage(msg));
                }
            } catch (IOException e) {
                if (gameRunning) {
                    Platform.runLater(() ->
                            showError("Connection Lost", "Lost connection to server: " + e.getMessage()));
                }
            }
        });
        networkThread.setDaemon(true);
        networkThread.start();
    }

    private void processServerMessage(String message) {
        System.out.println("Received: " + message);

        if (message.startsWith("MATCHED:")) {
            opponentUsername = message.substring(8);
            Platform.runLater(() -> {
                statusLabel.setText("PLAYING AGAINST: " + opponentUsername);
                ensureRemotePlayerVisibility();
            });

        } else if (message.startsWith("POS:")) {
            try {
                String[] parts = message.substring(4).split(",");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                lastPositionUpdate.set(System.currentTimeMillis());

                Platform.runLater(() -> {
                    if (remotePlayer != null) {
                        remotePlayer.setX(x);
                        remotePlayer.setY(y);
                        remotePlayer.setVisible(true);

                        // Add visual feedback for movement
                        Circle marker = new Circle(5, Color.YELLOW);
                        marker.setCenterX(x + remotePlayer.getFitWidth()/2);
                        marker.setCenterY(y + remotePlayer.getFitHeight()/2);
                        gamePane.getChildren().add(marker);

                        FadeTransition fade = new FadeTransition(Duration.millis(500), marker);
                        fade.setFromValue(1.0);
                        fade.setToValue(0);
                        fade.setOnFinished(e -> gamePane.getChildren().remove(marker));
                        fade.play();
                    }
                });
            } catch (Exception e) {
                System.err.println("Invalid position format: " + message);
            }

        } else if (message.equals("FIRE")) {
            Platform.runLater(() -> {
                if (remotePlayer != null) {
                    fireLaser(remotePlayer, false);
                }
            });

        } else if (message.startsWith("HIT")) {
            Platform.runLater(() -> {
                createExplosion(localPlayer.getX() + localPlayer.getFitWidth()/2,
                        localPlayer.getY() + localPlayer.getFitHeight()/2);
                handlePlayerHit();
            });

        } else if (message.startsWith("SCORE:")) {
            opponentScore = Integer.parseInt(message.substring(6));
            Platform.runLater(() -> enemyScoreLabel.setText("SCORE: " + opponentScore));

        } else if (message.equals("DISCONNECT")) {
            Platform.runLater(() -> showError("Opponent Left", "Your opponent has disconnected"));

        } else if (message.equals("PONG")) {
            System.out.println("Received PONG from server");
        }
    }

    private void setupGameScene() {
        gamePane = new Pane();
        gameRunning = true;

        // Setup background
        design designUtil = new design();
        ImageView background = designUtil.loadBestBackground();
        designUtil.setupBackgroundImage(background);
        gamePane.getChildren().add(background);

        // Create players
        GameManager gameManager = new GameManager();
        Player playerClass = new Player(gameManager, true);

        // Local player (bottom)
        localPlayer = playerClass.createMultiplayerPlayer(false);
        localPlayer.setX(WINDOW_WIDTH / 2 - localPlayer.getFitWidth() / 2);
        localPlayer.setY(WINDOW_HEIGHT - 150);
        gamePane.getChildren().add(localPlayer);

        // Remote player (top)
        remotePlayer = playerClass.createMultiplayerPlayer(true);
        remotePlayer.setX(WINDOW_WIDTH / 2 - remotePlayer.getFitWidth() / 2);
        remotePlayer.setY(100);
        remotePlayer.setVisible(true);

        // Make opponent ship clearly visible
        DropShadow enemyGlow = new DropShadow(15, COLORS.get("DANGER"));
        enemyGlow.setSpread(0.7);
        remotePlayer.setEffect(enemyGlow);

        gamePane.getChildren().add(remotePlayer);
        remotePlayer.toFront();

        // Setup HUD
        setupMultiplayerHUD();

        // Setup controls
        setupMultiplayerControls();

        // Create scene
        Scene gameScene = new Scene(gamePane, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(gameScene);
        gamePane.requestFocus();
    }

    private void setupMultiplayerHUD() {
        BorderPane hudContainer = new BorderPane();
        hudContainer.setPrefWidth(WINDOW_WIDTH);
        hudContainer.setPadding(new Insets(20));

        // Top HUD (opponent)
        VBox opponentInfo = new VBox(5);
        opponentInfo.setAlignment(Pos.CENTER);

        Label opponentLabel = new Label(opponentUsername);
        opponentLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 16));
        opponentLabel.setTextFill(COLORS.get("LIGHT"));

        enemyScoreLabel = new Label("SCORE: 0");
        enemyScoreLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 20));
        enemyScoreLabel.setTextFill(COLORS.get("LIGHT"));

        remoteHealthBar = new ProgressBar(1.0);
        remoteHealthBar.setPrefWidth(150);
        remoteHealthBar.setStyle("-fx-accent: #C73E1D;");

        opponentInfo.getChildren().addAll(opponentLabel, enemyScoreLabel, remoteHealthBar);

        // Status label
        statusLabel = new Label("CONNECTING...");
        statusLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 14));
        statusLabel.setTextFill(COLORS.get("SECONDARY"));
        statusLabel.setEffect(new Bloom(0.3));

        // Bottom HUD (player)
        VBox playerInfo = new VBox(5);
        playerInfo.setAlignment(Pos.CENTER);

        Label playerLabel = new Label(username);
        playerLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 16));
        playerLabel.setTextFill(COLORS.get("LIGHT"));

        scoreLabel = new Label("SCORE: 0");
        scoreLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 20));
        scoreLabel.setTextFill(COLORS.get("LIGHT"));

        localHealthBar = new ProgressBar(1.0);
        localHealthBar.setPrefWidth(150);
        localHealthBar.setStyle("-fx-accent: #2E86AB;");

        playerInfo.getChildren().addAll(playerLabel, scoreLabel, localHealthBar);

        // Assemble HUD
        hudContainer.setTop(opponentInfo);
        hudContainer.setCenter(statusLabel);
        hudContainer.setBottom(playerInfo);

        gamePane.getChildren().add(hudContainer);
    }

    private void setupMultiplayerControls() {
        gamePane.setFocusTraversable(true);
        gamePane.requestFocus();

        gamePane.setOnKeyPressed(e -> {
            keysPressed.add(e.getCode());

            if (e.getCode() == KeyCode.SPACE) {
                fireLaser(localPlayer, true);
                if (out != null) out.println("FIRE");
            } else if (e.getCode() == KeyCode.ESCAPE) {
                endMultiplayerGame();
            }
        });

        gamePane.setOnKeyReleased(e -> keysPressed.remove(e.getCode()));
        gamePane.setOnMouseClicked(e -> gamePane.requestFocus());

        movementLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                double speed = 8;
                boolean moved = false;

                if (keysPressed.contains(KeyCode.LEFT)) {
                    localPlayer.setX(Math.max(0, localPlayer.getX() - speed));
                    moved = true;
                }
                if (keysPressed.contains(KeyCode.RIGHT)) {
                    localPlayer.setX(Math.min(WINDOW_WIDTH - localPlayer.getFitWidth(), localPlayer.getX() + speed));
                    moved = true;
                }
                if (keysPressed.contains(KeyCode.UP)) {
                    localPlayer.setY(Math.max(WINDOW_HEIGHT / 2, localPlayer.getY() - speed));
                    moved = true;
                }
                if (keysPressed.contains(KeyCode.DOWN)) {
                    localPlayer.setY(Math.min(WINDOW_HEIGHT - localPlayer.getFitHeight(), localPlayer.getY() + speed));
                    moved = true;
                }

                // Send position updates at 30fps when moving or every 500ms when idle
                if ((moved || System.currentTimeMillis() - lastUpdate > 500) && out != null) {
                    lastUpdate = System.currentTimeMillis();
                    out.println("POS:" + localPlayer.getX() + "," + localPlayer.getY());
                }
            }
        };
        movementLoop.start();
    }

    private void startPeriodicVisibilityCheck() {
        Timeline check = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (remotePlayer != null) {
                ensureRemotePlayerVisibility();

                // Check if we haven't received updates in a while
                if (System.currentTimeMillis() - lastPositionUpdate.get() > 3000) {
                    if (out != null) out.println("PING");
                }
            }
        }));
        check.setCycleCount(Animation.INDEFINITE);
        check.play();
        activeAnimations.add(check);
    }

    private void ensureRemotePlayerVisibility() {
        if (remotePlayer != null) {
            remotePlayer.setVisible(true);
            remotePlayer.toFront();

            if (remotePlayer.getEffect() == null) {
                DropShadow enemyGlow = new DropShadow(15, COLORS.get("DANGER"));
                enemyGlow.setSpread(0.7);
                remotePlayer.setEffect(enemyGlow);
            }
        }
    }

    private void startConnectionHealthCheck() {
        Timeline healthCheck = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            if (out != null && gameRunning) {
                out.println("PING");
            }
        }));
        healthCheck.setCycleCount(Animation.INDEFINITE);
        healthCheck.play();
        activeAnimations.add(healthCheck);
    }

    private void startGameLoop() {
        Timeline gameLoop = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            // Main game logic handled by other timelines
        }));
        gameLoop.setCycleCount(Animation.INDEFINITE);
        gameLoop.play();
        activeAnimations.add(gameLoop);
    }

    private void fireLaser(ImageView player, boolean isPlayerLaser) {
        Rectangle laser = new Rectangle();
        laser.setWidth(4);
        laser.setHeight(20);

        if (isPlayerLaser) {
            laser.setX(player.getX() + player.getFitWidth() / 2 - 2);
            laser.setY(player.getY() - 20);
            laser.setFill(Color.CYAN);
            playerLasers.add(laser);
        } else {
            laser.setX(player.getX() + player.getFitWidth() / 2 - 2);
            laser.setY(player.getY() + player.getFitHeight());
            laser.setFill(Color.RED);
            opponentLasers.add(laser);
        }

        laser.setEffect(new Glow(0.8));
        gamePane.getChildren().add(laser);

        Timeline laserTimeline = new Timeline(new KeyFrame(Duration.millis(16), ev -> {
            if (isPlayerLaser) {
                laser.setY(laser.getY() - 10);

                if (laser.getBoundsInParent().intersects(remotePlayer.getBoundsInParent())) {
                    handleOpponentHit();
                    removeLaser(laser, isPlayerLaser);
                    createExplosion(laser.getX(), laser.getY());
                }

                if (laser.getY() < -20) removeLaser(laser, isPlayerLaser);
            } else {
                laser.setY(laser.getY() + 10);

                if (laser.getBoundsInParent().intersects(localPlayer.getBoundsInParent())) {
                    handlePlayerHit();
                    removeLaser(laser, isPlayerLaser);
                    createExplosion(laser.getX(), laser.getY());
                }

                if (laser.getY() > WINDOW_HEIGHT) removeLaser(laser, isPlayerLaser);
            }
        }));

        laserTimeline.setCycleCount(Animation.INDEFINITE);
        laserTimeline.play();
        activeAnimations.add(laserTimeline);

        laser.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                laserTimeline.stop();
                activeAnimations.remove(laserTimeline);
            }
        });
    }

    private void removeLaser(Rectangle laser, boolean isPlayerLaser) {
        gamePane.getChildren().remove(laser);
        if (isPlayerLaser) {
            playerLasers.remove(laser);
        } else {
            opponentLasers.remove(laser);
        }
    }

    private void handlePlayerHit() {
        lives--;
        localHealthBar.setProgress(lives / 3.0);

        if (out != null) out.println("HIT");

        if (lives <= 0) {
            gameOver(false);
            if (out != null) out.println("GAMEOVER");
        } else {
            Timeline blink = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> localPlayer.setOpacity(0.3)),
                    new KeyFrame(Duration.millis(100), e -> localPlayer.setOpacity(1.0)),
                    new KeyFrame(Duration.millis(200), e -> localPlayer.setOpacity(0.3)),
                    new KeyFrame(Duration.millis(300), e -> localPlayer.setOpacity(1.0))
            );
            blink.setCycleCount(3);
            blink.play();
            activeAnimations.add(blink);
        }
    }

    private void handleOpponentHit() {
        score += 10;
        scoreLabel.setText("SCORE: " + score);

        if (out != null) {
            out.println("HIT");
            out.println("SCORE:" + score);
        }

        opponentLives--;
        remoteHealthBar.setProgress(opponentLives / 3.0);

        if (opponentLives <= 0) {
            gameOver(true);
        } else {
            Timeline blink = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> remotePlayer.setOpacity(0.3)),
                    new KeyFrame(Duration.millis(100), e -> remotePlayer.setOpacity(1.0)),
                    new KeyFrame(Duration.millis(200), e -> remotePlayer.setOpacity(0.3)),
                    new KeyFrame(Duration.millis(300), e -> remotePlayer.setOpacity(1.0))
            );
            blink.setCycleCount(3);
            blink.play();
            activeAnimations.add(blink);
        }
    }

    private void createExplosion(double x, double y) {
        Circle explosion = new Circle(x, y, 0, Color.ORANGERED);
        explosion.setEffect(new Glow(0.8));
        gamePane.getChildren().add(explosion);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> explosion.setRadius(0)),
                new KeyFrame(Duration.millis(300), e -> {
                    explosion.setRadius(30);
                    explosion.setOpacity(0);
                })
        );
        timeline.setOnFinished(e -> gamePane.getChildren().remove(explosion));
        timeline.play();
    }

    private void gameOver(boolean victory) {
        gameRunning = false;

        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setPadding(new Insets(40));
        gameOverBox.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 15;");

        Label resultLabel = new Label(victory ? "VICTORY!" : "DEFEAT!");
        resultLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 56));
        resultLabel.setTextFill(victory ? COLORS.get("SECONDARY") : COLORS.get("DANGER"));

        Label scoreLabel = new Label("Your Score: " + score);
        scoreLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 24));
        scoreLabel.setTextFill(COLORS.get("LIGHT"));

        Label enemyScoreLabel = new Label("Opponent Score: " + opponentScore);
        enemyScoreLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 24));
        enemyScoreLabel.setTextFill(COLORS.get("LIGHT"));

        animation animationUtil = new animation();
        Button menuBtn = animationUtil.createActionButton("RETURN TO MENU", "PRIMARY");
        menuBtn.setPrefWidth(250);
        menuBtn.setOnAction(e -> {
            endMultiplayerGame();
            new MenuManager(primaryStage).returnToMenu();
        });

        Button playAgainBtn = animationUtil.createActionButton("PLAY AGAIN", "SECONDARY");
        playAgainBtn.setPrefWidth(250);
        playAgainBtn.setOnAction(e -> {
            endMultiplayerGame();
            startMultiplayerGame();
        });

        gameOverBox.getChildren().addAll(resultLabel, scoreLabel, enemyScoreLabel, playAgainBtn, menuBtn);

        StackPane overlay = new StackPane(gameOverBox);
        overlay.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        gamePane.getChildren().add(overlay);
    }

    private void showError(String title, String message) {
        gameRunning = false;

        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40));
        errorBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.9); -fx-background-radius: 15;");

        Label errorLabel = new Label(title);
        errorLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 42));
        errorLabel.setTextFill(COLORS.get("DANGER"));

        Label detailLabel = new Label(message);
        detailLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.NORMAL, 18));
        detailLabel.setTextFill(COLORS.get("LIGHT"));

        animation animationUtil = new animation();
        Button returnBtn = animationUtil.createActionButton("RETURN TO MENU", "PRIMARY");
        returnBtn.setPrefWidth(250);
        returnBtn.setOnAction(e -> {
            endMultiplayerGame();
            new MenuManager(primaryStage).returnToMenu();
        });

        errorBox.getChildren().addAll(errorLabel, detailLabel, returnBtn);

        StackPane overlay = new StackPane(errorBox);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

        if (gamePane == null) {
            gamePane = new Pane();
            ImageView background = new design().loadBestBackground();
            gamePane.getChildren().add(background);
            primaryStage.setScene(new Scene(gamePane, WINDOW_WIDTH, WINDOW_HEIGHT));
        }

        gamePane.getChildren().add(overlay);
    }

    public void endMultiplayerGame() {
        gameRunning = false;

        // Send disconnect message
        if (out != null) {
            out.println("DISCONNECT");
            try {
                Thread.sleep(100); // Give time for message to send
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // Close network resources
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (networkThread != null) networkThread.interrupt();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        // Stop animations
        for (Animation animation : activeAnimations) {
            if (animation != null) animation.stop();
        }
        activeAnimations.clear();

        // Clean up lasers
        for (Rectangle laser : playerLasers) gamePane.getChildren().remove(laser);
        for (Rectangle laser : opponentLasers) gamePane.getChildren().remove(laser);
        playerLasers.clear();
        opponentLasers.clear();

        // Stop movement loop
        if (movementLoop != null) movementLoop.stop();
    }

    public void startLocalServer() {
        try {
            System.out.println("Starting local game server...");
            new GameServer().start();
            Thread.sleep(500);
            System.out.println("Local server started");
        } catch (Exception e) {
            System.err.println("Error starting local server: " + e.getMessage());
        }
    }

    public void setAsHost(ServerSocket serverSock) {
        try {
            this.serverAddress = "localhost";
            this.isHost = true;
            System.out.println("Running in host mode");
        } catch (Exception e) {
            System.err.println("Error configuring host mode: " + e.getMessage());
        }
    }

    public void setAsClient(Socket clientSock) {
        try {
            this.socket = clientSock;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.isHost = false;
            startNetworkListener();
            System.out.println("Running in client mode");
        } catch (IOException e) {
            System.err.println("Error configuring client mode: " + e.getMessage());
        }
    }
}