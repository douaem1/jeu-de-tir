package chat_Client_Serveur;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

//listening to clients who wish to connect and when they do it will spawn a new thread to handle them
//each object of this class will be responsible for communicating with a client
//the client class will implement the interface runnable(each instance of this class will be excecuted by a separate thread)
public class Serveur {

    private ServerSocket serverSocket;//this object will be responsible for listening for incoming cnxs(clients)and creating a socket object to communicate with them
    //constructeur
    public Serveur(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    //methode:keep our server running
    public void startServeur() {
        //input output error handling
        try {
            while (!serverSocket.isClosed()) {//while the serversocket isn't close we're gonna wait for a client to connect by a serversocket that accept methode
                //blocking methode : our methode will be stopped here until a client connects
                //when a client connects a socket object is returned wich used to communicate with the client
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected to the server !");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }catch (IOException e){

        }

    }
    //avoid nested try catches(if an error accures we are gonna shut down our serversocket)
    public void closeServerSocket() {
        try {
            //make sure that our server socket is not null(cause if it is we're gonna have a null pointer exception)
            if (serverSocket != null) {
                serverSocket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    //main methode to intantiate object from it and run it
    public static void main(String[] args) throws IOException {
        //port number is 7103: it is saying that our server will be listening for clients that are making cnx to this port num
        //client will say hey i wanna talk on port num 7103
        ServerSocket serverSocket = new ServerSocket(7103);
        Serveur serveur = new Serveur(serverSocket);
        serveur.startServeur();//appel of function that we made up
    }

}