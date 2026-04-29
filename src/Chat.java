import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

class Chat {
    public static void main() {
        final int port = 5678;
        System.out.println("Starting Client");
        try {
            new Client(port, "localhost");
        }
        catch (IOException e) {
            System.out.println("Client failed, starting Server");
            new Server(port);
        }
    }
}

class Client {
    Socket mySocket;
    Client (int port, String ip) throws IOException{
        mySocket = new Socket(ip, port);
    }
}

class Server{
    Server (int port){
        try (ServerSocket myServerSocket = new ServerSocket(port);
            Socket clientSocket = myServerSocket.accept()){

            System.out.println("Klient anslöt!");
        }
        catch (IOException e){
            System.out.println("Kan inte ansluta till servern" + e.getMessage());
        }

    }
}