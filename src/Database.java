import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Database {
    final static String url = "jdbc:sqlite:data/battleJoker.db";

    public static ArrayList<HashMap<String, String>> getScores() throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM scores ORDER BY score DESC LIMIT 10";
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(url);
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                HashMap<String, String> m = new HashMap<>();
                m.put("name", resultSet.getString("name"));
                m.put("score", resultSet.getString("score"));
                m.put("level", resultSet.getString("level"));
                m.put("time", resultSet.getString("time"));
                data.add(m);
            }
        }
        return data;
    }

    public static void putScore(String name, int score, int level) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO scores (name, score, level, time) VALUES (?, ?, ?, datetime('now'))";
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setInt(2, score);
            statement.setInt(3, level);
            statement.executeUpdate();
        }
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        putScore("Bob", 1000, 13);
        getScores().forEach(map -> {
            System.out.println(map.get("name"));
        });
    }
}
