import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JokerServer {
    private int port;
    private List<ClientHandler> clients;
    private ConcurrentHashMap<String, GameEngine> playerGameEngines; // Map player names to GameEngine instances

    // Multicast settings
    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 4446;

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

    /**
     * Generates a formatted string containing all players' statuses.
     * Format: "ALL_PLAYERS:player1,score1,level1,combo1;player2,score2,level2,combo2;..."
     */
    public String getAllPlayersStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALL_PLAYERS:");
        boolean first = true;
        for (Map.Entry<String, GameEngine> entry : playerGameEngines.entrySet()) {
            if (!first) {
                sb.append(";");
            }
            GameEngine ge = entry.getValue();
            sb.append(ge.getPlayerName()).append(",")
                    .append(ge.getScore()).append(",")
                    .append(ge.getLevel()).append(",")
                    .append(ge.getCombo());
            first = false;
        }
        return sb.toString();
    }

    /**
     * Sends the top 10 scores via multicast.
     */
    public void sendTopScores() {
        try {
            String topScores = getTopScoresFormatted();
            byte[] buf = topScores.getBytes();
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
            MulticastSocket socket = new MulticastSocket();
            socket.send(packet);
            socket.close();
            System.out.println("Multicast top scores sent.");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and formats the top 10 scores from the database.
     * Format: "TOP_SCORES:player1,score1,level1,time1;player2,score2,level2,time2;..."
     */
    private String getTopScoresFormatted() throws SQLException {
        ArrayList<HashMap<String, String>> scores = Database.getScores();
        StringBuilder sb = new StringBuilder();
        sb.append("TOP_SCORES:");
        boolean first = true;
        for (HashMap<String, String> scoreData : scores) {
            if (!first) {
                sb.append(";");
            }
            sb.append(scoreData.get("name")).append(",")
                    .append(scoreData.get("score")).append(",")
                    .append(scoreData.get("level")).append(",")
                    .append(scoreData.get("time"));
            first = false;
        }
        return sb.toString();
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
