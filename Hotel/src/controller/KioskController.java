package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Room;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class KioskController {

    @FXML private TextField guestCountField;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private ListView<String> roomListView;

    private List<Room> availableRooms;

    @FXML
    public void initialize() {
        // Set sensible defaults so buttons work immediately
        LocalDate today = LocalDate.now();
        checkInPicker.setValue(today);
        checkOutPicker.setValue(today.plusDays(1));

        guestCountField.textProperty().addListener((obs, o, n) -> safeShowRooms());
        checkInPicker.valueProperty().addListener((obs, o, n) -> safeShowRooms());
        checkOutPicker.valueProperty().addListener((obs, o, n) -> safeShowRooms());
    }

    private void safeShowRooms() {
        try { handleShowRooms(); } catch (Exception ignored) {}
    }

    @FXML
    void handleShowRooms() {
        try {
            int guests = Integer.parseInt(guestCountField.getText().trim());
            if (guests <= 0) throw new NumberFormatException();

            LocalDate in  = checkInPicker.getValue();
            LocalDate out = checkOutPicker.getValue();
            if (in == null || out == null || !out.isAfter(in)) {
                showAlert("Dates", "Please select valid check-in/check-out dates (check-out after check-in).");
                return;
            }

            RoomAssignmentServiceController roomService = new RoomAssignmentServiceController();
            // Filtering is by guest count per your rules; dates are passed to booking stage.
            availableRooms = roomService.getAvailableRooms(guests);

            if (availableRooms == null || availableRooms.isEmpty()) {
                roomListView.setItems(FXCollections.observableArrayList());
                showAlert("No Rooms", "No rooms available for the given guest count.");
                return;
            }

            roomListView.setItems(FXCollections.observableArrayList(
                    availableRooms.stream()
                            .map(r -> "Room " + r.getRoomId() + " - " + r.getRoomType() + " - $" + r.getPrice())
                            .toList()
            ));
        } catch (NumberFormatException nfe) {
            showAlert("Input Error", "Please enter a valid number of guests.");
        } catch (Exception e) {
            LoggerServiceController.logException("Invalid input in kiosk", e);
            showAlert("Error", "Please check your inputs.");
        }
    }

    @FXML
    void handleBookRoom() {
        int selectedIndex = roomListView.getSelectionModel().getSelectedIndex();
        if (availableRooms == null || selectedIndex < 0 || selectedIndex >= availableRooms.size()) {
            showAlert("Warning", "Please select a room to book.");
            return;
        }

        Room selectedRoom = availableRooms.get(selectedIndex);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/GuestForm.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));

            // Pass selected room and dates to guest form
            GuestFormController guestController = loader.getController();
            guestController.setBookingInfo(
                    java.util.List.of(selectedRoom),
                    Date.valueOf(checkInPicker.getValue()),
                    Date.valueOf(checkOutPicker.getValue())
            );

            stage.setTitle("Enter Guest Details");
            stage.show();
        } catch (Exception e) {
            LoggerServiceController.logException("Failed to open guest form", e);
            showAlert("Error", "Could not proceed to guest details.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
