// Client.java
package socketchat;

import java.io.*;
import java.net.Socket;

public class Client {
    private static final int PACKET_SIZE = 1024;
    private static final int MAX_RETRY_COUNT = 3;

    public static void main(String[] args) {
        String serverIP = "localhost";
        int serverPort = 7777;
        String filePath = "file1.txt";

        try {
            Socket socket = new Socket(serverIP, serverPort);
            System.out.println("Connected to server.");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write("FILE_REQUEST");
            writer.newLine();
            writer.flush();

            writer.write(filePath);
            writer.newLine();
            writer.flush();

            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

            byte[] buffer = new byte[PACKET_SIZE];
            int bytesRead;
            int retryCount = 0;
            while ((bytesRead = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                fos.flush();

                if (!receiveConfirmation(socket)) {
                    retryCount++;
                    if (retryCount > MAX_RETRY_COUNT) {
                        System.out.println("Exceeded maximum retry count. File transfer failed.");
                        break;
                    }
                    System.out.println("Failed to receive confirmation. Retrying...");
                    continue;
                }

                retryCount = 0;
            }

            fos.close();
            bis.close();

            socket.close();
            System.out.println("Connection closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean receiveConfirmation(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String confirmation = reader.readLine();
            return confirmation != null && confirmation.equals("SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
