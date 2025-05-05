package Game;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiplayerServer extends Thread {

    private final int port;
    private final GameManager gameManager;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<String, String> playerAircrafts = new ConcurrentHashMap<>();
    private final AtomicInteger enemyIdCounter = new AtomicInteger(0);

    public MultiplayerServer(int port, GameManager gameManager) {
        this.port = port;
        this.gameManager = gameManager;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Serveur démarré sur le port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nouveau client connecté: " + clientSocket.getInetAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.start();

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Erreur lors de l'acceptation d'une connexion: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
        }
    }

    public void stopServer() {
        running = false;

        for (ClientHandler client : clients) {
            client.close();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void updatePlayerList() {
        StringBuilder playerList = new StringBuilder("PLAYER_LIST:");
        boolean first = true;

        for (ClientHandler client : clients) {
            if (client.username != null && !client.username.isEmpty()) {
                if (!first) {
                    playerList.append(",");
                }
                playerList.append(client.username);
                first = false;
            }
        }

        broadcastMessage(playerList.toString());
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private boolean isRunning = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String inputLine;
                while (isRunning && (inputLine = in.readLine()) != null) {
                    processMessage(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Erreur avec un client: " + e.getMessage());
            } finally {
                close();
            }
        }

        private void processMessage(String message) {
            System.out.println("Message du client: " + message);

            if (message.startsWith("JOIN:")) {
                // Format: JOIN:username,aircraftType
                String[] parts = message.split(":", 2)[1].split(",");
                username = parts[0];
                String aircraftType = parts[1];
                playerAircrafts.put(username, aircraftType);

                System.out.println("Joueur connecté: " + username + " avec l'avion: " + aircraftType);

                // Informer tous les autres clients qu'un nouveau joueur a rejoint
                broadcastMessage("PLAYER_JOIN:" + username + "," + aircraftType);

                // Envoyer la liste des joueurs actuels
                updatePlayerList();

            } else if (message.startsWith("LEAVE:")) {
                // Format: LEAVE:username
                String leavingUsername = message.split(":", 2)[1];
                playerAircrafts.remove(leavingUsername);

                // Informer tous les autres clients qu'un joueur est parti
                broadcastMessage("PLAYER_LEAVE:" + leavingUsername);

                // Mettre à jour la liste des joueurs
                updatePlayerList();

            } else if (message.equals("START_GAME")) {
                // L'hôte démarre la partie
                broadcastMessage("START_GAME");

            } else if (message.startsWith("PLAYER_POS:")) {
                // Rediffuser la position à tous les autres clients
                broadcastMessage(message);

            } else if (message.startsWith("PLAYER_FIRE:")) {
                // Rediffuser le tir à tous les autres clients
                broadcastMessage(message);

            } else if (message.equals("SYNC_ENEMIES")) {
                // Synchroniser les ennemis pour un nouveau joueur
                // (Si nous implémentons la logique côté serveur)

            } else if (message.startsWith("CREATE_ENEMY:")) {
                // L'hôte crée un ennemi
                // Format: CREATE_ENEMY:x,y
                String enemyId = "enemy-" + enemyIdCounter.incrementAndGet();
                String[] parts = message.split(":", 2)[1].split(",");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                // Diffuser à tous les clients
                broadcastMessage("SYNC_ENEMY:" + enemyId + "," + x + "," + y);
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public void close() {
            try {
                isRunning = false;

                if (username != null && !username.isEmpty()) {
                    broadcastMessage("PLAYER_LEAVE:" + username);
                    updatePlayerList();
                }

                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }

                clients.remove(this);

            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture d'un client: " + e.getMessage());
            }
        }
    }
}