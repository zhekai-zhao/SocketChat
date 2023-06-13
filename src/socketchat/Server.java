package socketchat;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Server started.");
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                clientHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                OutputStream os = socket.getOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                bis.close();
                fis.close();

                // Send "SUCCESS" message after successful file transfer
                sendMessage("SUCCESS");

                // Notify other clients about the file transfer completion
                String message = "File received: " + file.getName();
                broadcastMessage(message, this);
                System.out.println("File sent");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public Socket getSocket() {
            return socket;
        }
    }
    
    public static void main(String[] args) {
        Server server = new Server(6666);
        server.start();
    }
}
