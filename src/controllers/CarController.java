package controllers;

import models.Car;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarController {

    private Connection connection;

    public CarController() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    // ── Helper: map a ResultSet row → Car ─────────────────────────────────────
    private Car mapRow(ResultSet rs) throws SQLException {
        Car car = new Car();
        car.setId(rs.getInt("id"));
        car.setBrand(rs.getString("brand"));
        car.setModel(rs.getString("model"));
        car.setYear(rs.getInt("year"));
        car.setColor(rs.getString("color"));
        car.setPrice(rs.getDouble("price"));
        car.setCategory(rs.getString("category"));
        car.setStock(rs.getInt("stock"));
        car.setAvailable(rs.getBoolean("is_available"));
        car.setImagePath(rs.getString("image_path"));

        // Safely read new columns (may be NULL for old rows)
        try {
            car.setFuelType(rs.getString("fuel_type"));
        } catch (SQLException ignored) {
        }
        try {
            car.setTransmission(rs.getString("transmission"));
        } catch (SQLException ignored) {
        }
        try {
            car.setEngineSize(rs.getDouble("engine_size"));
        } catch (SQLException ignored) {
        }
        try {
            car.setSketchfabModelId(rs.getString("sketchfab_model_id"));
        } catch (SQLException ignored) {
        }

        // Parse comma-separated features
        String feat = rs.getString("features");
        if (feat != null && !feat.isEmpty()) {
            for (String f : feat.split(",")) {
                String trimmed = f.trim();
                if (!trimmed.isEmpty())
                    car.addFeature(trimmed);
            }
        }
        return car;
    }

    // ── GET ALL CARS ──────────────────────────────────────────────────────────
    public List<Car> getAllCars() {
        List<Car> cars = new ArrayList<>();
        try {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT * FROM cars ORDER BY id");
            while (rs.next())
                cars.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error fetching cars: " + e.getMessage());
        }
        return cars;
    }

    // ── GET CARS BY PRICE RANGE ───────────────────────────────────────────────
    public List<Car> getCarsByPriceRange(double minPrice, double maxPrice) {
        List<Car> cars = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM cars WHERE price BETWEEN ? AND ? AND is_available = TRUE ORDER BY price");
            stmt.setDouble(1, minPrice);
            stmt.setDouble(2, maxPrice);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                cars.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error fetching cars by price: " + e.getMessage());
        }
        return cars;
    }

    // ── GET CAR BY ID ─────────────────────────────────────────────────────────
    public Car getCarById(int id) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM cars WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Error fetching car: " + e.getMessage());
        }
        return null;
    }

    // ── ADD CAR ───────────────────────────────────────────────────────────────
    public boolean addCar(Car car) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO cars " +
                            "(brand, model, year, color, price, category, stock, " +
                            " fuel_type, transmission, engine_size, features, image_path, is_available) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, car.getBrand());
            stmt.setString(2, car.getModel());
            stmt.setInt(3, car.getYear());
            stmt.setString(4, car.getColor());
            stmt.setDouble(5, car.getPrice());
            stmt.setString(6, car.getCategory());
            stmt.setInt(7, car.getStock());
            stmt.setString(8, car.getFuelType());
            stmt.setString(9, car.getTransmission());
            stmt.setDouble(10, car.getEngineSize());
            stmt.setString(11, car.getFeatures() != null
                    ? String.join(",", car.getFeatures())
                    : "");
            stmt.setString(12, car.getImagePath() != null ? car.getImagePath() : "");
            stmt.setBoolean(13, car.isAvailable());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding car: " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE CAR ────────────────────────────────────────────────────────────
    public boolean updateCar(Car car) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE cars SET " +
                            "  brand=?, model=?, year=?, color=?, price=?, category=?, stock=?, " +
                            "  fuel_type=?, transmission=?, engine_size=?, features=?, " +
                            "  image_path=?, is_available=? " +
                            "WHERE id=?");
            stmt.setString(1, car.getBrand());
            stmt.setString(2, car.getModel());
            stmt.setInt(3, car.getYear());
            stmt.setString(4, car.getColor());
            stmt.setDouble(5, car.getPrice());
            stmt.setString(6, car.getCategory());
            stmt.setInt(7, car.getStock());
            stmt.setString(8, car.getFuelType());
            stmt.setString(9, car.getTransmission());
            stmt.setDouble(10, car.getEngineSize());
            stmt.setString(11, car.getFeatures() != null
                    ? String.join(",", car.getFeatures())
                    : "");
            stmt.setString(12, car.getImagePath() != null ? car.getImagePath() : "");
            stmt.setBoolean(13, car.isAvailable());
            stmt.setInt(14, car.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating car: " + e.getMessage());
            return false;
        }
    }

    // ── DELETE CAR ────────────────────────────────────────────────────────────
    public boolean deleteCar(int carId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM cars WHERE id = ?");
            stmt.setInt(1, carId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting car: " + e.getMessage());
            return false;
        }
    }

    // ── GET TOTAL STOCK COUNT ─────────────────────────────────────────────────
    /** Returns the total number of cars in stock across all vehicles. */
    public int getTotalStock() {
        try {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM cars WHERE is_available = TRUE");
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error getting total stock: " + e.getMessage());
        }
        return 0;
    }

    // ── GET LOW STOCK CARS ────────────────────────────────────────────────────
    /** Returns cars whose stock is at or below the given threshold. */
    public List<Car> getLowStockCars(int threshold) {
        List<Car> cars = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM cars WHERE stock <= ? AND stock >= 0 ORDER BY stock ASC");
            stmt.setInt(1, threshold);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                cars.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error getting low stock cars: " + e.getMessage());
        }
        return cars;
    }

    // ── ADVANCED SEARCH ───────────────────────────────────────────────────────
    /**
     * Searches cars by text query, category filter, and price range.
     * Pass null/"All" for category and 0/Double.MAX_VALUE for price range to skip
     * those filters.
     */
    public List<Car> searchCars(String query, String category, double minPrice, double maxPrice) {
        List<Car> cars = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT * FROM cars WHERE 1=1 ");
            boolean hasQuery = query != null && !query.trim().isEmpty();
            boolean hasCategory = category != null && !category.equals("All") && !category.isEmpty();
            boolean hasPrice = minPrice > 0 || maxPrice < Double.MAX_VALUE;

            if (hasQuery)
                sql.append("AND (brand LIKE ? OR model LIKE ? OR color LIKE ? OR category LIKE ?) ");
            if (hasCategory)
                sql.append("AND category = ? ");
            if (hasPrice)
                sql.append("AND price BETWEEN ? AND ? ");
            sql.append("ORDER BY brand, model");

            PreparedStatement stmt = connection.prepareStatement(sql.toString());
            int idx = 1;
            if (hasQuery) {
                String q = "%" + query.trim() + "%";
                stmt.setString(idx++, q);
                stmt.setString(idx++, q);
                stmt.setString(idx++, q);
                stmt.setString(idx++, q);
            }
            if (hasCategory)
                stmt.setString(idx++, category);
            if (hasPrice) {
                stmt.setDouble(idx++, minPrice);
                stmt.setDouble(idx, maxPrice);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                cars.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error searching cars: " + e.getMessage());
        }
        return cars;
    }

}
