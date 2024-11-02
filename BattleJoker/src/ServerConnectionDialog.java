// ServerConnectionDialog.java
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ServerConnectionDialog {
    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Button connectButton;

    private String serverIP;
    private int serverPort;

    public ServerConnectionDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/serverConnectionUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Connect to Server");
        stage.setResizable(false);

        connectButton.setOnAction(e -> onConnect(stage));

        stage.showAndWait();
    }

    private void onConnect(Stage stage) {
        serverIP = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (serverIP.isEmpty() || portText.isEmpty()) {
            // Optionally, show an error message
            return;
        }

        try {
            serverPort = Integer.parseInt(portText);
            stage.close();
        } catch (NumberFormatException e) {
            // Optionally, show an error message for invalid port number
            System.err.println("Invalid port number.");
        }
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }
}
