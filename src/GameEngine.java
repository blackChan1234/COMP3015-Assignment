import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;

public class GameEngine {

    private GameWindow gameWindow;
//    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
//    Random random = new Random(0);
private boolean gameOver = false;
    private static GameEngine instance;

    Socket clientSocket;
    DataOutputStream out;
    DataInputStream in;

    Thread receiverThread = new Thread(()->{
        try {
            in = new DataInputStream(clientSocket.getInputStream());

            while(true) {
                byte  data = in.readByte();

                switch (data){
                    case 'A':
                        //download array
                        receiveArray(in);
                        break;
                    case 'P':
                        // receive other player data
                        List<PlayerInfo> players = receivePlayersInfo(in);
                        // update window
                        gameWindow.updateOtherPlayersData(players);
                        break;
                    case 'S':
                        System.out.println("Client: Received game over scores.");
                        List<PlayerInfo> gameOverScores = receiveGameOverScores(in);
                        gameOver = true; // Set game over flag
                        Platform.runLater(() -> displayGameOverScores(gameOverScores));
                        break;
                    case 'T':
                        System.out.println("Client: Received top scores.");
                        List<HashMap<String, String>> topScores = receiveTopScores(in);
                        Platform.runLater(() -> displayTopScores(topScores));
                        break;

                    default:
                        //print the direction
                        System.out.println(data);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(); /// debugging only, remove it before production
        }
    });
    public void requestTopScores() throws IOException {
        out.writeByte('t');
        out.flush();
    }

    private void displayTopScores(List<HashMap<String, String>> topScores) {
        // Convert topScores list to ObservableList<String>
        ObservableList<String> items = FXCollections.observableArrayList();
        topScores.forEach(data -> {
            String scoreStr = String.format("%-10s | Score: %-5s | Level: %-3s | Time: %s",
                    data.get("name"), data.get("score"), data.get("level"), data.get("time").substring(0, 16));
            items.add(scoreStr);
        });

        // Use ScoreboardWindow to display the scores
        try {
            ScoreboardWindow scoreboard = new ScoreboardWindow(items, "Top 10 Scores");
            Platform.runLater(() -> scoreboard.show());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayGameOverScores(List<PlayerInfo> scores) {
        // Convert PlayerInfo list to ObservableList<String>
        ObservableList<String> items = FXCollections.observableArrayList();
        scores.forEach(player -> {
            String scoreStr = String.format("%-10s | Score: %-5d | Level: %-3d",
                    player.getName(), player.getScore(), player.getLevel());
            items.add(scoreStr);
        });

        // Use ScoreboardWindow to display the scores
        try {
            ScoreboardWindow scoreboard = new ScoreboardWindow(items, "Game Over - Scores");
            scoreboard.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void receiveArray(DataInputStream in) throws IOException {
        synchronized (board) {
            int size = in.readInt();
            System.out.println("Client: Received array size: " + size);
            if (size != board.length) {
                System.err.println("Error: Expected board size " + board.length + ", but received " + size);
                size = Math.min(size, board.length);
            }
            for (int i = 0; i < size; i++) {
                board[i] = in.readInt();

                if (board[i] < 0 || board[i] >= GameWindow.images.length) {
                    System.err.println("Invalid board value at index " + i + ": " + board[i]);
                    board[i] = 0;
                }
                System.out.print(board[i] + " ");
            }
            System.out.println();
        }
    }


    private List<PlayerInfo> receivePlayersInfo(DataInputStream in) throws IOException {
        int numPlayers = in.readInt();
        List<PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name = in.readUTF();
            int score = in.readInt();
            int level = in.readInt();
            int combo = in.readInt();
            int moves = in.readInt();
            PlayerInfo player = new PlayerInfo(name);
            player.setScore(score);
            player.setLevel(level);
            player.setCombo(combo);
            player.setMoves(moves);
            players.add(player);
        }
        return players;
    }
    private List<PlayerInfo> receiveGameOverScores(DataInputStream in) throws IOException {
        int numPlayers = in.readInt();
        List<PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name = in.readUTF();
            int score = in.readInt();
            int level = in.readInt();

            players.add(new PlayerInfo(name, score, level));
        }
        return players;
    }

    private List<HashMap<String, String>> receiveTopScores(DataInputStream in) throws IOException {
        int numScores = in.readInt();
        List<HashMap<String, String>> topScores = new ArrayList<>();
        for (int i = 0; i < numScores; i++) {
            HashMap<String, String> scoreData = new HashMap<>();
            scoreData.put("name", in.readUTF());
            scoreData.put("score", String.valueOf(in.readInt()));
            scoreData.put("level", String.valueOf(in.readInt()));
            scoreData.put("time", in.readUTF());
            topScores.add(scoreData);
        }
        return topScores;
    }

public void connectToServer(String serverIP, int serverPort, String playerName) {
    try {
        clientSocket = new Socket(serverIP, serverPort);
        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());


        out.writeUTF(playerName);
        out.flush();

        receiverThread.start();
    } catch (IOException ex) {
        ex.printStackTrace();
        System.exit(-1);
    }
}
    public void setGameWindow(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }

    public static GameEngine getInstance() {
        if (instance == null)
            instance = new GameEngine();
        return instance;
    }


    public void moveMerge(String dir) throws IOException {

        System.out.println("Client: Sending direction: " + dir.charAt(0));
        /// send direction to server
        out.writeByte(dir.charAt(0));
        out.flush();



    }



    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
        }
    }

}
