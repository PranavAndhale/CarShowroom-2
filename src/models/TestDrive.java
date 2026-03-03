package models;

import java.util.Date;

public class TestDrive {
    private int id;
    private int carId;
    private int customerId;
    private Date scheduledDate;
    private String timeSlot;
    private String status;
    private String feedback;
    private String carName;
    private String customerName;

    public TestDrive() {}

    public TestDrive(int id, int carId, int customerId, Date scheduledDate, String timeSlot, String status) {
        this.id = id; this.carId = carId; this.customerId = customerId;
        this.scheduledDate = scheduledDate; this.timeSlot = timeSlot; this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public Date getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(Date scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    @Override
    public String toString() { return "TestDrive #" + id + " - " + scheduledDate + " (" + status + ")"; }
}
