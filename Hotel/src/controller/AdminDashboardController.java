package controller;

import dao.GuestDAO;
import dao.ReservationDAO;
import dao.RoomDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Billing;
import model.Guest;
import model.Reservation;
import model.Room;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label statusLabel;

    private AdminController adminController;

    // DAOs
    private final GuestDAO guestDAO = new GuestDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    // Controllers
    private final BillingController billingController = new BillingController();

    public void init(AdminController adminController) {
        this.adminController = adminController;
        status("Logged in as: " + adminController.getCurrentAdmin().getUsername());
    }

    // -------------------- Search --------------------

    @FXML
    private void handleSearchGuest() {
        String phone = promptText("Search Guest", "Enter phone number:", "e.g. 437-438-1291");
        if (phone == null) return;

        try {
            Guest g = guestDAO.findByPhone(phone.trim());
            if (g == null) {
                warn("No guest found for: " + phone);
                status("No match");
                return;
            }

            List<Reservation> reservations = reservationDAO.listByGuestId(g.getGuestId());
            if (reservations.isEmpty()) {
                info("Guest Found",
                        g.getFirstName() + " " + g.getLastName() +
                                "\nPhone: " + g.getPhoneNumber() +
                                "\nEmail: " + nvl(g.getEmail()) +
                                "\nAddress: " + nvl(g.getAddress()) +
                                "\n\nNo reservations found.");
                status("Search completed");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(g.getFirstName()).append(" ").append(g.getLastName()).append('\n')
                    .append("Phone: ").append(g.getPhoneNumber()).append('\n')
                    .append("Email: ").append(nvl(g.getEmail())).append('\n')
                    .append("Address: ").append(nvl(g.getAddress())).append("\n\n");

            for (Reservation r : reservations) {
                LocalDate in  = r.getCheckInDate().toLocalDate();
                LocalDate out = r.getCheckOutDate().toLocalDate();
                long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));

                List<Room> rooms = roomDAO.findRoomsByReservation(r.getReservationId());
                String roomsLine = rooms.isEmpty()
                        ? "(no rooms linked)"
                        : rooms.stream()
                        .map(room -> "Room " + room.getRoomId() + " " + room.getRoomType()
                                + " ($" + room.getPrice() + ")")
                        .reduce((a,b) -> a + ", " + b).orElse("");

                sb.append("Reservation #").append(r.getReservationId())
                        .append("  [").append(r.getStatus()).append("]").append('\n')
                        .append(" • Stay: ").append(in).append(" → ").append(out)
                        .append("  (").append(nights).append(nights==1 ? " night" : " nights").append(')').append('\n')
                        .append(" • Guests: ").append(r.getNumberOfGuests()).append('\n')
                        .append(" • Rooms: ").append(roomsLine).append("\n\n");
            }

            info("Guest & Reservations", sb.toString());
            status("Search completed");
        } catch (Exception e) {
            LoggerServiceController.logException("Search guest error", e);
            error("Failed to search guest.\n" + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    // -------------------- Generate Bill --------------------

    @FXML
    private void handleGenerateBill() {
        Integer resId = promptReservationId("Generate Bill", "Enter Reservation ID:");
        if (resId == null) return;

        try {
            if (!reservationDAO.exists(resId)) {
                warn("Reservation " + resId + " not found.");
                return;
            }

            // Pull reservation & rooms from DB
            Reservation r = reservationDAO.findById(resId);
            List<Room> rooms = roomDAO.findRoomsByReservation(resId);

            LocalDate in = r.getCheckInDate().toLocalDate();
            LocalDate out = r.getCheckOutDate().toLocalDate();
            long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));

            // Ask discount
            Float discount = promptFloat("Discount", "Enter discount amount (e.g. 25):", "0");
            if (discount == null) return;

            // Build bill from DB (auto subtotal calculation)
            final float TAX_RATE = 0.13f;
            Billing bill = billingController.generateBillFromDB(resId, TAX_RATE, discount);

            // Build breakdown text
            StringBuilder nightly = new StringBuilder("Nightly breakdown:\n");
            float subtotal = 0f;
            for (Room rm : rooms) {
                float line = rm.getPrice() * (float) nights;
                nightly.append(String.format(" - Room %d (%s)  $%.2f × %d = $%.2f%n",
                        rm.getRoomId(), rm.getRoomType(), rm.getPrice(), nights, line));
                subtotal += line;
            }

            info("Bill Generated",
                    "Reservation " + resId + "\n" +
                            "Stay: " + in + " → " + out + " (" + nights + (nights == 1 ? " night" : " nights") + ")\n\n" +
                            nightly + "\n" +
                            String.format("Subtotal: $%.2f%nTax: $%.2f%nDiscount: $%.2f%nTOTAL:  $%.2f",
                                    bill.getAmount(), bill.getTax(), bill.getDiscount(), bill.getTotalAmount())
            );
            status("Bill generated");
        } catch (Exception e) {
            LoggerServiceController.logException("Generate bill error", e);
            error("Failed to generate bill.\n" + e.getMessage());
        }
    }

    // -------------------- Modify / Cancel --------------------

    @FXML
    private void handleModifyCancel() {
        Integer resId = promptReservationId("Modify / Cancel", "Enter Reservation ID:");
        if (resId == null) return;

        try {
            if (!reservationDAO.exists(resId)) {
                warn("Reservation " + resId + " not found.");
                return;
            }

            ChoiceDialog<String> dlg = new ChoiceDialog<>("Cancel (delete all)",
                    "Cancel (delete all)", "Modify");
            dlg.setTitle("Modify / Cancel");
            dlg.setHeaderText("Reservation " + resId);
            dlg.setContentText("Action:");

            dlg.showAndWait().ifPresent(choice -> {
                try {
                    if (choice.startsWith("Cancel")) {
                        boolean ok = reservationDAO.hardDelete(resId);
                        info("Success", ok ? "Reservation deleted." : "No change.");
                        status("Reservation deleted");
                    } else {
                        openModifyFlow(resId);
                    }
                } catch (Exception ex) {
                    LoggerServiceController.logException("Modify/Cancel error", ex);
                    error("Failed: " + ex.getMessage());
                }
            });
        } catch (Exception e) {
            LoggerServiceController.logException("Modify/Cancel error", e);
            error("Failed to modify/cancel.\n" + e.getMessage());
        }
    }

    private void openModifyFlow(int resId) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/ModifyReservation.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            ModifyReservationController ctrl = loader.getController();
            ctrl.loadReservation(resId); // <-- 전달

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modify Reservation " + resId);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            LoggerServiceController.logException("openModifyFlow failed", e);
            error("Failed to open modify window.\n" + e.getMessage());
        }
    }

    // -------------------- Checkout --------------------

    @FXML
    private void handleCheckout() {
        Integer resId = promptReservationId("Check-out Guest", "Enter Reservation ID:");
        if (resId == null) return;

        try {
            if (!reservationDAO.exists(resId)) {
                warn("Reservation " + resId + " not found.");
                return;
            }
            reservationDAO.checkoutReservation(resId);
            adminController.checkOutGuest(resId);
            info("Checkout Completed",
                    "Reservation " + resId + "\nThank you for your feedback!!");
            status("Checkout completed");
        } catch (Exception e) {
            LoggerServiceController.logException("Checkout error", e);
            error("Failed to checkout.\n" + e.getMessage());
        }
    }

    // -------------------- Logout --------------------

    @FXML
    private void handleLogout() {
        adminController.logout();
        info("Logout", "Logged out.");
        statusLabel.getScene().getWindow().hide();
    }

    // -------------------- Dialog helpers --------------------

    private String promptText(String title, String header, String placeholder) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getEditor().setPromptText(placeholder);
        return dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
    }

    private Integer promptReservationId(String title, String header) {
        String s = promptText(title, header, "e.g. 101");
        if (s == null) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException nfe) { warn("Please enter a valid number."); return null; }
    }

    private Float promptFloat(String title, String header, String placeholder) {
        String s = promptText(title, header, placeholder);
        if (s == null) return null;
        try { return Float.parseFloat(s); }
        catch (NumberFormatException nfe) { warn("Please enter a valid number."); return null; }
    }

    // -------------------- UI helpers --------------------

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }
    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.setContentText(msg);
        a.showAndWait();
    }
    private void status(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
}
