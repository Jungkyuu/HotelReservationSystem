BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS admins (id INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT UNIQUE NOT NULL,password TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS feedback (
        id             INTEGER PRIMARY KEY AUTOINCREMENT,
        reservation_id INTEGER NOT NULL,
        guest_id       INTEGER NOT NULL,
        rating         INTEGER NOT NULL,
        comments       TEXT,
        created_at     TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
        FOREIGN KEY (guest_id)       REFERENCES guests(id) ON DELETE CASCADE
    );
CREATE TABLE IF NOT EXISTS feedbacks (
        id             INTEGER PRIMARY KEY AUTOINCREMENT,
        reservation_id INTEGER NOT NULL,
        guest_id       INTEGER NOT NULL,
        rating         INTEGER NOT NULL,
        comments       TEXT,
        created_at     TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
        FOREIGN KEY (guest_id)       REFERENCES guests(id) ON DELETE CASCADE
    );
CREATE TABLE IF NOT EXISTS guests (
        id         INTEGER PRIMARY KEY AUTOINCREMENT,
        first_name TEXT NOT NULL,
        last_name  TEXT NOT NULL,
        phone      TEXT UNIQUE NOT NULL,
        email      TEXT,
        address    TEXT
    );
CREATE TABLE IF NOT EXISTS reservation_rooms (
        reservation_id INTEGER NOT NULL,
        room_id        INTEGER NOT NULL,
        PRIMARY KEY (reservation_id, room_id),
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
        FOREIGN KEY (room_id)        REFERENCES rooms(id)
    );
CREATE TABLE IF NOT EXISTS reservations (
        id          INTEGER PRIMARY KEY AUTOINCREMENT,
        guest_id    INTEGER NOT NULL,
        check_in    TEXT NOT NULL,
        check_out   TEXT NOT NULL,
        num_guests  INTEGER NOT NULL,
        status      TEXT NOT NULL,    -- PENDING | CONFIRMED | CHECKED_IN | CHECKED_OUT | CANCELLED
        FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE
    );
CREATE TABLE IF NOT EXISTS rooms (
        id     INTEGER PRIMARY KEY AUTOINCREMENT,
        type   TEXT NOT NULL,         -- SINGLE | DOUBLE | DELUXE | PENTHOUSE
        beds   INTEGER NOT NULL,
        price  REAL NOT NULL,
        status TEXT NOT NULL          -- Available | Booked | Maintenance
    );
INSERT INTO "admins" ("id","username","password") VALUES (1,'admin1','pass123'),
 (2,'admin2','pass456');
INSERT INTO "feedbacks" ("id","reservation_id","guest_id","rating","comments","created_at") VALUES (1,1,1,5,'It is so confortable place!','2025-08-10 07:04:34');
INSERT INTO "guests" ("id","first_name","last_name","phone","email","address") VALUES (1,'Jung Kyu','Mok','437-438-1291','jkmok@myseneca.ca','1760 Finch Ave');
INSERT INTO "reservation_rooms" ("reservation_id","room_id") VALUES (1,1),
 (1,3);
INSERT INTO "reservations" ("id","guest_id","check_in","check_out","num_guests","status") VALUES (1,1,'2025-08-01','2025-08-15',3,'CHECKED_OUT');
INSERT INTO "rooms" ("id","type","beds","price","status") VALUES (1,'SINGLE',1,80.0,'Available'),
 (2,'SINGLE',2,85.0,'Available'),
 (3,'DOUBLE',2,120.0,'Available'),
 (4,'DOUBLE',4,150.0,'Available'),
 (5,'DELUXE',2,220.0,'Available'),
 (6,'PENTHOUSE',2,320.0,'Available');
COMMIT;
