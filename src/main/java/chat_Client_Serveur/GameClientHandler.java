package chat_Client_Serveur;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
            this.clientUsername = "";
            clientHandlers.add(this);
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        try {
            while (socket.isConnected() && (messageFromClient = bufferedReader.readLine()) != null) {
                if (messageFromClient.startsWith("USER ")) {
                    handleUserLogin(messageFromClient.substring(5));
                }
                else if (messageFromClient.startsWith("SELECT_AIRCRAFT ")) {
                    handleAircraftSelection(messageFromClient.substring(15));
                }
                else if (messageFromClient.startsWith("GAME_READY")) {
                    handleGameReady();
                }
                else if (messageFromClient.startsWith("MOVE ")) {
                    handlePlayerMovement(messageFromClient.substring(5));
                }
                else if (messageFromClient.startsWith("FIRE")) {

                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void handleUserLogin(String username) {
        this.clientUsername = username;
        gameState.addPlayer(username);
        sendMessage(gameState.getGameStateAsString());

        if (clientHandlers.size() >= 2) {
            broadcastMessage("START_GAME");
            broadcastGameState();
        } else {
            sendMessage("WAITING_FOR_PLAYERS " + (2 - clientHandlers.size()));
        }
    }

    private void handleAircraftSelection(String aircraftType) {
        gameState.setPlayerAircraft(clientUsername, aircraftType);
        broadcastGameState();
    }

    private void handleGameReady() {
        // Envoyer l'état actuel du jeu incluant tous les ennemis
        sendMessage(gameState.getGameStateAsString());
    }

    private void handlePlayerMovement(String moveData) {
        String[] parts = moveData.split(",");
        if (parts.length == 2) {
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                gameState.updatePlayerPosition(clientUsername, x, y);
                broadcastGameState();
            } catch (NumberFormatException e) {

            }
        }
    }


    private void broadcastGameState() {
        String gameStateString = gameState.getGameStateAsString();
        for (GameClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(gameStateString);
        }
    }

    public void broadcastMessage(String message) {
        for (GameClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(message);
        }
    }

    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        if (clientUsername != null && !clientUsername.isEmpty()) {
            gameState.removePlayer(clientUsername);
            broadcastGameState();
        }
    }

    public void closeEverything() {
        removeClientHandler();
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}