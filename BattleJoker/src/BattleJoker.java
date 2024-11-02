import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

public class BattleJoker extends Application {
    Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private GameWindow gameWindow;
    private String playerName;

    @Override
    public void start(Stage primaryStage) {
        try {
            GetNameDialog dialog = new GetNameDialog();
            playerName = dialog.getPlayername();
            gameWindow = new GameWindow(primaryStage, this); // Pass reference to BattleJoker

            gameWindow.setName(playerName);

            ServerConnectionDialog serverDialog = new ServerConnectionDialog();
            String serverIP = serverDialog.getServerIP();
            int serverPort = serverDialog.getServerPort();

            // Connect to server
            try {
                clientSocket = new Socket(serverIP, serverPort);
            } catch (IOException e) {
                showErrorAndExit("Unable to connect to the server. Please ensure the server is running and the IP/Port are correct.");
                return;
            }

            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Send player name to server
            out.println(playerName);

            // Start a thread to listen for server messages
            new Thread(new ServerListener()).start();

            Database.connect();
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorAndExit("An unexpected error occurred: " + ex.getMessage());
        }
    }

    private void showErrorAndExit(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            Platform.exit();
        });
    }

    @Override
    public void stop() {
        try {
            Database.disconnect();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (SQLException | IOException ignored) {}
    }

    public void sendMove(String move) {
        out.println("MOVE:" + move);
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("GAME_STATE:")) {
                        String state = message.substring("GAME_STATE:".length());
                        gameWindow.updateGameState(state);
                    } else if (message.startsWith("GAME_OVER:")) {
                        String scores = message.substring("GAME_OVER:".length());
                        gameWindow.displayGameOver(scores);
                    } else if (message.startsWith("PLAYER_JOINED:")) {
                        String name = message.substring("PLAYER_JOINED:".length());
                        gameWindow.notifyPlayerJoined(name);
                    } else if (message.startsWith("PLAYER_LEFT:")) {
                        String name = message.substring("PLAYER_LEFT:".length());
                        gameWindow.notifyPlayerLeft(name);
                    } else if (message.startsWith("ALL_PLAYERS:")) {
                        String playersInfo = message.substring("ALL_PLAYERS:".length());
                        gameWindow.updateOtherPlayers(playersInfo);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                showErrorAndExit("Disconnected from server.");
            }
        }
    }

    public static void main(String[] args) {
        System.setErr(new FilteredStream(System.err));  // Suppress specific warnings
        launch();
    }
}

class FilteredStream extends PrintStream {

    public FilteredStream(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String x) {
        if (x != null && !x.contains("SLF4J: "))
            super.println(x);
    }

    @Override
    public void print(String x) {
        if (x!= null && !x.contains("WARNING: Loading FXML document with JavaFX API of version 18"))
            super.print(x);
    }
}
