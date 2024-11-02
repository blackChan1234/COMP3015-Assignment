import javafx.application.Application;
import javafx.stage.Stage;

public class ScoreboardTester extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Database.connect();
        new ScoreboardWindow();
        Database.disconnect();
    }
}
