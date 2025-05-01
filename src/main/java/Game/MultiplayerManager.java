package Game;

import javafx.application.Platform;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.geometry.Pos;

import chat_Client_Serveur.GameState;
import java.util.*;

/**
 * Enhanced Multiplayer Manager for managing opponent aircraft display and synchronization
 */
public class MultiplayerManager {
    private GameManager gameManager;
    private Map<String, ImageView> playerViews = new HashMap<>();
    private Map<String, StackPane> playerContainers = new HashMap<>();
    private String currentUsername;

    public MultiplayerManager(GameManager gameManager, String currentUsername) {
        this.gameManager = gameManager;
        this.currentUsername = currentUsername;
        preloadAircraftImages();
    }

    /**
     * Preload all aircraft images to avoid loading delays during gameplay
     */
    private void preloadAircraftImages() {
        // Make sure the game manager's aircraft images are loaded
        if (gameManager.aircraftImages.isEmpty()) {
            loadDefaultAircraftImages();
        }
    }

    /**
     * Load default aircraft images if they haven't been loaded yet
     */
    private void loadDefaultAircraftImages() {
        try {
            // These are fallback images in case the main loading fails
            gameManager.aircraftImages.put("default", new Image(getClass().getResourceAsStream("/airplane.png")));
            gameManager.aircraftImages.put("fighter", new Image(getClass().getResourceAsStream("/avion1.png")));
            gameManager.aircraftImages.put("bomber", new Image(getClass().getResourceAsStream("/avion2.png")));
            System.out.println("Default aircraft images loaded");
        } catch (Exception e) {
            System.err.println("Failed to load default aircraft images: " + e.getMessage());
            // Create emergency placeholder images
            createPlaceholderImage("default");
            createPlaceholderImage("fighter");
            createPlaceholderImage("bomber");
        }
    }

    /**
     * Create a placeholder colored image when actual images can't be loaded
     */
    private void createPlaceholderImage(String type) {
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(50, 50);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // Different colors for different aircraft types
        switch(type) {
            case "fighter":
                gc.setFill(javafx.scene.paint.Color.RED);
                break;
            case "bomber":
                gc.setFill(javafx.scene.paint.Color.GREEN);
                break;
            default:
                gc.setFill(javafx.scene.paint.Color.BLUE);
        }

        // Draw a simple aircraft shape
        gc.fillRect(0, 0, 50, 50);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillPolygon(
                new double[]{25, 10, 40},
                new double[]{10, 40, 40},
                3
        );

        // Convert canvas to image
        javafx.scene.image.WritableImage img = new javafx.scene.image.WritableImage(50, 50);
        canvas.snapshot(null, img);
        gameManager.aircraftImages.put(type, img);
    }

    /**
     * Update all players from the latest game state data
     */
    public void updatePlayers(Map<String, GameState.Position> positions,
                              Map<String, String> aircrafts) {
        Platform.runLater(() -> {
            // First, handle players who may have left
            Set<String> currentPlayers = new HashSet<>(positions.keySet());
            List<String> playersToRemove = new ArrayList<>();

            for (String username : playerViews.keySet()) {
                if (!currentPlayers.contains(username)) {
                    playersToRemove.add(username);
                }
            }

            // Remove players who are no longer in the game
            for (String username : playersToRemove) {
                removePlayer(username);
            }

            // Update or add current players
            for (String username : positions.keySet()) {
                GameState.Position pos = positions.get(username);
                String aircraftType = aircrafts.getOrDefault(username, "default");

                // Don't handle the current player here - it's managed separately
                if (!username.equals(currentUsername)) {
                    updateOrCreatePlayer(username, pos, aircraftType);
                }
            }
        });
    }

    /**
     * Update an existing player or create a new one
     */
    private void updateOrCreatePlayer(String username, GameState.Position pos, String aircraftType) {
        if (playerViews.containsKey(username)) {
            // Update existing player
            ImageView playerView = playerViews.get(username);
            StackPane container = playerContainers.get(username);

            // Update position
            playerView.setX(pos.x);
            playerView.setY(pos.y);
            container.setLayoutX(pos.x);
            container.setLayoutY(pos.y);

            // Check if aircraft type changed
            if (!playerView.getId().endsWith(aircraftType)) {
                updatePlayerAircraft(playerView, username, aircraftType);
            }
        } else {
            // Create new player
            createNewPlayer(username, pos, aircraftType);
        }
    }

    /**
     * Create a new player aircraft and add it to the game
     */
    private void createNewPlayer(String username, GameState.Position pos, String aircraftType) {
        // Create player image
        ImageView playerView = new ImageView();
        playerView.setId("player-" + username + "-" + aircraftType);

        // Get aircraft image
        Image aircraftImage = gameManager.aircraftImages.get(aircraftType);
        if (aircraftImage == null) {
            aircraftImage = gameManager.aircraftImages.get("default");
        }
        playerView.setImage(aircraftImage);

        // Set size
        playerView.setFitWidth(GameManager.PLAYER_WIDTH);
        playerView.setFitHeight(GameManager.PLAYER_HEIGHT);

        // Add visual effects to distinguish opponents
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setHue(0.2); // Red tint for opponents
        playerView.setEffect(colorAdjust);

        // Create name label
        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-padding: 3px;");

        // Create container
        StackPane playerContainer = new StackPane();
        playerContainer.getChildren().addAll(playerView, nameLabel);
        StackPane.setAlignment(nameLabel, Pos.TOP_CENTER);

        // Position the container
        playerContainer.setLayoutX(pos.x);
        playerContainer.setLayoutY(pos.y);

        // Store references
        playerViews.put(username, playerView);
        playerContainers.put(username, playerContainer);

        // Add to game
        if (gameManager.gamepane != null) {
            gameManager.gamepane.getChildren().add(playerContainer);
            System.out.println("Created new opponent: " + username + " with aircraft: " + aircraftType);
        }
    }

    /**
     * Update a player's aircraft image
     */
    private void updatePlayerAircraft(ImageView playerView, String username, String aircraftType) {
        // Get new aircraft image
        Image aircraftImage = gameManager.aircraftImages.get(aircraftType);
        if (aircraftImage == null) {
            aircraftImage = gameManager.aircraftImages.get("default");
        }

        // Update image and ID
        playerView.setImage(aircraftImage);
        playerView.setId("player-" + username + "-" + aircraftType);
    }

    /**
     * Remove a player who has left the game
     */
    private void removePlayer(String username) {
        if (playerContainers.containsKey(username)) {
            StackPane container = playerContainers.get(username);
            if (gameManager.gamepane != null) {
                gameManager.gamepane.getChildren().remove(container);
            }
            playerViews.remove(username);
            playerContainers.remove(username);
            System.out.println("Removed player: " + username);
        }
    }

    /**
     * Clear all players (useful when restarting or quitting)
     */
    public void clearAllPlayers() {
        if (gameManager.gamepane != null) {
            for (StackPane container : playerContainers.values()) {
                gameManager.gamepane.getChildren().remove(container);
            }
        }
        playerViews.clear();
        playerContainers.clear();
    }
}