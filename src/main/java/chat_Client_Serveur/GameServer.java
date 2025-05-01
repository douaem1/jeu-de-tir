package chat_Client_Serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Serveur de jeu multijoueur
 * Gère les connexions des clients et assure la synchronisation du jeu
 */
public class GameServer {
    private ServerSocket serverSocket;
    private Timer gameUpdateTimer;
    private GameState gameState;
    private boolean gameStarted = false;
    private static final int REQUIRED_PLAYERS = 2; // Nombre de joueurs requis pour démarrer le jeu
    private static final int UPDATE_RATE = 50; // Fréquence de mise à jour du jeu en ms (20 FPS)

    // Constructeur
    public GameServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.gameUpdateTimer = new Timer();
        this.gameState = GameState.getInstance();

        // Démarrer une tâche qui s'exécute périodiquement pour mettre à jour l'état du jeu
        this.gameUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Vérifier si le jeu doit démarrer (si on a assez de joueurs)
                checkGameStart();

                // Mettre à jour le jeu uniquement s'il est démarré
                if (gameStarted) {
                    updateGame();
                }
            }
        }, 0, UPDATE_RATE);
    }

    // Vérifier si le jeu doit démarrer ou s'arrêter
    private void checkGameStart() {
        int playerCount = GameClientHandler.clientHandlers.size();

        // Démarrer le jeu quand le nombre requis de joueurs est atteint
        if (!gameStarted && playerCount >= REQUIRED_PLAYERS) {
            gameStarted = true;
            System.out.println("Nombre de joueurs requis atteint (" + REQUIRED_PLAYERS + "). Démarrage du jeu!");

            // Envoyer un message à tous les clients pour démarrer le jeu
            for (GameClientHandler clientHandler : GameClientHandler.clientHandlers) {
                clientHandler.sendMessage("START_GAME");
            }
        }

        // Arrêter le jeu si on descend en-dessous du minimum requis
        else if (gameStarted && playerCount < REQUIRED_PLAYERS) {
            gameStarted = false;
            System.out.println("Nombre de joueurs insuffisant. Mise en pause du jeu.");

            // Envoyer un message à tous les clients pour mettre le jeu en pause
            for (GameClientHandler clientHandler : GameClientHandler.clientHandlers) {
                clientHandler.sendMessage("PAUSE_GAME");
            }
        }

        // Si on n'a pas assez de joueurs, envoyer un message d'attente
        else if (!gameStarted && playerCount > 0 && playerCount < REQUIRED_PLAYERS) {
            // Informer les clients qu'on attend plus de joueurs
            for (GameClientHandler clientHandler : GameClientHandler.clientHandlers) {
                clientHandler.sendMessage("WAITING_FOR_PLAYERS " + (REQUIRED_PLAYERS - playerCount));
            }
        }
    }

    // Mise à jour périodique du jeu
    private void updateGame() {
        // Générer des ennemis selon une probabilité
        gameState.spawnEnemies();

        // Déplacer les ennemis existants
        gameState.moveEnemies();

        // Vérifier les collisions entre joueurs et ennemis
        gameState.checkCollisions();

        // Diffuser le nouvel état du jeu à tous les clients
        if (!GameClientHandler.clientHandlers.isEmpty()) {
            String gameStateString = gameState.getGameStateAsString();
            for (GameClientHandler clientHandler : GameClientHandler.clientHandlers) {
                clientHandler.sendMessage(gameStateString);
            }
        }
    }

    // Démarrer le serveur
    public void startServer() {
        try {
            System.out.println("Serveur en attente de connexions sur le port " + serverSocket.getLocalPort());

            while (!serverSocket.isClosed()) {
                // Attendre qu'un client se connecte
                Socket socket = serverSocket.accept();
                System.out.println("Un nouveau joueur s'est connecté au serveur!");

                // Créer un handler pour ce client et le démarrer dans un thread séparé
                GameClientHandler clientHandler = new GameClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();

                // Informer tous les clients du nombre actuel de joueurs
                for (GameClientHandler handler : GameClientHandler.clientHandlers) {
                    handler.sendMessage("PLAYER_COUNT " + GameClientHandler.clientHandlers.size());
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur dans le serveur: " + e.getMessage());
            closeServerSocket();
        }
    }

    // Fermer le socket du serveur
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }

            // Arrêter le timer
            if (gameUpdateTimer != null) {
                gameUpdateTimer.cancel();
            }

            System.out.println("Serveur arrêté");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthode principale
    public static void main(String[] args) throws IOException {
        int port = 7103;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            GameServer server = new GameServer(serverSocket);
            System.out.println("Serveur de jeu démarré sur le port " + port);
            server.startServer();
        } catch (IOException e) {
            System.err.println("Impossible de démarrer le serveur sur le port " + port);
            e.printStackTrace();
        }
    }
}