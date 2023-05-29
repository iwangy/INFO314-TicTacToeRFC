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
    private static HashMap<String, GameState> games;

    public static void main(String... args) {
        PORT = args.length == 1 ? Integer.parseInt(args[0]) : 31161;
        games = new HashMap<>();
        executorService.submit(T3Server::tcp);
        executorService.submit(T3Server::udp);
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
                Map<String, String> clientCommand = readClient(in);
                // System.out.println(clientCommand);
                OutputStream out = socket.getOutputStream();

                // handle command
                String sessionID = "";
                switch (clientCommand.get("command")) {
                    case "HELO":
                        sessionID = generateRandomString();
//                        do something here to get the version...
                        sendResponse(out, "SESS 1 " + sessionID);
                    case "CREA":
                        // "JOND " CID " " + GID <- return format needs to be like this
                        String gid = generateRandomString();
                        GameState newGame = new GameState();
                        // find a way to get the player name and playerID
                        newGame.join("Jason", "playerID");
                        games.put(gid, newGame);
                        sendResponse(out, "JOND " + gid + " " + games.get(gid));
                        break;
                    case "LIST":
                        sendResponse(out, "GAMS " + getGames());
                        break;
                    case "JOIN": // join a given game
                        String clientSentGID = "";
                        sendResponse(out, "JOND " + games.get(clientSentGID)); // "JOND " CID " " + GID <- return format needs to be like thi
                        break;
                    case "QUIT":
//                        get game ID from client
//                        String gid
//                        sendResponse(out, gid);
                        socket.close();
                        break;
                    case "STAT":
                        break;
                    case "MOV":
                        break;
                    case "GDBY":
                        socket.close();
                        break;

                    default:
                        sendResponse(out, "command does not exist");
                        socket.close();
                        break;
                }

            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void sendResponse(OutputStream out, String message) {
        try {
            //Gson gson = new Gson(); // chatgpt didn't have this line
//            JsonObject jsonObject = new JsonObject();
//            jsonObject.addProperty("message", message);
//            JsonObject dataObject = new JsonObject();
//            dataObject.addProperty("clientIdentifer", )


            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateRandomString() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private static String getGames() {
        if (games.size() == 0) return "no games available";
        String result = "";
        for (Map.Entry<String, String> mapElement : games.entrySet()) {
            result += mapElement.getKey() + ", ";
        }
        return result.substring(0, result.length() - 2);
    }


    // NEED TO FIX TO READ THINGS
    private static Map<String, String> readClient(InputStream in) {

        try {
            String contentLength = "";
            int readChar1 = 0;

            while((readChar1 = in.read()) != '\n') {
                contentLength += (char)readChar1;
            }

            String clientRequest = "";
            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
                clientRequest += (char)in.read();
            }
            String[] clientRequestArr = clientRequest.split(": ");
            Map<String, String> clientCommand = new HashMap<>();
            for (int i = 0; i < clientRequestArr.length; i+=2) {
                clientCommand.put(clientRequestArr[i], clientRequestArr[i + 1]);
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
