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
    private static final String[] AIRCRAFT_TYPES = {"default", "fighter", "bomber"}; // Types d'avions disponibles
    private static final int DEFAULT_HEALTH = 100; // Santé par défaut
    private static final int ENEMY_SPAWN_DELAY = 2000; // Délai entre les apparitions d'ennemis (en ms)
    private static final int MAX_ENEMIES = 10; // Nombre maximum d'ennemis à l'écran

    // Constructeur privé (pattern Singleton)
    private GameState() {
        playerPositions = new ConcurrentHashMap<>();
        playerScores = new ConcurrentHashMap<>();
        playerAircrafts = new ConcurrentHashMap<>();
        playerHealth = new ConcurrentHashMap<>();
        enemies = new ArrayList<>();
        random = new Random();
        lastEnemySpawnTime = System.currentTimeMillis();
    }

    // Méthode pour obtenir l'instance unique
    public static synchronized GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    // Ajouter un nouveau joueur
    public void addPlayer(String username) {
        // Positionner le joueur à un endroit aléatoire mais sécurisé (éviter les bords)
        int x = random.nextInt(GAME_WIDTH - 100) + 50;
        int y = GAME_HEIGHT - 100; // Commencer en bas de l'écran
        playerPositions.put(username, new Position(x, y));

        // Initialiser le score à 0
        playerScores.put(username, 0);

        // Attribuer un type d'avion par défaut
        playerAircrafts.put(username, "default");

        // Initialiser la santé
        playerHealth.put(username, DEFAULT_HEALTH);
    }

    // Supprimer un joueur
    public void removePlayer(String username) {
        playerPositions.remove(username);
        playerScores.remove(username);
        playerAircrafts.remove(username);
        playerHealth.remove(username);
    }

    // Mettre à jour la position d'un joueur avec vérification des limites
    public void updatePlayerPosition(String username, int x, int y) {
        // S'assurer que la position reste dans les limites du jeu
        x = Math.max(0, Math.min(GAME_WIDTH, x));
        y = Math.max(0, Math.min(GAME_HEIGHT, y));

        // Mettre à jour la position
        playerPositions.put(username, new Position(x, y));
    }

    // Définir le type d'avion pour un joueur
    public void setPlayerAircraft(String username, String aircraftType) {
        if (playerAircrafts.containsKey(username)) {
            // Vérifier que le type d'avion est valide
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

    // Mettre à jour le score d'un joueur
    public void updatePlayerScore(String username, int points) {
        int currentScore = playerScores.getOrDefault(username, 0);
        playerScores.put(username, currentScore + points);
    }

    // Mettre à jour la santé d'un joueur
    public void updatePlayerHealth(String username, int healthChange) {
        int currentHealth = playerHealth.getOrDefault(username, DEFAULT_HEALTH);
        int newHealth = Math.max(0, currentHealth + healthChange); // Ne peut pas descendre sous 0
        playerHealth.put(username, newHealth);

        // Si la santé atteint 0, le joueur perd des points
        if (newHealth == 0) {
            updatePlayerScore(username, -50); // Pénalité de score pour la "mort"
            playerHealth.put(username, DEFAULT_HEALTH); // Réinitialiser la santé

            // Repositionner le joueur
            int x = random.nextInt(GAME_WIDTH - 100) + 50;
            int y = GAME_HEIGHT - 100;
            playerPositions.put(username, new Position(x, y));
        }
    }

    // Générer des ennemis
    public void spawnEnemies() {
        long currentTime = System.currentTimeMillis();

        // Vérifier s'il est temps de générer un nouvel ennemi
        if (currentTime - lastEnemySpawnTime > ENEMY_SPAWN_DELAY && enemies.size() < MAX_ENEMIES) {
            int x = random.nextInt(GAME_WIDTH - 50) + 25;
            int y = 0; // Commencer en haut de l'écran
            int speedX = random.nextInt(5) - 2; // Vitesse horizontale entre -2 et 2
            int speedY = random.nextInt(3) + 1; // Vitesse verticale entre 1 et 3
            int health = random.nextInt(3) + 1; // Santé entre 1 et 3

            enemies.add(new Enemy(x, y, speedX, speedY, health));
            lastEnemySpawnTime = currentTime;
        }
    }

    // Mettre à jour les positions des ennemis
    public void moveEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();

        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.move();

            // Supprimer l'ennemi s'il sort de l'écran
            if (enemy.y > GAME_HEIGHT) {
                iterator.remove();
            }
        }
    }

    // Gérer les collisions
    public void checkCollisions() {
        // Pour chaque joueur
        for (Map.Entry<String, Position> entry : playerPositions.entrySet()) {
            String username = entry.getKey();
            Position playerPos = entry.getValue();

            // Vérifier les collisions avec les ennemis
            Iterator<Enemy> iterator = enemies.iterator();
            while (iterator.hasNext()) {
                Enemy enemy = iterator.next();

                // Calcul simple de collision (cercle)
                int distance = (int) Math.sqrt(Math.pow(playerPos.x - enemy.x, 2) + Math.pow(playerPos.y - enemy.y, 2));

                if (distance < 30) { // Rayon de collision à ajuster selon les sprites
                    // Le joueur perd de la santé
                    updatePlayerHealth(username, -20);

                    // L'ennemi perd de la santé
                    enemy.health--;

                    // Supprimer l'ennemi s'il n'a plus de santé
                    if (enemy.health <= 0) {
                        iterator.remove();
                        updatePlayerScore(username, 10); // Points pour avoir détruit un ennemi
                    }
                }
            }
        }
    }

    // Obtenir la position d'un joueur
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

    // Classe interne pour représenter un ennemi
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