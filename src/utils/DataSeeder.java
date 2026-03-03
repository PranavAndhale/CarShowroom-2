package utils;

import models.*;
import java.sql.*;
import java.util.Date;

public class DataSeeder {
    private Connection connection;

    public DataSeeder(Connection connection) {
        this.connection = connection;
    }

    /** Seeds data only if the cars table is empty — safe to call on every startup. */
    public void seedIfEmpty() {
        try {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM cars");
            rs.next();
            if (rs.getInt(1) == 0) {
                seedSampleData();
                System.out.println("[DataSeeder] Sample data seeded.");
            } else {
                System.out.println("[DataSeeder] Data already exists, skipping seed.");
            }
        } catch (Exception e) {
            System.err.println("[DataSeeder] seedIfEmpty error: " + e.getMessage());
        }
    }

    public void seedSampleData() {
        try {
            seedCars();
            seedEmployees();
            seedManagers();
            seedCustomers();
            seedDealerships();
            seedReviews();
            System.out.println("Sample data seeded successfully!");
        } catch (SQLException e) {
            System.err.println("Error seeding data: " + e.getMessage());
        }
    }

    private void seedCars() throws SQLException {
        String sql = "INSERT INTO cars (brand, model, year, color, price, category, stock, features, is_available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        String[][] cars = {
                {"Toyota", "Camry", "2023", "Silver", "28000", "Sedan", "5", "Sunroof,Leather Seats,Navigation", "1"},
                {"Honda", "Accord", "2023", "Black", "30000", "Sedan", "3", "Bluetooth,Backup Camera,Cruise Control", "1"},
                {"Ford", "Explorer", "2024", "Blue", "45000", "SUV", "4", "4WD,Third Row Seats,Safety Package", "1"},
                {"Tesla", "Model 3", "2024", "White", "42000", "Electric", "2", "Autopilot,Electric,Premium Audio", "1"},
                {"BMW", "X5", "2023", "Gray", "65000", "SUV", "2", "Luxury Package,Sport Mode,Panoramic Roof", "1"},
                {"Mercedes", "C-Class", "2023", "Red", "55000", "Luxury", "3", "AMG Package,Premium Sound,LED Lights", "1"},
                {"Audi", "Q7", "2024", "White", "68000", "SUV", "1", "Quattro,Virtual Cockpit,Bang & Olufsen", "1"},
                {"Hyundai", "Tucson", "2023", "Blue", "32000", "SUV", "6", "Smart Cruise,Lane Assist,Wireless Charging", "1"},
                {"Mazda", "CX-5", "2023", "Silver", "29000", "SUV", "4", "Turbo,Premium Interior,i-ACTIVSENSE", "1"},
                {"Nissan", "Altima", "2023", "Black", "27000", "Sedan", "5", "ProPILOT Assist,Remote Start,Bose Audio", "1"}
        };

        for (String[] car : cars) {
            stmt.setString(1, car[0]);
            stmt.setString(2, car[1]);
            stmt.setInt(3, Integer.parseInt(car[2]));
            stmt.setString(4, car[3]);
            stmt.setDouble(5, Double.parseDouble(car[4]));
            stmt.setString(6, car[5]);
            stmt.setInt(7, Integer.parseInt(car[6]));
            stmt.setString(8, car[7]);
            stmt.setBoolean(9, car[8].equals("1"));
            stmt.executeUpdate();
        }
    }

    private void seedEmployees() throws SQLException {
        String sql = "INSERT INTO employees (employee_id, name, email, phone, address, department, designation, salary, joining_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        String[][] employees = {
                {"EMP001", "John Smith", "john@showroom.com", "555-0101", "123 Main St", "Sales", "Sales Executive", "45000", "2022-01-15"},
                {"EMP002", "Sarah Johnson", "sarah@showroom.com", "555-0102", "456 Oak Ave", "Sales", "Sales Executive", "48000", "2022-03-20"},
                {"EMP003", "Mike Brown", "mike@showroom.com", "555-0103", "789 Pine Rd", "Service", "Technician", "42000", "2021-11-10"},
                {"EMP004", "Emily Davis", "emily@showroom.com", "555-0104", "321 Elm St", "Finance", "Finance Officer", "52000", "2023-02-01"},
                {"EMP005", "Robert Wilson", "robert@showroom.com", "555-0105", "654 Maple Dr", "Sales", "Sales Executive", "46000", "2022-07-15"}
        };

        for (String[] emp : employees) {
            stmt.setString(1, emp[0]); stmt.setString(2, emp[1]); stmt.setString(3, emp[2]);
            stmt.setString(4, emp[3]); stmt.setString(5, emp[4]); stmt.setString(6, emp[5]);
            stmt.setString(7, emp[6]); stmt.setDouble(8, Double.parseDouble(emp[7]));
            stmt.setDate(9, java.sql.Date.valueOf(emp[8]));
            stmt.executeUpdate();
        }
    }

    private void seedManagers() throws SQLException {
        String sql = "INSERT INTO managers (employee_id, name, email, phone, address, department, designation, salary, joining_date, region, bonus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        String[][] managers = {
                {"MGR001", "David Anderson", "david@showroom.com", "555-0201", "100 Executive Blvd", "Sales", "Sales Manager", "75000", "2020-05-10", "North Region", "15000"},
                {"MGR002", "Jennifer Lee", "jennifer@showroom.com", "555-0202", "200 Corporate Dr", "Operations", "Operations Manager", "80000", "2019-08-15", "Central Region", "18000"}
        };

        for (String[] mgr : managers) {
            stmt.setString(1, mgr[0]); stmt.setString(2, mgr[1]); stmt.setString(3, mgr[2]);
            stmt.setString(4, mgr[3]); stmt.setString(5, mgr[4]); stmt.setString(6, mgr[5]);
            stmt.setString(7, mgr[6]); stmt.setDouble(8, Double.parseDouble(mgr[7]));
            stmt.setDate(9, java.sql.Date.valueOf(mgr[8]));
            stmt.setString(10, mgr[9]); stmt.setDouble(11, Double.parseDouble(mgr[10]));
            stmt.executeUpdate();
        }
    }

    private void seedCustomers() throws SQLException {
        String sql = "INSERT INTO customers (customer_id, name, email, phone, address, registration_date, preferred_car_type, budget) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        String[][] customers = {
                {"CUST001", "Alice Cooper", "alice@email.com", "555-1001", "10 Customer Lane", "2024-01-10", "Sedan", "35000"},
                {"CUST002", "Bob Martinez", "bob@email.com", "555-1002", "20 Buyer St", "2024-01-15", "SUV", "50000"},
                {"CUST003", "Carol White", "carol@email.com", "555-1003", "30 Client Ave", "2024-02-01", "Sedan", "45000"},
                {"CUST004", "Daniel Green", "daniel@email.com", "555-1004", "40 Patron Rd", "2024-02-10", "SUV", "70000"},
                {"CUST005", "Eva Black", "eva@email.com", "555-1005", "50 Consumer Blvd", "2024-02-20", "Sedan", "30000"}
        };

        for (String[] cust : customers) {
            stmt.setString(1, cust[0]); stmt.setString(2, cust[1]); stmt.setString(3, cust[2]);
            stmt.setString(4, cust[3]); stmt.setString(5, cust[4]);
            stmt.setDate(6, java.sql.Date.valueOf(cust[5]));
            stmt.setString(7, cust[6]); stmt.setDouble(8, Double.parseDouble(cust[7]));
            stmt.executeUpdate();
        }
    }

    private void seedDealerships() throws SQLException {
        String sql = "INSERT INTO dealerships (name, address, city, latitude, longitude, opening_time, closing_time, phone, rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        String[][] dealerships = {
                {"Premium Auto Mall", "500 Auto Plaza Dr", "Downtown", "40.7128", "-74.0060", "09:00 AM", "08:00 PM", "555-2001", "4.5"},
                {"Elite Motors", "600 Car Center Rd", "Midtown", "40.7589", "-73.9851", "08:30 AM", "09:00 PM", "555-2002", "4.7"},
                {"Luxury Auto Group", "700 Premium Blvd", "Uptown", "40.7829", "-73.9654", "09:00 AM", "07:00 PM", "555-2003", "4.3"},
                {"City Car Showroom", "800 Metro Ave", "Central", "40.7484", "-73.9857", "10:00 AM", "08:00 PM", "555-2004", "4.6"},
                {"Highway Auto Sales", "900 Express Way", "Eastside", "40.7614", "-73.9776", "09:30 AM", "07:30 PM", "555-2005", "4.4"}
        };

        for (String[] dealer : dealerships) {
            stmt.setString(1, dealer[0]); stmt.setString(2, dealer[1]); stmt.setString(3, dealer[2]);
            stmt.setDouble(4, Double.parseDouble(dealer[3])); stmt.setDouble(5, Double.parseDouble(dealer[4]));
            stmt.setString(6, dealer[5]); stmt.setString(7, dealer[6]);
            stmt.setString(8, dealer[7]); stmt.setDouble(9, Double.parseDouble(dealer[8]));
            stmt.executeUpdate();
        }
    }

    private void seedReviews() throws SQLException {
        String sql = "INSERT INTO reviews (car_brand, car_model, author, platform, content, rating, review_date, source_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        String[][] reviews = {
                {"Toyota","Camry","user_auto123","Reddit","Best sedan for the price! Extremely reliable and great fuel economy.","5","2024-01-05","reddit.com/r/cars"},
                {"Honda","Accord","car_enthusiast","Twitter","Smooth ride and excellent build quality. Honda never disappoints!","5","2024-01-10","twitter.com/cars"},
                {"Ford","Explorer","suv_lover","Reddit","Perfect family SUV with tons of space. Third row is actually usable!","4","2024-01-15","reddit.com/r/suvs"},
                {"Tesla","Model 3","tech_driver","Twitter","Amazing tech and performance but service could be better.","4","2024-01-20","twitter.com/tesla"},
                {"BMW","X5","luxury_fan","Reddit","Pure luxury and driving pleasure. Worth every penny!","5","2024-02-01","reddit.com/r/bmw"},
                {"Mercedes","C-Class","mb_owner","Twitter","Elegant design and powerful engine. Best in class!","5","2024-02-05","twitter.com/mercedes"},
                {"Audi","Q7","german_cars","Reddit","Technology packed SUV with excellent handling.","4","2024-02-10","reddit.com/r/audi"},
                {"Hyundai","Tucson","value_seeker","Twitter","Great value for money with modern features.","4","2024-02-15","twitter.com/hyundai"}
        };

        for (String[] review : reviews) {
            stmt.setString(1, review[0]); stmt.setString(2, review[1]); stmt.setString(3, review[2]);
            stmt.setString(4, review[3]); stmt.setString(5, review[4]);
            stmt.setInt(6, Integer.parseInt(review[5]));
            stmt.setDate(7, java.sql.Date.valueOf(review[6]));
            stmt.setString(8, review[7]);
            stmt.executeUpdate();
        }
    }
}
