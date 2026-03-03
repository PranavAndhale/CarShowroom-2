package utils;

import models.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = ConfigManager.get("db.url", "jdbc:mysql://localhost:3306/car_showroom");
    private static final String DB_USER = ConfigManager.get("db.user", "root");
    private static final String DB_PASSWORD = ConfigManager.get("db.password", "Pranav#@77");

    private DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DB] Connected to MySQL successfully.");
        } catch (Exception e) {
            System.err.println("[DB] MySQL connection failed. Falling back to H2 in-memory database.");
            try {
                Class.forName("org.h2.Driver");
                connection = DriverManager.getConnection("jdbc:h2:mem:car_showroom;DB_CLOSE_DELAY=-1", "sa", "");
                System.out.println("[DB] Connected to H2 fallback successfully.");
            } catch (Exception ex) {
                System.err.println("[CRITICAL] Both MySQL and H2 connections failed.");
                ex.printStackTrace();
            }
        }

        if (connection != null) {
            createTables();
            try {
                // only run migration if it's MySQL, H2 doesn't like the syntax in
                // migrateExistingTables
                if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL")) {
                    migrateExistingTables();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null)
            instance = new DatabaseManager();
        return instance;
    }

    // ── CREATE TABLES (fresh install) ─────────────────────────────────────────
    private void createTables() {
        try {
            Statement stmt = connection.createStatement();

            // Cars table — now includes fuel_type, transmission, engine_size
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS cars (" +
                            "  id                INT PRIMARY KEY AUTO_INCREMENT," +
                            "  brand             VARCHAR(100)," +
                            "  model             VARCHAR(100)," +
                            "  year              INT," +
                            "  color             VARCHAR(80)," +
                            "  price             DOUBLE," +
                            "  category          VARCHAR(50)," +
                            "  stock             INT DEFAULT 0," +
                            "  fuel_type         VARCHAR(50)," +
                            "  transmission      VARCHAR(50)," +
                            "  engine_size       DOUBLE DEFAULT 0," +
                            "  features          TEXT," +
                            "  image_path        VARCHAR(500)," +
                            "  sketchfab_model_id VARCHAR(100)," +
                            "  is_available      BOOLEAN DEFAULT TRUE" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS employees (" +
                            "  id            INT PRIMARY KEY AUTO_INCREMENT," +
                            "  employee_id   VARCHAR(50) UNIQUE," +
                            "  name          VARCHAR(100)," +
                            "  email         VARCHAR(100)," +
                            "  phone         VARCHAR(20)," +
                            "  address       VARCHAR(255)," +
                            "  department    VARCHAR(100)," +
                            "  designation   VARCHAR(100)," +
                            "  salary        DOUBLE," +
                            "  joining_date  DATE" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS managers (" +
                            "  id            INT PRIMARY KEY AUTO_INCREMENT," +
                            "  employee_id   VARCHAR(50) UNIQUE," +
                            "  name          VARCHAR(100)," +
                            "  email         VARCHAR(100)," +
                            "  phone         VARCHAR(20)," +
                            "  address       VARCHAR(255)," +
                            "  department    VARCHAR(100)," +
                            "  designation   VARCHAR(100)," +
                            "  salary        DOUBLE," +
                            "  joining_date  DATE," +
                            "  region        VARCHAR(100)," +
                            "  bonus         DOUBLE" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS customers (" +
                            "  id                  INT PRIMARY KEY AUTO_INCREMENT," +
                            "  customer_id         VARCHAR(50) UNIQUE," +
                            "  name                VARCHAR(100)," +
                            "  email               VARCHAR(100)," +
                            "  phone               VARCHAR(20)," +
                            "  address             VARCHAR(255)," +
                            "  registration_date   DATE," +
                            "  preferred_car_type  VARCHAR(100)," +
                            "  budget              DOUBLE" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS sales (" +
                            "  id              INT PRIMARY KEY AUTO_INCREMENT," +
                            "  car_id          INT," +
                            "  customer_id     INT," +
                            "  employee_id     INT," +
                            "  sale_date       DATE," +
                            "  sale_price      DOUBLE," +
                            "  payment_method  VARCHAR(50)," +
                            "  status          VARCHAR(50)" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS test_drives (" +
                            "  id              INT PRIMARY KEY AUTO_INCREMENT," +
                            "  car_id          INT," +
                            "  customer_id     INT," +
                            "  scheduled_date  DATE," +
                            "  time_slot       VARCHAR(50)," +
                            "  status          VARCHAR(50)," +
                            "  feedback        TEXT" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS dealerships (" +
                            "  id            INT PRIMARY KEY AUTO_INCREMENT," +
                            "  name          VARCHAR(200)," +
                            "  address       VARCHAR(255)," +
                            "  city          VARCHAR(100)," +
                            "  latitude      DOUBLE," +
                            "  longitude     DOUBLE," +
                            "  opening_time  VARCHAR(20)," +
                            "  closing_time  VARCHAR(20)," +
                            "  phone         VARCHAR(20)," +
                            "  rating        DOUBLE" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS reviews (" +
                            "  id           INT PRIMARY KEY AUTO_INCREMENT," +
                            "  car_brand    VARCHAR(100)," +
                            "  car_model    VARCHAR(100)," +
                            "  author       VARCHAR(100)," +
                            "  platform     VARCHAR(50)," +
                            "  content      TEXT," +
                            "  rating       INT," +
                            "  review_date  DATE," +
                            "  source_url   VARCHAR(255)" +
                            ")");

            // ── Finance / EMI records ─────────────────────────────────────────
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS finance_records (" +
                            "  id              INT PRIMARY KEY AUTO_INCREMENT," +
                            "  sale_id         INT UNIQUE," +
                            "  down_payment    DOUBLE DEFAULT 0," +
                            "  loan_amount     DOUBLE DEFAULT 0," +
                            "  interest_rate   DOUBLE DEFAULT 0," +
                            "  tenure_months   INT DEFAULT 0," +
                            "  monthly_emi     DOUBLE DEFAULT 0" +
                            ")");

            // ── Commission system ─────────────────────────────────────────────
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS commission_rates (" +
                            "  employee_id   VARCHAR(50) PRIMARY KEY," +
                            "  rate_percent  DOUBLE DEFAULT 2.5" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS commissions (" +
                            "  id                 INT PRIMARY KEY AUTO_INCREMENT," +
                            "  sale_id            INT," +
                            "  employee_id        VARCHAR(50)," +
                            "  sale_amount        DOUBLE," +
                            "  commission_amount  DOUBLE," +
                            "  paid               BOOLEAN DEFAULT FALSE" +
                            ")");

            // ── Auth / Security tables ─────────────────────────────────────────
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS app_users (" +
                            "  id            INT PRIMARY KEY AUTO_INCREMENT," +
                            "  username      VARCHAR(100) UNIQUE NOT NULL," +
                            "  password_hash VARCHAR(255) NOT NULL," +
                            "  role          VARCHAR(50) NOT NULL DEFAULT \'SALESPERSON\'," +
                            "  email         VARCHAR(200)," +
                            "  phone         VARCHAR(30)," +
                            "  enabled       BOOLEAN DEFAULT TRUE," +
                            "  twofa_enabled BOOLEAN DEFAULT FALSE," +
                            "  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS audit_log (" +
                            "  id       INT PRIMARY KEY AUTO_INCREMENT," +
                            "  username VARCHAR(100)," +
                            "  action   VARCHAR(50)," +
                            "  entity   VARCHAR(200)," +
                            "  detail   TEXT," +
                            "  ts       TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")");

            // Seed default admin (admin / admin123) if no users exist
            ResultSet userCnt = stmt.executeQuery("SELECT COUNT(*) FROM app_users");
            if (userCnt.next() && userCnt.getInt(1) == 0) {
                String adminHash = sha256("admin123");
                PreparedStatement seedAdmin = connection.prepareStatement(
                        "INSERT INTO app_users (username, password_hash, role) VALUES (\'admin\',?,\'ADMIN\')");
                seedAdmin.setString(1, adminHash);
                seedAdmin.executeUpdate();
                System.out.println("[DB] Default admin created: username=admin  password=admin123");
            }

            // Migrate old role values to new granular system
            try {
                connection.createStatement().execute(
                        "UPDATE app_users SET role='SALESPERSON' WHERE role='SALES'");
                connection.createStatement().execute(
                        "UPDATE app_users SET role='FINANCE_STAFF' WHERE role='FINANCE'");
                // Also fix column type in case it's ENUM
                connection.createStatement().execute(
                        "ALTER TABLE app_users MODIFY COLUMN role VARCHAR(50) NOT NULL DEFAULT 'SALESPERSON'");
            } catch (SQLException ignored) {
                /* already VARCHAR or no old rows */ }
            if (false) { // close the if that was opened for user count
            }

        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }

        // Seed demo sales so the Reports line chart has real data to show
        seedDemoSales();
    }

    /**
     * Inserts demo sales for January 2026 (previous month) and February 2026
     * (current month) only when those months have no existing Completed sales.
     * Uses car_id=1, customer_id=1, employee_id=1 (created by the app on first
     * run).
     * Safe to call every startup — it skips insert if data already exists.
     */
    private void seedDemoSales() {
        if (connection == null)
            return;
        try {
            // Check if any cars/customers/employees exist — if not, skip (app hasn't been
            // set up yet)
            ResultSet carCheck = connection.createStatement().executeQuery("SELECT COUNT(*) FROM cars");
            carCheck.next();
            if (carCheck.getInt(1) == 0)
                return;

            // We'll pick the first available car and customer
            ResultSet carRs = connection.createStatement()
                    .executeQuery("SELECT id, price FROM cars ORDER BY id LIMIT 1");
            if (!carRs.next())
                return;
            int carId = carRs.getInt("id");
            double basePrice = carRs.getDouble("price");
            if (basePrice <= 0)
                basePrice = 45000;

            // Try to get a second car for variety
            int carId2 = carId;
            double basePrice2 = basePrice;
            ResultSet carRs2 = connection.createStatement()
                    .executeQuery("SELECT id, price FROM cars ORDER BY id LIMIT 1 OFFSET 1");
            if (carRs2.next()) {
                carId2 = carRs2.getInt("id");
                basePrice2 = carRs2.getDouble("price");
                if (basePrice2 <= 0)
                    basePrice2 = 38000;
            }

            ResultSet custRs = connection.createStatement()
                    .executeQuery("SELECT id FROM customers ORDER BY id LIMIT 1");
            int custId = custRs.next() ? custRs.getInt("id") : 0;
            if (custId == 0)
                return; // No customers yet

            ResultSet empRs = connection.createStatement().executeQuery("SELECT id FROM employees ORDER BY id LIMIT 1");
            int empId = empRs.next() ? empRs.getInt("id") : 0;
            if (empId == 0)
                return; // No employees yet

            // ── January 2026 — previous month sales ──────────────────────────
            ResultSet janCheck = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM sales WHERE status='Completed' AND sale_date BETWEEN '2026-01-01' AND '2026-01-31'");
            janCheck.next();
            if (janCheck.getInt(1) == 0) {
                PreparedStatement ins = connection.prepareStatement(
                        "INSERT INTO sales (car_id,customer_id,employee_id,sale_date,sale_price,payment_method,status) VALUES (?,?,?,?,?,?,?)");
                String[][] janSales = {
                        { "01", String.valueOf((long) (basePrice * 0.98)), "Bank Transfer" },
                        { "03", String.valueOf((long) (basePrice2 * 1.02)), "Credit Card" },
                        { "05", String.valueOf((long) (basePrice * 1.01)), "Cash" },
                        { "08", String.valueOf((long) (basePrice2 * 0.97)), "Bank Transfer" },
                        { "10", String.valueOf((long) (basePrice * 0.99)), "Credit Card" },
                        { "12", String.valueOf((long) (basePrice2 * 1.03)), "Cash" },
                        { "15", String.valueOf((long) (basePrice * 1.00)), "Bank Transfer" },
                        { "18", String.valueOf((long) (basePrice2 * 0.96)), "Credit Card" },
                        { "22", String.valueOf((long) (basePrice * 1.02)), "Cash" },
                        { "28", String.valueOf((long) (basePrice2 * 0.98)), "Bank Transfer" },
                };
                for (String[] row : janSales) {
                    ins.setInt(1, Integer.parseInt(row[0]) % 2 == 0 ? carId2 : carId);
                    ins.setInt(2, custId);
                    ins.setInt(3, empId);
                    ins.setDate(4, java.sql.Date.valueOf("2026-01-" + row[0]));
                    ins.setDouble(5, Double.parseDouble(row[1]));
                    ins.setString(6, row[2]);
                    ins.setString(7, "Completed");
                    ins.addBatch();
                }
                ins.executeBatch();
                System.out.println("[Demo] Seeded 10 January 2026 sales.");
            }

            // ── February 2026 — current month sales ──────────────────────────
            ResultSet febCheck = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM sales WHERE status='Completed' AND sale_date BETWEEN '2026-02-01' AND '2026-02-28'");
            febCheck.next();
            if (febCheck.getInt(1) == 0) {
                PreparedStatement ins2 = connection.prepareStatement(
                        "INSERT INTO sales (car_id,customer_id,employee_id,sale_date,sale_price,payment_method,status) VALUES (?,?,?,?,?,?,?)");
                String[][] febSales = {
                        { "02", String.valueOf((long) (basePrice * 1.01)), "Cash" },
                        { "07", String.valueOf((long) (basePrice2 * 0.99)), "Credit Card" },
                        { "12", String.valueOf((long) (basePrice * 1.03)), "Bank Transfer" },
                        { "17", String.valueOf((long) (basePrice2 * 0.98)), "Cash" },
                        { "22", String.valueOf((long) (basePrice * 0.97)), "Credit Card" },
                        { "27", String.valueOf((long) (basePrice2 * 1.02)), "Bank Transfer" },
                };
                for (String[] row : febSales) {
                    ins2.setInt(1, Integer.parseInt(row[0]) % 2 == 0 ? carId2 : carId);
                    ins2.setInt(2, custId);
                    ins2.setInt(3, empId);
                    ins2.setDate(4, java.sql.Date.valueOf("2026-02-" + row[0]));
                    ins2.setDouble(5, Double.parseDouble(row[1]));
                    ins2.setString(6, row[2]);
                    ins2.setString(7, "Completed");
                    ins2.addBatch();
                }
                ins2.executeBatch();
                System.out.println("[Demo] Seeded 6 February 2026 sales.");
            }
        } catch (SQLException e) {
            System.err.println("[Demo] Could not seed demo sales: " + e.getMessage());
        }
    }

    // ── MIGRATE EXISTING TABLES (safe ALTER TABLE — adds missing columns) ──────
    // MySQL silently errors on "ADD COLUMN IF NOT EXISTS" in older versions, so
    // we check the information_schema first.
    private void migrateExistingTables() {
        try {
            addColumnIfMissing("cars", "fuel_type", "VARCHAR(50)    DEFAULT NULL");
            addColumnIfMissing("cars", "transmission", "VARCHAR(50)    DEFAULT NULL");
            addColumnIfMissing("cars", "engine_size", "DOUBLE         DEFAULT 0");
            addColumnIfMissing("cars", "image_path", "VARCHAR(500)   DEFAULT NULL");
            addColumnIfMissing("cars", "sketchfab_model_id", "VARCHAR(100)   DEFAULT NULL");
        } catch (Exception e) {
            System.err.println("Migration warning: " + e.getMessage());
        }
    }

    /**
     * Adds a column to a table only if it doesn't already exist.
     * Uses information_schema so it works on MySQL 5.x and 8.x.
     */
    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            PreparedStatement check = connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "  AND TABLE_NAME   = ? " +
                            "  AND COLUMN_NAME  = ?");
            check.setString(1, table);
            check.setString(2, column);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                connection.createStatement().execute(
                        "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                System.out.println("[DB] Added missing column: " + table + "." + column);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Could not add column " + column + ": " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /** Returns true if the database connection is open and valid. */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // ── Security helpers ──────────────────────────────────────────────────────

    /** SHA-256 hex hash of a plain-text string. */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input; // fallback (never happens with SHA-256)
        }
    }

    /**
     * Validates username+password; returns the user's role string ("ADMIN",
     * "SALES", "FINANCE") on success, or null on failure / account disabled.
     */
    public String authenticate(String username, String password) {
        if (connection == null)
            return null;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT role, enabled FROM app_users WHERE username=? AND password_hash=?");
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getBoolean("enabled"))
                return rs.getString("role");
        } catch (SQLException e) {
            System.err.println("[Auth] " + e.getMessage());
        }
        return null;
    }

    /** Returns the stored phone for a username (for 2FA SMS), or null. */
    public String getUserPhone(String username) {
        if (connection == null)
            return null;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT phone, twofa_enabled FROM app_users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getBoolean("twofa_enabled"))
                return rs.getString("phone");
        } catch (SQLException e) {
            /* ignore */ }
        return null;
    }

    /** Returns whether 2FA is enabled for the user. */
    public boolean is2faEnabled(String username) {
        if (connection == null)
            return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT twofa_enabled FROM app_users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getBoolean("twofa_enabled");
        } catch (SQLException e) {
            /* ignore */ }
        return false;
    }

    /**
     * Returns list of all users as String[]: [username, role, email, phone,
     * enabled, twofa].
     */
    public List<String[]> getAllAppUsers() {
        List<String[]> list = new ArrayList<>();
        if (connection == null)
            return list;
        try {
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT username, role, COALESCE(email,'') as email, COALESCE(phone,'') as phone," +
                            "enabled, twofa_enabled FROM app_users ORDER BY id");
            while (rs.next())
                list.add(new String[] {
                        rs.getString("username"), rs.getString("role"),
                        rs.getString("email"), rs.getString("phone"),
                        rs.getBoolean("enabled") ? "Yes" : "No",
                        rs.getBoolean("twofa_enabled") ? "On" : "Off"
                });
        } catch (SQLException e) {
            System.err.println("[Users] " + e.getMessage());
        }
        return list;
    }

    /** Creates a new app user. Returns false if username already taken. */
    public boolean addAppUser(String username, String password, String role, String email, String phone) {
        if (connection == null)
            return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO app_users (username, password_hash, role, email, phone) VALUES (?,?,?,?,?)");
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            ps.setString(3, role);
            ps.setString(4, email);
            ps.setString(5, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AddUser] " + e.getMessage());
            return false;
        }
    }

    /** Deletes an app user by username. Refuses to delete the last admin. */
    public boolean deleteAppUser(String username) {
        if (connection == null)
            return false;
        try {
            // Safety: don't delete the last ADMIN
            ResultSet chk = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM app_users WHERE role='ADMIN' AND enabled=TRUE");
            if (chk.next() && chk.getInt(1) <= 1) {
                ResultSet isAdmin = connection.prepareStatement(
                        "SELECT role FROM app_users WHERE username='" + username + "'").executeQuery();
                if (isAdmin.next() && "ADMIN".equals(isAdmin.getString(1)))
                    return false;
            }
            PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM app_users WHERE username=?");
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Resets a user's password. */
    public boolean resetAppUserPassword(String username, String newPassword) {
        if (connection == null)
            return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE app_users SET password_hash=? WHERE username=?");
            ps.setString(1, sha256(newPassword));
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Toggles the enabled flag for a user. */
    public boolean toggleAppUser(String username, boolean enabled) {
        if (connection == null)
            return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE app_users SET enabled=? WHERE username=?");
            ps.setBoolean(1, enabled);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Updates 2FA settings (phone + enabled flag) for a user. */
    public boolean update2fa(String username, boolean enabled, String phone) {
        if (connection == null)
            return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE app_users SET twofa_enabled=?, phone=? WHERE username=?");
            ps.setBoolean(1, enabled);
            ps.setString(2, phone);
            ps.setString(3, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Returns recent audit log entries (newest first). */
    public List<String[]> getAuditLog(int limit) {
        List<String[]> list = new ArrayList<>();
        if (connection == null)
            return list;
        try {
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT ts, username, action, entity, COALESCE(detail,'') as detail " +
                            "FROM audit_log ORDER BY id DESC LIMIT " + limit);
            while (rs.next())
                list.add(new String[] {
                        rs.getString("ts"), rs.getString("username"),
                        rs.getString("action"), rs.getString("entity"), rs.getString("detail")
                });
        } catch (SQLException e) {
            System.err.println("[Audit] " + e.getMessage());
        }
        return list;
    }
}
