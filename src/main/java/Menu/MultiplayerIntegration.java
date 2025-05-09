package Menu;

import Game.MultiplayerManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import design.animation;

/**
 * Class that handles the multiplayer menu and connection interface
 */
public class MultiplayerIntegration {
    private Stage primaryStage;
    private MenuManager menuManager;
    private String username;

    /**
     * Constructor for the multiplayer menu
     * @param primaryStage The main application window
     * @param menuManager Reference to the menu manager
     */
    public MultiplayerIntegration(Stage primaryStage, MenuManager menuManager) {
        this.primaryStage = primaryStage;
        this.menuManager = menuManager;
    }

    /**
     * Creates and returns the multiplayer lobby interface
     * @return VBox containing the multiplayer menu elements
     */
    public VBox createMultiplayerLobby() {
        animation animation = new animation();

        // Main container
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(30));
        container.setMaxWidth(500);
        container.setStyle("-fx-background-color: rgba(20, 20, 40, 0.85); -fx-background-radius: 15;");

        // Title
        Label title = new Label("MULTIPLAYER MODE");
        title.setFont(Font.font("Agency FB", FontWeight.BOLD, 36));
        title.setStyle("-fx-text-fill: #F18F01;");

        // Username field
        HBox usernameBox = new HBox(15);
        usernameBox.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("USERNAME:");
        usernameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        usernameLabel.setStyle("-fx-text-fill: #F5F5F5;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefWidth(250);
        usernameField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA;");

        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Status label
        Label statusLabel = new Label("Waiting for connection...");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        statusLabel.setStyle("-fx-text-fill: #F5F5F5;");
        statusLabel.setVisible(false);

        // Buttons
        Button connectButton = animation.createActionButton("CONNECT TO SERVER", "PRIMARY");
        connectButton.setPrefWidth(300);

        Button backButton = animation.createActionButton("BACK TO MENU", "SECONDARY");
        backButton.setPrefWidth(300);

        // Connect button action
        connectButton.setOnAction(e -> {
            username = usernameField.getText().trim();

            if (username.isEmpty()) {
                statusLabel.setText("Please enter a username");
                statusLabel.setStyle("-fx-text-fill: #C73E1D;");
                statusLabel.setVisible(true);
                return;
            }

            // Show connecting status
            statusLabel.setText("Connecting to server...");
            statusLabel.setStyle("-fx-text-fill: #F5F5F5;");
            statusLabel.setVisible(true);

            // Start multiplayer game with the entered username
            MultiplayerManager multiplayerManager = new MultiplayerManager(primaryStage, username);
            multiplayerManager.startMultiplayerGame();
        });

        // Back button action
        backButton.setOnAction(e -> menuManager.returnToMenu());

        // Assemble the interface
        container.getChildren().addAll(title, usernameBox, statusLabel, connectButton, backButton);

        return container;
    }
}