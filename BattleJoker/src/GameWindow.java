import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class GameWindow {
    @FXML
    MenuBar menuBar;

    @FXML
    Label nameLabel;

    @FXML
    Label scoreLabel;

    @FXML
    Label levelLabel;

    @FXML
    Label comboLabel;

    @FXML
    Label moveCountLabel;

    @FXML
    Pane boardPane;

    @FXML
    Canvas canvas;

    @FXML
    ListView<String> otherPlayersList; // ListView for other players' info

    Stage stage;
    AnimationTimer animationTimer;

    final String[] symbols = {"bg", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "Joker"};
    final Image[] images = new Image[symbols.length];
    final GameEngine gameEngine;
    final String imagePath = "images/";
    BattleJoker battleJoker; // Reference to the main client class

    // To keep track of other players' info
    private Map<String, PlayerInfo> otherPlayers = new HashMap<>();

    public GameWindow(Stage stage, BattleJoker battleJoker) throws IOException {
        this.battleJoker = battleJoker;
        gameEngine = new GameEngine("Player"); // Placeholder, will be set correctly

        loadImages();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        this.stage = stage;

        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        stage.widthProperty().addListener(w -> onWidthChangedWindow(((ReadOnlyDoubleProperty) w).getValue()));
        stage.heightProperty().addListener(h -> onHeightChangedWindow(((ReadOnlyDoubleProperty) h).getValue()));
        stage.setOnCloseRequest(event -> quit());

        stage.show();
        initCanvas();

        gameStart();
    }

    private String playerNamePlaceholder() {
        // Temporarily set to "Player" until setName is called
        return "Player";
    }

    private void gameStart() {
        animationTimer.start();
    }

    private void loadImages() throws IOException {
        for (int i = 0; i < symbols.length; i++)
            images[i] = new Image(Files.newInputStream(Paths.get(imagePath + symbols[i] + ".png")));
    }

    private void initCanvas() {
        canvas.setOnKeyPressed(event -> {
            String move = event.getCode().toString();
            // Send the move to the server via BattleJoker
            battleJoker.sendMove(move);
        });

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
                // Game over is handled via server messages
            }
        };
        canvas.requestFocus();
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        double sceneSize = Math.min(w, h);
        double blockSize = sceneSize / GameEngine.SIZE;
        double padding = blockSize * .05;
        double startX = (w - sceneSize) / 2;
        double startY = (h - sceneSize) / 2;
        double cardSize = blockSize - (padding * 2);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double y = startY;
        int v;

        // Draw the background and cards from left to right, and top to bottom.
        for (int i = 0; i < GameEngine.SIZE; i++) {
            double x = startX;
            for (int j = 0; j < GameEngine.SIZE; j++) {
                gc.drawImage(images[0], x, y, blockSize, blockSize);  // Draw the background

                v = gameEngine.getValue(i, j);

                if (v > 0)  // if a card is in the place, draw it
                    gc.drawImage(images[v], x + padding, y + padding, cardSize, cardSize);

                x += blockSize;
            }
            y += blockSize;
        }
    }

    public void updateGameState(String state) {
        // Parse the state string and update the game board
        // State format: board_values|score|level
        String[] parts = state.split("\\|");
        if (parts.length < 3) {
            System.err.println("Invalid GAME_STATE format.");
            return;
        }
        String[] boardValues = parts[0].split(",");
        int score = Integer.parseInt(parts[1]);
        int level = Integer.parseInt(parts[2]);

        // Update GameEngine state
        Platform.runLater(() -> {
            gameEngine.setBoard(boardValues);
            gameEngine.setScore(score);
            gameEngine.setLevel(level);
            render();
            updateLabels();
        });
    }

    private void updateLabels() {
        scoreLabel.setText("Score: " + gameEngine.getScore());
        levelLabel.setText("Level: " + gameEngine.getLevel());
        comboLabel.setText("Combo: " + gameEngine.getCombo());
        moveCountLabel.setText("# of Moves: " + gameEngine.getMoveCount());
    }

    public void displayGameOver(String scores) {
        Platform.runLater(() -> {
            // Display game over dialog with options
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Final Scores");
            alert.setContentText(scores + "\n\nChoose an option:");

            ButtonType waitButton = new ButtonType("Wait");
            ButtonType rankButton = new ButtonType("Rank");
            ButtonType cancelButton = new ButtonType("Cancel");

            alert.getButtonTypes().setAll(waitButton, rankButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == rankButton) {
                    try {
                        new ScoreboardWindow();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showError("Failed to open scoreboard.");
                    }
                } else if (result.get() == waitButton) {
                    // Do nothing, continue to wait
                } else {
                    // Handle cancel if necessary
                }
            }

            // Optionally, reset the game or close the application
            // resetGame();
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void resetGame() {
        gameEngine.reset(); // Implement a reset method in GameEngine
        updateLabels();
        render();
    }

    public void notifyPlayerJoined(String name) {
        Platform.runLater(() -> {
            // Add the new player to the otherPlayers map with default info
            if (!name.equals(gameEngine.getPlayerName())) { // Don't add self
                otherPlayers.put(name, new PlayerInfo(name, 0, 1, 0));
                updateOtherPlayersList();
                // Display a notification that a new player has joined
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Player Joined");
                alert.setHeaderText(null);
                alert.setContentText(name + " has joined the game.");
                alert.showAndWait();
            }
        });
    }

    public void notifyPlayerLeft(String name) {
        Platform.runLater(() -> {
            // Remove the player from the otherPlayers map
            otherPlayers.remove(name);
            updateOtherPlayersList();
            // Display a notification that a player has left
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Player Left");
            alert.setHeaderText(null);
            alert.setContentText(name + " has left the game.");
            alert.showAndWait();
        });
    }

    public void updateOtherPlayers(String playersInfo) {
        Platform.runLater(() -> {
            String[] players = playersInfo.split(";");
            otherPlayers.clear();
            for (String p : players) {
                String[] parts = p.split(",");
                if (parts.length < 4) continue;
                String name = parts[0];
                if (name.equals(gameEngine.getPlayerName())) continue; // Don't include self
                int score = Integer.parseInt(parts[1]);
                int level = Integer.parseInt(parts[2]);
                int combo = Integer.parseInt(parts[3]);
                otherPlayers.put(name, new PlayerInfo(name, score, level, combo));
            }
            updateOtherPlayersList();
        });
    }

    private void updateOtherPlayersList() {
        // Update the ListView with other players' info
        List<String> playersInfo = new ArrayList<>();
        for (PlayerInfo pi : otherPlayers.values()) {
            String info = String.format("Name: %s | Score: %d | Level: %d | Combo: %d",
                    pi.getName(), pi.getScore(), pi.getLevel(), pi.getCombo());
            playersInfo.add(info);
        }
        otherPlayersList.setItems(FXCollections.observableArrayList(playersInfo));
    }

    void onWidthChangedWindow(double w) {
        double width = w - boardPane.getBoundsInParent().getMinX();
        boardPane.setMinWidth(width);
        canvas.setWidth(width);
        render();
    }

    void onHeightChangedWindow(double h) {
        double height = h - boardPane.getBoundsInParent().getMinY() - menuBar.getHeight();
        boardPane.setMinHeight(height);
        canvas.setHeight(height);
        render();
    }

    void quit() {
        System.out.println("Bye bye");
        stage.close();
        Platform.exit();
    }

    public void setName(String name) {
        nameLabel.setText(name);
        gameEngine.setPlayerName(name);
    }

    // Inner class to store player info
    private static class PlayerInfo {
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

        public String getName() { return name; }
        public int getScore() { return score; }
        public int getLevel() { return level; }
        public int getCombo() { return combo; }

        public void setScore(int score) { this.score = score; }
        public void setLevel(int level) { this.level = level; }
        public void setCombo(int combo) { this.combo = combo; }
    }
}
