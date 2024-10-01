import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class JokerServer {
    public JokerServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while(true){
            Socket clientSocket = serverSocket.accept();
            Thread childThred = new Thread(()->{
                serve(clientSocket);
            });
            childThred.start();
        }
    }
    public void serve (Socket clientSocket){
        System.out.println(clientSocket.getInetAddress());
    }

    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}
