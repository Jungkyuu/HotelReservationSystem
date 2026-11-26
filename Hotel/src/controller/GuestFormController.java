package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Guest;
import model.Reservation;
import model.Room;
import java.sql.Date;
import java.util.List;

public class GuestFormController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private ComboBox<String> roomComboBox;

    private List<Room> selectedRooms;
    private Date checkIn;
    private Date checkOut;

    public void setBookingInfo(List<Room> rooms, Date checkIn, Date checkOut) {
        this.selectedRooms = rooms;
        this.checkIn = checkIn;
        this.checkOut = checkOut;

        roomComboBox.getItems().clear();
        if (rooms != null) {
            for (Room r : rooms) {
                roomComboBox.getItems().add(r.toString());
            }
            if (!rooms.isEmpty()) {
                roomComboBox.getSelectionModel().select(0);
            }
        }
    }

    @FXML
    public void handleConfirmBooking() {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
                showAlert("Validation Error", "First name, Last name, and Phone are required.");
                return;
            }

            Guest guest = new Guest(0, firstName, lastName, phone, email, address);
            ReservationController reservationController = new ReservationController();

            StringBuilder confirmationMsg = new StringBuilder();
            for (Room r : selectedRooms) {
                Reservation reservation = reservationController.bookRoom(
                        r, guest, checkIn, checkOut, r.getNumberOfBeds()
                );
                confirmationMsg.append("Room: ")
                        .append(r.getRoomId())
                        .append(" → Reservation ID: ")
                        .append(reservation.getReservationId())
                        .append("\n");
            }

            showAlert("Booking confirmed!", confirmationMsg.toString());
            ((Stage) firstNameField.getScene().getWindow()).close();

        } catch (Exception e) {
            LoggerServiceController.logException("Booking error", e);
            showAlert("Error", "❌ Error occurred. Please check your input.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
