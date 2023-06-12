package socketchat;

import java.io.*;
import java.net.Socket;

public class Client {
    private static final int PACKET_SIZE = 1024;
    private static final int MAX_RETRY_COUNT = 3;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    public void connect(String serverIP, int serverPort) throws IOException {
        socket = new Socket(serverIP, serverPort);
        System.out.println("Connected to the server.");

        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void disconnect() throws IOException {
        socket.close();
        System.out.println("Connection closed.");
    }

    public void sendFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File does not exist or is not a regular file.");
            return;
        }

        // Send file request
        writer.write("FILE_REQUEST");
        writer.newLine();
        writer.flush();

        // Send file path
        writer.write(filePath);
        writer.newLine();
        writer.flush();

        // Receive file transfer result
        boolean success = receiveFile(filePath);
        if (success) {
            System.out.println("File transfer completed.");
        } else {
            System.out.println("File transfer failed.");
        }
    }

    public boolean receiveFile(String filePath) throws IOException {
   
        File file = new File(filePath);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

        byte[] buffer = new byte[PACKET_SIZE];
        int bytesRead;
        int retryCount = 0;

        while ((bytesRead = bis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
            fos.flush();

            if (!receiveConfirmation()) {
                retryCount++;
                if (retryCount > MAX_RETRY_COUNT) {
                    System.out.println("Exceeded the maximum retry count. File transfer failed.");
                    break;
                }
                System.out.println("Failed to receive confirmation. Retrying...");
                continue;
            }

            retryCount = 0;
        }

        fos.close();
        bis.close();

        if (retryCount == 0) {
            System.out.println("File received and saved");
            return true;
        } else {
            file.delete();
            return false;
        }
    }

    private boolean receiveConfirmation() throws IOException {
        String confirmation = reader.readLine();
        return confirmation != null && confirmation.equals("SUCCESS");
    }
}
