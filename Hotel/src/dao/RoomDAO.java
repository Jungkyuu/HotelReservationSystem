package dao;

import db.DBUtil;
import model.Room;
import model.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    public void upsert(Room r) throws SQLException {
        // Insert or replace room by id
        String sql = """
            INSERT INTO rooms(id, type, beds, price, status)
            VALUES(?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET type=excluded.type, beds=excluded.beds, price=excluded.price, status=excluded.status
            """;
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getRoomId());
            ps.setString(2, r.getRoomType().name());
            ps.setInt(3, r.getNumberOfBeds());
            ps.setDouble(4, r.getPrice());
            ps.setString(5, r.getStatus());
            ps.executeUpdate();
        }
    }

    public List<Room> findByIds(List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        String sql = "SELECT id, type, beds, price, status FROM rooms WHERE id IN (" + placeholders + ")";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : ids) ps.setInt(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Room> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new Room(
                            rs.getInt("id"),
                            RoomType.valueOf(rs.getString("type")),
                            rs.getInt("beds"),
                            (float) rs.getDouble("price"),
                            rs.getString("status")
                    ));
                }
                return result;
            }
        }
    }

    public void updateStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    public List<Room> findRoomsByReservation(int reservationId) throws SQLException {
        String sql = """
        SELECT r.id, r.type, r.beds, r.price, r.status
        FROM rooms r
        JOIN reservation_rooms rr ON rr.room_id = r.id
        WHERE rr.reservation_id = ?
    """;
        List<Room> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Room rm = new Room(
                            rs.getInt("id"),
                            RoomType.valueOf(rs.getString("type")),
                            rs.getInt("beds"),
                            rs.getFloat("price"),
                            rs.getString("status")
                    );
                    list.add(rm);
                }
            }
        }
        return list;
    }

    public List<Room> findAvailable() throws SQLException {
        String sql = "SELECT id, type, beds, price, status FROM rooms WHERE status = 'Available'";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
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
