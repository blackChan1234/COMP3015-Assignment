
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class LobbyController {

    @FXML
    private ListView<String> playerListView;

    private Stage lobbyStage;
    @FXML
    private Button startGameButton;

    private GameEngine gameEngine;

    public void initialize() {
        gameEngine = GameEngine.getInstance();
        gameEngine.setLobbyController(this);

        // 如果当前玩家是第一位玩家，显示开始游戏按钮
        if (gameEngine.isFirstPlayer()) {
            startGameButton.setVisible(true);
        }

        startGameButton.setOnAction(event -> {
            promptStartGame();
        });
    }
    private void promptStartGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Start Game");
        alert.setHeaderText("Do you want to start the game now?");
        alert.setContentText("Click OK to start the game, or Cancel to wait for more players.");

        Optional<ButtonType> result = alert.showAndWait();
        try {
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Send start game command to the server
                gameEngine.sendStartGameCommand();
            } else {
                // Do nothing, continue waiting
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 更新玩家列表
    public void updatePlayerList(List<String> players) {
        Platform.runLater(() -> {
            playerListView.getItems().setAll(players);
        });
    }

    public void setLobbyStage(Stage lobbyStage) {
        this.lobbyStage = lobbyStage;
    }

    // Update hideLobby method
    public void hideLobby() {
        Platform.runLater(() -> {
            if (lobbyStage != null) {
                lobbyStage.close();
            }
        });
    }
    public void setFirstPlayer(boolean isFirstPlayer) {
        Platform.runLater(() -> {
            startGameButton.setVisible(isFirstPlayer);
        });
    }


}
