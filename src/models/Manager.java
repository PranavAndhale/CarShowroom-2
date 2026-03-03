package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Manager extends Employee {
    private List<Employee> teamMembers;
    private String region;
    private double bonus;

    public Manager() { super(); this.teamMembers = new ArrayList<>(); }

    public Manager(int id, String name, String email, String phone, String address,
                   String employeeId, String department, double salary, Date joiningDate,
                   String designation, String region, double bonus) {
        super(id, name, email, phone, address, employeeId, department, salary, joiningDate, designation);
        this.teamMembers = new ArrayList<>(); this.region = region; this.bonus = bonus;
    }

    public List<Employee> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<Employee> teamMembers) { this.teamMembers = teamMembers; }
    public void addTeamMember(Employee employee) { this.teamMembers.add(employee); }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public double getBonus() { return bonus; }
    public void setBonus(double bonus) { this.bonus = bonus; }
    public double getTotalCompensation() { return getSalary() + bonus; }

    @Override
    public String getRole() { return "Manager"; }
}
