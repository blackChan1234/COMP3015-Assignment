import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private JokerServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;
    private GameEngine gameEngine;

    public ClientHandler(Socket socket, JokerServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Initialize input and output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Prompt client to enter their name
            out.println("ENTER_NAME");
            playerName = in.readLine();

            // Initialize GameEngine for this player
            gameEngine = new GameEngine(playerName);
            server.addGameEngine(playerName, gameEngine);

            // Notify all clients about the new player
            server.broadcast("PLAYER_JOINED:" + playerName);

            // Send initial game state to the new client
            out.println("GAME_STATE:" + gameEngine.serializeState());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Process player moves
                if (inputLine.startsWith("MOVE:")) {
                    String move = inputLine.substring(5);
                    gameEngine.moveMerge(move);

                    // Broadcast all players' status
                    server.broadcast(server.getAllPlayersStatus());

                    // Check for game over
                    if (gameEngine.isGameOver()) {
                        String scores = getAllScores(); // Gather all players' scores
                        server.broadcast("GAME_OVER:" + scores);
                        server.sendTopScores(); // Send multicast top scores
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + playerName);
        } finally {
            try {
                closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.removeClient(this);
            server.removeGameEngine(playerName);
            server.broadcast("PLAYER_LEFT:" + playerName);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void closeConnection() throws IOException {
        if (out != null)
            out.close();
        if (in != null)
            in.close();
        if (socket != null && !socket.isClosed())
            socket.close();
    }

    private String getAllScores() {
        // Retrieve scores from all GameEngine instances
        StringBuilder sb = new StringBuilder();
        for (GameEngine ge : server.getAllGameEngines().values()) {
            sb.append(ge.getPlayerName()).append(":").append(ge.getScore()).append(", ");
        }
        // Remove trailing comma and space
        if (sb.length() >= 2)
            sb.setLength(sb.length() - 2);
        return sb.toString();
    }
}
