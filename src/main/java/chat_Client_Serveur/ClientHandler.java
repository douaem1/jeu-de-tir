package chat_Client_Serveur;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientId;
    private GameServer server;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running = false;

    public ClientHandler(Socket socket, String clientId, GameServer server) {
        this.socket = socket;
        this.clientId = clientId;
        this.server = server;
    }

    public ClientHandler(Socket socket) {
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoyer l'ID du client
            sendMessage("INIT|" + clientId);

            running = true;
            String message;

            while (running && (message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Error handling client " + clientId + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];

        switch (command) {
            case "REGISTER":
                // Format: REGISTER|aircraft|x|y
                if (parts.length >= 4) {
                    String aircraft = parts[1];
                    double x = Double.parseDouble(parts[2]);
                    double y = Double.parseDouble(parts[3]);
                    server.registerPlayer(clientId, aircraft, x, y);
                }
                break;

            case "MOVE":
                // Format: MOVE|x|y
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    server.updatePlayerPosition(clientId, x, y);
                }
                break;

            case "SHOT":
                // Format: SHOT|x|y
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    server.handlePlayerShot(clientId, x, y);
                }
                break;

            case "ENEMY_HIT":
                // Format: ENEMY_HIT|enemyId
                if (parts.length >= 2) {
                    int enemyId = Integer.parseInt(parts[1]);
                    server.handleEnemyHit(clientId, enemyId);
                }
                break;

            case "CHAT":
                // Format: CHAT|message
                if (parts.length >= 2) {
                    String chatMessage = parts[1];
                    server.handleChatMessage(clientId, chatMessage);
                }
                break;
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void cleanup() {
        running = false;
        server.removeClient(clientId);

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}