package controllers;

import models.Employee;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeController {
    private Connection connection;
    public EmployeeController() { this.connection = DatabaseManager.getInstance().getConnection(); }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("id")); e.setEmployeeId(rs.getString("employee_id"));
        e.setName(rs.getString("name")); e.setEmail(rs.getString("email"));
        e.setPhone(rs.getString("phone")); e.setAddress(rs.getString("address"));
        e.setDepartment(rs.getString("department")); e.setDesignation(rs.getString("designation"));
        e.setSalary(rs.getDouble("salary")); e.setJoiningDate(rs.getDate("joining_date"));
        return e;
    }

    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        if (connection == null) return list;
        try { ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM employees ORDER BY name"); while (rs.next()) list.add(mapRow(rs)); }
        catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public boolean addEmployee(Employee emp) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO employees (employee_id, name, email, phone, address, department, designation, salary, joining_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, emp.getEmployeeId()); stmt.setString(2, emp.getName());
            stmt.setString(3, emp.getEmail()); stmt.setString(4, emp.getPhone());
            stmt.setString(5, emp.getAddress()); stmt.setString(6, emp.getDepartment());
            stmt.setString(7, emp.getDesignation()); stmt.setDouble(8, emp.getSalary());
            stmt.setDate(9, new java.sql.Date(emp.getJoiningDate().getTime()));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean updateEmployee(Employee emp) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE employees SET name=?, email=?, phone=?, address=?, department=?, designation=?, salary=? WHERE employee_id=?");
            stmt.setString(1, emp.getName()); stmt.setString(2, emp.getEmail());
            stmt.setString(3, emp.getPhone()); stmt.setString(4, emp.getAddress());
            stmt.setString(5, emp.getDepartment()); stmt.setString(6, emp.getDesignation());
            stmt.setDouble(7, emp.getSalary()); stmt.setString(8, emp.getEmployeeId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean deleteEmployee(String employeeId) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM employees WHERE employee_id=?");
            stmt.setString(1, employeeId); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public int getTotalCount() {
        if (connection == null) return 0;
        try { ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM employees"); if (rs.next()) return rs.getInt(1); } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<String[]> getPayrollSummary() {
        List<String[]> list = new ArrayList<>();
        if (connection == null) return list;
        try {
            String sql = "SELECT department, COUNT(*) as headcount, SUM(salary) as total " +
                         "FROM employees GROUP BY department ORDER BY total DESC";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(new String[]{rs.getString("department"), String.valueOf(rs.getInt("headcount")),
                    "$" + String.format("%,.0f", rs.getDouble("total"))});
            }
        } catch (SQLException e) { System.err.println("Payroll error: " + e.getMessage()); }
        return list;
    }
}
