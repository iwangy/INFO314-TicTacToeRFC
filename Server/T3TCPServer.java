package Server;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class T3TCPServer {

    private int PORT;

    public T3TCPServer(int port) {
        this.PORT = port;
    }

    public void runTCPServer() {
        try {
            ServerSocket server = new ServerSocket(PORT);
            Socket socket = null;
            while ((socket = server.accept()) != null) {
                System.out.println("Accepted TCP client request");
                final Socket threadSocket = socket;
                exec.submit( () -> handleTCPRequest(threadSocket));
            }
            server.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void handleTCPRequest(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);
            out.close();
            socket.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
