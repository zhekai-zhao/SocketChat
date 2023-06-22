package socketchat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> clients;
    private volatile boolean isRunning = true;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new CopyOnWriteArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Server started.");
        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                clientHandler.start();
            } catch (IOException e) {
                if (isRunning) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastFile(ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendFile(sender.getReceivedFile());
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server(6667);
        server.start();
    }
}
