package Server;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class T3Server {

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static int PORT;

    private static HashMap<String, GameState> games;
    private static HashMap<String, OutputStream> clientInWaiting;
    private static String sessionID;
    private static String clientID;

    public static void main(String... args) {
        PORT = args.length == 1 ? Integer.parseInt(args[0]) : 31161;
        games = new HashMap<>();
        clientInWaiting = new HashMap<>();

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
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

            String clientRequest = bufferedReader.readLine();
            String stringData = new String(clientRequest.getBytes(), StandardCharsets.US_ASCII);

            System.out.println("received client request: \n" + stringData);
            String[] requestParts = stringData.split("\\s+");
            String command = requestParts[0];

            // handle command
            switch (command) {
                case "HELO":
                    if(requestParts.length != 3) {
                        sendResponse("please provide all arguments ", out);
                    } else {
                        sessionID = generateRandomString();
                        clientID = requestParts[2];
                        if (!clientInWaiting.containsKey(clientID)) {
                            sendResponse("SESS " + sessionID + " " + clientID + "\n\r", out);
                        }
                    }
                    break;
                case "CREA":
                    //clientID = requestParts[2];
                    if (!clientInWaiting.containsKey(clientID)) {
                        String gid = generateRandomString();
                        GameState newGame = new GameState(gid);
                        newGame.join(clientID);
                        games.put(gid, newGame);
                        clientInWaiting.put(clientID, out);
                        sendResponse("JOND " + gid + "\n\r", out);
                    }
                    break;
                case "LIST":

                    Map <String, Integer> gamesIds = new HashMap<>();

                    if (requestParts.length > 1){
                        if (requestParts[1].equalsIgnoreCase("CURR")) {
                            for(String key: games.keySet()) {
                                if(games.get(key).getStatus() == 0 || games.get(key).getStatus() == 1) {
                                    gamesIds.put(games.get(key).getGameID(), games.get(key).getStatus());
                                }
                            }
                        }else if (requestParts[1].equalsIgnoreCase("ALL")) {
                            for(String key: games.keySet()) {
                                if(games.get(key).getStatus() == 0 || games.get(key).getStatus() == 1 || games.get(key).getStatus() == 2) {
                                    gamesIds.put(games.get(key).getGameID(), games.get(key).getStatus());
                                }
                            }
                        }
                    } else {
                        for(String key: games.keySet()) {
                            if(games.get(key).getStatus() == 0 ) {
                                gamesIds.put(games.get(key).getGameID(), games.get(key).getStatus());
                            }
                        }
                    }

                    StringBuilder gamesList = new StringBuilder("0: open, 1: in-play, 2: finished \n");
                    for (Map.Entry<String, Integer> entry: gamesIds.entrySet()) {
                        String gameID = entry.getKey();
                        Integer status = entry.getValue();
                        gamesList.append(gameID).append(" ").append(status).append("\n");
                    }
                    System.out.println(gamesList);
                    sendResponse("GAMS "+ gamesList + "\r", out);
                    break;
                case "JOIN":
                    String clientRequestGameID = requestParts[1];
                    if (games.get(clientRequestGameID) == null) {
                        sendResponse("game does not exist\n\r", out);
                    } else {
                        GameState game = games.get(clientRequestGameID);
                        String opponent = game.getPlayerids()[0];
                        if (game.join(clientID) == 1) { // this game is full!
                            sendResponse("gameID " + clientRequestGameID + " is full!" + "\n\r", out);
                            break;
                        }
                        sendResponse("JOND " + clientID + " " + clientRequestGameID + "\r", out); // "JOND " CID " " + GID <- return format needs to be like this

                        if (game.getStatus() == 1) { // ready to start!
//                       also have to implement logic to "Once this message is sent, the server will not accept any move commands from a client other than the one whose identifier was included in this message."
                            sendResponse("YRMV " + clientRequestGameID + " " + opponent + "\n\r", clientInWaiting.get(opponent));
                            sendResponse("YRMV " + clientRequestGameID + " " + opponent + "\n\r", out);
                        }
                        clientInWaiting.remove(opponent);
                    }
                    break;
                case "STAT":
                    String gameID = requestParts[1];
                    GameState game = games.get(gameID);
                    int gameStat = game.getStatus();
                    sendResponse("BORD " + gameID + " " + gameStat + "\n\r", out);
                    break;
                case "GDBY":
                    socket.close();
                    break;
                case "MOVE":
                    if(requestParts.length != 3) {
                        System.out.println("please provide all arguments");
                        continue;
                    }
                    gameID = requestParts[1];
                    String spot = requestParts[2];
                    CoordinatePair coordinatePair = new CoordinatePair(spot);
                    GameState curGame = games.get(gameID);
                    int moveResult = curGame.move(coordinatePair.getY(), coordinatePair.getX(), clientID);
                    if (moveResult == 0) {
                        String[] players = curGame.getPlayerids();
                        String nextMoveClient = curGame.getPlayerids()[curGame.getTurn() ^ 1];
                        sendResponse("BORD " + gameID + " " + players[0] + " " + players[1] + " " + nextMoveClient + "\n" + curGame.displayBoard() + "\n\r", out);
                    } else if(moveResult == 1) {
                        sendResponse("this move is out of bound" + "\n\r", out);
                    } else if (moveResult == 2) {
                        sendResponse("this move is already taken" + "\n\r", out);
                    } else if (moveResult == 3) {
                        sendResponse(clientID + " wins! " + "\n\r", out);
                    } else {
                        sendResponse("Not your turn!" + "\n\r", out);
                    }
                    break;
                case "QUIT":
                    //get game ID from client
                    //String gid
                    //sendResponse(out, gid);
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
            System.out.println("Sending server response...");
            out.write(message.getBytes());
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

    private static class CoordinatePair{
        private final int x;
        private final int y;

        public CoordinatePair(String spot) {
            String[] coordinates = spot.split(",");
            if (coordinates.length == 1) { // e.g 8
                this.x = Integer.parseInt(coordinates[0]) % 3; // x = 2
                this.y = Integer.parseInt(coordinates[0]) / 3 + 1; // y = 3
            } else { // e.g (2, 1)
                this.x = Integer.parseInt(coordinates[0]); // x = 2
                this.y = 3 - Integer.parseInt(coordinates[1]) + 1; // y = 3
            }
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

}