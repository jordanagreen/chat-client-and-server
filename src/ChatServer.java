import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {

    private Map<String, String> ipAddressMap; //maps names to hosts
    private int portNumber;

    public static final String IP_STRING = "IP: ";
    public static final String REGISTER_CONFIRM = "You are now registered.";

    ChatServer(int portNumber){
        this.portNumber = portNumber;
        ipAddressMap = new HashMap<>();
    }

    private void startServer(){
        try(ServerSocket serverSocket = new ServerSocket(portNumber)){
            while (true){
                try{
                    Socket socket = serverSocket.accept();
                    System.out.println("Starting new thread");
                    (new Thread(new ResponseHandler(socket))).start();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        } catch (IOException e){
            e.printStackTrace();
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


    private class ResponseHandler implements Runnable{
        private Socket socket;

        ResponseHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run(){
            try(PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                System.out.println("Connection made");
                String input;
                while ((input = in.readLine()) != null){
                    System.out.println("input " + input);
                    if (isRegistrationQuery(input)) {
                        String userInfo = input.substring(input.indexOf(" ")+1);
                        String userName = userInfo.split(":")[0];
                        int port = Integer.parseInt(userInfo.split(":")[1]);
                        if (ipAddressMap.containsKey(userName)){
                            out.println(userName + " is already registered.");
                            out.flush();
                        } else {
                            registerUser(userName, port);
                            out.println(REGISTER_CONFIRM);
                            out.flush();
                        }
                    } else if (isLookupQuery(input)){
                        String queriedName = input.substring(input.indexOf(" ")+1);
                        if (ipAddressMap.containsKey(queriedName)){
                            out.println(IP_STRING + ipAddressMap.get(queriedName));
                            out.flush();
                        } else {
                            out.println("User " + queriedName + " is not registered.");
                            out.flush();
                        }
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        private void registerUser(String userName, int port){
            String ip = socket.getInetAddress().getHostAddress();
            String fullIP = ip + ":" + port;
            ipAddressMap.put(userName, fullIP);
            System.out.println("added " + userName + " " + fullIP);
        }
    }
}
