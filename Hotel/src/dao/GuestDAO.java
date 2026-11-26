package dao;

import db.DBUtil;
import model.Guest;

import java.sql.*;

public class GuestDAO {

    public Guest findByPhone(String phone) throws SQLException {
        final String sql = "SELECT id, first_name, last_name, phone, email, address FROM guests WHERE phone = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Guest g = new Guest();
                    g.setGuestId(rs.getInt("id"));
                    g.setFirstName(rs.getString("first_name"));
                    g.setLastName(rs.getString("last_name"));
                    g.setPhoneNumber(rs.getString("phone"));
                    g.setEmail(rs.getString("email"));
                    g.setAddress(rs.getString("address"));
                    return g;
                }
            }
        }
        return null;
    }

    public int upsertAndGetId(Guest g) throws SQLException {
        final String upsert = """
            INSERT INTO guests(first_name, last_name, phone, email, address)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(phone) DO UPDATE SET
               first_name=excluded.first_name,
               last_name=excluded.last_name,
               email=excluded.email,
               address=excluded.address
            """;

        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(upsert)) {
                ps.setString(1, g.getFirstName());
                ps.setString(2, g.getLastName());
                ps.setString(3, g.getPhoneNumber());
                ps.setString(4, g.getEmail());
                ps.setString(5, g.getAddress());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM guests WHERE phone = ?")) {
                ps.setString(1, g.getPhoneNumber());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to upsert guest");
    }
}
