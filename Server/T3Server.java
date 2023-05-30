package Server;

import org.json.JSONObject;

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

        try {
            ServerSocket tcpSocket = new ServerSocket(PORT);
            DatagramSocket udpSocket = new DatagramSocket(PORT);

            new Thread(() -> {
                try {
                    System.out.println("TCP: Server is listening on port " + PORT);

                    while (true) {
                        Socket socket = tcpSocket.accept();

                        executorService.submit(() -> {
                            try {
                                tcp(socket);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            new Thread(() -> {
                System.out.println("UDP: Server is listening on port " + PORT);

                DatagramPacket request = new DatagramPacket(new byte[512], new byte[512].length);
                while (true) {
                    try {
                        udpSocket.receive(request);
                        executorService.submit(() -> {
                            udp(udpSocket, request);
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tcp(Socket socket) throws IOException {
        System.out.println("TCP: Received TCP request");
        while(socket != null) {
            // read client command
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String clientRequest = readClient(in);
            System.out.println("received client request: \n" + clientRequest);
            if(clientRequest == null) {
                sendResponse("bad request, please try again", out);
                socket.close();
                return ;
            }

            JSONObject clientReqJson = new JSONObject(clientRequest);
            // handle command
            String sessionID;
            switch (clientReqJson.getString("command")) {
                case "HELO":
                    sessionID = generateRandomString();
                    sendResponse("SESS " + sessionID + " " + clientReqJson.getString("clientID") + "\n\r", out);
                    break;
                case "CREA":
                    // "JOND " CID " " + GID <- return format needs to be like this
                    String gid = generateRandomString();
                    GameState newGame = new GameState();
                    // find a way to get the player name and playerID
                    String playerID = clientReqJson.getString("clientID");
                    newGame.join("Jason", playerID);
                    games.put(gid, newGame);
                    sendResponse("JOND " + gid + " " + games.get(gid) + "\n\r", out);
                    break;
                case "LIST":
                    sendResponse("GAMS " + getGames(), out);
                    break;
                case "JOIN": // join a given game
                    String clientSentGID = "";
                    sendResponse("JOND " + games.get(clientSentGID), out); // "JOND " CID " " + GID <- return format needs to be like thi
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
                    sendResponse("command does not exist", out);
                    socket.close();
                    break;
            }
        }
    }

    private static void sendResponse(String message, OutputStream out) {
        try {
            //Gson gson = new Gson(); // chatgpt didn't have this line
//            JsonObject jsonObject = new JsonObject();
//            jsonObject.addProperty("message", message);
//            JsonObject dataObject = new JsonObject();
//            dataObject.addProperty("clientIdentifer", )

            System.out.println("Sending server response...");
            out.write(message.getBytes(StandardCharsets.UTF_8));
            System.out.println("server response sent!");
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
        for (Map.Entry<String, GameState> mapElement : games.entrySet()) {
            result += mapElement.getKey() + ", ";
        }
        return result.substring(0, result.length() - 2);
    }


    // NEED TO FIX TO READ THINGS
    private static String readClient(InputStream in) {

        try {
            StringBuilder payload = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;
            while((line = bufferedReader.readLine()) != null) {
                if(line.isEmpty()) {
                    break;
                }
                payload.append(line);
            }

            return payload.toString();
//            String contentLength = "";
//            int readChar1 = 0;
//
//            while((readChar1 = in.read()) != '\n') {
//                contentLength += (char)readChar1;
//            }
//
//            String clientRequest = "";
//            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
//                clientRequest += (char)in.read();
//            }
//            String[] clientRequestArr = clientRequest.split(": ");
//            Map<String, String> clientCommand = new HashMap<>();
//            for (int i = 0; i < clientRequestArr.length; i+=2) {
//                clientCommand.put(clientRequestArr[i], clientRequestArr[i + 1]);
//            }
//
//            return clientCommand;
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return null;
    }

    private static void udp(DatagramSocket socket, DatagramPacket request) {
        try {
            while (true) {
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
