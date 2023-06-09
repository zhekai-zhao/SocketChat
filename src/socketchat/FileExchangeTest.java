package socketchat;

public class FileExchangeTest {
    public static void main(String[] args) {
        
        Server server = new Server(7777);
        Thread serverThread = new Thread(() -> server.start());
        serverThread.start();

        
        Client client1 = new Client("localhost", 7777);
        Thread client1Thread = new Thread(() -> {
            client1.sendFile("file1.txt");
            client1.requestFile("file2.txt");
            client1.close();
        });
        client1Thread.start();

        
        Client client2 = new Client("localhost", 7777);
        Thread client2Thread = new Thread(() -> {
            client2.sendFile("file2.txt");
            client2.requestFile("file1.txt");
            client2.close();
        });
        client2Thread.start();

        try {
            
            client1Thread.join();
            client2Thread.join();
            

            
            serverThread.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
