package socketchat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
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

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;
        private File receivedFile;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private String readUntilEndOfChunk() throws IOException {
            StringBuilder sb = new StringBuilder();
            int ch;
            String endOfChunk = "END_OF_CHUNK";
            int endIndex = 0;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
                if ((char) ch == endOfChunk.charAt(endIndex)) {
                    endIndex++;
                    if (endIndex == endOfChunk.length()) {
                        // Found the end of chunk, remove it from the result and return
                        sb.setLength(sb.length() - endIndex);
                        break;
                    }
                } else {
                    endIndex = 0;
                }
            }
            if (sb.length() == 0) {
                return null;
            }
            return sb.toString();
        }
    

        @Override
        public void run() {
            try {
                String message;
                while ((message = readUntilEndOfChunk()) != null) {
                    if (message.startsWith("FILE_TRANSFER_REQUEST")) {
                        String[] parts = message.split(" ");
                        String fileName = parts[1];
                        long fileSize = Long.parseLong(parts[2]);
                        String data = readUntilEndOfChunk();  // read the Base64 string data
                        receiveFile(fileName, fileSize, data);
                        System.out.println("File received.");
                        broadcastFile(this);  // Added: Broadcast file after receiving
                    } else {
                        System.out.println("Received message: " + message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error occurred while reading from the socket: " + e.getMessage());
            } finally {
                try {
                    // Wait before closing connection to allow "FILE_TRANSFER_COMPLETE" to be sent
                    Thread.sleep(1000);
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error occurred while closing the socket: " + e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void receiveFile(String fileName, long fileSize, String data) throws IOException {
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            receivedFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            fos.write(decodedBytes);
            fos.close();
        }

        public void sendMessage(String message) {
            try {
                writer.write(message);
                writer.write("END_OF_CHUNK");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendFile(File file) {
            try {
                writer.write("FILE_TRANSFER_REQUEST " + file.getName() + " " + file.length());
                writer.write("END_OF_CHUNK");
                writer.flush();

                byte[] buffer = new byte[1024];
                int bytesRead;
                FileInputStream fis = new FileInputStream(file);
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] actualBytes = Arrays.copyOf(buffer, bytesRead);
                    String base64Bytes = Base64.getEncoder().encodeToString(actualBytes);
                    writer.write(base64Bytes);
                    writer.write("END_OF_CHUNK");
                    writer.flush();
                }
                fis.close();

                writer.write("FILE_TRANSFER_COMPLETE");
                writer.write("END_OF_CHUNK");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Added: Method to broadcast file
    public void broadcastFile(ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendFile(sender.receivedFile);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server(6665);
        server.start();
    }
}
