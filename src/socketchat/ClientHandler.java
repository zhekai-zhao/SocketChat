package socketchat;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private File receivedFile;
    private Server server;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
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
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("FILE_TRANSFER_REQUEST")) {
                    processFileTransferRequest(message, reader);
                } else {
                    System.out.println("Received message: " + message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error occurred while reading from the socket: " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    private void processFileTransferRequest(String message, BufferedReader reader) throws IOException {
        String[] parts = message.split(" ");
        String fileName = parts[1];
        long fileSize = Long.parseLong(parts[2]);
        String data = reader.readLine();
        receiveFile(fileName, fileSize, data);
        System.out.println("File received.");
        server.broadcastFile(this);
    }

    private void receiveFile(String fileName, long fileSize, String data) throws IOException {
        byte[] fileData = Base64.getDecoder().decode(data);
        if (fileData.length != fileSize) {
            throw new IOException("File data length mismatch!");
        }
        receivedFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
            fos.write(fileData);
        }
    }

    private void closeResources() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(File file) {
        try {
            byte[] fileData = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileData);
            }
            String message = "FILE_TRANSFER_REQUEST " + file.getName() + " " + fileData.length;
            sendMessage(message);
            writer.write(Base64.getEncoder().encodeToString(fileData) + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getReceivedFile() {
        return receivedFile;
    }
}
