public class PlayerInfo {
    private String name;
    private int score;
    private int level;
    private int combo;
    private int moves;
    private static final long serialVersionUID = 1L;

    public PlayerInfo(String name) {
        this.name = name;
        this.score = 0;
        this.level = 1;
        this.combo = 0;
        this.moves = 0;
    }

    public PlayerInfo(String name, int score, int level) {
        this.name = name;
        this.score = score;
        this.level = level;
    }

    // Getter 和 Setter 方法
    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }


    public int getLevel() {
        return level;
    }


    public void setLevel(int level) {
        this.level = level;
    }


    public int getCombo() {
        return combo;
    }


    public void setCombo(int combo) {
        this.combo = combo;
    }


    public int getMoves() {
        return moves;
    }


    public void setMoves(int moves) {
        this.moves = moves;
    }
}
