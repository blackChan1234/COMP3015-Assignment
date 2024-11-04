import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            PlayerInfo playerInfo = new PlayerInfo(playerName);
            System.out.println("Server: Received player name from client: " + playerName);
            // 将玩家加入游戏房间
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

//                if (assignedRoom.isFirstPlayer(clientSocket)) {
//                    assignedRoom.promptFirstPlayerToStart();
//                } else if (assignedRoom.isFull()) {
//                    assignedRoom.startGame(null); // 达到 4 名玩家，自动开始游戏
//                }
//                assignedRoom.addPlayer(clientSocket, playerInfo, out);

                // If the room is full, start the game
                if (assignedRoom.PlayerisFull()) {
                    assignedRoom.startGame(clientSocket); // 传递 clientSocket 而非 null
                }
            }

            while (true) {
                char data = (char) in.readByte();
                if (data == 't') {
                    // 处理前 10 名记录请求
                    sendTopScoresToClient(out);
                } else {
                    assignedRoom.handleClientData(data, clientSocket,in);
                }
            }
        } catch (IOException e) {
            // 客户端可能已断开连接
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            if (assignedRoom != null) {
                assignedRoom.removePlayer(clientSocket);
            }
        } finally {
            // 关闭资源
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


    // 关闭 Closeable 资源的公共方法
    private void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 主方法
    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}

