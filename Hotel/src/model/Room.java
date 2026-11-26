package model;

public class Room {
    private int roomId;
    private RoomType roomType;
    private int numberOfBeds;
    private float price;
    private String status;

    // Constructor with all fields
    public Room(int roomId, RoomType roomType, int numberOfBeds, float price, String status) {
        this.roomId = roomId;
        this.roomType = roomType;
        this.numberOfBeds = numberOfBeds;
        this.price = price;
        this.status = status;
    }

    // Getter methods
    public int getRoomId() { return roomId; }
    public RoomType getRoomType() { return roomType; }
    public int getNumberOfBeds() { return numberOfBeds; }
    public float getPrice() { return price; }
    public String getStatus() { return status; }

    // Setter for status (if needed)
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Room " + roomId + " - " + roomType + " - Beds: " + numberOfBeds + " - $" + price + " - " + status;
    }
}
