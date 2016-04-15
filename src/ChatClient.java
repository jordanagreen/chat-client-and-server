import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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

    private boolean isRegistered;

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
                            String name = input.substring(input.lastIndexOf(" "));
                            String response = register(name, out, in);
                            isRegistered = response.trim().equals(ChatServer.REGISTER_CONFIRM);
                            System.out.println(response);
                        } else {
                            System.out.println("You are already registered.");
                        }
                    } else if (isLookup(input)){
                        String name = input.substring(input.lastIndexOf(" "));
                        String response = lookupIP(name, out, in);
                        if (response.startsWith(ChatServer.IP_STRING)){
                            System.out.println(name + " " + response);
//                          connectToIP(response);
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

    private String register(String name, PrintWriter out, BufferedReader in) throws IOException{
            out.println(REGISTER_QUERY + name);
            out.flush();
            String response = in.readLine();
            System.out.println("response: " + response);
            return response;
    }

    private String lookupIP(String name, PrintWriter out, BufferedReader in) throws IOException{
            out.println(LOOKUP_QUERY + name);
            out.flush();
            String response = in.readLine();
            System.out.println("response: " + response);
            return response;
    }


    private boolean isRegistration(String s){
        return s.trim().toLowerCase().startsWith(REGISTER_INPUT);
    }

    private boolean isLookup(String s){
        return s.trim().toLowerCase().startsWith(LOOKUP_INPUT);
    }

    ChatClient(String hostName, int portNumber){
        this.hostName = hostName;
        this.portNumber = portNumber;
    }


    public static void main(String[] args){
        if (args.length != 2){
            System.out.println("Usage: java ChatClient <hostname> <port>");
            System.exit(1);
        }
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        ChatClient client = new ChatClient(hostName, portNumber);
        client.runClient();
    }

}
