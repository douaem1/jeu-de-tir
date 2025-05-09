package Game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

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
                        gameManager.checkLaserCollisions(gamePane, laser);
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
}