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
    private boolean isInvincible = false;
    public String selelectedAircraft;
    public int scoreMultiplier = 1;
    public int currentLevel = 1;
    private final int[] LEVEL_SCORE_THRESHOLDS = {60, 100, 160, 220, 290, 350}; // Scores pour débloquer les niveaux
    public double enemySpeed = 0.5; // Vitesse de base
    public double enemySpeedMultiplier = 1.0; // Nouvelle variable
    public int enemiesPerWave = 1;  // Nombre d'ennemis par vague
    public Stage primaryStage;
    public MenuManager menuManager;
    public Authentification auth;

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

    public GameManager() {
        this.primaryStage = primaryStage;
    }

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

    public HUD hud;

    public void setupHUD() {
        hud = new HUD(this);
        hud.setupHUD();
        gamepane.getChildren().add(hud.hudContainer);
    }

    // In GameManager.java, modify the startGame method:
// Dans le GameManager, remplacez la méthode startGame() comme ceci:
    public void startGame(String selectedAircraft) {
        this.selelectedAircraft = selectedAircraft;
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
                currentLevel = 1;  // Réinitialisez le niveau
                hud.updateLevel(currentLevel);  // Mettez à jour l'affichage

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
        Player playerClass = new Player(this); // Passer THIS comme référence

        gamepane.setOnKeyPressed(e -> {
            if (!gameRunning) return;

            double speed = 14;
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
                    // Appelle la méthode de tir sans le hud (qui n'est pas nécessaire)
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

    private void checkPlayerCollision(ImageView enemy) {
        if (player == null || isInvincible || !gameRunning || lives <= 0) return;
        if (enemy.getBoundsInParent().intersects(player.getBoundsInParent())) {
            System.out.println("Collision unique détectée");
            System.out.println("Collision détectée à: " + System.currentTimeMillis());
            System.out.println("Position joueur: " + player.getBoundsInParent());
            System.out.println("Position ennemi: " + enemy.getBoundsInParent());

            // Suppression IMMÉDIATE et SYNCHRONE de l'ennemi
            Platform.runLater(() -> {
                gamepane.getChildren().remove(enemy);
                enemies.remove(enemy);
            });
            handlePlayerHit();
        }
    }

    public void handlePlayerHit() {
        // Vérifier si le joueur est déjà invincible ou n'a plus de vies
        if (isInvincible || lives <= 0) return;

        // Activer l'invincibilité IMMÉDIATEMENT
        isInvincible = true;

        // Décrémenter UNE vie seulement
        lives = Math.max(0, lives - 1);
        hud.updateLives(lives);

        // ▼▼▼ Combiner clignotement et invincibilité dans UNE animation ▼▼▼
        Timeline protection = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(player.opacityProperty(), 0.3)), // Opacité à 30%),
                new KeyFrame(Duration.millis(150), // Toutes les 150ms
                        String.valueOf(new KeyFrame(Duration.seconds(1.5), // Durée totale : 1.5 secondes
                                e -> {
                                    isInvincible = false; // Désactiver l'invincibilité
                                    System.out.println("Fin de la protection");
                                }
                        ))
                )
        );

        // Configurer le clignotement (10 cycles)
        protection.setCycleCount(10); // 10 × 150ms = 1.5s
        protection.setAutoReverse(true); // Alternance transparent/visible

        protection.play();
        activeAnimations.add(protection);

        // ▼▼▼ Vérifier le Game Over APRÈS la mise à jour ▼▼▼
        if (lives <= 0) {
            gameRunning = false;
            gameOver(gamepane);
        }
    }

    private void gameOver(Pane gamePane) {
        gameRunning = false;
        stopAllAnimations();

        // ▼▼▼ Nettoyage des ennemis ▼▼▼
        Platform.runLater(() -> {
            gamePane.getChildren().removeAll(enemies);
            enemies.clear();
        });

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
        animation animation = new animation();
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
                startGame(selelectedAircraft);
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
        enemies.clear();
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

    // Affiche une notification de changement de niveau
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

}
