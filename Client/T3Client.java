package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;

public class T3Client{

    private static String HOST;
    private static int PORT;

    private static String clientID;

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
//                System.out.println("Unacceptable connection type: " + args[0]);
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
                        String CREARequest = String.format(
                                "{command:%s," +
                                "clientID:%s}\n\r", ClientMessageMethod.CREA.getValue(), clientID
                        );
                        System.out.println("sending client request...");
                        out.write(CREARequest.getBytes());
                        System.out.println("client request sent");
                        break;
                    case GDBY:
                        break;
                    case HELO:
                        // All these come from user input args

                        String playerId = "clientID@uw.edu";
                        clientID = playerId;
                        String version = "1";
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
                        break;
                    case LIST:
                        ClientMessageMethod body = null;

                        version = "1";
                        
                        playerId = "ClientID@uw.edu";
                        clientID = playerId;
                        String LISTRequest = "";
                        if (command.length > 1) {
                            body = ClientMessageMethod.fromString(command[1]);
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
                        break;
                    case QUIT:
                        break;
                    case STAT:
                        break;
                    default:

                }
                // read and print server response
                String serverReply = "";
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = bufferedReader.readLine()) != null) {
                    if(line.isEmpty()) {
                        break;
                    }
                    serverReply += line;
                }
                System.out.println(serverReply);
            }
            // write command to server

            /*
            content-length: x\n
            version: x\n
            session-id: x\n
            parameters \n
            command
             */

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