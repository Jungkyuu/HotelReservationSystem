package dao;

import db.DBUtil;
import model.Reservation;
import model.Room;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class ReservationDAO {

    public int insertReservation(Reservation r) throws SQLException {
        final String sql = """
            INSERT INTO reservations
                (guest_id, check_in, check_out, num_guests, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, r.getGuestId());
            ps.setString(2, r.getCheckInDate().toString());
            ps.setString(3, r.getCheckOutDate().toString());
            ps.setInt(4, r.getNumberOfGuests());
            ps.setString(5, r.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to insert reservation (no id returned)");
    }


    public Reservation findById(int id) {
        String sql = "SELECT id, guest_id, check_in, check_out, num_guests, status FROM reservations WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation r = new Reservation();
                    r.setReservationId(rs.getInt("id"));
                    r.setGuestId(rs.getInt("guest_id"));
                    r.setCheckInDate(java.sql.Date.valueOf(rs.getString("check_in")));
                    r.setCheckOutDate(java.sql.Date.valueOf(rs.getString("check_out")));
                    r.setNumberOfGuests(rs.getInt("num_guests"));
                    r.setStatus(rs.getString("status"));
                    return r;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
        return null;
    }

    public void addReservationRooms(int reservationId, List<Room> rooms) throws SQLException {
        if (rooms == null || rooms.isEmpty()) return;

        final String linkSql = "INSERT OR IGNORE INTO reservation_rooms (reservation_id, room_id) VALUES (?, ?)";
        final String bookSql = "UPDATE rooms SET status = 'Booked' WHERE id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement link = con.prepareStatement(linkSql);
             PreparedStatement book = con.prepareStatement(bookSql)) {
            con.setAutoCommit(false);
            try {
                for (Room room : rooms) {
                    link.setInt(1, reservationId);
                    link.setInt(2, room.getRoomId());
                    link.addBatch();

                    book.setInt(1, room.getRoomId());
                    book.addBatch();
                }
                link.executeBatch();
                book.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean exists(int reservationId) throws SQLException {
        final String sql = "SELECT 1 FROM reservations WHERE id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hardDelete(int reservationId) throws SQLException {
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rooms SET status='Available' " +
                                "WHERE id IN (SELECT room_id FROM reservation_rooms WHERE reservation_id=?)")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM reservation_rooms WHERE reservation_id=?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }

                int affected;
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM reservations WHERE id=?")) {
                    ps.setInt(1, reservationId);
                    affected = ps.executeUpdate();
                }

                c.commit();
                return affected > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }


    public void checkoutReservation(int reservationId) throws SQLException {
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rooms SET status='Available' " +
                                "WHERE id IN (SELECT room_id FROM reservation_rooms WHERE reservation_id=?)")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }




    public void updateDatesAndGuests(int reservationId, java.sql.Date checkIn, java.sql.Date checkOut, int numGuests) throws SQLException {
        final String sql = "UPDATE reservations SET check_in=?, check_out=?, num_guests=? WHERE id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, checkIn.toString());
            ps.setString(2, checkOut.toString());
            ps.setInt(3, numGuests);
            ps.setInt(4, reservationId);
            ps.executeUpdate();
        }
    }


    public void replaceReservationRooms(int reservationId, List<Integer> roomIds) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE rooms SET status='Available' " +
                                "WHERE id IN (SELECT room_id FROM reservation_rooms WHERE reservation_id=?)")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM reservation_rooms WHERE reservation_id=?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }

                
                if (roomIds != null && !roomIds.isEmpty()) {
                    try (PreparedStatement link = con.prepareStatement(
                            "INSERT INTO reservation_rooms (reservation_id, room_id) VALUES (?, ?)");
                         PreparedStatement book = con.prepareStatement(
                                 "UPDATE rooms SET status='Booked' WHERE id=?")) {
                        for (Integer roomId : roomIds) {
                            link.setInt(1, reservationId);
                            link.setInt(2, roomId);
                            link.addBatch();

                            book.setInt(1, roomId);
                            book.addBatch();
                        }
                        link.executeBatch();
                        book.executeBatch();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Reservation> listByGuestId(int guestId) throws SQLException {
        String sql = """
            SELECT id, guest_id, check_in, check_out, num_guests, status
            FROM reservations
            WHERE guest_id = ?
            ORDER BY check_in
        """;
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = new Reservation();
                    r.setReservationId(rs.getInt("id"));
                    r.setGuestId(rs.getInt("guest_id"));
                    r.setCheckInDate(Date.valueOf(rs.getString("check_in")));
                    r.setCheckOutDate(Date.valueOf(rs.getString("check_out")));
                    r.setNumberOfGuests(rs.getInt("num_guests"));
                    r.setStatus(rs.getString("status"));
                    list.add(r);
                }
            }
        }
        return list;
    }
}
