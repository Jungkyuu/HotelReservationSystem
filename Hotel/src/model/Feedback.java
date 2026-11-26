package model;

public class Feedback {
    private int feedbackId;
    private int guestId;
    private int reservationId;
    private String comments;
    private int rating;

    public Feedback() {}

    public Feedback(int feedbackId, int guestId, int reservationId, String comments, int rating) {
        this.feedbackId = feedbackId;
        this.guestId = guestId;
        this.reservationId = reservationId;
        this.comments = comments;
        this.rating = rating;
    }

    // Getters and Setters
    public int getFeedbackId() { return feedbackId; }
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }

    public int getGuestId() { return guestId; }
    public void setGuestId(int guestId) { this.guestId = guestId; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}
