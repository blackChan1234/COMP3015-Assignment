import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameEngine {
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random(0);

    private static GameEngine instance;
    private boolean gameOver;

    private String playerName;
    private int level = 1;
    private int score;
    private int combo;
    private int totalMoveCount;
    private int numOfTilesMoved;

    private final Map<String, Runnable> actionMap = new HashMap<>();

    private GameEngine() {
        // define a hash map to contain the links from the actions to the corresponding methods
        actionMap.put("UP", this::moveUp);
        actionMap.put("DOWN", this::moveDown);
        actionMap.put("LEFT", this::moveLeft);
        actionMap.put("RIGHT", this::moveRight);

        // start the first round
        nextRound();
    }

    public static GameEngine getInstance() {
        if (instance == null)
            instance = new GameEngine();
        return instance;
    }

    /**
     * Generate a new random value and determine the game status.
     * @return true if the next round can be started, otherwise false.
     */
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
                    try {
                        Database.putScore(playerName, score, level);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * move the values downward and merge them.
     */
    private void moveDown() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
    }

    /**
     * move the values upward and merge them.
     */
    private void moveUp() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
    }

    /**
     * move the values rightward and merge them.
     */
    private void moveRight() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i);
    }

    /**
     * move the values leftward and merge them.
     */
    private void moveLeft() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(-1, i, SIZE - 1 + i);
    }

    /**
     * Move and merge the values in a specific row or column. The moving direction and the specific row or column is determined by d, s, and l.
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
