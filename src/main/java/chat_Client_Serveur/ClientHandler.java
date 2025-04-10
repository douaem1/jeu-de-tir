package chat_Client_Serveur;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
//it is runned by the thread so we don't have a main methode in here
public class ClientHandler implements Runnable {
    //list of clients : keep track of all our clients so whenever a client sends a mssg we can loop through our arraylist and send the mssg to each client
    //broadcast a mssg to multiple clients instead of just one or just the server
    //static because we want it to belong to the class not every object of the class
    private static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    //socket used to establish the cnx between the client and the server
    private Socket socket;
    //read data ; mssgs that have been sent from the client
    private BufferedReader bufferedReader;
    //send data ; mssgs to our client
    private BufferedWriter bufferedWriter;
    private String ClientUserName;

    //constructeur
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            //we wrapp our output stream(our byte stream) in a char stream because we want to send over chars
            //this is what are we're gonna use to send
            this.bufferedWriter = new BufferedWriter( new OutputStreamWriter(socket.getOutputStream()));//char streams ends with writer and byte stream ends with stream
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//this is what our client is sending
            this.ClientUserName = bufferedReader.readLine();
            clientHandlers.add(this);
            broadcastMssg("Server: " +ClientUserName + " has entered the chat !");
        }catch (IOException e){
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }
    //everything in this run methode is what is run in a separate thread
    //separate thread :  listen to mssgs wich is a blocking operation(prog stuck til the operation is completed)
    @Override
    public void run() {
        //hold a mssg received from a client
        String mssgFromClient;
        //while we're connected to a client let's listen for mssgs
        while (socket.isConnected()) {
            //read from our bufferedReader
            try {
                //prog will hold here til we receive a mssg from a client
                //we want to run this on a separate thread so the rest of our game isn't stopped by this line (blocking operation)
                mssgFromClient = bufferedReader.readLine();
                broadcastMssg(mssgFromClient);
            }catch (IOException e){
                closeEverything(socket,bufferedReader,bufferedWriter);
                //when the client disconnects this will break us out of the while loop
                break;
            }
        }

    }
    public void broadcastMssg(String mssg){
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                //we want to broadcast the mssg to everyone except the user who sent it
                if (!clientHandler.ClientUserName.equals(ClientUserName)) {
                    clientHandler.bufferedWriter.write(mssg);
                    //send over a new linw char
                    clientHandler.bufferedWriter.newLine();
                    //flush our bufferedWriter:buffer needs to be full before it is sent and the mssg maybe will be too short for filling it so we have to flush manually
                    clientHandler.bufferedWriter.flush();
                }
            }catch (IOException e){
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
    }
    //methode to signal that a user left the chat : user disconnected
    public void removeClientHandler(){
        //remove the clienthandler from the arraylist
        clientHandlers.remove(this);
        broadcastMssg("Server: " +ClientUserName + " has left the chat !");
    }
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
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
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}