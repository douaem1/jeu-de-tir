package Game;

import DAO.Scores;
import chat_Client_Serveur.GameState;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
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
import java.util.*;
import java.util.concurrent.*;


import design.design;
import design.*;
import Menu.MenuManager;

public class GameManager {

    public Stage primaryStage;
    public int scoreMultiplier = 1;
    public int currentLevel = 1;
    public double enemySpeed = 0.5; // Vitesse de base
    public double enemySpeedMultiplier = 1.0;
    public int enemiesPerWave = 1;
    private final int[] LEVEL_SCORE_THRESHOLDS = {60, 100, 160, 220, 290, 350};
    private boolean isInvincible = false;

    String currentUsername;
    String selectedAircraft;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int PLAYER_WIDTH = 50;
    public  static final int PLAYER_HEIGHT = 50;
    private long startTime;
    private Map<String, ImageView> opponentPlayers = new ConcurrentHashMap<>();
    private Map<String, Integer> opponentHealth = new ConcurrentHashMap<>();
    private boolean pvpMode = false;



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

    private final Set<KeyCode> keysPressed = new HashSet<>();


    // Thread et état du jeu
    public ExecutorService gameExecutor = Executors.newFixedThreadPool(3);
    public volatile boolean gameRunning = false;

    // Chat

    public PrintWriter out;

    public final  Map<String, ImageView> playerViews = new HashMap<>();
    private final Map<String, String> playerAircraftTypes = new HashMap<>();
    private  final Set<String> connectedPlayers = new HashSet<>();



    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setupMainMenu() {
        try {

            StackPane root = new StackPane();
            design design = new design();
            animation animation = new animation();
            animation.setPrimaryStage(primaryStage);
            ImageView background = design.loadBestBackground();
            root.getChildren().add(background);
            design.animateBackground(background);
            Rectangle overlay = design.createOverlay();
            root.getChildren().add(overlay);
            root.getChildren().add(design.createParticleEffect());
            MenuManager menumanager = new MenuManager(primaryStage);
            VBox mainContainer = menumanager.createMainContainer();
            root.getChildren().add(mainContainer);
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
        scoreLabel = hud.scoreLabel;
        if (gamepane != null) {
            gamepane.getChildren().add(hud.hudContainer);
        }
    }

    public void startGame(String selectedAircraft) {
        this.selectedAircraft = selectedAircraft;

        Platform.runLater(() -> {
            try {
                gamepane = new Pane();
                gameRunning = true;
                enemies.clear();
                playerViews.clear();
                activeAnimations.forEach(Animation::stop);
                activeAnimations.clear();
                score = 0;
                lives = 3;
                startTime=System.currentTimeMillis();
                design designHelper = new design();
                ImageView background = designHelper.loadBestBackground();
                gamepane.getChildren().add(background);
                Player playerClass = new Player(this);
                player = playerClass.createPlayer();
                double initialX = WINDOW_WIDTH / 2 - PLAYER_WIDTH / 2;
                double initialY = WINDOW_HEIGHT - PLAYER_HEIGHT - 50;
                player.setX(initialX);
                player.setY(initialY);

                gamepane.getChildren().add(player);
                hud = new HUD(this);
                hud.setupHUD();
                scoreLabel = hud.scoreLabel;
                gamepane.getChildren().add(hud.hudContainer);
                playerViews.put(currentUsername, player);
                playerAircraftTypes.put(currentUsername, selectedAircraft);
                connectedPlayers.add(currentUsername);
                setupControls();
                Scene gameScene = new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT);
                primaryStage.setScene(gameScene);
                gamepane.requestFocus();
                setupEnemySpawning();
                startGameThreads();
                currentLevel = 1;
                hud.updateLevel(currentLevel);
                if (isMultiplayerMode) {
                    // Envoyer une demande d'état initial au serveur
                    if (out != null) {
                        out.println("SYNC_ENEMIES");
                        out.flush();
                    }
                    boolean isHost = false;
                    if (isHost) {
                        setupEnemySpawning();
                    }
                } else {
                    setupEnemySpawning();
                }
                System.out.println("Game started successfully");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Critical error launching game: " + e.getMessage());
                setupMainMenu();
            }
        });
    }
       public void setupControls() {
        if (gamepane == null) return;

        gamepane.setFocusTraversable(true);
        gamepane.requestFocus();
        Player playerClass = new Player(this);
        gamepane.setOnKeyPressed(e -> {
            if (!gameRunning) return;

            keysPressed.add(e.getCode());
            if (e.getCode() == KeyCode.SPACE) {
                System.out.println("Space key pressed - firing laser");
                playerClass.fireEnhancedLaser(gamepane, player);
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
        AnimationTimer positionUpdater = new AnimationTimer() {
            private long lastUpdateTime = 0;

            @Override
            public void handle(long now) {
                if (!gameRunning) return;
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
                    if (currentLevel >= 5 && Math.random() < 0.3) {
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

    boolean isMultiplayerMode = false;



    private void gameOver(Pane gamePane) {
        gameRunning = false;
        long endTime = System.currentTimeMillis();
        int durationInSeconds = (int) ((endTime - startTime) / 1000);

        Scores scoreRecord = new Scores();
        scoreRecord.setUsername(currentUsername);
        scoreRecord.setScore(score);
        scoreRecord.setRoundDuration(durationInSeconds);
        Scores.addScore(scoreRecord);
        System.out.println("score ajouté ");
        animation animation = new animation();
        stopAllAnimations();
        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 15;");
        gameOverBox.setPadding(new Insets(40));
        Label title = new Label("GAME OVER");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 56));
        title.setTextFill(COLORS.get("LIGHT"));
        Label scoreLabel = new Label("Final Score: " + score);
        scoreLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 24));
        scoreLabel.setTextFill(COLORS.get("LIGHT"));
        Button restartBtn = animation.createActionButton("PLAY AGAIN", "PRIMARY");
        restartBtn.setPrefWidth(200);
        restartBtn.setOnAction(e -> {
            stopAllAnimations();
            gamePane.getChildren().clear();
            enemies.clear();
            activeAnimations.clear();
            keysPressed.clear();
            playerViews.clear();
            gameRunning = true;
            score = 0;
            lives = 3;
            gameRunning = true;
            isInvincible = false;
            currentLevel = 1;
            enemySpeed = 0.5;
            enemySpeedMultiplier = 1.0;
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
    private boolean isProcessingHit = false; // Nouvel indicateur pour éviter le traitement multiple
    private Timeline invincibilityTimeline; // Référence à l'animation d'invincibilité


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
                new KeyFrame(Duration.millis(300), new KeyValue(player.opacityProperty(), 1.0))
        );
        invincibilityTimeline.setCycleCount(10); // ~3 secondes au total
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



    public void animateEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(8), e -> {
                    if (!gameRunning ) return;

                    enemy.setY(enemy.getY() + (enemySpeed * enemySpeedMultiplier));
                    if (enemy.getY() > WINDOW_HEIGHT) {

                        enemies.remove(enemy);

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
     * Vérifie la collision entre un ennemi et le joueur
     * Cette méthode est conçue pour être appelée depuis animateEnemy
     */
    private void checkPlayerCollision(ImageView enemy) {
        // Ne pas vérifier les collisions si le joueur est déjà invincible ou en train de traiter un hit
        if (isInvincible || isProcessingHit) {
            return;
        }

        // Vérifier que l'ennemi et le joueur sont tous les deux visibles et actifs
        if (!enemy.isVisible() || !player.isVisible() || !enemies.contains(enemy)) {
            return;
        }

        // Obtenir les rectangles de collision avec une marge de précision
        Bounds playerBounds = player.getBoundsInParent();
        Bounds enemyBounds = enemy.getBoundsInParent();

        // Créer une hitbox légèrement plus petite pour l'ennemi (meilleure précision)
        double margin = Math.min(enemy.getFitWidth(), enemy.getFitHeight()) * 0.15; // 15% de marge

        // Vérifier l'intersection avec la hitbox ajustée
        boolean collision = playerBounds.intersects(
                enemyBounds.getMinX() + margin,
                enemyBounds.getMinY() + margin,
                enemyBounds.getWidth() - (margin * 2),
                enemyBounds.getHeight() - (margin * 2)
        );

        if (collision) {
            // Marquer immédiatement comme invincible pour éviter les hits multiples
            isProcessingHit = true;
            isInvincible = true;

            // Log de débogage pour suivre les collisions
            System.out.println("Collision détectée à " + System.currentTimeMillis() +
                    " | Joueur: (" + player.getX() + "," + player.getY() + ")" +
                    " | Ennemi: (" + enemy.getX() + "," + enemy.getY() + ")");

            // Retirer immédiatement l'ennemi de la liste pour éviter les collisions multiples
            enemies.remove(enemy);

            // Utiliser runLater pour gérer les opérations UI
            Platform.runLater(() -> {
                // Vérifier que l'ennemi est toujours dans la scène
                if (gamepane.getChildren().contains(enemy)) {
                    gamepane.getChildren().remove(enemy);

                    // Créer une explosion à la position de la collision
                    createExplosion(
                            enemy.getX() + enemy.getFitWidth()/2,
                            enemy.getY() + enemy.getFitHeight()/2
                    );
                }

                // Gérer la perte de vie
                handlePlayerHit();
            });
        }
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
    public void addNetworkEnemy(String enemyId, double x, double y) {
        Platform.runLater(() -> {
            if (!gameRunning) return;

            Enemy enemy = new Enemy(this);
            ImageView enemyView = enemy.createEnemyAirplane();

            if (enemyView != null) {
                enemyView.setX(x);
                enemyView.setY(y);
                enemyView.getStyleClass().add("enemy-ship");
                enemyView.setId("enemy-" + enemyId);
                gamepane.getChildren().add(enemyView);
                enemies.add(enemyView);
                animateNetworkEnemy(enemyView);
            }
        });
    }
    private void animateNetworkEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    if (!gameRunning) return;
                    enemy.setY(enemy.getY() + 0.1);
                    if (enemy.getY() > WINDOW_HEIGHT) {
                        Platform.runLater(() -> {
                            if (gamepane != null && gamepane.getChildren().contains(enemy)) {
                                gamepane.getChildren().remove(enemy);
                                enemies.remove(enemy);
                            }
                        });
                    } else {
                        checkAllPlayersCollision(enemy);
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        activeAnimations.add(animation);
    }
    private void showLevelUpNotification() {
        Label notification = new Label("NIVEAU " + currentLevel + " !");
        notification.setStyle("-fx-font-size: 40; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, red, 10, 0.5, 0, 0);");
        notification.setLayoutX(WINDOW_WIDTH / 2 - 100);
        notification.setLayoutY(WINDOW_HEIGHT / 2 - 50);
        gamepane.getChildren().add(notification);
        FadeTransition fade = new FadeTransition(Duration.seconds(2), notification);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gamepane.getChildren().remove(notification));
        fade.play();
    }
    private void spawnPowerUp(double x, double y) {
        if (currentLevel >= 2) {
            if (new Random().nextDouble() < 0.5) {// 30% de chance de spawn
                //slowEnemiesTemporarily(5); // Ralentit pendant 5 secondes
                PowerUp powerUp = new PowerUp(this, x, y);
                gamepane.getChildren().add(powerUp.getView());
                powerUp.animate();
            }
        }
    }
    public void slowEnemiesTemporarily(int seconds) {
        enemySpeedMultiplier = 0.3; // Réduit la vitesse à 30%
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
    private void increaseDifficulty() {
        if (currentLevel < 2) {
            enemySpeed *= 1.2;
        }
        if (currentLevel < 2) {
            enemiesPerWave++;
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


}