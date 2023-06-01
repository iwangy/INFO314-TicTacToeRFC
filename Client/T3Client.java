package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;

public class T3Client{

    private static String HOST;
    private static int PORT;
    private static String clientID;

    private static String version;

    public static void main(String... args) {
        HOST = "localhost";
        PORT = Integer.valueOf(31161);
        String temp = "tcp";
        //switch (args[0].toLowerCase()) {
        switch (temp.toLowerCase()) {
            case "tcp":
                sendTCP();
                break;
            case "udp":
                sendUDP();
                break;
            default:
        }
    }

    private static void sendTCP() {
        System.out.println("starting TCP");

        try (Socket sock = new Socket(HOST, PORT)) {
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();

            while(true) {
                System.out.println("Please specify your command");
                Scanner scanner = new Scanner(System.in);

                String[] command = scanner.nextLine().split(" ");

                ClientMessageMethod method = ClientMessageMethod.fromString(command[0]);

                if(method == null) {
                    System.out.println("Unacceptable request");
                    out.close();
                    break;
                }

                switch (method) {
                    case CREA:
                        // String version = "1";
                        String CREARequest = String.format(
                                "{command:%s," +
                                "version:%s," +
                                "clientID:%s}\n\r", ClientMessageMethod.CREA.getValue(), version, clientID
                        );
                        System.out.println("sending client request...");
                        out.write(CREARequest.getBytes());
                        System.out.println("client request sent");
                        break;
                    case GDBY:
                        break;
                    case HELO:
                        // All these come from user input args
                        if (command.length != 3) {
                            System.out.println("please provide all arguments");
                            continue;
                        }

                        String playerId = command[2];
                        // String playerId = "clientID@uw.edu";
                        clientID = playerId;
                        version = command[1];
                        String HELORequest = String.format(
                                "{command:%s," +
                                "version:%s," +
                                "clientID:%s}\n\r", ClientMessageMethod.HELO.getValue(), version, playerId
                        );
                        System.out.println("sending client request...");
                        out.write(HELORequest.getBytes());
                        System.out.println("client request sent");
                        break;
                    case JOIN:
                        ClientMessageMethod body = null;

                        if (command.length > 1) {
//                            body = ClientMessageMethod.fromString(command[1]);
//                            System.out.println(body);
                            //System.out.println(command[1]);
                            String gameID = command[1];

                            String JOINRequest = String.format(
                                "{command:%s," +
                                "version:%s," +
                                "clientID:%s," +
                                "body:%s}\n\r", ClientMessageMethod.JOIN.getValue(), version, clientID, gameID
                            );
                            out.write(JOINRequest.getBytes());
                        } else {
                            System.out.println("You must include a game ID for the JOIN command");
                            continue;
                        }

                        break;
                    case LIST:
                        body = null;

                        version = "1";
                        
                        playerId = "ClientID@uw.edu";
                        clientID = playerId;
                        String LISTRequest = "";
                        if (command.length > 1) {
                            body = ClientMessageMethod.fromString(command[1]);
                            System.out.println(body);
                            LISTRequest = String.format(
                                "{command:%s," +
                                "version:%s," +
                                "clientID:%s," +
                                "body:%s}\n\r", ClientMessageMethod.LIST.getValue(), version, playerId, body.getValue()
                            );
                        } else {
                            LISTRequest = String.format(
                                    "{command:%s," +
                                            "version:%s," +
                                            "clientID:%s}\n\r", ClientMessageMethod.LIST.getValue(), version, playerId
                            );
                        }

                        System.out.println("sending client request...");
                        out.write(LISTRequest.getBytes());
                        System.out.println("client request sent");

                        break;
                    case MOVE:
                        if (command.length != 3) {
                            System.out.println("please provide all arguments");
                            continue;
                        }
                        String gameIdentifier = command[1];
                        String spot = command[2];
                        String MOVERequest = String.format(
                                "{command:%s," +
                                "gameid:%s," +
                                "spot:%s}\n\r", ClientMessageMethod.MOVE.getValue(), gameIdentifier, spot
                        );
                        out.write(MOVERequest.getBytes());

                        break;
                    case QUIT:
                        break;
                    case STAT:
                         body = null;
                        if (command.length > 1) {
//                            body = ClientMessageMethod.fromString(command[1]);
//                            System.out.println(body);
                            //System.out.println(command[1]);
                            String gameID = command[1];

                            playerId = "clientID@uw.edu";
                            clientID = playerId;
                            version = "1";
                            String STATRequest = String.format(
                                    "{command:%s," +
                                            "version:%s," +
                                            "clientID:%s," +
                                            "body:%s}\n\r", ClientMessageMethod.STAT.getValue(), version, playerId, gameID
                            );
                            out.write(STATRequest.getBytes());
                        } else {
                            System.out.println("You must include a game ID for the STAT command");
                            continue;
                        }

                        break;

                    default:
                        System.out.println("something went wrong");
                }
                // read and print server response
                StringBuilder serverReply = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = bufferedReader.readLine()) != null) {
                    if(line.isEmpty()) {
                        break;
                    }
                    serverReply.append(line).append("\n");
                }
                System.out.println(serverReply);
            }

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

enum ClientMessageMethod {
    CREA("CREA"), GDBY("GDBY"),
    HELO("HELO"), JOIN("JOIN"),
    LIST("LIST"), MOVE("MOVE"),
    QUIT("QUIT"), STAT("STAT"),
    CURR("CURR"), ALL("ALL");

    private final String value;
    ClientMessageMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static ClientMessageMethod fromString(String str) {
        for(ClientMessageMethod method: ClientMessageMethod.values()) {
            if(method.getValue().equals(str)) {
                return method;
            }
        }
        return null;
    }
}