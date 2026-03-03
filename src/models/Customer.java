package models;

import java.util.Date;

public class Customer extends Person {
    private String customerId;
    private Date registrationDate;
    private String preferredCarType;
    private double budget;

    public Customer() { super(); }

    public Customer(int id, String name, String email, String phone, String address,
                    String customerId, Date registrationDate, String preferredCarType, double budget) {
        super(id, name, email, phone, address);
        this.customerId = customerId; this.registrationDate = registrationDate;
        this.preferredCarType = preferredCarType; this.budget = budget;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }
    public String getPreferredCarType() { return preferredCarType; }
    public void setPreferredCarType(String preferredCarType) { this.preferredCarType = preferredCarType; }
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    @Override
    public String getRole() { return "Customer"; }
}
