package chat_Client_Serveur;

import Game.GameManager;
import javafx.application.Platform;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameClient {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private GameManager gameManager;
    private volatile boolean running = true;

    // Lists to track players and enemies
    private final List<Player> players = new ArrayList<>();
    private final CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();

    public GameClient(String username, String serverAddress, GameManager gameManager) {
        try {
            this.socket = new Socket(serverAddress, 7103);
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.gameManager = gameManager;

            // Set the output writer for the GameManager
            gameManager.out = new PrintWriter(socket.getOutputStream(), true);

            // Send initial login message
            sendMessage("USER " + username);
        } catch (IOException e) {
            closeEverything();
            e.printStackTrace();
        }
    }

    public GameClient() {

    }

    public void sendAircraftSelection(String aircraftType) {
        sendMessage("SELECT_AIRCRAFT " + aircraftType);
    }

    public void sendPlayerReady() {
        sendMessage("GAME_READY");
    }

    public void sendPlayerPosition(double x, double y) {
        sendMessage("MOVE " + (int)x + "," + (int)y);
    }

    public void sendFireAction(double x, double y) {
        sendMessage("FIRE " + (int)x + "," + (int)y);
    }

    public void sendMessage(String message) {
        try {
            if (socket.isConnected() && bufferedWriter != null) {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything();
            e.printStackTrace();
        }
    }

    public void listenForMessage() {
        new Thread(() -> {
            String messageFromServer;

            try {
                while (running && socket.isConnected() && (messageFromServer = bufferedReader.readLine()) != null) {
                    final String message = messageFromServer;
                    System.out.println("Received from server: " + message);

                    // Process different message types
                    if (message.startsWith("START_GAME")) {
                        Platform.runLater(() -> {
                            // Game can now start
                            gameManager.gameRunning = true;
                        });
                    }
                    else if (message.startsWith("WAITING_FOR_PLAYERS")) {
                        String[] parts = message.split(" ");
                        int remaining = Integer.parseInt(parts[1]);
                        Platform.runLater(() -> {
                            // Show waiting message
                            System.out.println("Waiting for " + remaining + " more players");
                        });
                    }
                    else if (message.startsWith("GAME_STATE")) {
                        // Process game state update from server
                        processGameState(message.substring(11));
                    }
                    else if (message.startsWith("PLAYER_JOIN")) {
                        String playerName = message.substring(12);
                        Platform.runLater(() -> {
                            System.out.println("Player joined: " + playerName);
                        });
                    }
                    else if (message.startsWith("PLAYER_LEAVE")) {
                        String playerName = message.substring(13);
                        Platform.runLater(() -> {
                            System.out.println("Player left: " + playerName);
                            // Remove player from the game
                            removePlayerFromGame(playerName);
                        });
                    }
                    else if (message.startsWith("PLAYER_POS")) {
                        String[] parts = message.substring(11).split(",");
                        String playerName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);

                        // Update player position if it's not the current user
                        if (!playerName.equals(username)) {
                            Platform.runLater(() -> {
                                updatePlayerPosition(playerName, x, y);
                            });
                        }
                    }
                    else if (message.startsWith("PLAYER_FIRE")) {
                        String[] parts = message.substring(12).split(",");
                        String playerName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);

                        // Show firing animation if it's not the current user
                        if (!playerName.equals(username)) {
                            Platform.runLater(() -> {
                                showPlayerFiring(playerName, x, y);
                            });
                        }
                    }
                    else if (message.startsWith("ENEMY_SPAWN")) {
                        String[] parts = message.substring(12).split(",");
                        String enemyId = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);

                        Platform.runLater(() -> {
                            gameManager.addNetworkEnemy(enemyId, x, y);
                        });
                    }
                    else if (message.startsWith("ENEMY_HIT")) {
                        String[] parts = message.substring(10).split(",");
                        String enemyId = parts[0];
                        String playerName = parts[1];

                        Platform.runLater(() -> {
                            processEnemyHit(enemyId, playerName);
                        });
                    }
                }
            } catch (IOException e) {
                closeEverything();
            }
        }).start();
    }

    private void processGameState(String gameStateData) {
        // Format: players:[name:x:y:aircraft:health:score,...];enemies:[id:x:y:health,...]
        try {
            String[] sections = gameStateData.split(";");

            if (sections.length >= 1) {
                // Process players
                String playersSection = sections[0].substring(8);  // Remove "players:"
                if (!playersSection.isEmpty()) {
                    String[] playerData = playersSection.split(",");
                    for (String playerStr : playerData) {
                        String[] attrs = playerStr.split(":");
                        if (attrs.length >= 6) {
                            String name = attrs[0];
                            int x = Integer.parseInt(attrs[1]);
                            int y = Integer.parseInt(attrs[2]);
                            String aircraft = attrs[3];
                            int health = Integer.parseInt(attrs[4]);
                            int score = Integer.parseInt(attrs[5]);

                            // Skip if it's the current player
                            if (!name.equals(username)) {
                                Platform.runLater(() -> {
                                    updateOrCreatePlayer(name, x, y, aircraft, health, score);
                                });
                            }
                        }
                    }
                }
            }

            if (sections.length >= 2) {
                // Process enemies
                String enemiesSection = sections[1].substring(9);  // Remove "enemies:"
                if (!enemiesSection.isEmpty()) {
                    String[] enemyData = enemiesSection.split(",");
                    for (String enemyStr : enemyData) {
                        String[] attrs = enemyStr.split(":");
                        if (attrs.length >= 4) {
                            String id = attrs[0];
                            int x = Integer.parseInt(attrs[1]);
                            int y = Integer.parseInt(attrs[2]);
                            int health = Integer.parseInt(attrs[3]);

                            Platform.runLater(() -> {
                                updateOrCreateEnemy(id, x, y, health);
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing game state: " + e.getMessage());
        }
    }

    private void updateOrCreatePlayer(String name, int x, int y, String aircraft, int health, int score) {
        // Check if the player exists in the game
        // If not, create a new player view and add to game
        // If exists, update the player's position and stats

        boolean playerExists = false;
        for (Player player : players) {
            if (player.name.equals(name)) {
                player.x = x;
                player.y = y;
                player.aircraft = aircraft;
                player.health = health;
                player.score = score;
                playerExists = true;

                // Update player position in the game
                ImageView playerView = gameManager.playerViews.get(name);
                if (playerView != null) {
                    playerView.setX(x);
                    playerView.setY(y);
                }
                break;
            }
        }

        if (!playerExists) {
            Player newPlayer = new Player(name, x, y, aircraft, health, score);
            players.add(newPlayer);

            // Create player in game
            // This would need a method in GameManager to create other players
            createPlayerInGame(newPlayer);
        }
    }

    private void createPlayerInGame(Player player) {
        // Create a player view in the game for other players
        ImageView playerView = null;

        // Use the current player's createPlayer method but customize it
        // This would need to be implemented in GameManager
        // For now, we'll just log
        System.out.println("Creating player in game: " + player.name);
    }

    private void updateOrCreateEnemy(String id, int x, int y, int health) {
        // Similar to updateOrCreatePlayer but for enemies
        boolean enemyExists = false;
        for (Enemy enemy : enemies) {
            if (enemy.id.equals(id)) {
                enemy.x = x;
                enemy.y = y;
                enemy.health = health;
                enemyExists = true;
                break;
            }
        }

        if (!enemyExists) {
            Enemy newEnemy = new Enemy(id, x, y, health);
            enemies.add(newEnemy);

            // Create enemy in game
            gameManager.addNetworkEnemy(id, x, y);
        }
    }

    private void updatePlayerPosition(String playerName, int x, int y) {
        // Update the player's position in the UI
        ImageView playerView = gameManager.playerViews.get(playerName);
        if (playerView != null) {
            playerView.setX(x);
            playerView.setY(y);
        }
    }

    private void showPlayerFiring(String playerName, int x, int y) {
        // Show firing animation for another player
        // This would need a method in GameManager
        System.out.println("Player " + playerName + " fired at " + x + "," + y);
    }

    private void processEnemyHit(String enemyId, String playerName) {
        // Process when an enemy is hit
        // This would need a method in GameManager
        System.out.println("Enemy " + enemyId + " hit by " + playerName);
    }

    private void removePlayerFromGame(String playerName) {
        // Remove player from lists and UI
        players.removeIf(p -> p.name.equals(playerName));

        // Remove from UI - would need GameManager method
        ImageView playerView = gameManager.playerViews.remove(playerName);
        if (playerView != null && GameManager.gamepane != null) {
            GameManager.gamepane.getChildren().remove(playerView);
        }
    }

    public void closeEverything() {
        running = false;
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner classes for tracking game entities
    public static class Player {
        String name;
        int x;
        int y;
        String aircraft;
        int health;
        int score;

        public Player(String name, int x, int y, String aircraft, int health, int score) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.aircraft = aircraft;
            this.health = health;
            this.score = score;
        }
    }

    public static class Enemy {
        String id;
        int x;
        int y;
        int health;

        public Enemy(String id, int x, int y, int health) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.health = health;
        }
    }
    }