package controller;

import dao.ReservationDAO;
import dao.RoomDAO;
import db.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Room;
import model.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeParseException;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class ModifyReservationController {

    @FXML private TextField reservationIdField;
    @FXML private TextField guestIdField;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private TextField guestCountField;
    @FXML private TextArea  rulesArea;
    @FXML private ListView<Room> assignedRoomsList;
    @FXML private ListView<Room> availableRoomsList;
    @FXML private TextField statusField;

    private final ObservableList<Room> assigned = FXCollections.observableArrayList();
    private final ObservableList<Room> available = FXCollections.observableArrayList();

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    private int reservationId;
    private int guestId;

    @FXML
    public void initialize() {
        try {
            assignedRoomsList.setCellFactory(_ -> new RoomCell());
            availableRoomsList.setCellFactory(_ -> new RoomCell());
        } catch (Exception ex) {
            System.err.println("Modify init failed: " + ex.getMessage());
        }
    }

    public void loadReservation(int resId) {
        try {
            this.reservationId = resId;

            var r = reservationDAO.findById(resId);
            if (r == null) throw new IllegalStateException("Reservation not found: " + resId);

            guestId = r.getGuestId();
            reservationIdField.setText(String.valueOf(resId));
            guestIdField.setText(String.valueOf(guestId));

            if (r.getCheckInDate() != null) {
                checkInPicker.setValue(r.getCheckInDate().toLocalDate());
            }
            if (r.getCheckOutDate() != null) {
                checkOutPicker.setValue(r.getCheckOutDate().toLocalDate());
            }

            guestCountField.setText(String.valueOf(r.getNumberOfGuests()));
            statusField.setText(r.getStatus());

            rulesArea.setText("""
                    Rules:
                    - SINGLE, DELUXE, PENTHOUSE: up to 2 people each
                    - DOUBLE: up to 4 people
                    - For 3~4 guests: use DOUBLE or (SINGLE+SINGLE)
                    - For >4 guests: multiple DOUBLEs or DOUBLE + SINGLEs
                    """
            );

            assigned.setAll(roomDAO.findRoomsByReservation(resId));
            assignedRoomsList.setItems(assigned);
            handleRefreshAvailable(null);

        } catch (Exception e) {
            error("Failed to load reservation.\n" + e.getMessage());
        }
    }

    @FXML
    public void handleRemoveSelected(ActionEvent event) {
        Room sel = assignedRoomsList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        assigned.remove(sel);
        if (!available.contains(sel)) {
            available.add(sel);
            available.sort(this::byId);
        }
    }

    @FXML
    public void handleAddSelected(ActionEvent event) {
        Room sel = availableRoomsList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        available.remove(sel);
        if (!assigned.contains(sel)) {
            assigned.add(sel);
            assigned.sort(this::byId);
        }
    }

    @FXML
    public void handleRefreshAvailable(ActionEvent event) {
        try {
            int guests = parseIntSafe(guestCountField.getText().trim(), 1);
            if (guests <= 0) guests = 1;

            var assignedIds = assigned.stream()
                    .map(Room::getRoomId)
                    .collect(Collectors.toSet());

            List<Room> allAvail;
            try {
                allAvail = roomDAO.findAvailable();
            } catch (Exception e) {
                allAvail = findRoomsExcluding(new ArrayList<>(assignedIds));
            }

            var allowedTypes = allowedTypesForGuests(guests);
            List<Room> filtered = new ArrayList<>();
            for (Room r : allAvail) {
                if (assignedIds.contains(r.getRoomId())) continue;
                if (allowedTypes.contains(r.getRoomType().name())) {
                    filtered.add(r);
                }
            }

            available.setAll(filtered);
            availableRoomsList.setItems(available);
        } catch (Exception e) {
            error("Failed to refresh available rooms.\n" + e.getMessage());
        }
    }

    @FXML
    public void handleSave(ActionEvent event) {
        try {
            LocalDate in  = checkInPicker.getValue();
            LocalDate out = checkOutPicker.getValue();
            final int guests = parseIntSafe(guestCountField.getText().trim(), 1);

            if (in == null || out == null) {
                warn("Please select both check-in and check-out dates.");
                return;
            }

            if (out.isBefore(in)) {
                warn("Check-out date must be after check-in date.");
                return;
            }
            if (guests <= 0) {
                warn("Number of guests must be at least 1.");
                return;
            }
            if (!capacityOk(new ArrayList<>(assigned), guests)) {
                warn("Selected rooms cannot accommodate " + guests + " guests.\n" +
                        "Please add more capacity.");
                return;
            }

            reservationDAO.updateDatesAndGuests(
                    reservationId,
                    java.sql.Date.valueOf(in),
                    java.sql.Date.valueOf(out),
                    guests
            );

            List<Integer> roomIds = assigned.stream()
                    .map(Room::getRoomId)
                    .toList();
            reservationDAO.replaceReservationRooms(reservationId, roomIds);

            info("Saved", "Reservation updated successfully.");
            closeWindow();
        } catch (DateTimeParseException dtpe) {
            warn("Please enter dates in yyyy-MM-dd format.");
        } catch (Exception e) {
            error("Failed to save.\n" + e.getMessage());
        }
    }

    @FXML
    public void handleCancelReservation(ActionEvent event) {
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM feedback WHERE reservation_id = ?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }
            boolean deleted = reservationDAO.hardDelete(reservationId);
            if (deleted) {
                info("Deleted", "Reservation " + reservationId + " has been removed.");
                closeWindow();
            } else {
                warn("Reservation not found or could not be deleted.");
            }
        } catch (Exception e) {
            error("Failed to delete reservation.\n" + e.getMessage());
        }
    }

    private boolean capacityOk(List<Room> rooms, int guests) {
        int capacity = 0;
        for (Room r : rooms) {
            capacity += (r.getRoomType() == RoomType.DOUBLE) ? 4 : 2;
        }
        return capacity >= guests;
    }

    private Set<String> allowedTypesForGuests(int guests) {
        var set = new HashSet<String>();
        if (guests <= 2) {
            set.addAll(Arrays.asList("SINGLE", "DELUXE", "PENTHOUSE", "PENT"));
        } else {
            set.addAll(Arrays.asList("DOUBLE", "SINGLE"));
        }
        return set;
    }

    private int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private List<Room> findRoomsExcluding(List<Integer> excludeIds) throws Exception {
        String sql = "SELECT id, type, beds, price, status FROM rooms "
                + (excludeIds.isEmpty() ? "" :
                "WHERE id NOT IN (" + excludeIds.stream().map(_i -> "?").collect(Collectors.joining(",")) + ")");
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : excludeIds) ps.setInt(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Room> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Room(
                            rs.getInt("id"),
                            RoomType.valueOf(rs.getString("type")),
                            rs.getInt("beds"),
                            rs.getFloat("price"),
                            rs.getString("status")
                    ));
                }
                return list;
            }
        }
    }

    private int byId(Room a, Room b) { return Integer.compare(a.getRoomId(), b.getRoomId()); }

    private void closeWindow() {
        Stage st = (Stage) assignedRoomsList.getScene().getWindow();
        st.close();
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }
    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText("Warning");
        a.setContentText(msg);
        a.showAndWait();
    }
    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.setContentText(msg);
        a.showAndWait();
    }

    private static class RoomCell extends ListCell<Room> {
        @Override protected void updateItem(Room r, boolean empty) {
            super.updateItem(r, empty);
            if (empty || r == null) {
                setText(null);
            } else {
                setText(String.format("Room %d - %s - $%.2f",
                        r.getRoomId(),
                        r.getRoomType() != null ? r.getRoomType().name() : "UNKNOWN",
                        r.getPrice()));
            }
        }
    }
}
