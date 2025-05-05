package chat_Client_Serveur;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameClientHandler implements Runnable {
    public static List<GameClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private GameState gameState;
    private ScheduledExecutorService scheduler;
    private boolean readyToPlay = false;

    public GameClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.gameState = GameState.getInstance();
            this.clientUsername = "";
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            clientHandlers.add(this);
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        try {
            while (socket.isConnected() && (messageFromClient = bufferedReader.readLine()) != null) {
                System.out.println("Message from client: " + messageFromClient);

                if (messageFromClient.startsWith("USER ")) {
                    handleUserLogin(messageFromClient.substring(5));
                }
                else if (messageFromClient.startsWith("SELECT_AIRCRAFT ")) {
                    handleAircraftSelection(messageFromClient.substring(15));
                }
                else if (messageFromClient.startsWith("GAME_READY")) {
                    handleGameReady();
                }
                else if (messageFromClient.startsWith("MOVE ")) {
                    handlePlayerMovement(messageFromClient.substring(5));
                }
                else if (messageFromClient.startsWith("FIRE ")) {
                    handlePlayerFire(messageFromClient.substring(5));
                }
                else if (messageFromClient.startsWith("SYNC_ENEMIES")) {
                    // Send current game state on demand
                    sendMessage(gameState.getGameStateAsString());
                }
                else if (messageFromClient.startsWith("PLAYER_POS:")) {
                    // Format: PLAYER_POS:username,x,y
                    String[] parts = messageFromClient.substring(11).split(",");
                    if (parts.length == 3) {
                        try {
                            String playerName = parts[0];
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);

                            // Update player position in game state
                            gameState.updatePlayerPosition(playerName, x, y);

                            // Broadcast to all other clients
                            broadcastMessage("PLAYER_POS " + playerName + "," + x + "," + y);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid position format: " + messageFromClient);
                        }
                    }
                }
                else if (messageFromClient.startsWith("PLAYER_FIRE:")) {
                    // Format: PLAYER_FIRE:username,x,y
                    String[] parts = messageFromClient.substring(12).split(",");
                    if (parts.length == 3) {
                        try {
                            String playerName = parts[0];
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);

                            // Broadcast to all other clients
                            broadcastMessage("PLAYER_FIRE " + playerName + "," + x + "," + y);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid fire format: " + messageFromClient);
                        }
                    }
                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void handleUserLogin(String username) {
        this.clientUsername = username;
        gameState.addPlayer(username);

        // Notify all clients about the new player
        broadcastMessage("PLAYER_JOIN " + username);

        // Send current game state to the new player
        sendMessage(gameState.getGameStateAsString());

        // Check if we can start the game (at least 2 players)
        checkGameStart();
    }

    private void handleAircraftSelection(String aircraftType) {
        gameState.setPlayerAircraft(clientUsername, aircraftType);
        broadcastGameState();
    }

    private void handleGameReady() {
        readyToPlay = true;

        // Send current game state to this client
        sendMessage(gameState.getGameStateAsString());

        // Start regular game state updates for this client
        startGameStateUpdates();
    }

    private void handlePlayerMovement(String moveData) {
        String[] parts = moveData.split(",");
        if (parts.length == 2) {
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                gameState.updatePlayerPosition(clientUsername, x, y);

                // Broadcast new position to all clients
                broadcastMessage("PLAYER_POS " + clientUsername + "," + x + "," + y);
            } catch (NumberFormatException e) {
                System.err.println("Invalid movement format: " + moveData);
            }
        }
    }



    private void checkGameStart() {
        if (gameState.getPlayerCount() >= 2) {
            // Notify all clients that game can start
            broadcastMessage("START_GAME");
            // Send initial game state
            broadcastGameState();
        } else {
            // Wait for more players
            sendMessage("WAITING_FOR_PLAYERS " + gameState.getPlayerCount() + "/2");
        }
    }

    private void startGameStateUpdates() {
        // Schedule regular game state updates
        scheduler.scheduleAtFixedRate(() -> {
            if (socket.isConnected() && readyToPlay) {
                sendMessage(gameState.getGameStateAsString());
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    // Send message to all clients except this one
    public void broadcastMessage(String message) {
        for (GameClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.sendMessage(message);
                }
            } catch (Exception e) {
                // Skip clients with connection issues
                System.err.println("Error broadcasting to client: " + e.getMessage());
            }
        }
    }

    // Send game state to all connected clients
    private void broadcastGameState() {
        String gameStateString = gameState.getGameStateAsString();
        for (GameClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.sendMessage(gameStateString);
            } catch (Exception e) {
                System.err.println("Error sending game state to client: " + e.getMessage());
            }
        }
    }

    // Send message to this client
    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    // Close connection and clean up resources
    private void closeEverything() {
        clientHandlers.remove(this);

        // Notify all clients that this player left
        if (clientUsername != null && !clientUsername.isEmpty()) {
            broadcastMessage("PLAYER_LEFT " + clientUsername);
            gameState.removePlayer(clientUsername);
        }

        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
        }

        // Close all streams and socket
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get client username
    public String getClientUsername() {
        return clientUsername;
    }

    // Check if client is ready to play
    public boolean isReadyToPlay() {
        return readyToPlay;
    }
    private void handlePlayerFire(String fireData) {
        String[] parts = fireData.split(",");
        if (parts.length == 2) {
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);

                // Check for collision with enemies
                for (String enemyId : gameState.getEnemyIds()) {
                    int enemyX = gameState.getEnemyX(enemyId);
                    int enemyY = gameState.getEnemyY(enemyId);

                    // Simple collision detection (within 30 pixels)
                    if (Math.abs(x - enemyX) < 30 && Math.abs(y - enemyY) < 30) {
                        // Damage enemy
                        gameState.damageEnemy(enemyId, 25, clientUsername);

                        // Broadcast hit to all clients
                        broadcastMessage("ENEMY_HIT " + enemyId + "," + clientUsername);
                    }
                }

                // Broadcast fire action to all clients
                broadcastMessage("PLAYER_FIRE " + clientUsername + "," + x + "," + y);
            } catch (NumberFormatException e) {
                System.err.println("Invalid fire format: " + fireData);
            }
        }
    }
}