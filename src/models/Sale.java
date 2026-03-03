package models;

import java.util.Date;

public class Sale {
    private int id;
    private int carId;
    private int customerId;
    private int employeeId;
    private Date saleDate;
    private double salePrice;
    private String paymentMethod;
    private String status;
    // Display helpers
    private String carName;
    private String customerName;
    private String employeeName;

    public Sale() {}

    public Sale(int id, int carId, int customerId, int employeeId,
                Date saleDate, double salePrice, String paymentMethod, String status) {
        this.id = id; this.carId = carId; this.customerId = customerId;
        this.employeeId = employeeId; this.saleDate = saleDate;
        this.salePrice = salePrice; this.paymentMethod = paymentMethod; this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public Date getSaleDate() { return saleDate; }
    public void setSaleDate(Date saleDate) { this.saleDate = saleDate; }
    public double getSalePrice() { return salePrice; }
    public void setSalePrice(double salePrice) { this.salePrice = salePrice; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    @Override
    public String toString() { return "Sale #" + id + " - $" + String.format("%,.0f", salePrice) + " (" + status + ")"; }
}
