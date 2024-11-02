import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Database {
    final static String url = "jdbc:sqlite:data/battleJoker.db";
    static Connection conn;

    public static void connect() throws SQLException, ClassNotFoundException {
        if (conn == null) {
            conn = DriverManager.getConnection(url);
            initialize();
        }
    }

    public static void putScore(String name, int score, int level) {
        String sql = "INSERT INTO scores (name, score, level, time) VALUES (?, ?, ?, datetime('now'))";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, score);
            pstmt.setInt(3, level);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initialize() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS scores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "score INTEGER NOT NULL," +
                "level INTEGER NOT NULL," +
                "time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";
        Statement stmt = conn.createStatement();
        stmt.execute(createTableSQL);
    }

    public static void disconnect() throws SQLException {
        if (conn != null)
            conn.close();
    }

    public static ArrayList<HashMap<String, String>> getScores() throws SQLException {
        String sql = "SELECT * FROM scores ORDER BY score DESC LIMIT 10";
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        while (resultSet.next()) {
            HashMap<String, String> m = new HashMap<>();
            m.put("name", resultSet.getString("name"));
            m.put("score", resultSet.getString("score"));
            m.put("level", resultSet.getString("level"));
            m.put("time", resultSet.getString("time"));
            data.add(m);
        }
        return data;
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        connect();
        putScore("Bob", 1000, 13);
        getScores().forEach(map -> {
            System.out.println(map.get("name"));
        });
    }
}
