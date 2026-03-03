package controllers;

import models.Sale;
import utils.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleController {
    private Connection connection;

    public SaleController() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public List<Sale> getAllSales() {
        List<Sale> list = new ArrayList<>();
        if (connection == null)
            return list;
        try {
            String sql = "SELECT s.*, " +
                    "CONCAT(c_car.brand, ' ', c_car.model) as car_name, " +
                    "cust.name as customer_name, emp.name as employee_name " +
                    "FROM sales s " +
                    "LEFT JOIN cars c_car ON s.car_id = c_car.id " +
                    "LEFT JOIN customers cust ON s.customer_id = cust.id " +
                    "LEFT JOIN employees emp ON s.employee_id = emp.id " +
                    "ORDER BY s.sale_date DESC";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                Sale sale = new Sale();
                sale.setId(rs.getInt("id"));
                sale.setCarId(rs.getInt("car_id"));
                sale.setCustomerId(rs.getInt("customer_id"));
                sale.setEmployeeId(rs.getInt("employee_id"));
                sale.setSaleDate(rs.getDate("sale_date"));
                sale.setSalePrice(rs.getDouble("sale_price"));
                sale.setPaymentMethod(rs.getString("payment_method"));
                sale.setStatus(rs.getString("status"));
                try {
                    sale.setCarName(rs.getString("car_name"));
                } catch (Exception ignored) {
                }
                try {
                    sale.setCustomerName(rs.getString("customer_name"));
                } catch (Exception ignored) {
                }
                try {
                    sale.setEmployeeName(rs.getString("employee_name"));
                } catch (Exception ignored) {
                }
                list.add(sale);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addSale(Sale sale) {
        if (connection == null)
            return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO sales (car_id, customer_id, employee_id, sale_date, sale_price, payment_method, status) VALUES (?, ?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, sale.getCarId());
            stmt.setInt(2, sale.getCustomerId());
            stmt.setInt(3, sale.getEmployeeId());
            stmt.setDate(4, new java.sql.Date(sale.getSaleDate().getTime()));
            stmt.setDouble(5, sale.getSalePrice());
            stmt.setString(6, sale.getPaymentMethod());
            stmt.setString(7, sale.getStatus());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateSaleStatus(int saleId, String status) {
        if (connection == null)
            return false;
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE sales SET status=? WHERE id=?");
            stmt.setString(1, status);
            stmt.setInt(2, saleId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    public double getTotalRevenue() {
        if (connection == null)
            return 0;
        try {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT SUM(sale_price) FROM sales WHERE status='Completed'");
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTotalSales() {
        if (connection == null)
            return 0;
        try {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM sales WHERE status='Completed'");
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String[]> getMonthlySummary() {
        List<String[]> list = new ArrayList<>();
        if (connection == null)
            return list;
        try {
            String sql = "SELECT DATE_FORMAT(sale_date,'%Y-%m') as month, COUNT(*) as cnt, SUM(sale_price) as total " +
                    "FROM sales WHERE status='Completed' GROUP BY month ORDER BY month DESC LIMIT 12";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(new String[] { rs.getString("month"), String.valueOf(rs.getInt("cnt")),
                        "$" + String.format("%,.0f", rs.getDouble("total")) });
            }
        } catch (SQLException e) {
            // H2 fallback
            try {
                String sql2 = "SELECT FORMATDATETIME(sale_date,'yyyy-MM') as month, COUNT(*) as cnt, SUM(sale_price) as total "
                        +
                        "FROM sales WHERE status='Completed' GROUP BY month ORDER BY month DESC LIMIT 12";
                ResultSet rs = connection.createStatement().executeQuery(sql2);
                while (rs.next()) {
                    list.add(new String[] { rs.getString("month"), String.valueOf(rs.getInt("cnt")),
                            "$" + String.format("%,.0f", rs.getDouble("total")) });
                }
            } catch (SQLException ex) {
                System.err.println("Monthly summary error: " + ex.getMessage());
            }
        }
        return list;
    }

    public List<String[]> getSalesByCategory() {
        List<String[]> list = new ArrayList<>();
        if (connection == null)
            return list;
        try {
            String sql = "SELECT c.category, COUNT(*) as cnt, SUM(s.sale_price) as total " +
                    "FROM sales s JOIN cars c ON s.car_id = c.id " +
                    "WHERE s.status='Completed' GROUP BY c.category ORDER BY total DESC";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(new String[] { rs.getString("category"), String.valueOf(rs.getInt("cnt")),
                        "$" + String.format("%,.0f", rs.getDouble("total")) });
            }
        } catch (SQLException e) {
            System.err.println("Category sales error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Quarterly summary — returns rows of { "YYYY-Qn", count, "$total" }
     * ordered oldest→newest. Works on both MySQL and H2.
     */
    public List<String[]> getQuarterlySummary() {
        List<String[]> list = new ArrayList<>();
        if (connection == null)
            return list;
        // Attempt MySQL QUARTER() function first, fall back to manual derivation
        try {
            String sql = "SELECT CONCAT(YEAR(sale_date),'-Q',QUARTER(sale_date)) as qtr, " +
                    "COUNT(*) as cnt, SUM(sale_price) as total " +
                    "FROM sales WHERE status='Completed' GROUP BY qtr ORDER BY qtr ASC LIMIT 12";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next())
                list.add(new String[] { rs.getString("qtr"), String.valueOf(rs.getInt("cnt")),
                        "$" + String.format("%,.0f", rs.getDouble("total")) });
        } catch (SQLException e) {
            // H2 fallback: derive quarter from month number
            try {
                String sql2 = "SELECT CONCAT(YEAR(sale_date),'-Q',CEILING(MONTH(sale_date)/3.0)) as qtr," +
                        "COUNT(*) as cnt, SUM(sale_price) as total " +
                        "FROM sales WHERE status='Completed' GROUP BY qtr ORDER BY qtr ASC LIMIT 12";
                ResultSet rs = connection.createStatement().executeQuery(sql2);
                while (rs.next())
                    list.add(new String[] { rs.getString("qtr"), String.valueOf(rs.getInt("cnt")),
                            "$" + String.format("%,.0f", rs.getDouble("total")) });
            } catch (SQLException ex) {
                System.err.println("Quarterly summary error: " + ex.getMessage());
            }
        }
        return list;
    }

    /**
     * Yearly summary — returns rows of { "YYYY", count, "$total" }
     * ordered oldest→newest.
     */
    public List<String[]> getYearlySummary() {
        List<String[]> list = new ArrayList<>();
        if (connection == null)
            return list;
        try {
            String sql = "SELECT YEAR(sale_date) as yr, COUNT(*) as cnt, SUM(sale_price) as total " +
                    "FROM sales WHERE status='Completed' GROUP BY yr ORDER BY yr ASC";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next())
                list.add(new String[] { String.valueOf(rs.getInt("yr")), String.valueOf(rs.getInt("cnt")),
                        "$" + String.format("%,.0f", rs.getDouble("total")) });
        } catch (SQLException e) {
            System.err.println("Yearly summary error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Returns [0]=current year quarters, [1]=previous year quarters,
     * both as LinkedHashMap keyed by "Q1","Q2","Q3","Q4".
     */
    @SuppressWarnings("unchecked")
    public java.util.LinkedHashMap<String,Long>[] getQuarterlyByYear() {
        java.util.LinkedHashMap<String,Long> cur  = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String,Long> prev = new java.util.LinkedHashMap<>();
        for (String q : new String[]{"Q1","Q2","Q3","Q4"}) { cur.put(q,0L); prev.put(q,0L); }
        if (connection == null) return new java.util.LinkedHashMap[]{cur, prev};
        int curYear  = java.time.LocalDate.now().getYear();
        int prevYear = curYear - 1;
        try {
            String sql = "SELECT YEAR(sale_date) as yr, QUARTER(sale_date) as qn, SUM(sale_price) as total " +
                         "FROM sales WHERE status='Completed' AND YEAR(sale_date) IN ("+curYear+","+prevYear+") " +
                         "GROUP BY yr, qn ORDER BY yr, qn";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            while (rs.next()) {
                int yr = rs.getInt("yr");
                String qk = "Q" + rs.getInt("qn");
                long tot = (long) rs.getDouble("total");
                if (yr == curYear) cur.put(qk, tot);
                else if (yr == prevYear) prev.put(qk, tot);
            }
        } catch (SQLException e) {
            try {
                String sql2 = "SELECT YEAR(sale_date) as yr, MONTH(sale_date) as mn, SUM(sale_price) as total " +
                              "FROM sales WHERE status='Completed' AND YEAR(sale_date) IN ("+curYear+","+prevYear+") GROUP BY yr, mn";
                ResultSet rs = connection.createStatement().executeQuery(sql2);
                while (rs.next()) {
                    int yr = rs.getInt("yr"); int mn = rs.getInt("mn");
                    String qk = "Q" + (int) Math.ceil(mn / 3.0);
                    long tot = (long) rs.getDouble("total");
                    java.util.LinkedHashMap<String,Long> m = yr == curYear ? cur : prev;
                    m.put(qk, m.getOrDefault(qk, 0L) + tot);
                }
            } catch (SQLException ex) { System.err.println("Q-by-year error: " + ex.getMessage()); }
        }
        return new java.util.LinkedHashMap[]{cur, prev};
    }
}
