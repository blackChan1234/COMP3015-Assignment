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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;


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
    VBox player1Box;
    @FXML
    Label player1NameLabel;
    @FXML
    Label player1ScoreLabel;
    @FXML
    Label player1LevelLabel;

    @FXML
    Label player1ComboLabel;
    @FXML
    Label player1MoveCountLabel;
    @FXML
    MenuItem mnuTopScores;
    @FXML
    VBox player2Box;
    @FXML
    Label player2NameLabel;
    @FXML
    Label player2ScoreLabel;
    @FXML
    Label player2LevelLabel;
    @FXML
    Label player2ComboLabel;
    @FXML
    Label player2MoveCountLabel;

    @FXML
    VBox player3Box;
    @FXML
    Label player3NameLabel;
    @FXML
    Label player3ScoreLabel;
    @FXML
    Label player3LevelLabel;
    @FXML
    Label player3ComboLabel;
    @FXML
    Label player3MoveCountLabel;
    @FXML
    Label moveCountLabel;
    @FXML
    MenuItem mnuSavePuzzle;
    @FXML
    MenuItem mnuUploadPuzzle;
    @FXML
    Pane boardPane;


    @FXML
    Canvas canvas;

    @FXML
    Label turnIndicatorLabel; // 新增的回合指示器

    Stage stage;
    AnimationTimer animationTimer;

    final String imagePath = "images/";
    static final String[] symbols = {"bg", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "Joker"};
    static final Image[] images = new Image[symbols.length];
    static final GameEngine gameEngine = GameEngine.getInstance();

    public GameWindow(Stage stage) throws IOException {
        loadImages();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        GameEngine.getInstance().setGameWindow(this);
        this.stage = stage;

        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        stage.widthProperty().addListener(w -> onWidthChangedWindow(((ReadOnlyDoubleProperty) w).getValue()));
        stage.heightProperty().addListener(h -> onHeightChangedWindow(((ReadOnlyDoubleProperty) h).getValue()));
        stage.setOnCloseRequest(event -> quit());
        initializeMenuHandlers();
        stage.show();
        initCanvas();
    }
    private void initializeMenuHandlers() {
        mnuSavePuzzle.setOnAction(event -> savePuzzle());
        mnuUploadPuzzle.setOnAction(event -> uploadPuzzle());
    }
    private void savePuzzle() {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Puzzle");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Puzzle Files", "*.puzzle"));
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try {
                    // Get the current puzzle data from the GameEngine
                    int[] boardData = gameEngine.getBoardData();

                    // Save the board data to the file in binary format
                    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
                        out.writeInt(boardData.length);
                        for (int value : boardData) {
                            out.writeInt(value);
                        }
                    }

                    showMessage("Puzzle saved successfully.");

                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage("Failed to save the puzzle.");
                }
            }
        });
    }


    private void uploadPuzzle() {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Upload Puzzle");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Puzzle Files", "*.puzzle"));
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    // Read the puzzle data from the file
                    int[] boardData;
                    try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                        int length = in.readInt();
                        boardData = new int[length];
                        System.out.println(boardData.length);
                        for (int i = 0; i < length; i++) {
                            boardData[i] = in.readInt();
                        }
                    }
                    System.out.println(boardData);

                    // Send the puzzle data to the server
                    gameEngine.sendPuzzleDataToServer(boardData);

                    showMessage("Puzzle uploaded successfully.");

                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage("Failed to upload the puzzle.");
                }
            }
        });
    }


    public void showGameStart() {
        Platform.runLater(() -> {
            gameStart();
            // 可以在这里显示游戏开始的提示
        });
    }

    private void gameStart() {
        animationTimer.start();
    }

    public void updateOtherPlayersData(List<PlayerInfo> players) {
        Platform.runLater(() -> {
            String localPlayerName = nameLabel.getText();

            // 隐藏其他玩家的 VBox
            player1Box.setVisible(false);
            player2Box.setVisible(false);
            player3Box.setVisible(false);

            int index = 0;
            for (PlayerInfo player : players) {
                if (player.getName().equals(localPlayerName)) {
                    // 更新本地玩家的信息
                    scoreLabel.setText("Score: " + player.getScore());
                    levelLabel.setText("Level: " + player.getLevel());
                    comboLabel.setText("Combo: " + player.getCombo());
                    moveCountLabel.setText("# of Moves: " + player.getMoves());
                } else {
                    switch (index) {
                        case 0:
                            updatePlayerBox(player1Box, player1NameLabel, player1ScoreLabel, player1LevelLabel,
                                    player1ComboLabel, player1MoveCountLabel, player);
                            break;
                        case 1:
                            updatePlayerBox(player2Box, player2NameLabel, player2ScoreLabel, player2LevelLabel,
                                    player2ComboLabel, player2MoveCountLabel, player);
                            break;
                        case 2:
                            updatePlayerBox(player3Box, player3NameLabel, player3ScoreLabel, player3LevelLabel,
                                    player3ComboLabel, player3MoveCountLabel, player);
                            break;
                        default:
                            // 处理更多玩家
                            break;
                    }
                    index++;
                }
            }
        });
    }

    private void updatePlayerBox(VBox playerBox, Label nameLabel, Label scoreLabel, Label levelLabel,
                                 Label comboLabel, Label moveCountLabel, PlayerInfo player) {
        playerBox.setVisible(true);
        nameLabel.setText(player.getName());
        scoreLabel.setText("Score: " + player.getScore());
        levelLabel.setText("Level: " + player.getLevel());
        comboLabel.setText("Combo: " + player.getCombo());
        moveCountLabel.setText("# of Moves: " + player.getMoves());
    }

    private void loadImages() throws IOException {
        for (int i = 0; i < symbols.length; i++)
            images[i] = new Image(Files.newInputStream(Paths.get(imagePath + symbols[i] + ".png")));
    }

    private void initCanvas() {
        canvas.setOnKeyPressed(event -> {
            try {
                gameEngine.moveMerge(event.getCode().toString());
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        });

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
            }
        };
        canvas.setFocusTraversable(true);
        canvas.setDisable(true); // 初始时禁用输入
        canvas.requestFocus();
    }

    public void displayGameOverScores(List<PlayerInfo> scores) {
        Platform.runLater(() -> {
            try {
                // Convert PlayerInfo list to a suitable format for ScoreboardWindow
                ObservableList<String> items = FXCollections.observableArrayList();
                for (PlayerInfo player : scores) {
                    String scoreStr = String.format("%-10s | Score: %-5d | Level: %-3d",
                            player.getName(), player.getScore(), player.getLevel());
                    items.add(scoreStr);
                }

                // Create a new ScoreboardWindow instance with custom data
                new ScoreboardWindow(items, "Game Over Scores").show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void displayTopScores(List<HashMap<String, String>> topScores) {
        Platform.runLater(() -> {
            try {
                // Convert topScores list to a suitable format for ScoreboardWindow
                ObservableList<String> items = FXCollections.observableArrayList();
                for (HashMap<String, String> data : topScores) {
                    String scoreStr = String.format("%s (%s)", data.get("score"), data.get("level"));
                    items.add(String.format("%10s | %10s | %s", data.get("name"), scoreStr, data.get("time").substring(0, 16)));
                }

                // Create a new ScoreboardWindow instance with custom data
                new ScoreboardWindow(items, "Top 10 Scores").show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
        System.exit(0);
    }

    public void setName(String name) {
        nameLabel.setText(name);
    }

    public void updateCurrentPlayer(String currentPlayerName) {
        Platform.runLater(() -> {
            if (currentPlayerName.equals(nameLabel.getText())) {
                turnIndicatorLabel.setText("Your Turn");
                turnIndicatorLabel.setTextFill(Color.RED);
                canvas.setDisable(false);
                canvas.requestFocus();
            } else {
                turnIndicatorLabel.setText("Waiting for " + currentPlayerName);
                turnIndicatorLabel.setTextFill(Color.BLACK);
                canvas.setDisable(true);
            }
        });
    }

    public void showMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Message");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showLobby() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
                Parent root = loader.load();

                LobbyController lobbyController = loader.getController();

                Stage lobbyStage = new Stage();
                lobbyStage.setTitle("Game Lobby");
                lobbyStage.setScene(new Scene(root));

                // 设置 lobbyStage
                lobbyController.setLobbyStage(lobbyStage);

                // 设置 LobbyController 到 GameEngine
                GameEngine.getInstance().setLobbyController(lobbyController);

                lobbyStage.show();

                // 通知 GameEngine，lobby 已经准备好，可以启动 receiverThread
                GameEngine.getInstance().startReceiverThread();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }




}
