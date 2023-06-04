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

    private static HashMap<String, GameState> games;
    private static HashMap<String, OutputStream> clientInWaiting;

    public static void main(String... args) {
        PORT = args.length == 1 ? Integer.parseInt(args[0]) : 31161;
        games = new HashMap<>();
        clientInWaiting = new HashMap<>();

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

//            new Thread(() -> {
//                System.out.println("UDP: Server is listening on port " + PORT);
//
//                while (true) {
//                    //try {
//                    //udpSocket.receive(request);
//                    executorService.submit(() -> {
//                        //udp(udpSocket, request);
//                        udp(udpSocket);
//                    });
//                    //} catch (IOException e) {
//                    //    throw new RuntimeException(e);
//                    //}
//                }
//            }).start();

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
                contentlength\n
                command\n
                other stuff...\n
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
            String response = processRequest(content);
            /*
                RESPONSE FORMAT
                contentlength\n
                body\n
             */

            // send back response to client
            OutputStream out = socket.getOutputStream();
            out.write(response.getBytes());

        }
    }

    private static String processRequest(HashMap<Integer, String> content) {
        String result = "";
        String command = content.get(0);
        switch (command) {
            case "HELO":
                result = "this is HELO";
                break;
            case "CREA":
                result = "this is CREA";
                break;
            case "LIST":
                break;
            case "JOIN":
                break;
            case "QUIT":
                break;
            case "STAT":
                break;
            case "MOVE":
                break;
            case "GDBY":
                break;
            default:
                break;
        }

        int contentLength = result.length();
        return contentLength + "\n" + result;
    }

    private static void udp(DatagramSocket socket) {
        System.out.println("UDP: Server is listening on port " + PORT);
        try {
            while (true) {
                DatagramPacket request = new DatagramPacket(new byte[512], 1);
                socket.receive(request);
                System.out.println("UDP: Received UDP request");
                InetAddress clientAddress = request.getAddress();
                int clientPort = request.getPort();

                // read from client

                // put client request into hashmap

                // process client request

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

    /*
        pre: InputStream containing information from a TCP Client
        post: returns a hashmap containing information from the TCP Client
     */
    private static HashMap<Integer, String> readClient(InputStream in) {
        try {
            String contentLength = "";
            int readChar1 = 0;
            while((readChar1 = in.read()) != '\n') {
                contentLength += (char)readChar1;
            }

            int index = 0;
            HashMap<Integer, String> result = new HashMap<>();
            String content = "";
            for (int i = 0; i < Integer.valueOf(contentLength); i++) {
                char curr = (char)in.read();
                if (curr == '\n') {
                    result.put(index, content);
                    index++;
                    content = "";
                } else {
                    content += curr;
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return null;
    }




}
