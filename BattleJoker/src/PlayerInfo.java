// PlayerInfo.java
public class PlayerInfo {
    private String name;
    private int score;
    private int level;
    private int combo;

    public PlayerInfo(String name, int score, int level, int combo) {
        this.name = name;
        this.score = score;
        this.level = level;
        this.combo = combo;
    }

    // Getters and Setters for JavaFX PropertyValueFactory
    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getCombo() {
        return combo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setCombo(int combo) {
        this.combo = combo;
    }
}
