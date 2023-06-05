package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class T3Client{

    private static String HOST;
    private static int PORT;

    private static String clientID;
    private static String version;
    private static boolean waitingServer;
    private static String currentCommand;

    public static void main(String... args) {
        HOST = "localhost";
        PORT = Integer.valueOf(31161);
//        System.out.println("Enter which protocol to use: tcp or udp");
//        Scanner protocol = new Scanner(System.in);
//        String temp = protocol.nextLine();
//        waitingServer = false;
//        switch (temp.toLowerCase()) {
//            case "tcp":
//                sendTCP();
//                break;
//            case "udp":
//                sendUDP();
//                break;
//            default:
//        }
        waitingServer = false;
//        sendTCP();
         sendUDP();

    }

    private static void sendTCP() {
        try (Socket sock = new Socket(HOST, PORT)) {
            System.out.println("starting TCP");
            InputStream in = sock.getInputStream();
            OutputStream out = sock.getOutputStream();
            boolean closed = false;

            while(true) {
                if (waitingServer) {
                    System.out.println(readServer(in));
                    waitingServer = false;
                    continue;
                }

                System.out.println("Please specify your command");
                Scanner scanner = new Scanner(System.in);
                // write command to server
                String command = scanner.nextLine();

                // convert command into easier to read form
                String payload = makePayload(command);

                while (currentCommand.equals("MOVE")) {
                    out.write(payload.getBytes());
                    String serverMsg = readServer(in);

                    if (parseMOVResult(serverMsg) == 0) {
                        // receiving BORD from the server
                        System.out.println(serverMsg);
                        // receiving YRMV from the server
                        System.out.println(readServer(in));
                        // receiving YRMV from the other client
                        System.out.println(readServer(in));
                    } else if (parseMOVResult(serverMsg) == 3) {
                        System.out.println(serverMsg);
                        sock.close();
                        break;
                    } else {
                        // receiving ERROR Message from the server
                        System.out.println(serverMsg);
                    }

                    System.out.println("Please specify your command");
                    // write command to server
                    command = scanner.nextLine();
                    payload = makePayload(command);
                }

                out.write(payload.getBytes());

                if (currentCommand.equals("CREA") ||
                    currentCommand.equals("JOIN")) {
                    waitingServer = true;
                    System.out.println(readServer(in));
                    continue;
                }


                // read and print server response
                if (!waitingServer && !closed) {
                    String response = readServer(in);
                    System.out.println(response);
                }
            }

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void sendUDP() {

        try (DatagramSocket sock = new DatagramSocket()) {
            while(true) {
                InetAddress host = InetAddress.getByName(HOST);

                System.out.println("Please specify your command");
                Scanner scanner = new Scanner(System.in);
                // write command to server
                String command = scanner.nextLine();

                // convert command into easier to read form
                String payload = makePayload(command);

                DatagramPacket packet = new DatagramPacket(payload.getBytes(), payload.length(), host, PORT);
                sock.send(packet);

                byte[] buffer = new byte[512];
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                sock.receive(receivedPacket);

                System.out.println(new String(buffer, 0, receivedPacket.getLength()));

                //sock.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private static String makePayload(String command) {
        String result = "";

        String[] commandArr = command.split(" ");

        currentCommand = commandArr[0];

        if (currentCommand.equals("HELO")) {
            clientID = commandArr[2];
        }

        for (int i = 0; i < commandArr.length; i++) {
            result += commandArr[i] + "\n";
        }

        result += clientID + "\n";

        int contentLength = result.length();

        return contentLength + "\n" + result;
    }

    private static String readServer(InputStream in) {
        try {
            String contentLength = "";
            int readChar1 = 0;
            while((readChar1 = in.read()) != '\n') {
                contentLength += (char)readChar1;
            }

            String serverResponse = "";
            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
                serverResponse += (char)in.read();
            }
            return serverResponse;
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return null;
    }

    private static int parseMOVResult(String result) {
        return Integer.parseInt(result.split("\n") [0]);
    }
}
