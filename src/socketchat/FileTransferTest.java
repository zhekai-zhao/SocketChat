package socketchat;

import java.io.IOException;

public class FileTransferTest {
    public static void main(String[] args) {
        // Start the server
        Server server = new Server(1111);
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        try {
            // Create the first client to send the file
            Thread client1Thread = new Thread(() -> {
                Client client1 = new Client();
                try {
                    client1.connect("localhost", 1111);
                    client1.sendFile("file1.txt");
                    client1.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            client1Thread.start();

            // Wait for a while to ensure the first client has started transferring the file
            Thread.sleep(2000);

            // Create the second client to receive the file
            Thread client2Thread = new Thread(() -> {
                Client client2 = new Client();
                try {
                    client2.connect("localhost", 1111);
                    client2.receiveFile("file1.txt");
                    client2.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            client2Thread.start();

            // Wait for the client threads to finish
            client1Thread.join();
            client2Thread.join();

            // Stop the server
            serverThread.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
