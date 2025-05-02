package Game;

import DAO.Scores;
import chat_Client_Serveur.GameState;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
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
import java.util.function.BiConsumer;

import design.design;
import design.*;
import Menu.MenuManager;
import Menu.Authentification;

public class GameManager {
    //public static final double PLAYER_WIDTH = ;
    public Stage primaryStage;
    private MenuManager menuManager;
    private Authentification auth;
    private Canvas gameCanvas;
    private GraphicsContext gc;
    private Group gameRoot;
    private Scene gameScene;
    private StackPane mainLayout;
    public int scoreMultiplier = 1;
    public int currentLevel = 1;
    public double enemySpeed = 0.5; // Vitesse de base
    public double enemySpeedMultiplier = 1.0;
    public int enemiesPerWave = 1;
    private final int[] LEVEL_SCORE_THRESHOLDS = {60, 100, 160, 220, 290, 350};
    private boolean isInvincible = false;
    public String selelectedAircraft;
    // Pour stocker les images des avions
    Map<String, Image> aircraftImages = new HashMap<>();

    // Pour stocker les positions des autres joueurs
    private Map<String, GameState.Position> otherPlayers = new HashMap<>();

    // L'utilisateur courant
    private String currentPlayer;
    private GameState.Position currentPlayerPosition;
    private String currentUsername;
    String selectedAircraft;

    // Configuration
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    public static final int PLAYER_WIDTH = 50;
    public  static final int PLAYER_HEIGHT = 50;
    public static final double PLAYER_SPEED = 5.0;
    public static final double LASER_SPEED = 10.0;
    private long startTime;


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
    public static Pane gamepane;  // Conteneur principal du jeu
    public CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    public int score = 0;
    public int lives = 3;

    // Éléments du HUD
    public HUD hud;
    public Label scoreLabel;
    public Label levelLabel;
    public Label healthLabel;
    public Label ammoLabel;
    public Label notificationLabel;
    public ProgressBar healthBar;
    public BorderPane hudContainer;

    // Variables de contrôle du mouvement
    private Map<String, GameState.Position> playerPositions = new HashMap<>();
    private Map<String, String> playerAircrafts = new HashMap<>();
    private Set<KeyCode> keysPressed = new HashSet<>();
    private BiConsumer<Integer, Integer> positionChangeListener;

    // Thread et état du jeu
    public ExecutorService gameExecutor = Executors.newFixedThreadPool(3);
    public volatile boolean gameRunning = false;

    // Chat
    public Socket socket;
    public PrintWriter out;
    public TextArea chatArea;
    private Map<String, ImageView> playerViews = new HashMap<>(); // Pour stocker les vues des joueurs
    private Map<String, String> playerAircraftTypes = new HashMap<>(); // Pour stocker les types d'avions des joueurs
    private Set<String> connectedPlayers = new HashSet<>();



    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
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

    public void setupHUD() {
        if (hud == null) {
            hud = new HUD();
        }
        hud.setupHUD();

        // S'assurer que le scoreLabel est correctement référencé
        scoreLabel = hud.scoreLabel;

        if (gamepane != null) {
            gamepane.getChildren().add(hud.hudContainer);
        }
    }

    public void startGame(String selectedAircraft) {
        this.selectedAircraft = selectedAircraft;

        Platform.runLater(() -> {
            try {
                // Réinitialiser l'état du jeu
                gamepane = new Pane();
                gameRunning = true;
                enemies.clear();
                playerViews.clear();
                activeAnimations.forEach(Animation::stop);
                activeAnimations.clear();
                score = 0;
                lives = 3;
                startTime=System.currentTimeMillis();

                // Setup de base
                design designHelper = new design();
                ImageView background = designHelper.loadBestBackground();
                gamepane.getChildren().add(background);

                // Créer le joueur principal
                Player playerClass = new Player(this);
                player = playerClass.createPlayer();

                // Position initiale au centre
                double initialX = WINDOW_WIDTH / 2 - PLAYER_WIDTH / 2;
                double initialY = WINDOW_HEIGHT - PLAYER_HEIGHT - 50;
                player.setX(initialX);
                player.setY(initialY);

                gamepane.getChildren().add(player);
                hud = new HUD(this);
                hud.setupHUD();
                scoreLabel = hud.scoreLabel;
                gamepane.getChildren().add(hud.hudContainer);

                // Ajouter le joueur à la liste des vues
                playerViews.put(currentUsername, player);
                playerAircraftTypes.put(currentUsername, selectedAircraft);
                connectedPlayers.add(currentUsername);

                // Setup des contrôles
                setupControls();

                // Créer la scène
                Scene gameScene = new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT);
                primaryStage.setScene(gameScene);
                gamepane.requestFocus();

                // Démarrer le jeu
                setupEnemySpawning();
                startGameThreads();
                currentLevel = 1;  // Réinitialisez le niveau
                hud.updateLevel(currentLevel);

                // Si en mode multijoueur, informer les autres de notre présence
                if (isMultiplayerMode && out != null) {
                    // Envoyer PLAYER_JOIN avec position initiale
                    out.println("PLAYER_JOIN:" + currentUsername + "," + selectedAircraft + "," + initialX + "," + initialY);
                    out.flush();

                    System.out.println("Sent initial player position: " + initialX + "," + initialY);
                }

                System.out.println("Game started successfully");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Critical error launching game: " + e.getMessage());
                setupMainMenu();
            }
        });
    }
    // Méthode à appeler lorsque le joueur se déconnecte
    public void disconnectFromMultiplayerGame() {
        if (isMultiplayerMode && out != null) {
            out.println("PLAYER_LEAVE:" + currentUsername);
            out.flush();
            isMultiplayerMode = false;

            // Nettoyer les listes
            connectedPlayers.clear();
            playerAircraftTypes.clear();

            // Supprimer les vues des joueurs distants
            for (String player : new HashSet<>(playerViews.keySet())) {
                if (!player.equals(currentUsername)) {
                    ImageView view = playerViews.remove(player);
                    if (view != null && gamepane != null) {
                        Platform.runLater(() -> gamepane.getChildren().remove(view));
                    }

                    final String playerId = player;
                    Platform.runLater(() -> {
                        Node nameLabel = gamepane.lookup("#name-" + playerId);
                        if (nameLabel != null) {
                            gamepane.getChildren().remove(nameLabel);
                        }
                    });
                }
            }
        }
    }
    public void setupControls() {
        if (gamepane == null) return;

        gamepane.setFocusTraversable(true);
        gamepane.requestFocus();

        // Création d'une instance de Player unique pour gérer les tirs
        Player playerClass = new Player(this);

        // Gérer les touches enfoncées
        gamepane.setOnKeyPressed(e -> {
            if (!gameRunning) return;

            keysPressed.add(e.getCode());

            // Traiter le tir immédiatement
            if (e.getCode() == KeyCode.SPACE) {
                System.out.println("Space key pressed - firing laser");
                playerClass.fireEnhancedLaser(gamepane, player);

                // Envoyer l'événement de tir aux autres clients
                if (out != null && isMultiplayerMode) {
                    out.println("PLAYER_FIRE:" + currentUsername + "," + player.getX() + "," + player.getY());
                    out.flush();
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stopGame();
                setupMainMenu();
            }
        });

        gamepane.setOnKeyReleased(e -> {
            keysPressed.remove(e.getCode());
        });

        // Animation de mise à jour de position
        AnimationTimer positionUpdater = new AnimationTimer() {
            private long lastUpdateTime = 0;

            @Override
            public void handle(long now) {
                if (!gameRunning) return;

                // Mettre à jour la position selon les touches enfoncées
                double speed = 8;
                boolean positionChanged = false;

                if (keysPressed.contains(KeyCode.LEFT) || keysPressed.contains(KeyCode.A)) {
                    player.setX(Math.max(0, player.getX() - speed));
                    positionChanged = true;
                }
                if (keysPressed.contains(KeyCode.RIGHT) || keysPressed.contains(KeyCode.D)) {
                    player.setX(Math.min(WINDOW_WIDTH - player.getFitWidth(), player.getX() + speed));
                    positionChanged = true;
                }
                if (keysPressed.contains(KeyCode.UP) || keysPressed.contains(KeyCode.W)) {
                    player.setY(Math.max(0, player.getY() - speed));
                    positionChanged = true;
                }
                if (keysPressed.contains(KeyCode.DOWN) || keysPressed.contains(KeyCode.S)) {
                    player.setY(Math.min(WINDOW_HEIGHT - player.getFitHeight(), player.getY() + speed));
                    positionChanged = true;
                }

                // Envoyer la position seulement si changée et pas trop fréquemment
                if (positionChanged && out != null && isMultiplayerMode) {
                    // Limiter l'envoi à 30 fois par seconde
                    if (now - lastUpdateTime > 33_000_000) { // 33ms en nanosecondes
                        out.println("PLAYER_POS:" + currentUsername + "," + player.getX() + "," + player.getY());
                        out.flush();
                        lastUpdateTime = now;
                    }
                }
            }
        };

        positionUpdater.start();

        // Stocker l'animation pour pouvoir l'arrêter plus tard
        //activeAnimations.add(positionUpdater);
    }
    private void setupEnemySpawning() {
        Timeline enemySpawner = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    if (!gameRunning) return;

                    for (int i = 0; i < enemiesPerWave; i++) { // <-- Spawn multiple
                        Enemy enemy1 = new Enemy(this);
                        ImageView enemy = enemy1.createEnemyAirplane();
                        if (gamepane != null && enemy != null) {
                            // Add CSS class for identification
                            enemy.getStyleClass().add("enemy-ship");

                            gamepane.getChildren().add(enemy);
                            enemies.add(enemy);
                            animateEnemy(enemy);
                        }
                    }
                    // Ennemis tireurs à partir du niveau 5
                    if (currentLevel >= 5 && Math.random() < 0.3) { // 30% de chance de spawn
                        ShooterEnemy shooter = new ShooterEnemy(this);
                        ImageView shooterEnemy = shooter.getEnemy();
                        gamepane.getChildren().add(shooterEnemy);
                        enemies.add(shooterEnemy);
                        animateEnemy(shooterEnemy);
                    }
                }
                ));
        enemySpawner.setCycleCount(Animation.INDEFINITE);
        enemySpawner.play();
        activeAnimations.add(enemySpawner);
    }
    // 1. Ajoutez ces variables pour suivre les vies de chaque joueur
    private int player1Lives = 3;
    private int player2Lives = 3;
    private boolean isMultiplayerMode = false;

    // 2. Méthode modifiée pour vérifier les collisions entre les ennemis et les deux joueurs
    private void checkGameEndMultiplayer() {
        // En mode multijoueur, la partie continue tant qu'au moins un joueur est en vie
        if (player1Lives <= 0 && player2Lives <= 0) {
            gameOver(gamepane);
        } else if (player1Lives <= 0) {
            // Joueur 1 est éliminé
            removePlayer("player-player1");
            showNotification("Joueur 1 éliminé! Joueur 2 continue seul.", 2000);
        } else if (player2Lives <= 0) {
            // Joueur 2 est éliminé
            removePlayer("player-player2");
            showNotification("Joueur 2 éliminé! Joueur 1 continue seul.", 2000);
        }
    }

    // 5. Méthode pour supprimer un joueur de l'écran
    private void removePlayer(String playerId) {
        for (Node node : gamepane.getChildren()) {
            if (node instanceof ImageView && node.getId() != null &&
                    node.getId().equals(playerId)) {
                // Animation de disparition
                FadeTransition fade = new FadeTransition(Duration.millis(500), node);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(e -> gamepane.getChildren().remove(node));
                fade.play();
                break;
            }
        }
    }


    // 6. Mise à jour du HUD pour le mode multijoueur
    private void updateMultiplayerHUD() {
        // Mettre à jour l'affichage des vies pour les deux joueurs
        for (Node node : gamepane.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                if (vbox.getChildren().size() > 0 && vbox.getChildren().get(0) instanceof Label) {
                    Label firstLabel = (Label) vbox.getChildren().get(0);
                    if (firstLabel.getText().equals("SCORES")) {
                        // Ajouter ou mettre à jour les indicateurs de vie
                        boolean hasLivesIndicator = false;
                        for (int i = 0; i < vbox.getChildren().size(); i++) {
                            if (vbox.getChildren().get(i) instanceof Label &&
                                    ((Label)vbox.getChildren().get(i)).getText().startsWith("VIES:")) {
                                hasLivesIndicator = true;

                                // Mise à jour des indicateurs de vie
                                Label player1Lives = (Label) vbox.getChildren().get(i);
                                player1Lives.setText("VIES: J1: " + this.player1Lives + " | J2: " + this.player2Lives);

                                break;
                            }
                        }

                        // Si pas encore d'indicateur de vies, en créer un
                        if (!hasLivesIndicator) {
                            Label livesLabel = new Label("VIES: J1: " + player1Lives + " | J2: " + player2Lives);
                            livesLabel.setTextFill(Color.WHITE);
                            livesLabel.setStyle("-fx-font-weight: bold;");
                            vbox.getChildren().add(livesLabel);
                        }

                        break;
                    }
                }
            }
        }
    }



    private void gameOver(Pane gamePane) {
        gameRunning = false;
        //database
        long endTime = System.currentTimeMillis();
        int durationInSeconds = (int) ((endTime - startTime) / 1000);

        Scores scoreRecord = new Scores();
        scoreRecord.setUsername(currentUsername);
        scoreRecord.setScore(score);
        scoreRecord.setRoundDuration(durationInSeconds);
        Scores.addScore(scoreRecord);
        System.out.println("score ajouté ");
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
            stopAllAnimations();
            // Nettoyage complet avant de recommencer
            gamePane.getChildren().clear();
            enemies.clear();
            activeAnimations.clear();
            playerViews.clear(); // Réinitialiser les vues des joueurs
            keysPressed.clear();
            score = 0;
            lives = 3;
            gameRunning = true;
            score = 0;
            lives = 3;
            gameRunning = true;
            isInvincible = false; // Réinitialiser l'état d'invincibilité
            currentLevel = 1; // Réinitialiser le niveau
            enemySpeed = 0.5; // Réinitialiser la vitesse des ennemis
            enemySpeedMultiplier = 1.0;

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
                    createExplosion(enemy.getX() + enemy.getFitWidth() / 2,
                            enemy.getY() + enemy.getFitHeight() / 2);

                    // Mettre à jour le score
                    score += 10 * scoreMultiplier;
                    if (scoreLabel != null) {
                        scoreLabel.setText("SCORE: " + score);
                    }

                    // Vérifier si on change de niveau
                    checkLevelUp();
                });
                return true;
            }
            return false;
        });
    }

    public void updateGameState() {
        try {
            Enemy enemy = new Enemy(this);
            enemy.updateEnemies();

            if (hud != null) {
                hud.updateHUD();
            }
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
                new KeyFrame(Duration.millis(200), new KeyValue(explosion.radiusProperty(), 30)),
                new KeyFrame(Duration.millis(400), new KeyValue(explosion.opacityProperty(), 0))
        );
        timeline.setOnFinished(e -> gamepane.getChildren().remove(explosion));
        timeline.play();
        if (new Random().nextDouble() < 0.3) {
            spawnPowerUp(x, y);
        }
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

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }




    // Trouver ou créer un sprite d'avion pour un joueur
    // Mettre à jour le tableau des scores

    // Méthode pour définir un écouteur d'événements de tir
    private BiConsumer<Void, Void> fireEventListener;

    public void setFireEventListener(Runnable listener) {
        this.fireEventListener = (v1, v2) -> listener.run();
    }

    // Méthode utilitaire pour récupérer tous les avions ennemis
    public List<ImageView> getEnemyShips() {
        List<ImageView> enemyShips = new ArrayList<>();
        for (Node node : gamepane.getChildren()) {
            if (node instanceof ImageView &&
                    node.getStyleClass().contains("enemy-ship")) {
                enemyShips.add((ImageView)node);
            }
        }
        return enemyShips;
    }




    private void setupGameLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        }.start();
    }

    public void update() {
        if (currentUsername != null && playerPositions.containsKey(currentUsername)) {
            GameState.Position pos = playerPositions.get(currentUsername);
            int moveSpeed = 5;
            boolean moved = false;

            // Mettre à jour la position du joueur selon les touches pressées
            if (keysPressed.contains(KeyCode.UP) || keysPressed.contains(KeyCode.W)) {
                pos.y -= moveSpeed;
                moved = true;
            }
            if (keysPressed.contains(KeyCode.DOWN) || keysPressed.contains(KeyCode.S)) {
                pos.y += moveSpeed;
                moved = true;
            }
            if (keysPressed.contains(KeyCode.LEFT) || keysPressed.contains(KeyCode.A)) {
                pos.x -= moveSpeed;
                moved = true;
            }
            if (keysPressed.contains(KeyCode.RIGHT) || keysPressed.contains(KeyCode.D)) {
                pos.x += moveSpeed;
                moved = true;
            }

            // Garder le joueur dans les limites du canevas
            pos.x = Math.max(0, Math.min(GAME_WIDTH - PLAYER_WIDTH, pos.x));
            pos.y = Math.max(0, Math.min(GAME_HEIGHT - PLAYER_HEIGHT, pos.y));

            // Si le joueur a bougé, notifier le serveur
            if (moved && positionChangeListener != null) {
                positionChangeListener.accept(pos.x, pos.y);
            }
        }
    }

    private void showNotification(String message, int duration) {
        if (gamepane == null) return;

        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5px;");
        notification.setTranslateY(50);

        // Centrer la notification
        StackPane notificationContainer = new StackPane(notification);
        notificationContainer.setAlignment(Pos.TOP_CENTER);
        notificationContainer.setLayoutX(WINDOW_WIDTH / 2 - 150);
        notificationContainer.prefWidth(300);

        // Ajouter à la scène
        gamepane.getChildren().add(notificationContainer);

        // Animation de fondu
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notificationContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Temporisation pour la disparition
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(duration), e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), notificationContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> gamepane.getChildren().remove(notificationContainer));
            fadeOut.play();
        }));
        timeline.play();
        activeAnimations.add(timeline);
    }

    /**
     * Met à jour les données des joueurs reçues du serveur
     */

    // Variables de classe importantes
// État d'invincibilité
    private boolean isProcessingHit = false; // Nouvel indicateur pour éviter le traitement multiple
    private Timeline invincibilityTimeline; // Référence à l'animation d'invincibilité
    private long lastHitTime = 0; // Timestamp de la dernière collision

    // Variables de classe simplifiées
    // Timestamp de la dernière collision
    private static final long INVINCIBILITY_DURATION = 3000; // 3 secondes d'invincibilité

    /**
     * Méthode de vérification des collisions simplifiée et efficace
     */
    // Variables de classe

     // 3 secondes
    private static final long MIN_TIME_BETWEEN_HITS = 4000; // 4 secondes minimum entre les hits
    private Set<Integer> hitEnemies = new HashSet<>(); // Pour suivre les ennemis qui ont déjà causé un hit

    /**
     * Méthode de détection de collision avec protection contre les déclenchements multiples
     */
    public void checkPlayerCollision(ImageView enemy) {
        // Vérifications de base
        if (player == null || !gameRunning || lives <= 0 || enemy == null) {
            return;
        }

        // Obtenir un ID unique pour cet ennemi
        int enemyId = System.identityHashCode(enemy);

        // Si cet ennemi a déjà causé un hit, l'ignorer complètement
        if (hitEnemies.contains(enemyId)) {
            return;
        }

        // Vérifier le temps écoulé depuis le dernier hit (protection forte)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHitTime < MIN_TIME_BETWEEN_HITS) {
            // Trop tôt pour un autre hit, ignorer cette collision
            return;
        }

        // Si le joueur est invincible, ignorer la collision
        if (isInvincible) {
            return;
        }

        // Vérifier la collision avec une zone réduite
        Bounds playerBounds = player.getBoundsInParent();
        Bounds enemyBounds = enemy.getBoundsInParent();

        double playerShrinkX = playerBounds.getWidth() * 0.25;
        double playerShrinkY = playerBounds.getHeight() * 0.25;
        double enemyShrinkX = enemyBounds.getWidth() * 0.25;
        double enemyShrinkY = enemyBounds.getHeight() * 0.25;

        Bounds adjustedPlayerBounds = new BoundingBox(
                playerBounds.getMinX() + playerShrinkX,
                playerBounds.getMinY() + playerShrinkY,
                playerBounds.getWidth() - (playerShrinkX * 2),
                playerBounds.getHeight() - (playerShrinkY * 2)
        );

        Bounds adjustedEnemyBounds = new BoundingBox(
                enemyBounds.getMinX() + enemyShrinkX,
                enemyBounds.getMinY() + enemyShrinkY,
                enemyBounds.getWidth() - (enemyShrinkX * 2),
                enemyBounds.getHeight() - (enemyShrinkY * 2)
        );

        // Si collision détectée
        if (adjustedPlayerBounds.intersects(adjustedEnemyBounds)) {
            // Triple protection contre les déclenchements multiples
            synchronized (this) {
                // Revérifier toutes les conditions à l'intérieur du bloc synchronisé
                if (isInvincible || currentTime - lastHitTime < MIN_TIME_BETWEEN_HITS || hitEnemies.contains(enemyId)) {
                    return;
                }

                // Marquer cet ennemi comme ayant causé un hit
                hitEnemies.add(enemyId);

                // Activer l'invincibilité et enregistrer le timestamp
                isInvincible = true;
                lastHitTime = currentTime;

                System.out.println("COLLISION LÉGITIME à " + currentTime + " avec ennemi " + enemyId);

                // Gérer la réduction de vie dans le thread UI pour éviter les problèmes de concurrence
                Platform.runLater(() -> {
                    // Réduire les vies en toute sécurité
                    handlePlayerHitSafely();

                    // Supprimer l'ennemi si encore présent
                    if (gamepane.getChildren().contains(enemy)) {
                        gamepane.getChildren().remove(enemy);
                        if (enemies.contains(enemy)) {
                            enemies.remove(enemy);
                        }
                        createExplosion(enemy.getX() + enemy.getFitWidth()/2,
                                enemy.getY() + enemy.getFitHeight()/2);
                    }
                });
            }
        }
    }

    /**
     * Méthode sécurisée pour gérer la perte de vie du joueur
     */
    private void handlePlayerHitSafely() {
        // Protection supplémentaire pour ne pas réduire les vies plusieurs fois
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHitTime < MIN_TIME_BETWEEN_HITS - 1000) {
            System.out.println("PROTECTION: Tentative de réduire les vies trop rapidement ignorée");
            return;
        }

        // Réduire les vies
        lives = Math.max(0, lives - 1);

        // Mettre à jour le HUD
        hud.updateLives(lives);
        System.out.println("VIE PERDUE: Vies restantes = " + lives);

        // Animation d'invincibilité
        Timeline blinkTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> player.setOpacity(0.3)),
                new KeyFrame(Duration.millis(150), e -> player.setOpacity(1.0))
        );
        blinkTimeline.setCycleCount((int)(INVINCIBILITY_DURATION / 300));
        blinkTimeline.setAutoReverse(true);

        // Programmer la fin de l'invincibilité
        PauseTransition invincibilityEnd = new PauseTransition(Duration.millis(INVINCIBILITY_DURATION));
        invincibilityEnd.setOnFinished(e -> {
            isInvincible = false;
            player.setOpacity(1.0);
            System.out.println("FIN INVINCIBILITÉ à " + System.currentTimeMillis());
        });

        // Jouer les animations en séquence
        SequentialTransition sequence = new SequentialTransition(blinkTimeline, invincibilityEnd);
        sequence.play();

        // Vérifier Game Over
        if (lives <= 0) {
            gameRunning = false;
            gameOver(gamepane);
        }
    }

    /**
     * Méthode pour nettoyer les ennemis traités périodiquement
     * À appeler dans gameLoop ou avec un Timer
     */
    public void cleanupHitEnemies() {
        // Nettoyer la liste des ennemis qui ont causé un hit il y a longtemps
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHitTime > 10000) { // 10 secondes
            hitEnemies.clear();
        }
    }

    /**
     * Ajouter cet appel dans votre gameLoop
     */

    /**
     * Méthode pour appliquer l'effet visuel d'invincibilité
     */
    private void applyInvincibilityEffect() {
        // Arrêter toute animation en cours
        Timeline blinkTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> player.setOpacity(0.3)),
                new KeyFrame(Duration.millis(150), e -> player.setOpacity(1.0))
        );
        blinkTimeline.setCycleCount((int)(INVINCIBILITY_DURATION / 300));
        blinkTimeline.setAutoReverse(true);
        blinkTimeline.play();
    }
    public void handlePlayerHit() {
        // Ne pas réinitialiser les états ici - seulement retirer une vie
        lives = Math.max(0, lives - 1);
        hud.updateLives(lives);
        System.out.println("Joueur touché! Vies restantes: " + lives + " à " + System.currentTimeMillis());

        // Arrêter toute animation d'invincibilité existante
        if (invincibilityTimeline != null) {
            invincibilityTimeline.stop();
        }

        // Créer une nouvelle animation avec clignotement
        invincibilityTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(player.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(900), new KeyValue(player.opacityProperty(), 1.0))
        );
        invincibilityTimeline.setCycleCount(15); // ~3 secondes au total
        invincibilityTimeline.setAutoReverse(true);

        // Éviter de créer plusieurs gestionnaires d'événements
        invincibilityTimeline.setOnFinished(null);
        invincibilityTimeline.setOnFinished(e -> {
            // Ajouter un délai supplémentaire avant de désactiver l'invincibilité
            PauseTransition invincibilityEndDelay = new PauseTransition(Duration.millis(500));
            invincibilityEndDelay.setOnFinished(event -> {
                // S'assurer que cette méthode n'est exécutée qu'une seule fois
                synchronized (this) {
                    isInvincible = false;
                    isProcessingHit = false; // S'assurer que le traitement du hit est terminé
                    player.setOpacity(1.0);
                    System.out.println("Période d'invincibilité terminée à " + System.currentTimeMillis());
                }
            });
            invincibilityEndDelay.play();
        });

        // Démarrer l'animation
        invincibilityTimeline.play();

        // Vérifier Game Over
        if (lives <= 0) {
            gameRunning = false;
            gameOver(gamepane);
        }
    }

    // Ajouter cette méthode pour vérifier si des collisions fantômes sont détectées

    public void animateEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(8), e -> {
                    if (!gameRunning ) return;

                    enemy.setY(enemy.getY() + (enemySpeed * enemySpeedMultiplier));
                    if (enemy.getY() > WINDOW_HEIGHT) {
                        //  Platform.runLater(() -> {
                        //     if (gamepane != null && gamepane.getChildren().contains(enemy)) {
                        //         gamepane.getChildren().remove(enemy);
                        enemies.remove(enemy);
                        //      }
                        //  });
                    } else {
                        checkPlayerCollision(enemy);
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        activeAnimations.add(animation);
    }
    /**
     * Vérifie les collisions avec tous les joueurs sur l'écran
     */
    private void checkAllPlayersCollision(ImageView enemy) {
        for (Node node : gamepane.getChildren()) {
            if (node instanceof ImageView && node.getId() != null &&
                    node.getId().startsWith("player-")) {
                ImageView playerView = (ImageView) node;

                if (enemy.getBoundsInParent().intersects(playerView.getBoundsInParent())) {
                    // Si c'est notre joueur qui est touché
                    if (playerView.getId().equals("player-" + currentUsername)) {
                        handlePlayerHit();
                    }

                    // Dans tous les cas, supprimer l'ennemi
                    Platform.runLater(() -> {
                        if (gamepane != null && gamepane.getChildren().contains(enemy)) {
                            gamepane.getChildren().remove(enemy);
                            enemies.remove(enemy);
                            createExplosion(enemy.getX() + enemy.getFitWidth()/2,
                                    enemy.getY() + enemy.getFitHeight()/2);
                        }
                    });

                    break;
                }
            }
        }
    }
    public void handleServerEnemyCommand(String command) {
        // Parse the command from server that contains enemy data
        // Format example: "SPAWN_ENEMY:id=123,x=150,y=0,type=basic"
        if (command.startsWith("SPAWN_ENEMY:")) {
            String[] parts = command.substring("SPAWN_ENEMY:".length()).split(",");
            String enemyId = "";
            double x = 0;
            double y = 0;
            String type = "basic";

            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    switch (keyValue[0]) {
                        case "id": enemyId = keyValue[1]; break;
                        case "x": x = Double.parseDouble(keyValue[1]); break;
                        case "y": y = Double.parseDouble(keyValue[1]); break;
                        case "type": type = keyValue[1]; break;
                    }
                }
            }

            // Create the enemy with the specified ID and position
            addNetworkEnemy(enemyId, x, y, type);
        }
        // Handle enemy movement updates
        else if (command.startsWith("MOVE_ENEMY:")) {
            String[] parts = command.substring("MOVE_ENEMY:".length()).split(",");
            String enemyId = "";
            double x = 0;
            double y = 0;

            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    switch (keyValue[0]) {
                        case "id": enemyId = keyValue[1]; break;
                        case "x": x = Double.parseDouble(keyValue[1]); break;
                        case "y": y = Double.parseDouble(keyValue[1]); break;
                    }
                }
            }

            // Update the enemy position
            updateNetworkEnemyPosition(enemyId, x, y);
        }
        // Handle enemy destruction
        else if (command.startsWith("DESTROY_ENEMY:")) {
            String enemyId = command.substring("DESTROY_ENEMY:".length());
            removeNetworkEnemy(enemyId);
        }
    }

    // Method to add a network-synchronized enemy
    public void addNetworkEnemy(String enemyId, double x, double y, String type) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            Enemy enemy = new Enemy(this);
            ImageView enemyView = enemy.createEnemyAirplane();

            if (enemyView != null) {
                enemyView.setX(x);
                enemyView.setY(y);
                enemyView.getStyleClass().add("enemy-ship");
                enemyView.setId("enemy-" + enemyId); // Use the server-provided ID

                gamepane.getChildren().add(enemyView);
                enemies.add(enemyView);

                // Don't animate locally - wait for server updates
                // Instead use a simplified animation that can be overridden by server updates
                animateNetworkEnemy(enemyView);
            }
        });
    }

    // Method to update a network enemy's position
    private void updateNetworkEnemyPosition(String enemyId, double x, double y) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            // Find the enemy with the matching ID
            for (Node node : gamepane.getChildren()) {
                if (node instanceof ImageView && node.getId() != null &&
                        node.getId().equals("enemy-" + enemyId)) {
                    ImageView enemy = (ImageView) node;

                    // Update position
                    enemy.setX(x);
                    enemy.setY(y);

                    // Check for collisions
                    checkAllPlayersCollision(enemy);
                    break;
                }
            }
        });
    }

    // Method to remove a network enemy
    private void removeNetworkEnemy(String enemyId) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            // Find the enemy with the matching ID
            for (Node node : gamepane.getChildren()) {
                if (node instanceof ImageView && node.getId() != null &&
                        node.getId().equals("enemy-" + enemyId)) {
                    ImageView enemy = (ImageView) node;

                    // Remove from game
                    gamepane.getChildren().remove(enemy);
                    enemies.remove(enemy);
                    createExplosion(enemy.getX() + enemy.getFitWidth()/2,
                            enemy.getY() + enemy.getFitHeight()/2);
                    break;
                }
            }
        });
    }

    // Simplified animation for network enemies that can be overridden
    private void animateNetworkEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    if (!gameRunning) return;

                    // Very minimal movement - will be overridden by server updates
                    enemy.setY(enemy.getY() + 0.1);

                    // Check if off screen
                    if (enemy.getY() > WINDOW_HEIGHT) {
                        Platform.runLater(() -> {
                            if (gamepane != null && gamepane.getChildren().contains(enemy)) {
                                gamepane.getChildren().remove(enemy);
                                enemies.remove(enemy);
                            }
                        });
                    } else {
                        // Check collisions
                        checkAllPlayersCollision(enemy);
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        activeAnimations.add(animation);
    }

    // 2. Modify the setupEnemySpawning method for multiplayer mode

    // Helper method to send messages to all clients (to be implemented with your network code)
    private void sendToAllClients(String message) {
        // Implement according to your network architecture
        // This is where you would use your socket connection to broadcast to all clients
        if (out != null) {
            try {
                out.println("GAME:" + message);
                out.flush();
            } catch (Exception e) {
                System.err.println("Error sending message to clients: " + e.getMessage());
            }
        }
    }

    // 3. Update the client message handling code to process server enemy commands
// This would go in your client message processing method

    // Méthode pour gérer l'arrivée d'un nouveau joueur
    public void handlePlayerJoin(String username, String aircraftType) {
        if (!connectedPlayers.contains(username)) {
            Platform.runLater(() -> {
                // Ajouter ce joueur à nos listes
                connectedPlayers.add(username);
                playerAircraftTypes.put(username, aircraftType);

                // Créer la vue pour ce nouveau joueur
                createRemotePlayerView(username, aircraftType);

                // Notification
                showNotification("Joueur " + username + " a rejoint la partie!", 2000);
            });
        }
    }

    // Méthode pour créer la vue d'un joueur distant
    private ImageView createRemotePlayerView(String username, String aircraftType) {
        if (gamepane == null) return null;

        // Créer la vue du joueur
        ImageView playerView = new ImageView();

        // Charger l'image selon le type d'avion
        try {
            Image aircraftImage = new Image(getClass().getResourceAsStream("/img/aircraft/" + aircraftType + ".png"));
            playerView.setImage(aircraftImage);
        } catch (Exception e) {
            // Image par défaut si problème
            try {
                playerView.setImage(new Image(getClass().getResourceAsStream("/img/aircraft/default.png")));
            } catch (Exception ex) {
                System.err.println("Impossible de charger l'image par défaut: " + ex.getMessage());
                return null;
            }
        }

        // Configuration de base
        playerView.setId("player-" + username);
        playerView.setFitWidth(PLAYER_WIDTH);
        playerView.setFitHeight(PLAYER_HEIGHT);

        // Position initiale (centre de l'écran)
        playerView.setX(WINDOW_WIDTH / 2 - PLAYER_WIDTH / 2);
        playerView.setY(WINDOW_HEIGHT / 2 - PLAYER_HEIGHT / 2);

        // Effet visuel pour distinguer les autres joueurs
        if (!username.equals(currentUsername)) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.BLUE);
            glow.setRadius(10);
            playerView.setEffect(glow);
        }

        // Ajouter un label avec le nom
        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 2px 5px; -fx-background-radius: 3px;");

        // Conteneur pour le nom
        StackPane nameContainer = new StackPane(nameLabel);
        nameContainer.setLayoutX(playerView.getX());
        nameContainer.setLayoutY(playerView.getY() - 20);
        nameContainer.setId("name-" + username);

        // Ajouter à la scène
        gamepane.getChildren().addAll(playerView, nameContainer);

        // Stocker la référence
        playerViews.put(username, playerView);

        return playerView;
    }

    // Méthode pour mettre à jour la position d'un joueur distant
    public void updateRemotePlayerPosition(String username, double x, double y) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            // Si le joueur n'est pas encore dans notre vue, le créer
            if (!playerViews.containsKey(username)) {
                String aircraftType = playerAircraftTypes.getOrDefault(username, "default");
                createPlayerAircraft(username, aircraftType, x, y);
                return;
            }

            // Mettre à jour la position
            ImageView playerView = playerViews.get(username);
            if (playerView != null) {
                // Animation fluide pour déplacer l'avion
                TranslateTransition transition = new TranslateTransition(Duration.millis(100), playerView);
                transition.setToX(x - playerView.getX());
                transition.setToY(y - playerView.getY());
                transition.setOnFinished(e -> {
                    playerView.setX(x);
                    playerView.setY(y);
                    playerView.setTranslateX(0);
                    playerView.setTranslateY(0);

                    // Mettre à jour la position du nom
                    Node nameContainer = gamepane.lookup("#name-" + username);
                    if (nameContainer != null) {
                        nameContainer.setLayoutX(x + PLAYER_WIDTH/2 - nameContainer.getBoundsInLocal().getWidth()/2);
                        nameContainer.setLayoutY(y - 20);
                    }
                });
                transition.play();
            }
        });
    }

    // Méthode pour gérer le départ d'un joueur
    public void handlePlayerLeave(String username) {
        if (connectedPlayers.contains(username)) {
            Platform.runLater(() -> {
                // Supprimer les éléments visuels
                ImageView playerView = playerViews.remove(username);
                if (playerView != null) {
                    gamepane.getChildren().remove(playerView);
                }

                Node nameContainer = gamepane.lookup("#name-" + username);
                if (nameContainer != null) {
                    gamepane.getChildren().remove(nameContainer);
                }

                // Mettre à jour les listes
                connectedPlayers.remove(username);
                playerAircraftTypes.remove(username);

                // Notification
                showNotification("Joueur " + username + " a quitté la partie.", 2000);
            });
        }
    }

    // Méthode pour gérer un tir distant
    public void handleRemotePlayerFire(String username, double x, double y) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            ImageView playerView = playerViews.get(username);
            if (playerView != null) {
                // Créer le laser
                Rectangle laser = new Rectangle(x + PLAYER_WIDTH/2 - 2, y - 20, 4, 15);
                laser.setFill(Color.RED);
                laser.setEffect(new Glow(0.8));

                // Ajouter à la scène
                gamepane.getChildren().add(laser);

                // Effets visuels de tir
                createMuzzleFlash(x + PLAYER_WIDTH/2, y);

                // Animation du laser
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(laser.yProperty(), laser.getY())),
                        new KeyFrame(Duration.seconds(1), new KeyValue(laser.yProperty(), -100))
                );

                timeline.setOnFinished(e -> {
                    gamepane.getChildren().remove(laser);
                });

                timeline.play();
                activeAnimations.add(timeline);

                // Gérer les collisions du laser
                new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        checkLaserCollisions(gamepane, laser);
                        if (!gamepane.getChildren().contains(laser)) {
                            this.stop();
                        }
                    }
                }.start();
            }
        });
    }

    // Effet de tir
    private void createMuzzleFlash(double x, double y) {
        Circle flash = new Circle(x, y, 10, Color.YELLOW);
        flash.setOpacity(0.8);
        flash.setEffect(new Glow(0.9));

        gamepane.getChildren().add(flash);

        FadeTransition fade = new FadeTransition(Duration.millis(150), flash);
        fade.setFromValue(0.8);
        fade.setToValue(0);
        fade.setOnFinished(e -> gamepane.getChildren().remove(flash));
        fade.play();
    }
    public void handleServerMessage(String message) {
        try {
            if (message.startsWith("PLAYER_JOIN:")) {
                // Format: PLAYER_JOIN:username,aircraftType,x,y
                String[] parts = message.substring("PLAYER_JOIN:".length()).split(",");
                if (parts.length >= 2) {
                    String username = parts[0];
                    String aircraftType = parts[1];

                    // Si le message contient aussi des coordonnées
                    double x = WINDOW_WIDTH / 2 - PLAYER_WIDTH / 2;  // position par défaut
                    double y = WINDOW_HEIGHT - PLAYER_HEIGHT - 50;

                    if (parts.length >= 4) {
                        try {
                            x = Double.parseDouble(parts[2]);
                            y = Double.parseDouble(parts[3]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid position format: " + e.getMessage());
                        }
                    }

                    // Ne pas créer d'avion pour le joueur local
                    if (!username.equals(currentUsername)) {
                        createPlayerAircraft(username, aircraftType, x, y);
                        System.out.println("Remote player joined: " + username + " at position " + x + "," + y);

                        // Envoyer notre propre position à ce joueur pour qu'il nous voie aussi
                        if (player != null && out != null) {
                            out.println("PLAYER_POS:" + currentUsername + "," + player.getX() + "," + player.getY());
                            out.flush();
                        }
                    }

                    // Notification
                    showNotification("Joueur " + username + " a rejoint la partie!", 2000);
                }
            }
            else if (message.startsWith("PLAYER_LEAVE:")) {
                // Un joueur quitte
                String username = message.substring("PLAYER_LEAVE:".length());
                handlePlayerLeave(username);
            }
            else if (message.startsWith("PLAYER_POS:")) {
                // Format: PLAYER_POS:username,x,y
                String[] parts = message.substring("PLAYER_POS:".length()).split(",");
                if (parts.length >= 3) {
                    String username = parts[0];
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);

                    // Ne pas mettre à jour notre propre position à partir du serveur
                    if (!username.equals(currentUsername)) {
                        updateRemotePlayerPosition(username, x, y);
                    }
                }
            }
            else if (message.startsWith("PLAYER_FIRE:")) {
                // Un joueur tire
                String[] parts = message.substring("PLAYER_FIRE:".length()).split(",");
                if (parts.length >= 3) {
                    String username = parts[0];
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    handleRemotePlayerFire(username, x, y);
                }
            }
            else if (message.startsWith("GAME:")) {
                // Commande liée au jeu (ennemis, etc.)
                String gameCommand = message.substring("GAME:".length());
                handleServerEnemyCommand(gameCommand);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void showLevelUpNotification() {
        Label notification = new Label("NIVEAU " + currentLevel + " !");
        notification.setStyle("-fx-font-size: 40; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, red, 10, 0.5, 0, 0);");
        notification.setLayoutX(WINDOW_WIDTH / 2 - 100);
        notification.setLayoutY(WINDOW_HEIGHT / 2 - 50);
        gamepane.getChildren().add(notification);

        // Animation de disparition
        FadeTransition fade = new FadeTransition(Duration.seconds(2), notification);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gamepane.getChildren().remove(notification));
        fade.play();
    }

    //cette méthode pour générer des power-ups
    private void spawnPowerUp(double x, double y) {
        if (new Random().nextDouble() < 0.3) {// 30% de chance de spawn
            GameManager gameManager = new GameManager();
            gameManager.slowEnemiesTemporarily(5); // Ralentit pendant 5 secondes

            PowerUp powerUp = new PowerUp(this, x, y);
            gamepane.getChildren().add(powerUp.getView());
            powerUp.animate();
        }
    }
    // ▼▼▼ Méthode mise à jour pour ralentir au lieu d'arrêter ▼▼▼
    public void slowEnemiesTemporarily(int seconds) {
        enemySpeedMultiplier = 0.3; // Réduit la vitesse à 30%

        // ▼▼▼ Animation visuelle du ralentissement ▼▼▼
        Label effectLabel = new Label("ENNEMIS RALENTIS!");
        effectLabel.setStyle("-fx-font-size: 24; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, blue, 10, 0.5, 0, 0);");
        effectLabel.setLayoutX(WINDOW_WIDTH / 2 - 100);
        effectLabel.setLayoutY(100);
        gamepane.getChildren().add(effectLabel);

        FadeTransition fade = new FadeTransition(Duration.seconds(2), effectLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gamepane.getChildren().remove(effectLabel));
        fade.play();

        // ▼▼▼ Réinitialisation après 'seconds' secondes ▼▼▼
        Timeline slowTimer = new Timeline(
                new KeyFrame(Duration.seconds(seconds), e -> {
                    enemySpeedMultiplier = 1.0; // Rétablir la vitesse normale

                    Label endLabel = new Label("LES ENNEMIS REPRENNENT LEUR VITESSE!");
                    endLabel.setStyle("-fx-font-size: 16; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, red, 5, 0.5, 0, 0);");
                    endLabel.setLayoutX(WINDOW_WIDTH / 2 - 150);
                    endLabel.setLayoutY(100);
                    gamepane.getChildren().add(endLabel);

                    FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), endLabel);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(ev -> gamepane.getChildren().remove(endLabel));
                    fadeOut.play();
                })
        );
        slowTimer.play();
        activeAnimations.add(slowTimer);
    }
    // Augmente la difficulté
    private void increaseDifficulty() {
        if (currentLevel < 5) {
            enemySpeed *= 1.2; // +20% de vitesse
        }
        if (currentLevel < 4) {
            enemiesPerWave++;   // +1 ennemi par vague
        }
        System.out.println("Niveau " + currentLevel + " - Vitesse: " + enemySpeed);
    }
    // Méthode pour vérifier si le niveau doit changer
    private void checkLevelUp() {
        if (currentLevel <= LEVEL_SCORE_THRESHOLDS.length &&
                score >= LEVEL_SCORE_THRESHOLDS[currentLevel - 1]) {

            currentLevel++;
            increaseDifficulty();
            hud.updateLevel(currentLevel);
            showLevelUpNotification();
        }
    }
    public void startFriendlyMultiplayerMode(String selectedAircraft) {
        this.selectedAircraft = selectedAircraft;
        this.isMultiplayerMode = true;

        Platform.runLater(() -> {
            try {
                // Réinitialiser l'état du jeu
                gamepane = new Pane();
                gameRunning = true;
                enemies.clear();
                playerViews.clear();
                activeAnimations.forEach(Animation::stop);
                activeAnimations.clear();
                score = 0;
                lives = 3;
                startTime = System.currentTimeMillis();

                // Setup de base
                design designHelper = new design();
                ImageView background = designHelper.loadBestBackground();
                gamepane.getChildren().add(background);

                // Créer le joueur principal
                Player playerClass = new Player(this);
                player = playerClass.createPlayer();
                gamepane.getChildren().add(player);

                // Initialiser le HUD
                hud = new HUD(this);
                hud.setupHUD();
                scoreLabel = hud.scoreLabel;
                gamepane.getChildren().add(hud.hudContainer);

                // Ajouter le joueur à la liste des vues
                playerViews.put(currentUsername, player);

                // Setup des contrôles
                setupControls();

                // Créer la scène
                Scene gameScene = new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT);
                primaryStage.setScene(gameScene);
                gamepane.requestFocus();

                // Démarrer le jeu (sans les ennemis)
                startGameThreads();
                currentLevel = 1;
                hud.updateLevel(currentLevel);

                // Notifier le serveur de notre présence dans ce mode spécial
                if (out != null) {
                    out.println("FRIENDLY_MODE_JOIN:" + currentUsername + "," + selectedAircraft);
                    out.flush();

                    // Ajouter une notification spéciale pour ce mode
                    showNotification("Mode multijoueur entre amis actif! Pas d'ennemis, juste du fun!", 3000);
                }

                System.out.println("Friendly multiplayer mode started successfully");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Critical error launching friendly multiplayer mode: " + e.getMessage());
                setupMainMenu();
            }
        });
    }
    public void createPlayerAircraft(String username, String aircraftType, double x, double y) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            // Si le joueur existe déjà, on met seulement à jour sa position
            if (playerViews.containsKey(username)) {
                ImageView existingPlayer = playerViews.get(username);
                existingPlayer.setX(x);
                existingPlayer.setY(y);

                // Mettre à jour la position du nom aussi
                Node nameLabel = gamepane.lookup("#name-" + username);
                if (nameLabel != null) {
                    nameLabel.setLayoutX(x + PLAYER_WIDTH/2 - nameLabel.getBoundsInLocal().getWidth()/2);
                    nameLabel.setLayoutY(y - 20);
                }
                return;
            }

            // Créer une nouvelle vue pour le joueur
            ImageView playerView = new ImageView();
            try {
                // Charger l'image correspondant au type d'avion
                String imagePath = "aircraft/" + aircraftType + ".png";
                Image aircraftImage = new Image(getClass().getResourceAsStream(imagePath));
                playerView.setImage(aircraftImage);
                System.out.println("Aircraft image loaded: " + imagePath);
            } catch (Exception e) {
                System.err.println("Unable to load aircraft image for type: " + aircraftType);
                try {
                    // Utiliser une image par défaut si l'image demandée n'est pas trouvée
                    playerView.setImage(new Image(getClass().getResourceAsStream("airplane.png")));
                } catch (Exception ex) {
                    System.err.println("Failed to load default aircraft image: " + ex.getMessage());
                    return;
                }
            }

            // Configurer l'affichage de l'avion
            playerView.setId("player-" + username);
            playerView.setFitWidth(PLAYER_WIDTH);
            playerView.setFitHeight(PLAYER_HEIGHT);
            playerView.setX(x);
            playerView.setY(y);

            // Ajouter un effet visuel pour distinguer les joueurs
            if (!username.equals(currentUsername)) {
                DropShadow glow = new DropShadow();
                glow.setColor(Color.BLUE);
                glow.setRadius(10);
                playerView.setEffect(glow);
            }

            // Créer et ajouter un label avec le nom du joueur
            Label nameLabel = new Label(username);
            nameLabel.setTextFill(Color.WHITE);
            nameLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 2px 5px; -fx-background-radius: 3px;");

            StackPane nameContainer = new StackPane(nameLabel);
            nameContainer.setLayoutX(x + PLAYER_WIDTH/2 - 30); // Approximation de la largeur
            nameContainer.setLayoutY(y - 20);
            nameContainer.setId("name-" + username);

            // Ajouter les éléments à la scène
            gamepane.getChildren().addAll(playerView, nameContainer);

            // Enregistrer la référence de l'avion
            playerViews.put(username, playerView);
            playerAircraftTypes.put(username, aircraftType);
            connectedPlayers.add(username);

            System.out.println("Created aircraft for player: " + username + " with type: " + aircraftType);
        });
    }


}