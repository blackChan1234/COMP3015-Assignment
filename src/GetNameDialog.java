import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class GetNameDialog {
    @FXML
    TextField nameField;

    @FXML
    Button goButton;
    @FXML
    TextField ipField;

    @FXML
    TextField portField;
    String serverIP;
    int serverPort;
    Stage stage;
    String playername;

    public GetNameDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("getNameUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        goButton.setOnMouseClicked(this::OnButtonClick);

        stage.showAndWait();
    }

    @FXML
    void OnButtonClick(Event event) {
        playername = nameField.getText().trim();
        serverIP = ipField.getText().trim();
        String portText = portField.getText().trim();
        if (playername.length() > 0 && serverIP.length() > 0 && portText.length() > 0) {
            try {
                serverPort = Integer.parseInt(portText);
                stage.close();
            } catch (NumberFormatException e) {

                System.out.println("Please input a valid port");
            }
        } else {

            System.out.println("Please input all fields");
        }
    }

    public String getPlayername() {
        return playername;
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }
}
