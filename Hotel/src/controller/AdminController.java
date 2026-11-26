package controller;

import db.DBUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Admin;
import model.Guest;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.Alert;

public class AdminController {

    private final List<Admin> admins = new ArrayList<>();
    private Admin currentAdmin;

    public AdminController() {
        ensureMinimumAdmins();
        loadAdminsFromDB();
    }

    private void ensureMinimumAdmins() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS admins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL)";
        String countSQL = "SELECT COUNT(*) FROM admins";
        String insertSQL = "INSERT INTO admins (username, password) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);

            int count = 0;
            try (ResultSet rs = stmt.executeQuery(countSQL)) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }

            if (count < 2) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    if (count == 0) {
                        pstmt.setString(1, "admin1");
                        pstmt.setString(2, "pass123");
                        pstmt.executeUpdate();

                        pstmt.setString(1, "admin2");
                        pstmt.setString(2, "pass456");
                        pstmt.executeUpdate();
                    } else if (count == 1) {
                        pstmt.setString(1, "admin2");
                        pstmt.setString(2, "pass456");
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            LoggerServiceController.logException("Failed to ensure minimum admins", e);
        }
    }

    private void loadAdminsFromDB() {
        admins.clear();
        String sql = "SELECT id, username, password FROM admins";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                admins.add(new Admin(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                ));
            }
        } catch (SQLException e) {
            LoggerServiceController.logException("Failed to load admins from DB", e);
        }
    }

    public boolean login(String username, String password) {
        String u = safe(username);
        String p = safe(password);

        for (Admin a : admins) {
            if (a.getUsername().equals(u) && a.getPassword().equals(p)) {
                currentAdmin = a;
                LoggerServiceController.logAdminActivity("Admin login success: " + u);
                return true;
            }
        }
        LoggerServiceController.logAdminActivity("Admin login fail: " + u);
        return false;
    }

    public void logout() {
        if (currentAdmin != null) {
            LoggerServiceController.logAdminActivity("Admin logout: " + currentAdmin.getUsername());
        }
        currentAdmin = null;
    }

    public Admin getCurrentAdmin() {
        return currentAdmin;
    }

    public List<Admin> getAdmins() {
        return Collections.unmodifiableList(admins);
    }

    public Guest searchGuestByPhone(String phoneNumber) {
        String phone = safe(phoneNumber);
        LoggerServiceController.logAdminActivity("Search guest by phone: " + phone);

        String sql = "SELECT id, first_name, last_name, phone, email, address FROM guests WHERE phone = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Guest(
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("address")
                    );
                }
            }
        } catch (SQLException e) {
            LoggerServiceController.logException("Search guest by phone failed", e);
        }
        return null;
    }

    public boolean checkOutGuest(int reservationId) {
        LoggerServiceController.logAdminActivity("Checked out reservation: " + reservationId);

        String sql = "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerServiceController.logException("Checkout failed", e);
            return false;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Checkout Completed");
        alert.setHeaderText(null);
        alert.setContentText("Reservation " + reservationId + " has been checked out.\n"
                + "Please invite the guest to leave feedback.");
        alert.showAndWait();

        openFeedbackWindow(reservationId);

        return true;
    }

    private void openFeedbackWindow(int reservationId) {
        try {
            int guestId = -1;
            String sql = "SELECT guest_id FROM reservations WHERE id = ?";
            try (Connection c = DBUtil.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        guestId = rs.getInt("guest_id");
                    }
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FeedbackForm.fxml"));
            Parent root = loader.load();

            FeedbackController controller = loader.getController();
            controller.setReservationId(reservationId);
            controller.setGuestId(guestId);

            Stage stage = new Stage();
            stage.setTitle("Feedback");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException | SQLException e) {
            LoggerServiceController.logException("Failed to open feedback form", e);
        }
    }


    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
