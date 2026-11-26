package controller;

import model.Guest;
import model.Reservation;
import model.Room;
import dao.ReservationDAO;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import db.DBUtil;
import java.sql.*;
import java.time.LocalDate;

public class ReservationController {
    private static final List<Reservation> reservationList = new ArrayList<>();
    private static final AtomicInteger reservationIdSequence = new AtomicInteger(1000);

    public Reservation bookRoom(Room room, Guest guest, Date checkIn, Date checkOut, int guestCount) {
        int reservationId = reservationIdSequence.getAndIncrement();
        guest.setGuestId(reservationId * 10);
        Reservation reservation = new Reservation(
                reservationId,
                guest.getGuestId(),
                room.getRoomId(),
                checkIn,
                checkOut,
                guestCount,
                "CONFIRMED"
        );
        reservationList.add(reservation);

        String guestFullName = guest.getFirstName() + " " + guest.getLastName();
        LoggerServiceController.logActivity("Reservation booked: " + reservationId + " for guest: " + guestFullName);
        return reservation;
    }

    public boolean cancelReservation(int reservationId) {
        try {
            ReservationDAO dao = new ReservationDAO();
            boolean deleted = dao.hardDelete(reservationId);
            if (deleted) {
                LoggerServiceController.logActivity("Reservation deleted: " + reservationId);
            } else {
                LoggerServiceController.logActivity("Reservation not found for delete: " + reservationId);
            }
            return deleted;
        } catch (SQLException e) {
            LoggerServiceController.logException("Cancel failed", e);
            return false;
        }
    }


    public LocalDate getCheckInDate(int reservationId) {
        String sql = "SELECT check_in FROM reservations WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return LocalDate.parse(rs.getString(1));
            }
        } catch (SQLException e) {
            LoggerServiceController.logException("getCheckInDate failed", e);
        }
        return LocalDate.now();
    }

    public LocalDate getCheckOutDate(int reservationId) {
        String sql = "SELECT check_out FROM reservations WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return LocalDate.parse(rs.getString(1));
            }
        } catch (SQLException e) {
            LoggerServiceController.logException("getCheckOutDate failed", e);
        }
        return LocalDate.now().plusDays(1);
    }
}
