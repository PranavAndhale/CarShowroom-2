package utils;

/**
 * Holds the currently logged-in user's identity and role.
 *
 * Role hierarchy (lowest → highest access):
 * SALESPERSON – can add sales/customers/test drives
 * SALES_MANAGER – salesperson + team visibility, commissions
 * FINANCE_STAFF – read-only reports & commissions
 * FINANCE_MANAGER – finance_staff + full commissions, all reports
 * ADMIN – unrestricted access
 */
public class SessionManager {

    public enum Role {
        SALESPERSON,
        SALES_MANAGER,
        FINANCE_STAFF,
        FINANCE_MANAGER,
        ADMIN;

        /** Human-readable label shown in UI. */
        public String label() {
            return switch (this) {
                case SALESPERSON -> "Salesperson";
                case SALES_MANAGER -> "Sales Manager";
                case FINANCE_STAFF -> "Finance Staff";
                case FINANCE_MANAGER -> "Finance Manager";
                case ADMIN -> "Admin";
            };
        }

        /** All role DB strings in order for combobox. */
        public static final String[] DB_VALUES = {
                "SALESPERSON", "SALES_MANAGER", "FINANCE_STAFF", "FINANCE_MANAGER", "ADMIN"
        };

        /** Labels matching DB_VALUES order. */
        public static final String[] LABELS = {
                "Salesperson", "Sales Manager", "Finance Staff", "Finance Manager", "Admin"
        };

        public static Role fromDb(String dbValue) {
            try {
                return Role.valueOf(dbValue);
            }
            // Legacy mapping for old 2-role system
            catch (IllegalArgumentException e) {
                return switch (dbValue) {
                    case "SALES" -> SALESPERSON;
                    case "FINANCE" -> FINANCE_STAFF;
                    default -> SALESPERSON;
                };
            }
        }
    }

    private static String currentUser = null;
    private static Role currentRole = null;

    private SessionManager() {
    }

    public static void login(String username, Role role) {
        currentUser = username;
        currentRole = role;
    }

    public static void logout() {
        currentUser = null;
        currentRole = null;
    }

    public static String getUser() {
        return currentUser;
    }

    public static Role getRole() {
        return currentRole;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return Role.ADMIN.equals(currentRole);
    }

    public static boolean isSalesManager() {
        return Role.SALES_MANAGER.equals(currentRole);
    }

    public static boolean isSalesperson() {
        return Role.SALESPERSON.equals(currentRole);
    }

    public static boolean isFinanceManager() {
        return Role.FINANCE_MANAGER.equals(currentRole);
    }

    public static boolean isFinanceStaff() {
        return Role.FINANCE_STAFF.equals(currentRole);
    }

    /**
     * Panel access matrix.
     *
     * | Panel | ADMIN | SALES_MGR | SALESPERSON | FIN_MGR | FIN_STAFF |
     * |--------------|-------|-----------|-------------|---------|-----------|
     * | DASHBOARD | ✓ | ✓ | ✓ | ✓ | ✓ |
     * | CARS | ✓ | ✓ | ✓ | ✓ | |
     * | CUSTOMERS | ✓ | ✓ | ✓ | | |
     * | EMPLOYEES | ✓ | ✓ | | | |
     * | SALES | ✓ | ✓ | ✓ | ✓ | |
     * | TESTDRIVES | ✓ | ✓ | ✓ | | |
     * | COMMISSIONS | ✓ | ✓ | | ✓ | ✓ |
     * | DEALERSHIPS | ✓ | ✓ | | | |
     * | REVIEWS | ✓ | ✓ | ✓ | | |
     * | REPORTS | ✓ | ✓ | | ✓ | ✓ |
     * | AUDIT | ✓ | | | | |
     * | SETTINGS | ✓ | | | | |
     */
    public static boolean canView(String panelKey) {
        if (currentRole == null || currentRole == Role.ADMIN)
            return true;

        return switch (panelKey) {
            case "DASHBOARD" -> true; // everyone sees dashboard
            case "CARS" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.SALESPERSON
                    || currentRole == Role.FINANCE_MANAGER;
            case "CUSTOMERS" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.SALESPERSON;
            case "EMPLOYEES" -> currentRole == Role.SALES_MANAGER;
            case "SALES" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.SALESPERSON
                    || currentRole == Role.FINANCE_MANAGER;
            case "TESTDRIVES" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.SALESPERSON;
            case "COMMISSIONS" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.FINANCE_MANAGER
                    || currentRole == Role.FINANCE_STAFF;
            case "DEALERSHIPS" -> currentRole == Role.SALES_MANAGER;
            case "REVIEWS" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.SALESPERSON;
            case "REPORTS" -> currentRole == Role.SALES_MANAGER
                    || currentRole == Role.FINANCE_MANAGER
                    || currentRole == Role.FINANCE_STAFF;
            case "AUDIT" -> false; // Admin only
            case "SETTINGS" -> false; // Admin only
            default -> false;
        };
    }
}
