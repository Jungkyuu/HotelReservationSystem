package controller;

import dao.ReservationDAO;
import dao.RoomDAO;
import model.Billing;
import model.Reservation;
import model.Room;

import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BillingController {
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final RoomDAO roomDAO = new RoomDAO();


    public Billing generateBillFromDB(int reservationId, float taxRate, float discount) throws SQLException {
        // 1) reservation
        Reservation r = reservationDAO.findById(reservationId);
        if (r == null) throw new SQLException("Reservation not found: " + reservationId);

        // 2) rooms
        List<Room> rooms = roomDAO.findRoomsByReservation(reservationId);
        if (rooms == null || rooms.isEmpty()) {
            throw new SQLException("No rooms linked to reservation: " + reservationId);
        }

        // 3) nights
        long nights = ChronoUnit.DAYS.between(
                r.getCheckInDate().toLocalDate(),
                r.getCheckOutDate().toLocalDate()
        );
        if (nights <= 0) nights = 1;

        // 4) subtotal = sum(nightly prices) * nights
        float nightlySum = 0f;
        for (Room rm : rooms) nightlySum += rm.getPrice();
        float subtotal = nightlySum * (float) nights;

        // 5) tax/total
        float tax = round2(subtotal * taxRate);
        float total = round2(subtotal + tax - discount);

        // 6) build
        Billing bill = new Billing(0, reservationId, subtotal, tax, discount);
        try { bill.setTotalAmount(total); } catch (Throwable ignore) {}

        LoggerServiceController.logAdminActivity(
                "Generated bill (DB) for reservation " + reservationId +
                        ", nights=" + nights + ", subtotal=" + subtotal + ", tax=" + tax + ", total=" + total
        );
        return bill;
    }

    public Billing generateBillFromValues(int reservationId, float subtotal, float taxRate, float discount) {
        float tax = round2(subtotal * taxRate);
        float total = round2(subtotal + tax - discount);
        Billing bill = new Billing(0, reservationId, subtotal, tax, discount);
        try { bill.setTotalAmount(total); } catch (Throwable ignore) {}
        return bill;
    }

    private float round2(float v) { return Math.round(v * 100f) / 100f; }
}
