package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class T3Server {

    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String... args) {

        int port = args.length == 1 ? Integer.parseInt(args[0]) : 31161;

        while (true) {
            executorService.submit(T3Server::tcp);
            executorService.submit(T3Server::udp);
            System.out.println("Server started, listening on port: " + port);
        }
    }

    public static void handleRequestTCP(Socket socket) {
        int quoteIndex = rnd.nextInt(quotes.length);
        try {

            byte[] quoteBytes = quotes[quoteIndex].getBytes("UTF-8");
            OutputStream out = socket.getOutputStream();
            out.write(quoteBytes);
            System.out.println("Quote Sent");
            out.close();
            socket.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static void tcp() {
        try (ServerSocket server = new ServerSocket(port)) {
            Socket socket = null;
            System.out.println("TCP: Server is listening on port " + port);

            while((socket = server.accept()) != null) {
                System.out.println("TCP: Accepted client request");
                final Socket threadSocket = socket;
                handleRequestTCP(threadSocket);
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }
    public static void udp() {
        try {
            while (true) {
                int quoteIndex = rnd.nextInt(quotes.length);
                DatagramSocket socket = new DatagramSocket(port);
                DatagramPacket request = new DatagramPacket(new byte[512], 1);
                socket.receive(request);
                System.out.println("UDP: Server has received datagram");

                InetAddress clientAddress = request.getAddress();
                int clientPort = request.getPort();

                String quote = quotes[quoteIndex];
                byte[] buffer = quote.getBytes();

                DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                System.out.println("UDP: Quote Sent");
                socket.send(response);
                socket.close();
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }


}
