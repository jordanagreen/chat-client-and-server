import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {

    private Map<String, InetAddress> ipAddressMap;
    private int portNumber;

    public static final String REGISTRATION_STRING = "register me as";
    public static final String QUERY_STRING = "i want to talk to";
    public static final String IP_STRING = "IP: ";

    ChatServer(int portNumber){
        this.portNumber = portNumber;
        ipAddressMap = new HashMap<>();
    }

    private void startServer(){
        try(
                ServerSocket serverSocket = new ServerSocket(portNumber);
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
                )
        {
            String input;
            while ((input = in.readLine()) != null){
                if (isRegistration(input)) {
                    String userName = input.substring(input.lastIndexOf(" "));
                    if (ipAddressMap.containsKey(userName)){
                        out.println(userName + " is already registered.");
                    } else {
                        InetAddress ip = clientSocket.getInetAddress();
                        ipAddressMap.put(userName, ip);
                        out.println("You have been registered.");
                    }
                } else if (isQuery(input)){
                    String queriedName = input.substring(input.lastIndexOf(" "));
                    if (ipAddressMap.containsKey(queriedName)){
                        out.write(IP_STRING + ipAddressMap.get(queriedName).toString());
                    } else {
                        out.write("User " + queriedName + " is not registered.");
                    }
                }
            }
        }
        catch (IOException e){
            System.err.println("Couldn't start the server at port " + portNumber);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean isRegistration(String s){
        return s.trim().toLowerCase().startsWith(REGISTRATION_STRING);
    }

    private boolean isQuery(String s){
        return s.trim().toLowerCase().startsWith(QUERY_STRING);
    }


    public static void main(String[] args) {
        if (args.length != 1){
            System.out.println("Usage: java ChatServer <port>");
        }
        int portNumber = Integer.parseInt(args[0]);
        ChatServer server = new ChatServer(portNumber);
        server.startServer();
    }
}
