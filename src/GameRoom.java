import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;

public class GameRoom {
    private static final int MAX_PLAYERS = 4;
    private List<Socket> players = new ArrayList<>();
    private Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
    private Map<Socket, DataOutputStream> clientOutputMap = new HashMap<>();
    private int currentPlayerIndex = 0;
    private int movesRemaining = 4;
    private boolean gameStarted = false;
    private boolean puzzleUploadedInGame = false;
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    private int[] previousBoardState = new int[SIZE * SIZE];
    Random random = new Random();
    private int combo;
    private int numOfTilesMoved;
    private int totalMoveCount;
    private boolean gameOver;
    private int level = 1;
    private int score;
    private final Map<String, Runnable> actionMap = new HashMap<>();
    private Map<Socket, String> clientSocketIdMap = new HashMap<>();


    public GameRoom() {
        actionMap.put("U", this::moveUp);
        actionMap.put("D", this::moveDown);
        actionMap.put("L", this::moveLeft);
        actionMap.put("R", this::moveRight);
        nextRound();
    }

    // add player to room
    public boolean addPlayer(Socket clientSocket, PlayerInfo playerInfo, DataOutputStream out) {
        if (gameStarted) {
            // Game has already started, cannot join
            return false;
        }
        players.add(clientSocket);
        playerInfoMap.put(playerInfo.getPlayerId(), playerInfo);
        clientOutputMap.put(clientSocket, out);
        clientSocketIdMap.put(clientSocket, playerInfo.getPlayerId());
        // Send whether the player is the first player
        try {
            out.writeByte('F'); // 'F' indicates first player notification
            out.writeBoolean(isFirstPlayer(clientSocket));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Send updated player list to all players
        sendPlayerListToAll();

        return true;
    }


    // Check if the room is full
    public boolean PlayerisFull() {
        return players.size() >= MAX_PLAYERS;
    }

    // Check if the game has started
    public boolean isStarted() {
        return gameStarted;
    }

    // Check if it is the first player
    public boolean isFirstPlayer(Socket clientSocket) {
        return players.get(0).equals(clientSocket);
    }

    // start game
    public void startGame(Socket starterSocket) {
        if (players.size() < 2) {
            String message = "At least 2 players are required to start the game.";
            if (starterSocket != null) {
                DataOutputStream out = clientOutputMap.get(starterSocket);
                try {
                    out.writeByte('M');
                    out.writeUTF(message);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {

                for (DataOutputStream out : clientOutputMap.values()) {
                    try {
                        out.writeByte('M');
                        out.writeUTF(message);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
        gameStarted = true;
        System.out.println("Game started!");
        notifyAllPlayersGameStart();
    }

    public void sendPlayerListToAll() {
        Iterator<Socket> iterator = players.iterator();
        while (iterator.hasNext()) {
            Socket s = iterator.next();
            if (s.isClosed() || !s.isConnected()) {
                // Remove disconnected client
                iterator.remove();
                clientOutputMap.remove(s);
                clientSocketIdMap.remove(s);
                continue;
            }
            DataOutputStream out = clientOutputMap.get(s);
            String playerId = null;
            try {
                out.writeByte('L'); // 'L' indicates player list
                out.writeInt(players.size());
                for (Socket playerSocket : players) {
                    playerId = clientSocketIdMap.get(playerSocket);
                    PlayerInfo playerInfo = playerInfoMap.get(playerId);
                    out.writeUTF(playerInfo.getName());
                    out.writeUTF(playerId);
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                // If write fails, client might be disconnected, remove client
                iterator.remove();
                clientOutputMap.remove(s);
                clientSocketIdMap.remove(s);
                playerInfoMap.remove(playerId);
            }
        }
    }


    private void notifyAllPlayersGameStart() {
        for (Socket s : players) {
            DataOutputStream out = clientOutputMap.get(s);
            try {
                out.writeByte('G'); // 'G' mean game start
                out.flush();
                sendGameStateToAll();
                System.out.println("Notifying all players that the game has started.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        notifyCurrentPlayer();
    }
    private void updatePlayerDataAndBroadcast(Socket clientSocket) {
        // Update the player's data
        String playerId = clientSocketIdMap.get(clientSocket);
        PlayerInfo playerInfo = playerInfoMap.get(playerId);

        if (playerInfo != null) {
            playerInfo.setScore(score);
            playerInfo.setLevel(level);
            playerInfo.setCombo(combo);
            playerInfo.setMoves(totalMoveCount);
        }

        // Send updated game state to all clients
        sendGameStateToAll();
    }

    private void notifyCurrentPlayer() {
        Socket currentPlayerSocket = players.get(currentPlayerIndex);
        String currentPlayerId = clientSocketIdMap.get(currentPlayerSocket);
        PlayerInfo currentPlayerInfo = playerInfoMap.get(currentPlayerId);
        String currentPlayerName = currentPlayerInfo.getName();
        System.out.println("Notifying players that current player is: " + currentPlayerName + " (ID: " + currentPlayerId + ")");
        for (Socket s : players) {
            DataOutputStream out = clientOutputMap.get(s);
            try {
                out.writeByte('N'); // 'N' indicates notify current player
                out.writeUTF(currentPlayerId); // Send playerId instead of name
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public void handleClientData(char data, Socket clientSocket, DataInputStream in) throws SQLException {
        if (!gameStarted) {
            if (data == 'S' && isFirstPlayer(clientSocket)) {
                startGame(clientSocket);
            } else {
                System.out.println("Received unexpected data before game started: '" + data + "'");
            }
        } else {
            if (actionMap.containsKey(String.valueOf(data))) {
                moveMerge(String.valueOf(data), clientSocket);
                updatePlayerDataAndBroadcast(clientSocket);
            } else if (data == 'P') {
                System.out.println("Received 'U' command from client.");
                // Handle upload puzzle data
                receivePuzzleData(clientSocket, in);
            }  else {
                System.out.println("Unknown command during game: '" + data + "'");
            }
        }
    }




    private void receivePuzzleData(Socket clientSocket, DataInputStream in) {
        try {
            if (puzzleUploadedInGame) {
                // Send an error message to the client
                sendMessageToClient(clientSocket, "Puzzle upload has already been used in this game.");
                return;
            }

            int length = in.readInt();
            System.out.println("Server: Receiving puzzle data of length " + length);

            int[] newBoard = new int[length];
            for (int i = 0; i < length; i++) {
                newBoard[i] = in.readInt();
            }

            // Validate the received board data
            if (validateBoardData(newBoard)) {
                // Update the game board
                synchronized (board) {
                    System.arraycopy(newBoard, 0, board, 0, board.length);
                }

                // Reset game state variables
                resetGameState();

                // Set the puzzleUploadedInGame to true
                puzzleUploadedInGame = true;

                // Set the currentPlayerIndex to the uploader
                currentPlayerIndex = players.indexOf(clientSocket);
                movesRemaining = 4; // Reset movesRemaining

                // Broadcast the updated board to all clients
                sendGameStateToAll();

                // Get the player's info using playerId
                String playerId = clientSocketIdMap.get(clientSocket);
                PlayerInfo playerInfo = playerInfoMap.get(playerId);

                // Notify all players about the new puzzle
                broadcastMessage("Puzzle updated by " + playerInfo.getName());

                // Notify all players of the new current player
                notifyCurrentPlayer();

                System.out.println("Puzzle data uploaded by player " + playerInfo.getName());
            } else {
                // Send an error message to the client

                sendMessageToClient(clientSocket, "Invalid puzzle data uploaded.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendMessageToClient(clientSocket, "Error processing uploaded puzzle data.");
        }
    }


    private boolean validateBoardData(int[] boardData) {
        if (boardData.length != SIZE * SIZE) {
            return false;
        }
        for (int value : boardData) {
            if (value < 0 || value >= LIMIT) {
                return false;
            }
        }
        return true;
    }

    private void resetGameState() {
        combo = 0;
        numOfTilesMoved = 0;
        totalMoveCount = 0;
        gameOver = false;
        level = 1;
        score = 0;
        puzzleUploadedInGame = false;
        // Reset player data
        for (PlayerInfo player : playerInfoMap.values()) {
            player.setScore(0);
            player.setLevel(1);
            player.setCombo(0);
            player.setMoves(0);
        }
    }

    private void broadcastMessage(String message) {
        for (DataOutputStream out : clientOutputMap.values()) {
            try {
                out.writeByte('M'); // 'M' indicates message
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // handle move
    public void moveMerge(String dir, Socket clientSocket) throws SQLException {
        if (!isPlayerTurn(clientSocket)) {
            // Non-current player's request, ignore
            return;
        }
        synchronized (board) {
            if (actionMap.containsKey(dir)) {

                combo = numOfTilesMoved = 0;

                // Execute move logic
                actionMap.get(dir).run();

                // Calculate new score
                 score += combo / 5 * 2;

                // Check if game is over
                if (numOfTilesMoved > 0) {
                    totalMoveCount++;
                    gameOver = level == LIMIT || !nextRound();
                } else
                    gameOver = isFull();

                movesRemaining--;
                if (movesRemaining == 0) {
                    // reset move
                    movesRemaining = 4;
                    // exchange to next player
                    currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
                    notifyCurrentPlayer();
                }

                updatePlayerDataAndBroadcast(clientSocket);
                // Check if game is over
                if (gameOver) {
                    checkGameOver();
                }
            }
        }
    }

    private boolean isPlayerTurn(Socket clientSocket) {
        return players.get(currentPlayerIndex).equals(clientSocket);
    }

    // send the game state to all player
    private void sendGameStateToAll() {
        for (Socket s : players) {
            DataOutputStream outClient = clientOutputMap.get(s);
            try {
                sendArray(outClient);
                sendPlayersInfo(outClient);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // send  board
    void sendArray(DataOutputStream outClient) throws IOException {
        outClient.writeByte('A');
        outClient.writeInt(board.length);
        for (int v : board) {
            outClient.writeInt(v);
        }
        outClient.flush();
    }

    // send player Info
    private void sendPlayersInfo(DataOutputStream out) throws IOException {
        out.writeByte('P');
        synchronized (playerInfoMap) {
            out.writeInt(playerInfoMap.size());
            for (PlayerInfo playerInfo : playerInfoMap.values()) {
                out.writeUTF(playerInfo.getPlayerId());
                out.writeUTF(playerInfo.getName());
                out.writeInt(playerInfo.getScore());
                out.writeInt(playerInfo.getLevel());
                out.writeInt(playerInfo.getCombo());
                out.writeInt(playerInfo.getMoves());
            }
        }
        out.flush();
    }
    private void sendMessageToClient(Socket clientSocket, String message) {
        DataOutputStream out = clientOutputMap.get(clientSocket);
        if (out != null) {
            try {
                out.writeByte('M'); // 'M' indicates a message
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // game over
    public synchronized void checkGameOver() throws SQLException {
        if (gameOver) {
            System.out.println("Game over. Sending scores to clients.");
            PlayerInfo winner = getWinner();

            // save the winner score to the database
            try {

                Database.putScore(winner.getName(), winner.getScore(), winner.getLevel());
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            sendGameOverScores();
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
        for (Socket s : players) {
            DataOutputStream out = clientOutputMap.get(s);
            try {
                out.writeByte('S'); // 'S' indicates game over scores
                // Convert the playerInfoMap values to a list
                List<PlayerInfo> playerList = new ArrayList<>(playerInfoMap.values());

                // Sort the list in descending order by score
                playerList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

                // Send the size of the list
                out.writeInt(playerList.size());

                // Iterate over the sorted list and send player data
                for (PlayerInfo player : playerList) {
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

    // game logic
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

    private boolean nextRound() {
        if (isFull()) return false;
        int i;
        do {
            i = random.nextInt(SIZE * SIZE);
        } while (board[i] > 0);


        board[i] = random.nextInt(level) / 4 + 1;
        return true;
    }

    boolean isFull() {
        for (int v : board)
            if (v == 0) return false;
        return true;
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


    public void close() {
        for (Socket s : players) {
            try {
                DataOutputStream out = clientOutputMap.get(s);
                if (out != null) out.close();
                if (!s.isClosed()) s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removePlayer(Socket clientSocket) {
        String playerId = clientSocketIdMap.get(clientSocket);
        players.remove(clientSocket);
        clientOutputMap.remove(clientSocket);
        playerInfoMap.remove(playerId);
        clientSocketIdMap.remove(clientSocket);
        sendPlayerListToAll();
    }
}
