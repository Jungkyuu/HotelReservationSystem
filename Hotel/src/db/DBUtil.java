package db;

import java.sql.*;

public final class DBUtil {
    private static final String URL = "jdbc:sqlite:hotel.db";
    private static boolean initialized = false;

    private DBUtil() {}


    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }


    public static synchronized void init() {
        if (initialized) return;

        try (Connection c = getConnection();
             Statement st = c.createStatement()) {

            // --- tables ---

            // guests
            st.execute("""
                CREATE TABLE IF NOT EXISTS guests (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name  TEXT NOT NULL,
                    phone      TEXT UNIQUE NOT NULL,
                    email      TEXT,
                    address    TEXT
                )
            """);

            // rooms
            st.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    type   TEXT NOT NULL,         -- SINGLE | DOUBLE | DELUXE | PENTHOUSE
                    beds   INTEGER NOT NULL,
                    price  REAL NOT NULL,
                    status TEXT NOT NULL          -- Available | Booked | Maintenance
                )
            """);

            // reservations
            st.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    guest_id    INTEGER NOT NULL,
                    check_in    TEXT NOT NULL,
                    check_out   TEXT NOT NULL,
                    num_guests  INTEGER NOT NULL,
                    status      TEXT NOT NULL,    -- PENDING | CONFIRMED | CHECKED_IN | CHECKED_OUT | CANCELLED
                    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE
                )
            """);

            // reservation_rooms
            st.execute("""
                CREATE TABLE IF NOT EXISTS reservation_rooms (
                    reservation_id INTEGER NOT NULL,
                    room_id        INTEGER NOT NULL,
                    PRIMARY KEY (reservation_id, room_id),
                    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
                    FOREIGN KEY (room_id)        REFERENCES rooms(id)
                )
            """);

            // feedbacks
            st.execute("""
                CREATE TABLE IF NOT EXISTS feedbacks (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    reservation_id INTEGER NOT NULL,
                    guest_id       INTEGER NOT NULL,
                    rating         INTEGER NOT NULL,
                    comments       TEXT,
                    created_at     TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
                    FOREIGN KEY (guest_id)       REFERENCES guests(id) ON DELETE CASCADE
                )
            """);

            // --- seed rooms ---
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.addBatch("INSERT INTO rooms (id,type,beds,price,status) VALUES (1,'SINGLE',1,80,'Available')");
                    st.addBatch("INSERT INTO rooms (id,type,beds,price,status) VALUES (2,'SINGLE',2,85,'Available')");
                    st.addBatch("INSERT INTO rooms (id,type,beds,price,status) VALUES (3,'DOUBLE',2,120,'Available')");
                    st.addBatch("INSERT INTO rooms (id,type,beds,price,status) VALUES (4,'DOUBLE',4,150,'Available')");
                    st.addBatch("INSERT INTO rooms (id,type,beds,price,status) VALUES (5,'DELUXE',2,220,'Available')");
                    st.addBatch("INSERT INTO rooms (id,type,beds,price,status) VALUES (6,'PENTHOUSE',2,320,'Available')");
                    st.executeBatch();
                }
            }

            initialized = true;
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }
}
