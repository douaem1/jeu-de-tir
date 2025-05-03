package chat_Client_Serveur;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe représentant l'état du jeu
 * Elle utilise le pattern Singleton pour garantir qu'il n'y ait qu'une seule instance
 */
public class GameState {
    // Instance unique
    private static GameState instance = null;

    // Données du jeu
    private Map<String, Position> playerPositions;
    private Map<String, Integer> playerScores;
    private Map<String, String> playerAircrafts; // Stocker le type d'avion de chaque joueur
    private Map<String, Integer> playerHealth; // Santé des joueurs

    // Liste des ennemis partagés par tous les joueurs
    private List<Enemy> enemies;

    private Random random;
    private long lastEnemySpawnTime;

    // Constantes
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final String[] AIRCRAFT_TYPES = {"default", "fighter", "bomber"};
    private static final int DEFAULT_HEALTH = 100;
    private static final int ENEMY_SPAWN_DELAY = 2000;
    private static final int MAX_ENEMIES = 10;

    private GameState() {
        playerPositions = new ConcurrentHashMap<>();
        playerScores = new ConcurrentHashMap<>();
        playerAircrafts = new ConcurrentHashMap<>();
        playerHealth = new ConcurrentHashMap<>();
        enemies = new ArrayList<>();
        random = new Random();
        lastEnemySpawnTime = System.currentTimeMillis();
    }
    public static synchronized GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }
    public void addPlayer(String username) {
        int x = random.nextInt(GAME_WIDTH - 100) + 50;
        int y = GAME_HEIGHT - 100;
        playerPositions.put(username, new Position(x, y));
        playerScores.put(username, 0);
        playerAircrafts.put(username, "default");
        playerHealth.put(username, DEFAULT_HEALTH);
    }
    public void removePlayer(String username) {
        playerPositions.remove(username);
        playerScores.remove(username);
        playerAircrafts.remove(username);
        playerHealth.remove(username);
    }
    public void updatePlayerPosition(String username, int x, int y) {
        x = Math.max(0, Math.min(GAME_WIDTH, x));
        y = Math.max(0, Math.min(GAME_HEIGHT, y));
        playerPositions.put(username, new Position(x, y));
    }
    public void setPlayerAircraft(String username, String aircraftType) {
        if (playerAircrafts.containsKey(username)) {
            boolean validType = false;
            for (String type : AIRCRAFT_TYPES) {
                if (type.equals(aircraftType)) {
                    validType = true;
                    break;
                }
            }

            if (validType) {
                playerAircrafts.put(username, aircraftType);
            }
        }
    }
    public void updatePlayerScore(String username, int points) {
        int currentScore = playerScores.getOrDefault(username, 0);
        playerScores.put(username, currentScore + points);
    }
    public void updatePlayerHealth(String username, int healthChange) {
        int currentHealth = playerHealth.getOrDefault(username, DEFAULT_HEALTH);
        int newHealth = Math.max(0, currentHealth + healthChange);
        playerHealth.put(username, newHealth);
        if (newHealth == 0) {
            updatePlayerScore(username, -50);
            playerHealth.put(username, DEFAULT_HEALTH);
            int x = random.nextInt(GAME_WIDTH - 100) + 50;
            int y = GAME_HEIGHT - 100;
            playerPositions.put(username, new Position(x, y));
        }
    }
    public void spawnEnemies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemySpawnTime > ENEMY_SPAWN_DELAY && enemies.size() < MAX_ENEMIES) {
            int x = random.nextInt(GAME_WIDTH - 50) + 25;
            int y = 0;
            int speedX = random.nextInt(5) - 2;
            int speedY = random.nextInt(3) + 1;
            int health = random.nextInt(3) + 1;

            enemies.add(new Enemy(x, y, speedX, speedY, health));
            lastEnemySpawnTime = currentTime;
        }
    }
    public void moveEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();

        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.move();
            if (enemy.y > GAME_HEIGHT) {
                iterator.remove();
            }
        }
    }
    public void checkCollisions() {
        for (Map.Entry<String, Position> entry : playerPositions.entrySet()) {
            String username = entry.getKey();
            Position playerPos = entry.getValue();
            Iterator<Enemy> iterator = enemies.iterator();
            while (iterator.hasNext()) {
                Enemy enemy = iterator.next();
                int distance = (int) Math.sqrt(Math.pow(playerPos.x - enemy.x, 2) + Math.pow(playerPos.y - enemy.y, 2));

                if (distance < 30) {
                    updatePlayerHealth(username, -20);
                    enemy.health--;
                    if (enemy.health <= 0) {
                        iterator.remove();
                        updatePlayerScore(username, 10);
                    }
                }
            }
        }
    }
    public Position getPlayerPosition(String username) {
        return playerPositions.get(username);
    }

    // Obtenir le type d'avion d'un joueur
    public String getPlayerAircraft(String username) {
        return playerAircrafts.getOrDefault(username, "default");
    }

    // Obtenir le score d'un joueur
    public int getPlayerScore(String username) {
        return playerScores.getOrDefault(username, 0);
    }

    // Obtenir la santé d'un joueur
    public int getPlayerHealth(String username) {
        return playerHealth.getOrDefault(username, DEFAULT_HEALTH);
    }

    // Accès à toutes les positions des joueurs pour le gestionnaire de collision
    public Map<String, Position> getPlayerPositions() {
        return playerPositions;
    }

    // Obtenir l'état du jeu sous forme de chaîne
    public String getGameStateAsString() {
        StringBuilder sb = new StringBuilder("GAMESTATE");

        // Ajouter les joueurs
        for (Map.Entry<String, Position> entry : playerPositions.entrySet()) {
            String username = entry.getKey();
            Position pos = entry.getValue();
            int score = playerScores.getOrDefault(username, 0);
            String aircraft = playerAircrafts.getOrDefault(username, "default");
            int health = playerHealth.getOrDefault(username, DEFAULT_HEALTH);

            sb.append("|PLAYER|").append(username).append(",")
                    .append(pos.x).append(",")
                    .append(pos.y).append(",")
                    .append(score).append(",")
                    .append(aircraft).append(",") // Ajouter le type d'avion
                    .append(health); // Ajouter la santé
        }

        // Ajouter les ennemis
        for (Enemy enemy : enemies) {
            sb.append("|ENEMY|").append(enemy.x).append(",")
                    .append(enemy.y).append(",")
                    .append(enemy.health);
        }

        return sb.toString();
    }

    // Obtenir le nombre de joueurs actuellement dans le jeu
    public int getPlayerCount() {
        return playerPositions.size();
    }

    // Vérifier si un joueur existe
    public boolean playerExists(String username) {
        return playerPositions.containsKey(username);
    }

    // Classe interne pour représenter une position
    public static class Position {
        public int x;
        public int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    public static class Enemy {
        public int x;
        public int y;
        public int speedX;
        public int speedY;
        public int health;

        public Enemy(int x, int y, int speedX, int speedY, int health) {
            this.x = x;
            this.y = y;
            this.speedX = speedX;
            this.speedY = speedY;
            this.health = health;
        }

        public void move() {
            x += speedX;
            y += speedY;

            // Rebondir sur les bords de l'écran
            if (x < 0 || x > GAME_WIDTH) {
                speedX = -speedX;
            }
        }
    }

}