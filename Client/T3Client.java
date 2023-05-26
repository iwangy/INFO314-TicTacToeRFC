package Client;

import java.io.*;
import java.net.*;

public class T3Client{

    private static String HOST;
    private static int PORT;

    public static void main(String... args) {
        HOST = "localhost";
        PORT = Integer.valueOf(31161);
        sendTCP();
        sendUDP();

    }

    private static void sendTCP() {
        try (Socket sock = new Socket(HOST, PORT)) {
            System.out.println("starting TCP");
            InputStream in = sock.getInputStream();
            String result = "";
            int readChar = 0;
            while ((readChar = in.read()) != -1) {
                result += (char)readChar;
            }
            System.out.println(result);
            sock.close();
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

}
