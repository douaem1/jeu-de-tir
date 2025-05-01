package chat_Client_Serveur;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de clients pour le serveur de jeu
 */
public class GameClientHandler implements Runnable {
    public static List<GameClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private GameState gameState;

    public GameClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.gameState = GameState.getInstance();

            // Le nom d'utilisateur sera défini lorsque nous recevrons le message USER
            this.clientUsername = "";

            // Ajouter ce gestionnaire à la liste
            clientHandlers.add(this);

            // Diffuser un message indiquant qu'un nouveau joueur s'est connecté
            broadcastMessage("SERVER: Un nouveau joueur s'est connecté au serveur!");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        try {
            while (socket.isConnected() && (messageFromClient = bufferedReader.readLine()) != null) {
                // Traiter différents types de messages
                if (messageFromClient.startsWith("USER ")) {
                    // Traiter le message d'identification de l'utilisateur
                    this.clientUsername = messageFromClient.substring(5);
                    broadcastMessage("SERVER: " + clientUsername + " a rejoint le jeu!");

                    // Ajouter le joueur à l'état du jeu
                    gameState.addPlayer(clientUsername);

                    // Envoyer un message pour démarrer le jeu immédiatement si assez de joueurs
                    if (clientHandlers.size() >= 2) {
                        broadcastMessage("START_GAME");
                    } else {
                        sendMessage("WAITING_FOR_PLAYERS " + (2 - clientHandlers.size()));
                    }

                } else if (messageFromClient.startsWith("SELECT_AIRCRAFT ")) {
                    // Traiter la sélection d'avion
                    String aircraftType = messageFromClient.substring(15);

                    // Mettre à jour le type d'avion du joueur
                    gameState.setPlayerAircraft(clientUsername, aircraftType);

                    broadcastMessage("SERVER: " + clientUsername + " a choisi l'avion " + aircraftType);

                    // Diffuser le nouvel état du jeu
                    broadcastGameState();

                } else if (messageFromClient.startsWith("GAME_READY")) {
                    // Le client est prêt à jouer
                    broadcastMessage("SERVER: " + clientUsername + " est prêt à jouer!");

                    // Envoyer l'état actuel du jeu à ce client
                    sendMessage(gameState.getGameStateAsString());

                } else if (messageFromClient.startsWith("MOVE ")) {
                    // Traiter le message de mouvement
                    String[] parts = messageFromClient.substring(5).split(",");
                    if (parts.length == 2) {
                        try {
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);

                            // Mettre à jour la position du joueur
                            gameState.updatePlayerPosition(clientUsername, x, y);

                            // Diffuser le nouvel état du jeu
                            broadcastGameState();
                        } catch (NumberFormatException e) {
                            // Ignorer les messages mal formatés
                        }
                    }
                } else if (messageFromClient.startsWith("FIRE")) {
                    // Traiter les tirs du joueur
                    // Implémenter la logique de tir ici
                    // Pour l'instant, nous diffusons juste un message indiquant que le joueur a tiré
                    broadcastMessage("SERVER: " + clientUsername + " a tiré!");

                } else if (messageFromClient.startsWith("CHAT ")) {
                    // Message de chat ordinaire - le diffuser à tous les clients
                    broadcastMessage("CHAT " + clientUsername + ": " + messageFromClient.substring(5));
                }
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Diffuser l'état du jeu à tous les clients
    private void broadcastGameState() {
        String gameStateString = gameState.getGameStateAsString();
        for (GameClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.bufferedWriter.write(gameStateString);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    // Diffuser un message à tous les clients
    public void broadcastMessage(String message) {
        for (GameClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.bufferedWriter.write(message);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    // Envoyer un message à ce client seulement
    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Supprimer ce gestionnaire de la liste
    public void removeClientHandler() {
        clientHandlers.remove(this);
        if (clientUsername != null && !clientUsername.isEmpty()) {
            broadcastMessage("SERVER: " + clientUsername + " a quitté le jeu!");

            // Supprimer le joueur de l'état du jeu
            gameState.removePlayer(clientUsername);

            // Diffuser le nouvel état du jeu
            broadcastGameState();
        }
    }

    // Fermer toutes les ressources
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}