/* Jungkyu Mok */


import db.DBUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Main.fxml"));
            Scene scene = new Scene(loader.load());
            primaryStage.setTitle("🏨 Hotel Reservation System");
            primaryStage.setScene(scene);
            primaryStage.show();
            DBUtil.init();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
