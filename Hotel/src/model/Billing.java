package model;

public class Billing {
    private int billId;
    private int reservationId;
    private float amount;
    private float tax;
    private float totalAmount;
    private float discount;

    public Billing() {}

    public Billing(int billId, int reservationId, float amount, float tax, float discount) {
        this.billId = billId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.tax = tax;
        this.discount = discount;
        this.totalAmount = calculateTotal();
    }

    public float calculateTotal() {
        return (amount + tax) - discount;
    }

    // Getters and Setters
    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public float getTax() { return tax; }
    public void setTax(float tax) { this.tax = tax; }

    public float getTotalAmount() { return totalAmount; }
    public void setTotalAmount(float totalAmount) { this.totalAmount = totalAmount; }

    public float getDiscount() { return discount; }
    public void setDiscount(float discount) { this.discount = discount; }
}
