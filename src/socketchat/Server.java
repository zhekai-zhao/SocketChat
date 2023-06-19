
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
                ClientHandler clientHandler = new ClientHandler(socket);
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

    public void broadcastFile(String fileName, ClientHandler sender) {
        File file = new File(fileName);
        if (!file.exists() || !file.isFile()) {
            sender.sendMessage("File does not exist or is not a regular file.");
            return;
        }
        
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendFile(file);
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = reader.readLine();
                    if (message == null) {
                        break;
                    }
                    if (message.startsWith("FILE_TRANSFER_REQUEST ")) {
                        String fileName = message.substring("FILE_TRANSFER_REQUEST ".length()).trim();
                        broadcastFile(fileName, this);
                    } else {
                        broadcastMessage(message, this);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

     
        public void sendFile(File file) {
            try {
                // Inform the client that a file is available for transfer
                writer.write("FILE_AVAILABLE " + file.getName() + " " + file.length());
                writer.newLine();
                writer.flush();

                // Actually send the file
                FileInputStream fis = new FileInputStream(file);
                OutputStream os = socket.getOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    
    public static void main(String[] args) {
        Server server = new Server(6666);
        server.start();
    }
}
