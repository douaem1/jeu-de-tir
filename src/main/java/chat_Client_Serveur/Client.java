package chat_Client_Serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {
    private Socket clientsocket;
    private PrintWriter out;
    private BufferedReader in;

    public Client(Socket socket) {
        this.clientsocket = socket;
    }
    @Override
    public void run() {
        try {
            out = new PrintWriter(clientsocket.getOutputStream(),true);
            in = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));

            String message ;
            while ((message = in.readLine()) != null) {
                System.out.println("Message received : "+message);
                Serveur.broadcast(message);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendMessage(String message) {
        out.println(message);
    }
}
