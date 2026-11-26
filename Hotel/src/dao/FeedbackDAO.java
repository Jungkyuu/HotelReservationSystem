package dao;

import db.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FeedbackDAO {
    public void insert(int reservationId, int guestId, int rating, String comments) throws SQLException {
        String sql = "INSERT INTO feedbacks (reservation_id, guest_id, rating, comments) VALUES (?, ?, ?, ?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, guestId);
            ps.setInt(3, rating);
            ps.setString(4, comments);
            ps.executeUpdate();
        }
    }

    public boolean existsForReservation(int reservationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM feedbacks WHERE reservation_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

}
