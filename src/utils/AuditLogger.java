package utils;

import java.sql.*;

/**
 * Inserts an audit entry asynchronously (daemon thread) so the UI never blocks.
 * Usage: AuditLogger.log("CREATE", "Customer CUST001", "Name=John Doe");
 */
public class AuditLogger {

    private AuditLogger() {
    }

    public static void log(String action, String entity, String detail) {
        String user = SessionManager.getUser();
        if (user == null)
            user = "system";
        final String u = user;
        Thread t = new Thread(() -> {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                if (conn == null)
                    return;
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO audit_log (username, action, entity, detail) VALUES (?,?,?,?)");
                ps.setString(1, u);
                ps.setString(2, action);
                ps.setString(3, entity);
                ps.setString(4, detail);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Audit] Log error: " + e.getMessage());
            }
        }, "audit-logger");
        t.setDaemon(true);
        t.start();
    }
}
