package chat_Client_Serveur;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.*;
import java.net.*;

public class Serveur {
    private static final int PORT = 7234;
    private static List<Client> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur start");
            while (true) {
                Socket clientsocket = serverSocket.accept();
                System.out.println("New client connected" + clientsocket);
                Client clientThread = new Client(clientsocket);
                clients.add(clientThread);
                new Thread(clientThread).start();
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void broadcast(String message) {
        for (Client client : clients) {
            client.sendMessage(message);
        }
    }
}
