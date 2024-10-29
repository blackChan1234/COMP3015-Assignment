import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.lang.System.out;

public class JokerServer {
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random(0);
    private final Map<String, Runnable> actionMap = new HashMap<>();
    private int combo;
    private int numOfTilesMoved;
    private int score;
    private int totalMoveCount;
    private boolean gameOver;
    private int level = 1;
    private String playerName;
    private Map<Socket, PlayerInfo> playerInfoMap = new HashMap<>();
    private Map<Socket, DataOutputStream> clientOutputMap = new HashMap<>();

    private boolean gameOverHandled = false;

    ArrayList<Socket> clientList = new ArrayList<>();

    public JokerServer(int port) throws IOException {
        actionMap.put("U", this::moveUp);
        actionMap.put("D", this::moveDown);
        actionMap.put("L", this::moveLeft);
        actionMap.put("R", this::moveRight);
        nextRound();

        ServerSocket srvSocket = new ServerSocket(port);
        while (true) {
            Socket clientSocket = srvSocket.accept();

            synchronized (clientList) {
                clientList.add(clientSocket);

            }

            Thread childThread = new Thread(() -> {
                try {
                    serve(clientSocket);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                synchronized (clientList) {
                    clientList.remove(clientSocket);
                }

            });
            childThread.start();

        }
    }

    private void moveDown() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
    }

    private void moveUp() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
    }

    private void moveRight() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i);
    }

    private void moveLeft() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(-1, i, SIZE - 1 + i);
    }

    private void moveMerge(int d, int s, int l) {
        int v, j;
        for (int i = s - d; i != l - d; i -= d) {
            j = i;
            if (board[j] <= 0) continue;
            v = board[j];
            board[j] = 0;
            while (j + d != s && board[j + d] == 0)
                j += d;

            if (board[j + d] == 0) {
                j += d;
                board[j] = v;
            } else {
                while (j != s && board[j + d] == v) {
                    j += d;
                    board[j] = 0;
                    v++;
                    score++;
                    combo++;
                }
                board[j] = v;
                if (v > level) level = v;
            }
            if (i != j)
                numOfTilesMoved++;

        }
    }

    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setPlayerName(String name) {
        playerName = name;
    }

    public int getScore() {
        return score;
    }

    public int getCombo() {
        return combo;
    }

    public int getLevel() {
        return level;
    }

    public int getMoveCount() {
        return totalMoveCount;
    }

    public void serve(Socket clientSocket) throws IOException, SQLException {
        out.println(clientSocket.getInetAddress());
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

        String playerName = in.readUTF();
        PlayerInfo playerInfo = new PlayerInfo(playerName);
        synchronized (playerInfoMap) {
            clientList.add(clientSocket);
            playerInfoMap.put(clientSocket, playerInfo);
            clientOutputMap.put(clientSocket, out);
        }
        //send a copy of the array to the client when it has just connected
        sendArray(out);
        sendPlayersInfo(out);
        while (true) {
            char dir = (char) in.readByte();
            System.out.println("Server: Received direction from client: " + dir);


            synchronized (clientList) {
                /// lock the client list, other threads will wait outside the zone
                moveMerge("" + dir);
                //update player data
                playerInfo.setScore(getScore());
                playerInfo.setLevel(getLevel());
                playerInfo.setCombo(getCombo());
                playerInfo.setMoves(getMoveCount());
                for (Socket s : clientList) {
                    DataOutputStream outClient = clientOutputMap.get(s);

                    //out.write(dir);
                    // out.flush();
                    /// DO NOT CLOSE the socket or the output stream

                    //send the array to the client
                    sendArray(outClient);
                    sendPlayersInfo(outClient);
                }
                if (dir == 't') {
            // Client is requesting top scores
            System.out.println("Server: Client " + playerName + " requested top scores.");
            sendTopScoresToClient(out);
        } else {
            System.out.println("Server: Unknown command from client: " + (char) dir);
        }
            }
        }
    }
    private void sendTopScoresToClient(DataOutputStream out) {
        try {
            Database.connect();
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

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private PlayerInfo getPlayerInfo(Socket clientSocket) {
        synchronized (playerInfoMap) {
            return playerInfoMap.get(clientSocket);
        }
    }

    private void sendPlayersInfo(DataOutputStream out) throws IOException {
        out.writeByte('P');
        synchronized (playerInfoMap) {
            out.writeInt(playerInfoMap.size());
            for (PlayerInfo playerInfo : playerInfoMap.values()) {
                out.writeUTF(playerInfo.getName());
                out.writeInt(playerInfo.getScore());
                out.writeInt(playerInfo.getLevel());
                out.writeInt(playerInfo.getCombo());
                out.writeInt(playerInfo.getMoves());
            }
        }
        out.flush();
    }


    void sendArray(DataOutputStream outClient) throws IOException {
        outClient.writeByte('A');
        outClient.writeInt(board.length);
        for (int v : board) {

            outClient.writeInt(v);
        }
        outClient.flush();
        System.out.println("Server: Sending array to clients.");

    }

    public void moveMerge(String dir) throws SQLException {
        synchronized (board) {
            if (actionMap.containsKey(dir)) {
                combo = numOfTilesMoved = 0;

                // go to the hash map, find the corresponding method and call it
                actionMap.get(dir).run();

                // calculate the new score
                score += combo / 5 * 2;

                // determine whether the game is over or not
                if (numOfTilesMoved > 0) {
                    totalMoveCount++;
                    gameOver = level == LIMIT || !nextRound();
                } else
                    gameOver = isFull();

                // update the database if the game is over
                if (gameOver) {
                    checkGameOver();

                }
            }
        }
    }

    public synchronized  void checkGameOver() throws SQLException {
        if (gameOver && !gameOverHandled) {
            gameOverHandled = true;
            System.out.println("Server: Game over. Sending scores to clients.");
            // Find the winner
            PlayerInfo winner = getWinner();

            // Record the winner's score in the database
            try {

                Database.putScore(winner.getName(), winner.getScore(), winner.getLevel());

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Send all player scores to clients
            sendGameOverScores();
            Database.disconnect();
        } else if (gameOverHandled) {
            System.out.println("Server: Game over has already been handled.");
        }
    }

    private PlayerInfo getWinner() {
        PlayerInfo winner = null;
        int highestScore = -1;
        for (PlayerInfo player : playerInfoMap.values()) {
            if (player.getScore() > highestScore) {
                highestScore = player.getScore();
                winner = player;
            }
        }
        return winner;
    }

    private void sendGameOverScores() {
        synchronized (clientList) {
            for (Socket s : clientList) {
                try {
                    DataOutputStream out = clientOutputMap.get(s);
                    out.writeByte('S'); // 'S' indicates game over scores
                    out.writeInt(playerInfoMap.size());
                    for (PlayerInfo player : playerInfoMap.values()) {
                        out.writeUTF(player.getName());
                        out.writeInt(player.getScore());
                        out.writeInt(player.getLevel());
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    private boolean nextRound() {
        if (isFull()) return false;
        int i;

        // randomly find an empty place
        do {
            i = random.nextInt(SIZE * SIZE);
        } while (board[i] > 0);

        // randomly generate a card based on the existing level, and assign it to the select place
        board[i] = random.nextInt(level) / 4 + 1;
        return true;
    }

    private boolean isFull() {
        for (int v : board)
            if (v == 0) return false;
        return true;
    }

    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}

