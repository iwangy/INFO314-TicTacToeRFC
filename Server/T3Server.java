package Server;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class T3Server {

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static int PORT;

    private static HashMap<String, GameState> games;
    private static HashMap<String, Object[]> clientInWaiting;

    private static HashMap<Object, String> clientSockets;

    private static HashMap<DatagramSocket, String> udpSockets;

    private static String curClient;

    public static void main(String... args) {
        PORT = args.length == 1 ? Integer.parseInt(args[0]) : 3116;
        games = new HashMap<>();
        clientInWaiting = new HashMap<>();
        clientSockets = new HashMap<>();
        udpSockets = new HashMap<>();

        try {
            ServerSocket tcpSocket = new ServerSocket(PORT);
//            DatagramPacket request = new DatagramPacket(new byte[512], new byte[512].length);
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

                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    try {
                        udpSocket.receive(packet);
                        executorService.submit(() -> {
                            udp(udpSocket, packet);
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

            // read from client
            InputStream in = socket.getInputStream();

            /*
                INPUTSTREAM PAYLOAD
                command otherstuff... \r
             */

            // put client request into hashmap
            HashMap<Integer, String> content = readClient(in);
            /*
                HASHMAP CONTENTS
                0 : command
                1 : ...
                n : ...
             */



            // process client request
            OutputStream out = socket.getOutputStream();
            processRequest(content, new Object[]{"tcp", out}, new Object[]{"tcp", socket});
            /*
                RESPONSE FORMAT
                contentlength\n
                body\n
             */

            // send back response to client
            //OutputStream out = socket.getOutputStream();
            // out.write(response.getBytes());

        }
    }

    private static void processRequest(HashMap<Integer, String> content, Object[] out, Object[] socket) throws IOException {
        String command = content.get(0);
        String clientID;
        String sessionID;
        String gameID;
        switch (command) {

            case "HELO":
                /*
                    1 : version
                    2 : clientid
                */
                sessionID = generateRandomString();
                clientID = content.get(2);
                if (!clientInWaiting.containsKey(clientID)) {
                    sendResponse("SESS " + sessionID + " " + clientID + "\n\r", out);
                }
                if (socket[0].equals("tcp")) {
                    clientSockets.put(socket[1], clientID);
                } else {
                    clientSockets.put(socket[1] + clientID, clientID);
                }
                break;
            case "CREA":
                /*
                    1 : clientid
                */
                clientID = content.get(1);
                curClient = clientID;
                //System.out.println(clientID);

                if (!clientInWaiting.containsKey(clientID)) {
                    gameID = generateRandomString();
                    GameState newGame = new GameState(gameID);
                    newGame.join(clientID, out);
                    games.put(gameID, newGame);
                    clientInWaiting.put(clientID, out);
                    sendResponse("JOND " + gameID + "\n\r", out);
                }
                break;
            case "LIST":
                /*
                    1 : optional parameters (ALL / CURR)
                */
                Map <String, Integer> gamesIds = new HashMap<>();
                if(content.size() > 1 && content.get(1).equals("ALL")) {
                    for(String key: games.keySet()) {
                        if(games.get(key).getStatus() == 0 || games.get(key).getStatus() == 1) {
                            gamesIds.put(games.get(key).getGameID(), games.get(key).getStatus());
                        }
                    }
                }else if(content.size() > 1 && content.get(1).equals("CURR")) {
                    for(String key: games.keySet()) {
                        if(games.get(key).getStatus() == 0 || games.get(key).getStatus() == 1 || games.get(key).getStatus() == 2) {
                            gamesIds.put(games.get(key).getGameID(), games.get(key).getStatus());
                        }
                    }
                }else {
                    for(String key: games.keySet()) {
                        if(games.get(key).getStatus() == 0 ) {
                            gamesIds.put(games.get(key).getGameID(), games.get(key).getStatus());
                        }
                    }
                }

                StringBuilder gamesList = new StringBuilder("0: open, 1: in-play, 2: finished \n");
                for (Map.Entry<String, Integer> entry: gamesIds.entrySet()) {
                    gameID = entry.getKey();
                    Integer status = entry.getValue();
                    gamesList.append(gameID).append(" ").append(status).append("\n");
                }
                sendResponse("GAMS "+ gamesList + "\n\r", out);
                break;
            case "JOIN":
                /*
                    1 : gameid
                */
                gameID = content.get(1);
                if (socket[0].equals("tcp")) {
                    clientID = clientSockets.get(socket[1]);
                } else {
                    clientID = curClient;
                }
                // figure out how to get this without sending it over the socket


                if (games.get(gameID) == null) {
                    sendResponse("game does not exist\n\r" ,out);
                    break;
                }
                GameState game = games.get(gameID);
                if (game.join(clientID, out) == 1) {
                    sendResponse("game is full\n\r" ,out);
                    break;
                }
                String opponentid = game.getPlayerids()[0];

                sendResponse("JOND " + clientID + " " + gameID + "\n\n" + "YRMV " + opponentid + " " + gameID + "\n\r" ,out);

                if (game.getStatus() == 1) {
                    // send message to opponent
                    sendResponse("YRMV " + opponentid + " " + gameID + "\n\r" , clientInWaiting.get(opponentid));
                }

                clientInWaiting.remove(opponentid);
                break;
            case "QUIT":
                /*
                    1 : gameid
                    2 : clientid
                 */
                gameID = content.get(1);
                clientID = content.get(2);
                game = games.get(gameID);
                game.setGameStatusToDone(clientID);
                Object[] curSocket = (Object[])game.getPlayerOutputStream()[game.getTurn()];
                Object[] opponentSocket = (Object[])game.getPlayerOutputStream()[game.getTurn() ^ 1];
                sendResponse(game.getWinner() + " wins! " + "\n\r", curSocket);
                sendResponse(game.getWinner() + " wins! " + "\n\r", opponentSocket);
                break;
            case "STAT":
                /*
                    1 : gameid
                */
                gameID = content.get(1);
                game = games.get(gameID);
                int gameStat = game.getStatus();
                sendResponse("BORD " + gameID + " " + gameStat + "\n\r", out);
                break;
            case "MOVE":
                /*
                    1 : gameid
                    2 : location (1 - 9)
                */
                gameID = content.get(1);
                // figure out how to get this too
                String spot = content.get(2);
                if (socket[0].equals("tcp")) {
                    clientID = clientSockets.get(socket[1]);
                } else {
                    clientID = curClient;
                }

                CoordinatePair coordinatePair = new CoordinatePair(spot);
                GameState curGame = games.get(gameID);

                int moveResult = curGame.move(coordinatePair.getY(), coordinatePair.getX(), clientID);
                // [X, X, X], [X, X, X], [X, X, X] (1, 3) -> (0, 2)
                if (moveResult == 0) {
                    String[] players = curGame.getPlayerids();
                    String nextMoveClient = curGame.getPlayerids()[curGame.getTurn()];
                    // BORD GID1 CID1 CID2 CID2 |*|*|*|*|X|*|*|*|*|
                    // figure out how to send it properly without moveResult
                    sendResponse(moveResult + "\n" +"BORD " + gameID + " " + players[0] + " " + players[1] + " " + nextMoveClient +
                            "\n" + curGame.displayBoard() + "\n\r", out);
                    sendResponse("YRMV " + gameID + " " + players[0] + " " + players[1] + " " + nextMoveClient + "\n\r", (Object[])curGame.getPlayerOutputStream()[curGame.getTurn()]);
                    sendResponse("YRMV " + gameID + " " + players[0] + " " + players[1] + " " + nextMoveClient + "\n\r",(Object[])curGame.getPlayerOutputStream()[curGame.getTurn() ^ 1]);
                    curClient = nextMoveClient;
                } else if(moveResult == 1) {
                    sendResponse(moveResult + "\n" + "this move is out of bound" + "\n\r", out);
                } else if (moveResult == 2) {
                    sendResponse(moveResult + "\n" +"this move is already taken" + "\n\r", out);
                } else if (moveResult == 3) {
                    sendResponse(moveResult + "\n" + clientID + " wins! " + "\n\r", (Object[])curGame.getPlayerOutputStream()[curGame.getTurn()]);
                    sendResponse(moveResult + "\n" + clientID + " wins! " + "\n\r", (Object[])curGame.getPlayerOutputStream()[curGame.getTurn() ^ 1]);
                } else {
                    sendResponse(moveResult + "\n" +"Not your turn!" + "\n\r", out);
                }
                break;
            case "GDBY":
                 /*
                    1 : gameid
                    2 : clientid
                    or
                    2 : clientid
                 */
                if (content.size() == 3) {
                    gameID = content.get(1);
                    clientID = content.get(2);
                    game = games.get(gameID);
                    game.setGameStatusToDone(clientID);
                    curSocket = (Object[])game.getPlayerOutputStream()[game.getTurn()];
                    opponentSocket = (Object[])game.getPlayerOutputStream()[game.getTurn() ^ 1];
                    sendResponse(game.getWinner() + " wins! " + "\n\r", curSocket);
                    sendResponse(game.getWinner() + " wins! " + "\n\r", opponentSocket);
                }

                if (((String)socket[0]).equals("tcp")) {
                    sendResponse("GDBY " + "\n\r", out);
                    ((Socket)socket[1]).close();
                } else {
                    ((DatagramSocket)socket[1]).close();
                }
                break;
            default:
                sendResponse("Something went wrong" + "\n\r", out);
                break;
        }

        // things are now returned inside the switch statement
        //int contentLength = result.length();
        //return contentLength + "\n" + result;
    }

    private static void udp(DatagramSocket socket, DatagramPacket request) {
        try {
            System.out.println("UDP: Received UDP request");
            InetAddress clientAddress = request.getAddress();
            int clientPort = request.getPort();

            String command = new String(request.getData(), 0, request.getLength());

            // turn to hashmap
            int index = 0;
            HashMap<Integer, String> content = new HashMap<>();
            String tempString = "";
            for (char c : command.toCharArray()) {
                if (c == ' ') {
                    content.put(index, tempString);
                    index++;
                    tempString = "";
                } else {
                    tempString += c;
                }
            }

            processRequest(content, new Object[]{"udp", socket, clientAddress, clientPort}, new Object[]{"udp", socket});


            // handle commands here
//                String reply = "UDP Reply";
//
//
//                byte[] buffer = reply.getBytes();
//                DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
//                socket.send(response);
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
        pre: InputStream containing information from a TCP Client
        post: returns a hashmap containing information from the TCP Client
     */
    private static HashMap<Integer, String> readClient(InputStream in) {
        try {
//            String contentLength = "";
//            int readChar1 = 0;
//            while((readChar1 = in.read()) != '\n') {
//                contentLength += (char)readChar1;
//            }

            int index = 0;
            HashMap<Integer, String> result = new HashMap<>();
            String content = "";
            int readChar = 0;
            while ((readChar = in.read()) != '\r') {
                //System.out.println(readChar);
                content += (char)readChar;
            }

            String[] temp = content.split(" ");
            for (int i = 0; i < temp.length; i++) {
                result.put(i, temp[i]);
            }

//            String content = "";
//            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
//                char curr = (char)in.read();
//                if (curr == '\n') {
//                    result.put(index, content);
//                    index++;
//                    content = "";
//                } else {
//                    content += curr;
//                }
//            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return null;
    }

    private static void sendResponse(String message, Object[] out) {

        try {
            String method = (String)out[0];
            if (method.equals("tcp")) {
                // int contentLength = message.length();
                System.out.println("Sending server response...");
                // ((OutputStream)out[1]).write((contentLength + "\n" + message).getBytes());
                ((OutputStream)out[1]).write((message).getBytes());
                System.out.println("server response sent!");

            } else {
                byte[] buffer = message.getBytes();
                DatagramPacket response = new DatagramPacket(buffer, buffer.length, (InetAddress)out[2], (int)out[3]);
                ((DatagramSocket)out[1]).send(response);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateRandomString() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private static class CoordinatePair{
        private final int x;
        private final int y;

        public CoordinatePair(String spot) {
            String[] coordinates = spot.split(",");
            if (coordinates.length == 1) { // e.g 8
                this.x = Integer.parseInt(coordinates[0]) % 3 == 0 ? 3 : Integer.parseInt(coordinates[0]) % 3; // x = 2
                this.y = Integer.parseInt(coordinates[0]) % 3 == 0 ? Integer.parseInt(coordinates[0]) / 3 : Integer.parseInt(coordinates[0]) / 3 + 1; // y = 3
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
