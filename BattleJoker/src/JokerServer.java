import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class JokerServer {
    private int port;
    private List<ClientHandler> clients;
    private ConcurrentHashMap<String, GameEngine> playerGameEngines; // Map player names to GameEngine instances

    public JokerServer(int port) {
        this.port = port;
        clients = new ArrayList<>();
        playerGameEngines = new ConcurrentHashMap<>();
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("JokerServer started on port " + port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocket.close();
                for (ClientHandler client : clients) {
                    client.closeConnection();
                }
                System.out.println("JokerServer shut down gracefully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.out.println("ServerSocket closed, stopping server.");
                break;
            }
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public GameEngine getGameEngineForPlayer(String playerName) {
        return playerGameEngines.get(playerName);
    }

    public void addGameEngine(String playerName, GameEngine gameEngine) {
        playerGameEngines.put(playerName, gameEngine);
    }

    public void removeGameEngine(String playerName) {
        playerGameEngines.remove(playerName);
    }

    public ConcurrentHashMap<String, GameEngine> getAllGameEngines() {
        return playerGameEngines;
    }

    public static void main(String[] args) {
        int port = 12345; // Default port
        try {
            new JokerServer(port).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
