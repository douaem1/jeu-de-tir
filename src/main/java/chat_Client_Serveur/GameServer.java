package chat_Client_Serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;
    private final Random random = new Random();
    private final GameState gameState = GameState.getInstance();

    // Game settings
    private static final int MIN_ENEMY_SPAWN_DELAY = 3000; // ms
    private static final int MAX_ENEMY_SPAWN_DELAY = 8000; // ms
    private static final int ENEMY_MOVEMENT_INTERVAL = 500; // ms
    private static final int GAME_STATE_BROADCAST_INTERVAL = 1000; // ms
    private static final int ENEMY_DESCENT_SPEED = 1; // pixels per update

    // Constructor that takes a server socket
    public GameServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.threadPool = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void startServer() {
        try {
            isRunning = true;
            System.out.println("Server started on port " + serverSocket.getLocalPort());

            // Start enemy spawn thread
            startEnemySpawner();

            // Start game state broadcast thread
            startGameStateBroadcaster();

            // Start enemy movement thread
            startEnemyMovement();

            // Main server loop
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());

                    // Create a new client handler for each connected client
                    GameClientHandler clientHandler = new GameClientHandler(socket);
                    threadPool.execute(clientHandler);

                } catch (SocketException se) {
                    if (isRunning) {
                        System.err.println("Server socket exception: " + se.getMessage());
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeServerSocket();
        }
    }

    private void startEnemySpawner() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                // Only spawn enemies if game has at least 2 players
                if (gameState.getPlayerCount() >= 2) {
                    // Random position at top of screen
                    int x = random.nextInt(1100) + 50; // Between 50 and 1150
                    int y = random.nextInt(100); // Top of screen
                    int health = 100;

                    // Add enemy to game state
                    String enemyId = gameState.addEnemy(x, y, health);

                    // Broadcast to all clients
                    for (GameClientHandler handler : GameClientHandler.clientHandlers) {
                        handler.sendMessage("ENEMY_SPAWN " + enemyId + "," + x + "," + y);
                    }

                    System.out.println("Spawned enemy " + enemyId + " at position " + x + "," + y);
                }
            } catch (Exception e) {
                System.err.println("Error in enemy spawner: " + e.getMessage());
            }
        }, 5000, MIN_ENEMY_SPAWN_DELAY + random.nextInt(MAX_ENEMY_SPAWN_DELAY - MIN_ENEMY_SPAWN_DELAY), TimeUnit.MILLISECONDS);
    }

    private void startGameStateBroadcaster() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (gameState.getPlayerCount() >= 1) {
                    String gameStateStr = gameState.getGameStateAsString();
                    for (GameClientHandler handler : GameClientHandler.clientHandlers) {
                        handler.sendMessage(gameStateStr);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting game state: " + e.getMessage());
            }
        }, GAME_STATE_BROADCAST_INTERVAL, GAME_STATE_BROADCAST_INTERVAL, TimeUnit.MILLISECONDS);
    }

      public void stopServer() {
        isRunning = false;
        closeServerSocket();
        threadPool.shutdown();
        scheduler.shutdown();

        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Server stopped");
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }
    private void startEnemyMovement() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (gameState.getPlayerCount() >= 2) {
                    // Update positions for all enemies
                    for (String enemyId : gameState.getEnemyIds()) {
                        // Make enemies move downward
                        gameState.moveEnemyDown(enemyId, ENEMY_DESCENT_SPEED);

                        // Check if enemy has reached bottom of screen
                        if (gameState.getEnemyY(enemyId) > 750) {
                            // Remove enemy when it goes off screen
                            gameState.removeEnemy(enemyId);
                            // Broadcast enemy removal to all clients
                            for (GameClientHandler handler : GameClientHandler.clientHandlers) {
                                handler.sendMessage("ENEMY_REMOVE " + enemyId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating enemy positions: " + e.getMessage());
            }
        }, ENEMY_MOVEMENT_INTERVAL, ENEMY_MOVEMENT_INTERVAL, TimeUnit.MILLISECONDS);
    }
}