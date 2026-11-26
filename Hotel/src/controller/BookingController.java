package controller;

import dao.GuestDAO;
import dao.ReservationDAO;
import db.DBUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Guest;
import model.Reservation;
import model.Room;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookingController {

    // Guest fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;

    // Booking fields
    @FXML private TextField guestCountField;
    @FXML private DatePicker checkInDatePicker;
    @FXML private DatePicker checkOutDatePicker;
    @FXML private ComboBox<String> roomComboBox;

    private List<List<Room>> roomSuggestions = new ArrayList<>();

    @FXML
    public void initialize() {
        DBUtil.init(); // make sure tables exist
        guestCountField.textProperty().addListener((obs, oldV, newV) -> handleShowRooms());
        handleShowRooms();
    }

    @FXML
    public void handleShowRooms() {
        roomComboBox.getItems().clear();
        roomSuggestions.clear();

        int guests;
        try {
            guests = Integer.parseInt(guestCountField.getText().trim());
            if (guests <= 0) return;
        } catch (Exception ignored) {
            return;
        }

        RoomAssignmentServiceController svc = new RoomAssignmentServiceController();
        roomSuggestions = svc.suggestRoomCombinations(guests);

        if (roomSuggestions == null || roomSuggestions.isEmpty()) {
            roomComboBox.getItems().add("No available room combinations.");
            return;
        }

        for (List<Room> combo : roomSuggestions) {
            String label = combo.stream().map(Room::toString).collect(Collectors.joining(", "));
            roomComboBox.getItems().add(label);
        }
        roomComboBox.getSelectionModel().selectFirst();
    }


    @FXML
    public void handleConfirmBooking() {
        try {
            // ---- 1) Validate guest info ----
            String firstName = safe(firstNameField);
            String lastName = safe(lastNameField);
            String phone = safe(phoneField);
            String email = safe(emailField);
            String address = safe(addressField);

            if (firstName.isEmpty() || phone.isEmpty()) {
                showInfo("Please enter first name and phone.");
                return;
            }

            // ---- 2) Validate date pickers ----
            LocalDate checkInLocal = checkInDatePicker.getValue();
            LocalDate checkOutLocal = checkOutDatePicker.getValue();
            if (checkInLocal == null || checkOutLocal == null) {
                showInfo("Please select both check-in and check-out dates.");
                return;
            }
            if (checkOutLocal.isBefore(checkInLocal)) {
                showInfo("Check-out date must be after check-in date.");
                return;
            }

            // ---- 3) Validate guest count ----
            int guests;
            try {
                guests = Integer.parseInt(safe(guestCountField));
                if (guests <= 0) {
                    showInfo("Guest count must be greater than 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                showInfo("Invalid guest count.");
                return;
            }

            // ---- 4) Upsert guest ----
            Guest guest = new Guest();
            guest.setFirstName(firstName);
            guest.setLastName(lastName);
            guest.setPhoneNumber(phone);
            guest.setEmail(email);
            guest.setAddress(address);

            GuestDAO guestDAO = new GuestDAO();
            int guestId = guestDAO.upsertAndGetId(guest);

            // ---- 5) Validate chosen combo ----
            int idx = roomComboBox.getSelectionModel().getSelectedIndex();
            if (roomSuggestions == null || roomSuggestions.isEmpty() || idx < 0) {
                showInfo("Please select a room (combination).");
                return;
            }
            List<Room> chosenRooms = roomSuggestions.get(idx);

            // ---- 6) Build reservation ----
            Reservation r = new Reservation();
            r.setGuestId(guestId);
            r.setCheckInDate(Date.valueOf(checkInLocal));
            r.setCheckOutDate(Date.valueOf(checkOutLocal));
            r.setNumberOfGuests(guests);
            r.setStatus("CONFIRMED");

            // ---- 7) Save reservation ----
            ReservationDAO resDAO = new ReservationDAO();
            int resId = resDAO.insertReservation(r);
            resDAO.addReservationRooms(resId, chosenRooms);

            showInfo("✅ Booking confirmed! Reservation ID: " + resId);
            ((Stage) guestCountField.getScene().getWindow()).close();

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("❌ Failed to save reservation. Please check the inputs.");
        }
    }

    private String safe(TextField tf) {
        return tf == null || tf.getText() == null ? "" : tf.getText().trim();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
