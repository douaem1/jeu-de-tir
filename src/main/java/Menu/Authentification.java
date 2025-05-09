package Menu;
import Game.GameManager;
import Game.MultiplayerManager;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import DAO.users;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import design.design;
import design.animation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class handles the user authentication process, including sign-in and sign-up functionalities.
 * It provides a graphical user interface for users to input their credentials and interact with the application.
 */

public class Authentification {

    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int MULTIPLAYER_PORT = 5555;
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
    private Stage primaryStage;
    private GameManager gameManager;
    private String currentUsername;
    private ServerSocket serverSocket;
    private List<String> connectedPlayers = new ArrayList<>();
    private AtomicBoolean isServer = new AtomicBoolean(false);
    private AtomicBoolean isClient = new AtomicBoolean(false);
    private Socket clientSocket;
    private List<Socket> clientSockets = new ArrayList<>();
    private List<PrintWriter> clientWriters = new ArrayList<>();
    private PrintWriter clientOut;
    private BufferedReader clientIn;

    /**
     * Constructor for the Authentication class
     * @param primaryStage The main application window
     */
    public Authentification(Stage primaryStage) {
        if (primaryStage == null) {
            throw new IllegalArgumentException("primaryStage cannot be null");
        }
        this.primaryStage = primaryStage;
        this.gameManager = new GameManager();
    }

    /**
     * Displays the sign-in interface
     */
    public void showSignInScene() {
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));
        loginBox.setMaxWidth(500);

        loginBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");
        design design = new design();
        animation animation = new animation();
        StackPane root = new StackPane();
        ImageView background = design.loadBestBackground();
        design.setupBackgroundImage(background);
        design.animateBackground(background);
        root.getChildren().add(background);
        Scene loginScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(loginScene);
        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        Label title = new Label("SIGN IN");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animation.animateTextGlow(title, glow);

        TextField usernameField = animation.createStylizedTextField("Username");
        PasswordField passwordField = animation.createStylizedPasswordField("Password");

        Button loginBtn = animation.createActionButton("SIGN IN", "PRIMARY");
        loginBtn.setPrefWidth(200);

        // Multiplayer buttons with distinctive styling
        Button hostGameBtn = animation.createActionButton("HOST MULTIPLAYER", "ACCENT");
        hostGameBtn.setPrefWidth(200);

        Button joinGameBtn = animation.createActionButton("JOIN MULTIPLAYER", "SECONDARY");
        joinGameBtn.setPrefWidth(200);

        Button backBtn = animation.createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        // Correction for the return button
        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);
            MenuManager menuManager = new MenuManager(primaryStage);
            menuManager.returnToMenu();
        });

        loginBox.getChildren().addAll(title, usernameField, passwordField, loginBtn, hostGameBtn, joinGameBtn, backBtn);

        loginBox.setOpacity(0);
        loginBox.setTranslateY(20);
        root.getChildren().add(loginBox);

        animation.animateFormEntrance(loginBox);

        loginBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(loginBtn);
            String username = usernameField.getText();
            String password = passwordField.getText();
            users user = new users();
            boolean isValid = user.verifyUser(username, password);
            MenuManager menuManager = new MenuManager(primaryStage);
            if (isValid) {
                currentUsername = username; // Store the username
                menuManager.showNotification("Sign in successful!");

                // We still show the player selection interface for single player mode
                PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);
                selectionInterface.showSelectionInterface();
            } else {
                menuManager.showNotification("Incorrect username or password or non-existent account (Sign up first)");
            }
        });

        // Action for host multiplayer button
        hostGameBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(hostGameBtn);

            // Get username from the field, even if not logged in
            String username = usernameField.getText();
            // Use "Host" as default if no username entered
            if (username == null || username.trim().isEmpty()) {
                username = "Host";
            }

            MenuManager menuManager = new MenuManager(primaryStage);
            menuManager.showNotification("Creating multiplayer lobby...");

            // Show multiplayer lobby with host capabilities
            showMultiplayerLobby(username, true);
        });

        // Action for join multiplayer button
        joinGameBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(joinGameBtn);

            // Get username from the field, even if not logged in
            String username = usernameField.getText();
            // Use "Player" as default if no username entered
            if (username == null || username.trim().isEmpty()) {
                username = "Player";
            }

            MenuManager menuManager = new MenuManager(primaryStage);

            // Show join game dialog
            showJoinGameDialog(username);
        });
    }

    /**
     * Shows dialog for entering IP address to join a game
     * @param username The player's username
     */
    private void showJoinGameDialog(String username) {
        VBox dialogBox = new VBox(20);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setPadding(new Insets(40));
        dialogBox.setMaxWidth(400);
        dialogBox.setMinHeight(300);
        dialogBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.9); -fx-background-radius: 15;");

        animation animation = new animation();

        Label title = new Label("JOIN GAME");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 30));
        title.setTextFill(COLORS.get("LIGHT"));

        Label ipLabel = new Label("Enter Host IP Address:");
        ipLabel.setFont(Font.font(FONT_FAMILIES[1], 14));
        ipLabel.setTextFill(COLORS.get("LIGHT"));

        TextField ipField = animation.createStylizedTextField("127.0.0.1");
        ipField.setMaxWidth(300);

        Button connectBtn = animation.createActionButton("CONNECT", "SECONDARY");
        connectBtn.setPrefWidth(150);

        Button cancelBtn = animation.createActionButton("CANCEL", "DARK");
        cancelBtn.setPrefWidth(150);

        dialogBox.getChildren().addAll(title, ipLabel, ipField, connectBtn, cancelBtn);

        StackPane dialogRoot = new StackPane();
        Rectangle dimOverlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        dimOverlay.setFill(Color.rgb(0, 0, 0, 0.7));
        dialogRoot.getChildren().addAll(dimOverlay, dialogBox);

        Scene currentScene = primaryStage.getScene();
        StackPane currentRoot = (StackPane) currentScene.getRoot();
        currentRoot.getChildren().add(dialogRoot);

        // Set up button actions
        connectBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(connectBtn);
            String ipAddress = ipField.getText().trim();

            if (ipAddress.isEmpty()) {
                ipAddress = "127.0.0.1"; // Default to localhost
            }

            // Remove dialog
            currentRoot.getChildren().remove(dialogRoot);

            // Try to connect to the server
            connectToServer(ipAddress, username);
        });

        cancelBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(cancelBtn);
            currentRoot.getChildren().remove(dialogRoot);
        });
    }

    /**
     * Connects to a multiplayer server
     * @param ipAddress The server's IP address
     * @param username The player's username
     */
    private void connectToServer(String ipAddress, String username) {
        MenuManager menuManager = new MenuManager(primaryStage);

        new Thread(() -> {
            try {
                System.out.println("Attempting to connect to host at " + ipAddress + ":" + MULTIPLAYER_PORT);
                clientSocket = new Socket(ipAddress, MULTIPLAYER_PORT);
                isClient.set(true);

                // Store communication streams
                clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Send join message to server
                clientOut.println("CONNECT:" + username);

                // Wait for acknowledgment from server
                String response = clientIn.readLine();

                if (response != null && response.startsWith("WELCOME")) {
                    Platform.runLater(() -> {
                        menuManager.showNotification("Connected to server!");
                        showMultiplayerLobby(username, false);
                    });

                    // Start a listener thread for server messages
                    startServerListener(username);
                } else {
                    throw new IOException("Unexpected server response: " + response);
                }
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    menuManager.showNotification("Failed to connect: " + ex.getMessage());
                });

                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                    clientSocket = null;
                }
            }
        }).start();
    }

    /**
     * Shows the multiplayer game lobby where players can connect and wait for opponents
     * @param username The current player's username
     * @param isHost Whether this player is hosting the game
     */
    private void showMultiplayerLobby(String username, boolean isHost) {
        VBox lobbyBox = new VBox(20);
        lobbyBox.setAlignment(Pos.CENTER);
        lobbyBox.setPadding(new Insets(40));
        lobbyBox.setMaxWidth(600);
        lobbyBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.8); -fx-background-radius: 15;");

        design design = new design();
        animation animation = new animation();
        StackPane root = new StackPane();

        // Set up background
        ImageView background = design.loadBestBackground();
        design.setupBackgroundImage(background);
        design.animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        // Lobby title
        String titleText = isHost ? "HOST MULTIPLAYER LOBBY" : "MULTIPLAYER LOBBY";
        Label title = new Label(titleText);
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 42));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("ACCENT"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animation.animateTextGlow(title, glow);

        // Welcome message
        Label welcomeLabel = new Label("Welcome, " + username + "!");
        welcomeLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 20));
        welcomeLabel.setTextFill(COLORS.get("LIGHT"));

        // Status indicator
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        Circle statusIndicator = new Circle(8);
        statusIndicator.setFill(Color.YELLOW);
        Label statusLabel = new Label(isHost ? "Waiting for players to join..." : "Waiting for host to start game...");
        statusLabel.setTextFill(COLORS.get("LIGHT"));
        statusLabel.setFont(Font.font(FONT_FAMILIES[1], 16));
        statusBox.getChildren().addAll(statusIndicator, statusLabel);

        // Server information for host
        VBox serverInfoBox = new VBox(10);
        serverInfoBox.setAlignment(Pos.CENTER);

        if (isHost) {
            try {
                // Start server socket for hosting
                serverSocket = new ServerSocket(MULTIPLAYER_PORT);
                isServer.set(true);

                String localIp = InetAddress.getLocalHost().getHostAddress();
                Label ipLabel = new Label("Your IP Address: " + localIp);
                ipLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 16));
                ipLabel.setTextFill(COLORS.get("SECONDARY"));

                Label portLabel = new Label("Port: " + MULTIPLAYER_PORT);
                portLabel.setFont(Font.font(FONT_FAMILIES[1], 14));
                portLabel.setTextFill(COLORS.get("LIGHT"));

                serverInfoBox.getChildren().addAll(ipLabel, portLabel);

                // Start listening for connections in a background thread
                startListeningForConnections(statusIndicator, statusLabel);

            } catch (IOException e) {
                Label errorLabel = new Label("Failed to create server: " + e.getMessage());
                errorLabel.setTextFill(COLORS.get("DANGER"));
                serverInfoBox.getChildren().add(errorLabel);
            }
        }

        // Player list
        VBox playerListBox = new VBox(10);
        playerListBox.setPadding(new Insets(15));
        playerListBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");
        playerListBox.setMaxHeight(200);
        playerListBox.setMinHeight(200);

        Label playersHeader = new Label("CONNECTED PLAYERS");
        playersHeader.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 18));
        playersHeader.setTextFill(COLORS.get("LIGHT"));

        ListView<String> playerListView = new ListView<>();
        playerListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: rgba(0, 0, 0, 0.5);");
        playerListView.getItems().add(username + " (You)");
        connectedPlayers.add(username);

        if (!isHost && isClient.get()) {
            // If we're a client who just connected to a host
            playerListView.getItems().add("Host (Waiting...)");
        }

        playerListView.setPrefHeight(150);
        playerListBox.getChildren().addAll(playersHeader, playerListView);

        // Action buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button startGameBtn = animation.createActionButton("START GAME", "SECONDARY");
        startGameBtn.setPrefWidth(150);
        // Only the host can start the game
        startGameBtn.setDisable(false);
// Change text for client
        if (!isHost) {
            startGameBtn.setText("READY TO PLAY");
        }



        Button selectAircraftBtn = animation.createActionButton("SELECT AIRCRAFT", "PRIMARY");
        selectAircraftBtn.setPrefWidth(150);

        Button backBtn = animation.createActionButton("BACK", "DARK");
        backBtn.setPrefWidth(150);

        buttonBox.getChildren().addAll(startGameBtn, selectAircraftBtn, backBtn);

        // Player count indicator
        Label playerCountLabel = new Label("Players: 1/2");
        playerCountLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 16));
        playerCountLabel.setTextFill(COLORS.get("LIGHT"));

        // Add all elements to lobby container
        lobbyBox.getChildren().addAll(title, welcomeLabel);

        if (isHost) {
            lobbyBox.getChildren().add(serverInfoBox);
        }

        lobbyBox.getChildren().addAll(statusBox, playerCountLabel, playerListBox, buttonBox);

        // Initial animation
        lobbyBox.setOpacity(0);
        lobbyBox.setTranslateY(20);
        root.getChildren().add(lobbyBox);

        Scene lobbyScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(lobbyScene);

        animation.animateFormEntrance(lobbyBox);

        // Button actions
        // Fix for the startGameBtn action in Authentification.java (showMultiplayerLobby method)
        startGameBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(startGameBtn);

            if (isHost) {
                // Host functionality: Start the server-side game and notify clients
                MultiplayerManager multiplayerManager = new MultiplayerManager(primaryStage, username);
                multiplayerManager.setAsHost(serverSocket);

                // Notify all clients that game is starting - ensure clean message delivery
                for (PrintWriter writer : clientWriters) {
                    if (writer != null) {
                        writer.println("START_GAME");
                        writer.flush(); // Ensure immediate sending
                        System.out.println("START_GAME message sent to client");
                    }
                }

                // Add a small delay to ensure messages are sent before starting the game
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                // Start the game for host
                multiplayerManager.startMultiplayerGame();
            } else {
                // Client functionality: Show that client is ready
                startGameBtn.setDisable(true);
                startGameBtn.setText("WAITING FOR HOST...");
                statusLabel.setText("Ready! Waiting for host to start the game.");
                statusIndicator.setFill(Color.GREEN);

                // Notify host that client is ready
                if (clientSocket != null && clientOut != null) {
                    clientOut.println("CLIENT_READY:" + username);
                    System.out.println("Sent CLIENT_READY:" + username);
                    // Flush to ensure message is sent
                    clientOut.flush();
                }
            }
        });
        selectAircraftBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(selectAircraftBtn);

            // Clean up resources when navigating away
            cleanupNetworkResources();

            PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);
            selectionInterface.showSelectionInterface();
        });

        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);

            // Clean up resources when going back
            cleanupNetworkResources();

            showSignInScene();
        });

        // Make sure resources are cleaned up when window is closed
        primaryStage.setOnCloseRequest(windowEvent -> {
            cleanupNetworkResources();
        });
    }
    private void notifyClientsGameStarting(List<PrintWriter> clientWriters) {
        try {
            if (isServer.get() && !clientWriters.isEmpty()) {
                System.out.println("Notifying " + clientWriters.size() + " clients that game is starting...");

                for (PrintWriter writer : clientWriters) {
                    if (writer != null) {
                        writer.println("START_GAME");
                        writer.flush(); // Make sure the message is sent immediately
                        System.out.println("START_GAME message sent to client");
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error notifying clients: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Starts listening for client connections in a background thread
     * @param statusIndicator The UI indicator showing connection status
     * @param statusLabel The UI label showing connection status text
     */
    private void startListeningForConnections(Circle statusIndicator, Label statusLabel) {
        if (serverSocket == null || !isServer.get()) {
            return;
        }

        // Start a background thread to listen for connections
        new Thread(() -> {
            try {
                // Update UI to show we're listening
                Platform.runLater(() -> {
                    statusIndicator.setFill(Color.YELLOW);
                    statusLabel.setText("Waiting for opponent to connect...");
                });

                System.out.println("Server listening on port " + MULTIPLAYER_PORT);

                // Keep accepting connections until server is stopped
                while (!serverSocket.isClosed()) {
                    try {
                        // Wait for a client to connect (blocks until connection)
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Client connected from: " + clientSocket.getInetAddress());

                        // Set up communication channels
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                        // Add to our collections
                        clientSockets.add(clientSocket);
                        clientWriters.add(out);

                        // Wait for client identification
                        String clientMessage = in.readLine();
                        String clientUsername = "Unknown";

                        if (clientMessage != null && clientMessage.startsWith("CONNECT:")) {
                            clientUsername = clientMessage.substring(8);
                            System.out.println("Client identified as: " + clientUsername);

                            // Send welcome message
                            out.println("WELCOME:" + clientUsername);
                        }

                        // Store final username for UI updates
                        final String finalUsername = clientUsername;

                        // Update UI when client connects
                        Platform.runLater(() -> {
                            statusIndicator.setFill(Color.GREEN);
                            statusLabel.setText("Player connected! Ready to start");

                            // Update player list
                            ListView<String> playerList = findPlayerListView();
                            if (playerList != null) {
                                playerList.getItems().add(finalUsername);
                                connectedPlayers.add(finalUsername);
                            }

                            // Update player count
                            findPlayerCountLabel().setText("Players: " + (connectedPlayers.size()) + "/2");
                        });

                        // Start a new thread to listen for messages from this client
                        startClientListener(clientSocket, in, finalUsername);
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.err.println("Error accepting client: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Server error: " + e.getMessage());
                    Platform.runLater(() -> {
                        statusIndicator.setFill(Color.RED);
                        statusLabel.setText("Connection error: " + e.getMessage());
                    });
                }
            }
        }).start();
    }
    private void startClientListener(Socket clientSocket, BufferedReader in, String username) {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Message from " + username + ": " + message);

                    // Handle client messages
                    if (message.startsWith("CLIENT_READY:")) {
                        final String readyPlayer = message.substring(13);
                        Platform.runLater(() -> {
                            // Update UI to show player is ready
                            ListView<String> playerList = findPlayerListView();
                            if (playerList != null) {
                                for (int i = 0; i < playerList.getItems().size(); i++) {
                                    if (playerList.getItems().get(i).startsWith(readyPlayer)) {
                                        playerList.getItems().set(i, readyPlayer + " (Ready)");
                                        break;
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading from client " + username + ": " + e.getMessage());
            }
        }).start();
    }
    /**
     * Utility method to find the player list view in the current scene
     * @return The ListView for player names
     */
    private ListView<String> findPlayerListView() {
        Scene currentScene = primaryStage.getScene();
        if (currentScene == null) return null;

        // Find the ListView in the scene graph
        for (Node node : currentScene.getRoot().lookupAll(".list-view")) {
            if (node instanceof ListView) {
                return (ListView<String>) node;
            }
        }
        return null;
    }

    /**
     * Utility method to find the player count label in the current scene
     * @return The Label showing player count
     */
    private Label findPlayerCountLabel() {
        Scene currentScene = primaryStage.getScene();
        if (currentScene == null) return null;

        // Find all labels in the scene
        for (Node node : currentScene.getRoot().lookupAll(".label")) {
            if (node instanceof Label && ((Label) node).getText().startsWith("Players:")) {
                return (Label) node;
            }
        }

        // Default label if not found
        return new Label("Players: 1/2");
    }

    /**
     * Cleans up network resources when leaving the multiplayer screens
     */
    private void cleanupNetworkResources() {
        if (isServer.get() && serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                System.err.println("Error closing server socket: " + ex.getMessage());
            }
        }

        if (isClient.get() && clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.err.println("Error closing client socket: " + ex.getMessage());
            }
        }

        // Reset flags
        isServer.set(false);
        isClient.set(false);
    }

    /**
     * Displays the sign-up interface
     */
    public void showSignUpScene() {
        VBox signupBox = new VBox(20);
        signupBox.setAlignment(Pos.CENTER);
        signupBox.setPadding(new Insets(40));
        signupBox.setMaxWidth(500);
        signupBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");
        design design = new design();
        animation animation = new animation();
        StackPane root = new StackPane();
        ImageView background = design.loadBestBackground();
        design.setupBackgroundImage(background);
        design.animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        Label title = new Label("SIGN UP");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("ACCENT"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animation.animateTextGlow(title, glow);

        TextField usernameField = animation.createStylizedTextField("Enter your Username");
        PasswordField passwordField = animation.createStylizedPasswordField("Enter your Password");
        PasswordField passwordField1 = animation.createStylizedPasswordField("Confirm your Password");
        Button signupBtn = animation.createActionButton("SIGN UP", "ACCENT");
        signupBtn.setPrefWidth(200);
        MenuManager menuManager = new MenuManager(primaryStage);

        // Input validation and registration
        signupBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(signupBtn);

            String username = usernameField.getText();
            String password = passwordField.getText();
            String confirmPassword = passwordField1.getText();
            if (username.isEmpty() || confirmPassword.isEmpty() || password.isEmpty()) {
                menuManager.showNotification("Please fill in all fields.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                menuManager.showNotification("Passwords do not match!");
                return;
            }
            users newUser = new users(username, password);
            if (newUser.userExists(username)) {
                menuManager.showNotification("Username already exists. Please choose another one.");
                return;
            } else {
                System.out.println("Registration attempt with: " + username);

                newUser.addUser(newUser);

                menuManager.showNotification("Account created successfully!");
                // Store username for possible multiplayer use
                currentUsername = username;
                PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);
                selectionInterface.showSelectionInterface();
            }
        });

        Button backBtn = animation.createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        // Correction for the return button
        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);
            menuManager.returnToMenu();
        });

        signupBox.getChildren().addAll(title, usernameField, passwordField, passwordField1, signupBtn, backBtn);

        signupBox.setOpacity(0);
        signupBox.setTranslateY(20);
        root.getChildren().add(signupBox);

        Scene signupScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(signupScene);

        animation.animateFormEntrance(signupBox);
    }
    // Fix for the client message listener method in Authentification.java
    private void startServerListener(String username) {
        new Thread(() -> {
            try {
                String message = "";
                System.out.println("Client started listening for server messages");

                while (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        // Check if there's data available to read
                        if (clientIn.ready() || (message = clientIn.readLine()) != null) {
                            if (message == null) {
                                // If ready() was true but readLine() returned null,
                                // try again in the next iteration
                                Thread.sleep(100);
                                continue;
                            }

                            System.out.println("Message from server: " + message);

                            // Handle different message types
                            if (message.equals("START_GAME")) {
                                // When host starts the game, start game for client too
                                Platform.runLater(() -> {
                                    System.out.println("Received START_GAME command from host!");
                                    try {
                                        // Create and configure the multiplayer manager
                                        MultiplayerManager multiplayerManager = new MultiplayerManager(primaryStage, username);
                                        multiplayerManager.setAsClient(clientSocket);

                                        // Start the multiplayer game
                                        multiplayerManager.startMultiplayerGame();
                                    } catch (Exception ex) {
                                        System.err.println("Error starting client game: " + ex.getMessage());
                                        ex.printStackTrace();
                                    }
                                });
                                // Break out of the loop after receiving START_GAME
                                // The game will handle communication from now on
                                break;
                            }
                        } else {
                            // No data available, sleep briefly to avoid CPU hogging
                            Thread.sleep(100);
                        }
                    } catch (InterruptedException iex) {
                        // Thread interrupted, just continue
                        continue;
                    }
                }
            } catch (IOException e) {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    System.err.println("Error reading from server: " + e.getMessage());

                    Platform.runLater(() -> {
                        MenuManager menuManager = new MenuManager(primaryStage);
                        menuManager.showNotification("Lost connection to host!");
                    });
                }
            }
        }).start();
    }
}