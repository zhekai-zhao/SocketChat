
package socketchat;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> clients;
    private File receivedFile;
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

    public void broadcastFile(ClientHandler sender) {
        if (receivedFile == null || !receivedFile.exists() || !receivedFile.isFile()) {
            sender.sendMessage("File does not exist or is not a regular file.");
            return;
        }
        
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendFile(receivedFile);
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
                        receivedFile = new File(fileName); // create file for the received file
                        FileOutputStream fos = new FileOutputStream(receivedFile);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        InputStream is = socket.getInputStream();

                        while (!(message = reader.readLine()).equals("FILE_TRANSFER_COMPLETE")) {
                            bytesRead = is.read(buffer);
                            if (bytesRead == -1) {
                                break;
                            }
                            bos.write(buffer, 0, bytesRead);
                        }

                        // Wait for a short time to ensure all data has been read
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        bos.flush();
                        bos.close();
                        fos.close();

                        // Confirm that the file transfer is complete
                        sendMessage("FILE_RECEIVE_COMPLETE");

                        broadcastFile(this); // broadcast the file after it's received completely
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
                    System.out.println("Sent " + bytesRead + " bytes to client."); // Add debug info
                }
                fis.close();
            } catch (IOException e) {
                System.err.println("Error occurred while sending file: " + e.getMessage()); // Change this line
            }
        }

    }
    
    public static void main(String[] args) {
        Server server = new Server(9999);
        server.start();
    }
}
