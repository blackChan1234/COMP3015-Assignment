import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameEngine {
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random();

    private String playerName;
    private int level = 1;
    private int score;
    private int combo;
    private int totalMoveCount;
    private int numOfTilesMoved;
    private boolean gameOver;

    public GameEngine(String playerName) {
        this.playerName = playerName;
        // Initialize the game board
        reset();
    }

    public void reset() {
        synchronized (board) {
            Arrays.fill(board, 0);
        }
        this.score = 0;
        this.level = 1;
        this.combo = 0;
        this.totalMoveCount = 0;
        this.numOfTilesMoved = 0;
        this.gameOver = false;
        nextRound();
    }

    /**
     * Generate a new random value and determine the game status.
     * @return true if the next round can be started, otherwise false.
     */
    private boolean nextRound() {
        if (isFull()) return false;
        int i;

        // Randomly find an empty place
        do {
            i = random.nextInt(SIZE * SIZE);
        } while (board[i] > 0);

        // Randomly generate a card based on the existing level, and assign it to the selected place
        board[i] = random.nextInt(level) / 4 + 1;
        return true;
    }

    public String serializeState() {
        StringBuilder sb = new StringBuilder();
        synchronized (board) {
            for (int value : board) {
                sb.append(value).append(",");
            }
        }
        sb.append("|").append(score).append("|").append(level);
        return sb.toString();
    }

    /**
     * Retrieves top 10 scores in a formatted string.
     * Placeholder implementation; replace with actual retrieval logic if needed.
     */
    public String getScores() {
        // Implement a method to get scores, possibly from the database
        // For simplicity, return a placeholder string
        return "Player1:1000,Player2:800"; // Example format
    }

    public void setBoard(String[] boardValues) {
        synchronized (board) {
            for (int i = 0; i < board.length; i++) {
                board[i] = Integer.parseInt(boardValues[i]);
            }
        }
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * @return true if all blocks are occupied.
     */
    private boolean isFull() {
        for (int v : board)
            if (v == 0) return false;
        return true;
    }

    /**
     * Move and combine the cards based on the input direction
     * @param dir
     */
    public void moveMerge(String dir) {
        synchronized (board) {
            combo = numOfTilesMoved = 0;

            // Determine the move direction
            switch (dir.toUpperCase()) {
                case "UP":
                    moveUp();
                    break;
                case "DOWN":
                    moveDown();
                    break;
                case "LEFT":
                    moveLeft();
                    break;
                case "RIGHT":
                    moveRight();
                    break;
                default:
                    return; // Invalid direction
            }

            // Calculate the new score
            score += combo / 5 * 2;

            // Determine whether the game is over or not
            if (numOfTilesMoved > 0) {
                totalMoveCount++;
                gameOver = level == LIMIT || !nextRound();
            } else {
                gameOver = isFull();
            }

            // Update the database if the game is over
            if (gameOver) {
                try {
                    Database.putScore(playerName, score, level);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Move the values downward and merge them.
     */
    private void moveDown() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
    }

    /**
     * Move the values upward and merge them.
     */
    private void moveUp() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
    }

    /**
     * Move the values rightward and merge them.
     */
    private void moveRight() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i);
    }

    /**
     * Move the values leftward and merge them.
     */
    private void moveLeft() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(-1, i, SIZE - 1 + i);
    }

    /**
     * Move and merge the values in a specific row or column.
     * @param d - move distance
     * @param s - the index of the first element in the row or column
     * @param l - the index of the last element in the row or column.
     */
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

    public String getPlayerName() {
        return playerName;
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
}
