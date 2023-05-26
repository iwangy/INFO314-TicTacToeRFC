package Server;

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class T3Server {

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static int PORT;

    // might change to HashMap<String, GameState>
    private static HashMap<Long, String> sessions;
    private static long gameIndex = 0;

    public static void main(String... args) {
        PORT = args.length == 1 ? Integer.parseInt(args[0]) : 31161;
        sessions = new HashMap<>();
        executorService.submit(T3Server::tcp);
        executorService.submit(T3Server::udp);
    }

    private static String getSessions() {
        if (sessions.size() == 0) return "no games available";
        String result = "";
        for (Map.Entry<Long, String> mapElement : sessions.entrySet()) {
            result += mapElement.getKey() + ", ";
        }
        return result.substring(0, result.length() - 2);
    }

    private static void tcp() {
        System.out.println("TCP: Server is listening on port " + PORT);
        try (ServerSocket server = new ServerSocket(PORT)) {
            Socket socket = null;
            while((socket = server.accept()) != null) {
                System.out.println("TCP: Received TCP request");
                final Socket threadSocket = socket;

                // read client command
                InputStream in = socket.getInputStream();
                String clientCommand = readClient(in);
                // System.out.println(clientCommand);

                // handle command
                String reply = "";
                switch (clientCommand) {
                    case "CREA":
                        reply = "JOND";
                        sessions.put(gameIndex, "temp");
                        gameIndex++;
                        break;
                    case "LIST":
                        reply = getSessions();
                        break;
                    case "JOIN":
                        reply = "JOND";

                        break;
                    default:
                        reply = "command does not exist";
                        break;
                }

                // reply to client
                OutputStream out = socket.getOutputStream();
                out.write(reply.getBytes(StandardCharsets.UTF_8));
                out.close();
                socket.close();
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String readClient(InputStream in) {
        try {
            String contentLength = "";
            int readChar1 = 0;
            while((readChar1 = in.read()) != ' ') {
                contentLength += (char)readChar1;
            }

            String clientCommand = "";
            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
                clientCommand += (char)in.read();
            }
            return clientCommand;
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return "";
    }

    private static void udp() {
        System.out.println("UDP: Server is listening on port " + PORT);
        try {
            while (true) {
                DatagramSocket socket = new DatagramSocket(PORT);
                DatagramPacket request = new DatagramPacket(new byte[512], 1);
                socket.receive(request);
                System.out.println("UDP: Received UDP request");
                InetAddress clientAddress = request.getAddress();
                int clientPort = request.getPort();

                // handle commands here
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
