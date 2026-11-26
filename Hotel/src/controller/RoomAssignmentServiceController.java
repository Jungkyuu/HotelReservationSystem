package controller;

import model.Room;
import model.RoomType;

import java.util.ArrayList;
import java.util.List;

public class RoomAssignmentServiceController {

    private final List<Room> allRooms = List.of(
            new Room(1, RoomType.SINGLE, 1, 80f, "Available"),
            new Room(2, RoomType.SINGLE, 2, 85f, "Available"),
            new Room(3, RoomType.DOUBLE, 2, 130f, "Available"),
            new Room(4, RoomType.DOUBLE, 4, 150f, "Available"),
            new Room(5, RoomType.DELUXE, 2, 200f, "Available"),
            new Room(6, RoomType.PENTHOUSE, 2, 400f, "Available")
    );

    public List<List<Room>> suggestRoomCombinations(int guestCount) {
        List<List<Room>> result = new ArrayList<>();

        // Rule: 1~2 guests: Single, Deluxe, Penthouse
        if (guestCount == 1 || guestCount == 2) {
            for (Room r : allRooms) {
                if ((r.getRoomType() == RoomType.SINGLE
                        || r.getRoomType() == RoomType.DELUXE
                        || r.getRoomType() == RoomType.PENTHOUSE)
                        && r.getNumberOfBeds() >= guestCount
                        && r.getStatus().equalsIgnoreCase("Available")) {
                    result.add(List.of(r));
                }
            }
        }
        // Rule: 3~4 guests: Double (or 2 Single), Deluxe+Single, etc.
        else if (guestCount >= 3 && guestCount <= 4) {
            // Double rooms
            for (Room r : allRooms) {
                if (r.getRoomType() == RoomType.DOUBLE
                        && r.getNumberOfBeds() >= guestCount
                        && r.getStatus().equalsIgnoreCase("Available")) {
                    result.add(List.of(r));
                }
            }
            // Two single rooms
            List<Room> singles = new ArrayList<>();
            for (Room r : allRooms) {
                if (r.getRoomType() == RoomType.SINGLE
                        && r.getStatus().equalsIgnoreCase("Available")) {
                    singles.add(r);
                }
            }
            if (singles.size() >= 2) {
                result.add(List.of(singles.get(0), singles.get(1)));
            }
            // Deluxe/Penthouse + Double for 4 guests
            if (guestCount == 4) {
                for (Room r : allRooms) {
                    if ((r.getRoomType() == RoomType.DELUXE
                            || r.getRoomType() == RoomType.PENTHOUSE)
                            && r.getStatus().equalsIgnoreCase("Available")) {
                        for (Room doubleRoom : allRooms) {
                            if (doubleRoom.getRoomType() == RoomType.DOUBLE
                                    && doubleRoom.getStatus().equalsIgnoreCase("Available")
                                    && doubleRoom != r) {
                                result.add(List.of(doubleRoom, r));
                            }
                        }
                    }
                }
            }
        }
        // Rule: More than 4 guests - multiple Doubles and Singles
        else if (guestCount > 4) {
            int left = guestCount;
            List<Room> combo = new ArrayList<>();
            // Use as many Double rooms as possible
            for (Room r : allRooms) {
                if (r.getRoomType() == RoomType.DOUBLE
                        && r.getStatus().equalsIgnoreCase("Available")
                        && left >= 2) {
                    combo.add(r);
                    left -= r.getNumberOfBeds();
                    if (left <= 0) break;
                }
            }
            // Fill remaining with Singles
            if (left > 0) {
                for (Room r : allRooms) {
                    if (r.getRoomType() == RoomType.SINGLE
                            && r.getStatus().equalsIgnoreCase("Available")) {
                        combo.add(r);
                        left -= r.getNumberOfBeds();
                        if (left <= 0) break;
                    }
                }
            }
            if (left <= 0 && !combo.isEmpty()) result.add(combo);
        }
        return result;
    }

    public List<Room> getAvailableRooms(int guestCount) {
        List<List<Room>> combos = suggestRoomCombinations(guestCount);
        List<Room> result = new ArrayList<>();
        for (List<Room> combo : combos) {
            if (combo.size() == 1) {
                result.add(combo.get(0));
            }
        }
        return result;
    }
}
