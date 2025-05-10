package Menu;

import Game.GameConstants;
import Game.GameManager;
import Game.MultiplayerManager;
import chat_Client_Serveur.NetworkManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import design.animation;

import java.util.Map;
import java.util.Optional;

public class MultiplayerDialog {
    private Stage primaryStage;
    private GameManager gameManager;
    private Stage dialog;
    private final NetworkManager networkManager;

    // UI Elements
    private Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),
            "DARK", Color.web("#1A1A2E")
    );

    public MultiplayerDialog(Stage primaryStage, GameManager gameManager) {
        this.primaryStage = primaryStage;
        this.gameManager = gameManager;
        this.networkManager = new NetworkManager();
    }

    public void show() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);

        animation animator = new animation();

        VBox dialogContent = new VBox(20);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setPadding(new Insets(30));
        dialogContent.setStyle("-fx-background-color: rgba(10, 10, 30, 0.9); -fx-background-radius: 15;");

        Label title = new Label("MULTIPLAYER SETUP");
        title.setFont(Font.font("Agency FB", FontWeight.BOLD, 32));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
        glow.setSpread(0.3);
        title.setEffect(glow);

        // Options
        Button hostButton = animator.createActionButton("HOST GAME", "PRIMARY");
        hostButton.setPrefWidth(200);

        Button joinButton = animator.createActionButton("JOIN GAME", "SECONDARY");
        joinButton.setPrefWidth(200);

        Button cancelButton = animator.createActionButton("CANCEL", "DARK");
        cancelButton.setPrefWidth(200);

        // Add actions
        hostButton.setOnAction(e -> {
            animator.playButtonPressAnimation(hostButton);
            hostGame();
        });

        joinButton.setOnAction(e -> {
            animator.playButtonPressAnimation(joinButton);
            joinGame();
        });

        cancelButton.setOnAction(e -> {
            animator.playButtonPressAnimation(cancelButton);
            dialog.close();
        });

        dialogContent.getChildren().addAll(title, hostButton, joinButton, cancelButton);

        Scene dialogScene = new Scene(dialogContent, 400, 300);
        dialog.setScene(dialogScene);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialogScene.setFill(Color.TRANSPARENT);

        dialog.showAndWait();
    }

    private void hostGame() {
        // Create a dialog to select aircraft first
        PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);
        selectionInterface.showSelectionInterface();
        
        // Start local server
        MultiplayerManager multiplayerManager = new MultiplayerManager(primaryStage, "Host");
        multiplayerManager.startLocalServer();
        multiplayerManager.setAsHost(null); // Pass null as we'll create the server socket in startLocalServer
        
        // Close the multiplayer dialog
        dialog.close();
    }

    private void joinGame() {
        // Create input dialog for IP
        TextInputDialog ipDialog = new TextInputDialog();
        ipDialog.setTitle("Join Game");
        ipDialog.setHeaderText("Enter host's Radmin VPN IP address");
        ipDialog.setContentText("IP Address:");

        // Style the dialog
        DialogPane dialogPane = ipDialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: rgba(10, 10, 30, 0.9); -fx-background-radius: 15;");
        dialogPane.getStyleClass().add("custom-dialog");

        // Get IP and connect
        Optional<String> result = ipDialog.showAndWait();
        if (result.isPresent() && !result.get().isEmpty()) {
            String hostIP = result.get();

        }
    }
}