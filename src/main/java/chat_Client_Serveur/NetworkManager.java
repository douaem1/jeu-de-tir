package chat_Client_Serveur;

import Game.GameConstants;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.*;
import java.net.*;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean isHost;
    private boolean isConnected = false;

    // Radmin VPN uses the local network adapter for connections
    // Each computer on Radmin VPN gets a local IP in the virtual network

    public boolean connect(String radminIP) {
        try {
            if (radminIP == null || radminIP.isEmpty() || radminIP.equals("host")) {
                // Mode hôte - crée un serveur et attend une connexion
                isHost = true;
                ServerSocket serverSocket = new ServerSocket(GameConstants.PORT);

                // Show waiting message
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Waiting for Connection");
                    alert.setHeaderText("Server Started");
                    alert.setContentText("Waiting for another player to connect via Radmin VPN.\nMake sure both players are connected to the same Radmin network.");
                    alert.show();
                });

                // Accept connection (blocking call)
                socket = serverSocket.accept();
                serverSocket.close();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Connection Established");
                    alert.setHeaderText("Player Connected");
                    alert.setContentText("Another player has joined your game!");
                    alert.show();
                });
            } else {
                // Mode client - se connecte à un hôte existant via son IP Radmin
                isHost = false;
                socket = new Socket(radminIP, GameConstants.PORT);
            }

            // Setup streams once connected
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            isConnected = true;

            return true;
        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Failed to establish connection");
                alert.setContentText("Error: " + e.getMessage() +
                        "\nMake sure both players are connected to the same Radmin VPN network.");
                alert.show();
            });
            e.printStackTrace();
            return false;
        }
    }

    public void sendData(Object data) {
        try {
            if (isConnected && outputStream != null) {
                outputStream.writeObject(data);
                outputStream.flush();
            }
        } catch (IOException e) {
            handleDisconnect(e);
        }
    }

    public Object receiveData() {
        try {
            if (isConnected && inputStream != null) {
                return inputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            handleDisconnect(e);
        }
        return null;
    }

    private void handleDisconnect(Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Lost");
            alert.setHeaderText("Connection to other player was lost");
            alert.setContentText("The game will continue in single player mode.");
            alert.show();
        });
        close();
    }

    public void close() {
        try {
            isConnected = false;
            if (socket != null) socket.close();
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isHost() {
        return isHost;
    }

    public boolean isConnected() {
        return isConnected;
    }
}