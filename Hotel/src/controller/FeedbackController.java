package controller;

import dao.FeedbackDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class FeedbackController {

    @FXML private TextField ratingField;
    @FXML private TextArea commentsArea;
    @FXML
    private TextField reservationIdField;

    private int reservationId;
    private int guestId;


    public void setGuestId(int guestId) {
        this.guestId = guestId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
        if (reservationIdField != null) {
            reservationIdField.setText(String.valueOf(reservationId));
        }
    }

    @FXML
    private void handleSubmit() {
        try {

            FeedbackDAO dao = new FeedbackDAO();
            if (dao.existsForReservation(reservationId)) {
                show("Error", "You have already submitted feedback for this reservation.");
                return;
            }

            int rating = Integer.parseInt(ratingField.getText().trim());
            if (rating < 1 || rating > 5) {
                show("Validation", "Please enter rating between 1 and 5.");
                return;
            }
            String comments = commentsArea.getText();

            dao.insert(reservationId, guestId, rating, comments);
            LoggerServiceController.logAdminActivity("Feedback submitted for reservation " + reservationId);

            show("Thank you!", "Your feedback has been recorded.");
            ((Stage) ratingField.getScene().getWindow()).close();
        } catch (Exception e) {
            LoggerServiceController.logException("Feedback submit failed", e);
            show("Error", "Could not save feedback.");
        }
    }

    private void show(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
