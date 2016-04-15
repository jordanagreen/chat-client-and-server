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


    public static final String IP_STRING = "IP: ";
    public static final String REGISTER_CONFIRM = "You are now registered.";

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
            System.out.println("Connection made");
            String input;
            while ((input = in.readLine()) != null){
                System.out.println("input " + input);
                if (isRegistrationQuery(input)) {
                    System.out.println("register");
                    String userName = input.substring(input.indexOf(" "));
                    if (ipAddressMap.containsKey(userName)){
                        out.println(userName + " is already registered.");
                        out.flush();
                    } else {
                        InetAddress ip = clientSocket.getInetAddress();
                        ipAddressMap.put(userName, ip);
                        System.out.println("added " + userName + " " + ip);
                        out.println(REGISTER_CONFIRM);
                        out.flush();
                    }
                } else if (isLookupQuery(input)){
                    String queriedName = input.substring(input.indexOf(" "));
                    if (ipAddressMap.containsKey(queriedName)){
                        out.write(IP_STRING + ipAddressMap.get(queriedName).toString());
                    } else {
                        out.write("User " + queriedName + " is not registered.");
                    }
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean isRegistrationQuery(String s){
        return s.trim().startsWith(ChatClient.REGISTER_QUERY);
    }

    private boolean isLookupQuery(String s){
        return s.trim().startsWith(ChatClient.LOOKUP_QUERY);
    }



    public static void main(String[] args) {
        if (args.length != 1){
            System.out.println("Usage: java ChatServer <port>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        ChatServer server = new ChatServer(portNumber);
        System.out.println("Starting server");
        server.startServer();
    }
}
