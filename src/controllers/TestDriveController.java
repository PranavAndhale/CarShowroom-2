package controllers;

import models.TestDrive;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestDriveController {
    private Connection connection;
    public TestDriveController() { this.connection = DatabaseManager.getInstance().getConnection(); }

    public List<TestDrive> getAllTestDrives() {
        List<TestDrive> list = new ArrayList<>();
        if (connection == null) return list;
        try {
            String sql = "SELECT td.*, CONCAT(c.brand,' ',c.model) as car_name, cust.name as customer_name " +
                "FROM test_drives td LEFT JOIN cars c ON td.car_id=c.id " +
                "LEFT JOIN customers cust ON td.customer_id=cust.id ORDER BY td.scheduled_date DESC";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                TestDrive td = new TestDrive();
                td.setId(rs.getInt("id")); td.setCarId(rs.getInt("car_id"));
                td.setCustomerId(rs.getInt("customer_id")); td.setScheduledDate(rs.getDate("scheduled_date"));
                td.setTimeSlot(rs.getString("time_slot")); td.setStatus(rs.getString("status"));
                td.setFeedback(rs.getString("feedback"));
                try { td.setCarName(rs.getString("car_name")); } catch (Exception ignored) {}
                try { td.setCustomerName(rs.getString("customer_name")); } catch (Exception ignored) {}
                list.add(td);
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public boolean addTestDrive(TestDrive td) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO test_drives (car_id, customer_id, scheduled_date, time_slot, status, feedback) VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, td.getCarId()); stmt.setInt(2, td.getCustomerId());
            stmt.setDate(3, new java.sql.Date(td.getScheduledDate().getTime()));
            stmt.setString(4, td.getTimeSlot()); stmt.setString(5, td.getStatus());
            stmt.setString(6, td.getFeedback());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean updateStatus(int id, String status, String feedback) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE test_drives SET status=?, feedback=? WHERE id=?");
            stmt.setString(1, status); stmt.setString(2, feedback); stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean deleteTestDrive(int id) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM test_drives WHERE id=?");
            stmt.setInt(1, id); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public int getScheduledCount() {
        if (connection == null) return 0;
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM test_drives WHERE status='Scheduled'");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
