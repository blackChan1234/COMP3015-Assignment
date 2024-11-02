import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.util.Optional;

public class ScoreboardClient extends Application {

    @FXML
    private ListView<String> scoreList;

    // Multicast settings
    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 4446;
    private volatile boolean running = true;

    private ObservableList<String> scoresObservableList;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/scoreboardUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Battle Joker - Scoreboard");
        primaryStage.setMinWidth(450);
        primaryStage.setMinHeight(500);

        primaryStage.setOnCloseRequest(event -> {
            running = false;
        });

        setFont(14);
        scoresObservableList = FXCollections.observableArrayList();
        scoreList.setItems(scoresObservableList);

        primaryStage.show();

        // Start listening to multicast
        new Thread(new MulticastListener()).start();
    }

    private void setFont(int fontSize) {
        scoreList.setCellFactory(param -> {
            javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<>();
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                cell.setFont(Font.font("Courier New", fontSize));
            } else if (osName.contains("mac")) {
                cell.setFont(Font.font("Menlo", fontSize));
            } else {
                cell.setFont(Font.font("Monospaced", fontSize));
            }
            return cell;
        });
    }

    private class MulticastListener implements Runnable {
        @Override
        public void run() {
            MulticastSocket socket = null;
            try {
                InetAddress group = InetAddress.getByName(MULTICAST_IP);
                socket = new MulticastSocket(MULTICAST_PORT);
                socket.joinGroup(group);

                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                while (running) {
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    if (received.startsWith("TOP_SCORES:")) {
                        String scoresData = received.substring("TOP_SCORES:".length());
                        updateScores(scoresData);
                    }
                }

                socket.leaveGroup(group);
            } catch (IOException e) {
                if (running) { // Only print if not shutting down
                    e.printStackTrace();
                    showError("Error receiving multicast data: " + e.getMessage());
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }

    private void updateScores(String scoresData) {
        Platform.runLater(() -> {
            scoresObservableList.clear();
            String[] scores = scoresData.split(";");
            for (String s : scores) {
                String[] parts = s.split(",");
                if (parts.length < 4) continue;
                String name = parts[0];
                String score = parts[1];
                String level = parts[2];
                String time = parts[3];
                String display = String.format("%-10s | %-5s | %-5s | %s", name, score, level, time.substring(0, 16));
                scoresObservableList.add(display);
            }
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
            alert.setHeaderText("Error");
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
