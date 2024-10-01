import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    Stage stage;
    AnimationTimer animationTimer;

    final String[] symbols = {"bg", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "Joker"};
    final Image[] images = new Image[symbols.length];
    final GameEngine gameEngine;
    final String imagePath = "images/";
    BattleJoker battleJoker; // Reference to the main client class

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
        // Update other labels if necessary
    }

    public void displayGameOver(String scores) {
        Platform.runLater(() -> {
            // Display game over dialog with scores
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Final Scores");
            alert.setContentText(scores);
            alert.showAndWait();

            // Optionally, reset the game or close the application
            // For example, you can reset the game state:
            resetGame();
        });
    }

    private void resetGame() {
        gameEngine.reset(); // Implement a reset method in GameEngine
        updateLabels();
        render();
    }

    public void notifyPlayerJoined(String name) {
        Platform.runLater(() -> {
            // Display a notification that a new player has joined
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Player Joined");
            alert.setHeaderText(null);
            alert.setContentText(name + " has joined the game.");
            alert.showAndWait();
        });
    }

    public void notifyPlayerLeft(String name) {
        Platform.runLater(() -> {
            // Display a notification that a player has left
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Player Left");
            alert.setHeaderText(null);
            alert.setContentText(name + " has left the game.");
            alert.showAndWait();
        });
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
}
