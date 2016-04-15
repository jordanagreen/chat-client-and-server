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
            String input;
            while ((input = stdIn.readLine()) != null){
                if (!isRegistered && isRegistration(input)){
                    String name = input.substring(input.lastIndexOf(" "));
                    isRegistered = register(name);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean register(String name){
        try(Socket socket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println(REGISTER_QUERY + name);
            out.println(REGISTER_QUERY + name);
            out.flush();
            String response = in.readLine();
            System.out.println("response: " + response);
            return response.trim().equals(ChatServer.REGISTER_CONFIRM);
        } catch (IOException e){
            System.err.println("Couldn't connect to " + hostName + ":" + portNumber);
            e.printStackTrace();
            return false;
        }
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
