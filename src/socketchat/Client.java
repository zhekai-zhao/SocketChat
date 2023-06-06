package socketchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Connected to server.");

        Thread inputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        byte[] buffer = new byte[1024];
                        int bytesRead = inputStream.read(buffer);

                        if (bytesRead == -1) {
                            break;
                        }

                        String message = new String(buffer, 0, bytesRead);
                        System.out.println("Received message: " + message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        inputThread.start();

        Scanner scanner = new Scanner(System.in);

        try {
            while (true) {
                String message = scanner.nextLine();
                outputStream.write(message.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputThread.interrupt();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 7777);
        client.start();
    }
}

