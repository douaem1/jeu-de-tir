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
import Game.Enemy;
import Game.Player;
import design.animation;
import design.HUD;
import Menu.MenuManager;
import Menu.Authentification;


// ========================= MENU PRINCIPAL =========================



public class GameManager {

    private Stage primaryStage;
    private MenuManager menuManager;
    private Authentification auth;

    // Configuration
    public static int WINDOW_WIDTH = 1200;
    public static int WINDOW_HEIGHT = 800;
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

    // Dans la section Éléments du jeu

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


    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Transmettre le stage aux autres classes
        MenuManager menuManager = new MenuManager();
        menuManager.setPrimaryStage(primaryStage);
        Authentification auth = new Authentification();
        auth.setPrimaryStage(primaryStage);
    }


    public void setupMainMenu() {
        StackPane root = new StackPane();
        design design = new design();
        animation animation = new animation();
        // Chargement optimisé du fond
        ImageView background = design.loadBestBackground();
        root.getChildren().add(background);
        design.animateBackground(background);

        // Overlay avec effet de dégradé amélioré
        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        // Particules optimisées
        root.getChildren().add(design.createParticleEffect());

        // Contenu principal
        MenuManager menumanager = new MenuManager();
        VBox mainContainer = menumanager.createMainContainer();
        root.getChildren().add(mainContainer);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);

        primaryStage.show();
    }
    public void setupControls() {
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
                    Player player1 = new Player();
                    player1.fireEnhancedLaser(gamepane, player, hudContainer); // Utilisez hudContainer ici
                    break;
                case ESCAPE:
                    MenuManager menumanager = new MenuManager();
                    menumanager.returnToMenu();
                    break;
            }
        });
    }

    public void startGame() {
        gamepane = new Pane();
        Player Player = new Player();
        design design = new design();
        animation animation = new animation();
        player = Player.createPlayer();
        gamepane.getChildren().add(player);
        score = 0;
        lives = 3;
        gameRunning = true;
        ImageView background = design.loadBestBackground();
        gamepane.getChildren().add(background);
        design.animateBackground(background);
        player = Player.createPlayer();
        gamepane.getChildren().add(player);

        // Initialisation du HUD
        HUD hud = new HUD();
        hud.HUD();
        hud.updateHUD();
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
                    Enemy enemy1 = new Enemy();
                    ImageView enemy = enemy1.createEnemyAirplane();
                    gamepane.getChildren().add(enemy);
                    enemies.add(enemy);
                    animateEnemy(enemy);
                }
                ));
        enemySpawner.setCycleCount(Animation.INDEFINITE);
        enemySpawner.play();
        activeAnimations.add(enemySpawner);
    }

    public void animateEnemy(ImageView enemy) {
        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(1000), e -> {
                    enemy.setY(enemy.getY() + 0.5);
                    checkPlayerCollision(enemy);
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
                startGame();
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

    private void fireLaser() {
        Rectangle laser = new Rectangle(5, 20, Color.RED);
        laser.setX(player.getX() + player.getFitWidth() / 2 - 2.5);
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
                        ((Timeline) e.getSource()).stop();
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
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
                    scoreLabel.setText("SCORE: " + score); // Mise à jour directe du label


                });
                return true;
            }
            return false;
        });
    }

    public void updateGameState() {
        Enemy enemy = new Enemy();
        enemy.updateEnemies();
        HUD hud = new HUD();
        hud.updateHUD();
        try {
            // Code qui pourrait causer un problème
        } catch (Exception e) {
            System.err.println("Erreur détectée: " + e.getMessage());
            e.printStackTrace();
            // Continuer le jeu au lieu de planter
        }
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


    public void initializeGame() {
        gamepane = new Pane();
        Player Player = new Player();
        HUD hud = new HUD();
        setupGameBackground();

        player = Player.createPlayer();
        gamepane.getChildren().add(player);
        // Vérification de position
        System.out.println("Position joueur - X: " + player.getX() + " Y: " + player.getY());

        hud.setupHUD();
        setupControls();
        primaryStage.setScene(new Scene(gamepane, WINDOW_WIDTH, WINDOW_HEIGHT));
        gamepane.getChildren().add(player);
    }

    public void setupGameBackground() {
        design design = new design();
        ImageView background = design.loadBestBackground();
        gamepane.getChildren().add(background);
        design.animateBackground(background);
    }

    private void createExplosion(double x, double y) {
        Circle explosion = new Circle(x, y, 0, Color.ORANGERED);
        Glow glow = new Glow(0.8);
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

    public void startGameThreads() {
        // Thread de mise à jour du jeu

        gameExecutor.submit(() -> {
            while (gameRunning) {
                Platform.runLater(this::updateGameState);
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        // Thread d'apparition des ennemis
        gameExecutor.submit(() -> {

            while (gameRunning) {
                Enemy enemy = new Enemy();
                Platform.runLater(() -> enemy.createEnemyAirplane());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
}

