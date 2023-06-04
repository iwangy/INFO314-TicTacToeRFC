package Client;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class T3Client{

    private static String HOST;
    private static int PORT;

    public static void main(String... args) {
        HOST = "localhost";
        PORT = Integer.valueOf(31161);
        sendTCP();
        // sendUDP();

    }

    private static void sendTCP() {
        try (Socket sock = new Socket(HOST, PORT)) {
            System.out.println("starting TCP");

            // write command to server
            String command = "CREA 1 C1ID";

            // convert command into easier to read form
            String payload = makePayload(command);
            OutputStream out = sock.getOutputStream();
            out.write(payload.getBytes());

            // read and print server response
            InputStream in = sock.getInputStream();
            String response = readServer(in);
            System.out.println(response);

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void sendUDP() {
        try (DatagramSocket sock = new DatagramSocket()) {
            InetAddress host = InetAddress.getByName(HOST);
            // ignored message
            String message = "hello";
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), host, PORT);
            sock.send(packet);

            byte[] buffer = new byte[512];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            sock.receive(receivedPacket);

            System.out.println(new String(buffer, 0, receivedPacket.getLength()));

            sock.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String makePayload(String command) {
        String result = "";

        String[] commandArr = command.split(" ");
        for (int i = 0; i < commandArr.length; i++) {
            result += commandArr[i] + "\n";
        }

        int contentLength = result.length();

        return contentLength + "\n" + result;
    }

    private static String readServer(InputStream in) {
        try {
            String contentLength = "";
            int readChar1 = 0;
            while((readChar1 = in.read()) != '\n') {
                contentLength += (char)readChar1;
            }

            String serverResponse = "";
            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
                serverResponse += (char)in.read();
            }
            return serverResponse;
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return null;
    }

}
