/**
 * Classe GameServer à créer dans un nouveau fichier Game/GameServer.java
 * Cette classe gère la communication entre les joueurs
 */
package chat_Client_Serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiplayerServer {
    private static final int DEFAULT_PORT = 5555;
    private ServerSocket serverSocket;
    private boolean running = false;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private Map<String, String> matches = new HashMap<>(); // player -> opponent


    public void start() {
        if (serverSocket == null) return;

        running = true;
        new Thread(() -> {
            System.out.println("Server listening for connections...");
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // Create and start a new handler for this client
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        for (ClientHandler handler : clients.values()) {
            handler.disconnect();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server");
            e.printStackTrace();
        }
    }

    private void matchPlayers() {
        // Simple matching algorithm - just match any two waiting players
        synchronized (clients) {
            String[] waiting = clients.keySet().stream()
                    .filter(player -> !matches.containsKey(player) && !matches.containsValue(player))
                    .toArray(String[]::new);

            // Match pairs
            for (int i = 0; i < waiting.length - 1; i += 2) {
                String player1 = waiting[i];
                String player2 = waiting[i + 1];

                matches.put(player1, player2);
                matches.put(player2, player1);

                // Notify both players
                clients.get(player1).send("MATCHED:" + player2);
                clients.get(player2).send("MATCHED:" + player1);

                System.out.println("Matched players: " + player1 + " and " + player2);
            }

            // If any player is left without a match, notify they're waiting
            if (waiting.length % 2 != 0 && waiting.length > 0) {
                String lastPlayer = waiting[waiting.length - 1];
                clients.get(lastPlayer).send("WAITING");
                System.out.println("Player waiting for match: " + lastPlayer);
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private boolean connected = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Error setting up client handler");
                e.printStackTrace();
                connected = false;
            }
        }

        @Override
        public void run() {
            try {
                // Wait for JOIN message with username
                String joinMessage = in.readLine();
                if (joinMessage != null && joinMessage.startsWith("JOIN:")) {
                    username = joinMessage.substring(5); // Extract username
                    clients.put(username, this);
                    System.out.println("Player registered: " + username);

                    // Send welcome message
                    send("WELCOME:" + username);

                    // Try to match this player
                    matchPlayers();

                    // Main message loop
                    String message;
                    while (connected && (message = in.readLine()) != null) {
                        handleMessage(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
            } finally {
                disconnect();
            }
        }

        private void handleMessage(String message) {
            System.out.println("From " + username + ": " + message);

            // Find opponent
            String opponent = matches.get(username);
            ClientHandler opponentHandler = opponent != null ? clients.get(opponent) : null;

            // Handle messages and relay to opponent
            if (message.startsWith("POS:")) {
                // Position messages are relayed directly to opponent
                if (opponentHandler != null) {
                    opponentHandler.send(message);
                }
            } else if (message.equals("FIRE")) {
                // Fire messages are relayed directly to opponent
                if (opponentHandler != null) {
                    opponentHandler.send("FIRE");
                }
            } else if (message.startsWith("HIT")) {
                // Hit notifications are relayed to opponent
                if (opponentHandler != null) {
                    opponentHandler.send("HIT");
                }
            } else if (message.startsWith("SCORE:")) {
                // Score updates are relayed to opponent
                if (opponentHandler != null) {
                    opponentHandler.send(message);
                }
            } else if (message.equals("PING")) {
                // Respond to ping with PONG to confirm connection
                send("PONG");
            } else if (message.equals("DISCONNECT") || message.equals("GAMEOVER")) {
                // Handle disconnection
                if (opponentHandler != null) {
                    opponentHandler.send("DISCONNECT");
                }
                disconnect();
            }
        }

        public void send(String message) {
            if (connected && out != null) {
                out.println(message);
            }
        }

        public void disconnect() {
            if (!connected) return;

            connected = false;

            // Remove player from matches and notify opponent if needed
            synchronized (clients) {
                String opponent = matches.remove(username);
                if (opponent != null) {
                    matches.remove(opponent); // Remove reverse mapping
                    ClientHandler opponentHandler = clients.get(opponent);
                    if (opponentHandler != null) {
                        opponentHandler.send("DISCONNECT");
                    }
                }

                clients.remove(username);
            }

            // Close resources
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection");
                e.printStackTrace();
            }

            System.out.println("Client handler for " + username + " closed");
        }
    }

    // Main method to run the server standalone
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default port " + DEFAULT_PORT);
            }
        }

        GameServer server = new GameServer();
        server.start();
    }
}