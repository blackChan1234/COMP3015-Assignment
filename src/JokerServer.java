import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
public class JokerServer {
    private List<GameRoom> gameRooms = new ArrayList<>();
    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 4446;
    private ScheduledExecutorService multicastScheduler;
    public JokerServer(int port) throws IOException {
        ServerSocket srvSocket = new ServerSocket(port);
        System.out.println("Server started on port: " + port);
        startMulticastScheduler();
        while (true) {
            Socket clientSocket = srvSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            Thread childThread = new Thread(() -> {
                try {
                    serve(clientSocket);
                } catch (IOException | SQLException ex) {
                    ex.printStackTrace();
                }
            });
            childThread.start();
        }
    }
    private void startMulticastScheduler() {
        multicastScheduler = Executors.newSingleThreadScheduledExecutor();
        multicastScheduler.scheduleAtFixedRate(() -> {
            try {
                sendTopScoresMulticast();
            } catch (IOException | SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS); // Adjust the interval as needed
    }

    private void sendTopScoresMulticast() throws IOException, SQLException, ClassNotFoundException {
        ArrayList<HashMap<String, String>> topScores = Database.getScores();

        // Serialize the top scores to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(topScores.size());
            for (Map<String, String> scoreData : topScores) {
                dos.writeUTF(scoreData.get("name"));
                dos.writeInt(Integer.parseInt(scoreData.get("score")));
                dos.writeInt(Integer.parseInt(scoreData.get("level")));
                dos.writeUTF(scoreData.get("time"));
            }
        }

        byte[] data = baos.toByteArray();

        // Create a multicast socket
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
            socket.send(packet);
            System.out.println("Server: Sent top scores via multicast.");
        }
    }
    public void serve(Socket clientSocket) throws IOException, SQLException {

        DataInputStream in = null;
        DataOutputStream out = null;
        GameRoom assignedRoom = null;
        try {

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            String playerName = in.readUTF();
            String playerId = UUID.randomUUID().toString();
            PlayerInfo playerInfo = new PlayerInfo(playerName,playerId);
            System.out.println("Server: Received player name from client: " + playerName+ ", assigned ID: " + playerId);
            out.writeByte('I');
            out.writeUTF(playerId);
            out.flush();

            assignedRoom = null;
            synchronized (gameRooms) {
                // Find an available room
                for (GameRoom room : gameRooms) {
                    if (!room.PlayerisFull() && !room.isStarted()) {
                        assignedRoom = room;
                        break;
                    }
                }
                // Create a new room if none are available
                if (assignedRoom == null) {
                    assignedRoom = new GameRoom();
                    gameRooms.add(assignedRoom);
                }
                boolean joined = assignedRoom.addPlayer(clientSocket, playerInfo, out);
                if (!joined) {
                    // Game has already started, cannot join
                    out.writeByte('M'); // 'M' indicates message
                    out.writeUTF("The game has already started. Please join a new game.");
                    out.flush();
                    return;
                }

                // If the room is full, start the game
                if (assignedRoom.PlayerisFull()) {
                    assignedRoom.startGame(clientSocket); // send clientSocket not null
                }
            }

            while (true) {
                char data = (char) in.readByte();
                if (data == 't') {
                    // handle request top 10 score
                    sendTopScoresToClient(out);
                } else {
                    assignedRoom.handleClientData(data, clientSocket,in);
                }
            }
        } catch (IOException e) {
            // client lost connect
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            if (assignedRoom != null) {
                assignedRoom.removePlayer(clientSocket);
            }
        } finally {
            // close
            closeResource(in);
            closeResource(out);
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        }
    }

    private void sendTopScoresToClient(DataOutputStream out) {
        try {
            // Retrieve top 10 scores from the database
            ArrayList<HashMap<String, String>> topScores = Database.getScores();

            out.writeByte('T'); // 'T' indicates top scores
            out.writeInt(topScores.size());

            for (HashMap<String, String> scoreData : topScores) {
                out.writeUTF(scoreData.get("name"));
                out.writeInt(Integer.parseInt(scoreData.get("score")));
                out.writeInt(Integer.parseInt(scoreData.get("level")));
                out.writeUTF(scoreData.get("time"));
            }
            out.flush();

        }catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }


    // close Closeable
    private void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}

