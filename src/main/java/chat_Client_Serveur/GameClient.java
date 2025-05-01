package chat_Client_Serveur;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client de jeu pour se connecter au serveur de jeu multijoueur
 */
public class GameClient {
    // Interface utilisateur
    private JFrame frame;
    private GamePanel gamePanel;
    private JButton connectButton;
    private JTextField usernameField;
    private JComboBox<String> aircraftSelector;
    private JTextArea chatArea;
    private JTextField chatField;
    private JButton sendButton;

    // Réseau
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private boolean connected = false;
    private String username;

    // Données du jeu
    private Map<String, Player> players = new HashMap<>();
    private List<Enemy> enemies = new ArrayList<>();
    private boolean gameStarted = false;
    private Timer gameTimer;
    private boolean waitingForPlayers = true;

    // Constantes du jeu
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int MOVE_SPEED = 5;





    private void listenForMessages() {
        String messageFromServer;

        try {
            while (socket.isConnected() && (messageFromServer = bufferedReader.readLine()) != null) {
                final String message = messageFromServer;

                SwingUtilities.invokeLater(() -> {
                    if (message.startsWith("CHAT ")) {
                        // Message de chat
                        chatArea.append(message.substring(5) + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    else if (message.startsWith("GAMESTATE")) {
                        // Mise à jour de l'état du jeu
                        parseGameState(message);
                        gamePanel.repaint();
                    }
                    else if (message.startsWith("START_GAME")) {
                        gameStarted = true;
                        waitingForPlayers = false;
                        chatArea.append("Le jeu commence!\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    else if (message.startsWith("PAUSE_GAME")) {
                        gameStarted = false;
                        waitingForPlayers = true;
                        chatArea.append("Le jeu est en pause. En attente de joueurs...\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    else if (message.startsWith("WAITING_FOR_PLAYERS")) {
                        String[] parts = message.split(" ");
                        if (parts.length > 1) {
                            waitingForPlayers = true;
                            chatArea.append("En attente de " + parts[1] + " joueur(s) supplémentaire(s)...\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        }
                    }
                    else if (message.startsWith("SERVER:")) {
                        // Message du serveur
                        chatArea.append(message + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                });
            }
        } catch (IOException e) {
            closeConnection();
        }
    }

    private void parseGameState(String gameState) {
        // Vider les listes actuelles
        players.clear();
        enemies.clear();

        // Format: GAMESTATE|PLAYER|username,x,y,score,aircraft,health|ENEMY|x,y,health...
        String[] parts = gameState.split("\\|");

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equals("PLAYER") && i + 1 < parts.length) {
                String[] playerData = parts[i + 1].split(",");
                if (playerData.length >= 6) {
                    String playerName = playerData[0];
                    int x = Integer.parseInt(playerData[1]);
                    int y = Integer.parseInt(playerData[2]);
                    int score = Integer.parseInt(playerData[3]);
                    String aircraft = playerData[4];
                    int health = Integer.parseInt(playerData[5]);

                    // Créer ou mettre à jour le joueur
                    Player player = new Player(playerName, x, y, aircraft, health, score);
                    players.put(playerName, player);
                }
                i++; // Sauter la partie des données du joueur
            }
            else if (parts[i].equals("ENEMY") && i + 1 < parts.length) {
                String[] enemyData = parts[i + 1].split(",");
                if (enemyData.length >= 3) {
                    int x = Integer.parseInt(enemyData[0]);
                    int y = Integer.parseInt(enemyData[1]);
                    int health = Integer.parseInt(enemyData[2]);

                    // Ajouter l'ennemi
                    enemies.add(new Enemy(x, y, health));
                }
                i++; // Sauter la partie des données de l'ennemi
            }
        }
    }

    private void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
            closeConnection();
        }
    }

    private void closeConnection() {
        connected = false;
        gameStarted = false;

        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Classe interne pour le panel de jeu
    private class GamePanel extends JPanel {

        public GamePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Fond
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (connected) {
                if (waitingForPlayers) {
                    // Afficher un message d'attente
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 24));
                    String waitingMessage = "En attente d'autres joueurs...";
                    int messageWidth = g.getFontMetrics().stringWidth(waitingMessage);
                    g.drawString(waitingMessage, (getWidth() - messageWidth) / 2, getHeight() / 2);
                    return;
                }

                // Dessiner les ennemis
                g.setColor(Color.RED);
                for (Enemy enemy : enemies) {
                    drawEnemy(g, enemy);
                }

                // Dessiner les joueurs (avions)
                for (Player player : players.values()) {
                    drawPlayer(g, player);
                }

                // Afficher les scores et la santé en haut de l'écran
                drawScoreboard(g);
            } else {
                // Message si non connecté
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                String connectMessage = "Entrez votre nom et connectez-vous pour jouer";
                int messageWidth = g.getFontMetrics().stringWidth(connectMessage);
                g.drawString(connectMessage, (getWidth() - messageWidth) / 2, getHeight() / 2);
            }
        }

        private void drawEnemy(Graphics g, Enemy enemy) {
            // Dessiner l'ennemi sous forme de triangle rouge
            g.setColor(Color.RED);
            int[] xPoints = {enemy.x, enemy.x - 10, enemy.x + 10};
            int[] yPoints = {enemy.y + 15, enemy.y - 5, enemy.y - 5};
            g.fillPolygon(xPoints, yPoints, 3);

            // Barre de vie
            g.setColor(Color.GREEN);
            g.fillRect(enemy.x - 10, enemy.y - 15, enemy.health * 7, 3);
        }

        private void drawPlayer(Graphics g, Player player) {
            // Couleur en fonction du joueur
            Color playerColor;
            if (player.name.equals(username)) {
                playerColor = Color.BLUE; // Notre joueur est bleu
            } else {
                playerColor = Color.GREEN; // Les autres joueurs sont verts
            }

            // Dessiner l'avion en fonction du type
            if (player.aircraft.equals("fighter")) {
                drawFighterAircraft(g, player, playerColor);
            } else if (player.aircraft.equals("bomber")) {
                drawBomberAircraft(g, player, playerColor);
            } else {
                // Type par défaut
                drawDefaultAircraft(g, player, playerColor);
            }

            // Nom du joueur
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString(player.name, player.x - 25, player.y - 25);

            // Barre de vie
            int healthWidth = (int)(player.health * 0.5);
            g.setColor(Color.RED);
            g.fillRect(player.x - 25, player.y - 20, 50, 5);
            g.setColor(Color.GREEN);
            g.fillRect(player.x - 25, player.y - 20, healthWidth, 5);
        }

        private void drawDefaultAircraft(Graphics g, Player player, Color color) {
            g.setColor(color);
            // Corps principal
            g.fillRect(player.x - 10, player.y - 5, 20, 30);
            // Ailes
            g.fillRect(player.x - 25, player.y + 5, 50, 5);
            // Queue
            g.fillRect(player.x - 5, player.y + 25, 10, 10);
        }

        private void drawFighterAircraft(Graphics g, Player player, Color color) {
            g.setColor(color);
            // Corps principal (plus mince)
            g.fillRect(player.x - 5, player.y - 10, 10, 40);
            // Ailes (plus pointues)
            int[] xPoints = {player.x - 20, player.x, player.x + 20};
            int[] yPoints = {player.y + 15, player.y - 5, player.y + 15};
            g.fillPolygon(xPoints, yPoints, 3);
            // Queue
            int[] xPointsTail = {player.x - 10, player.x, player.x + 10};
            int[] yPointsTail = {player.y + 30, player.y + 20, player.y + 30};
            g.fillPolygon(xPointsTail, yPointsTail, 3);
        }

        private void drawBomberAircraft(Graphics g, Player player, Color color) {
            g.setColor(color);
            // Corps principal (plus gros)
            g.fillOval(player.x - 15, player.y - 5, 30, 40);
            // Ailes (plus larges)
            g.fillRect(player.x - 35, player.y, 70, 10);
            // Queue
            g.fillRect(player.x - 5, player.y + 25, 10, 15);
        }

        private void drawScoreboard(Graphics g) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));

            int y = 20;
            g.drawString("Joueur | Score | Santé", 10, y);
            y += 15;

            for (Player player : players.values()) {
                g.drawString(player.name + " | " + player.score + " | " + player.health, 10, y);
                y += 15;
            }
        }
    }

    // Classe interne pour représenter un joueur
    private static class Player {
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

    // Classe interne pour représenter un ennemi
    private static class Enemy {
        int x;
        int y;
        int health;

        public Enemy(int x, int y, int health) {
            this.x = x;
            this.y = y;
            this.health = health;
        }
    }

    // Point d'entrée du programme
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }
}