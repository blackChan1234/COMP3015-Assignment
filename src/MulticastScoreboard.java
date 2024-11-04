import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MulticastScoreboard extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("scoreboard.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Multicast Scoreboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        ScoreboardController controller = loader.getController();
        controller.startListening();

        // Ensure the socket is closed when the window is closed
        primaryStage.setOnCloseRequest(event -> controller.stopListening());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
