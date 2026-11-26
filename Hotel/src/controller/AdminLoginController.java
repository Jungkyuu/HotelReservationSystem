package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Admin;

public class AdminLoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final AdminController adminController = new AdminController();

    @FXML
    private void handleLogin() {
        String user = safe(usernameField.getText());
        String pass = safe(passwordField.getText());

        if (adminController.login(user, pass)) {
            openDashboardAndCloseSelf();
        } else {
            showError("Login failed. Please check your username/password.");
        }
    }

    private void openDashboardAndCloseSelf() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminDashboard.fxml"));
            Parent root = loader.load();

            // wire controller
            AdminDashboardController dash = loader.getController();
            dash.init(adminController); // pass the same AdminController instance

            Stage stage = new Stage();
            stage.setTitle("Admin Dashboard");
            stage.setScene(new Scene(root));
            stage.show();

            // close login window
            Stage me = (Stage) usernameField.getScene().getWindow();
            me.close();
        } catch (Exception e) {
            e.printStackTrace(); // show exact reason in console
            LoggerServiceController.logException("Failed to open dashboard", e);
            showError("Failed to open dashboard: " + e.getMessage());
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
