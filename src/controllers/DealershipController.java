package controllers;

import models.Dealership;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DealershipController {
    private Connection connection;
    public DealershipController() { this.connection = DatabaseManager.getInstance().getConnection(); }

    private Dealership mapRow(ResultSet rs) throws SQLException {
        Dealership d = new Dealership();
        d.setId(rs.getInt("id")); d.setName(rs.getString("name"));
        d.setAddress(rs.getString("address")); d.setCity(rs.getString("city"));
        d.setLatitude(rs.getDouble("latitude")); d.setLongitude(rs.getDouble("longitude"));
        d.setOpeningTime(rs.getString("opening_time")); d.setClosingTime(rs.getString("closing_time"));
        d.setPhone(rs.getString("phone")); d.setRating(rs.getDouble("rating"));
        return d;
    }

    public List<Dealership> getAllDealerships() {
        List<Dealership> list = new ArrayList<>();
        if (connection == null) return list;
        try { ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM dealerships ORDER BY rating DESC"); while (rs.next()) list.add(mapRow(rs)); }
        catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public boolean addDealership(Dealership d) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO dealerships (name, address, city, latitude, longitude, opening_time, closing_time, phone, rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, d.getName()); stmt.setString(2, d.getAddress()); stmt.setString(3, d.getCity());
            stmt.setDouble(4, d.getLatitude()); stmt.setDouble(5, d.getLongitude());
            stmt.setString(6, d.getOpeningTime()); stmt.setString(7, d.getClosingTime());
            stmt.setString(8, d.getPhone()); stmt.setDouble(9, d.getRating());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean deleteDealership(int id) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM dealerships WHERE id=?");
            stmt.setInt(1, id); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }
}
