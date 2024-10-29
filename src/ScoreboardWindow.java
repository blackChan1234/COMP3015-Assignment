import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class ScoreboardWindow {
    Stage stage;
    @FXML
    private Button top10Button;

    @FXML
    private Button waitNewGameButton;
    @FXML
    ListView<String> scoreList;
    private ObservableList<String> gameOverItems;
    private ObservableList<String> top10Items;
    private String title;
    public ScoreboardWindow() throws IOException {
        this(null, "Score Board");
    }

    // New constructor to accept custom data and title
    public ScoreboardWindow(ObservableList<String> items, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("scoreUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        setFont(14);

        if (items != null) {
            scoreList.setItems(items);
        } else {
            updateList();
        }

        //stage.showAndWait();
    }
    public void show() {
        stage.show();
    }
    @FXML
    private void handleTop10Button() {
        // Request top 10 scores from the server
        try {
            GameEngine.getInstance().requestTopScores();
            // Disable the Top 10 button until the data is received
            top10Button.setDisable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleWaitNewGameButton() {
        // Logic to wait for a new game
        // This could be as simple as closing the scoreboard window
        // and resetting the game state in GameEngine
        stage.close();
        //GameEngine.getInstance().waitForNewGame();
    }
    private void setFont(int fontSize) {
        scoreList.setCellFactory(param -> {
            TextFieldListCell<String> cell = new TextFieldListCell<>();
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

    private void updateList() {
        try {
            ObservableList<String> items = FXCollections.observableArrayList();
            Database.getScores().forEach(data->{
                String scoreStr = String.format("%s (%s)", data.get("score"), data.get("level"));
                items.add(String.format("%10s | %10s | %s", data.get("name"), scoreStr, data.get("time").substring(0, 16)));
            });
            scoreList.setItems(items);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
