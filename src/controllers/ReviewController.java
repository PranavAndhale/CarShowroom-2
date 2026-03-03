package controllers;

import models.Review;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewController {
    private Connection connection;
    public ReviewController() { this.connection = DatabaseManager.getInstance().getConnection(); }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id")); r.setCarBrand(rs.getString("car_brand"));
        r.setCarModel(rs.getString("car_model")); r.setAuthor(rs.getString("author"));
        r.setPlatform(rs.getString("platform")); r.setContent(rs.getString("content"));
        r.setRating(rs.getInt("rating")); r.setReviewDate(rs.getDate("review_date"));
        r.setSourceUrl(rs.getString("source_url"));
        return r;
    }

    public List<Review> getAllReviews() {
        List<Review> list = new ArrayList<>();
        if (connection == null) return list;
        try { ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM reviews ORDER BY review_date DESC"); while (rs.next()) list.add(mapRow(rs)); }
        catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public List<Review> getReviewsByBrand(String brand) {
        List<Review> list = new ArrayList<>();
        if (connection == null) return list;
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM reviews WHERE car_brand=? ORDER BY review_date DESC");
            stmt.setString(1, brand);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public boolean addReview(Review r) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO reviews (car_brand, car_model, author, platform, content, rating, review_date, source_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, r.getCarBrand()); stmt.setString(2, r.getCarModel());
            stmt.setString(3, r.getAuthor()); stmt.setString(4, r.getPlatform());
            stmt.setString(5, r.getContent()); stmt.setInt(6, r.getRating());
            stmt.setDate(7, new java.sql.Date(r.getReviewDate().getTime()));
            stmt.setString(8, r.getSourceUrl());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }

    public boolean deleteReview(int id) {
        if (connection == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM reviews WHERE id=?");
            stmt.setInt(1, id); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); return false; }
    }
}
