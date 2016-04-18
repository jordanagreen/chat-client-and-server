import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jordan on 4/14/2016.
 */
public class ChatClient {

    public static final String REGISTER_INPUT = "register me as";
    public static final String LOOKUP_INPUT = "i want to talk to";
    public static final String REGISTER_QUERY = "REGISTER ";
    public static final String LOOKUP_QUERY = "LOOKUP ";

    private String hostName;
    private int portNumber;
    private int listeningPortNumber;
    private Map<String, ChatHandler> openChats; //maps host addresses to the open chat with them
    private Map<String, String> hosts; //maps names to hosts
    private String userName;

    private boolean isRegistered;

    ChatClient(String hostName, int portNumber, int listeningPortNumber){
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.listeningPortNumber = listeningPortNumber;
        this.openChats = new HashMap<>();
        this.hosts = new HashMap<>();
        this.userName = "";
    }

    private void runClient(){
        try(BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))){
            System.out.println("client started");

//          connect to server
            try(Socket socket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String input;
                while ((input = stdIn.readLine()) != null){
                    if (isRegistration(input)){
                        if (!isRegistered){
                            String name = input.substring(input.lastIndexOf(" ")+1);
                            String response = register(name, listeningPortNumber, out, in);
                            isRegistered = response.trim().equals(ChatServer.REGISTER_CONFIRM);
                            System.out.println(response);
                            listenForChats(); // can listen now that we're registered
                        } else {
                            System.out.println("You are already registered.");
                        }
                    } else if (isLookup(input)){
                        String name = input.substring(input.lastIndexOf(" ")+1);
                        String response = lookupIP(name, out, in);
                        if (response.startsWith(ChatServer.IP_STRING)){
                            String fullIP = response.substring(response.indexOf(" ")+1);
                            String[] parts = fullIP.split(":");
                            String ip = parts[0];
//                            int port = Integer.parseInt(parts[1]);
                            int port = ChatServer.PORT_NUMBER;
                            System.out.println("Connecting to " + name + " at " + ip + ":" + port);
                            startChat(ip, port);
                            startListeningForInput();
                            break; //don't need the server anymore
                        } else {
                            System.out.println(response);
                        }
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void startChat(String ip, int portNumber){
        try(Socket socket = new Socket(ip, portNumber)){
            String host = socket.getInetAddress().getHostAddress();
            ChatHandler chatHandler = new ChatHandler(socket);
            openChats.put(host, chatHandler);
            //send this client's username
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println(userName);
            out.flush();
            (new Thread(chatHandler)).start();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private String register(String name, int listeningPortNumber, PrintWriter out, BufferedReader in)
            throws IOException{
        out.println(REGISTER_QUERY + name + ":" + listeningPortNumber);
        out.flush();
        String response = in.readLine();
//        System.out.println("response: " + response);
        return response;
    }

    private String lookupIP(String name, PrintWriter out, BufferedReader in) throws IOException{
        out.println(LOOKUP_QUERY + name);
        out.flush();
        String response = in.readLine();
//        System.out.println("response: " + response);
        return response;
    }


    private boolean isRegistration(String s){
        return s.trim().toLowerCase().startsWith(REGISTER_INPUT);
    }

    private boolean isLookup(String s){
        return s.trim().toLowerCase().startsWith(LOOKUP_INPUT);
    }

    private void listenForChats(){
        (new Thread(new ChatListener())).start();
    }

    private class ChatListener implements Runnable{
        @Override
        public void run(){
            try{
                ServerSocket serverSocket = new ServerSocket(listeningPortNumber);
                while (true){
                    try{
                        Socket socket = serverSocket.accept();
                        System.out.println("Starting new chat thread");
                        ChatHandler chatHandler = new ChatHandler(socket);
                        String host = socket.getInetAddress().getHostAddress();
                        openChats.put(host, chatHandler);
                        (new Thread(chatHandler)).start();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // Gets messages from a single other user in a new thread
    private class ChatHandler implements Runnable{

        private Socket socket;
//        private String name;

        ChatHandler(Socket socket){
            this.socket = socket;
        }

//        public String getName(){
//            return name;
//        }

        @Override
        public void run(){
            try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){

                // first thing sent will be the name
                String name = in.readLine().trim();
                String host = socket.getInetAddress().getHostAddress();
                hosts.put(name, host);

                String input;
                while ((input = in.readLine()) != null) {
                    System.out.println(input);
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

        public void sendMessage(String message){
            try(PrintWriter out = new PrintWriter(socket.getOutputStream())){
                out.println(userName + ": " + message);
                out.flush();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void startListeningForInput(){
        (new Thread(new InputListener())).start();
    }

    private class InputListener implements Runnable{

        @Override
        public void run() {
            try(BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))){
                String input;
                while ((input = stdIn.readLine()) != null){
                    if (input.split(" ")[0].endsWith(":")){
                        String targetName = input.split(":")[0];
//                        ChatHandler chatHandler = getChatByName(targetName);
                        ChatHandler chatHandler = openChats.get(hosts.getOrDefault(targetName, null));
                        if (chatHandler != null){
                            String message = input.substring(input.indexOf(":")+1);
                            chatHandler.sendMessage(message);
                        } else {
                            //send it to everyone?
                        }
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args){
        if (args.length != 3 && args.length != 2){
            System.out.println("Usage: java ChatClient <hostname> <port> [listening port]");
            System.exit(1);
        }
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        int listeningPortNumber;
        if (args.length == 3){
            listeningPortNumber = Integer.parseInt(args[2]);
        } else {
            listeningPortNumber = 8888;
        }
        // connect to server to register and get IP
        ChatClient client = new ChatClient(hostName, portNumber, listeningPortNumber);
        client.runClient();
        // wait for any incoming chats in a new thread
    }

}
