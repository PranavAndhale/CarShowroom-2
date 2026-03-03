package controllers;

import utils.DatabaseManager;
import java.sql.*;
import java.util.*;

/**
 * Manages commission rates per employee and commission entries per sale.
 *
 * Tables used:
 * commission_rates (employee_id VARCHAR, rate_percent DOUBLE)
 * commissions (id, sale_id, employee_id, sale_amount, commission_amount, paid,
 * created_at)
 */
public class CommissionController {

    private final Connection conn;

    public CommissionController() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── Rate Management ───────────────────────────────────────────────────────

    /** Returns the commission rate (%) for an employee, default 2.5%. */
    public double getRate(String employeeId) {
        if (conn == null)
            return 2.5;
        try {
            // Try direct match first (string employee_id like EMP001)
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT rate_percent FROM commission_rates WHERE employee_id=?");
            ps.setString(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getDouble("rate_percent");
            // Fallback: the stored key might be the numeric id — resolve to string and
            // retry
            try {
                PreparedStatement resolve = conn.prepareStatement(
                        "SELECT employee_id FROM employees WHERE id=?");
                resolve.setInt(1, Integer.parseInt(employeeId));
                ResultSet rr = resolve.executeQuery();
                if (rr.next()) {
                    String resolved = rr.getString(1);
                    ps.setString(1, resolved);
                    ResultSet rs2 = ps.executeQuery();
                    if (rs2.next())
                        return rs2.getDouble("rate_percent");
                }
            } catch (NumberFormatException ignored) {
            }
        } catch (SQLException e) {
            System.err.println("Commission rate error: " + e.getMessage());
        }
        return 2.5;
    }

    /** Upsert an employee's commission rate. */
    public boolean setRate(String employeeId, double rate) {
        if (conn == null)
            return false;
        try {
            // Try update first
            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE commission_rates SET rate_percent=? WHERE employee_id=?");
            upd.setDouble(1, rate);
            upd.setString(2, employeeId);
            if (upd.executeUpdate() == 0) {
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO commission_rates (employee_id, rate_percent) VALUES (?,?)");
                ins.setString(1, employeeId);
                ins.setDouble(2, rate);
                ins.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Set rate error: " + e.getMessage());
            return false;
        }
    }

    // ── Commission Entries ─────────────────────────────────────────────────────

    /**
     * Creates a commission entry for a completed sale.
     * Should be called when a sale status is set to "Completed".
     */
    public boolean recordCommission(int saleId, String employeeId, double saleAmount) {
        if (conn == null)
            return false;
        try {
            // Check if already recorded
            PreparedStatement chk = conn.prepareStatement(
                    "SELECT id FROM commissions WHERE sale_id=? AND employee_id=?");
            chk.setInt(1, saleId);
            chk.setString(2, employeeId);
            if (chk.executeQuery().next())
                return true; // idempotent

            double rate = getRate(employeeId);
            double amount = saleAmount * rate / 100.0;
            PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO commissions (sale_id, employee_id, sale_amount, commission_amount, paid) VALUES (?,?,?,?,FALSE)");
            ins.setInt(1, saleId);
            ins.setString(2, employeeId);
            ins.setDouble(3, saleAmount);
            ins.setDouble(4, amount);
            return ins.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Record commission error: " + e.getMessage());
            return false;
        }
    }

    /** Marks a commission entry as paid. */
    public boolean markPaid(int commissionId) {
        if (conn == null)
            return false;
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE commissions SET paid=TRUE WHERE id=?");
            ps.setInt(1, commissionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Mark paid error: " + e.getMessage());
            return false;
        }
    }

    // ── Summary Queries ───────────────────────────────────────────────────────

    /**
     * Returns all commission rows joined with employee names.
     * Columns: [id, employee_id, emp_name, sale_id, sale_amount, commission_amount,
     * rate%, paid]
     */
    public List<String[]> getAllCommissions() {
        List<String[]> list = new ArrayList<>();
        if (conn == null)
            return list;
        try {
            String sql = "SELECT c.id, c.employee_id, COALESCE(e.name,'Unknown') as emp_name, " +
                    "c.sale_id, c.sale_amount, c.commission_amount, " +
                    "ROUND(c.commission_amount*100.0/NULLIF(c.sale_amount,0),2) as rate_pct, " +
                    "c.paid " +
                    "FROM commissions c LEFT JOIN employees e " +
                    "  ON c.employee_id=e.employee_id OR CAST(e.id AS CHAR)=c.employee_id " +
                    "ORDER BY c.id DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(new String[] {
                        String.valueOf(rs.getInt("id")),
                        rs.getString("employee_id"),
                        rs.getString("emp_name"),
                        String.valueOf(rs.getInt("sale_id")),
                        String.format("$%,.0f", rs.getDouble("sale_amount")),
                        String.format("$%,.0f", rs.getDouble("commission_amount")),
                        rs.getDouble("rate_pct") + "%",
                        rs.getBoolean("paid") ? "✅ Paid" : "⏳ Pending"
                });
            }
        } catch (SQLException e) {
            System.err.println("getAllCommissions error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Per-employee earning summary.
     * Columns: [employee_id, name, sales_count, total_sales, total_commission,
     * unpaid_commission]
     */
    public List<String[]> getEmployeeSummary() {
        List<String[]> list = new ArrayList<>();
        if (conn == null)
            return list;
        try {
            String sql = "SELECT COALESCE(e.employee_id, c.employee_id) as eid, " +
                    "COALESCE(e.name,'Unknown') as nm, " +
                    "COUNT(*) as cnt, SUM(c.sale_amount) as ts, SUM(c.commission_amount) as tc, " +
                    "SUM(CASE WHEN c.paid=FALSE THEN c.commission_amount ELSE 0 END) as unpaid " +
                    "FROM commissions c LEFT JOIN employees e " +
                    "  ON c.employee_id=e.employee_id OR CAST(e.id AS CHAR)=c.employee_id " +
                    "GROUP BY eid, nm ORDER BY tc DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(new String[] {
                        rs.getString("eid"),
                        rs.getString("nm"),
                        String.valueOf(rs.getInt("cnt")),
                        String.format("$%,.0f", rs.getDouble("ts")),
                        String.format("$%,.0f", rs.getDouble("tc")),
                        String.format("$%,.0f", rs.getDouble("unpaid"))
                });
            }
        } catch (SQLException e) {
            System.err.println("Employee summary error: " + e.getMessage());
        }
        return list;
    }

    /** Returns total unpaid commission amount across all employees. */
    public double getTotalUnpaid() {
        if (conn == null)
            return 0;
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT SUM(commission_amount) FROM commissions WHERE paid=FALSE");
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Total unpaid error: " + e.getMessage());
        }
        return 0;
    }

    /** Returns the top earner's name and total commission. */
    public String[] getTopEarner() {
        if (conn == null)
            return new String[] { "N/A", "$0" };
        try {
            String sql = "SELECT COALESCE(e.name,'Unknown') as nm, SUM(c.commission_amount) as tc " +
                    "FROM commissions c LEFT JOIN employees e " +
                    "  ON c.employee_id=e.employee_id OR CAST(e.id AS CHAR)=c.employee_id " +
                    "GROUP BY c.employee_id, nm ORDER BY tc DESC LIMIT 1";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next())
                return new String[] { rs.getString("nm"),
                        String.format("$%,.0f", rs.getDouble("tc")) };
        } catch (SQLException e) {
            System.err.println("Top earner error: " + e.getMessage());
        }
        return new String[] { "N/A", "$0" };
    }
}
