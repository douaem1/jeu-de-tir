package chat_Client_Serveur;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 5555;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PlayerInfo> playerInfos = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<EnemyInfo> enemies = new CopyOnWriteArrayList<>();
    private ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running = false;
    private int nextEnemyId = 1;

    public GameServer() {
        this.serverSocket = null;
        this.clients = new ConcurrentHashMap<>();
        this.playerInfos = new ConcurrentHashMap<>();
        this.enemies = new CopyOnWriteArrayList<>();
        this.pool = Executors.newCachedThreadPool();
        this.running = false;
        this.nextEnemyId = 1;
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Game server started on port " + PORT);

            // Démarrer le thread pour la génération des ennemis
            startEnemyGenerator();

            // Démarrer le thread pour la mise à jour des ennemis
            startEnemyUpdater();

            // Accepter les connexions entrantes
            while (running) {
                Socket clientSocket = serverSocket.accept();
                String clientId = UUID.randomUUID().toString();

                System.out.println("New client connected: " + clientId);

                // Créer un gestionnaire de client et le démarrer
                ClientHandler handler = new ClientHandler(clientSocket, clientId, this);
                clients.put(clientId, handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void startEnemyGenerator() {
        pool.execute(() -> {
            try {
                while (running) {
                    if (clients.size() > 0) {
                        // Générer un nouvel ennemi toutes les 2 secondes
                        createEnemy();
                        Thread.sleep(2000);
                    } else {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void startEnemyUpdater() {
        pool.execute(() -> {
            try {
                while (running) {
                    updateEnemies();
                    Thread.sleep(16);  // ~60 FPS
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void createEnemy() {
        // Création d'un ennemi avec une position aléatoire en haut de l'écran
        int enemyId = nextEnemyId++;
        double x = Math.random() * 1100;  // Pour éviter les bords
        EnemyInfo enemy = new EnemyInfo(enemyId, x, -50);
        enemies.add(enemy);

        // Informer tous les clients du nouvel ennemi
        broadcast("ENEMY_SPAWN|" + enemyId + "|" + x + "|" + -50);
    }

    private void updateEnemies() {
        Iterator<EnemyInfo> iter = enemies.iterator();
        while (iter.hasNext()) {
            EnemyInfo enemy = iter.next();
            enemy.y += 0.5;  // Même vitesse que dans le code original

            // Si l'ennemi sort de l'écran, le supprimer
            if (enemy.y > 800) {
                iter.remove();
                broadcast("ENEMY_REMOVE|" + enemy.id);
            }
        }

        // Envoyer les mises à jour seulement si nécessaire (tous les 5 frames soit ~12 fois par seconde)
        if (System.currentTimeMillis() % 80 == 0) {
            for (EnemyInfo enemy : enemies) {
                broadcast("ENEMY_POS|" + enemy.id + "|" + enemy.x + "|" + enemy.y);
            }
        }
    }

    public void registerPlayer(String clientId, String aircraft, double x, double y) {
        PlayerInfo info = new PlayerInfo(clientId, aircraft, x, y);
        playerInfos.put(clientId, info);

        // Informer tous les clients du nouveau joueur
        broadcast("PLAYER_JOIN|" + clientId + "|" + aircraft + "|" + x + "|" + y);

        // Informer le nouveau joueur de tous les joueurs existants
        ClientHandler newClient = clients.get(clientId);
        for (Map.Entry<String, PlayerInfo> entry : playerInfos.entrySet()) {
            if (!entry.getKey().equals(clientId)) {
                PlayerInfo player = entry.getValue();
                newClient.sendMessage("PLAYER_JOIN|" + player.id + "|" + player.aircraft + "|" + player.x + "|" + player.y);
            }
        }

        // Informer le nouveau joueur de tous les ennemis existants
        for (EnemyInfo enemy : enemies) {
            newClient.sendMessage("ENEMY_SPAWN|" + enemy.id + "|" + enemy.x + "|" + enemy.y);
        }
    }

    public void updatePlayerPosition(String clientId, double x, double y) {
        PlayerInfo info = playerInfos.get(clientId);
        if (info != null) {
            info.x = x;
            info.y = y;

            // Informer tous les autres clients du mouvement
            broadcastExcept("PLAYER_POS|" + clientId + "|" + x + "|" + y, clientId);
        }
    }

    public void handlePlayerShot(String clientId, double x, double y) {
        // Informer tous les clients du tir
        broadcast("PLAYER_SHOT|" + clientId + "|" + x + "|" + y);
    }

    public void handleEnemyHit(String clientId, int enemyId) {
        // Vérifier si l'ennemi existe
        boolean enemyFound = false;
        for (EnemyInfo enemy : enemies) {
            if (enemy.id == enemyId) {
                enemyFound = true;
                enemies.remove(enemy);
                break;
            }
        }

        if (enemyFound) {
            // Mettre à jour le score du joueur
            PlayerInfo player = playerInfos.get(clientId);
            if (player != null) {
                player.score += 10;

                // Informer tous les clients de la destruction de l'ennemi et du score
                broadcast("ENEMY_DESTROYED|" + enemyId + "|" + clientId + "|" + player.score);
            }
        }
    }

    public void handleChatMessage(String clientId, String message) {
        PlayerInfo sender = playerInfos.get(clientId);
        if (sender != null) {
            broadcast("CHAT|" + clientId + "|" + message);
        }
    }

    public void removeClient(String clientId) {
        clients.remove(clientId);
        PlayerInfo removedPlayer = playerInfos.remove(clientId);

        if (removedPlayer != null) {
            broadcast("PLAYER_LEAVE|" + clientId);
        }

        System.out.println("Client disconnected: " + clientId);
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }

    private void broadcastExcept(String message, String exceptClientId) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(exceptClientId)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
    }

    // Classe interne pour stocker les infos d'un joueur
    private static class PlayerInfo {
        String id;
        String aircraft;
        double x;
        double y;
        int score;
        int lives;

        public PlayerInfo(String id, String aircraft, double x, double y) {
            this.id = id;
            this.aircraft = aircraft;
            this.x = x;
            this.y = y;
            this.score = 0;
            this.lives = 3;
        }
    }

    // Classe interne pour stocker les infos d'un ennemi
    private static class EnemyInfo {
        int id;
        double x;
        double y;

        public EnemyInfo(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}