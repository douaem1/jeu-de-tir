package chat_Client_Serveur;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    //constructeur
    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.username = username;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }catch (IOException e){
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }
    //send mssgt to our clientHandler
    public void sendMessage() {
        try {
            //1st:send the client username over
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            //get input from the consol
            Scanner scanner = new Scanner(System.in);
            while(socket.isConnected()){
                //when the user presses enter in the terminal after typing in that will be captured in this variable
                String messageToSend = scanner.nextLine();
                if (messageToSend.startsWith("/")){
                    bufferedWriter.write(messageToSend);//mssg privé
                }else {
                    bufferedWriter.write(username + " : " + messageToSend);//mssg public
                }
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }catch (IOException e){
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }
    //listen to mssgs: blocking operation -> use thread (create a new thread and pass the runnable object without implementing the runnable interface )
    public void listenForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String mssgFromGrChat;
                while (socket.isConnected()) {
                    try {
                        //read the broadcast mssg then output it
                        mssgFromGrChat = bufferedReader.readLine();
                        if (mssgFromGrChat != null) {
                            if (mssgFromGrChat.contains("[Private]")) {
                                System.out.println("\u001B[35m"+mssgFromGrChat+"\u001B[0m");//violet
                            } else if (mssgFromGrChat.startsWith("Connected players: ")) {
                                System.out.println("\u001B[36m" + mssgFromGrChat + "\u001B[0m");
                            } else {
                                System.out.println(mssgFromGrChat);
                            }
                        }
                    }catch (IOException e){
                        closeEverything(socket,bufferedReader,bufferedWriter);
                    }
                }
            }
        }).start();
    }
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
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
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);//we take input from keyboard
        System.out.println("Enter your username (write /pm : for private message and /list : for users) : ");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost" , 7103);
        Client client = new Client(socket,username);
        client.listenForMessages();
        client.sendMessage();
    }

}