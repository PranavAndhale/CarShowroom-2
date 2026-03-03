package controllers;

import models.Customer;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerController {
    private Connection connection;

    public CustomerController() { this.connection = DatabaseManager.getInstance().getConnection(); }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id")); c.setCustomerId(rs.getString("customer_id"));
        c.setName(rs.getString("name")); c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone")); c.setAddress(rs.getString("address"));
        c.setRegistrationDate(rs.getDate("registration_date"));
        c.setPreferredCarType(rs.getString("preferred_car_type"));
        c.setBudget(rs.getDouble("budget"));
        return c;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        if (connection == null) return list;
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM customers ORDER BY name");
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public List<Customer> searchCustomers(String query) {
        List<Customer> list = new ArrayList<>();
        if (connection == null) return list;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM customers WHERE name LIKE ? OR email LIKE ? OR customer_id LIKE ?");
            stmt.setString(1, "%" + query + "%"); stmt.setString(2, "%" + query + "%");
            stmt.setString(3, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public boolean addCustomer(Customer c) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO customers (customer_id, name, email, phone, address, registration_date, preferred_car_type, budget) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, c.getCustomerId()); stmt.setString(2, c.getName());
            stmt.setString(3, c.getEmail()); stmt.setString(4, c.getPhone());
            stmt.setString(5, c.getAddress());
            stmt.setDate(6, new java.sql.Date(c.getRegistrationDate().getTime()));
            stmt.setString(7, c.getPreferredCarType()); stmt.setDouble(8, c.getBudget());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean updateCustomer(Customer c) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE customers SET name=?, email=?, phone=?, address=?, preferred_car_type=?, budget=? WHERE customer_id=?");
            stmt.setString(1, c.getName()); stmt.setString(2, c.getEmail());
            stmt.setString(3, c.getPhone()); stmt.setString(4, c.getAddress());
            stmt.setString(5, c.getPreferredCarType()); stmt.setDouble(6, c.getBudget());
            stmt.setString(7, c.getCustomerId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean deleteCustomer(String customerId) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM customers WHERE customer_id=?");
            stmt.setString(1, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public int getTotalCount() {
        if (connection == null) return 0;
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM customers");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
