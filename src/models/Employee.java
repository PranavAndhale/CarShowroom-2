package models;

import java.util.Date;

public class Employee extends Person {
    private String employeeId;
    private String department;
    private double salary;
    private Date joiningDate;
    private String designation;

    public Employee() { super(); }

    public Employee(int id, String name, String email, String phone, String address,
                    String employeeId, String department, double salary, Date joiningDate, String designation) {
        super(id, name, email, phone, address);
        this.employeeId = employeeId; this.department = department;
        this.salary = salary; this.joiningDate = joiningDate; this.designation = designation;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    public Date getJoiningDate() { return joiningDate; }
    public void setJoiningDate(Date joiningDate) { this.joiningDate = joiningDate; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    @Override
    public String getRole() { return "Employee"; }
}
