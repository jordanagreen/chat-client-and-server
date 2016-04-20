import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jordan on 4/14/2016.
 */
public class ChatClient {

    public static final String REGISTER_INPUT = "register me as";
    public static final String LOOKUP_INPUT = "i would like to talk to";
    public static final String REGISTER_QUERY = "REGISTER ";
    public static final String LOOKUP_QUERY = "LOOKUP ";
    public static final String MESSAGE_CMD = "/msg";

    private String hostName;
    private int portNumber;
    private int listeningPortNumber;
    private Map<String, ChatHandler> openChats = new HashMap<>(); //maps host addresses to the open chat with them
    private Map<String, String> hosts = new HashMap<>(); //maps names to hosts
    private String userName = "";

    private boolean isRegistered;

    ChatClient(String hostName, int portNumber, int listeningPortNumber){
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.listeningPortNumber = listeningPortNumber;
    }

    private void runClient(){
        try(BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))){
            String input;
            while ((input = stdIn.readLine()) != null){
                if (!isRegistered){
                    if (isRegistration(input)){
                        try(Socket socket = new Socket(hostName, portNumber);
                            PrintWriter out = new PrintWriter(socket.getOutputStream());
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                            String name = input.substring(input.lastIndexOf(" ")+1);
                            String response = register(name, listeningPortNumber, out, in);
                            isRegistered = response.trim().equals(ChatServer.REGISTER_CONFIRM);
                            System.out.println(response);
                            if (isRegistered){
                                userName = name;
                                listenForChats(); // can listen now that we're registered
                            }
                        }
                    }
                } else {
                    if (isLookup(input)){
                        input = input.trim();
                        String[] names = input.substring(LOOKUP_INPUT.length()+1).split(" ");
                        try(Socket socket = new Socket(hostName, portNumber);
                            PrintWriter out = new PrintWriter(socket.getOutputStream());
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                            for (String name: names){
                                if (name.trim().equalsIgnoreCase("and")){
                                    continue;
                                }
                                name = name.replace(",", " ").trim();
                                String response = lookupIP(name, out, in);
                                if (response.startsWith(ChatServer.IP_STRING)){
                                    String fullIP = response.substring(response.indexOf(" ")+1);
                                    String[] parts = fullIP.split(":");
                                    String ip = parts[0];
                                    int port = Integer.parseInt(parts[1]);
                                    startChat(ip, port);
                                } else {
                                    System.out.println(response);
                                }
                            }
                        }
                    } else { //message
                        // message to a specific person
                        if (input.startsWith(MESSAGE_CMD)){
                            try{
                                String targetName = input.split(" ")[1];
                                ChatHandler chatHandler = openChats.get(hosts.get(targetName));
                                if (chatHandler != null){
                                    String message = input.substring(input.indexOf(" ", input.indexOf(" ")+1)+1);
                                    chatHandler.sendMessage(message);
                                } else {
                                    System.out.println("Couldn't find user " + targetName);
                                }
                            } catch (ArrayIndexOutOfBoundsException e){
                                System.out.println("You need to specify a user to message.");
                            }
                        }
                        // send it to everyone
                        else {
                            for (String host: openChats.keySet()){
                                openChats.get(host).sendMessage(input);
                            }
                        }
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void startChat(String ip, int portNumber) throws IOException{
        Socket socket = new Socket(ip, portNumber);
        ChatHandler chatHandler = new ChatHandler(socket);
        (new Thread(chatHandler)).start();
    }

    private String register(String name, int listeningPortNumber, PrintWriter out, BufferedReader in)
            throws IOException{
        out.println(REGISTER_QUERY + name + ":" + listeningPortNumber);
        out.flush();
        return in.readLine();
    }

    private String lookupIP(String name, PrintWriter out, BufferedReader in) throws IOException{
        out.println(LOOKUP_QUERY + name);
        out.flush();
        return in.readLine();
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
            try(ServerSocket serverSocket = new ServerSocket(listeningPortNumber)){
                while (true){
                    try{
                        Socket socket = serverSocket.accept();
                        ChatHandler chatHandler = new ChatHandler(socket);
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
        private BufferedReader in;
        private PrintWriter out;
        private String host;
        private String name;

        ChatHandler(Socket socket) throws IOException{
            this.socket = socket;
            this.host = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            //keep references, using try-with-resources closes the sockets when the reader/writer is closed
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream());
        }

        @Override
        public void run(){
            try{
                //send this client's username
                out.println(userName);
                out.flush();
                // first thing sent will be the name
                name = in.readLine().trim();
                hosts.put(name, host);
                openChats.put(host, this);
                System.out.println("You are connected to " + name);
                String input;
                while ((input = in.readLine()) != null) {
                    System.out.println(input);
                }
            }
            catch (IOException e){
                //chat was terminated somehow
                try{
                    System.out.println("Chat with " + name + " closed");
                    openChats.remove(host);
                    socket.close();
                } catch (IOException oe){
                    oe.printStackTrace();
                }
            }
        }

        public void sendMessage(String message){
            out.println(userName + ": " + message);
            out.flush();
        }
    }

    public static void main(String[] args){
        if (args.length != 3 && args.length != 2){
            System.out.println("Usage: java ChatClient <hostname> <port> [listening port]");
            System.out.println("If no port is specified to listen on, the default will be 8888.");
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
        ChatClient client = new ChatClient(hostName, portNumber, listeningPortNumber);
        client.runClient();
    }
}
