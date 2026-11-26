package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Room;


import java.sql.Date;
import java.util.List;

public class MainController {

    public void handleKiosk(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Booking.fxml"));
            Parent root = loader.load();

            BookingController bookingController = loader.getController();


            Stage stage = new Stage();
            stage.setTitle("Kiosk Booking");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            LoggerServiceController.logException("Kiosk loading error", e);
        }
    }

    public void handleAdmin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminLogin.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Admin Login");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            LoggerServiceController.logException("Admin login error", e);
        }
    }
}
