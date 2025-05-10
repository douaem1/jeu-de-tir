package Game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.Glow;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Player {
    // Référence directe au GameManager parent
    private GameManager gameManager;
    private String selectedAircraft;

    // Constantes
    private final double LASER_SPEED = 10.0;

    // Nouvelle variable pour gérer le mode multijoueur
    private boolean isMultiplayerMode = false;

    public Player(GameManager gameManager) {
        this.gameManager = gameManager;
        // Get the selected aircraft from the GameManager
        if (gameManager != null) {
            this.selectedAircraft = gameManager.selectedAircraft;
        }
    }

    // Constructeur alternatif pour le mode multijoueur
    public Player(GameManager gameManager, boolean isMultiplayerMode) {
        this(gameManager);
        this.isMultiplayerMode = isMultiplayerMode;
    }

    public void fireEnhancedLaser(Pane gamePane, ImageView player) {
        System.out.println("Tentative de tir: " + (gameManager != null ? "Manager OK" : "Manager NULL"));

        // Vérification visuelle dans la console pour s'assurer que le tir est tenté
        System.out.println("Position du laser: X=" + (player.getX() + player.getFitWidth()/2) + ", Y=" + player.getY());

        // Création du laser
        Rectangle laser = new Rectangle(6, 25, Color.LIMEGREEN);
        laser.setX(player.getX() + player.getFitWidth()/2 - 3);
        laser.setY(player.getY() - 10); // Commence juste au-dessus du joueur

        // Ajout d'un effet visuel pour mieux voir le laser
        laser.setArcWidth(4);
        laser.setArcHeight(4);

        // Ajout du laser à la scène
        Platform.runLater(() -> {
            gamePane.getChildren().add(laser);
            System.out.println("Laser ajouté à la scène");
        });

        // Animation du mouvement du laser
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(16), event -> {
                    laser.setY(laser.getY() - LASER_SPEED);

                    // Vérification des collisions
                    if (gameManager != null) {
                        checkLaserCollisions(gamePane, laser);
                    }

                    // Suppression du laser s'il sort de l'écran
                    if (laser.getY() < -30) {
                        Platform.runLater(() -> {
                            gamePane.getChildren().remove(laser);
                        });
                    }
                })
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Ajouter aux animations actives pour pouvoir l'arrêter plus tard
        if (gameManager != null) {
            gameManager.activeAnimations.add(timeline);
        }
    }


    public ImageView createPlayer() {
        return createPlayerWithImage(false, 0);
    }

    // Nouvelle méthode pour créer un joueur pour le mode multijoueur
    public ImageView createMultiplayerPlayer(boolean isOpponent) {
        return createPlayerWithImage(true, isOpponent ? 180 : 0);
    }
    public ImageView player;
    public CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    public int score = 0;
    public int lives = 3;
    public static Pane gamepane;
    public BorderPane hudContainer;
    public Label scoreLabel;
    public Label levelLabel;
    public ProgressBar healthBar;
    public Label healthLabel;
    public Label ammoLabel;
    public Label notificationLabel;
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

    private ImageView createPlayerWithImage(boolean isMultiplayer, double rotation) {
        try {
            String aircraftImagePath = getAircraftImagePath(selectedAircraft);
            System.out.println("Loading aircraft image: " + aircraftImagePath);

            // Tentative de chargement de l'image avec gestion d'erreur améliorée
            Image image;
            try {
                image = new Image(getClass().getResourceAsStream(aircraftImagePath));
                if (image.isError()) {
                    throw new Exception("Image loading error: " + image.getException().getMessage());
                }
                System.out.println("Aircraft image loaded successfully");
            } catch (Exception e) {
                System.err.println("Failed to load aircraft image: " + e.getMessage());
                // Fallback to a simpler path
                image = new Image("/airplane.png");
                if (image.isError()) {
                    // Ultimate fallback - create a colored rectangle
                    Rectangle rect = new Rectangle(100, 80, Color.BLUE);
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    image = rect.snapshot(params, null);
                    System.out.println("Using fallback rectangle as player image");
                }
            }

            ImageView player = new ImageView(image);
            player.setFitWidth(100);
            player.setPreserveRatio(true);

            // Configuration spécifique pour le multijoueur
            if (isMultiplayer) {
                player.setRotate(rotation);
            }


            // Pour le débogage
            System.out.println("Player created with rotation: " + rotation);

            return player;

        } catch (Exception e) {
            System.err.println("Critical error creating player: " + e.getMessage());
            e.printStackTrace();

            // Fallback to a simple shape if everything else fails
            Rectangle fallbackPlayer = new Rectangle(100, 80, Color.BLUE);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            Image fallbackImage = fallbackPlayer.snapshot(params, null);
            ImageView fallbackView = new ImageView(fallbackImage);
            fallbackView.setRotate(rotation);
            return fallbackView;
        }
    }

    private String getAircraftImagePath(String aircraftName) {
        if (aircraftName == null) {
            System.out.println("Warning: No aircraft selected, using default");
            return "/airplane.png"; // Default aircraft
        }

        switch (aircraftName) {
            case "F-22 Raptor":
                return "/avion1.png";
            case "Eurofighter Typhoon":
                return "/avion2.png";
            case "Sukhoi Su-57":
                return "/airplane.png";
            default:
                System.out.println("Unknown aircraft: " + aircraftName + ", using default");
                return "/airplane.png";  // Option par défaut
        }
    }
    class player {
        private String name;
        private String aircraftType;
        private Image image;
        private double x, y;
        private int score;
        private int lives;
        private boolean isHost;


        public player(String name, String aircraftType, boolean isHost) {
            this.name = name;
            this.aircraftType = aircraftType;
            this.isHost = isHost;
            this.lives = GameConstants.MAX_LIVES;
            loadImage();
        }

        private void loadImage() {
            try {
                this.image = new Image(getClass().getResourceAsStream(
                        "/images/" + aircraftType + ".png"));
            } catch (Exception e) {
                // Image par défaut si problème
                this.image = new Image(getClass().getResourceAsStream(
                        "/images/default_aircraft.png"));
            }
        }

        // Getters et setters
        public Image getImage() { return image; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public int getScore() { return score; }
        public void addScore(int points) { score += points; }
        public int getLives() { return lives; }
        public void loseLife() { lives--; }
        public boolean isAlive() { return lives > 0; }
        public String getName() { return name; }
        public boolean isHost() { return isHost; }
    }
}