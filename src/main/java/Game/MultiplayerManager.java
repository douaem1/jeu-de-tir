// Ajoutez cette classe pour gérer le multijoueur
package Game;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MultiplayerManager {

    private GameManager gameManager;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIP;
    private int serverPort;
    private boolean isConnected = false;
    private boolean isHost = false;
    private final Map<String, ImageView> remotePlayers = new ConcurrentHashMap<>();
    private ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private List<String> waitingPlayers = new ArrayList<>();

    public MultiplayerManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void showMultiplayerDialog() {
        Platform.runLater(() -> {
            // Créer la boîte de dialogue
            VBox dialogPane = new VBox(15);
            dialogPane.setStyle("-fx-background-color: rgba(30, 30, 60, 0.9); -fx-padding: 20px; -fx-border-radius: 15px; -fx-background-radius: 15px;");
            dialogPane.setAlignment(Pos.CENTER);
            dialogPane.setMaxWidth(400);
            dialogPane.setPrefHeight(300);

            Label titleLabel = new Label("MODE MULTIJOUEUR");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

            // Champs pour le mode hôte
            Label hostLabel = new Label("CRÉER UNE PARTIE");
            hostLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

            TextField portField = new TextField("12345");
            portField.setPromptText("Port (ex: 12345)");
            portField.setMaxWidth(300);

            Button hostButton = new Button("HÉBERGER");
            hostButton.setStyle("-fx-background-color: #2E86AB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            hostButton.setPrefWidth(150);

            // Champs pour rejoindre une partie
            Label joinLabel = new Label("REJOINDRE UNE PARTIE");
            joinLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

            HBox joinInputs = new HBox(10);
            joinInputs.setAlignment(Pos.CENTER);

            TextField ipField = new TextField("localhost");
            ipField.setPromptText("IP du serveur");
            ipField.setPrefWidth(200);

            TextField joinPortField = new TextField("12345");
            joinPortField.setPromptText("Port");
            joinPortField.setPrefWidth(100);

            joinInputs.getChildren().addAll(ipField, joinPortField);

            Button joinButton = new Button("REJOINDRE");
            joinButton.setStyle("-fx-background-color: #F18F01; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            joinButton.setPrefWidth(150);

            Button backButton = new Button("RETOUR");
            backButton.setStyle("-fx-background-color: #A23B72; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            backButton.setPrefWidth(150);

            // Ajouter les actions
            hostButton.setOnAction(e -> {
                try {
                    int port = Integer.parseInt(portField.getText());
                    hostGame(port);
                    gameManager.gamepane.getChildren().remove(dialogPane);
                } catch (NumberFormatException ex) {
                    showAlert("Erreur", "Port invalide");
                }
            });

            joinButton.setOnAction(e -> {
                try {
                    String ip = ipField.getText();
                    int port = Integer.parseInt(joinPortField.getText());
                    joinGame(ip, port);
                    gameManager.gamepane.getChildren().remove(dialogPane);
                } catch (NumberFormatException ex) {
                    showAlert("Erreur", "Port invalide");
                }
            });

            backButton.setOnAction(e -> {
                gameManager.gamepane.getChildren().remove(dialogPane);
            });

            // Assembler la boîte de dialogue
            dialogPane.getChildren().addAll(
                    titleLabel,
                    hostLabel,
                    portField,
                    hostButton,
                    joinLabel,
                    joinInputs,
                    joinButton,
                    backButton
            );

            // Centrer la boîte de dialogue
            dialogPane.setLayoutX((GameManager.WINDOW_WIDTH - 400) / 2);
            dialogPane.setLayoutY((GameManager.WINDOW_HEIGHT - 300) / 2);

            // Ajouter au gamePane
            if (gameManager.gamepane != null) {
                gameManager.gamepane.getChildren().add(dialogPane);
                dialogPane.requestFocus();
            }
        });
    }

    private void hostGame(int port) {
        try {
            isHost = true;
            MultiplayerServer server = new MultiplayerServer(port, gameManager);
            server.start();

            // Se connecter aussi en tant que client au serveur local
            joinGame("localhost", port);

            showWaitingRoomDialog();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer le serveur: " + e.getMessage());
        }
    }

    private void joinGame(String ip, int port) {
        try {
            this.serverIP = ip;
            this.serverPort = port;

            // Se connecter au serveur
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Envoyer le nom d'utilisateur et le type d'avion
            out = new PrintWriter(socket.getOutputStream(), true);
            gameManager.out = out;
            out.println("JOIN:" + gameManager.currentUsername + "," + gameManager.selectedAircraft);

            // Démarrer le thread d'écoute
            listenForServerMessages();

            isConnected = true;
            gameManager.isMultiplayerMode = true;

            if (!isHost) {
                showWaitingRoomDialog();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de rejoindre le serveur: " + e.getMessage());
        }
    }

    private void showWaitingRoomDialog() {
        Platform.runLater(() -> {
            VBox waitingRoom = new VBox(15);
            waitingRoom.setId("waiting-room");
            waitingRoom.setStyle("-fx-background-color: rgba(30, 30, 60, 0.9); -fx-padding: 20px; -fx-border-radius: 15px; -fx-background-radius: 15px;");
            waitingRoom.setAlignment(Pos.CENTER);
            waitingRoom.setMaxWidth(400);
            waitingRoom.setPrefHeight(400);

            Label titleLabel = new Label("SALLE D'ATTENTE");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

            Label infoLabel = new Label("En attente d'autres joueurs...");
            infoLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

            VBox playersBox = new VBox(5);
            playersBox.setAlignment(Pos.CENTER);
            playersBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-padding: 10px;");
            playersBox.setPrefHeight(200);

            // Ajouter le joueur courant
            Label currentPlayerLabel = new Label("• " + gameManager.currentUsername + " (Vous)");
            currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #A0FFA0;");
            playersBox.getChildren().add(currentPlayerLabel);

            // Ajouter les joueurs en attente
            for (String player : waitingPlayers) {
                Label playerLabel = new Label("• " + player);
                playerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
                playersBox.getChildren().add(playerLabel);
            }

            Button startButton = new Button("DÉMARRER LA PARTIE");
            startButton.setStyle("-fx-background-color: #2E86AB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            startButton.setPrefWidth(200);
            startButton.setDisable(!isHost || waitingPlayers.size() < 1);

            Button leaveButton = new Button("QUITTER");
            leaveButton.setStyle("-fx-background-color: #C73E1D; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            leaveButton.setPrefWidth(150);

            waitingRoom.getChildren().addAll(
                    titleLabel,
                    infoLabel,
                    playersBox,
                    startButton,
                    leaveButton
            );

            // Centrer la boîte de dialogue
            waitingRoom.setLayoutX((GameManager.WINDOW_WIDTH - 400) / 2);
            waitingRoom.setLayoutY((GameManager.WINDOW_HEIGHT - 400) / 2);

            // Actions des boutons
            startButton.setOnAction(e -> {
                if (isHost && waitingPlayers.size() >= 1) {
                    out.println("START_GAME");
                    gameManager.gamepane.getChildren().remove(waitingRoom);
                }
            });

            leaveButton.setOnAction(e -> {
                disconnect();
                gameManager.gamepane.getChildren().remove(waitingRoom);
                gameManager.setupMainMenu();
            });

            // Mise à jour de la liste des joueurs
            Runnable updatePlayersList = () -> {
                Platform.runLater(() -> {
                    playersBox.getChildren().clear();

                    // Ajouter le joueur courant
                    Label selfLabel = new Label("• " + gameManager.currentUsername + " (Vous)");
                    selfLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #A0FFA0;");
                    playersBox.getChildren().add(selfLabel);

                    // Ajouter les autres joueurs
                    for (String player : waitingPlayers) {
                        Label playerLabel = new Label("• " + player);
                        playerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
                        playersBox.getChildren().add(playerLabel);
                    }

                    // Activer le bouton si assez de joueurs
                    startButton.setDisable(!isHost || waitingPlayers.size() < 1);

                    // Mettre à jour le message d'attente
                    if (waitingPlayers.size() < 1) {
                        infoLabel.setText("En attente d'au moins un autre joueur...");
                    } else {
                        infoLabel.setText("Prêt à démarrer la partie !");
                    }
                });
            };

            // Ajouter au gamePane
            if (gameManager.gamepane != null) {
                gameManager.gamepane.getChildren().add(waitingRoom);
                waitingRoom.requestFocus();
            }
        });
    }

    private void updateWaitingRoom() {
        Platform.runLater(() -> {
            VBox waitingRoom = (VBox) gameManager.gamepane.lookup("#waiting-room");
            if (waitingRoom != null) {
                VBox playersBox = (VBox) waitingRoom.getChildren().get(2);
                playersBox.getChildren().clear();

                // Ajouter le joueur courant
                Label currentPlayerLabel = new Label("• " + gameManager.currentUsername + " (Vous)");
                currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #A0FFA0;");
                playersBox.getChildren().add(currentPlayerLabel);

                // Ajouter les joueurs en attente
                for (String player : waitingPlayers) {
                    Label playerLabel = new Label("• " + player);
                    playerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
                    playersBox.getChildren().add(playerLabel);
                }

                // Mettre à jour le message d'attente
                Label infoLabel = (Label) waitingRoom.getChildren().get(1);
                if (waitingPlayers.size() < 1) {
                    infoLabel.setText("En attente d'au moins un autre joueur...");
                } else {
                    infoLabel.setText("Prêt à démarrer la partie !");
                }

                // Activer le bouton si assez de joueurs
                Button startButton = (Button) waitingRoom.getChildren().get(3);
                startButton.setDisable(!isHost || waitingPlayers.size() < 1);
            }
        });
    }

    private void listenForServerMessages() {
        networkExecutor.submit(() -> {
            try {
                String message;
                while (isConnected && (message = in.readLine()) != null) {
                    processServerMessage(message);
                }
            } catch (Exception e) {
                System.err.println("Erreur de communication avec le serveur: " + e.getMessage());
                if (isConnected) {
                    disconnect();
                }
            }
        });
    }

    private void processServerMessage(String message) {
        System.out.println("Message reçu: " + message);

        if (message.startsWith("PLAYER_LIST:")) {
            // Format: PLAYER_LIST:player1,player2,player3
            String[] parts = message.split(":", 2);
            if (parts.length > 1) {
                String[] players = parts[1].split(",");
                waitingPlayers.clear();
                for (String player : players) {
                    if (!player.equals(gameManager.currentUsername)) {
                        waitingPlayers.add(player);
                    }
                }
                updateWaitingRoom();
            }
        } else if (message.startsWith("PLAYER_JOIN:")) {
            // Format: PLAYER_JOIN:username,aircraftType
            String[] parts = message.split(":", 2)[1].split(",");
            String username = parts[0];
            String aircraftType = parts[1];
            if (!username.equals(gameManager.currentUsername)) {
                if (!waitingPlayers.contains(username)) {
                    waitingPlayers.add(username);
                }
                updateWaitingRoom();
            }
        } else if (message.startsWith("PLAYER_LEAVE:")) {
            // Format: PLAYER_LEAVE:username
            String username = message.split(":", 2)[1];
            waitingPlayers.remove(username);
            updateWaitingRoom();

            // Supprimer l'avion du joueur qui part
            Platform.runLater(() -> {
                ImageView playerView = remotePlayers.get(username);
                if (playerView != null && gameManager.gamepane != null) {
                    gameManager.gamepane.getChildren().remove(playerView);
                    remotePlayers.remove(username);
                }
            });
        } else if (message.equals("START_GAME")) {
            // Le serveur a démarré la partie
            Platform.runLater(() -> {
                VBox waitingRoom = (VBox) gameManager.gamepane.lookup("#waiting-room");
                if (waitingRoom != null) {
                    gameManager.gamepane.getChildren().remove(waitingRoom);
                }

                // Si la partie n'est pas déjà en cours, la démarrer
                if (!gameManager.gameRunning) {
                    gameManager.startGame(gameManager.selectedAircraft);
                }

                // Ajouter les avions des autres joueurs
                for (String player : waitingPlayers) {
                    addRemotePlayer(player, 0, 0);
                }
            });
        } else if (message.startsWith("PLAYER_POS:")) {
            // Format: PLAYER_POS:username,x,y
            String[] parts = message.split(":", 2)[1].split(",");
            String username = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);

            updateRemotePlayerPosition(username, x, y);
        } else if (message.startsWith("PLAYER_FIRE:")) {
            // Format: PLAYER_FIRE:username,x,y
            String[] parts = message.split(":", 2)[1].split(",");
            String username = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);

            createRemotePlayerLaser(username, x, y);
        } else if (message.startsWith("SYNC_ENEMY:")) {
            // Format: SYNC_ENEMY:enemyId,x,y
            String[] parts = message.split(":", 2)[1].split(",");
            String enemyId = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);

            gameManager.addNetworkEnemy(enemyId, x, y);
        }
    }

    private void addRemotePlayer(String username, double x, double y) {
        Platform.runLater(() -> {
            if (gameManager.gamepane != null && !username.equals(gameManager.currentUsername)) {
                // Créer l'avion pour le joueur distant
                Player playerClass = new Player(gameManager);
                ImageView remotePlayerView = playerClass.createNetworkPlayer(username);
                remotePlayerView.setX(x);
                remotePlayerView.setY(y);
                remotePlayerView.setId("player-" + username);

                // Ajouter l'avion à la scène
                gameManager.gamepane.getChildren().add(remotePlayerView);
                remotePlayers.put(username, remotePlayerView);
            }
        });
    }

    private void updateRemotePlayerPosition(String username, double x, double y) {
        Platform.runLater(() -> {
            ImageView playerView = remotePlayers.get(username);
            if (playerView == null) {
                // Si le joueur n'existe pas encore, l'ajouter
                addRemotePlayer(username, x, y);
            } else {
                // Sinon, mettre à jour sa position
                playerView.setX(x);
                playerView.setY(y);
            }
        });
    }

    private void createRemotePlayerLaser(String username, double x, double y) {
        Platform.runLater(() -> {
            if (gameManager.gamepane != null) {
                Player playerClass = new Player(gameManager);
                playerClass.createRemotePlayerLaser(gameManager.gamepane, x, y);
            }
        });
    }

    public void disconnect() {
        try {
            isConnected = false;

            if (out != null) {
                out.println("LEAVE:" + gameManager.currentUsername);
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (in != null) {
                in.close();
            }

            if (out != null) {
                out.close();
            }

            networkExecutor.shutdownNow();

            gameManager.isMultiplayerMode = false;
            gameManager.out = null;

        } catch (IOException e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}