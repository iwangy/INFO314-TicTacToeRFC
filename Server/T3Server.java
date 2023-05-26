package Server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class T3Server {

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static int PORT;

    public static void main(String... args) {
        PORT = args.length == 1 ? Integer.parseInt(args[0]) : 31161;
        executorService.submit(T3Server::tcp);
        executorService.submit(T3Server::udp);
    }

    public static void tcp() {
        System.out.println("TCP: Server is listening on port " + PORT);
        try (ServerSocket server = new ServerSocket(PORT)) {
            Socket socket = null;
            while((socket = server.accept()) != null) {
                System.out.println("TCP: Received TCP request");
                final Socket threadSocket = socket;
                handleRequestTCP(threadSocket);
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void handleRequestTCP(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            out.write("TCP Reply".getBytes(StandardCharsets.UTF_8));
            out.close();
            socket.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void udp() {
        System.out.println("UDP: Server is listening on port " + PORT);
        try {
            while (true) {
                DatagramSocket socket = new DatagramSocket(PORT);
                DatagramPacket request = new DatagramPacket(new byte[512], 1);
                socket.receive(request);
                System.out.println("UDP: Received UDP request");

                InetAddress clientAddress = request.getAddress();
                int clientPort = request.getPort();

                String reply = "UDP Reply";
                byte[] buffer = reply.getBytes();

                DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                socket.send(response);
                socket.close();
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }


}
