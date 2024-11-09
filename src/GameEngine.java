import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class GameEngine {

    private GameWindow gameWindow;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    private boolean gameOver = false;
    private static GameEngine instance;
    private String playerId;
    Socket clientSocket;
    DataOutputStream out;
    DataInputStream in;
    List<PlayerInfo> players = new ArrayList<>();
    Thread receiverThread;

    private String playerName;
    public LobbyController lobbyController;

    private List<PlayerInfo> playerList = new ArrayList<>();
    private String currentPlayerId;

    public void setLobbyController(LobbyController lobbyController) {
        this.lobbyController = lobbyController;
    }

    public boolean isFirstPlayer() {
        return isFirstPlayer;
    }

    private boolean isFirstPlayer = false;

    public void connectToServer(String serverIP, int serverPort, String playerName) {
        this.playerName = playerName;
        try {
            clientSocket = new Socket(serverIP, serverPort);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());

            out.writeUTF(playerName);
            out.flush();
            System.out.println("Client: Sent player name to server: " + playerName);
            if (gameWindow != null) {
                gameWindow.showLobby();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                while (true) {
                    byte data = in.readByte();
                    System.out.println("Client: Received data: " + (char)data);
                    switch (data) {
                        case 'L':
                            // update the player list
                            List<PlayerInfo> playerslist = receivePlayerList(in);
                            playerList = playerslist;
                            System.out.println(playerList);
                            System.out.println(lobbyController);
                            if (lobbyController != null) {
                                lobbyController.updatePlayerList(playerslist);
                            }
                            break;
                        case 'F':
                            // Server notifies whether the player is the first player
                            isFirstPlayer = in.readBoolean();
                            if (lobbyController != null) {
                                lobbyController.setFirstPlayer(isFirstPlayer);
                            }
                            break;
                        case 'A':
                            // Download array
                            receiveArray(in);
                            break;
                        case 'P':
                            // Receive other player data
                            List<PlayerInfo> players = receivePlayersInfo(in);
                            // Update window
                            gameWindow.updateOtherPlayersData(players);
                            break;
                        case 'S':
                            System.out.println("Client: Received game over scores.");
                            List<PlayerInfo> gameOverScores = receiveGameOverScores(in);
                            gameOver = true; // Set game over flag
                            Platform.runLater(() -> gameWindow.displayGameOverScores(gameOverScores));
                            break;
                        case 'T':
                            System.out.println("Client: Received top scores.");
                            List<HashMap<String, String>> topScores = receiveTopScores(in);
                            Platform.runLater(() -> gameWindow.displayTopScores(topScores));
                            break;
                        case 'N':
                            String currentPlayerId = in.readUTF();
                            System.out.println("Client: Current player ID is " + currentPlayerId);
                            updateCurrentPlayer(currentPlayerId);
                            break;
                        case 'G':
                            // Game start notification
                            if (lobbyController != null) {
                                lobbyController.hideLobby();
                            }
                            Platform.runLater(() -> gameWindow.showGameStart());
                            break;
                        case 'I':
                            // Receive player ID from server
                            String playerId = in.readUTF();
                            this.playerId = playerId;
                            System.out.println("Client: Received player ID: " + playerId);
                            break;
                        case 'M':
                            // notify
                            String message = in.readUTF();
                            Platform.runLater(() -> gameWindow.showMessage(message));
                            break;
                        default:
                            // Print the direction
                            System.out.println("Unknown data: " + (char)data);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        receiverThread.start();
    }

    private List<PlayerInfo> receivePlayerList(DataInputStream in) throws IOException {
        int numPlayers = in.readInt();
        List<PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String name = in.readUTF();
            String playerId = in.readUTF();
            PlayerInfo player = new PlayerInfo(name, playerId);
            players.add(player);
        }
        // Update the players list in GameEngine
        this.players = players;
        return players;
    }


    public void sendStartGameCommand() throws IOException {
        out.writeByte('S'); // 'S' mean game start
        out.flush();
        System.out.println("Client: Sent start game command.");
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
        List<PlayerInfo> receivedPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String playerId = in.readUTF();
            String name = in.readUTF();
            int score = in.readInt();
            int level = in.readInt();
            int combo = in.readInt();
            int moves = in.readInt();
            PlayerInfo player = new PlayerInfo(name, playerId);
            player.setScore(score);
            player.setLevel(level);
            player.setCombo(combo);
            player.setMoves(moves);
            receivedPlayers.add(player);
        }
        // Update the players list
        this.players = receivedPlayers;
        return receivedPlayers;
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

    public int[] getBoardData() {
        synchronized (board) {
            return board.clone(); // Return a copy of the board data
        }
    }
    public void sendPuzzleDataToServer(int[] boardData) throws IOException {
        out.writeByte('P'); // 'U' indicates upload puzzle
        out.flush(); // Ensure the command byte is sent

        // Send the length of the data (number of ints)
        out.writeInt(boardData.length);

        // Send the int[] data directly
        for (int value : boardData) {
            out.writeInt(value);
        }
        out.flush(); // Ensure all data is sent
        System.out.println("Client: Sent puzzle data to server.");
    }


    public void moveMerge(String dir) throws IOException {
        if (!isMyTurn()) {
            // 提示玩家不是他的回合
            System.out.println("Not your turn!");
            return;
        }

        System.out.println("Client: Sending direction: " + dir.charAt(0));
        // Send direction to server
        out.writeByte(dir.charAt(0));
        out.flush();
    }

    private boolean isMyTurn() {
        return currentPlayerId != null && currentPlayerId.equals(playerId);
    }

    public void updateCurrentPlayer(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        if (gameWindow != null) {
            gameWindow.updateCurrentPlayer(currentPlayerId);
        }
    }


    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
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

    public void requestTopScores() throws IOException {
        out.writeByte('t');
        out.flush();
    }

    public void stop() {
        try {
            if (receiverThread != null && receiverThread.isAlive()) {
                receiverThread.interrupt();
            }
            closeResource(in);
            closeResource(out);
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

}
