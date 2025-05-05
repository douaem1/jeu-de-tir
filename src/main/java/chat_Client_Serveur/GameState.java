package chat_Client_Serveur;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton class to track game state on the server
 */
public class GameState {
    private static GameState instance;
    private final Map<String, PlayerState> players;
    private final Map<String, EnemyState> enemies;
    private final AtomicInteger enemyIdCounter;

    private GameState() {
        players = new ConcurrentHashMap<>();
        enemies = new ConcurrentHashMap<>();
        enemyIdCounter = new AtomicInteger(1);
    }

    public static synchronized GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    public void addPlayer(String username) {
        players.putIfAbsent(username, new PlayerState(username));
        System.out.println("Player added: " + username + ", total players: " + players.size());
    }

    public void removePlayer(String username) {
        players.remove(username);
        System.out.println("Player removed: " + username + ", total players: " + players.size());
    }

    public void setPlayerAircraft(String username, String aircraftType) {
        PlayerState player = players.get(username);
        if (player != null) {
            player.setAircraftType(aircraftType);
        }
    }

    public void updatePlayerPosition(String username, int x, int y) {
        PlayerState player = players.get(username);
        if (player != null) {
            player.setX(x);
            player.setY(y);
        }
    }

    public void updatePlayerScore(String username, int scoreChange) {
        PlayerState player = players.get(username);
        if (player != null) {
            player.addScore(scoreChange);
        }
    }

    public void damagePlayer(String username, int damage) {
        PlayerState player = players.get(username);
        if (player != null) {
            player.takeDamage(damage);
        }
    }

    public String addEnemy(int x, int y, int health) {
        String enemyId = "enemy_" + enemyIdCounter.getAndIncrement();
        enemies.put(enemyId, new EnemyState(enemyId, x, y, health));
        return enemyId;
    }

    public void removeEnemy(String enemyId) {
        enemies.remove(enemyId);
    }

    public void damageEnemy(String enemyId, int damage, String playerName) {
        EnemyState enemy = enemies.get(enemyId);
        if (enemy != null) {
            int remainingHealth = enemy.takeDamage(damage);

            // If enemy defeated, award points to player
            if (remainingHealth <= 0) {
                PlayerState player = players.get(playerName);
                if (player != null) {
                    player.addScore(100); // 100 points per enemy defeated
                }
                enemies.remove(enemyId);
            }
        }
    }

    public int getPlayerCount() {
        return players.size();
    }

    public String getGameStateAsString() {
        StringBuilder sb = new StringBuilder("GAME_STATE ");

        // Add players section
        sb.append("players:");
        if (!players.isEmpty()) {
            boolean first = true;
            for (PlayerState player : players.values()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(player.toString());
                first = false;
            }
        }

        sb.append(";enemies:");
        if (!enemies.isEmpty()) {
            boolean first = true;
            for (EnemyState enemy : enemies.values()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(enemy.toString());
                first = false;
            }
        }

        return sb.toString();
    }

    // Inner classes for state tracking
    public static class PlayerState {
        private final String name;
        private String aircraftType = "default";
        private int x = 600; // Default start position
        private int y = 700;
        private int health = 100;
        private int score = 0;

        public PlayerState(String name) {
            this.name = name;
        }

        public void setAircraftType(String aircraftType) {
            this.aircraftType = aircraftType;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void takeDamage(int damage) {
            health = Math.max(0, health - damage);
        }

        public void addScore(int points) {
            score += points;
        }

        @Override
        public String toString() {
            return name + ":" + x + ":" + y + ":" + aircraftType + ":" + health + ":" + score;
        }
    }

    public static class EnemyState {
        private final String id;
        private int x;
        private int y;
        private int health;

        public EnemyState(String id, int x, int y, int health) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.health = health;
        }

        public int takeDamage(int damage) {
            health = Math.max(0, health - damage);
            return health;
        }

        @Override
        public String toString() {
            return id + ":" + x + ":" + y + ":" + health;
        }
    }
    public void moveEnemyDown(String enemyId, int speed) {
        EnemyState enemy = enemies.get(enemyId);
        if (enemy != null) {
            enemy.y += speed;
        }
    }

    public int getEnemyY(String enemyId) {
        EnemyState enemy = enemies.get(enemyId);
        if (enemy != null) {
            return enemy.y;
        }
        return 0;
    }

    public Set<String> getEnemyIds() {
        return new HashSet<>(enemies.keySet());
    }
    public int getEnemyX(String enemyId) {
        EnemyState enemy = enemies.get(enemyId);
        if (enemy != null) {
            return enemy.x;
        }
        return 0;
    }

    public void broadcastEnemyRemoval(String enemyId) {
        enemies.remove(enemyId);
        // The broadcast itself is handled in GameServer's startEnemyMovement
    }
}