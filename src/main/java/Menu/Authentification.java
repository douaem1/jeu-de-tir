package Menu;


import Game.GameManager;
import chat_Client_Serveur.GameClient;
import chat_Client_Serveur.GameServer;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import DAO.users;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import design.design;
import design.animation;
import javafx.scene.Scene;
import javafx.application.Platform;

/*This class handles the user authentication process, including sign-in and sign-up functionalities.
  It provides a graphical user interface for users to input their credentials and interact with the application.
 */

public class Authentification {

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
    private Stage primaryStage;
    private GameManager gameManager;


    public Authentification(Stage primaryStage) {
        if (primaryStage == null) {
            throw new IllegalArgumentException("primaryStage cannot be null");
        }
        this.primaryStage = primaryStage;
        this.gameManager = new GameManager();
    }

    // [showSignInScene method remains unchanged]

    private Map<String, ImageView> playerViews = new HashMap<>(); // Pour stocker les vues des joueurs
    private Map<String, String> playerAircraftTypes = new HashMap<>(); // Pour stocker les types d'avions des joueurs
    private Set<String> connectedPlayers = new HashSet<>();
    /**
     * Démarre l'interface du jeu multijoueur
     * @param gameStage La fenêtre du jeu
     * @param gameClient Le client connecté au serveur
     * @param user L'utilisateur connecté
     */
    private void startMultiplayerGame(Stage gameStage, GameClient gameClient, users user) {
        Platform.runLater(() -> {
            try {
                // Configurer la scène du jeu
                StackPane root = new StackPane();
                design design = new design();

                // Charger et configurer l'arrière-plan
                ImageView background = design.loadBestBackground();
                design.setupBackgroundImage(background);
                root.getChildren().add(background);

                // Ajouter un overlay pour une meilleure visibilité
                Rectangle overlay = design.createOverlay();
                root.getChildren().add(overlay);

                // Créer le conteneur principal du jeu
                BorderPane gameLayout = new BorderPane();

                // Initialiser le gestionnaire de jeu avec la scène
                GameManager gameManager = new GameManager();
                gameManager.setPrimaryStage(gameStage);

                // Configurer le panneau de jeu et l'ajouter au centre
                Pane gamePane = new Pane();
                gamePane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
                gameLayout.setCenter(gamePane);

                // Définir le GamePane comme attribut statique pour pouvoir l'utiliser ailleurs
                GameManager.gamepane = gamePane;

                // Ajouter le chat en bas (si nécessaire)
                //VBox chatBox = gameClient.getChatInterface();

                // Ajouter le layout du jeu à la racine
                root.getChildren().add(gameLayout);

                // Créer la scène
                Scene gameScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
                gameStage.setScene(gameScene);
                gameStage.show();

                // Notifier le serveur que le jeu est prêt
                //gameClient.sendMessage("GAME_READY");

                // Sélectionner un avion par défaut pour le joueur
                String selectedAircraft = "default";

                // Démarrer le jeu immédiatement avec une configuration par défaut
                gameManager.startGame(selectedAircraft);

                // Créer des joueurs et configurer les contrôles
                gameManager.setupHUD();
                gameManager.setupControls();

                // Démarrer les threads de jeu
                gameManager.gameRunning = true;
                gameManager.startGameThreads();

                System.out.println("Jeu multijoueur démarré avec succès!");

            } catch (Exception e) {
                e.printStackTrace();
                MenuManager menuManager = new MenuManager(primaryStage);
                menuManager.showNotification("Error starting game: " + e.getMessage());
            }
        });
    }

    // [showSignUpScene method remains unchanged]


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
        Scene loginScene = new Scene(root, 1200, 800);
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

        // Nouveau bouton pour jouer avec des amis
        Button playWithFriendsBtn = animation.createActionButton("PLAY WITH FRIENDS", "SECONDARY");
        playWithFriendsBtn.setPrefWidth(200);

        Button backBtn = animation.createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        // Correction pour le bouton de retour
        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);
            MenuManager menuManager = new MenuManager(primaryStage);
            menuManager.returnToMenu();
        });

        loginBox.getChildren().addAll(title, usernameField, passwordField, loginBtn, playWithFriendsBtn, backBtn);

        loginBox.setOpacity(0);
        loginBox.setTranslateY(20);
        root.getChildren().add(loginBox);

        animation.animateFormEntrance(loginBox);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            users user = new users();
            boolean isValid = user.verifyUser(username, password);
            MenuManager menuManager = new MenuManager(primaryStage);
            if (isValid) {
                menuManager.showNotification("Sign in successful!");
            } else {
                menuManager.showNotification("Incorrect username or password or inexistant account (Sign up first)");
            }
        });

        // Action pour le bouton Play with Friends
        playWithFriendsBtn.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            users user = new users();
            boolean isValid = user.verifyUser(username, password);
            MenuManager menuManager = new MenuManager(primaryStage);

            if (isValid) {
                menuManager.showNotification("Starting multiplayer mode...");
                showMultiplayerOptions(user);
            } else {
                menuManager.showNotification("Please sign in first to play with friends");
            }
        });
    }


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

        // Récupération Input
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
                System.out.println("Tentative d'inscription avec :" + username + " et " + password);

                newUser.addUser(newUser);

                menuManager.showNotification("Account created successfully!");
            }
        });

        Button backBtn = animation.createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        // Correction pour le bouton de retour
        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);
            // MenuManager menuManager = new MenuManager(primaryStage);
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
    // 1. Modified joinGameServer method
    private void joinGameServer(users user, String serverAddress) {
        Thread clientThread = new Thread(() -> {
            try {
                // Add a timeout to the socket connection
                Socket socket = null;
                try {
                    // Try to connect with a 5-second timeout
                    socket = new Socket();
                    socket.connect(new java.net.InetSocketAddress(serverAddress, 7103), 5000);

                    final Socket finalSocket = socket;
                    Platform.runLater(() -> {
                        // Create new window for multiplayer game
                        Stage gameStage = new Stage();
                        gameStage.setTitle("Jet Fighters Multiplayer - " + user.getUsername());

                        // Initialize game client
                        GameClient gameClient = new GameClient();

                        // Start listening for messages from server
                        //gameClient.listenForMessage();

                        // Start the multiplayer game
                        startMultiplayerGame(gameStage, gameClient, user);
                    });
                } catch (java.net.ConnectException e) {
                    Platform.runLater(() -> {
                        MenuManager menuManager = new MenuManager(primaryStage);
                        menuManager.showNotification("Connection refused: The server may not be running.");
                    });
                    e.printStackTrace();
                } catch (java.net.SocketTimeoutException e) {
                    Platform.runLater(() -> {
                        MenuManager menuManager = new MenuManager(primaryStage);
                        menuManager.showNotification("Connection timed out: Server might be unreachable.");
                    });
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    MenuManager menuManager = new MenuManager(primaryStage);
                    menuManager.showNotification("Error connecting to server: " + e.getMessage());
                });
                e.printStackTrace();
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }

    // 2. Modified startGameServer method
    private void startGameServer(users user) {
        // Launch server in a separate thread
        Thread serverThread = new Thread(() -> {
            try {
                // Try to create server socket with specific port
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(7103);
                    final ServerSocket finalServerSocket = serverSocket;

                    Platform.runLater(() -> {
                        MenuManager menuManager = new MenuManager(primaryStage);
                        menuManager.showNotification("Game server started on port 7103");
                    });

                    GameServer server = new GameServer(finalServerSocket);
                    server.startServer();
                } catch (java.net.BindException e) {
                    Platform.runLater(() -> {
                        MenuManager menuManager = new MenuManager(primaryStage);
                        menuManager.showNotification("Port 7103 is already in use. Server cannot start.");
                    });
                    e.printStackTrace();
                    return;
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    MenuManager menuManager = new MenuManager(primaryStage);
                    menuManager.showNotification("Error starting server: " + e.getMessage());
                });
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait a moment for the server to initialize before connecting
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Connect client host to own server
        joinGameServer(user, "localhost");
    }

    // 3. Modified showMultiplayerOptions method
    private void showMultiplayerOptions(users user) {
        // Create dialog box to choose between creating or joining a game
        VBox optionsBox = new VBox(20);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.setPadding(new Insets(40));
        optionsBox.setMaxWidth(500);
        optionsBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.8); -fx-background-radius: 15;");

        Label title = new Label("MULTIPLAYER OPTIONS");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.BOLD, 36));
        title.setTextFill(COLORS.get("LIGHT"));

        animation animation = new animation();

        Button hostGameBtn = animation.createActionButton("HOST GAME", "PRIMARY");
        hostGameBtn.setPrefWidth(300);

        Button joinGameBtn = animation.createActionButton("JOIN GAME", "SECONDARY");
        joinGameBtn.setPrefWidth(300);

        // Add label to show connection status
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font(FONT_FAMILIES[0], FontWeight.NORMAL, 14));
        statusLabel.setTextFill(COLORS.get("LIGHT"));
        statusLabel.setVisible(false);

        TextField serverAddressField = animation.createStylizedTextField("Server address (default: localhost)");
        serverAddressField.setText("localhost");  // Default value
        serverAddressField.setVisible(false);

        Button backBtn = animation.createActionButton("BACK", "DARK");
        backBtn.setPrefWidth(300);

        optionsBox.getChildren().addAll(title, hostGameBtn, joinGameBtn, serverAddressField, statusLabel, backBtn);

        // Display options
        Stage optionsStage = new Stage();
        optionsStage.setTitle("Multiplayer Options");
        StackPane root = new StackPane();

        design design = new design();
        ImageView background = design.loadBestBackground();
        design.setupBackgroundImage(background);
        root.getChildren().add(background);

        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        root.getChildren().add(optionsBox);

        Scene optionsScene = new Scene(root, 800, 600);
        optionsStage.setScene(optionsScene);
        optionsStage.show();

        // Button actions
        hostGameBtn.setOnAction(e -> {
            statusLabel.setText("Starting server...");
            statusLabel.setVisible(true);
            hostGameBtn.setDisable(true);
            joinGameBtn.setDisable(true);



            // Use a timer to provide feedback while starting server
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(() -> {
                        optionsStage.close();
                        startGameServer(user);
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        joinGameBtn.setOnAction(e -> {
            if (!serverAddressField.isVisible()) {
                serverAddressField.setVisible(true);
                statusLabel.setVisible(true);
                statusLabel.setText("Enter server address and click JOIN GAME again");
            } else {
                String serverAddress = serverAddressField.getText().trim();
                if (serverAddress.isEmpty()) {
                    serverAddress = "localhost";
                }
                statusLabel.setText("Connecting to " + serverAddress + "...");
                final String finalServerAddress = serverAddress;

                joinGameBtn.setDisable(true);

                // Use a timer to provide feedback while connecting
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        Platform.runLater(() -> {
                            optionsStage.close();
                            joinGameServer(user, finalServerAddress);
                        });
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        backBtn.setOnAction(e -> {
            optionsStage.close();
        });
    }

    // 4. New method: Check if a server is running locally
    private boolean isLocalServerRunning() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", 7103), 300);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 5. New utility method to get local IP address
    private String getLocalIPAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
    }
}
