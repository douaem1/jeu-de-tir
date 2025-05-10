package chat_Client_Serveur;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class RadminNetworkManager {

    public GameManager gameManager;

    private String playerName;
    private String aircraftType;
    private int PORT = 9876;
    // Port standard pour le jeu

    // Pour les connexions entrantes
    private ServerSocket serverSocket;
    private ExecutorService connectionPool;

    // Pour les connexions aux autres joueurs
    private Map<String, Socket> playerConnections = new ConcurrentHashMap<>();
    private Map<String, PrintWriter> playerOutputStreams = new ConcurrentHashMap<>();

    // Liste des joueurs connectés au réseau
    private Set<String> connectedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RadminNetworkManager(GameManager gameManager, String playerName, String aircraftType) {
        this.gameManager = gameManager;
        this.playerName = playerName;
        this.aircraftType = aircraftType;
        this.connectionPool = Executors.newCachedThreadPool();
    }



    /**
     * Démarre le serveur qui attend les connexions entrantes
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Serveur de jeu démarré sur le port " + PORT);

            // Thread d'acceptation des connexions
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleNewConnection(clientSocket);
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.err.println("Erreur d'acceptation de connexion: " + e.getMessage());
                        }
                    }
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Impossible de démarrer le serveur: " + e.getMessage());
        }
    }

    /**
     * Traite une nouvelle connexion entrante
     */
    private void handleNewConnection(Socket socket) {
        connectionPool.submit(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Première ligne = CONNECT:nom_joueur,type_avion
                String connectionRequest = in.readLine();
                if (connectionRequest != null && connectionRequest.startsWith("CONNECT:")) {
                    String[] parts = connectionRequest.substring("CONNECT:".length()).split(",");
                    if (parts.length >= 2) {
                        String remotePlayerName = parts[0];
                        String remoteAircraftType = parts[1];

                        // Enregistrer la connexion
                        playerConnections.put(remotePlayerName, socket);
                        playerOutputStreams.put(remotePlayerName, out);
                        connectedPlayers.add(remotePlayerName);

                        // Envoyer notification d'ajout de joueur
                        gameManager.handlePlayerJoin(remotePlayerName, remoteAircraftType);

                        // Envoyer notre propre info à ce joueur
                        out.println("PLAYER_JOIN:" + playerName + "," + aircraftType);

                        // Écouter les messages de ce joueur
                        String message;
                        while ((message = in.readLine()) != null) {
                            final String finalMessage = message;
                            javafx.application.Platform.runLater(() -> {
                                gameManager.handleServerMessage(finalMessage);
                            });
                        }

                        // Si on sort de la boucle, c'est que la connexion est fermée
                        disconnectPlayer(remotePlayerName);
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur de communication avec un client: " + e.getMessage());
                // Trouver le joueur qui correspond à cette socket pour le déconnecter
                for (Map.Entry<String, Socket> entry : playerConnections.entrySet()) {
                    if (entry.getValue() == socket) {
                        disconnectPlayer(entry.getKey());
                        break;
                    }
                }
            }
        });
    }

    /**
     * Se connecte à un autre joueur via son adresse IP
     */
    public boolean connectToPlayer(String ipAddress) {
        try {
            Socket socket = new Socket(ipAddress, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Envoyer demande de connexion
            out.println("CONNECT:" + playerName + "," + aircraftType);

            // Configurer la réception des messages
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Créer un thread pour recevoir les messages
            connectionPool.submit(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("PLAYER_JOIN:")) {
                            String[] parts = message.substring("PLAYER_JOIN:".length()).split(",");
                            if (parts.length >= 2) {
                                String remotePlayerName = parts[0];
                                String remoteAircraftType = parts[1];

                                // Enregistrer la connexion
                                playerConnections.put(remotePlayerName, socket);
                                playerOutputStreams.put(remotePlayerName, out);
                                connectedPlayers.add(remotePlayerName);

                                // Notifier l'interface
                                final String finalRemotePlayerName = remotePlayerName;
                                final String finalRemoteAircraftType = remoteAircraftType;
                                javafx.application.Platform.runLater(() -> {
                                    gameManager.handlePlayerJoin(finalRemotePlayerName, finalRemoteAircraftType);
                                });
                            }
                        } else {
                            final String finalMessage = message;
                            javafx.application.Platform.runLater(() -> {
                                gameManager.handleServerMessage(finalMessage);
                            });
                        }
                    }

                    // Si on sort de la boucle, la connexion est fermée
                    // Trouver quel joueur correspond à cette socket
                    for (Map.Entry<String, Socket> entry : playerConnections.entrySet()) {
                        if (entry.getValue() == socket) {
                            disconnectPlayer(entry.getKey());
                            break;
                        }
                    }

                } catch (IOException e) {
                    System.err.println("Erreur de réception: " + e.getMessage());
                    // En cas d'erreur, déconnecter tous les joueurs liés à cette socket
                    List<String> playersToRemove = new ArrayList<>();
                    for (Map.Entry<String, Socket> entry : playerConnections.entrySet()) {
                        if (entry.getValue() == socket) {
                            playersToRemove.add(entry.getKey());
                        }
                    }
                    for (String player : playersToRemove) {
                        disconnectPlayer(player);
                    }
                }
            });

            return true;
        } catch (IOException e) {
            System.err.println("Impossible de se connecter à " + ipAddress + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Gère la déconnexion d'un joueur
     */
    private void disconnectPlayer(String playerName) {
        Socket socket = playerConnections.remove(playerName);
        PrintWriter out = playerOutputStreams.remove(playerName);
        connectedPlayers.remove(playerName);

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erreur à la fermeture du socket: " + e.getMessage());
            }
        }

        // Notifier l'interface
        javafx.application.Platform.runLater(() -> {
            gameManager.handlePlayerLeave(playerName);
        });
    }

    /**
     * Envoie un message à tous les joueurs connectés
     */
    public void broadcast(String message) {
        for (PrintWriter out : playerOutputStreams.values()) {
            out.println(message);
            out.flush();
        }
    }

    /**
     * Envoie un message à un joueur spécifique
     */
    public void sendToPlayer(String playerName, String message) {
        PrintWriter out = playerOutputStreams.get(playerName);
        if (out != null) {
            out.println(message);
            out.flush();
        }
    }

    /**
     * Ferme toutes les connexions
     */
    public void shutdown() {
        for (Socket socket : playerConnections.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignorer les erreurs de fermeture
            }
        }

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignorer les erreurs de fermeture
        }

        connectionPool.shutdown();
        System.out.println("Fermeture de toutes les connexions réseau");
    }

    /**
     * Retourne la liste des joueurs connectés
     */
    public Set<String> getConnectedPlayers() {
        return new HashSet<>(connectedPlayers);
    }

    /**
     * Interface GameManager pour gérer les événements du jeu
     */
    public interface GameManager {
        void handlePlayerJoin(String playerName, String aircraftType);
        void handlePlayerLeave(String playerName);
        void handleServerMessage(String message);
    }
    class ConcurrentHashSet<E> extends AbstractSet<E> {
        private final ConcurrentHashMap<E, Boolean> map;

        public ConcurrentHashSet() {
            map = new ConcurrentHashMap<>();
        }

        @Override
        public boolean add(E e) {
            return map.put(e, Boolean.TRUE) == null;
        }

        @Override
        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        @Override
        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        @Override
        public Iterator<E> iterator() {
            return map.keySet().iterator();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public void clear() {
            map.clear();
        }
    }
}