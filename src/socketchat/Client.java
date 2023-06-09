package socketchat;

import java.io.*;
import java.net.Socket;

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

    public void sendMessage(String message) {
        try {
            outputStream.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                sendMessage("FILE_REQUEST");

                byte[] buffer = new byte[1024];
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fileInputStream);

                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    sendBytesWithParity(buffer, bytesRead);
                }

                bis.close();
                outputStream.flush();
                System.out.println("File sent: " + filePath);
            } else {
                System.out.println("File not found: " + filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFile(String fileName) {
        try {
            sendMessage("FILE_REQUEST");
            outputStream.write(fileName.getBytes());
            outputStream.flush();

            byte[] buffer = new byte[1024];
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
            System.out.println("File received: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBytesWithParity(byte[] bytes, int length) {
        byte[] dataWithParity = addParityBits(bytes, length);
        try {
            outputStream.write(dataWithParity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] addParityBits(byte[] data, int length) {
        byte[] dataWithParity = new byte[length + 1];
        for (int i = 0; i < length; i++) {
            dataWithParity[i] = data[i];
        }
        dataWithParity[length] = computeParity(data, length);
        return dataWithParity;
    }

    private byte computeParity(byte[] data, int length) {
        byte parity = 0;
        for (int i = 0; i < length; i++) {
            parity ^= data[i];
        }
        return parity;
    }
}
