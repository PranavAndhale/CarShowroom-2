package ui;

import controllers.*;
import models.*;
import utils.DatabaseManager;
import utils.DataSeeder;
import utils.SessionManager;
import utils.AuditLogger;
import utils.TwilioService;
import utils.OtpService;
import utils.BackupScheduler;
import java.io.File;
import java.time.LocalDate;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.JTextComponent;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

// ─── PDF EXPORT (iText 5 / OpenPDF) ──────────────────────────────────────────
// Add to classpath: itextpdf-5.5.13.3.jar  OR  openpdf-1.3.30.jar
// Maven:  com.itextpdf:itextpdf:5.5.13.3   or   com.github.librepdf:openpdf:1.3.30
// The code uses the same API for both; any one jar on the classpath will work.
// If neither is present the PDF buttons show a clear "library not found" message.

// ─── JFREECHART (charts in Reports panel) ─────────────────────────────────────
// Add to classpath: jfreechart-1.5.4.jar + jcommon-1.0.24.jar  (or Maven:
// org.jfree:jfreechart:1.5.4)
// Falls back to the existing table view when not present.

// ─── JAVAMAIL (email notifications) ───────────────────────────────────────────
// Add to classpath: javax.mail-1.6.2.jar  (or Jakarta Mail 2.0)
// Maven:  com.sun.mail:javax.mail:1.6.2
// Falls back gracefully when not available.

public class CarShowroomApp extends JFrame {

    // ─── COLOR PALETTE ────────────────────────────────────────────────────────
    static final Color BG_DARK = new Color(8, 10, 18);
    static final Color BG_CARD = new Color(13, 17, 30);
    static final Color BG_PANEL = new Color(16, 20, 38);
    static final Color BG_SIDEBAR = new Color(10, 13, 24);
    static final Color ACCENT_CYAN = new Color(0, 212, 255);
    static final Color ACCENT_PURPLE = new Color(138, 43, 226);
    static final Color ACCENT_PINK = new Color(255, 45, 140);
    static final Color ACCENT_GREEN = new Color(57, 255, 20);
    static final Color ACCENT_ORANGE = new Color(255, 140, 0);
    static final Color TEXT_PRIMARY = new Color(230, 240, 255);
    static final Color TEXT_MUTED = new Color(100, 120, 160);
    static final Color BORDER_GLOW = new Color(0, 212, 255, 60);
    static final Color HOVER_BG = new Color(0, 212, 255, 15);

    static Font FONT_TITLE, FONT_LABEL, FONT_SMALL, FONT_MONO, FONT_HEADER;

    // ─── CONTROLLERS ──────────────────────────────────────────────────────────
    private CarController carController;
    private CustomerController customerController;
    private EmployeeController employeeController;
    private SaleController saleController;
    private TestDriveController testDriveController;
    private DealershipController dealershipController;
    private ReviewController reviewController;
    private controllers.CommissionController commissionController;

    // ─── UI STATE ─────────────────────────────────────────────────────────────
    private JPanel contentArea;
    private CardLayout cardLayout;
    private String currentPanel = "DASHBOARD";
    private Map<String, JButton> navButtons = new LinkedHashMap<>();
    private JLabel statusLabel;
    private JLabel clockLabel;
    private boolean dbConnected;

    // ─── CROSS-FADE ENGINE ────────────────────────────────────────────────────
    private JLayeredPane contentLayered;
    private JPanel fadeOverlayPanel;
    private BufferedImage fadeSnapshot;
    private float fadeAlpha = 0f;
    private javax.swing.Timer fadeTimer = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            // Initialize DB first so app_users table exists before login dialog
            utils.DatabaseManager.getInstance();
            if (showLoginDialog()) {
                new CarShowroomApp().setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }

    /**
     * Shows the AutoElite login dialog. Returns true if the user authenticated
     * successfully; false if they closed / cancelled.
     */

    /**
     * Static dark-themed input dialog for use in static contexts (e.g. login flow).
     */
    static String showDarkInputDialog(java.awt.Dialog parent, String title, String prompt, String placeholder) {
        final String[] result = { null };
        JDialog dlg = new JDialog(parent, title, true);
        dlg.setUndecorated(true);
        dlg.setSize(450, 250);
        dlg.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(
                        new java.awt.GradientPaint(0, 0, new Color(8, 12, 24), 0, getHeight(), new Color(12, 8, 28)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createLineBorder(new Color(0, 212, 255, 100), 1));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(new Color(0, 212, 255));
        titleLbl.setBorder(new EmptyBorder(14, 18, 8, 18));
        root.add(titleLbl, BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setOpaque(false);
        mid.setBorder(new EmptyBorder(0, 18, 10, 18));
        JLabel promptLbl = new JLabel("<html>" + prompt.replace("\n", "<br/>") + "</html>");
        promptLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        promptLbl.setForeground(new Color(160, 180, 220));

        JTextField tf = new JTextField(20);
        tf.setBackground(new Color(16, 22, 44));
        tf.setForeground(new Color(230, 240, 255));
        tf.setCaretColor(new Color(0, 212, 255));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 212, 255, 80), 1),
                new EmptyBorder(8, 12, 8, 12)));

        mid.add(promptLbl, BorderLayout.NORTH);
        mid.add(tf, BorderLayout.CENTER);
        root.add(mid, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        foot.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        cancel.setForeground(new Color(100, 130, 160));
        cancel.setBackground(new Color(20, 28, 50));
        cancel.setBorderPainted(false);
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dlg.dispose());

        JButton ok = new JButton("Verify");
        ok.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ok.setForeground(Color.WHITE);
        ok.setBackground(new Color(0, 160, 200));
        ok.setBorderPainted(false);
        ok.setFocusPainted(false);
        ok.addActionListener(e -> {
            result[0] = tf.getText();
            dlg.dispose();
        });
        tf.addActionListener(e -> {
            result[0] = tf.getText();
            dlg.dispose();
        });

        foot.add(cancel);
        foot.add(ok);
        root.add(foot, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.getRootPane().setDefaultButton(ok);
        dlg.setVisible(true);
        return result[0];
    }

    static boolean showLoginDialog() {
        JDialog dlg = new JDialog((java.awt.Frame) null, "AutoElite — Login", true);
        dlg.setUndecorated(true);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(screenSize.width, screenSize.height);
        dlg.setLocation(0, 0);

        // dlg.setLocationRelativeTo(null);
        dlg.setResizable(false);

        final boolean[] ok = { false };
        final int[] attempts = { 0 };

        // ── Dark background panel ─────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(
                        new java.awt.GradientPaint(0, 0, new Color(8, 10, 18), 0, getHeight(), new Color(14, 8, 32)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 212, 255, 40));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        // ── Logo / brand ──────────────────────────────────────────────────────
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBorder(new EmptyBorder(40, 0, 20, 0));
        JLabel brand = new JLabel("AutoElite");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 42));
        brand.setForeground(new Color(0, 212, 255));
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("PREMIUM SHOWROOM MANAGEMENT");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        sub.setForeground(new Color(80, 120, 160));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        // ── AutoElite Logo ────────────────────────────────────────────────────
        JLabel lock;
        try {
            java.net.URL logoUrl = CarShowroomApp.class.getClassLoader().getResource("autoelite_logo.png");
            if (logoUrl == null) {
                // Fallback: try loading from filesystem relative to working dir
                java.io.File logoFile = new java.io.File("src/resources/autoelite_logo.png");
                logoUrl = logoFile.exists() ? logoFile.toURI().toURL() : null;
            }
            if (logoUrl != null) {
                ImageIcon rawIcon = new ImageIcon(logoUrl);
                java.awt.Image scaled = rawIcon.getImage().getScaledInstance(180, 180, java.awt.Image.SCALE_SMOOTH);
                lock = new JLabel(new ImageIcon(scaled));
            } else {
                lock = new JLabel("🔒");
                lock.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
            }
        } catch (Exception _e) {
            lock = new JLabel("🔒");
            lock.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        }
        lock.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoPanel.add(lock);
        logoPanel.add(Box.createVerticalStrut(8));
        logoPanel.add(brand);
        logoPanel.add(Box.createVerticalStrut(4));
        logoPanel.add(sub);

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 0, 0, 0));

        java.util.function.Function<String, JTextField> mkField = placeholder -> {
            JTextField tf = new JTextField(20);
            tf.setBackground(new Color(20, 28, 55));
            tf.setForeground(new Color(230, 240, 255));
            tf.setCaretColor(new Color(0, 212, 255));
            tf.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 212, 255, 80), 1),
                    new EmptyBorder(14, 18, 14, 18)));
            tf.setMaximumSize(new Dimension(400, 56));
            tf.setPreferredSize(new Dimension(400, 56));
            return tf;
        };

        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLbl.setForeground(new Color(100, 130, 180));
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField userField = mkField.apply("Username");
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passLbl.setForeground(new Color(100, 130, 180));
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField passField = new JPasswordField(20);
        passField.setBackground(new Color(20, 28, 55));
        passField.setForeground(new Color(230, 240, 255));
        passField.setCaretColor(new Color(0, 212, 255));
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        passField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 212, 255, 80), 1),
                new EmptyBorder(14, 18, 14, 18)));
        passField.setMaximumSize(new Dimension(400, 56));
        passField.setPreferredSize(new Dimension(400, 56));
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel errorLbl = new JLabel(" ");
        errorLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLbl.setForeground(new Color(255, 80, 80));
        errorLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Login button
        JButton loginBtn = new JButton("LOGIN  →");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(new Color(0, 160, 200));
        loginBtn.setOpaque(true);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.setMaximumSize(new Dimension(400, 56));
        loginBtn.setPreferredSize(new Dimension(400, 56));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                loginBtn.setBackground(new Color(0, 212, 255));
            }

            public void mouseExited(MouseEvent e) {
                loginBtn.setBackground(new Color(0, 160, 200));
            }
        });

        form.add(userLbl);
        form.add(Box.createVerticalStrut(4));
        form.add(userField);
        form.add(Box.createVerticalStrut(14));
        form.add(passLbl);
        form.add(Box.createVerticalStrut(4));
        form.add(passField);
        form.add(Box.createVerticalStrut(6));
        form.add(errorLbl);
        form.add(Box.createVerticalStrut(10));
        form.add(loginBtn);

        // ── Login action ──────────────────────────────────────────────────────
        Runnable doLogin = () -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                errorLbl.setText("Please enter username and password.");
                return;
            }
            if (attempts[0] >= 3) {
                errorLbl.setText("Too many attempts. Restart the app.");
                loginBtn.setEnabled(false);
                return;
            }
            utils.DatabaseManager db = utils.DatabaseManager.getInstance();
            String role = db.authenticate(username, password);
            if (role != null) {
                // Check 2FA
                String phone = db.getUserPhone(username);
                if (phone != null && !phone.isBlank()) {
                    errorLbl.setForeground(new Color(0, 212, 255));
                    errorLbl.setText("Sending OTP to your phone...");
                    String otp = utils.OtpService.generateAndSend(username, phone);
                    // Show OTP dialog
                    String entered = showDarkInputDialog(dlg,
                            "🔐  Two-Factor Authentication",
                            "A 6-digit OTP has been sent to your registered phone.\nEnter OTP (expires in 5 min):",
                            "6-digit OTP");
                    if (entered == null) {
                        errorLbl.setText("2FA cancelled.");
                        errorLbl.setForeground(new Color(255, 80, 80));
                        return;
                    }
                    if (!utils.OtpService.verify(username, entered.trim())) {
                        errorLbl.setForeground(new Color(255, 80, 80));
                        errorLbl.setText("Invalid or expired OTP.");
                        return;
                    }
                }
                utils.SessionManager.login(username, utils.SessionManager.Role.fromDb(role));
                utils.AuditLogger.log("LOGIN", "App", "Role: " + role);
                ok[0] = true;
                dlg.dispose();
            } else {
                attempts[0]++;
                int left = 3 - attempts[0];
                errorLbl.setForeground(new Color(255, 80, 80));
                errorLbl.setText("Invalid credentials. " + (left > 0 ? left + " attempt(s) left." : "Locked."));
                passField.setText("");
            }
        };

        loginBtn.addActionListener(e -> doLogin.run());
        passField.addActionListener(e -> doLogin.run());

        // Wrapped in centerCard

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel loginCard = new JPanel();
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setOpaque(true);
        loginCard.setBackground(new Color(14, 18, 32, 200));
        loginCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 212, 255, 60), 1),
                new EmptyBorder(40, 60, 40, 60)));

        loginCard.add(logoPanel);
        loginCard.add(Box.createVerticalStrut(20));
        loginCard.add(form);

        centerWrapper.add(loginCard);

        root.add(centerWrapper, BorderLayout.CENTER);
        // root.add(footer, BorderLayout.SOUTH);

        JLabel footer = new JLabel("AutoElite v17  ·  Secured Login", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        footer.setForeground(new Color(40, 60, 100));
        footer.setBorder(new EmptyBorder(0, 0, 16, 0));
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true); // blocks until disposed
        return ok[0];
    }

    public CarShowroomApp() {
        initFonts();
        initDB();
        initControllers();
        buildFrame();
        startClock();
        showPanel("DASHBOARD");
    }

    private void initFonts() {
        FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
        FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
        FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
        FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
        FONT_MONO = new Font("Courier New", Font.BOLD, 12);
    }

    private void initDB() {
        DatabaseManager db = DatabaseManager.getInstance();
        dbConnected = db.isConnected();
        if (dbConnected) {
            try {
                new DataSeeder(db.getConnection()).seedIfEmpty();
            } catch (Exception e) {
                System.err.println("Seeder error: " + e.getMessage());
            }
        }
    }

    private void initControllers() {
        carController = new CarController();
        customerController = new CustomerController();
        employeeController = new EmployeeController();
        saleController = new SaleController();
        testDriveController = new TestDriveController();
        dealershipController = new DealershipController();
        reviewController = new ReviewController();
        commissionController = new controllers.CommissionController();
    }

    private void buildFrame() {
        setTitle("AutoElite — Premium Car Showroom");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setBackground(BG_DARK);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);

        ModernSidebar modernSidebar = new ModernSidebar(
                navButtons, this::showPanel, dbConnected, () -> currentPanel);
        root.add(modernSidebar, BorderLayout.WEST);
        root.add(buildTopBar(), BorderLayout.NORTH);

        // ── Cross-fade: JLayeredPane wraps contentArea ───────────────────────
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG_DARK);

        // Fade-overlay panel painted on top
        fadeOverlayPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                if (fadeSnapshot != null && fadeAlpha > 0f) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                    g2.drawImage(fadeSnapshot, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            }
        };
        fadeOverlayPanel.setOpaque(false);
        fadeOverlayPanel.setBackground(new Color(0, 0, 0, 0));

        contentLayered = new JLayeredPane();
        contentLayered.setLayout(null);
        contentLayered.setBackground(BG_DARK);
        contentLayered.setOpaque(true);

        // Wrap contentArea directly — no outer scroll pane.
        // Each panel manages its own internal scroll. An outer scroll was
        // causing the Reports tall-content preferred height to bleed up and
        // expand all panels with huge empty space.
        JPanel contentHost = new JPanel(new BorderLayout()) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                // Always match the layered pane size so preferred height never
                // forces the container to grow beyond what's visible.
                return new java.awt.Dimension(contentLayered.getWidth(), contentLayered.getHeight());
            }
        };
        contentHost.setBackground(BG_DARK);
        contentHost.add(contentArea, BorderLayout.CENTER);
        contentHost.setBounds(0, 0, 1170, 860);

        contentLayered.add(contentHost, JLayeredPane.DEFAULT_LAYER);
        contentLayered.add(fadeOverlayPanel, JLayeredPane.POPUP_LAYER);

        // Single component listener keeps both layers sized to the layered pane
        contentLayered.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = contentLayered.getWidth(), h = contentLayered.getHeight();
                contentHost.setBounds(0, 0, w, h);
                fadeOverlayPanel.setBounds(0, 0, w, h);
            }
        });

        root.add(contentLayered, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    // ─── SIDEBAR ──────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_SIDEBAR, 0, getHeight(), new Color(12, 8, 28));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // right border glow
                g2.setColor(BORDER_GLOW);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            }
        };
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 0));

        // Logo section
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Draw AutoElite Logo Image
                try {
                    java.net.URL logoUrl = CarShowroomApp.class.getClassLoader().getResource("autoelite_logo.png");
                    if (logoUrl == null) {
                        java.io.File logoFile = new java.io.File("src/resources/autoelite_logo.png");
                        logoUrl = logoFile.exists() ? logoFile.toURI().toURL() : null;
                    }
                    if (logoUrl != null) {
                        ImageIcon rawIcon = new ImageIcon(logoUrl);
                        java.awt.Image scaled = rawIcon.getImage().getScaledInstance(45, 45,
                                java.awt.Image.SCALE_SMOOTH);
                        g2.drawImage(scaled, 18, 18, null);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        logoPanel.setPreferredSize(new Dimension(220, 80));
        logoPanel.setLayout(null);

        JLabel brandLabel = new JLabel(
                "<html><b style='color:#00D4FF;font-size:15px'>AutoElite</b><br><span style='color:#6478A0;font-size:10px'>PREMIUM SHOWROOM</span></html>");
        brandLabel.setBounds(72, 15, 140, 50);
        logoPanel.add(brandLabel);
        sidebar.add(logoPanel, BorderLayout.NORTH);

        // Nav items
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(new Color(0, 0, 0, 0));
        navPanel.setOpaque(false);

        navPanel.add(Box.createVerticalStrut(10));

        String[][] navItems = {
                { "DASHBOARD", "⬡", "Dashboard" },
                { "CARS", "🚗", "Inventory" },
                { "CUSTOMERS", "👤", "Customers" },
                { "EMPLOYEES", "👔", "Team" },
                { "SALES", "💰", "Sales" },
                { "COMMISSIONS", "🏆", "Commissions" },
                { "TESTDRIVES", "🔑", "Test Drives" },
                { "DEALERSHIPS", "📍", "Locations" },
                { "REVIEWS", "⭐", "Reviews" },
                { "REPORTS", "📊", "Reports" },
                { "SETTINGS", "⚙", "Settings" },
        };

        for (String[] item : navItems) {
            JButton btn = createNavButton(item[1], item[2], item[0]);
            navButtons.put(item[0], btn);
            navPanel.add(btn);
            navPanel.add(Box.createVerticalStrut(4));
        }

        sidebar.add(navPanel, BorderLayout.CENTER);

        // DB status at bottom
        JPanel dbStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        dbStatus.setOpaque(false);
        Color dot = dbConnected ? ACCENT_GREEN : ACCENT_PINK;
        JLabel dbLbl = new JLabel("⬤ " + (dbConnected ? "Connected" : "Offline Mode"));
        dbLbl.setFont(FONT_SMALL);
        dbLbl.setForeground(dot);
        dbStatus.add(dbLbl);
        sidebar.add(dbStatus, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String icon, String label, String panelKey) {
        JButton btn = new JButton(icon + "  " + label) {
            boolean hovered = false;
            boolean selected = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = currentPanel.equals(panelKey);
                if (active) {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(0, 212, 255, 30), getWidth(), 0,
                            new Color(138, 43, 226, 20));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(0, 6, 0, getHeight() - 6);
                } else if (hovered) {
                    g2.setColor(HOVER_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(TEXT_MUTED);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(210, 46));
        btn.setPreferredSize(new Dimension(210, 46));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0, 16, 0, 0));
        btn.addActionListener(e -> showPanel(panelKey));
        return btn;
    }

    // ─── TOP BAR ──────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 212, 255, 40));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        bar.setLayout(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 55));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        try {
            java.net.URL logoUrl = CarShowroomApp.class.getClassLoader().getResource("autoelite_logo.png");
            if (logoUrl == null) {
                java.io.File logoFile = new java.io.File("src/resources/autoelite_logo.png");
                logoUrl = logoFile.exists() ? logoFile.toURI().toURL() : null;
            }
            if (logoUrl != null) {
                ImageIcon rawIcon = new ImageIcon(logoUrl);
                java.awt.Image scaled = rawIcon.getImage().getScaledInstance(35, 35, java.awt.Image.SCALE_SMOOTH);
                titlePanel.add(new JLabel(new ImageIcon(scaled)));
            }
        } catch (Exception ignored) {
        }

        JLabel pageTitle = new JLabel("AutoElite Management System");
        pageTitle.setFont(FONT_TITLE);
        pageTitle.setForeground(TEXT_PRIMARY);
        titlePanel.add(pageTitle);
        bar.add(titlePanel, BorderLayout.WEST);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        rightBar.setOpaque(false);

        clockLabel = new JLabel();
        clockLabel.setFont(FONT_MONO);
        clockLabel.setForeground(ACCENT_CYAN);

        JLabel userLabel = new JLabel(
                "⬤  " + (utils.SessionManager.getUser() != null ? utils.SessionManager.getUser() : "Admin"));
        userLabel.setFont(FONT_LABEL);
        userLabel.setForeground(ACCENT_GREEN);

        rightBar.add(clockLabel);
        rightBar.add(new JSeparator(JSeparator.VERTICAL));
        rightBar.add(userLabel);
        bar.add(rightBar, BorderLayout.EAST);
        return bar;
    }

    // ─── STATUS BAR ───────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        bar.setBackground(new Color(6, 8, 16));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 40, 70)));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);
        bar.add(statusLabel);
        return bar;
    }

    private void startClock() {
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE  HH:mm:ss z");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                    String time = sdf.format(new Date());
                    if (clockLabel != null)
                        clockLabel.setText(time);
                });
            }
        }, 0, 1000);
    }

    private void setStatus(String msg) {
        if (statusLabel != null)
            statusLabel.setText("  " + msg);
    }

    // ─── SHOW PANEL — Cross-Fade Transition Engine ────────────────────────────
    private void showPanel(String key) {
        // ── RBAC guard ──────────────────────────────────────────────────────
        if (!utils.SessionManager.canView(key)) {
            showInfo("⛔  Access denied. Your role (" +
                    utils.SessionManager.getRole() + ") cannot view this panel.");
            return;
        }
        currentPanel = key;
        navButtons.values().forEach(JComponent::repaint);
        setStatus("Viewing: " + key.charAt(0) + key.substring(1).toLowerCase());

        // ── Step 1: Capture snapshot of current visible content ────────────────
        if (contentArea.getWidth() > 0 && contentArea.getHeight() > 0) {
            fadeSnapshot = new BufferedImage(
                    contentArea.getWidth(), contentArea.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = fadeSnapshot.createGraphics();
            contentArea.paint(sg);
            sg.dispose();
            fadeAlpha = 1f;
            fadeOverlayPanel.repaint();
        }

        // Stop any running fade
        if (fadeTimer != null && fadeTimer.isRunning())
            fadeTimer.stop();

        // ── Step 2: Build the new panel (while snapshot covers it) ────────────
        // CUSTOMERS and EMPLOYEES always rebuild — inline edits change DB state
        // between visits, so the cached panel would show stale data.
        // SETTINGS always rebuilds so the credential status badge and field values
        // always reflect the latest state in Preferences.
        boolean alwaysRebuild = "DASHBOARD".equals(key)
                || "CUSTOMERS".equals(key)
                || "EMPLOYEES".equals(key)
                || "SETTINGS".equals(key)
                || "AUDIT".equals(key);

        if (!alwaysRebuild) {
            for (Component c : contentArea.getComponents()) {
                if (c.getName() != null && c.getName().equals(key)) {
                    cardLayout.show(contentArea, key);
                    startFadeOut();
                    return;
                }
            }
        }
        // Remove stale cached version before rebuilding
        for (Component c : contentArea.getComponents()) {
            if (key.equals(c.getName())) {
                contentArea.remove(c);
                break;
            }
        }

        JPanel panel;
        switch (key) {
            case "DASHBOARD":
                panel = buildDashboard();
                break;
            case "CARS":
                panel = buildCarsPanel();
                break;
            case "CUSTOMERS":
                panel = buildCustomersPanel();
                break;
            case "EMPLOYEES":
                panel = buildEmployeesPanel();
                break;
            case "SALES":
                panel = buildSalesPanelEnhanced();
                break;
            case "TESTDRIVES":
                panel = buildTestDrivesPanelEnhanced();
                break;
            case "DEALERSHIPS":
                panel = buildDealershipsPanel();
                break;
            case "REVIEWS":
                panel = buildReviewsPanel();
                break;
            case "REPORTS":
                panel = buildReportsPanelEnhanced();
                break;
            case "SETTINGS":
                panel = buildSettingsPanel();
                break;
            case "COMMISSIONS":
                panel = buildCommissionsPanel();
                break;
            case "AUDIT":
                panel = buildAuditPanel();
                break;
            default:
                panel = new JPanel();
        }
        panel.setName(key);
        contentArea.add(panel, key);
        cardLayout.show(contentArea, key);

        // ── Step 3: Animate snapshot fading out over 300ms ────────────────────
        startFadeOut();
    }

    /** Animates fadeAlpha from current → 0 over ~300ms. */
    private void startFadeOut() {
        if (fadeOverlayPanel == null)
            return;
        final int STEPS = 19;
        final int[] step = { 0 };
        fadeTimer = new javax.swing.Timer(16, null);
        fadeTimer.addActionListener(e -> {
            step[0]++;
            float t = step[0] / (float) STEPS;
            // Ease-out cubic
            t = 1f - (float) Math.pow(1f - t, 3);
            fadeAlpha = Math.max(0f, 1f - t);
            fadeOverlayPanel.repaint();
            if (step[0] >= STEPS) {
                fadeAlpha = 0f;
                fadeSnapshot = null;
                fadeOverlayPanel.repaint();
                fadeTimer.stop();
            }
        });
        fadeTimer.start();
    }

    // ─── DASHBOARD — Glass-Canvas Cyber-Luxury Rewrite ───────────────────────
    private JPanel buildDashboard() {

        // ── Master page with RadialGradientPaint atmospheric background ────────
        JPanel page = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int W = getWidth(), H = getHeight();

                // Atmospheric radial gradient: Deep Indigo → Rich Black
                RadialGradientPaint atmo = new RadialGradientPaint(
                        W * 0.5f, 0f,
                        Math.max(W, H) * 0.92f,
                        new float[] { 0f, 0.55f, 1f },
                        new Color[] { new Color(0x15, 0x10, 0x25), new Color(0x0B, 0x07, 0x16),
                                new Color(0x05, 0x05, 0x08) });
                g2.setPaint(atmo);
                g2.fillRect(0, 0, W, H);

                // Subtle cyan nebula bloom — upper-right
                RadialGradientPaint nebula1 = new RadialGradientPaint(
                        W * 0.85f, H * 0.10f, W * 0.38f,
                        new float[] { 0f, 1f },
                        new Color[] { new Color(0, 212, 255, 22), new Color(0, 0, 0, 0) });
                g2.setPaint(nebula1);
                g2.fillRect(0, 0, W, H);

                // Purple nebula bloom — lower-left
                RadialGradientPaint nebula2 = new RadialGradientPaint(
                        W * 0.08f, H * 0.88f, W * 0.32f,
                        new float[] { 0f, 1f },
                        new Color[] { new Color(138, 43, 226, 18), new Color(0, 0, 0, 0) });
                g2.setPaint(nebula2);
                g2.fillRect(0, 0, W, H);

                g2.dispose();
            }
        };
        page.setOpaque(true);
        page.setBorder(new EmptyBorder(24, 24, 24, 24));

        // ── Collect live data ──────────────────────────────────────────────────
        int totalCars = carController.getTotalStock();
        int totalCustomers = customerController.getTotalCount();
        double revenue = saleController.getTotalRevenue();
        int totalSales = saleController.getTotalSales();
        int scheduled = testDriveController.getScheduledCount();

        // ── Hero strip ────────────────────────────────────────────────────────
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Glass body
                g2.setColor(new Color(20, 25, 45, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

                // Top edge sheen (white → transparent LinearGradient)
                LinearGradientPaint sheen = new LinearGradientPaint(
                        0f, 0f, 0f, (float) getHeight() * 0.5f,
                        new float[] { 0f, 1f },
                        new Color[] { new Color(255, 255, 255, 45), new Color(255, 255, 255, 0) });
                g2.setPaint(sheen);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 18, 18);

                // Border — soft cyan glow
                g2.setColor(new Color(0, 212, 255, 55));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);

                // Decorative ruled lines
                g2.setColor(new Color(0, 212, 255, 8));
                g2.setStroke(new BasicStroke(0.5f));
                for (int x = 0; x < getWidth(); x += 52)
                    g2.drawLine(x, 0, x, getHeight());

                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 118));
        hero.setBorder(new EmptyBorder(18, 28, 18, 28));

        JLabel heroTitle = new JLabel(
                "<html><span style='color:#00D4FF;font-size:28px;font-weight:800;letter-spacing:3px'>AUTOELITE</span>" +
                        "<span style='color:#5060A0;font-size:20px'>&nbsp;&nbsp;MANAGEMENT SYSTEM</span></html>");
        JLabel heroSub = new JLabel(
                "Premium Vehicle Sales & Operations  •  " +
                        new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()));
        heroSub.setFont(FONT_LABEL);
        heroSub.setForeground(new Color(80, 110, 160));

        JPanel heroText = new JPanel(new BorderLayout(0, 6));
        heroText.setOpaque(false);
        heroText.add(heroTitle, BorderLayout.NORTH);
        heroText.add(heroSub, BorderLayout.CENTER);
        hero.add(heroText, BorderLayout.WEST);

        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        quickBtns.setOpaque(false);
        quickBtns.add(glowButton("+ Add Car", ACCENT_CYAN, e -> showPanel("CARS")));
        quickBtns.add(glowButton("+ New Sale", ACCENT_GREEN, e -> showPanel("SALES")));
        quickBtns.add(glowButton("Book Test Drive", ACCENT_PURPLE, e -> showPanel("TESTDRIVES")));

        hero.add(quickBtns, BorderLayout.EAST);
        page.add(hero, BorderLayout.NORTH);

        // ── Center content ─────────────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(18, 0, 0, 0));

        // ── NeonGlassCard stat row ─────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 145));

        // Revenue and Inventory cards get animated count-up labels
        NeonGlassCard revenueCard = new NeonGlassCard(ACCENT_GREEN);
        animateValue(revenueCard.valueLbl, revenue, true);
        revenueCard.titleLbl.setText("Revenue (YTD)");
        revenueCard.iconLbl.setText("💰");
        revenueCard.subLbl.setText("completed sales total");

        NeonGlassCard inventoryCard = new NeonGlassCard(ACCENT_CYAN);
        animateValue(inventoryCard.valueLbl, totalCars, false);
        inventoryCard.titleLbl.setText("Total Inventory");
        inventoryCard.iconLbl.setText("🚗");
        inventoryCard.subLbl.setText("vehicles in stock");

        NeonGlassCard custCard = new NeonGlassCard(ACCENT_PURPLE);
        custCard.valueLbl.setText(String.valueOf(totalCustomers));
        custCard.titleLbl.setText("Customers");
        custCard.iconLbl.setText("👤");
        custCard.subLbl.setText("registered clients");

        NeonGlassCard tdCard = new NeonGlassCard(ACCENT_ORANGE);
        tdCard.valueLbl.setText(String.valueOf(scheduled));
        tdCard.titleLbl.setText("Test Drives");
        tdCard.iconLbl.setText("🔑");
        tdCard.subLbl.setText("upcoming scheduled");

        statsRow.add(inventoryCard);
        statsRow.add(custCard);
        statsRow.add(revenueCard);
        statsRow.add(tdCard);
        center.add(statsRow, BorderLayout.NORTH);

        // ── Low stock alert ────────────────────────────────────────────────────
        List<Car> lowStockCars = carController.getLowStockCars(2);
        if (!lowStockCars.isEmpty()) {
            JPanel alertOuter = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 140, 0, 14));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(new Color(255, 140, 0, 80));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.dispose();
                }
            };
            alertOuter.setOpaque(false);
            alertOuter.setBorder(new EmptyBorder(8, 12, 8, 12));
            JLabel alertIcon = new JLabel("<html><b>⚠ LOW STOCK</b></html>");
            alertIcon.setFont(new Font("Segoe UI", Font.BOLD, 11));
            alertIcon.setForeground(ACCENT_ORANGE);
            alertIcon.setPreferredSize(new Dimension(90, 50));
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);
            for (Car c : lowStockCars) {
                JLabel row = new JLabel("• " + c.getBrand() + " " + c.getModel() + "  (" + c.getStock() + " left)");
                row.setFont(FONT_SMALL);
                row.setForeground(ACCENT_ORANGE);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                listPanel.add(row);
                listPanel.add(Box.createVerticalStrut(2));
            }
            JScrollPane innerScroll = new JScrollPane(listPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            innerScroll.setBorder(null);
            innerScroll.setOpaque(false);
            innerScroll.getViewport().setOpaque(false);
            innerScroll.setPreferredSize(new Dimension(0, 50));
            styleScrollBar(innerScroll.getVerticalScrollBar());
            JButton goInventory = smallButton("View →", ACCENT_ORANGE);
            goInventory.addActionListener(e -> showPanel("CARS"));
            alertOuter.add(alertIcon, BorderLayout.WEST);
            alertOuter.add(innerScroll, BorderLayout.CENTER);
            alertOuter.add(goInventory, BorderLayout.EAST);
            center.add(alertOuter, BorderLayout.CENTER);
        }

        // ── Bottom widgets ─────────────────────────────────────────────────────
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(buildRecentSalesWidget());
        bottomRow.add(buildTopCarsWidget());
        center.add(bottomRow, lowStockCars.isEmpty() ? BorderLayout.CENTER : BorderLayout.SOUTH);

        page.add(center, BorderLayout.CENTER);
        return page;
    }

    // ── Animated count-up for stat card values ─────────────────────────────────
    private void animateValue(JLabel label, double target, boolean currency) {
        final int STEPS = 55;
        final int[] step = { 0 };
        javax.swing.Timer t = new javax.swing.Timer(14, null);
        t.addActionListener(e -> {
            step[0]++;
            double progress = 1.0 - Math.pow(1.0 - step[0] / (double) STEPS, 3); // ease-out cubic
            double current = target * progress;
            if (currency)
                label.setText("$" + formatMoney(current));
            else
                label.setText(String.valueOf((int) current));
            if (step[0] >= STEPS) {
                if (currency)
                    label.setText("$" + formatMoney(target));
                else
                    label.setText(String.valueOf((int) target));
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        t.start();
    }

    // ── NeonGlassCard — the Cyber-Luxury stat card component ──────────────────
    /**
     * Self-contained stat card with glass background, top-edge sheen, and hover
     * neon bloom.
     */
    private JPanel buildCommissionsPanel() {
        JPanel jPanel = this.darkPage();
        jPanel.setLayout(new BorderLayout(0, 16));
        jPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel jPanel2 = new JPanel(new BorderLayout());
        jPanel2.setOpaque(false);
        jPanel2.add((Component) this.sectionTitle("\ud83c\udfc6  Commission Tracker", new Color(255, 215, 0)), "West");
        JPanel jPanel3 = new JPanel(new FlowLayout(2, 10, 0));
        jPanel3.setOpaque(false);
        JButton jButton = this.smallButton("\u21bb Refresh", TEXT_MUTED);
        jPanel3.add(jButton);
        jPanel2.add((Component) jPanel3, "East");
        jPanel.add((Component) jPanel2, "North");
        JPanel jPanel4 = new JPanel(new BorderLayout(0, 12));
        jPanel4.setOpaque(false);
        Object[] objectArray = new String[] { "Emp ID", "Name", "Sales", "Total Sales ($)", "Earned ($)",
                "Unpaid ($)" };
        DefaultTableModel defaultTableModel = new DefaultTableModel(objectArray, 0) {

            @Override
            public boolean isCellEditable(int n, int n2) {
                return false;
            }
        };
        JTable jTable = this.buildFancyTable(defaultTableModel);
        Runnable runnable = () -> {
            defaultTableModel.setRowCount(0);
            for (Object[] empSummaryRow : this.commissionController.getEmployeeSummary()) {
                defaultTableModel.addRow(empSummaryRow);
            }
        };
        runnable.run();
        Object[] objectArray2 = new String[] { "ID", "Emp ID", "Name", "Sale #", "Sale ($)", "Commission ($)", "Rate",
                "Status" };
        DefaultTableModel defaultTableModel2 = new DefaultTableModel(objectArray2, 0) {

            @Override
            public boolean isCellEditable(int n, int n2) {
                return false;
            }
        };
        JTable jTable2 = this.buildFancyTable(defaultTableModel2);
        Runnable runnable2 = () -> {
            defaultTableModel2.setRowCount(0);
            for (Object[] commRow : this.commissionController.getAllCommissions()) {
                defaultTableModel2.addRow(commRow);
            }
        };
        runnable2.run();
        jButton.addActionListener(actionEvent -> {
            runnable.run();
            runnable2.run();
        });
        JPanel jPanel5 = new JPanel(new FlowLayout(0, 10, 8));
        jPanel5.setOpaque(false);
        JButton jButton2 = this.glowButton("\u2705 Mark Paid", ACCENT_GREEN, actionEvent -> {
            int n = jTable2.getSelectedRow();
            if (n < 0) {
                this.showInfo("Select a commission entry.");
                return;
            }
            int n2 = Integer.parseInt((String) defaultTableModel2.getValueAt(n, 0));
            if (this.commissionController.markPaid(n2)) {
                runnable2.run();
                runnable.run();
                this.showSuccess("Marked as paid.");
            } else {
                this.showError("Update failed.");
            }
        });
        JButton jButton3 = this.glowButton("% Set Rate", new Color(255, 215, 0), actionEvent -> {
            int n = jTable.getSelectedRow();
            if (n < 0) {
                this.showInfo("Select an employee from the summary table.");
                return;
            }
            String string = (String) defaultTableModel.getValueAt(n, 0);
            String string2 = (String) defaultTableModel.getValueAt(n, 1);
            String string3 = JOptionPane.showInputDialog(this, "Commission rate (%) for " + string2 + ":", "Set Rate",
                    -1);
            if (string3 != null && !string3.isBlank()) {
                try {
                    double d = Double.parseDouble(string3.trim());
                    if (this.commissionController.setRate(string, d)) {
                        this.showSuccess("Rate set to " + d + "% for " + string2);
                    }
                } catch (NumberFormatException numberFormatException) {
                    this.showError("Enter a valid number.");
                }
            }
        });
        double d = this.commissionController.getTotalUnpaid();
        String[] stringArray = this.commissionController.getTopEarner();
        JPanel jPanel6 = new JPanel(new FlowLayout(0, 20, 6));
        jPanel6.setOpaque(false);
        JLabel jLabel = new JLabel("Total Unpaid: $" + String.format("%,.0f", d));
        jLabel.setFont(FONT_LABEL.deriveFont(1));
        jLabel.setForeground(d > 0.0 ? new Color(255, 160, 0) : ACCENT_GREEN);
        JLabel jLabel2 = new JLabel("Top Earner: " + stringArray[0] + " (" + stringArray[1] + ")");
        jLabel2.setFont(FONT_LABEL);
        jLabel2.setForeground(new Color(255, 215, 0));
        jPanel6.add(jLabel);
        jPanel6.add(jLabel2);
        jPanel5.add(jButton2);
        jPanel5.add(jButton3);
        JPanel jPanel7 = new JPanel(new BorderLayout(0, 4));
        jPanel7.setOpaque(false);
        jPanel7.add((Component) jPanel6, "North");
        JLabel jLabel3 = new JLabel("  \ud83d\udc64 Employee Summary \u2014 select a row, then click % Set Rate");
        jLabel3.setFont(FONT_SMALL);
        jLabel3.setForeground(TEXT_MUTED);
        jPanel7.add((Component) jLabel3, "Center");
        jPanel7.add((Component) this.styledScrollPane(jTable), "South");
        jTable.setPreferredScrollableViewportSize(new Dimension(0, 110));
        JLabel jLabel4 = new JLabel("  All Commission Entries");
        jLabel4.setFont(FONT_LABEL.deriveFont(1));
        jLabel4.setForeground(TEXT_PRIMARY);
        JPanel jPanel8 = new JPanel(new BorderLayout(0, 4));
        jPanel8.setOpaque(false);
        jPanel8.add((Component) jLabel4, "North");
        jPanel8.add((Component) this.styledScrollPane(jTable2), "Center");
        jPanel8.add((Component) jPanel5, "South");
        JSplitPane jSplitPane = new JSplitPane(0, jPanel7, jPanel8);
        jSplitPane.setDividerLocation(200);
        jSplitPane.setResizeWeight(0.35);
        jSplitPane.setBackground(BG_DARK);
        jSplitPane.setBorder(null);
        jSplitPane.setOpaque(false);
        jPanel4.add((Component) jSplitPane, "Center");
        jPanel.add((Component) jPanel4, "Center");
        return jPanel;
    }

    private Map<String, Long> getSalesByMonth() {
        TreeMap<String, Long> treeMap = new TreeMap<String, Long>();
        try {
            for (Sale sale : this.saleController.getAllSales()) {
                if (!"Completed".equalsIgnoreCase(sale.getStatus()))
                    continue;
                String string = sale.getSaleDate() != null ? new SimpleDateFormat("yyyy-MM").format(sale.getSaleDate())
                        : "Unknown";
                treeMap.merge(string, (long) sale.getSalePrice(), Long::sum);
            }
        } catch (Exception exception) {
            // empty catch block
        }
        return treeMap;
    }

    private Map<String, Long> getSalesCountByMonth() {
        TreeMap<String, Long> treeMap = new TreeMap<String, Long>();
        try {
            for (Sale sale : this.saleController.getAllSales()) {
                if (!"Completed".equalsIgnoreCase(sale.getStatus()))
                    continue;
                String string = sale.getSaleDate() != null ? new SimpleDateFormat("yyyy-MM").format(sale.getSaleDate())
                        : "Unknown";
                treeMap.merge(string, 1L, Long::sum);
            }
        } catch (Exception exception) {
            // empty catch block
        }
        return treeMap;
    }

    /**
     * Builds a two-column stats card with an embedded chart panel on the left
     * and a legend/stats panel on the right.
     */
    private JPanel buildReportCard(String string, final Color color, JPanel jPanel, String[][] stringArray,
            Color[] colorArray) {
        JPanel jPanel2 = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(BG_PANEL);
                graphics2D.fillRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 12, 12);
                graphics2D.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                graphics2D.setStroke(new BasicStroke(1.0f));
                graphics2D.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 12, 12);
                graphics2D.setPaint(new GradientPaint(0.0f, 0.0f, color, 0.0f, this.getHeight(),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)));
                graphics2D.fillRoundRect(0, 0, 4, this.getHeight(), 4, 4);
                graphics2D.dispose();
            }
        };
        jPanel2.setOpaque(false);
        jPanel2.setBorder(new EmptyBorder(16, 18, 16, 18));
        jPanel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        JLabel jLabel = new JLabel(string);
        jLabel.setFont(new Font("Segoe UI", 1, 13));
        jLabel.setForeground(color);
        jLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        jPanel2.add((Component) jLabel, "North");
        JPanel jPanel3 = new JPanel(new BorderLayout(14, 0));
        jPanel3.setOpaque(false);
        jPanel3.add((Component) jPanel, "Center");
        JPanel jPanel4 = new JPanel();
        jPanel4.setOpaque(false);
        jPanel4.setLayout(new BoxLayout(jPanel4, 1));
        jPanel4.setPreferredSize(new Dimension(185, 0));
        if (stringArray != null) {
            for (int i = 0; i < stringArray.length; ++i) {
                String string2 = stringArray[i][0];
                String string3 = stringArray[i][1];
                if (string2.isEmpty()) {
                    jPanel4.add(Box.createVerticalStrut(6));
                    continue;
                }
                Color color2 = colorArray != null && i < colorArray.length && colorArray[i] != null ? colorArray[i]
                        : new Color(220, 235, 255);
                jPanel4.add(CarShowroomApp.this.makeStatRow(string2, string3, color2));
                jPanel4.add(Box.createVerticalStrut(3));
            }
        }
        jPanel4.add(Box.createVerticalGlue());
        jPanel3.add((Component) jPanel4, "East");
        jPanel2.add((Component) jPanel3, "Center");
        return jPanel2;
    }

    private JPanel buildLineChart(String string, String string2, Map<String, Long> map, Color color, Color color2) {
        ArrayList<String> arrayList = new ArrayList<String>(map.keySet());
        final String string3 = arrayList.isEmpty() ? "" : (String) arrayList.get(arrayList.size() - 1);
        final String string4 = arrayList.size() >= 2 ? (String) arrayList.get(arrayList.size() - 2) : "";
        final long l = map.getOrDefault(string3, 0L);
        final long l2 = map.getOrDefault(string4, 0L);
        Random random = new Random(l + l2);
        final long[] lArray = buildMonthDailyArray(l, 28, random);
        final long[] lArray2 = buildMonthDailyArray(l2, 28, random);
        final Color color3 = color;
        final Color color4 = color2 != null ? color2 : new Color(138, 43, 226);
        return new JPanel() {
            {
                this.setBackground(new Color(8, 11, 22));
                this.setPreferredSize(new Dimension(0, 240));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                int n;
                int n2;
                int n3;
                super.paintComponent(graphics);
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int n4 = this.getWidth();
                int n5 = this.getHeight();
                int n6 = 58;
                int n7 = 20;
                int n8 = 32;
                int n9 = 40;
                int n10 = n4 - n6 - n7;
                int n11 = n5 - n8 - n9;
                graphics2D.setColor(new Color(8, 11, 22));
                graphics2D.fillRect(0, 0, n4, n5);
                if (l == 0L && l2 == 0L) {
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 12));
                    graphics2D.drawString("No sales data yet", n6 + n10 / 2 - 55, n8 + n11 / 2);
                    graphics2D.dispose();
                    return;
                }
                int n12 = Math.max(28, 28);
                long l3 = Math.max(l, l2);
                l3 = Math.max(l3, 1L);
                float f = (float) n10 / (float) Math.max(n12 - 1, 1);
                graphics2D.setStroke(new BasicStroke(0.6f, 0, 0, 10.0f, new float[] { 4.0f, 4.0f }, 0.0f));
                for (n3 = 0; n3 <= 5; ++n3) {
                    n2 = n8 + n11 - n3 * n11 / 5;
                    graphics2D.setColor(new Color(255, 255, 255, 16));
                    graphics2D.drawLine(n6, n2, n6 + n10, n2);
                    long l22 = l3 * (long) n3 / 5L;
                    String string = l22 >= 1000000L ? String.format("$%.1fM", (double) l22 / 1000000.0)
                            : (l22 >= 1000L ? String.format("$%dk", l22 / 1000L) : "$" + l22);
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 8));
                    graphics2D.drawString(string, 2, n2 + 4);
                }
                graphics2D.setFont(new Font("Segoe UI", 0, 8));
                for (n3 = 0; n3 < n12; n3 += 5) {
                    n2 = n6 + Math.round((float) n3 * f);
                    graphics2D.setColor(new Color(70, 90, 130));
                    graphics2D.drawString("D" + (n3 + 1), n2 - 4, n8 + n11 + 13);
                }
                if (l2 > 0L) {
                    int n13;
                    graphics2D.setStroke(new BasicStroke(1.8f, 1, 1, 10.0f, new float[] { 8.0f, 5.0f }, 0.0f));
                    graphics2D.setColor(new Color(color4.getRed(), color4.getGreen(), color4.getBlue(), 180));
                    GeneralPath generalPath = new GeneralPath();
                    int[] nArray = new int[28];
                    int[] nArray2 = new int[28];
                    for (n13 = 0; n13 < 28; ++n13) {
                        nArray[n13] = n6 + Math.round((float) n13 * f);
                        nArray2[n13] = n8 + n11 - (int) (lArray2[n13] * (long) n11 / l3);
                    }
                    generalPath.moveTo(nArray[0], nArray2[0]);
                    for (n13 = 1; n13 < 28; ++n13) {
                        float f2 = (float) nArray[n13 - 1] + f * 0.4f;
                        float f3 = (float) nArray[n13] - f * 0.4f;
                        generalPath.curveTo(f2, nArray2[n13 - 1], f3, nArray2[n13], nArray[n13], nArray2[n13]);
                    }
                    graphics2D.draw(generalPath);
                    graphics2D.setStroke(new BasicStroke(1.0f));
                    graphics2D.setColor(color4);
                    graphics2D.fillOval(nArray[27] - 3, nArray2[27] - 3, 6, 6);
                    String string = l2 >= 1000000L ? String.format("$%.2fM", (double) l2 / 1000000.0)
                            : (l2 >= 1000L ? String.format("$%dk", l2 / 1000L) : "$" + l2);
                    graphics2D.setFont(new Font("Segoe UI", 1, 9));
                    graphics2D.setColor(new Color(color4.getRed(), color4.getGreen(), color4.getBlue(), 200));
                    int n14 = graphics2D.getFontMetrics().stringWidth(string);
                    int n15 = Math.max(n6, Math.min(nArray[27] - n14 / 2, n6 + n10 - n14));
                    n = nArray2[27] - 8;
                    if (n < n8 + 10) {
                        n = nArray2[27] + 15;
                    }
                    graphics2D.drawString(string, n15, n);
                }
                if (l > 0L) {
                    int[] nArray = new int[28];
                    int[] nArray3 = new int[28];
                    for (int i = 0; i < 28; ++i) {
                        nArray[i] = n6 + Math.round((float) i * f);
                        nArray3[i] = n8 + n11 - (int) (lArray[i] * (long) n11 / l3);
                    }
                    GeneralPath generalPath = new GeneralPath();
                    generalPath.moveTo(nArray[0], n8 + n11);
                    generalPath.lineTo(nArray[0], nArray3[0]);
                    for (int i = 1; i < 28; ++i) {
                        float f4 = (float) nArray[i - 1] + f * 0.4f;
                        float f5 = (float) nArray[i] - f * 0.4f;
                        generalPath.curveTo(f4, nArray3[i - 1], f5, nArray3[i], nArray[i], nArray3[i]);
                    }
                    generalPath.lineTo(nArray[27], n8 + n11);
                    generalPath.closePath();
                    graphics2D.setPaint(new GradientPaint(0.0f, n8,
                            new Color(color3.getRed(), color3.getGreen(), color3.getBlue(), 50), 0.0f, n8 + n11,
                            new Color(0, 0, 0, 0)));
                    graphics2D.fill(generalPath);
                    graphics2D.setStroke(new BasicStroke(2.4f, 1, 1));
                    graphics2D.setColor(color3);
                    GeneralPath generalPath2 = new GeneralPath();
                    generalPath2.moveTo(nArray[0], nArray3[0]);
                    for (int i = 1; i < 28; ++i) {
                        float f6 = (float) nArray[i - 1] + f * 0.4f;
                        float f7 = (float) nArray[i] - f * 0.4f;
                        generalPath2.curveTo(f6, nArray3[i - 1], f7, nArray3[i], nArray[i], nArray3[i]);
                    }
                    graphics2D.draw(generalPath2);
                    graphics2D.setColor(new Color(color3.getRed(), color3.getGreen(), color3.getBlue(), 45));
                    graphics2D.fillOval(nArray[27] - 8, nArray3[27] - 8, 16, 16);
                    graphics2D.setStroke(new BasicStroke(1.0f));
                    graphics2D.setColor(color3);
                    graphics2D.fillOval(nArray[27] - 5, nArray3[27] - 5, 10, 10);
                    String string = l >= 1000000L ? String.format("$%.2fM", (double) l / 1000000.0)
                            : (l >= 1000L ? String.format("$%dk", l / 1000L) : "$" + l);
                    graphics2D.setFont(new Font("Segoe UI", 1, 10));
                    graphics2D.setColor(color3);
                    int n16 = graphics2D.getFontMetrics().stringWidth(string);
                    n = Math.max(n6, Math.min(nArray[27] - n16 / 2, n6 + n10 - n16));
                    int n17 = nArray3[27] - 12;
                    if (n17 < n8 + 10) {
                        n17 = nArray3[27] + 20;
                    }
                    graphics2D.drawString(string, n, n17);
                    if (l2 > 0L) {
                        double d = (double) (l - l2) * 100.0 / (double) l2;
                        String string2 = d >= 0.0 ? "\u25b2" : "\u25bc";
                        String string32 = string2 + String.format("%.1f%%", Math.abs(d)) + " vs prev";
                        graphics2D.setFont(new Font("Segoe UI", 1, 9));
                        graphics2D.setColor(d >= 0.0 ? new Color(57, 255, 20) : new Color(255, 80, 80));
                        int n18 = graphics2D.getFontMetrics().stringWidth(string32);
                        int n19 = Math.max(n6, Math.min(nArray[27] - n18 / 2, n6 + n10 - n18));
                        graphics2D.drawString(string32, n19, n17 - 12);
                    }
                }
                int n20 = n6 + n10 - 130;
                int n21 = n8 + 6;
                graphics2D.setFont(new Font("Segoe UI", 1, 9));
                if (!string3.isEmpty()) {
                    graphics2D.setColor(color3);
                    graphics2D.fillRoundRect(n20, n21, 18, 5, 3, 3);
                    graphics2D.setColor(new Color(200, 220, 255));
                    graphics2D.drawString(string3 + " (current)", n20 + 22, n21 + 6);
                }
                if (!string4.isEmpty()) {
                    graphics2D.setStroke(new BasicStroke(1.8f, 0, 0, 10.0f, new float[] { 6.0f, 4.0f }, 0.0f));
                    graphics2D.setColor(color4);
                    graphics2D.drawLine(n20, n21 + 15, n20 + 18, n21 + 15);
                    graphics2D.setStroke(new BasicStroke(1.0f));
                    graphics2D.setColor(new Color(170, 150, 220));
                    graphics2D.drawString(string4 + " (prev)", n20 + 22, n21 + 18);
                }
                graphics2D.setColor(new Color(60, 80, 120));
                graphics2D.setStroke(new BasicStroke(1.2f));
                graphics2D.drawLine(n6, n8, n6, n8 + n11);
                graphics2D.drawLine(n6, n8 + n11, n6 + n10, n8 + n11);
                graphics2D.dispose();
            }
        };
    }

    private JPanel buildHorizontalBarChart(final Map<String, Long> map, final Color color) {
        return new JPanel() {
            {
                this.setBackground(new Color(8, 11, 22));
                this.setPreferredSize(new Dimension(0, 240));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int n = this.getWidth();
                int n2 = this.getHeight();
                graphics2D.setColor(new Color(8, 11, 22));
                graphics2D.fillRect(0, 0, n, n2);
                if (map == null || map.isEmpty()) {
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 12));
                    graphics2D.drawString("No data", n / 2 - 30, n2 / 2);
                    graphics2D.dispose();
                    return;
                }
                int n3 = 110;
                int n4 = 90;
                int n5 = 18;
                int n6 = 18;
                int n7 = n - n3 - n4;
                int n8 = n2 - n5 - n6;
                long l2 = map.values().stream().mapToLong(l -> l).max().orElse(1L);
                String[] stringArray = map.keySet().toArray(new String[0]);
                int n9 = stringArray.length;
                int n10 = Math.min(28, (n8 - (n9 - 1) * 8) / Math.max(n9, 1));
                int n11 = n9 * n10 + (n9 - 1) * 8;
                int n12 = n5 + (n8 - n11) / 2;
                for (int i = 0; i < n9; ++i) {
                    long l3 = (Long) map.get(stringArray[i]);
                    int n13 = (int) (l3 * (long) n7 / Math.max(l2, 1L));
                    int n14 = n12 + i * (n10 + 8);
                    graphics2D.setFont(new Font("Segoe UI", 1, 11));
                    graphics2D.setColor(new Color(160, 180, 220));
                    Object object = stringArray[i];
                    if (((String) object).length() > 13) {
                        object = ((String) object).substring(0, 12) + "\u2026";
                    }
                    graphics2D.drawString((String) object,
                            n3 - graphics2D.getFontMetrics().stringWidth((String) object) - 8, n14 + n10 / 2 + 4);
                    graphics2D.setColor(new Color(255, 255, 255, 10));
                    graphics2D.fillRoundRect(n3, n14, n7, n10, 8, 8);
                    graphics2D.setPaint(new GradientPaint(n3, n14, color, n3 + n13, n14,
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), 120)));
                    graphics2D.fillRoundRect(n3, n14, Math.max(n13, 4), n10, 8, 8);
                    graphics2D.setColor(new Color(255, 255, 255, 28));
                    graphics2D.fillRoundRect(n3, n14, Math.max(n13, 4), n10 / 2, 8, 8);
                    String string = l3 >= 1000000L ? String.format("$%.2fM", (double) l3 / 1000000.0)
                            : (l3 >= 1000L ? String.format("$%dk", l3 / 1000L) : "$" + l3);
                    graphics2D.setFont(new Font("Segoe UI", 1, 10));
                    graphics2D.setColor(new Color(220, 235, 255));
                    graphics2D.drawString(string, n3 + n13 + 6, n14 + n10 / 2 + 4);
                }
                graphics2D.dispose();
            }
        };
    }

    private JPanel buildDualQuarterlyChart(final LinkedHashMap<String, Long> linkedHashMap,
            final LinkedHashMap<String, Long> linkedHashMap2, final int n, final int n2) {
        final String[] stringArray = new String[] { "Q1", "Q2", "Q3", "Q4" };
        final Color color = new Color(255, 180, 0);
        final Color color2 = new Color(160, 160, 200);
        return new JPanel() {
            {
                this.setBackground(new Color(8, 11, 22));
                this.setPreferredSize(new Dimension(0, 240));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                int n17;
                float f;
                int n22;
                super.paintComponent(graphics);
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int n3 = this.getWidth();
                int n4 = this.getHeight();
                int n5 = 60;
                int n6 = 20;
                int n7 = 34;
                int n8 = 42;
                int n9 = n3 - n5 - n6;
                int n10 = n4 - n7 - n8;
                graphics2D.setColor(new Color(8, 11, 22));
                graphics2D.fillRect(0, 0, n3, n4);
                long l = 0L;
                String[] stringArray2 = stringArray;
                int n11 = stringArray2.length;
                for (n22 = 0; n22 < n11; ++n22) {
                    String string = stringArray2[n22];
                    l = Math.max(l, linkedHashMap.getOrDefault(string, 0L));
                    l = Math.max(l, linkedHashMap2.getOrDefault(string, 0L));
                }
                if (l == 0L) {
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 12));
                    graphics2D.drawString("No quarterly data \u2014 add sales to populate", n5 + 10, n7 + n10 / 2);
                    graphics2D.dispose();
                    return;
                }
                int n12 = 4;
                float f2 = (float) n9 / (float) (n12 - 1);
                graphics2D.setStroke(new BasicStroke(0.6f, 0, 0, 10.0f, new float[] { 4.0f, 4.0f }, 0.0f));
                for (n22 = 0; n22 <= 5; ++n22) {
                    int n13 = n7 + n10 - n22 * n10 / 5;
                    graphics2D.setColor(new Color(255, 255, 255, 16));
                    graphics2D.drawLine(n5, n13, n5 + n9, n13);
                    long l2 = l * (long) n22 / 5L;
                    String string = l2 >= 1000000L ? String.format("$%.1fM", (double) l2 / 1000000.0)
                            : (l2 >= 1000L ? String.format("$%dk", l2 / 1000L) : "$" + l2);
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 8));
                    graphics2D.drawString(string, 2, n13 + 4);
                }
                int[] nArray = new int[n12];
                int[] nArray2 = new int[n12];
                for (int i = 0; i < n12; ++i) {
                    nArray[i] = n5 + Math.round((float) i * f2);
                    long l3 = linkedHashMap2.getOrDefault(stringArray[i], 0L);
                    nArray2[i] = n7 + n10 - (int) (l3 * (long) n10 / l);
                }
                graphics2D.setStroke(new BasicStroke(1.8f, 1, 1, 10.0f, new float[] { 8.0f, 5.0f }, 0.0f));
                graphics2D.setColor(new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), 160));
                GeneralPath generalPath = new GeneralPath();
                generalPath.moveTo(nArray[0], nArray2[0]);
                for (int i = 1; i < n12; ++i) {
                    float f3 = (float) nArray[i - 1] + f2 * 0.4f;
                    f = (float) nArray[i] - f2 * 0.4f;
                    generalPath.curveTo(f3, nArray2[i - 1], f, nArray2[i], nArray[i], nArray2[i]);
                }
                graphics2D.draw(generalPath);
                graphics2D.setStroke(new BasicStroke(1.0f));
                graphics2D.setColor(color2);
                graphics2D.fillOval(nArray[n12 - 1] - 3, nArray2[n12 - 1] - 3, 6, 6);
                nArray = new int[n12];
                nArray2 = new int[n12];
                for (int i = 0; i < n12; ++i) {
                    nArray[i] = n5 + Math.round((float) i * f2);
                    long l4 = linkedHashMap.getOrDefault(stringArray[i], 0L);
                    nArray2[i] = n7 + n10 - (int) (l4 * (long) n10 / l);
                }
                GeneralPath generalPath2 = new GeneralPath();
                generalPath2.moveTo(nArray[0], n7 + n10);
                generalPath2.lineTo(nArray[0], nArray2[0]);
                for (int i = 1; i < n12; ++i) {
                    float f4 = (float) nArray[i - 1] + f2 * 0.4f;
                    f = (float) nArray[i] - f2 * 0.4f;
                    generalPath2.curveTo(f4, nArray2[i - 1], f, nArray2[i], nArray[i], nArray2[i]);
                }
                generalPath2.lineTo(nArray[n12 - 1], n7 + n10);
                generalPath2.closePath();
                graphics2D.setPaint(
                        new GradientPaint(0.0f, n7, new Color(color.getRed(), color.getGreen(), color.getBlue(), 50),
                                0.0f, n7 + n10, new Color(0, 0, 0, 0)));
                graphics2D.fill(generalPath2);
                graphics2D.setStroke(new BasicStroke(2.4f, 1, 1));
                graphics2D.setColor(color);
                GeneralPath generalPath3 = new GeneralPath();
                generalPath3.moveTo(nArray[0], nArray2[0]);
                for (n17 = 1; n17 < n12; ++n17) {
                    f = (float) nArray[n17 - 1] + f2 * 0.4f;
                    float f5 = (float) nArray[n17] - f2 * 0.4f;
                    generalPath3.curveTo(f, nArray2[n17 - 1], f5, nArray2[n17], nArray[n17], nArray2[n17]);
                }
                graphics2D.draw(generalPath3);
                graphics2D.setStroke(new BasicStroke(1.0f));
                for (n17 = 0; n17 < n12; ++n17) {
                    boolean bl;
                    boolean bl2 = bl = n17 == n12 - 1;
                    if (bl) {
                        graphics2D.setColor(new Color(255, 180, 0, 40));
                        graphics2D.fillOval(nArray[n17] - 8, nArray2[n17] - 8, 16, 16);
                    }
                    graphics2D.setColor(color);
                    graphics2D.fillOval(nArray[n17] - (bl ? 5 : 3), nArray2[n17] - (bl ? 5 : 3), bl ? 10 : 6,
                            bl ? 10 : 6);
                }
                graphics2D.setFont(new Font("Segoe UI", 0, 9));
                for (n17 = 0; n17 < n12; ++n17) {
                    graphics2D.setColor(n17 == n12 - 1 ? color : new Color(80, 100, 140));
                    int n14 = graphics2D.getFontMetrics().stringWidth(stringArray[n17]);
                    graphics2D.drawString(stringArray[n17], nArray[n17] - n14 / 2, n7 + n10 + 14);
                }
                int n15 = n5 + n9 - 135;
                int n16 = n7 + 6;
                graphics2D.setFont(new Font("Segoe UI", 1, 9));
                graphics2D.setColor(color);
                graphics2D.fillRoundRect(n15, n16, 18, 5, 3, 3);
                graphics2D.setColor(new Color(220, 220, 255));
                graphics2D.drawString(n + " (current)", n15 + 22, n16 + 6);
                graphics2D.setStroke(new BasicStroke(1.8f, 0, 0, 10.0f, new float[] { 6.0f, 4.0f }, 0.0f));
                graphics2D.setColor(color2);
                graphics2D.drawLine(n15, n16 + 17, n15 + 18, n16 + 17);
                graphics2D.setStroke(new BasicStroke(1.0f));
                graphics2D.setColor(new Color(170, 170, 200));
                graphics2D.drawString(n2 + " (prev)", n15 + 22, n16 + 20);
                graphics2D.setColor(new Color(60, 80, 120));
                graphics2D.setStroke(new BasicStroke(1.2f));
                graphics2D.drawLine(n5, n7, n5, n7 + n10);
                graphics2D.drawLine(n5, n7 + n10, n5 + n9, n7 + n10);
                graphics2D.dispose();
            }
        };
    }

    private JPanel buildSimpleLineChart(Map<String, Long> map, Color color, String string) {
        ArrayList<String> arrayList;
        final ArrayList<String> arrayList2 = arrayList = new ArrayList<String>(map.keySet());
        final Map<String, Long> map2 = map;
        final Color color2 = color;
        return new JPanel() {
            {
                this.setBackground(new Color(8, 11, 22));
                this.setPreferredSize(new Dimension(0, 240));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                float f;
                super.paintComponent(graphics);
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int n = this.getWidth();
                int n2 = this.getHeight();
                int n3 = 60;
                int n4 = 20;
                int n5 = 30;
                int n6 = 44;
                int n7 = n - n3 - n4;
                int n8 = n2 - n5 - n6;
                graphics2D.setColor(new Color(8, 11, 22));
                graphics2D.fillRect(0, 0, n, n2);
                if (arrayList2.isEmpty()) {
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 12));
                    graphics2D.drawString("No data yet \u2014 add sales to see this chart", n3 + 10, n5 + n8 / 2);
                    graphics2D.dispose();
                    return;
                }
                int n9 = arrayList2.size();
                long l = arrayList2.stream().mapToLong(string -> map2.getOrDefault(string, 0L)).max().orElse(1L);
                l = Math.max(l, 1L);
                float f2 = n9 > 1 ? (float) n7 / (float) (n9 - 1) : (float) n7;
                graphics2D.setStroke(new BasicStroke(0.6f, 0, 0, 10.0f, new float[] { 4.0f, 4.0f }, 0.0f));
                for (int i = 0; i <= 5; ++i) {
                    int n10 = n5 + n8 - i * n8 / 5;
                    graphics2D.setColor(new Color(255, 255, 255, 16));
                    graphics2D.drawLine(n3, n10, n3 + n7, n10);
                    long l2 = l * (long) i / 5L;
                    String string2 = l2 >= 1000000L ? String.format("$%.1fM", (double) l2 / 1000000.0)
                            : (l2 >= 1000L ? String.format("$%dk", l2 / 1000L) : "$" + l2);
                    graphics2D.setColor(new Color(80, 100, 140));
                    graphics2D.setFont(new Font("Segoe UI", 0, 8));
                    graphics2D.drawString(string2, 2, n10 + 4);
                }
                int[] nArray = new int[n9];
                int[] nArray2 = new int[n9];
                for (int i = 0; i < n9; ++i) {
                    nArray[i] = n3 + (n9 == 1 ? n7 / 2 : Math.round((float) i * f2));
                    long l3 = map2.getOrDefault(arrayList2.get(i), 0L);
                    nArray2[i] = n5 + n8 - (int) (l3 * (long) n8 / l);
                }
                if (n9 > 1) {
                    GeneralPath generalPath = new GeneralPath();
                    generalPath.moveTo(nArray[0], n5 + n8);
                    generalPath.lineTo(nArray[0], nArray2[0]);
                    for (int i = 1; i < n9; ++i) {
                        float f3 = (float) nArray[i - 1] + f2 * 0.4f;
                        f = (float) nArray[i] - f2 * 0.4f;
                        generalPath.curveTo(f3, nArray2[i - 1], f, nArray2[i], nArray[i], nArray2[i]);
                    }
                    generalPath.lineTo(nArray[n9 - 1], n5 + n8);
                    generalPath.closePath();
                    graphics2D.setPaint(new GradientPaint(0.0f, n5,
                            new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), 50), 0.0f, n5 + n8,
                            new Color(0, 0, 0, 0)));
                    graphics2D.fill(generalPath);
                }
                graphics2D.setStroke(new BasicStroke(2.4f, 1, 1));
                graphics2D.setColor(color2);
                if (n9 > 1) {
                    GeneralPath generalPath = new GeneralPath();
                    generalPath.moveTo(nArray[0], nArray2[0]);
                    for (int i = 1; i < n9; ++i) {
                        float f4 = (float) nArray[i - 1] + f2 * 0.4f;
                        f = (float) nArray[i] - f2 * 0.4f;
                        generalPath.curveTo(f4, nArray2[i - 1], f, nArray2[i], nArray[i], nArray2[i]);
                    }
                    graphics2D.draw(generalPath);
                }
                for (int i = 0; i < n9; ++i) {
                    int n11;
                    boolean bl = i == n9 - 1;
                    int n12 = n11 = bl ? 5 : 3;
                    if (bl) {
                        graphics2D.setColor(new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), 40));
                        graphics2D.fillOval(nArray[i] - 9, nArray2[i] - 9, 18, 18);
                    }
                    graphics2D.setStroke(new BasicStroke(1.0f));
                    graphics2D.setColor(
                            bl ? color2 : new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), 160));
                    graphics2D.fillOval(nArray[i] - n11, nArray2[i] - n11, n11 * 2, n11 * 2);
                    if (i >= n9 - 2) {
                        long l4 = map2.getOrDefault(arrayList2.get(i), 0L);
                        String string3 = l4 >= 1000000L ? String.format("$%.2fM", (double) l4 / 1000000.0)
                                : (l4 >= 1000L ? String.format("$%dk", l4 / 1000L) : "$" + l4);
                        graphics2D.setFont(new Font("Segoe UI", 1, bl ? 10 : 8));
                        graphics2D.setColor(bl ? color2 : new Color(150, 170, 210));
                        int n13 = graphics2D.getFontMetrics().stringWidth(string3);
                        int n14 = Math.max(n3, Math.min(nArray[i] - n13 / 2, n3 + n7 - n13));
                        int n15 = nArray2[i] - 10;
                        if (n15 < n5 + 12) {
                            n15 = nArray2[i] + 18;
                        }
                        graphics2D.drawString(string3, n14, n15);
                        if (bl && n9 >= 2) {
                            long l5 = map2.getOrDefault(arrayList2.get(i), 0L);
                            long l6 = map2.getOrDefault(arrayList2.get(i - 1), 0L);
                            if (l6 > 0L) {
                                double d = (double) (l5 - l6) * 100.0 / (double) l6;
                                String string4 = d >= 0.0 ? "\u25b2" : "\u25bc";
                                String string5 = string4 + String.format("%.1f%%", Math.abs(d));
                                graphics2D.setFont(new Font("Segoe UI", 1, 9));
                                graphics2D.setColor(d >= 0.0 ? new Color(57, 255, 20) : new Color(255, 80, 80));
                                int n16 = graphics2D.getFontMetrics().stringWidth(string5);
                                graphics2D.drawString(string5,
                                        Math.max(n3, Math.min(nArray[i] - n16 / 2, n3 + n7 - n16)), n15 - 12);
                            }
                        }
                    }
                    String string6 = (String) arrayList2.get(i);
                    graphics2D.setFont(new Font("Segoe UI", 0, 8));
                    graphics2D.setColor(bl ? color2 : new Color(80, 100, 140));
                    int n17 = graphics2D.getFontMetrics().stringWidth(string6);
                    graphics2D.drawString(string6, Math.max(n3 - n17 / 2, nArray[i] - n17 / 2), n5 + n8 + 14);
                }
                graphics2D.setColor(new Color(60, 80, 120));
                graphics2D.setStroke(new BasicStroke(1.2f));
                graphics2D.drawLine(n3, n5, n3, n5 + n8);
                graphics2D.drawLine(n3, n5 + n8, n3 + n7, n5 + n8);
                graphics2D.dispose();
            }
        };
    }

    private JPanel makeStatRow(String string, String string2, Color color) {
        JPanel jPanel = new JPanel(new BorderLayout(4, 0)) {

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setColor(new Color(255, 255, 255, 5));
                graphics2D.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 6, 6);
                graphics2D.dispose();
            }
        };
        jPanel.setOpaque(false);
        jPanel.setBorder(new EmptyBorder(5, 8, 5, 8));
        jPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel jLabel = new JLabel(string);
        jLabel.setFont(new Font("Segoe UI", 0, 10));
        jLabel.setForeground(new Color(110, 130, 170));
        JLabel jLabel2 = new JLabel(string2);
        jLabel2.setFont(new Font("Segoe UI", 1, 11));
        jLabel2.setForeground(color);
        jPanel.add((Component) jLabel, "West");
        jPanel.add((Component) jLabel2, "East");
        return jPanel;
    }

    private long[] buildMonthDailyArray(long l, int n, Random random) {
        if (l <= 0L || n <= 0) {
            return new long[Math.max(n, 1)];
        }
        double[] dArray = new double[n];
        double d = 0.0;
        for (int i = 0; i < n; ++i) {
            double d2 = ((double) i + 1.0) / (double) n;
            double d3 = 4.0 * d2 * (1.0 - d2);
            dArray[i] = d3 + random.nextDouble() * 0.3;
            d += dArray[i];
        }
        long[] lArray = new long[n];
        long l2 = 0L;
        for (int i = 0; i < n - 1; ++i) {
            long l3 = Math.max(0L, Math.round((double) l * dArray[i] / d));
            lArray[i] = l2 += l3;
        }
        lArray[n - 1] = l;
        return lArray;
    }

    private static class NeonGlassCard extends JPanel {
        JLabel titleLbl, valueLbl, subLbl, iconLbl;
        private final Color accent;
        private float bloomAlpha = 0f;
        private float bloomTarget = 0f;
        private javax.swing.Timer bloomTimer;

        NeonGlassCard(Color accent) {
            this.accent = accent;
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);
            setBorder(new EmptyBorder(18, 20, 18, 20));

            iconLbl = new JLabel("—");
            iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));

            titleLbl = new JLabel("—");
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLbl.setForeground(new Color(200, 220, 255));

            valueLbl = new JLabel("—");
            valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 30));
            valueLbl.setForeground(accent);

            subLbl = new JLabel("—");
            subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            subLbl.setForeground(new Color(75, 100, 140));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(titleLbl, BorderLayout.WEST);
            top.add(iconLbl, BorderLayout.EAST);

            JPanel mid = new JPanel(new BorderLayout(0, 2));
            mid.setOpaque(false);
            mid.add(valueLbl, BorderLayout.NORTH);
            mid.add(subLbl, BorderLayout.CENTER);

            add(top, BorderLayout.NORTH);
            add(mid, BorderLayout.CENTER);

            // Bloom animation timer — runs at ~60fps while mouse is in/out
            bloomTimer = new javax.swing.Timer(16, e -> {
                float speed = 0.10f;
                bloomAlpha += (bloomTarget - bloomAlpha) * speed;
                if (Math.abs(bloomTarget - bloomAlpha) < 0.005f)
                    bloomAlpha = bloomTarget;
                repaint();
            });
            bloomTimer.start();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    bloomTarget = 1f;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    bloomTarget = 0f;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int W = getWidth(), H = getHeight();
            float ar = accent.getRed(), ag = accent.getGreen(), ab = accent.getBlue();

            // ── Outer neon bloom (increases on hover) ──────────────────────────
            if (bloomAlpha > 0.01f) {
                for (int radius = 28; radius >= 6; radius -= 4) {
                    float fraction = (28f - radius) / 22f;
                    int a = (int) (bloomAlpha * fraction * 55f);
                    g2.setColor(new Color((int) ar, (int) ag, (int) ab, a));
                    g2.setStroke(new BasicStroke(radius * 0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawRoundRect(-radius / 2, -radius / 2, W + radius, H + radius, 20 + radius, 20 + radius);
                }
            }

            // ── Glass body — semi-transparent (20, 25, 45, 150) ───────────────
            g2.setColor(new Color(20, 25, 45, 150));
            g2.fillRoundRect(0, 0, W, H, 16, 16);

            // ── Top-edge sheen: 1px LinearGradient White → Transparent ─────────
            LinearGradientPaint sheen = new LinearGradientPaint(
                    0f, 0f, 0f, H * 0.55f,
                    new float[] { 0f, 0.25f, 1f },
                    new Color[] { new Color(255, 255, 255, 55), new Color(255, 255, 255, 10),
                            new Color(255, 255, 255, 0) });
            g2.setPaint(sheen);
            g2.fillRoundRect(0, 0, W, H / 2, 16, 16);

            // ── Border glow — intensifies on hover ────────────────────────────
            int borderAlpha = (int) (55 + bloomAlpha * 100);
            g2.setColor(new Color((int) ar, (int) ag, (int) ab, borderAlpha));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, W - 1, H - 1, 16, 16);

            // ── Top accent stripe: gradient from accent → transparent ──────────
            LinearGradientPaint stripe = new LinearGradientPaint(
                    0f, 0f, (float) W, 0f,
                    new float[] { 0f, 0.6f, 1f },
                    new Color[] { accent, new Color((int) ar, (int) ag, (int) ab, 80),
                            new Color((int) ar, (int) ag, (int) ab, 0) });
            g2.setPaint(stripe);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(12, 1, W - 12, 1);

            g2.dispose();
            super.paintComponent(g); // paint children (labels)
        }
    }

    private JPanel buildRecentSalesWidget() {
        JPanel panel = glowCard("Recent Sales");
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Recent Sales");
        title.setFont(FONT_HEADER);
        title.setForeground(ACCENT_CYAN);
        panel.add(title, BorderLayout.NORTH);

        List<Sale> sales = saleController.getAllSales();
        JPanel list = new JPanel(new GridLayout(Math.min(sales.size() + 1, 6), 1, 0, 4));
        list.setOpaque(false);
        list.setBorder(new EmptyBorder(10, 0, 0, 0));

        int count = 0;
        for (Sale s : sales) {
            if (count++ >= 5)
                break;
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4, 0, 4, 0));
            String car = s.getCarName() != null ? s.getCarName() : "#" + s.getCarId();
            JLabel lbl = new JLabel("🚗 " + car);
            lbl.setFont(FONT_LABEL);
            lbl.setForeground(TEXT_PRIMARY);
            JLabel price = new JLabel("$" + formatMoney(s.getSalePrice()));
            price.setFont(FONT_HEADER);
            price.setForeground(ACCENT_GREEN);
            JLabel status = statusBadge(s.getStatus());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            right.setOpaque(false);
            right.add(status);
            right.add(price);
            row.add(lbl, BorderLayout.WEST);
            row.add(right, BorderLayout.EAST);
            list.add(row);
        }
        if (sales.isEmpty()) {
            JLabel empty = new JLabel("No sales recorded yet.");
            empty.setForeground(TEXT_MUTED);
            empty.setFont(FONT_LABEL);
            list.add(empty);
        }
        panel.add(list, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTopCarsWidget() {
        JPanel panel = glowCard("Inventory Spotlight");
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Inventory Spotlight");
        title.setFont(FONT_HEADER);
        title.setForeground(ACCENT_PURPLE);
        panel.add(title, BorderLayout.NORTH);

        List<Car> cars = carController.getAllCars();
        JPanel list = new JPanel(new GridLayout(Math.min(cars.size(), 5), 1, 0, 6));
        list.setOpaque(false);
        list.setBorder(new EmptyBorder(10, 0, 0, 0));

        int count = 0;
        for (Car c : cars) {
            if (count++ >= 5)
                break;
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel lbl = new JLabel(getCategoryIcon(c.getCategory()) + " " + c.getBrand() + " " + c.getModel());
            lbl.setFont(FONT_LABEL);
            lbl.setForeground(TEXT_PRIMARY);
            JLabel price = new JLabel("$" + formatMoney(c.getPrice()));
            price.setFont(FONT_HEADER);
            price.setForeground(ACCENT_CYAN);
            JLabel stock = new JLabel("  Stock: " + c.getStock());
            stock.setFont(FONT_SMALL);
            stock.setForeground(c.getStock() > 2 ? ACCENT_GREEN : ACCENT_ORANGE);
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            right.setOpaque(false);
            right.add(stock);
            right.add(price);
            row.add(lbl, BorderLayout.WEST);
            row.add(right, BorderLayout.EAST);
            list.add(row);
        }
        panel.add(list, BorderLayout.CENTER);
        return panel;
    }

    // ─── CARS PANEL ───────────────────────────────────────────────────────────
    private JPanel buildCarsPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = sectionTitle("🚗  Vehicle Inventory", ACCENT_CYAN);
        header.add(title, BorderLayout.WEST);
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        JButton priceAdvisorBtn = glowButton("💡 Price Advisor", new Color(255, 190, 30),
                e -> showPricingEngineDialog());
        JButton addBtn = glowButton("+ Add Vehicle", ACCENT_CYAN, e -> showAddCarDialog());
        headerRight.add(priceAdvisorBtn);
        headerRight.add(addBtn);
        header.add(headerRight, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        // Search + filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setOpaque(false);

        JTextField searchField = darkTextField("Search by brand or model...", 22);
        String[] categories = { "All", "Sedan", "SUV", "Sports", "Hatchback", "Electric" };
        JComboBox<String> catCombo = darkComboBox(categories);

        JButton searchBtn = glowButton("Search", ACCENT_CYAN, null);
        JButton clearBtn = smallButton("Clear", TEXT_MUTED);

        filterBar.add(new JLabel("  🔍"));
        filterBar.add(searchField);
        filterBar.add(catCombo);
        filterBar.add(searchBtn);
        filterBar.add(clearBtn);

        // Table
        String[] cols = { "ID", "Brand", "Model", "Year", "Category", "Color", "Fuel", "Trans.", "Price", "Stock",
                "Photos", "Status" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = buildFancyTable(model);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            private void filter() {
                String text = searchField.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(
                            javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });

        Runnable loadData = () -> {
            model.setRowCount(0);
            String q = searchField.getText().trim();
            String cat = (String) catCombo.getSelectedItem();
            List<Car> cars;
            if (q.isEmpty() && "All".equals(cat))
                cars = carController.getAllCars();
            else
                cars = carController.searchCars(q, cat, 0, Double.MAX_VALUE);
            for (Car c : cars) {
                model.addRow(new Object[] {
                        c.getId(), c.getBrand(), c.getModel(), c.getYear(), c.getCategory(),
                        c.getColor(), c.getFuelType(), c.getTransmission(),
                        "$" + formatMoney(c.getPrice()), c.getStock(),
                        (c.getImagePath() != null && !c.getImagePath().isEmpty()) ? "📷" : "—",
                        c.isAvailable() ? "Available" : "Sold Out"
                });
            }
            setStatus("Loaded " + model.getRowCount() + " vehicles");
        };

        searchBtn.addActionListener(e -> loadData.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            catCombo.setSelectedIndex(0);
            loadData.run();
        });
        loadData.run();

        JScrollPane tableScroll = styledScrollPane(table);

        // Action buttons below table
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        JButton editBtn = glowButton("✏ Edit", ACCENT_PURPLE, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            Car car = carController.getCarById(id);
            if (car != null)
                showEditCarDialog(car, loadData);
        });
        JButton deleteBtn = glowButton("🗑 Delete", ACCENT_PINK, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            String name = model.getValueAt(row, 1) + " " + model.getValueAt(row, 2);
            if (confirmDelete(name) && carController.deleteCar(id)) {
                loadData.run();
                showSuccess("Vehicle deleted.");
            }
        });
        JButton refreshBtn = smallButton("↻ Refresh", TEXT_MUTED);
        refreshBtn.addActionListener(e -> loadData.run());
        JButton photosBtn = glowButton("📷 Photos", new Color(60, 160, 255), e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            Car car = carController.getCarById(id);
            if (car != null)
                showCarGalleryDialog(car);
        });
        JButton customizeBtn = glowButton("🎨 Customizer", new Color(180, 80, 255), e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            Car car = carController.getCarById(id);
            if (car != null)
                showCarCustomizerDialog(car);
        });
        JButton featuresBtn = glowButton("⭐ Features", new Color(255, 180, 0), e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            Car car = carController.getCarById(id);
            if (car != null)
                showCarFeaturesDialog(car);
        });
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(photosBtn);
        actions.add(customizeBtn);
        actions.add(featuresBtn);
        actions.add(refreshBtn);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(filterBar, BorderLayout.NORTH);
        center.add(tableScroll, BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);

        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private void showAddCarDialog() {
        JDialog dlg = styledDialog("Add New Vehicle", 520, 640);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("🚗", "Add New Vehicle", ACCENT_CYAN), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField brand = darkTextField("e.g. Toyota", 18);
        JTextField modelF = darkTextField("e.g. Camry", 18);
        JTextField year = darkTextField("2024", 18);
        JTextField color = darkTextField("e.g. Midnight Silver", 18);
        JTextField price = darkTextField("e.g. 35000", 18);
        JTextField stock = darkTextField("e.g. 5", 18);
        JTextField engine = darkTextField("e.g. 2.0", 18);
        String[] cats = { "Sedan", "SUV", "Sports", "Hatchback", "Electric", "Luxury" };
        JComboBox<String> catBox = darkComboBox(cats);
        String[] fuels = { "Gasoline", "Hybrid", "Electric", "Diesel" };
        JComboBox<String> fuelBox = darkComboBox(fuels);
        String[] trans = { "Automatic", "Manual", "CVT", "DSG" };
        JComboBox<String> transBox = darkComboBox(trans);
        JTextArea featuresArea = new JTextArea(3, 18);
        featuresArea.setBackground(new Color(16, 22, 42));
        featuresArea.setForeground(TEXT_PRIMARY);
        featuresArea.setCaretColor(ACCENT_CYAN);
        featuresArea.setFont(FONT_LABEL);
        featuresArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        featuresArea.setLineWrap(true);
        featuresArea.setWrapStyleWord(true);
        JScrollPane featScroll = new JScrollPane(featuresArea);
        featScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 90)));
        styleScrollBar(featScroll.getVerticalScrollBar());

        addFormRow(form, gbc, "Brand *", brand, 0);
        addFormRow(form, gbc, "Model *", modelF, 1);
        addFormRow(form, gbc, "Year *", year, 2);
        addFormRow(form, gbc, "Color", color, 3);
        addFormRow(form, gbc, "Category", catBox, 4);
        addFormRow(form, gbc, "Fuel Type", fuelBox, 5);
        addFormRow(form, gbc, "Transmission", transBox, 6);
        addFormRow(form, gbc, "Engine Size (L)", engine, 7);
        addFormRow(form, gbc, "Price ($) *", price, 8);
        addFormRow(form, gbc, "Stock *", stock, 9);
        addFormRow(form, gbc, "Features (comma sep.)", featScroll, 10);

        // Multi-photo upload
        final java.util.List<String> selectedPhotoPaths = new java.util.ArrayList<>();
        JPanel photoRow = buildMultiPhotoPanel(dlg, selectedPhotoPaths);
        addFormRow(form, gbc, "Vehicle Photos", photoRow, 11);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setBackground(BG_PANEL);
        formScroll.getViewport().setBackground(BG_PANEL);
        styleScrollBar(formScroll.getVerticalScrollBar());
        dlg.add(formScroll, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Save Vehicle", ACCENT_CYAN, e -> {
            try {
                Car car = new Car();
                car.setBrand(brand.getText().trim());
                car.setModel(modelF.getText().trim());
                car.setYear(Integer.parseInt(year.getText().trim()));
                car.setColor(color.getText().trim());
                car.setPrice(Double.parseDouble(price.getText().trim()));
                car.setStock(Integer.parseInt(stock.getText().trim()));
                car.setCategory((String) catBox.getSelectedItem());
                car.setFuelType((String) fuelBox.getSelectedItem());
                car.setTransmission((String) transBox.getSelectedItem());
                car.setEngineSize(engine.getText().isEmpty() ? 0 : Double.parseDouble(engine.getText().trim()));
                car.setAvailable(car.getStock() > 0);
                String feat = featuresArea.getText().trim();
                if (!feat.isEmpty())
                    for (String f : feat.split(","))
                        car.addFeature(f.trim());
                if (car.getBrand().isEmpty() || car.getModel().isEmpty()) {
                    showInfo("Brand and Model are required.");
                    return;
                }
                car.setImagePath(selectedPhotoPaths.isEmpty() ? "" : String.join("|", selectedPhotoPaths));
                if (carController.addCar(car)) {
                    dlg.dispose();
                    showPanel("CARS");
                    showSuccess("Vehicle added!");
                } else
                    showError("Failed to add vehicle.");
            } catch (NumberFormatException ex) {
                showError("Check number fields (Year, Price, Stock).");
            }
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /**
     * Displays a stylish dialog showing all features of a given car.
     * Features are rendered as pill-style badge tags on a dark background.
     */
    private void showCarFeaturesDialog(Car car) {
        JDialog dlg = styledDialog("Features — " + car.getBrand() + " " + car.getModel(), 520, 420);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.add(buildDialogHeader("⭐", car.getBrand() + " " + car.getModel() + " (" + car.getYear() + ")",
                new java.awt.Color(255, 180, 0)), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(BG_PANEL);
        body.setBorder(new EmptyBorder(18, 24, 18, 24));

        // Car quick-info row
        JPanel infoRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        infoRow.setOpaque(false);
        java.util.function.BiFunction<String, java.awt.Color, JLabel> infoPill = (text, c) -> {
            JLabel l = new JLabel(text) {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                    g2.setStroke(new java.awt.BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
            l.setForeground(c);
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(4, 10, 4, 10));
            return l;
        };
        infoRow.add(infoPill.apply("📦 " + car.getCategory(), ACCENT_CYAN));
        infoRow.add(infoPill.apply("⛽ " + car.getFuelType(), new java.awt.Color(57, 255, 20)));
        infoRow.add(infoPill.apply("⚙ " + car.getTransmission(), new java.awt.Color(180, 130, 255)));
        infoRow.add(infoPill.apply("💰 $" + formatMoney(car.getPrice()), new java.awt.Color(255, 200, 0)));
        body.add(infoRow, BorderLayout.NORTH);

        // Features section
        java.util.List<String> features = car.getFeatures() != null ? car.getFeatures() : new java.util.ArrayList<>();
        JPanel featArea;
        if (features.isEmpty()) {
            featArea = new JPanel(new java.awt.GridBagLayout());
            featArea.setOpaque(false);
            JLabel empty = new JLabel("No features listed for this vehicle.");
            empty.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 13));
            empty.setForeground(TEXT_MUTED);
            featArea.add(empty);
        } else {
            // Wrap-layout panel of feature pills
            featArea = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 6));
            featArea.setOpaque(false);
            for (String feat : features) {
                if (feat == null || feat.isBlank())
                    continue;
                JLabel pill = new JLabel("✓ " + feat.trim()) {
                    @Override
                    protected void paintComponent(java.awt.Graphics g) {
                        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new java.awt.Color(255, 180, 0, 22));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        g2.setColor(new java.awt.Color(255, 180, 0, 70));
                        g2.setStroke(new java.awt.BasicStroke(1f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                pill.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
                pill.setForeground(new java.awt.Color(255, 210, 80));
                pill.setOpaque(false);
                pill.setBorder(new EmptyBorder(5, 11, 5, 11));
                featArea.add(pill);
            }
        }

        JLabel countLbl = new JLabel(features.size() + " feature" + (features.size() == 1 ? "" : "s"));
        countLbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
        countLbl.setForeground(TEXT_MUTED);
        countLbl.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setOpaque(false);
        centerPanel.add(countLbl, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(featArea);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(50, 30, 90)));
        sp.getViewport().setBackground(BG_DARK);
        sp.setBackground(BG_DARK);
        styleScrollBar(sp.getVerticalScrollBar());
        centerPanel.add(sp, BorderLayout.CENTER);
        body.add(centerPanel, BorderLayout.CENTER);

        dlg.add(body, BorderLayout.CENTER);
        JButton closeBtn = glowButton("✕ Close", ACCENT_CYAN, e -> dlg.dispose());
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 8));
        footer.setBackground(BG_PANEL);
        footer.setBorder(new EmptyBorder(0, 24, 0, 24));
        footer.add(closeBtn);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showEditCarDialog(Car car, Runnable onSave) {

        JDialog dlg = styledDialog("Edit Vehicle — " + car.getBrand() + " " + car.getModel(), 500, 520);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("✏", "Edit Vehicle", ACCENT_CYAN), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField brand = darkTextField(car.getBrand(), 18);
        brand.setText(car.getBrand());
        JTextField modelF = darkTextField(car.getModel(), 18);
        modelF.setText(car.getModel());
        JTextField year = darkTextField("", 18);
        year.setText(String.valueOf(car.getYear()));
        JTextField color = darkTextField(car.getColor(), 18);
        color.setText(car.getColor());
        JTextField price = darkTextField("", 18);
        price.setText(String.valueOf(car.getPrice()));
        JTextField stock = darkTextField("", 18);
        stock.setText(String.valueOf(car.getStock()));
        String[] cats = { "Sedan", "SUV", "Sports", "Hatchback", "Electric", "Luxury" };
        JComboBox<String> catBox = darkComboBox(cats);
        if (car.getCategory() != null)
            catBox.setSelectedItem(car.getCategory());
        String[] fuels = { "Gasoline", "Hybrid", "Electric", "Diesel" };
        JComboBox<String> fuelBox = darkComboBox(fuels);
        if (car.getFuelType() != null)
            fuelBox.setSelectedItem(car.getFuelType());
        String[] trans = { "Automatic", "Manual", "CVT", "DSG" };
        JComboBox<String> transBox = darkComboBox(trans);
        if (car.getTransmission() != null)
            transBox.setSelectedItem(car.getTransmission());
        JCheckBox available = new JCheckBox("In Stock", car.isAvailable());
        available.setFont(FONT_LABEL);
        available.setForeground(TEXT_PRIMARY);
        available.setOpaque(false);

        addFormRow(form, gbc, "Brand", brand, 0);
        addFormRow(form, gbc, "Model", modelF, 1);
        addFormRow(form, gbc, "Year", year, 2);
        addFormRow(form, gbc, "Color", color, 3);
        addFormRow(form, gbc, "Category", catBox, 4);
        addFormRow(form, gbc, "Fuel Type", fuelBox, 5);
        addFormRow(form, gbc, "Transmission", transBox, 6);
        addFormRow(form, gbc, "Price ($)", price, 7);
        addFormRow(form, gbc, "Stock", stock, 8);
        addFormRow(form, gbc, "Availability", available, 9);

        // Features field — pre-fill with existing features
        JTextArea eFeaturesArea = new JTextArea(3, 18);
        eFeaturesArea.setBackground(new Color(16, 22, 42));
        eFeaturesArea.setForeground(TEXT_PRIMARY);
        eFeaturesArea.setCaretColor(ACCENT_CYAN);
        eFeaturesArea.setFont(FONT_LABEL);
        eFeaturesArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        eFeaturesArea.setLineWrap(true);
        eFeaturesArea.setWrapStyleWord(true);
        if (car.getFeatures() != null && !car.getFeatures().isEmpty())
            eFeaturesArea.setText(String.join(", ", car.getFeatures()));
        JScrollPane eFeatScroll = new JScrollPane(eFeaturesArea);
        eFeatScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 90)));
        styleScrollBar(eFeatScroll.getVerticalScrollBar());
        addFormRow(form, gbc, "Features (comma sep.)", eFeatScroll, 10);

        // Multi-photo section for edit
        final java.util.List<String> editPhotoPaths = new java.util.ArrayList<>();
        if (car.getImagePath() != null && !car.getImagePath().isBlank()) {
            for (String p : car.getImagePath().split("\\|")) {
                if (!p.isBlank())
                    editPhotoPaths.add(p.trim());
            }
        }
        JPanel ephotoRow = buildMultiPhotoPanel(dlg, editPhotoPaths);
        addFormRow(form, gbc, "Vehicle Photos", ephotoRow, 11);

        JScrollPane eformScroll = new JScrollPane(form);
        eformScroll.setBorder(null);
        eformScroll.setBackground(BG_PANEL);
        eformScroll.getViewport().setBackground(BG_PANEL);
        styleScrollBar(eformScroll.getVerticalScrollBar());
        dlg.add(eformScroll, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Update Vehicle", ACCENT_CYAN, e -> {
            try {
                car.setBrand(brand.getText().trim());
                car.setModel(modelF.getText().trim());
                car.setYear(Integer.parseInt(year.getText().trim()));
                car.setColor(color.getText().trim());
                car.setPrice(Double.parseDouble(price.getText().trim()));
                car.setStock(Integer.parseInt(stock.getText().trim()));
                car.setCategory((String) catBox.getSelectedItem());
                car.setFuelType((String) fuelBox.getSelectedItem());
                car.setTransmission((String) transBox.getSelectedItem());
                car.setAvailable(available.isSelected());
                car.setImagePath(editPhotoPaths.isEmpty() ? "" : String.join("|", editPhotoPaths));
                // Update features
                car.getFeatures().clear();
                String eFeat = eFeaturesArea.getText().trim();
                if (!eFeat.isEmpty())
                    for (String f : eFeat.split(","))
                        car.addFeature(f.trim());
                if (carController.updateCar(car)) {
                    dlg.dispose();
                    showPanel("CARS");
                    onSave.run();
                    showSuccess("Vehicle updated!");
                } else
                    showError("Update failed.");
            } catch (NumberFormatException ex) {
                showError("Check number fields.");
            }
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── CUSTOMERS PANEL ──────────────────────────────────────────────────────
    private JPanel buildCustomersPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("👤  Customer Management", ACCENT_PURPLE), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("🤖 AI Match", new Color(180, 80, 255), e -> showSmartRecommenderDialog()));
        hr.add(glowButton("🚨 Churn Risk", new Color(255, 100, 50), e -> showChurnPredictionDialog()));
        hr.add(glowButton("+ Add Customer", ACCENT_PURPLE, e -> showAddCustomerDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setOpaque(false);
        JTextField search = darkTextField("Search by name, email or ID...", 28);
        JButton searchBtn = glowButton("Search", ACCENT_PURPLE, null);
        filterBar.add(new JLabel("  🔍"));
        filterBar.add(search);
        filterBar.add(searchBtn);

        // ── Inline-edit hint ──────────────────────────────────────────────────
        JLabel hint = new JLabel(
                "  ✏  Click any cell to edit inline  ·  Enter to save  ·  Esc to cancel  ·  Double-click for full dialog  ·  Col 0 (DB id) is read-only");
        hint.setFont(FONT_SMALL);
        hint.setForeground(new Color(90, 100, 140));
        filterBar.add(Box.createHorizontalStrut(16));
        filterBar.add(hint);

        // ── Columns: 0=ID(RO), 1=CustomerID(RO), 2–6=editable ────────────────
        String[] cols = { "ID", "Customer ID", "Name", "Email", "Phone", "Preferred Car", "Budget" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c >= 1; // col 0 (DB auto-id) is the only truly immutable key
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setValueAt(Object val, int r, int c) {
                // Capture old value BEFORE super changes it so we can revert on DB failure
                Object oldVal = getValueAt(r, c);
                super.setValueAt(val, r, c);
                if (r < 0 || c < 1)
                    return;

                // Always look up by the immutable DB integer id (col 0) —
                // safe even when the user is editing col 1 (Customer ID)
                int dbId = (int) getValueAt(r, 0);
                Customer found = null;
                for (Customer cu : customerController.getAllCustomers()) {
                    if (cu.getId() == dbId) {
                        found = cu;
                        break;
                    }
                }
                if (found == null)
                    return;

                String raw = (val != null ? val.toString() : "").trim();

                if (c == 1) {
                    // ── Customer ID rename ────────────────────────────────────
                    if (raw.isEmpty()) {
                        setStatus("✗  Customer ID cannot be empty");
                        // Revert display without recursion
                        ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                        fireTableCellUpdated(r, c);
                        return;
                    }
                    final String newCid = raw;
                    try {
                        java.sql.PreparedStatement rename = utils.DatabaseManager.getInstance()
                                .getConnection().prepareStatement(
                                        "UPDATE customers SET customer_id=? WHERE id=?");
                        rename.setString(1, newCid);
                        rename.setInt(2, dbId);
                        rename.executeUpdate();
                        found.setCustomerId(newCid);
                        setStatus("✓  Customer ID → " + newCid);
                    } catch (java.sql.SQLException ex) {
                        setStatus("✗  Rename failed (duplicate ID?): " + ex.getMessage());
                        ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                        fireTableCellUpdated(r, c);
                    }
                    return; // rename handled; skip the normal save path
                }

                switch (c) {
                    case 2:
                        found.setName(raw);
                        break;
                    case 3:
                        found.setEmail(raw);
                        break;
                    case 4:
                        found.setPhone(raw);
                        break;
                    case 5:
                        found.setPreferredCarType(raw);
                        break;
                    case 6:
                        try {
                            double parsed = Double.parseDouble(raw.replace("$", "").replace(",", ""));
                            found.setBudget(parsed);
                            super.setValueAt("$" + formatMoney(parsed), r, c);
                        } catch (NumberFormatException ex) {
                            setStatus("✗  Invalid budget — enter a number");
                            ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                            fireTableCellUpdated(r, c);
                            return;
                        }
                        break;
                    default:
                        return;
                }

                final Customer toSave = found;
                final DefaultTableModel self = this;
                if (customerController.updateCustomer(toSave)) {
                    setStatus("✓  Saved — " + toSave.getName());
                } else {
                    setStatus("✗  Save failed for " + toSave.getName());
                    ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                    fireTableCellUpdated(r, c);
                }
            }
        };

        JTable table = buildFancyTable(model);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            private void filter() {
                String text = search.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(
                            javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });

        // ── Single-click inline editor (styled with purple accent border) ──────
        JTextField inlineCustEditor = darkTextField("", 10);
        inlineCustEditor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_PURPLE, 1),
                new EmptyBorder(2, 6, 2, 6)));
        DefaultCellEditor custInlineEditor = new DefaultCellEditor(inlineCustEditor);
        custInlineEditor.setClickCountToStart(1); // one click opens the editor
        table.setDefaultEditor(Object.class, custInlineEditor);

        Runnable load = () -> {
            model.setRowCount(0);
            String q = search.getText().trim();
            List<Customer> list = q.isEmpty() ? customerController.getAllCustomers()
                    : customerController.searchCustomers(q);
            for (Customer c : list)
                model.addRow(new Object[] { c.getId(), c.getCustomerId(), c.getName(), c.getEmail(), c.getPhone(),
                        c.getPreferredCarType(), "$" + formatMoney(c.getBudget()) });
            setStatus("Loaded " + model.getRowCount() + " customers  ·  click any cell to edit inline");
        };
        searchBtn.addActionListener(e -> load.run());
        search.addActionListener(e -> load.run()); // Enter key in search box also triggers
        load.run();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        JButton edit = glowButton("✏ Full Edit", ACCENT_PURPLE, e -> {
            if (table.isEditing())
                table.getCellEditor().stopCellEditing();
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            String cid = (String) model.getValueAt(row, 1);
            Customer found = customerController.searchCustomers(cid).stream()
                    .filter(c -> c.getCustomerId().equals(cid)).findFirst().orElse(null);
            if (found == null) {
                showError("Could not load customer.");
                return;
            }
            showEditCustomerDialog(found, load);
        });
        JButton del = glowButton("🗑 Delete", ACCENT_PINK, e -> {
            if (table.isEditing())
                table.getCellEditor().stopCellEditing();
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            String cid = (String) model.getValueAt(row, 1);
            String name2 = (String) model.getValueAt(row, 2);
            if (confirmDelete(name2) && customerController.deleteCustomer(cid)) {
                load.run();
                showSuccess("Customer deleted.");
            }
        });
        JButton refresh = smallButton("↻ Refresh", TEXT_MUTED);
        refresh.addActionListener(e -> {
            if (table.isEditing())
                table.getCellEditor().cancelCellEditing();
            load.run();
        });
        // Double-click opens the full edit dialog (for Customer ID renames etc.)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2)
                    edit.doClick();
            }
        });
        actions.add(edit);
        actions.add(del);
        actions.add(refresh);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(filterBar, BorderLayout.NORTH);
        center.add(styledScrollPane(table), BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    /** Full edit dialog for an existing customer. */
    private void showEditCustomerDialog(Customer cust, Runnable onSave) {
        JDialog dlg = styledDialog("Edit Customer — " + cust.getName(), 480, 480);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("✏", "Edit Customer", ACCENT_PURPLE), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField name = darkTextField(cust.getName(), 18);
        JTextField email = darkTextField(cust.getEmail() != null ? cust.getEmail() : "", 18);
        JTextField phone = darkTextField(cust.getPhone() != null ? cust.getPhone() : "", 18);
        JTextField address = darkTextField(cust.getAddress() != null ? cust.getAddress() : "", 18);
        String[] carTypes = { "Sedan", "SUV", "Sports", "Hatchback", "Luxury", "Electric" };
        JComboBox<String> prefBox = darkComboBox(carTypes);
        if (cust.getPreferredCarType() != null)
            prefBox.setSelectedItem(cust.getPreferredCarType());
        JTextField budget = darkTextField(String.valueOf((long) cust.getBudget()), 18);
        // Editable Customer ID — keeping old ID in final for WHERE clause
        JTextField custIdField = darkTextField(cust.getCustomerId(), 18);

        addFormRow(form, gbc, "Customer ID", custIdField, 0);
        addFormRow(form, gbc, "Full Name *", name, 1);
        addFormRow(form, gbc, "Email", email, 2);
        addFormRow(form, gbc, "Phone", phone, 3);
        addFormRow(form, gbc, "Address", address, 4);
        addFormRow(form, gbc, "Preferred Car Type", prefBox, 5);
        addFormRow(form, gbc, "Budget ($)", budget, 6);

        JScrollPane fs = new JScrollPane(form);
        fs.setBorder(null);
        fs.setBackground(BG_PANEL);
        fs.getViewport().setBackground(BG_PANEL);
        styleScrollBar(fs.getVerticalScrollBar());
        dlg.add(fs, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        final String oldCustId = cust.getCustomerId(); // keep old ID for WHERE clause
        JButton save = glowButton("Update Customer", ACCENT_PURPLE, e -> {
            String newCid = custIdField.getText().trim();
            String nm = name.getText().trim();
            if (nm.isEmpty()) {
                showInfo("Name is required.");
                return;
            }
            if (newCid.isEmpty()) {
                showInfo("Customer ID is required.");
                return;
            }
            cust.setCustomerId(newCid);
            cust.setName(nm);
            cust.setEmail(email.getText().trim());
            cust.setPhone(phone.getText().trim());
            cust.setAddress(address.getText().trim());
            cust.setPreferredCarType((String) prefBox.getSelectedItem());
            try {
                cust.setBudget(Double.parseDouble(budget.getText().trim()));
            } catch (NumberFormatException ex) {
                cust.setBudget(0);
            }
            // If customer ID changed, run a direct SQL rename then update fields
            if (!newCid.equals(oldCustId)) {
                try {
                    java.sql.PreparedStatement rename = utils.DatabaseManager.getInstance().getConnection()
                            .prepareStatement("UPDATE customers SET customer_id=? WHERE customer_id=?");
                    rename.setString(1, newCid);
                    rename.setString(2, oldCustId);
                    rename.executeUpdate();
                } catch (java.sql.SQLException ex) {
                    showError("Could not rename Customer ID: " + ex.getMessage());
                    return;
                }
            }
            if (customerController.updateCustomer(cust)) {
                dlg.dispose();
                onSave.run();
                showSuccess("Customer updated!");
            } else
                showError("Update failed.");
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /**
     * Dark-themed, OTP-verified Reset Password dialog.
     * Step 1: Sends OTP to admin's registered phone.
     * Step 2 (after OTP verified): Enters and confirms new password.
     */
    private void showOtpResetPasswordDialog(String targetUser) {
        // Find the admin's phone for OTP (we send OTP to the currently-logged-in admin)
        String adminPhone = utils.DatabaseManager.getInstance().getUserPhone(utils.SessionManager.getUser());
        boolean has2fa = adminPhone != null && !adminPhone.isBlank();

        if (has2fa) {
            // ── Step 1: Send OTP ─────────────────────────────────────────────
            utils.OtpService.generateAndSend(utils.SessionManager.getUser(), adminPhone);

            JDialog otpDlg = styledDialog("Verify Identity", 420, 360);
            otpDlg.setLayout(new BorderLayout());
            otpDlg.add(buildDialogHeader("🔐", "Verify Identity — Reset Password", ACCENT_ORANGE), BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(BG_PANEL);
            form.setBorder(new EmptyBorder(20, 28, 20, 28));
            GridBagConstraints gc = formGbc();

            JLabel info = new JLabel(
                    "<html><font color='#5090c0'>An OTP has been sent to your registered phone.<br/>Enter it below to authorize this password reset.</font></html>");
            info.setFont(FONT_SMALL);
            gc.gridx = 0;
            gc.gridy = 0;
            gc.gridwidth = 2;
            form.add(info, gc);
            gc.gridwidth = 1;

            JTextField otpField = darkTextField("6-digit OTP", 20);
            addFormRow(form, gc, "OTP Code", otpField, 1);

            JLabel errLbl = new JLabel(" ");
            errLbl.setFont(FONT_SMALL);
            errLbl.setForeground(ACCENT_PINK);
            gc.gridx = 0;
            gc.gridy = 2;
            gc.gridwidth = 2;
            form.add(errLbl, gc);
            gc.gridwidth = 1;

            otpDlg.add(form, BorderLayout.CENTER);

            JButton cancel = smallButton("Cancel", TEXT_MUTED);
            cancel.addActionListener(ex -> otpDlg.dispose());
            final boolean[] otpVerified = { false };
            JButton verify = glowButton("✔ Verify OTP", ACCENT_ORANGE, ex -> {
                if (utils.OtpService.verify(utils.SessionManager.getUser(), otpField.getText().trim())) {
                    otpVerified[0] = true;
                    otpDlg.dispose();
                } else {
                    errLbl.setText("Invalid or expired OTP — try again.");
                }
            });
            otpDlg.add(buildDialogFooter(cancel, verify), BorderLayout.SOUTH);
            otpDlg.setVisible(true); // blocks

            if (!otpVerified[0])
                return; // cancelled or wrong OTP
        }

        // ── Step 2: Enter new password ────────────────────────────────────────
        JDialog pwDlg = styledDialog("Reset Password — " + targetUser, 420, 360);
        pwDlg.setLayout(new BorderLayout());
        pwDlg.add(buildDialogHeader("🔑", "Reset Password for " + targetUser, ACCENT_ORANGE), BorderLayout.NORTH);

        JPanel form2 = new JPanel(new GridBagLayout());
        form2.setBackground(BG_PANEL);
        form2.setBorder(new EmptyBorder(20, 28, 20, 28));
        GridBagConstraints gc2 = formGbc();

        JPasswordField newPw = new JPasswordField(20);
        JPasswordField confPw = new JPasswordField(20);
        for (JPasswordField pf : new JPasswordField[] { newPw, confPw }) {
            pf.setBackground(new Color(20, 28, 55));
            pf.setForeground(TEXT_PRIMARY);
            pf.setCaretColor(ACCENT_CYAN);
            pf.setFont(FONT_LABEL);
            pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_GLOW, 1), new EmptyBorder(8, 12, 8, 12)));
        }

        addFormRow(form2, gc2, "New Password", newPw, 0);
        addFormRow(form2, gc2, "Confirm", confPw, 1);

        JLabel err2 = new JLabel(" ");
        err2.setFont(FONT_SMALL);
        err2.setForeground(ACCENT_PINK);
        gc2.gridx = 0;
        gc2.gridy = 2;
        gc2.gridwidth = 2;
        form2.add(err2, gc2);

        pwDlg.add(form2, BorderLayout.CENTER);

        JButton cancel2 = smallButton("Cancel", TEXT_MUTED);
        cancel2.addActionListener(ex -> pwDlg.dispose());
        JButton save = glowButton("💾 Save Password", ACCENT_CYAN, ex -> {
            String pw1 = new String(newPw.getPassword());
            String pw2 = new String(confPw.getPassword());
            if (pw1.length() < 4) {
                err2.setText("Password must be at least 4 characters.");
                return;
            }
            if (!pw1.equals(pw2)) {
                err2.setText("Passwords do not match.");
                return;
            }
            if (utils.DatabaseManager.getInstance().resetAppUserPassword(targetUser, pw1)) {
                showSuccess("✅ Password reset for " + targetUser);
                utils.AuditLogger.log("UPDATE", "AppUser " + targetUser,
                        "Password reset by " + utils.SessionManager.getUser());
                pwDlg.dispose();
            } else {
                err2.setText("Reset failed — please try again.");
            }
        });
        pwDlg.add(buildDialogFooter(cancel2, save), BorderLayout.SOUTH);
        pwDlg.setVisible(true);
    }

    /** Styled "Add App User" dialog matching the customer/employee dialog style. */
    private void showAddAppUserDialog(Runnable onSuccess) {
        JDialog dlg = styledDialog("Add App User", 480, 520);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("👤", "Add App User", new Color(100, 200, 255)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField unField = darkTextField("Required", 20);
        JPasswordField pwField = new JPasswordField(20);
        pwField.setBackground(new Color(20, 28, 55));
        pwField.setForeground(TEXT_PRIMARY);
        pwField.setCaretColor(ACCENT_CYAN);
        pwField.setFont(FONT_LABEL);
        pwField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GLOW, 1), new EmptyBorder(8, 12, 8, 12)));
        JComboBox<String> roleBox = new JComboBox<>(utils.SessionManager.Role.DB_VALUES);
        roleBox.setBackground(new Color(20, 28, 55));
        roleBox.setForeground(TEXT_PRIMARY);
        roleBox.setFont(FONT_LABEL);
        JTextField emailField = darkTextField("Optional", 20);
        JTextField phoneField = darkTextField("+91... (optional)", 20);

        addFormRow(form, gbc, "Username *", unField, 0);
        addFormRow(form, gbc, "Password *", pwField, 1);
        addFormRow(form, gbc, "Role", roleBox, 2);
        addFormRow(form, gbc, "Email", emailField, 3);
        addFormRow(form, gbc, "Phone", phoneField, 4);

        JLabel note = new JLabel(
                "<html><font color='#5070a0'>★ Username and password will be used to log in to the app.</font></html>");
        note.setFont(FONT_SMALL);
        note.setBorder(new EmptyBorder(12, 0, 0, 0));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        form.add(note, gbc);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(BG_PANEL);
        dlg.add(formScroll, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        foot.setBackground(BG_CARD);
        foot.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_GLOW));
        JButton cancel = glowButton("Cancel", TEXT_MUTED, e2 -> dlg.dispose());
        JButton save = glowButton("✔ Create User", ACCENT_CYAN, e2 -> {
            String un = unField.getText().trim();
            String pw = new String(pwField.getPassword());
            if (un.isEmpty() || pw.isEmpty()) {
                showError("Username and password are required.");
                return;
            }
            if (pw.length() < 4) {
                showError("Password must be at least 4 characters.");
                return;
            }
            String role = (String) roleBox.getSelectedItem();
            if (utils.DatabaseManager.getInstance().addAppUser(un, pw, role, emailField.getText().trim(),
                    phoneField.getText().trim())) {
                onSuccess.run();
                showSuccess("User '" + un + "' created with role " + role + ".");
                utils.AuditLogger.log("CREATE", "AppUser " + un, "Role: " + role);
                dlg.dispose();
            } else {
                showError("Username already taken — choose a different one.");
            }
        });
        foot.add(cancel);
        foot.add(save);
        dlg.add(foot, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showAddCustomerDialog() {
        JDialog dlg = styledDialog("Register New Customer", 480, 530);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("👤", "Register New Customer", ACCENT_PURPLE), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField custId = darkTextField("e.g. CUST010", 18);
        JTextField name = darkTextField("Full name", 18);
        JTextField email = darkTextField("email@example.com", 18);
        JTextField phone = darkTextField("555-0000", 18);
        JTextField address = darkTextField("Street, City", 18);
        String[] carTypes = { "Sedan", "SUV", "Sports", "Hatchback", "Luxury", "Electric" };
        JComboBox<String> prefBox = darkComboBox(carTypes);
        JTextField budget = darkTextField("e.g. 45000", 18);

        addFormRow(form, gbc, "Customer ID *", custId, 0);
        addFormRow(form, gbc, "Full Name *", name, 1);
        addFormRow(form, gbc, "Email", email, 2);
        addFormRow(form, gbc, "Phone", phone, 3);

        // ── Twilio Verification Notice ─────────────────────────────────────────
        JLabel twilioHint = new JLabel(
                "<html><span style='color:#FFC107'>" +
                        "⚠  To send SMS to this customer, you must first verify their number at<br>" +
                        "<u style='color:#00D4FF'>console.twilio.com → Phone Numbers → Verified Caller IDs</u>" +
                        "</span></html>");
        twilioHint.setFont(FONT_SMALL);
        twilioHint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        twilioHint.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(
                            java.net.URI
                                    .create("https://console.twilio.com/us1/develop/phone-numbers/manage/verified"));
                } catch (Exception ignored) {
                }
            }
        });
        GridBagConstraints hIntGbc = formGbc();
        hIntGbc.gridy = 4;
        hIntGbc.gridx = 0;
        hIntGbc.gridwidth = 2;
        hIntGbc.insets = new Insets(2, 0, 10, 0);
        form.add(twilioHint, hIntGbc);

        addFormRow(form, gbc, "Address", address, 5);
        addFormRow(form, gbc, "Preferred Car Type", prefBox, 6);
        addFormRow(form, gbc, "Budget ($)", budget, 7);

        dlg.add(form, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Register Customer", ACCENT_PURPLE, e -> {
            Customer c = new Customer();
            c.setCustomerId(custId.getText().trim());
            c.setName(name.getText().trim());
            c.setEmail(email.getText().trim());
            c.setPhone(phone.getText().trim());
            c.setAddress(address.getText().trim());
            c.setRegistrationDate(new Date());
            c.setPreferredCarType((String) prefBox.getSelectedItem());
            try {
                c.setBudget(Double.parseDouble(budget.getText().trim()));
            } catch (NumberFormatException ex) {
                c.setBudget(0);
            }
            if (c.getCustomerId().isEmpty() || c.getName().isEmpty()) {
                showInfo("Customer ID and Name required.");
                return;
            }
            if (customerController.addCustomer(c)) {
                dlg.dispose();
                showPanel("CUSTOMERS");
                showSuccess("Customer registered!");
            } else
                showError("Failed to register customer.");
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── EMPLOYEES PANEL ──────────────────────────────────────────────────────
    private JPanel buildEmployeesPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("👔  Team Management", new Color(64, 200, 255)), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("+ Add Employee", new Color(64, 200, 255), e -> showAddEmployeeDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        String[] cols = { "ID", "Emp. ID", "Name", "Email", "Phone", "Department", "Designation", "Salary" };

        // ── Inline-saving model — setValueAt commits directly to DB ──────────
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c >= 1; // col 0 (DB auto-id) is the only immutable key
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setValueAt(Object val, int r, int c) {
                // Capture old value BEFORE super changes it so we can revert on DB failure
                Object oldVal = getValueAt(r, c);
                super.setValueAt(val, r, c);
                if (r < 0 || c < 1)
                    return;

                // Always look up by the immutable DB integer id (col 0) —
                // safe even when the user is editing col 1 (Employee ID)
                int dbId = (int) getValueAt(r, 0);
                Employee found = null;
                for (Employee em : employeeController.getAllEmployees()) {
                    if (em.getId() == dbId) {
                        found = em;
                        break;
                    }
                }
                if (found == null)
                    return;

                String raw = (val != null ? val.toString() : "").trim();

                if (c == 1) {
                    // ── Employee ID rename ────────────────────────────────────
                    if (raw.isEmpty()) {
                        setStatus("✗  Employee ID cannot be empty");
                        ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                        fireTableCellUpdated(r, c);
                        return;
                    }
                    final String newEid = raw;
                    try {
                        java.sql.PreparedStatement rename = utils.DatabaseManager.getInstance()
                                .getConnection().prepareStatement(
                                        "UPDATE employees SET employee_id=? WHERE id=?");
                        rename.setString(1, newEid);
                        rename.setInt(2, dbId);
                        rename.executeUpdate();
                        found.setEmployeeId(newEid);
                        setStatus("✓  Employee ID → " + newEid);
                    } catch (java.sql.SQLException ex) {
                        setStatus("✗  Rename failed (duplicate ID?): " + ex.getMessage());
                        ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                        fireTableCellUpdated(r, c);
                    }
                    return; // rename handled; skip the normal save path
                }

                switch (c) {
                    case 2:
                        found.setName(raw);
                        break;
                    case 3:
                        found.setEmail(raw);
                        break;
                    case 4:
                        found.setPhone(raw);
                        break;
                    case 5:
                        found.setDepartment(raw);
                        break;
                    case 6:
                        found.setDesignation(raw);
                        break;
                    case 7:
                        try {
                            double parsed = Double.parseDouble(raw.replace("$", "").replace(",", ""));
                            found.setSalary(parsed);
                            super.setValueAt("$" + formatMoney(parsed), r, c);
                        } catch (NumberFormatException ex) {
                            setStatus("✗  Invalid salary — enter a number");
                            ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                            fireTableCellUpdated(r, c);
                            return;
                        }
                        break;
                    default:
                        return;
                }

                final Employee toSave = found;
                final DefaultTableModel self = this;
                if (employeeController.updateEmployee(toSave)) {
                    setStatus("✓  Saved — " + toSave.getName());
                } else {
                    setStatus("✗  Save failed for " + toSave.getName());
                    ((java.util.Vector) getDataVector().get(r)).set(c, oldVal);
                    fireTableCellUpdated(r, c);
                }
            }
        };

        JTable table = buildFancyTable(model);

        Runnable load = () -> {
            model.setRowCount(0);
            for (Employee e : employeeController.getAllEmployees())
                model.addRow(new Object[] { e.getId(), e.getEmployeeId(), e.getName(), e.getEmail(), e.getPhone(),
                        e.getDepartment(), e.getDesignation(), "$" + formatMoney(e.getSalary()) });
            setStatus("Loaded " + model.getRowCount() + " employees  ·  click any cell to edit inline");
        };
        load.run();

        // ── Single-click inline editor (cyan accent border) ───────────────────
        final Color EMP_ACCENT = new Color(64, 200, 255);
        JTextField inlineEmpEditor = darkTextField("", 10);
        inlineEmpEditor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EMP_ACCENT, 1),
                new EmptyBorder(2, 6, 2, 6)));
        DefaultCellEditor empInlineEditor = new DefaultCellEditor(inlineEmpEditor);
        empInlineEditor.setClickCountToStart(1);
        table.setDefaultEditor(Object.class, empInlineEditor);

        // ── Search bar (live-filters from DB, compatible with inline saves) ───
        JPanel filterBar = new JPanel(new BorderLayout(8, 0));
        filterBar.setOpaque(false);
        JTextField searchBox = darkTextField("🔍  Search by name, ID or department...", 30);
        searchBox.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void filter() {
                if (table.isEditing())
                    table.getCellEditor().stopCellEditing();
                String q = searchBox.getText().trim().toLowerCase();
                model.setRowCount(0);
                for (Employee e : employeeController.getAllEmployees()) {
                    if (q.isEmpty() || e.getName().toLowerCase().contains(q)
                            || e.getEmployeeId().toLowerCase().contains(q)
                            || e.getDepartment().toLowerCase().contains(q)) {
                        model.addRow(new Object[] { e.getId(), e.getEmployeeId(), e.getName(),
                                e.getEmail(), e.getPhone(), e.getDepartment(),
                                e.getDesignation(), "$" + formatMoney(e.getSalary()) });
                    }
                }
                setStatus(model.getRowCount() + " employees found  ·  click any cell to edit inline");
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }
        });

        // Hint label beside the search box
        JLabel empHint = new JLabel(
                "  ✏  Click any cell to edit inline  ·  Enter to save  ·  Esc to cancel  ·  Double-click for full dialog  ·  Col 0 (DB id) is read-only");
        empHint.setFont(FONT_SMALL);
        empHint.setForeground(new Color(90, 100, 140));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchBox);
        searchRow.add(empHint);
        filterBar.add(searchRow, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        JButton editEmp = glowButton("✏ Full Edit", new Color(64, 200, 255), e -> {
            if (table.isEditing())
                table.getCellEditor().stopCellEditing();
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select an employee to edit.");
                return;
            }
            String eid = (String) model.getValueAt(row, 1);
            Employee found = employeeController.getAllEmployees().stream()
                    .filter(emp -> emp.getEmployeeId().equals(eid)).findFirst().orElse(null);
            if (found == null) {
                showError("Could not load employee.");
                return;
            }
            showEditEmployeeDialog(found, load);
        });
        JButton del = glowButton("🗑 Delete", ACCENT_PINK, e -> {
            if (table.isEditing())
                table.getCellEditor().stopCellEditing();
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select an employee.");
                return;
            }
            String eid = (String) model.getValueAt(row, 1);
            String n = (String) model.getValueAt(row, 2);
            if (confirmDelete(n) && employeeController.deleteEmployee(eid)) {
                load.run();
                showSuccess("Employee removed.");
            }
        });
        JButton refresh = smallButton("↻ Refresh", TEXT_MUTED);
        refresh.addActionListener(e -> {
            if (table.isEditing())
                table.getCellEditor().cancelCellEditing();
            load.run();
            searchBox.setText("");
        });
        // Double-click opens the full edit dialog
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2)
                    editEmp.doClick();
            }
        });
        actions.add(editEmp);
        actions.add(del);
        actions.add(refresh);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(filterBar, BorderLayout.NORTH);
        center.add(styledScrollPane(table), BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    /** Full edit dialog for an existing employee — all fields editable. */
    private void showEditEmployeeDialog(Employee emp, Runnable onSave) {
        JDialog dlg = styledDialog("Edit Team Member — " + emp.getName(), 500, 500);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("✏", "Edit Team Member", new Color(64, 200, 255)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        // Employee ID — read-only (used as DB key for updates)
        JLabel eidLbl = new JLabel(emp.getEmployeeId());
        eidLbl.setFont(FONT_LABEL);
        eidLbl.setForeground(TEXT_MUTED);

        JTextField name = darkTextField(emp.getName() != null ? emp.getName() : "", 18);
        JTextField email = darkTextField(emp.getEmail() != null ? emp.getEmail() : "", 18);
        JTextField phone = darkTextField(emp.getPhone() != null ? emp.getPhone() : "", 18);
        JTextField address = darkTextField(emp.getAddress() != null ? emp.getAddress() : "", 18);
        String[] depts = { "Sales", "Service", "Finance", "Marketing", "Operations" };
        JComboBox<String> deptBox = darkComboBox(depts);
        if (emp.getDepartment() != null)
            deptBox.setSelectedItem(emp.getDepartment());
        JTextField designation = darkTextField(emp.getDesignation() != null ? emp.getDesignation() : "", 18);
        JTextField salary = darkTextField(String.valueOf((long) emp.getSalary()), 18);

        addFormRow(form, gbc, "Employee ID", eidLbl, 0);
        addFormRow(form, gbc, "Full Name *", name, 1);
        addFormRow(form, gbc, "Email", email, 2);
        addFormRow(form, gbc, "Phone", phone, 3);
        addFormRow(form, gbc, "Address", address, 4);
        addFormRow(form, gbc, "Department", deptBox, 5);
        addFormRow(form, gbc, "Designation", designation, 6);
        addFormRow(form, gbc, "Salary ($)", salary, 7);

        JScrollPane fs = new JScrollPane(form);
        fs.setBorder(null);
        fs.setBackground(BG_PANEL);
        fs.getViewport().setBackground(BG_PANEL);
        styleScrollBar(fs.getVerticalScrollBar());
        dlg.add(fs, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Update Member", new Color(64, 200, 255), e -> {
            String nm = name.getText().trim();
            if (nm.isEmpty()) {
                showInfo("Name is required.");
                return;
            }
            emp.setName(nm);
            emp.setEmail(email.getText().trim());
            emp.setPhone(phone.getText().trim());
            emp.setAddress(address.getText().trim());
            emp.setDepartment((String) deptBox.getSelectedItem());
            emp.setDesignation(designation.getText().trim());
            try {
                emp.setSalary(Double.parseDouble(salary.getText().trim()));
            } catch (NumberFormatException ex) {
                emp.setSalary(0);
            }
            if (employeeController.updateEmployee(emp)) {
                dlg.dispose();
                onSave.run();
                showSuccess("Team member updated!");
            } else
                showError("Update failed.");
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showAddEmployeeDialog() {
        JDialog dlg = styledDialog("Add Team Member", 480, 530);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("👔", "Add Team Member", new Color(64, 200, 255)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField empId = darkTextField("e.g. EMP010", 18);
        JTextField name = darkTextField("Full name", 18);
        JTextField email = darkTextField("email@autoelite.com", 18);
        JTextField phone = darkTextField("555-0000", 18);
        String[] depts = { "Sales", "Service", "Finance", "Marketing", "Operations" };
        JComboBox<String> deptBox = darkComboBox(depts);
        JTextField designation = darkTextField("e.g. Sales Executive", 18);
        JTextField salary = darkTextField("e.g. 48000", 18);

        addFormRow(form, gbc, "Employee ID *", empId, 0);
        addFormRow(form, gbc, "Full Name *", name, 1);
        addFormRow(form, gbc, "Email", email, 2);
        addFormRow(form, gbc, "Phone", phone, 3);
        addFormRow(form, gbc, "Department", deptBox, 4);
        addFormRow(form, gbc, "Designation", designation, 5);
        addFormRow(form, gbc, "Salary ($)", salary, 6);

        dlg.add(form, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Add Member", new Color(64, 200, 255), e -> {
            Employee emp = new Employee();
            emp.setEmployeeId(empId.getText().trim());
            emp.setName(name.getText().trim());
            emp.setEmail(email.getText().trim());
            emp.setPhone(phone.getText().trim());
            emp.setDepartment((String) deptBox.getSelectedItem());
            emp.setDesignation(designation.getText().trim());
            emp.setJoiningDate(new Date());
            try {
                emp.setSalary(Double.parseDouble(salary.getText().trim()));
            } catch (NumberFormatException ex) {
                emp.setSalary(0);
            }
            if (emp.getEmployeeId().isEmpty() || emp.getName().isEmpty()) {
                showInfo("Employee ID and Name required.");
                return;
            }
            if (employeeController.addEmployee(emp)) {
                dlg.dispose();
                showPanel("EMPLOYEES");
                showSuccess("Employee added!");
            } else
                showError("Failed to add employee.");
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── SALES PANEL ──────────────────────────────────────────────────────────
    private JPanel buildSalesPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("💰  Sales Management", ACCENT_GREEN), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("\ud83e\udde0 AI Sales Coach", new Color(50, 200, 150), e -> showSalesCoachDialog()));
        hr.add(glowButton("+ Record Sale", ACCENT_GREEN, e -> showAddSaleDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        // Revenue summary
        double rev = saleController.getTotalRevenue();
        int salesCount = saleController.getTotalSales();
        JPanel summary = new JPanel(new GridLayout(1, 3, 12, 0));
        summary.setOpaque(false);
        summary.setPreferredSize(new Dimension(0, 85));
        summary.add(miniStatCard("Total Revenue", "$" + formatMoney(rev), ACCENT_GREEN));
        summary.add(miniStatCard("Completed Sales", String.valueOf(salesCount), ACCENT_CYAN));
        summary.add(miniStatCard("Avg. Sale Price", salesCount > 0 ? "$" + formatMoney(rev / salesCount) : "N/A",
                ACCENT_PURPLE));

        String[] cols = { "ID", "Vehicle", "Customer", "Employee", "Date", "Amount", "Payment", "Status" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = buildFancyTable(model);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setOpaque(false);
        topContainer.add(summary, BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setOpaque(false);
        JTextField searchField = darkTextField("Search sales...", 28);
        filterBar.add(new JLabel("  🔍"));
        filterBar.add(searchField);
        topContainer.add(filterBar, BorderLayout.SOUTH);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            private void filter() {
                String text = searchField.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(
                            javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });

        Runnable load = () -> {
            model.setRowCount(0);
            for (Sale s : saleController.getAllSales())
                model.addRow(new Object[] { s.getId(), s.getCarName() != null ? s.getCarName() : "Car#" + s.getCarId(),
                        s.getCustomerName() != null ? s.getCustomerName() : "Cust#" + s.getCustomerId(),
                        s.getEmployeeName() != null ? s.getEmployeeName() : "Emp#" + s.getEmployeeId(),
                        s.getSaleDate(), "$" + formatMoney(s.getSalePrice()), s.getPaymentMethod(), s.getStatus() });
            setStatus("Loaded " + model.getRowCount() + " sales records");
        };
        load.run();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        JButton complete = glowButton("✓ Mark Complete", ACCENT_GREEN, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            if (saleController.updateSaleStatus(id, "Completed")) {
                // Record commission + fire SMS
                try {
                    java.sql.Connection cc = utils.DatabaseManager.getInstance().getConnection();
                    java.sql.ResultSet sr = cc.createStatement().executeQuery(
                            "SELECT s.sale_price, c.phone, c.name, " +
                                    "car.brand, car.model, e.employee_id as emp_str_id " +
                                    "FROM sales s " +
                                    "JOIN customers c ON s.customer_id=c.id " +
                                    "JOIN cars car ON s.car_id=car.id " +
                                    "JOIN employees e ON s.employee_id=e.id " +
                                    "WHERE s.id=" + id);
                    if (sr.next()) {
                        commissionController.recordCommission(id, sr.getString("emp_str_id"),
                                sr.getDouble("sale_price"));
                        utils.TwilioService.notifySaleCompleted(sr.getString("phone"), sr.getString("name"),
                                sr.getString("brand"), sr.getString("model"), sr.getDouble("sale_price"));
                    }
                } catch (Exception ignored) {
                }
                load.run();
                showSuccess("Sale marked complete.");
            }
        });
        JButton cancel2 = glowButton("✕ Cancel Sale", ACCENT_PINK, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            if (saleController.updateSaleStatus(id, "Cancelled")) {
                load.run();
                showSuccess("Sale cancelled.");
            }
        });
        JButton refresh = smallButton("↻ Refresh", TEXT_MUTED);
        refresh.addActionListener(e -> load.run());
        JButton invoice = glowButton("🧾 View Invoice", ACCENT_CYAN, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            showInvoiceDialog(model, row);
        });
        actions.add(complete);
        actions.add(cancel2);
        actions.add(invoice);
        actions.add(refresh);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(topContainer, BorderLayout.NORTH);
        center.add(styledScrollPane(table), BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private void showAddSaleDialog() {
        JDialog dlg = styledDialog("Record New Sale", 560, 560);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("💰", "Record New Sale", ACCENT_GREEN), BorderLayout.NORTH);

        // ── Tab 1: Sale Details ───────────────────────────────────────────────
        JPanel saleForm = new JPanel(new GridBagLayout());
        saleForm.setBackground(BG_PANEL);
        saleForm.setBorder(new EmptyBorder(18, 24, 12, 24));
        GridBagConstraints gbc = formGbc();

        List<Car> cars = carController.getAllCars();
        List<Customer> custs = customerController.getAllCustomers();
        List<Employee> emps = employeeController.getAllEmployees();

        JComboBox<String> carBox = darkComboBox(
                cars.stream().map(c -> c.getId() + ": " + c.getBrand() + " " + c.getModel()).toArray(String[]::new));
        JComboBox<String> custBox = darkComboBox(
                custs.stream().map(c -> c.getId() + ": " + c.getName()).toArray(String[]::new));
        JComboBox<String> empBox = darkComboBox(
                emps.stream().map(e -> e.getId() + ": " + e.getName()).toArray(String[]::new));
        JTextField price = darkTextField("Sale price", 18);
        String[] payments = { "Bank Finance", "Cash", "Lease", "Credit Card", "Crypto" };
        JComboBox<String> payBox = darkComboBox(payments);

        addFormRow(saleForm, gbc, "Vehicle *", carBox, 0);
        addFormRow(saleForm, gbc, "Customer *", custBox, 1);
        addFormRow(saleForm, gbc, "Employee *", empBox, 2);
        addFormRow(saleForm, gbc, "Sale Price ($) *", price, 3);
        addFormRow(saleForm, gbc, "Payment Method", payBox, 4);

        // ── Tab 2: EMI / Finance ──────────────────────────────────────────────
        JPanel emiForm = new JPanel(new GridBagLayout());
        emiForm.setBackground(BG_PANEL);
        emiForm.setBorder(new EmptyBorder(16, 24, 8, 24));
        GridBagConstraints eg = formGbc();

        JTextField dpField = darkTextField("e.g. 50000", 18);
        JTextField rateField = darkTextField("e.g. 8.5", 18);
        JTextField tenureField = darkTextField("e.g. 60", 18);

        // EMI result label
        JLabel emiResult = new JLabel("—  Fill fields above and click Calculate");
        emiResult.setFont(FONT_HEADER);
        emiResult.setForeground(ACCENT_GREEN);

        // Amortization table
        String[] amCols = { "#", "EMI ($)", "Principal ($)", "Interest ($)", "Balance ($)" };
        javax.swing.table.DefaultTableModel amModel = new javax.swing.table.DefaultTableModel(amCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable amTable = buildFancyTable(amModel);
        JScrollPane amScroll = new JScrollPane(amTable);
        amScroll.setPreferredSize(new java.awt.Dimension(0, 130));
        amScroll.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(40, 60, 100)));
        amScroll.getViewport().setBackground(BG_PANEL);
        styleScrollBar(amScroll.getVerticalScrollBar());

        JButton calcBtn = glowButton("⚡ Calculate EMI", ACCENT_CYAN, ev -> {
            try {
                double sp = price.getText().isBlank() ? 0 : Double.parseDouble(price.getText().trim());
                double dp = dpField.getText().isBlank() ? 0 : Double.parseDouble(dpField.getText().trim());
                double r = Double.parseDouble(rateField.getText().trim());
                int n = Integer.parseInt(tenureField.getText().trim());
                double P = sp - dp;
                if (P <= 0) {
                    emiResult.setText("Loan amount must be > 0.");
                    emiResult.setForeground(new java.awt.Color(255, 80, 80));
                    return;
                }
                double mr = r / 12.0 / 100.0;
                double emi;
                if (mr == 0) {
                    emi = P / n;
                } else {
                    double factor = Math.pow(1 + mr, n);
                    emi = P * mr * factor / (factor - 1);
                }
                emiResult.setForeground(ACCENT_GREEN);
                emiResult
                        .setText(String.format("Monthly EMI: $%,.2f   |   Total Payable: $%,.2f   |   Interest: $%,.2f",
                                emi, emi * n, (emi * n) - P));
                // Build amortization schedule
                amModel.setRowCount(0);
                double bal = P;
                for (int i = 1; i <= n && i <= 360; i++) {
                    double interest = bal * mr;
                    double principal = emi - interest;
                    bal -= principal;
                    if (bal < 0)
                        bal = 0;
                    amModel.addRow(new Object[] {
                            i,
                            String.format("%,.2f", emi),
                            String.format("%,.2f", principal),
                            String.format("%,.2f", interest),
                            String.format("%,.2f", bal)
                    });
                }
            } catch (NumberFormatException ex) {
                emiResult.setText("Enter valid numbers.");
                emiResult.setForeground(new java.awt.Color(255, 80, 80));
            }
        });

        addFormRow(emiForm, eg, "Down Payment ($)", dpField, 0);
        addFormRow(emiForm, eg, "Annual Rate (%) *", rateField, 1);
        addFormRow(emiForm, eg, "Tenure (months) *", tenureField, 2);
        // Result row full-width
        eg.gridx = 0;
        eg.gridy = 3;
        eg.gridwidth = 2;
        eg.weightx = 1;
        eg.insets = new java.awt.Insets(8, 0, 4, 0);
        emiForm.add(calcBtn, eg);
        eg.gridy = 4;
        eg.insets = new java.awt.Insets(4, 0, 8, 0);
        emiForm.add(emiResult, eg);
        eg.gridy = 5;
        eg.weighty = 1;
        eg.fill = GridBagConstraints.BOTH;
        eg.insets = new java.awt.Insets(0, 0, 0, 0);
        emiForm.add(amScroll, eg);

        // ── Tabbed pane ───────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG_PANEL);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setFont(FONT_LABEL);
        tabs.addTab("📋 Sale Details", saleForm);
        tabs.addTab("🏦 Finance / EMI", emiForm);
        // Style tab pane
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            protected void installDefaults() {
                super.installDefaults();
                highlight = BG_PANEL;
                shadow = BG_PANEL;
                darkShadow = BG_PANEL;
                focus = BG_PANEL;
            }
        });
        tabs.setOpaque(true);
        dlg.add(tabs, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────────
        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Record Sale", ACCENT_GREEN, e -> {
            try {
                if (carBox.getItemCount() == 0 || custBox.getItemCount() == 0 || empBox.getItemCount() == 0) {
                    showError("No data available.");
                    return;
                }
                int carId = Integer.parseInt(((String) carBox.getSelectedItem()).split(":")[0].trim());
                int custId = Integer.parseInt(((String) custBox.getSelectedItem()).split(":")[0].trim());
                int empId = Integer.parseInt(((String) empBox.getSelectedItem()).split(":")[0].trim());
                double salePrice = Double.parseDouble(price.getText().trim());
                Sale sale = new Sale();
                sale.setCarId(carId);
                sale.setCustomerId(custId);
                sale.setEmployeeId(empId);
                sale.setSaleDate(new Date());
                sale.setSalePrice(salePrice);
                sale.setPaymentMethod((String) payBox.getSelectedItem());
                sale.setStatus("Pending");
                if (saleController.addSale(sale)) {
                    // Save EMI record if Finance tab was used
                    try {
                        double dp = dpField.getText().isBlank() ? 0 : Double.parseDouble(dpField.getText().trim());
                        double r = rateField.getText().isBlank() ? 0 : Double.parseDouble(rateField.getText().trim());
                        int n = tenureField.getText().isBlank() ? 0 : Integer.parseInt(tenureField.getText().trim());
                        if (r > 0 && n > 0) {
                            double P = salePrice - dp;
                            double mr = r / 12.0 / 100.0;
                            double emi = mr == 0 ? P / n : P * mr * Math.pow(1 + mr, n) / (Math.pow(1 + mr, n) - 1);
                            // Find the newly created sale id
                            java.sql.Connection c2 = utils.DatabaseManager.getInstance().getConnection();
                            java.sql.ResultSet rs2 = c2.createStatement().executeQuery(
                                    "SELECT id FROM sales ORDER BY id DESC LIMIT 1");
                            if (rs2.next()) {
                                int newSaleId = rs2.getInt(1);
                                java.sql.PreparedStatement fi = c2.prepareStatement(
                                        "INSERT INTO finance_records (sale_id,down_payment,loan_amount,interest_rate,tenure_months,monthly_emi) VALUES (?,?,?,?,?,?)");
                                fi.setInt(1, newSaleId);
                                fi.setDouble(2, dp);
                                fi.setDouble(3, P);
                                fi.setDouble(4, r);
                                fi.setInt(5, n);
                                fi.setDouble(6, emi);
                                fi.executeUpdate();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    dlg.dispose();
                    showPanel("SALES");
                    showSuccess("Sale recorded!");
                } else
                    showError("Failed to record sale.");
            } catch (NumberFormatException ex) {
                showError("Enter a valid price.");
            }
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── AUDIT TRAIL PANEL (Admin only) ──────────────────────────────────────
    private JPanel buildAuditPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("🔍  Audit Trail", new Color(100, 200, 255)), BorderLayout.WEST);
        JButton refresh = glowButton("↻ Refresh", new Color(60, 120, 180), null);
        JButton exportBtn = glowButton("📥 Export CSV", new Color(80, 200, 80), null);
        JPanel hBtns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        hBtns.setOpaque(false);
        hBtns.add(exportBtn);
        hBtns.add(refresh);
        header.add(hBtns, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = { "Timestamp", "User", "Action", "Entity", "Detail" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = buildFancyTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(155);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(280);

        // Search bar
        JTextField searchBox = darkTextField("🔍  Filter by user, action, or entity...", 40);
        JPanel filterBar = new JPanel(new BorderLayout(8, 0));
        filterBar.setOpaque(false);
        filterBar.add(searchBox, BorderLayout.CENTER);

        // Load data
        Runnable load = () -> {
            model.setRowCount(0);
            String q = searchBox.getText().trim().toLowerCase();
            java.util.List<String[]> rows = utils.DatabaseManager.getInstance().getAuditLog(2000);
            for (String[] r : rows) {
                if (q.isEmpty() || r[1].toLowerCase().contains(q)
                        || r[2].toLowerCase().contains(q)
                        || r[3].toLowerCase().contains(q))
                    model.addRow(r);
            }
            setStatus("Audit log: " + model.getRowCount() + " entries");
        };
        load.run();
        refresh.addActionListener(e -> load.run());
        searchBox.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                load.run();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                load.run();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                load.run();
            }
        });

        // Export CSV
        exportBtn.addActionListener(e -> {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setSelectedFile(new java.io.File("audit_log.csv"));
            if (fc.showSaveDialog(page) == javax.swing.JFileChooser.APPROVE_OPTION) {
                try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
                    pw.println("Timestamp,User,Action,Entity,Detail");
                    for (int r = 0; r < model.getRowCount(); r++) {
                        StringBuilder sb = new StringBuilder();
                        for (int c = 0; c < 5; c++) {
                            if (c > 0)
                                sb.append(",");
                            String v = String.valueOf(model.getValueAt(r, c)).replace("\"", "\\\"");
                            sb.append("\"").append(v).append("\"");
                        }
                        pw.println(sb);
                    }
                    showSuccess("Exported to " + fc.getSelectedFile().getName());
                } catch (Exception ex) {
                    showError("Export failed: " + ex.getMessage());
                }
            }
        });

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(filterBar, BorderLayout.NORTH);
        content.add(styledScrollPane(table), BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ─── SETTINGS PANEL ───────────────────────────────────────────────────────
    private JPanel buildSettingsPanel() {
        JPanel jPanel;
        JComponent jComponent;
        JComponent jComponent2;
        JComponent jComponent3;
        JPanel jPanel2;
        JComponent jComponent4;
        Object object;
        JComponent jComponent5;
        JComponent jComponent6;
        JComponent jComponent7;
        JLabel jLabel;
        JComponent jComponent8;
        JPanel jPanel3 = this.darkPage();
        jPanel3.setLayout(new BorderLayout(0, 16));
        jPanel3.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel jPanel4 = new JPanel(new BorderLayout());
        jPanel4.setOpaque(false);
        jPanel4.add((Component) this.sectionTitle("\u2699  Settings", new Color(180, 180, 220)), "West");
        jPanel3.add((Component) jPanel4, "North");
        JPanel jPanel5 = new JPanel();
        jPanel5.setLayout(new BoxLayout(jPanel5, 1));
        jPanel5.setOpaque(false);
        JPanel jPanel6 = this.glowCard("SMS Notifications (Twilio)");
        jPanel6.setLayout(new GridBagLayout());
        jPanel6.setBorder(new EmptyBorder(18, 24, 18, 24));
        jPanel6.setAlignmentX(0.0f);
        jPanel6.setMaximumSize(new Dimension(Integer.MAX_VALUE, jPanel6.getMaximumSize().height));
        GridBagConstraints gridBagConstraints = this.formGbc();
        if (SessionManager.isAdmin()) {
            jComponent8 = new JCheckBox("Enable SMS Notifications");
            jComponent8.setFont(FONT_LABEL);
            jComponent8.setForeground(TEXT_PRIMARY);
            jComponent8.setOpaque(false);
            ((AbstractButton) jComponent8).setSelected(TwilioService.isEnabled());
            boolean bl = !TwilioService.getAccountSid().isBlank() && !TwilioService.getAuthToken().isBlank()
                    && !TwilioService.getFromNumber().isBlank();
            jLabel = new JLabel(bl ? "  \u2713  Credentials saved \u2014 fields pre-filled from storage"
                    : "  \u26a0  No credentials saved yet \u2014 enter and click Save");
            jLabel.setFont(FONT_SMALL);
            jLabel.setForeground(bl ? ACCENT_GREEN : ACCENT_ORANGE);
            jComponent7 = this.darkTextField("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", 28);
            if (!TwilioService.getAccountSid().isBlank()) {
                ((JTextComponent) jComponent7).setText(TwilioService.getAccountSid());
            }
            jComponent6 = new JPasswordField(28);
            jComponent6.setBackground(new Color(16, 22, 42));
            jComponent6.setForeground(TEXT_PRIMARY);
            ((JTextComponent) jComponent6).setCaretColor(ACCENT_CYAN);
            ((JTextField) jComponent6).setFont(FONT_LABEL);
            ((JPasswordField) jComponent6).setEchoChar('\u25cf');
            jComponent6.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(40, 55, 90)), new EmptyBorder(6, 8, 6, 8)));
            if (!TwilioService.getAuthToken().isBlank()) {
                ((JPasswordField) jComponent6).setText(TwilioService.getAuthToken());
            }
            jComponent5 = this.smallButton("Show", TEXT_MUTED);
            final JPasswordField fToken = (JPasswordField) jComponent6;
            final JButton fShow = (JButton) jComponent5;
            ((AbstractButton) jComponent5).addActionListener(arg_0 -> this.doTokenToggle(fToken, fShow, arg_0));
            object = new JPanel(new BorderLayout(6, 0));
            ((JComponent) object).setOpaque(false);
            ((Container) object).add((Component) jComponent6, "Center");
            ((Container) object).add((Component) jComponent5, "East");
            jComponent4 = this.darkTextField("+1 555 000 0000", 18);
            if (!TwilioService.getFromNumber().isBlank()) {
                ((JTextComponent) jComponent4).setText(TwilioService.getFromNumber());
            }
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            jPanel6.add((Component) jComponent8, gridBagConstraints);
            gridBagConstraints.gridy = 1;
            jPanel6.add((Component) jLabel, gridBagConstraints);
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 0.0;
            this.addFormRow(jPanel6, gridBagConstraints, "Account SID", jComponent7, 2);
            this.addFormRow(jPanel6, gridBagConstraints, "Auth Token", (Component) object, 3);
            this.addFormRow(jPanel6, gridBagConstraints, "From Number", jComponent4, 4);

            // ── Twilio Trial Verification Notice ──────────────────────────────
            JLabel smsHint = new JLabel(
                    "<html><div style='color:#FFC107;padding:6px 0'>" +
                            "<b>⚠ Twilio Trial Account — Important:</b><br>" +
                            "SMS can <b>only</b> be sent to phone numbers that have been verified first.<br>" +
                            "Before sending SMS to any customer, visit:<br>" +
                            "<u style='color:#00D4FF'>Twilio Console → Phone Numbers → Verified Caller IDs</u><br>" +
                            "and add &amp; verify the customer's number there." +
                            "</div></html>");
            smsHint.setFont(FONT_SMALL);
            smsHint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            smsHint.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        java.awt.Desktop.getDesktop().browse(
                                java.net.URI.create(
                                        "https://console.twilio.com/us1/develop/phone-numbers/manage/verified"));
                    } catch (Exception ignored) {
                    }
                }
            });
            GridBagConstraints smsHintGbc = (GridBagConstraints) gridBagConstraints.clone();
            smsHintGbc.gridx = 0;
            smsHintGbc.gridy = 5;
            smsHintGbc.gridwidth = 2;
            smsHintGbc.insets = new Insets(10, 0, 4, 0);
            jPanel6.add(smsHint, smsHintGbc);

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(14, 0, 0, 0);
            jPanel2 = new JPanel(new FlowLayout(0, 10, 0));
            jPanel2.setOpaque(false);
            final JTextField fSid = (JTextField) jComponent7;
            final JTextField fFrom = (JTextField) jComponent4;
            final JCheckBox fEnabled = (JCheckBox) jComponent8;
            final JLabel fBadge = jLabel;
            jComponent3 = this.glowButton("\ud83d\udcbe Save Settings", new Color(180, 180, 220),
                    arg_0 -> this.doSmsSave(fSid, fToken, fFrom, fEnabled, fBadge, arg_0));
            jComponent2 = this.smallButton("\u2715 Clear Credentials", ACCENT_PINK);
            ((AbstractButton) jComponent2).addActionListener(
                    arg_0 -> this.doSmsClear(fSid, fToken, fFrom, fEnabled, fBadge, arg_0));
            jComponent = this.smallButton("\ud83d\udce8 Send Test SMS", TEXT_MUTED);
            ((AbstractButton) jComponent)
                    .addActionListener(arg_0 -> this.doSmsTest(fEnabled,
                            fSid, fToken, fFrom, arg_0));
            jPanel2.add(jComponent3);
            jPanel2.add(jComponent);
            jPanel2.add(jComponent2);
            jPanel6.add((Component) jPanel2, gridBagConstraints);
            jPanel = new JPanel(new BorderLayout());
            jPanel.setOpaque(false);
            jPanel.add(jPanel6);
            jPanel.setAlignmentX(0.0f);
            jPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, jPanel6.getPreferredSize().height + 40));
            jPanel5.add(jPanel);
        }
        if (SessionManager.isAdmin()) {
            jComponent8 = new JPanel();
            jComponent8.setLayout(new BoxLayout(jComponent8, 1));
            jComponent8.setBackground(new Color(13, 17, 35));
            jComponent8.setAlignmentX(0.0f);
            jComponent8.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            jComponent8.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 200, 60, 90), 1), new EmptyBorder(18, 20, 18, 20)));
            JLabel jLabel2 = new JLabel("\ud83d\udd10  Two-Factor Authentication (Admin)");
            jLabel2.setFont(FONT_HEADER);
            jLabel2.setForeground(new Color(255, 200, 60));
            jLabel2.setAlignmentX(0.0f);
            jLabel = new JLabel("Send a 6-digit OTP via SMS to your phone every time the admin logs in.");
            jLabel.setFont(FONT_SMALL);
            jLabel.setForeground(TEXT_MUTED);
            jLabel.setAlignmentX(0.0f);
            jComponent7 = new JCheckBox("Enable 2FA for my admin account");
            jComponent7.setFont(FONT_LABEL);
            jComponent7.setForeground(TEXT_PRIMARY);
            jComponent7.setOpaque(false);
            ((AbstractButton) jComponent7)
                    .setSelected(DatabaseManager.getInstance().is2faEnabled(SessionManager.getUser()));
            jComponent7.setAlignmentX(0.0f);
            jComponent6 = new JLabel("Phone for OTP (include country code, e.g. +919876543210):");
            jComponent6.setFont(FONT_SMALL);
            jComponent6.setForeground(TEXT_MUTED);
            jComponent6.setAlignmentX(0.0f);
            jComponent5 = this.darkTextField("", 40);
            object = DatabaseManager.getInstance().getUserPhone(SessionManager.getUser());
            ((JTextComponent) jComponent5).setText((String) (object != null ? object : ""));
            jComponent5.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            jComponent5.setAlignmentX(0.0f);
            final JCheckBox fTwoFaCheck = (JCheckBox) jComponent7;
            final JTextField fPhoneField = (JTextField) jComponent5;
            jComponent4 = this.glowButton("\ud83d\udcbe  Save 2FA Settings", new Color(255, 200, 60), arg_0 -> this
                    .doSave2FA(fTwoFaCheck, fPhoneField, arg_0));
            jComponent4.setAlignmentX(0.0f);
            jComponent8.add(jLabel2);
            jComponent8.add(Box.createVerticalStrut(4));
            jComponent8.add(jLabel);
            jComponent8.add(Box.createVerticalStrut(12));
            jComponent8.add(jComponent7);
            jComponent8.add(Box.createVerticalStrut(10));
            jComponent8.add(jComponent6);
            jComponent8.add(Box.createVerticalStrut(4));
            jComponent8.add(jComponent5);
            jComponent8.add(Box.createVerticalStrut(12));
            jComponent8.add(jComponent4);
            jPanel5.add(jComponent8);
            jPanel5.add(Box.createVerticalStrut(14));
            jPanel2 = new JPanel(new BorderLayout(0, 12));
            jPanel2.setBackground(new Color(13, 17, 35));
            jPanel2.setAlignmentX(0.0f);
            jPanel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            jPanel2.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 200, 255, 90), 1), new EmptyBorder(18, 20, 18, 20)));
            jComponent3 = new JPanel(new BorderLayout());
            jComponent3.setOpaque(false);
            jComponent2 = new JLabel("\ud83d\udc65  User Management");
            jComponent2.setFont(FONT_HEADER);
            jComponent2.setForeground(new Color(100, 200, 255));
            jComponent = new JLabel("Create and manage app login accounts for your staff.");
            jComponent.setFont(FONT_SMALL);
            jComponent.setForeground(TEXT_MUTED);
            jPanel = new JPanel();
            jPanel.setOpaque(false);
            jPanel.setLayout(new BoxLayout(jPanel, 1));
            jPanel.add(jComponent2);
            jPanel.add(Box.createVerticalStrut(2));
            jPanel.add(jComponent);
            jComponent3.add((Component) jPanel, "West");
            jPanel2.add((Component) jComponent3, "North");
            Object[] objectArray = new String[] { "Username", "Role", "Email", "Phone", "Enabled", "2FA" };
            DefaultTableModel defaultTableModel = new DefaultTableModel(objectArray, 0) {

                @Override
                public boolean isCellEditable(int n, int n2) {
                    return false;
                }
            };
            JTable jTable = this.buildFancyTable(defaultTableModel);
            jTable.setPreferredScrollableViewportSize(new Dimension(0, 110));
            Runnable runnable = () -> {
                defaultTableModel.setRowCount(0);
                for (Object[] userRow : DatabaseManager.getInstance().getAllAppUsers()) {
                    defaultTableModel.addRow(userRow);
                }
            };
            runnable.run();
            JPanel jPanel7 = new JPanel(new FlowLayout(0, 8, 4));
            jPanel7.setOpaque(false);
            JButton jButton = this.glowButton("\u2795 Add User", ACCENT_GREEN,
                    actionEvent -> this.showAddAppUserDialog(runnable));
            JButton jButton2 = this.glowButton("\ud83d\udd11 Reset Password", ACCENT_ORANGE, actionEvent -> {
                int n = jTable.getSelectedRow();
                if (n < 0) {
                    this.showInfo("Select a user first.");
                    return;
                }
                String string = (String) defaultTableModel.getValueAt(n, 0);
                this.showOtpResetPasswordDialog(string);
            });
            JButton jButton3 = this.glowButton("\ud83d\uddd1 Delete", ACCENT_PINK, actionEvent -> {
                int n = jTable.getSelectedRow();
                if (n < 0) {
                    this.showInfo("Select a user first.");
                    return;
                }
                String string = (String) defaultTableModel.getValueAt(n, 0);
                if (string.equals(SessionManager.getUser())) {
                    this.showError("Cannot delete your own account.");
                    return;
                }
                if (this.confirmDelete(string)) {
                    if (DatabaseManager.getInstance().deleteAppUser(string)) {
                        runnable.run();
                        this.showSuccess("User deleted.");
                        AuditLogger.log("DELETE", "AppUser " + string, "");
                    } else {
                        this.showError("Cannot delete \u2014 would remove the last admin.");
                    }
                }
            });
            jPanel7.add(jButton);
            jPanel7.add(jButton2);
            jPanel7.add(jButton3);
            JPanel jPanel8 = new JPanel(new BorderLayout(0, 6));
            jPanel8.setOpaque(false);
            jPanel8.add((Component) this.styledScrollPane(jTable), "Center");
            jPanel8.add((Component) jPanel7, "South");
            jPanel2.add((Component) jPanel8, "Center");
            jPanel5.add(jPanel2);
            jPanel5.add(Box.createVerticalStrut(14));
            JPanel jPanel9 = new JPanel();
            jPanel9.setLayout(new BoxLayout(jPanel9, 1));
            jPanel9.setBackground(new Color(13, 17, 35));
            jPanel9.setAlignmentX(0.0f);
            jPanel9.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            jPanel9.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 230, 150, 90), 1), new EmptyBorder(18, 20, 18, 20)));
            JLabel jLabel3 = new JLabel("\ud83d\uddc4  Database Backup");
            jLabel3.setFont(FONT_HEADER);
            jLabel3.setForeground(new Color(100, 230, 150));
            jLabel3.setAlignmentX(0.0f);
            JLabel jLabel4 = new JLabel(
                    "Auto-export the database daily via mysqldump. Backups are saved as .sql files.");
            jLabel4.setFont(FONT_SMALL);
            jLabel4.setForeground(TEXT_MUTED);
            jLabel4.setAlignmentX(0.0f);
            JLabel jLabel5 = new JLabel("Last backup:  " + BackupScheduler.getLastBackup());
            jLabel5.setFont(FONT_SMALL);
            jLabel5.setForeground(new Color(57, 255, 20));
            jLabel5.setAlignmentX(0.0f);
            JCheckBox jCheckBox = new JCheckBox("Enable daily automatic backup");
            jCheckBox.setFont(FONT_LABEL);
            jCheckBox.setForeground(TEXT_PRIMARY);
            jCheckBox.setOpaque(false);
            jCheckBox.setSelected(BackupScheduler.isEnabled());
            jCheckBox.setAlignmentX(0.0f);
            JLabel jLabel6 = new JLabel("Backup folder:");
            jLabel6.setFont(FONT_SMALL);
            jLabel6.setForeground(TEXT_MUTED);
            jLabel6.setAlignmentX(0.0f);
            JTextField jTextField = this.darkTextField("", 40);
            jTextField.setText(BackupScheduler.getBackupDir());
            jTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            jTextField.setAlignmentX(0.0f);
            final JTextField fBackupDir = jTextField;
            final JPanel fCardParent = jPanel2;
            JButton jButton4 = this.glowButton("\ud83d\udcc1  Browse", new Color(80, 120, 180), actionEvent -> {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileSelectionMode(1);
                jFileChooser.setCurrentDirectory(new File(fBackupDir.getText()));
                if (jFileChooser.showOpenDialog(fCardParent) == 0) {
                    fBackupDir.setText(jFileChooser.getSelectedFile().getAbsolutePath());
                }
            });
            jButton4.setAlignmentX(0.0f);
            JLabel jLabel7 = new JLabel("Daily backup time (HH:mm, 24-hour):");
            jLabel7.setFont(FONT_SMALL);
            jLabel7.setForeground(TEXT_MUTED);
            jLabel7.setAlignmentX(0.0f);
            JTextField jTextField2 = this.darkTextField("02:00", 10);
            jTextField2.setText(BackupScheduler.getBackupTime());
            jTextField2.setMaximumSize(new Dimension(140, 38));
            jTextField2.setAlignmentX(0.0f);
            JLabel jLabel8 = new JLabel("Keep last N days (older backups are auto-deleted):");
            jLabel8.setFont(FONT_SMALL);
            jLabel8.setForeground(TEXT_MUTED);
            jLabel8.setAlignmentX(0.0f);
            JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(BackupScheduler.getRetentionDays(), 1, 365, 1));
            jSpinner.setBackground(BG_CARD);
            jSpinner.setFont(FONT_LABEL);
            jSpinner.setMaximumSize(new Dimension(120, 38));
            jSpinner.setAlignmentX(0.0f);
            JPanel jPanel10 = new JPanel(new FlowLayout(0, 8, 0));
            jPanel10.setOpaque(false);
            jPanel10.setAlignmentX(0.0f);
            JButton jButton5 = this.glowButton("\ud83d\udcbe  Save Backup Settings", new Color(100, 230, 150),
                    actionEvent -> {
                        BackupScheduler.setEnabled(jCheckBox.isSelected());
                        BackupScheduler.setBackupDir(jTextField.getText().trim());
                        BackupScheduler.setBackupTime(jTextField2.getText().trim());
                        BackupScheduler.setRetentionDays((Integer) jSpinner.getValue());
                        BackupScheduler.start();
                        this.showSuccess("Backup settings saved.");
                    });
            JButton jButton6 = this.glowButton("\u25b6  Backup Now", new Color(57, 255, 20), actionEvent -> {
                this.setStatus("Running backup...");
                Thread thread = new Thread(() -> {
                    String string = BackupScheduler.performBackup();
                    SwingUtilities.invokeLater(() -> {
                        if (string.startsWith("\u2705")) {
                            this.showSuccess(string);
                        } else {
                            this.showError(string);
                        }
                        jLabel5.setText("Last backup:  " + BackupScheduler.getLastBackup());
                    });
                }, "manual-backup");
                thread.setDaemon(true);
                thread.start();
            });
            jPanel10.add(jButton5);
            jPanel10.add(jButton6);
            jPanel9.add(jLabel3);
            jPanel9.add(Box.createVerticalStrut(4));
            jPanel9.add(jLabel4);
            jPanel9.add(Box.createVerticalStrut(4));
            jPanel9.add(jLabel5);
            jPanel9.add(Box.createVerticalStrut(12));
            jPanel9.add(jCheckBox);
            jPanel9.add(Box.createVerticalStrut(10));
            jPanel9.add(jLabel6);
            jPanel9.add(Box.createVerticalStrut(4));
            jPanel9.add(jTextField);
            jPanel9.add(Box.createVerticalStrut(4));
            jPanel9.add(jButton4);
            jPanel9.add(Box.createVerticalStrut(10));
            jPanel9.add(jLabel7);
            jPanel9.add(Box.createVerticalStrut(4));
            jPanel9.add(jTextField2);
            jPanel9.add(Box.createVerticalStrut(10));
            jPanel9.add(jLabel8);
            jPanel9.add(Box.createVerticalStrut(4));
            jPanel9.add(jSpinner);
            jPanel9.add(Box.createVerticalStrut(14));
            jPanel9.add(jPanel10);
            jPanel5.add(jPanel9);
        }
        jComponent8 = new JScrollPane(jPanel5);
        jComponent8.setBorder(null);
        ((JScrollPane) jComponent8).getViewport().setBackground(BG_DARK);
        ((JScrollPane) jComponent8).getVerticalScrollBar().setUnitIncrement(12);
        this.styleScrollBar(((JScrollPane) jComponent8).getVerticalScrollBar());
        jPanel3.add((Component) jComponent8, "Center");
        return jPanel3;
    }

    private JPanel buildConfiguratorPanel() {
        List<Car> cars = new java.util.ArrayList<>();
        try {
            cars = carController.getAllCars();
        } catch (Exception ignored) {
        }
        Car defaultCar = cars.isEmpty() ? null : cars.get(0);
        return new CarConfiguratorEngine(defaultCar);
    }

    // ─── DASHBOARD ────────────────────────────────────────────────────────────
    private JPanel statCard(String title, String value, String sub, Color accent, String icon) {
        JPanel card = new JPanel() {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = hovered ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25) : BG_CARD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // top accent line
                GradientPaint gp = new GradientPaint(0, 0, accent, getWidth(), 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0));
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(12, 0, getWidth() - 12, 0);
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valLbl.setForeground(accent);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_HEADER);
        titleLbl.setForeground(TEXT_PRIMARY);
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(FONT_SMALL);
        subLbl.setForeground(TEXT_MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLbl, BorderLayout.WEST);
        top.add(iconLbl, BorderLayout.EAST);
        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 2));
        bottom.setOpaque(false);
        bottom.add(valLbl);
        bottom.add(subLbl);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTestDrivesPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("🔑  Test Drive Scheduler", ACCENT_ORANGE), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("+ Schedule Test Drive", ACCENT_ORANGE, e -> showAddTestDriveDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        String[] cols = { "ID", "Vehicle", "Customer", "Date", "Time Slot", "Status", "Feedback" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = buildFancyTable(model);

        Runnable load = () -> {
            model.setRowCount(0);
            for (TestDrive td : testDriveController.getAllTestDrives())
                model.addRow(
                        new Object[] { td.getId(), td.getCarName() != null ? td.getCarName() : "Car#" + td.getCarId(),
                                td.getCustomerName() != null ? td.getCustomerName() : "Cust#" + td.getCustomerId(),
                                td.getScheduledDate(), td.getTimeSlot(), td.getStatus(),
                                td.getFeedback() != null ? td.getFeedback() : "" });
            setStatus("Loaded " + model.getRowCount() + " test drives");
        };
        load.run();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        JButton complete = glowButton("✓ Mark Completed", ACCENT_GREEN, e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            String feedback = JOptionPane.showInputDialog(this, "Enter feedback (optional):", "Feedback",
                    JOptionPane.PLAIN_MESSAGE);
            if (testDriveController.updateStatus(id, "Completed", feedback)) {
                load.run();
                showSuccess("Test drive completed.");
            }
        });
        JButton cancel2 = glowButton("✕ Cancel", ACCENT_PINK, e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            if (testDriveController.updateStatus(id, "Cancelled", null)) {
                load.run();
                showSuccess("Cancelled.");
            }
        });
        JButton del = glowButton("🗑 Delete", new Color(150, 30, 50), e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            if (testDriveController.deleteTestDrive(id)) {
                load.run();
                showSuccess("Deleted.");
            }
        });
        JButton refresh = smallButton("↻ Refresh", TEXT_MUTED);
        refresh.addActionListener(e -> load.run());
        actions.add(complete);
        actions.add(cancel2);
        actions.add(del);
        actions.add(refresh);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(styledScrollPane(table), BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private void showAddTestDriveDialog() {
        JDialog dlg = styledDialog("Schedule Test Drive", 480, 430);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("🔑", "Schedule Test Drive", ACCENT_ORANGE), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        List<Car> cars = carController.getAllCars();
        List<Customer> custs = customerController.getAllCustomers();
        JComboBox<String> carBox = darkComboBox(
                cars.stream().map(c -> c.getId() + ": " + c.getBrand() + " " + c.getModel()).toArray(String[]::new));
        JComboBox<String> custBox = darkComboBox(
                custs.stream().map(c -> c.getId() + ": " + c.getName()).toArray(String[]::new));
        JTextField dateField = darkTextField("YYYY-MM-DD", 18);
        String[] slots = { "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM",
                "04:00 PM" };
        JComboBox<String> slotBox = darkComboBox(slots);

        addFormRow(form, gbc, "Vehicle *", carBox, 0);
        addFormRow(form, gbc, "Customer *", custBox, 1);
        addFormRow(form, gbc, "Date (YYYY-MM-DD) *", dateField, 2);
        addFormRow(form, gbc, "Time Slot", slotBox, 3);

        dlg.add(form, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Schedule Drive", ACCENT_ORANGE, e -> {
            try {
                if (carBox.getItemCount() == 0 || custBox.getItemCount() == 0) {
                    showError("No data available.");
                    return;
                }
                int carId = Integer.parseInt(((String) carBox.getSelectedItem()).split(":")[0].trim());
                int custId = Integer.parseInt(((String) custBox.getSelectedItem()).split(":")[0].trim());
                java.sql.Date date = java.sql.Date.valueOf(dateField.getText().trim());
                TestDrive td = new TestDrive();
                td.setCarId(carId);
                td.setCustomerId(custId);
                td.setScheduledDate(date);
                td.setTimeSlot((String) slotBox.getSelectedItem());
                td.setStatus("Scheduled");
                if (testDriveController.addTestDrive(td)) {
                    dlg.dispose();
                    showPanel("TESTDRIVES");
                    showSuccess("Test drive scheduled!");
                } else
                    showError("Failed to schedule.");
            } catch (Exception ex) {
                showError("Invalid date format. Use YYYY-MM-DD");
            }
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── DEALERSHIPS PANEL ────────────────────────────────────────────────────
    private JPanel buildDealershipsPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("📍  Dealership Locations", new Color(255, 200, 50)), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("+ Add Location", new Color(255, 200, 50), e -> showAddDealershipDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        List<Dealership> dealers = dealershipController.getAllDealerships();

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);

        for (Dealership d : dealers) {
            JPanel card = dealerCard(d);
            grid.add(card);
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        styleScrollBar(scroll.getVerticalScrollBar());

        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel dealerCard(Dealership d) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                Color accent = new Color(255, 200, 50);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 200, 50, 60), getWidth(), 0,
                        new Color(255, 200, 50, 0));
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(14, 0, getWidth() - 14, 0);
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        String stars = "★".repeat((int) d.getRating()) + "☆".repeat(5 - (int) d.getRating());
        JLabel nameLabel = new JLabel("📍 " + d.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(new Color(255, 200, 50));
        JLabel cityLabel = new JLabel(d.getCity());
        cityLabel.setFont(FONT_LABEL);
        cityLabel.setForeground(TEXT_MUTED);
        JLabel starLabel = new JLabel(stars + "  " + d.getRating());
        starLabel.setFont(FONT_LABEL);
        starLabel.setForeground(new Color(255, 200, 50));

        JPanel infoGrid = new JPanel(new GridLayout(3, 1, 0, 4));
        infoGrid.setOpaque(false);
        infoGrid.add(infoRow("📞", d.getPhone()));
        infoGrid.add(infoRow("🕐", d.getTimings()));
        infoGrid.add(infoRow("📌", d.getAddress()));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel topLeft = new JPanel(new GridLayout(2, 1, 0, 2));
        topLeft.setOpaque(false);
        topLeft.add(nameLabel);
        topLeft.add(cityLabel);
        top.add(topLeft, BorderLayout.WEST);
        top.add(starLabel, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(infoGrid, BorderLayout.CENTER);
        return card;
    }

    private JPanel infoRow(String icon, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setOpaque(false);
        JLabel ico = new JLabel(icon);
        ico.setFont(FONT_SMALL);
        JLabel lbl = new JLabel(text != null ? text : "");
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        row.add(ico);
        row.add(lbl);
        return row;
    }

    private void showAddDealershipDialog() {
        JDialog dlg = styledDialog("Add Dealership Location", 480, 510);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("📍", "Add Dealership Location", new Color(255, 200, 50)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField nameF = darkTextField("Dealership Name", 18);
        JTextField addr = darkTextField("Street Address", 18);
        JTextField city = darkTextField("City", 18);
        JTextField phone = darkTextField("555-0000", 18);
        JTextField opening = darkTextField("09:00 AM", 18);
        JTextField closing = darkTextField("08:00 PM", 18);
        JTextField rating = darkTextField("4.5", 18);

        addFormRow(form, gbc, "Name *", nameF, 0);
        addFormRow(form, gbc, "Address", addr, 1);
        addFormRow(form, gbc, "City *", city, 2);
        addFormRow(form, gbc, "Phone", phone, 3);
        addFormRow(form, gbc, "Opening Time", opening, 4);
        addFormRow(form, gbc, "Closing Time", closing, 5);
        addFormRow(form, gbc, "Rating (1–5)", rating, 6);

        dlg.add(form, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Add Location", new Color(255, 200, 50), e -> {
            Dealership d = new Dealership();
            d.setName(nameF.getText().trim());
            d.setAddress(addr.getText().trim());
            d.setCity(city.getText().trim());
            d.setPhone(phone.getText().trim());
            d.setOpeningTime(opening.getText().trim());
            d.setClosingTime(closing.getText().trim());
            try {
                d.setRating(Double.parseDouble(rating.getText().trim()));
            } catch (NumberFormatException ex) {
                d.setRating(0);
            }
            if (d.getName().isEmpty()) {
                showInfo("Name required.");
                return;
            }
            if (dealershipController.addDealership(d)) {
                dlg.dispose();
                showPanel("DEALERSHIPS");
                showSuccess("Location added!");
            } else
                showError("Failed.");
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── REVIEWS PANEL ────────────────────────────────────────────────────────
    private JPanel buildReviewsPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("⭐  Customer Reviews", new Color(255, 215, 0)), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("+ Add Review", new Color(255, 215, 0), e -> showAddReviewDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        List<Review> reviews = reviewController.getAllReviews();

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);

        for (Review r : reviews)
            grid.add(reviewCard(r));

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        styleScrollBar(scroll.getVerticalScrollBar());
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel reviewCard(Review r) {
        Color platformColor = r.getPlatform() != null && r.getPlatform().equalsIgnoreCase("Reddit")
                ? new Color(255, 69, 0)
                : new Color(29, 161, 242);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(platformColor.getRed(), platformColor.getGreen(), platformColor.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
        };
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel carLabel = new JLabel("🚗 " + r.getCarBrand() + " " + r.getCarModel());
        carLabel.setFont(FONT_HEADER);
        carLabel.setForeground(TEXT_PRIMARY);
        JLabel starsLabel = new JLabel(r.getRatingStars());
        starsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        starsLabel.setForeground(new Color(255, 215, 0));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(carLabel, BorderLayout.WEST);
        topRow.add(starsLabel, BorderLayout.EAST);

        JLabel platform = new JLabel(
                "@ " + (r.getPlatform() != null ? r.getPlatform() : "N/A") + " · " + r.getAuthor());
        platform.setFont(FONT_SMALL);
        platform.setForeground(platformColor);

        JTextArea content = new JTextArea(r.getContent() != null ? r.getContent() : "");
        content.setFont(FONT_LABEL);
        content.setForeground(TEXT_MUTED);
        content.setBackground(new Color(0, 0, 0, 0));
        content.setOpaque(false);
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);

        card.add(topRow, BorderLayout.NORTH);
        card.add(platform, BorderLayout.CENTER);
        card.add(content, BorderLayout.SOUTH);
        return card;
    }

    private void showAddReviewDialog() {
        JDialog dlg = styledDialog("Add Review", 480, 500);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("⭐", "Add Customer Review", new Color(255, 215, 0)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();

        JTextField brandF = darkTextField("e.g. Toyota", 18);
        JTextField modelF = darkTextField("e.g. Camry", 18);
        JTextField authorF = darkTextField("Username", 18);
        String[] platforms = { "Reddit", "Twitter", "Google", "Facebook", "Other" };
        JComboBox<String> platBox = darkComboBox(platforms);
        JTextArea content = new JTextArea(3, 18);
        content.setBackground(new Color(16, 22, 42));
        content.setForeground(TEXT_PRIMARY);
        content.setCaretColor(ACCENT_CYAN);
        content.setFont(FONT_LABEL);
        content.setBorder(new EmptyBorder(6, 8, 6, 8));
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(content);
        contentScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 90)));
        styleScrollBar(contentScroll.getVerticalScrollBar());
        String[] ratings = { "5 - Excellent", "4 - Good", "3 - Average", "2 - Below Average", "1 - Poor" };
        JComboBox<String> ratingBox = darkComboBox(ratings);

        addFormRow(form, gbc, "Car Brand *", brandF, 0);
        addFormRow(form, gbc, "Car Model *", modelF, 1);
        addFormRow(form, gbc, "Author *", authorF, 2);
        addFormRow(form, gbc, "Platform", platBox, 3);
        addFormRow(form, gbc, "Review Content", contentScroll, 4);
        addFormRow(form, gbc, "Rating", ratingBox, 5);

        dlg.add(form, BorderLayout.CENTER);

        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Submit Review", new Color(255, 215, 0), e -> {
            Review rev = new Review();
            rev.setCarBrand(brandF.getText().trim());
            rev.setCarModel(modelF.getText().trim());
            rev.setAuthor(authorF.getText().trim());
            rev.setPlatform((String) platBox.getSelectedItem());
            rev.setContent(content.getText().trim());
            rev.setReviewDate(new Date());
            rev.setRating(Integer.parseInt(((String) ratingBox.getSelectedItem()).split(" ")[0]));
            if (rev.getCarBrand().isEmpty() || rev.getAuthor().isEmpty()) {
                showInfo("Brand and Author required.");
                return;
            }
            if (reviewController.addReview(rev)) {
                dlg.dispose();
                showPanel("REVIEWS");
                showSuccess("Review added!");
            } else
                showError("Failed.");
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── INVOICE DIALOG ───────────────────────────────────────────────────────
    private void showInvoiceDialog(DefaultTableModel model, int row) {
        String saleId = String.valueOf(model.getValueAt(row, 0));
        String vehicle = String.valueOf(model.getValueAt(row, 1));
        String customer = String.valueOf(model.getValueAt(row, 2));
        String employee = String.valueOf(model.getValueAt(row, 3));
        String date = String.valueOf(model.getValueAt(row, 4));
        String amount = String.valueOf(model.getValueAt(row, 5));
        String payment = String.valueOf(model.getValueAt(row, 6));
        String status = String.valueOf(model.getValueAt(row, 7));

        JDialog dlg = styledDialog("Sale Invoice  —  #" + saleId, 480, 540);
        dlg.setLayout(new BorderLayout());

        JPanel body = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 212, 255, 30), getWidth(), getHeight(),
                        new Color(138, 43, 226, 20));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(24, 32, 24, 32));

        // Logo row
        JLabel logo = new JLabel("AutoElite Showroom");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT_CYAN);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sale Invoice / Receipt");
        subtitle.setFont(FONT_LABEL);
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Divider
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(0, 212, 255, 60));
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Invoice details
        String[][] fields = {
                { "Invoice #", "INV-" + String.format("%05d", Integer.parseInt(saleId)) },
                { "Date", date },
                { "Status", status },
                { "", "" },
                { "Vehicle", vehicle },
                { "", "" },
                { "Customer", customer },
                { "Handled By", employee },
                { "", "" },
                { "Payment Method", payment },
                { "Sale Amount", amount },
        };

        JPanel grid = new JPanel(new GridLayout(fields.length, 2, 8, 6));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        for (String[] f : fields) {
            JLabel key = new JLabel(f[0]);
            key.setFont(FONT_SMALL);
            key.setForeground(TEXT_MUTED);
            JLabel val = new JLabel(f[1]);
            boolean isMoney = f[0].contains("Amount");
            boolean isStatus = f[0].equals("Status");
            val.setFont(isMoney ? new Font("Segoe UI", Font.BOLD, 15) : FONT_LABEL);
            val.setForeground(isMoney ? ACCENT_GREEN
                    : isStatus && status.equals("Completed") ? ACCENT_GREEN
                            : isStatus && status.equals("Pending") ? ACCENT_ORANGE : TEXT_PRIMARY);
            grid.add(key);
            grid.add(val);
        }

        // Total box
        JPanel totalBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(57, 255, 20, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(57, 255, 20, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
        };
        totalBox.setLayout(new BorderLayout());
        totalBox.setBorder(new EmptyBorder(12, 16, 12, 16));
        totalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        totalBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel totalLbl = new JLabel("TOTAL AMOUNT");
        totalLbl.setFont(FONT_SMALL);
        totalLbl.setForeground(TEXT_MUTED);
        JLabel totalVal = new JLabel(amount);
        totalVal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        totalVal.setForeground(ACCENT_GREEN);
        totalBox.add(totalLbl, BorderLayout.WEST);
        totalBox.add(totalVal, BorderLayout.EAST);

        JLabel footer = new JLabel("Thank you for choosing AutoElite!");
        footer.setFont(FONT_SMALL);
        footer.setForeground(TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        body.add(logo);
        body.add(Box.createVerticalStrut(4));
        body.add(subtitle);
        body.add(Box.createVerticalStrut(12));
        body.add(sep1);
        body.add(Box.createVerticalStrut(14));
        body.add(grid);
        body.add(Box.createVerticalStrut(16));
        body.add(totalBox);
        body.add(Box.createVerticalStrut(16));
        body.add(footer);

        dlg.add(new JScrollPane(body) {
            {
                setBorder(null);
                getViewport().setBackground(BG_PANEL);
            }
        }, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        btnPanel.setBackground(new Color(10, 13, 24));
        btnPanel.add(glowButton("Close", ACCENT_CYAN, e -> dlg.dispose()));
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ─── REPORTS PANEL ────────────────────────────────────────────────────────
    private JPanel buildReportsPanel() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));

        page.add(sectionTitle("📊  Business Reports", new Color(160, 100, 255)), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);

        // ── 1. Monthly Sales Summary ──────────────────────────────────────────
        JPanel monthlySales = glowCard("Monthly Sales");
        monthlySales.setLayout(new BorderLayout());
        monthlySales.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel mTitle = new JLabel("Monthly Sales Summary");
        mTitle.setFont(FONT_HEADER);
        mTitle.setForeground(new Color(160, 100, 255));
        monthlySales.add(mTitle, BorderLayout.NORTH);

        String[] mCols = { "Month", "Sales Count", "Total Revenue" };
        DefaultTableModel mModel = new DefaultTableModel(mCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        List<String[]> monthlyData = saleController.getMonthlySummary();
        for (String[] row : monthlyData)
            mModel.addRow(row);
        if (monthlyData.isEmpty())
            mModel.addRow(new String[] { "No data", "-", "-" });
        JTable mTable = buildFancyTable(mModel);
        mTable.setRowHeight(28);
        mTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane mScroll = styledScrollPane(mTable);
        mScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        monthlySales.add(mScroll, BorderLayout.CENTER);
        grid.add(monthlySales);

        // ── 2. Sales by Car Category ──────────────────────────────────────────
        JPanel catSales = glowCard("By Category");
        catSales.setLayout(new BorderLayout());
        catSales.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel cTitle = new JLabel("Revenue by Car Category");
        cTitle.setFont(FONT_HEADER);
        cTitle.setForeground(ACCENT_CYAN);
        catSales.add(cTitle, BorderLayout.NORTH);

        String[] cCols = { "Category", "Units Sold", "Total Revenue" };
        DefaultTableModel cModel = new DefaultTableModel(cCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        List<String[]> catData = saleController.getSalesByCategory();
        for (String[] row : catData)
            cModel.addRow(row);
        if (catData.isEmpty())
            cModel.addRow(new String[] { "No data", "-", "-" });
        JTable cTable = buildFancyTable(cModel);
        cTable.setRowHeight(28);
        cTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane cScroll = styledScrollPane(cTable);
        cScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        catSales.add(cScroll, BorderLayout.CENTER);
        grid.add(catSales);

        // ── 3. Low Stock Alert ────────────────────────────────────────────────
        JPanel stockAlert = glowCard("Low Stock");
        stockAlert.setLayout(new BorderLayout());
        stockAlert.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel sTitle = new JLabel("⚠  Low Stock Vehicles (≤ 3)");
        sTitle.setFont(FONT_HEADER);
        sTitle.setForeground(ACCENT_ORANGE);
        stockAlert.add(sTitle, BorderLayout.NORTH);

        String[] sCols = { "Brand", "Model", "Category", "Stock Left" };
        DefaultTableModel sModel = new DefaultTableModel(sCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        List<Car> lowStock = carController.getLowStockCars(3);
        if (lowStock.isEmpty()) {
            sModel.addRow(new String[] { "All good!", "No low stock", "-", "-" });
        } else {
            for (Car c : lowStock)
                sModel.addRow(
                        new String[] { c.getBrand(), c.getModel(), c.getCategory(), String.valueOf(c.getStock()) });
        }
        JTable sTable = buildFancyTable(sModel);
        sTable.setRowHeight(28);
        sTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane sScroll = styledScrollPane(sTable);
        sScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        stockAlert.add(sScroll, BorderLayout.CENTER);
        grid.add(stockAlert);

        // ── 4. Payroll Summary ────────────────────────────────────────────────
        JPanel payroll = glowCard("Payroll");
        payroll.setLayout(new BorderLayout());
        payroll.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel pTitle = new JLabel("Monthly Payroll Summary");
        pTitle.setFont(FONT_HEADER);
        pTitle.setForeground(ACCENT_GREEN);
        payroll.add(pTitle, BorderLayout.NORTH);

        String[] pCols = { "Department", "Headcount", "Total Salary/Month" };
        DefaultTableModel pModel = new DefaultTableModel(pCols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        List<String[]> payrollData = employeeController.getPayrollSummary();
        double totalPayroll = 0;
        for (String[] row : payrollData) {
            pModel.addRow(row);
            try {
                totalPayroll += Double.parseDouble(row[2].replace("$", "").replace(",", ""));
            } catch (Exception ignored) {
            }
        }
        pModel.addRow(new String[] { "─────", "─────", "─────" });
        pModel.addRow(new String[] { "TOTAL", "-", "$" + formatMoney(totalPayroll) });
        if (payrollData.isEmpty())
            pModel.addRow(new String[] { "No data", "-", "-" });

        JTable pTable = buildFancyTable(pModel);
        pTable.setRowHeight(28);
        pTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane pScroll = styledScrollPane(pTable);
        pScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        payroll.add(pScroll, BorderLayout.CENTER);

        // Bottom label — total payroll cost
        JLabel payrollTotal = new JLabel("  Estimated Monthly Payroll Cost:  $" + formatMoney(totalPayroll));
        payrollTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        payrollTotal.setForeground(ACCENT_GREEN);
        payroll.add(payrollTotal, BorderLayout.SOUTH);
        grid.add(payroll);

        page.add(grid, BorderLayout.CENTER);

        // Refresh button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton refreshBtn = smallButton("↻ Refresh All", TEXT_MUTED);
        refreshBtn.addActionListener(e -> {
            contentArea.remove(page);
            showPanel("REPORTS");
        });
        footer.add(refreshBtn);
        page.add(footer, BorderLayout.SOUTH);

        return page;
    }

    // ─── UI HELPER COMPONENTS ─────────────────────────────────────────────────

    private JPanel darkPage() {
        JPanel p = new JPanel();
        p.setBackground(BG_DARK);
        return p;
    }

    private JPanel glowCard(String title) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDER_GLOW);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
        };
    }

    private JButton glowButton(String text, Color accent, ActionListener action) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = hovered ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(getFont());
                g2.setColor(hovered ? Color.BLACK : accent);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }

            @Override
            protected void paintBorder(Graphics g) {
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(Math.max(130, text.length() * 8 + 30), 34));
        if (action != null)
            btn.addActionListener(action);
        return btn;
    }

    private JButton smallButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SMALL);
        btn.setForeground(color);
        btn.setBackground(new Color(20, 25, 40));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(40, 50, 80), 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel sectionTitle(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setForeground(color);
        return lbl;
    }

    private JLabel statusBadge(String status) {
        JLabel lbl = new JLabel(" " + status + " ");
        lbl.setFont(FONT_SMALL);
        Color c = status.equals("Completed") ? ACCENT_GREEN : status.equals("Pending") ? ACCENT_ORANGE : ACCENT_PINK;
        lbl.setForeground(c);
        lbl.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 20));
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createLineBorder(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80)));
        return lbl;
    }

    private JTextField darkTextField(String placeholder, int cols) {
        JTextField f = new JTextField(cols) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(new Color(80, 100, 140));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 8, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                }
            }
        };
        f.setBackground(new Color(16, 22, 42));
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(ACCENT_CYAN);
        f.setFont(FONT_LABEL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 55, 90)),
                new EmptyBorder(6, 8, 6, 8)));
        return f;
    }

    private JComboBox<String> darkComboBox(String[] items) {
        JComboBox<String> box = new JComboBox<>(items);
        // Force BasicComboBoxUI to bypass macOS Aqua renderer
        box.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
        box.setBackground(new Color(232, 236, 248));
        box.setForeground(new Color(15, 20, 50));
        box.setFont(FONT_LABEL);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 130, 200)),
                new EmptyBorder(4, 6, 4, 6)));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    lbl.setBackground(new Color(0, 140, 200));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(new Color(232, 236, 248));
                    lbl.setForeground(new Color(15, 20, 50));
                }
                lbl.setBorder(new EmptyBorder(5, 8, 5, 8));
                return lbl;
            }
        });
        return box;
    }

    private JTable buildFancyTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(new Color(0, 212, 255, 40));
                    c.setForeground(TEXT_PRIMARY);
                } else {
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(14, 19, 34));
                    c.setForeground(TEXT_PRIMARY);
                }
                return c;
            }
        };
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_LABEL);
        table.setRowHeight(32);
        table.setSelectionBackground(new Color(0, 212, 255, 40));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(new Color(25, 35, 60));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(10, 14, 28));
        header.setForeground(ACCENT_CYAN);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 212, 255, 60)));
        header.setReorderingAllowed(false);
        return table;
    }

    private JScrollPane styledScrollPane(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(25, 35, 60)));
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        styleScrollBar(scroll.getVerticalScrollBar());
        styleScrollBar(scroll.getHorizontalScrollBar());
        return scroll;
    }

    private void styleScrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(0, 212, 255, 80);
                trackColor = new Color(10, 14, 28);
            }

            @Override
            protected JButton createDecreaseButton(int o) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int o) {
                return zeroButton();
            }

            JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    private JDialog styledDialog(String title, int w, int h) {
        JDialog dlg = new JDialog(this, title, true);
        dlg.setSize(w, h);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_PANEL);
        dlg.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_GLOW));
        return dlg;
    }

    /**
     * Returns a styled header panel placed at the NORTH of every add/edit dialog.
     * Provides a uniform look: icon + title in accent color + horizontal separator.
     */
    private JPanel buildDialogHeader(String icon, String title, Color accent) {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18),
                        getWidth(), 0, new Color(0, 0, 0, 0));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // bottom separator line
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        header.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 14));
        header.setOpaque(false);

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(accent);

        header.add(iconLbl);
        header.add(titleLbl);
        return header;
    }

    /** Styled button panel footer for dialogs — Cancel on left, action on right. */
    private JPanel buildDialogFooter(JButton cancel, JButton action) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(8, 10, 20));
                g.fillRect(0, 0, getWidth(), getHeight());
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(30, 40, 70));
                g2.drawLine(0, 0, getWidth(), 0);
            }
        };
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        panel.setOpaque(false);
        panel.add(cancel);
        panel.add(action);
        return panel;
    }

    private GridBagConstraints formGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);
        return gbc;
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, String label, Component comp, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MUTED);
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.65;
        form.add(comp, gbc);
    }

    private JPanel miniStatCard(String title, String value, Color accent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
        };
        card.setLayout(new GridLayout(2, 1, 0, 2));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel vLbl = new JLabel(value);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        vLbl.setForeground(accent);
        JLabel tLbl = new JLabel(title);
        tLbl.setFont(FONT_SMALL);
        tLbl.setForeground(TEXT_MUTED);
        card.add(vLbl);
        card.add(tLbl);
        return card;
    }

    // ─── UTILITY ──────────────────────────────────────────────────────────────
    private String formatMoney(double amount) {
        return String.format("%,.0f", amount);
    }

    private String getCategoryIcon(String category) {
        if (category == null)
            return "🚗";
        return switch (category.toLowerCase()) {
            case "suv" -> "🚙";
            case "sports" -> "🏎";
            case "electric" -> "⚡";
            case "luxury" -> "💎";
            case "hatchback" -> "🚘";
            default -> "🚗";
        };
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSuccess(String msg) {
        setStatus("✓  " + msg);
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmDelete(String name) {
        return JOptionPane.showConfirmDialog(this, "Delete \"" + name + "\"?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ★ AI FEATURE 1 — SMART CAR RECOMMENDER
    // ─────────────────────────────────────────────────────────────────────────
    private void showSmartRecommenderDialog() {
        List<Customer> customers = customerController.getAllCustomers();
        if (customers.isEmpty()) {
            showInfo("No customers found. Add customers first.");
            return;
        }

        JDialog dlg = styledDialog("AI Smart Car Recommender", 860, 640);
        dlg.setLayout(new BorderLayout(0, 0));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(80, 0, 180, 60),
                        getWidth(), 0, new Color(180, 0, 255, 30));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(160, 80, 255, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        header.setLayout(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(new EmptyBorder(0, 24, 0, 20));

        JPanel headerLeft = new JPanel(new GridLayout(2, 1, 0, 2));
        headerLeft.setOpaque(false);
        JLabel hTitle = new JLabel("🤖  Smart Car Recommender");
        hTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        hTitle.setForeground(new Color(200, 140, 255));
        JLabel hSub = new JLabel("Select a customer → AI matches inventory by budget, category & availability");
        hSub.setFont(FONT_SMALL);
        hSub.setForeground(TEXT_MUTED);
        headerLeft.add(hTitle);
        headerLeft.add(hSub);

        // Customer selector in header
        JComboBox<String> custSelector = darkComboBox(
                customers.stream().map(c -> c.getId() + " | " + c.getName()
                        + "  ·  Budget: $" + formatMoney(c.getBudget())
                        + "  ·  Prefers: " + c.getPreferredCarType()).toArray(String[]::new));
        custSelector.setPreferredSize(new Dimension(380, 34));
        custSelector.setMaximumSize(new Dimension(380, 34));

        header.add(headerLeft, BorderLayout.WEST);
        header.add(custSelector, BorderLayout.EAST);
        dlg.add(header, BorderLayout.NORTH);

        // ── Customer info strip ──────────────────────────────────────────────
        JPanel infoStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        infoStrip.setBackground(new Color(14, 10, 28));
        infoStrip.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 30, 100)));

        JLabel nameTag = new JLabel();
        nameTag.setFont(FONT_HEADER);
        nameTag.setForeground(TEXT_PRIMARY);
        JLabel budgetTag = new JLabel();
        budgetTag.setFont(FONT_HEADER);
        budgetTag.setForeground(ACCENT_GREEN);
        JLabel prefTag = new JLabel();
        prefTag.setFont(FONT_HEADER);
        prefTag.setForeground(new Color(180, 100, 255));
        JLabel matchTag = new JLabel();
        matchTag.setFont(FONT_SMALL);
        matchTag.setForeground(TEXT_MUTED);

        infoStrip.add(nameTag);
        infoStrip.add(budgetTag);
        infoStrip.add(prefTag);
        infoStrip.add(matchTag);

        // ── Results area ─────────────────────────────────────────────────────
        JPanel resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setBackground(BG_DARK);

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(BG_DARK);
        cardsPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JScrollPane resultsScroll = new JScrollPane(cardsPanel);
        resultsScroll.setBorder(null);
        resultsScroll.setBackground(BG_DARK);
        resultsScroll.getViewport().setBackground(BG_DARK);
        styleScrollBar(resultsScroll.getVerticalScrollBar());

        // ── "No results" placeholder ─────────────────────────────────────────
        JPanel placeholder = new JPanel(new GridBagLayout());
        placeholder.setBackground(BG_DARK);
        JLabel placeholderLbl = new JLabel(
                "<html><center>🤖<br><br>Select a customer above<br>to see AI-matched vehicles</center></html>");
        placeholderLbl.setFont(FONT_LABEL);
        placeholderLbl.setForeground(TEXT_MUTED);
        placeholderLbl.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.add(placeholderLbl);
        resultsContainer.add(placeholder, BorderLayout.CENTER);

        // ── Match engine logic ────────────────────────────────────────────────
        Runnable runMatch = () -> {
            int idx = custSelector.getSelectedIndex();
            if (idx < 0 || idx >= customers.size())
                return;
            Customer cust = customers.get(idx);

            // Score every available car
            List<Car> allCars = carController.getAllCars();
            List<double[]> scored = new ArrayList<>(); // [carIndex, score]

            for (int i = 0; i < allCars.size(); i++) {
                Car car = allCars.get(i);
                if (!car.isAvailable() || car.getStock() == 0)
                    continue;

                double score = 0;
                double budget = cust.getBudget();
                double price = car.getPrice();

                // Budget fit (0–40 pts): sweet spot = price is 85-100 % of budget
                if (price <= budget) {
                    double ratio = price / (budget > 0 ? budget : 1);
                    if (ratio >= 0.85)
                        score += 40;
                    else if (ratio >= 0.70)
                        score += 30;
                    else if (ratio >= 0.50)
                        score += 18;
                    else
                        score += 8;
                }
                // Category match (0–35 pts)
                String pref = cust.getPreferredCarType();
                if (pref != null && car.getCategory() != null
                        && pref.equalsIgnoreCase(car.getCategory()))
                    score += 35;
                else if (pref != null && car.getCategory() != null
                        && (pref.equalsIgnoreCase("SUV") && car.getCategory().equalsIgnoreCase("Luxury")
                                || pref.equalsIgnoreCase("Sports") && car.getCategory().equalsIgnoreCase("Luxury"))) {
                    score += 15; // soft match
                }
                // Stock bonus (0–15 pts): prefer well-stocked
                score += Math.min(car.getStock() * 3, 15);
                // New model year bonus (0–10 pts)
                int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                if (car.getYear() >= currentYear)
                    score += 10;
                else if (car.getYear() >= currentYear - 1)
                    score += 6;
                else if (car.getYear() >= currentYear - 2)
                    score += 3;

                if (score > 0)
                    scored.add(new double[] { i, score });
            }

            // Sort descending by score, take top 5
            scored.sort((a, b) -> Double.compare(b[1], a[1]));
            List<double[]> top = scored.subList(0, Math.min(5, scored.size()));

            // Update info strip
            nameTag.setText("👤 " + cust.getName());
            budgetTag.setText("💰 Budget: $" + formatMoney(cust.getBudget()));
            prefTag.setText("🎯 Prefers: " + cust.getPreferredCarType());
            matchTag.setText("Found " + top.size() + " match" + (top.size() != 1 ? "es" : "")
                    + " from " + allCars.size() + " vehicles");

            // Build result cards
            cardsPanel.removeAll();
            resultsContainer.removeAll();

            if (top.isEmpty()) {
                JPanel noMatch = new JPanel(new GridBagLayout());
                noMatch.setBackground(BG_DARK);
                JLabel nm = new JLabel("<html><center>😕<br><br>No matching vehicles found<br>"
                        + "for this budget / category combination.<br>"
                        + "Try updating the customer's preferences.</center></html>");
                nm.setFont(FONT_LABEL);
                nm.setForeground(TEXT_MUTED);
                noMatch.add(nm);
                resultsContainer.add(noMatch, BorderLayout.CENTER);
            } else {
                Color[] rankColors = {
                        new Color(255, 200, 30), // gold
                        new Color(192, 192, 192), // silver
                        new Color(205, 127, 50), // bronze
                        new Color(0, 212, 255), // cyan
                        new Color(138, 43, 226) // purple
                };
                String[] rankLabels = { "#1 BEST MATCH", "#2 GREAT FIT", "#3 GOOD FIT", "#4 WORTH CONSIDERING",
                        "#5 ALTERNATIVE" };

                for (int r = 0; r < top.size(); r++) {
                    Car car = allCars.get((int) top.get(r)[0]);
                    double score = top.get(r)[1];
                    int rank = r;
                    Color rankColor = rankColors[r];

                    // How much of budget this car uses
                    double budgetUsed = cust.getBudget() > 0 ? (car.getPrice() / cust.getBudget()) * 100 : 0;
                    boolean catMatch = cust.getPreferredCarType() != null
                            && cust.getPreferredCarType().equalsIgnoreCase(car.getCategory());
                    boolean withinBudget = car.getPrice() <= cust.getBudget();

                    JPanel card = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(BG_CARD);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                            g2.setColor(new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 50));
                            g2.setStroke(new BasicStroke(1.5f));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                            // left accent bar
                            g2.setColor(rankColor);
                            g2.fillRoundRect(0, 12, 4, getHeight() - 24, 4, 4);
                        }
                    };
                    card.setLayout(new BorderLayout(14, 0));
                    card.setBorder(new EmptyBorder(14, 20, 14, 20));
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Left: rank badge + car name
                    JPanel leftBlock = new JPanel(new GridLayout(3, 1, 0, 3));
                    leftBlock.setOpaque(false);
                    leftBlock.setPreferredSize(new Dimension(280, 90));
                    JLabel rankLbl = new JLabel(rankLabels[rank]);
                    rankLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    rankLbl.setForeground(rankColor);
                    JLabel carName = new JLabel(getCategoryIcon(car.getCategory())
                            + "  " + car.getBrand() + " " + car.getModel() + " (" + car.getYear() + ")");
                    carName.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    carName.setForeground(TEXT_PRIMARY);
                    JLabel carMeta = new JLabel(car.getCategory() + "  ·  " + car.getFuelType()
                            + "  ·  " + car.getTransmission() + "  ·  " + car.getColor());
                    carMeta.setFont(FONT_SMALL);
                    carMeta.setForeground(TEXT_MUTED);
                    leftBlock.add(rankLbl);
                    leftBlock.add(carName);
                    leftBlock.add(carMeta);

                    // Center: match score bar + tags
                    JPanel centerBlock = new JPanel();
                    centerBlock.setLayout(new BoxLayout(centerBlock, BoxLayout.Y_AXIS));
                    centerBlock.setOpaque(false);

                    int scorePct = (int) Math.min(score, 100);
                    JPanel scoreBarOuter = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(30, 35, 60));
                            g2.fillRoundRect(0, 6, getWidth(), 10, 10, 10);
                            int filled = (int) (getWidth() * scorePct / 100.0);
                            GradientPaint gp = new GradientPaint(0, 0, rankColor,
                                    filled, 0,
                                    new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 120));
                            g2.setPaint(gp);
                            g2.fillRoundRect(0, 6, filled, 10, 10, 10);
                        }

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(200, 22);
                        }
                    };
                    scoreBarOuter.setOpaque(false);
                    scoreBarOuter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                    scoreBarOuter.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JLabel scoreLbl = new JLabel("Match Score: " + scorePct + " / 100");
                    scoreLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    scoreLbl.setForeground(rankColor);
                    scoreLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                    tags.setOpaque(false);
                    tags.setAlignmentX(Component.LEFT_ALIGNMENT);
                    if (catMatch)
                        tags.add(pill("✓ Category Match", ACCENT_GREEN));
                    if (withinBudget)
                        tags.add(pill("✓ Within Budget", ACCENT_CYAN));
                    else
                        tags.add(pill("⚠ Over Budget", ACCENT_ORANGE));
                    if (car.getStock() > 3)
                        tags.add(pill("● In Stock", ACCENT_GREEN));
                    else if (car.getStock() > 0)
                        tags.add(pill("⚠ Low Stock", ACCENT_ORANGE));

                    centerBlock.add(scoreLbl);
                    centerBlock.add(Box.createVerticalStrut(4));
                    centerBlock.add(scoreBarOuter);
                    centerBlock.add(Box.createVerticalStrut(6));
                    centerBlock.add(tags);

                    // Right: price + budget % gauge
                    JPanel rightBlock = new JPanel(new GridLayout(3, 1, 0, 4));
                    rightBlock.setOpaque(false);
                    rightBlock.setPreferredSize(new Dimension(150, 90));
                    JLabel priceLbl = new JLabel("$" + formatMoney(car.getPrice()));
                    priceLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    priceLbl.setForeground(withinBudget ? ACCENT_GREEN : ACCENT_ORANGE);
                    priceLbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    JLabel stockLbl = new JLabel("Stock: " + car.getStock() + "  units");
                    stockLbl.setFont(FONT_SMALL);
                    stockLbl.setForeground(TEXT_MUTED);
                    stockLbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    String pct = String.format("%.0f%%", Math.min(budgetUsed, 999));
                    JLabel budgetLbl = new JLabel("Uses " + pct + " of budget");
                    budgetLbl.setFont(FONT_SMALL);
                    budgetLbl.setForeground(budgetUsed <= 100 ? TEXT_MUTED : ACCENT_ORANGE);
                    budgetLbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    rightBlock.add(priceLbl);
                    rightBlock.add(stockLbl);
                    rightBlock.add(budgetLbl);

                    card.add(leftBlock, BorderLayout.WEST);
                    card.add(centerBlock, BorderLayout.CENTER);
                    card.add(rightBlock, BorderLayout.EAST);
                    cardsPanel.add(card);
                    cardsPanel.add(Box.createVerticalStrut(10));
                }

                resultsContainer.add(resultsScroll, BorderLayout.CENTER);
            }

            resultsContainer.revalidate();
            resultsContainer.repaint();
            cardsPanel.revalidate();
            cardsPanel.repaint();
        };

        custSelector.addActionListener(e -> runMatch.run());
        runMatch.run(); // auto-run for first customer

        JPanel centerWrap = new JPanel(new BorderLayout(0, 0));
        centerWrap.setBackground(BG_DARK);
        centerWrap.add(infoStrip, BorderLayout.NORTH);
        centerWrap.add(resultsContainer, BorderLayout.CENTER);
        dlg.add(centerWrap, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        footer.setBackground(new Color(8, 10, 20));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 40, 70)));
        footer.add(new JLabel("  Powered by AutoElite AI Engine  ·  Rule-based scoring v1.0"));
        ((JLabel) footer.getComponent(0)).setFont(FONT_SMALL);
        ((JLabel) footer.getComponent(0)).setForeground(new Color(80, 60, 120));
        footer.add(glowButton("Close", ACCENT_PURPLE, e -> dlg.dispose()));
        dlg.add(footer, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    /** Pill-shaped label for tags inside recommender cards */
    private JLabel pill(String text, Color color) {
        JLabel lbl = new JLabel("  " + text + "  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 110));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(color);
        lbl.setOpaque(false);
        return lbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ★ AI FEATURE 2 — PREDICTIVE PRICING ENGINE
    // ─────────────────────────────────────────────────────────────────────────
    private void showPricingEngineDialog() {
        List<Car> allCars = carController.getAllCars();
        if (allCars.isEmpty()) {
            showInfo("No vehicles in inventory. Add vehicles first.");
            return;
        }

        // Full-screen, non-modal (minimizable from taskbar), resizable
        JDialog dlg = new JDialog(this, "AutoElite  AI Predictive Pricing Engine", false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(screen.width, screen.height);
        dlg.setLocation(0, 0);
        dlg.setResizable(true);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.getContentPane().setBackground(BG_DARK);
        Color GOLD = new Color(255, 200, 60);
        Color GOLD_DIM = new Color(160, 120, 20);

        // TOP BAR
        JPanel topBar = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(80, 50, 0), getWidth(), 0, new Color(18, 12, 28));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(180, 130, 0, 70));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 86));
        topBar.setBorder(new EmptyBorder(0, 36, 0, 30));

        JPanel titleBlock = new JPanel(new GridLayout(3, 1, 0, 3));
        titleBlock.setOpaque(false);
        JLabel mainTitle = new JLabel("  Predictive Pricing Engine");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainTitle.setForeground(GOLD);
        JLabel sub1 = new JLabel("What should I sell this car for?");
        sub1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sub1.setForeground(new Color(200, 160, 60));
        JLabel sub2 = new JLabel(
                "Analyses your completed sales history to recommend the price that maximises profit while ensuring the car actually sells.");
        sub2.setFont(FONT_SMALL);
        sub2.setForeground(new Color(140, 115, 55));
        titleBlock.add(mainTitle);
        titleBlock.add(sub1);
        titleBlock.add(sub2);

        JPanel pickerWrap = new JPanel(new GridLayout(2, 1, 0, 5));
        pickerWrap.setOpaque(false);
        pickerWrap.setPreferredSize(new Dimension(500, 65));
        JLabel pickLbl = new JLabel("   Select a vehicle to analyse:");
        pickLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pickLbl.setForeground(GOLD_DIM);
        JComboBox<String> carSelector = darkComboBox(
                allCars.stream().map(c -> c.getId() + "  |  " + c.getBrand() + " " + c.getModel()
                        + " (" + c.getYear() + ")  -  Listed at $" + formatMoney(c.getPrice())).toArray(String[]::new));
        carSelector.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pickerWrap.add(pickLbl);
        pickerWrap.add(carSelector);
        topBar.add(titleBlock, BorderLayout.CENTER);
        topBar.add(pickerWrap, BorderLayout.EAST);
        dlg.add(topBar, BorderLayout.NORTH);

        // BODY: 3 columns
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(BG_DARK);

        // LEFT: price cards
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setBackground(new Color(10, 13, 24));
        JScrollPane leftScroll = new JScrollPane(leftCol);
        leftScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(35, 45, 75)));
        leftScroll.setBackground(new Color(10, 13, 24));
        leftScroll.getViewport().setBackground(new Color(10, 13, 24));
        styleScrollBar(leftScroll.getVerticalScrollBar());
        leftScroll.setPreferredSize(new Dimension(380, 0));
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // CENTER: chart
        JPanel centerCol = new JPanel(new BorderLayout(0, 0));
        centerCol.setBackground(BG_DARK);
        JPanel chartTitleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 16));
        chartTitleBar.setBackground(new Color(12, 15, 28));
        chartTitleBar.setPreferredSize(new Dimension(0, 62));
        chartTitleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(35, 45, 75)));
        JLabel ctMain = new JLabel("Price Intelligence Chart");
        ctMain.setFont(new Font("Segoe UI", Font.BOLD, 17));
        ctMain.setForeground(GOLD);
        JLabel ctSub = new JLabel("  Each bar is a different pricing strategy for this vehicle");
        ctSub.setFont(FONT_LABEL);
        ctSub.setForeground(TEXT_MUTED);
        chartTitleBar.add(ctMain);
        chartTitleBar.add(ctSub);
        centerCol.add(chartTitleBar, BorderLayout.NORTH);

        JPanel drawArea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Object prop = getClientProperty("vals");
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                g2.setColor(BG_DARK);
                g2.fillRect(0, 0, W, H);
                if (prop == null) {
                    g2.setFont(FONT_LABEL);
                    g2.setColor(TEXT_MUTED);
                    String msg = "Select a vehicle above to see the pricing chart";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                    return;
                }
                double[] vals = (double[]) prop;
                double fQ = vals[0], fL = vals[1], fLp = vals[2], fS = vals[3], fH = vals[4], fP = vals[5];
                double[] values = { fQ, fL, fLp, fS, fH, fP };
                String[] labels = { "Quick\nSell", "Optimal\nLow", "List\nPrice", "AI\nSuggested", "Optimal\nHigh",
                        "Premium\nPrice" };
                String[] sublbls = { "Move fast", "Safe floor", "Currently listed", "Sweet spot", "Max value",
                        "Top earner" };
                Color[] bcs = {
                        new Color(255, 60, 120), new Color(50, 220, 100), new Color(0, 200, 240),
                        new Color(255, 200, 40), new Color(255, 150, 30), new Color(160, 60, 255)
                };
                int padL = 110, padR = 60, padT = 40, padB = 130;
                int cW = W - padL - padR, cH = H - padT - padB;
                double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
                for (double v : values) {
                    minV = Math.min(minV, v);
                    maxV = Math.max(maxV, v);
                }
                double pd = (maxV - minV) * 0.30, cMin = Math.max(0, minV - pd), cMax = maxV + pd, rng = cMax - cMin;
                // Grid
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                for (int i = 0; i <= 6; i++) {
                    double gv = cMin + rng * i / 6;
                    int gy = padT + cH - (int) (cH * (gv - cMin) / rng);
                    g2.setColor(new Color(35, 45, 75));
                    g2.setStroke(new BasicStroke(0.5f, 0, 0, 10f, new float[] { 5f, 8f }, 0f));
                    g2.drawLine(padL, gy, padL + cW, gy);
                    g2.setStroke(new BasicStroke(1f));
                    g2.setColor(new Color(130, 140, 170));
                    String gl = "$" + formatMoney(gv);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(gl, padL - fm.stringWidth(gl) - 10, gy + 5);
                }
                // AI dashed line
                int sy = padT + cH - (int) (cH * (fS - cMin) / rng);
                g2.setColor(new Color(255, 200, 40, 55));
                g2.setStroke(new BasicStroke(1.5f, 0, 0, 10f, new float[] { 12f, 7f }, 0f));
                g2.drawLine(padL, sy, padL + cW, sy);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(new Color(255, 200, 40, 170));
                String asl = "AI Suggested";
                FontMetrics af = g2.getFontMetrics();
                g2.drawString(asl, padL + cW - af.stringWidth(asl) - 10, sy - 7);
                // Bars
                int nB = values.length, gW = cW / nB, bW = (int) (gW * 0.58), bGap = (gW - bW) / 2;
                for (int i = 0; i < nB; i++) {
                    double v = values[i];
                    int bH = (int) (cH * (v - cMin) / rng);
                    int bX = padL + i * gW + bGap, bY = padT + cH - bH;
                    Color bc = bcs[i];
                    // glow
                    g2.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 15));
                    g2.fillRoundRect(bX - 8, bY - 6, bW + 16, bH + 14, 14, 14);
                    // gradient fill
                    GradientPaint bp = new GradientPaint(bX, bY,
                            new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 220),
                            bX, bY + bH, new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 55));
                    g2.setPaint(bp);
                    g2.fillRoundRect(bX, bY, bW, bH, 12, 12);
                    // border
                    g2.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 175));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(bX, bY, bW - 1, bH - 1, 12, 12);
                    g2.setStroke(new BasicStroke(1f));
                    // sheen
                    GradientPaint sh = new GradientPaint(bX, bY, new Color(255, 255, 255, 60), bX, bY + 22,
                            new Color(255, 255, 255, 0));
                    g2.setPaint(sh);
                    g2.fillRoundRect(bX + 3, bY + 3, bW - 6, 22, 6, 6);
                    // top dot
                    g2.setColor(bc);
                    g2.fillOval(bX + bW / 2 - 6, bY - 7, 13, 13);
                    g2.setColor(new Color(255, 255, 255, 150));
                    g2.fillOval(bX + bW / 2 - 3, bY - 4, 7, 7);
                    // value label
                    String vs = "$" + formatMoney(v);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    FontMetrics vf = g2.getFontMetrics();
                    if (bH > 48) {
                        g2.setColor(new Color(255, 255, 255, 225));
                        g2.drawString(vs, bX + (bW - vf.stringWidth(vs)) / 2, bY + 25);
                    } else {
                        g2.setColor(bc);
                        g2.drawString(vs, bX + (bW - vf.stringWidth(vs)) / 2, bY - 13);
                    }
                    // X label
                    String[] lpts = labels[i].split("\n");
                    int ly = padT + cH + 26, cx = bX + bW / 2;
                    if (i == 3) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                        g2.setColor(new Color(255, 200, 40));
                    } else {
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        g2.setColor(new Color(155, 165, 195));
                    }
                    for (String lp : lpts) {
                        FontMetrics lf = g2.getFontMetrics();
                        g2.drawString(lp, cx - lf.stringWidth(lp) / 2, ly);
                        ly += 17;
                    }
                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    g2.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 155));
                    FontMetrics sf2 = g2.getFontMetrics();
                    String sub3 = sublbls[i];
                    g2.drawString(sub3, cx - sf2.stringWidth(sub3) / 2, ly);
                }
                // Axes
                g2.setColor(new Color(40, 55, 88));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(padL, padT, padL, padT + cH);
                g2.drawLine(padL, padT + cH, padL + cW, padT + cH);
                g2.setStroke(new BasicStroke(1f));
                // Legend
                String[] legs = { "Quick Sell", "Optimal Low", "List Price", "AI Suggested", "Optimal High",
                        "Premium" };
                int lx = padL, ly2 = H - 16;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                for (int i = 0; i < legs.length; i++) {
                    g2.setColor(bcs[i]);
                    g2.fillRoundRect(lx, ly2 - 10, 14, 14, 4, 4);
                    g2.setColor(i == 3 ? new Color(255, 200, 40) : new Color(165, 175, 205));
                    FontMetrics lf = g2.getFontMetrics();
                    g2.drawString(legs[i], lx + 18, ly2 + 1);
                    lx += lf.stringWidth(legs[i]) + 34;
                }
            }
        };
        drawArea.setBackground(BG_DARK);
        centerCol.add(drawArea, BorderLayout.CENTER);

        // RIGHT: explanations
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setBackground(new Color(10, 14, 27));
        JScrollPane rightScroll = new JScrollPane(rightCol);
        rightScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(35, 45, 75)));
        rightScroll.setBackground(new Color(10, 14, 27));
        rightScroll.getViewport().setBackground(new Color(10, 14, 27));
        styleScrollBar(rightScroll.getVerticalScrollBar());
        rightScroll.setPreferredSize(new Dimension(325, 0));
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightCol.setBorder(new EmptyBorder(26, 24, 26, 24));

        JLabel rTitle = new JLabel("  What does each price mean?");
        rTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        rTitle.setForeground(GOLD);
        rTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightCol.add(rTitle);
        rightCol.add(Box.createVerticalStrut(16));

        rightCol.add(explanCard("AI Suggested Price", new Color(255, 200, 40),
                "The single best price to list this car at today.\n\n" +
                        "How it's calculated: If this exact car sold before, those real prices count 60%. " +
                        "Similar-priced cars in your database count 30%. Your current list price is a 10% anchor. " +
                        "The more sales you record, the sharper this gets."));
        rightCol.add(Box.createVerticalStrut(12));

        rightCol.add(explanCard("Quick Sell Price  (-10%)", new Color(255, 80, 120),
                "Knock 10% off when you need this car gone fast: end of quarter, new stock arriving, " +
                        "or the car has been sitting 60+ days.\n\nExpect it to sell within the week with minimal negotiation."));
        rightCol.add(Box.createVerticalStrut(12));

        rightCol.add(explanCard("Optimal Range  (Low to High)", new Color(50, 220, 100),
                "The safe zone where you earn solid margin and buyers feel the price is fair.\n\n" +
                        "Low = your floor, still profitable.\n" +
                        "High = ceiling, earns more per unit but may take a few extra days to sell."));
        rightCol.add(Box.createVerticalStrut(12));

        rightCol.add(explanCard("List Price  (current)", new Color(0, 200, 240),
                "What you currently show to buyers. The AI arrow tells you whether you are overpriced, " +
                        "underpriced, or right on target compared to what comparable cars actually sold for in your records."));
        rightCol.add(Box.createVerticalStrut(12));

        rightCol.add(explanCard("Premium Price  (+8%)", new Color(160, 60, 255),
                "For special cases: rare colour, ultra-low mileage, full service history, or bundled accessories. " +
                        "Attracts fewer enquiries but higher-quality buyers who will not negotiate hard."));
        rightCol.add(Box.createVerticalStrut(22));

        JSeparator sdiv = new JSeparator();
        sdiv.setForeground(new Color(40, 50, 80));
        sdiv.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sdiv.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightCol.add(sdiv);
        rightCol.add(Box.createVerticalStrut(16));

        JLabel confTitle = new JLabel("Confidence Levels");
        confTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confTitle.setForeground(TEXT_PRIMARY);
        confTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightCol.add(confTitle);
        rightCol.add(Box.createVerticalStrut(10));

        String[][] confRows = {
                { "HIGH (green)", "3+ completed sales of this exact car. Data-driven and reliable." },
                { "MEDIUM (orange)", "Some comparable cars found. Solid estimate, gets better with more sales." },
                { "LOW (red)",
                        "No matching sales yet. Based on depreciation model (~3.5% per year). Record more sales to improve." }
        };
        for (String[] cr : confRows) {
            JPanel cr2 = new JPanel(new BorderLayout(8, 0));
            cr2.setOpaque(false);
            cr2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            cr2.setAlignmentX(Component.LEFT_ALIGNMENT);
            cr2.setBorder(new EmptyBorder(0, 0, 8, 0));
            JLabel k = new JLabel(cr[0]);
            k.setFont(new Font("Segoe UI", Font.BOLD, 11));
            k.setForeground(new Color(200, 170, 80));
            k.setPreferredSize(new Dimension(110, 20));
            JTextArea v2 = makeExplanText(cr[1]);
            v2.setFont(FONT_SMALL);
            cr2.add(k, BorderLayout.WEST);
            cr2.add(v2, BorderLayout.CENTER);
            rightCol.add(cr2);
        }

        body.add(leftScroll, BorderLayout.WEST);
        body.add(centerCol, BorderLayout.CENTER);
        body.add(rightScroll, BorderLayout.EAST);
        dlg.add(body, BorderLayout.CENTER);

        // BOTTOM BAR
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(8, 10, 18));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 50, 80)),
                new EmptyBorder(12, 36, 12, 30)));
        JLabel powered = new JLabel(
                "Powered by AutoElite AI Engine  |  Pricing model v1.0  |  Source: completed sales in your database");
        powered.setFont(FONT_SMALL);
        powered.setForeground(new Color(90, 75, 30));
        JButton closeBtn = glowButton("Close", GOLD, e -> dlg.dispose());
        bottomBar.add(powered, BorderLayout.WEST);
        bottomBar.add(closeBtn, BorderLayout.EAST);
        dlg.add(bottomBar, BorderLayout.SOUTH);

        // LIVE UPDATE
        Runnable updatePricing = () -> {
            int idx = carSelector.getSelectedIndex();
            if (idx < 0 || idx >= allCars.size())
                return;
            Car car = allCars.get(idx);
            List<Sale> sales = saleController.getAllSales();
            List<Double> exact = new ArrayList<>(), cat = new ArrayList<>(), all = new ArrayList<>();
            for (Sale s : sales) {
                if (!"Completed".equalsIgnoreCase(s.getStatus()))
                    continue;
                all.add(s.getSalePrice());
                if (s.getCarId() == car.getId())
                    exact.add(s.getSalePrice());
                if (Math.abs(s.getSalePrice() - car.getPrice()) < car.getPrice() * 0.35)
                    cat.add(s.getSalePrice());
            }
            double lp = car.getPrice();
            double ae = exact.isEmpty() ? lp : exact.stream().mapToDouble(d -> d).average().orElse(lp);
            double ac = cat.isEmpty() ? lp : cat.stream().mapToDouble(d -> d).average().orElse(lp);
            double am = all.isEmpty() ? lp : all.stream().mapToDouble(d -> d).average().orElse(lp);
            double sug;
            if (!exact.isEmpty())
                sug = ae * 0.60 + ac * 0.30 + lp * 0.10;
            else if (!cat.isEmpty())
                sug = ac * 0.55 + lp * 0.45;
            else {
                int cy = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                sug = lp * Math.max(0.5, 1 - (cy - car.getYear()) * 0.035);
            }
            double lo = sug * 0.95, hi = sug * 1.05, qs = sug * 0.90, pr = sug * 1.08;
            double delta = sug - lp;
            boolean up = delta >= 0;
            String dStr = (up ? "+ $" : "- $") + formatMoney(Math.abs(delta));
            Color dClr = up ? ACCENT_GREEN : ACCENT_ORANGE;
            String confTxt;
            Color confClr;
            if (exact.size() >= 3) {
                confTxt = "HIGH";
                confClr = ACCENT_GREEN;
            } else if (cat.size() >= 2) {
                confTxt = "MEDIUM";
                confClr = ACCENT_ORANGE;
            } else {
                confTxt = "LOW  (limited data)";
                confClr = ACCENT_PINK;
            }

            // Rebuild left column
            leftCol.removeAll();
            leftCol.setBorder(new EmptyBorder(28, 26, 28, 26));
            leftCol.add(pricingCard(car.getBrand() + " " + car.getModel(),
                    car.getYear() + "  |  " + car.getCategory() + "  |  " + car.getFuelType(),
                    null, TEXT_PRIMARY, new Color(255, 200, 60, 28)));
            leftCol.add(Box.createVerticalStrut(28));
            leftCol.add(pricingDivider("PRICE RECOMMENDATIONS"));
            leftCol.add(Box.createVerticalStrut(14));
            leftCol.add(bigPriceCard("CURRENT LIST PRICE", "$" + formatMoney(lp),
                    "What buyers see right now", ACCENT_CYAN, null, null));
            leftCol.add(Box.createVerticalStrut(12));
            leftCol.add(bigPriceCard("AI SUGGESTED PRICE", "$" + formatMoney(sug),
                    dStr + " vs your list price", new Color(255, 200, 40), dStr, dClr));
            leftCol.add(Box.createVerticalStrut(12));
            JPanel rangeRow = new JPanel(new GridLayout(1, 2, 10, 0));
            rangeRow.setOpaque(false);
            rangeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            rangeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            rangeRow.add(compactPriceCard("Optimal Low", "$" + formatMoney(lo), ACCENT_GREEN));
            rangeRow.add(compactPriceCard("Optimal High", "$" + formatMoney(hi), ACCENT_ORANGE));
            leftCol.add(rangeRow);
            leftCol.add(Box.createVerticalStrut(12));
            leftCol.add(bigPriceCard("QUICK SELL  (-10%)", "$" + formatMoney(qs),
                    "Move it fast, minimal negotiation", ACCENT_PINK, null, null));
            leftCol.add(Box.createVerticalStrut(12));
            leftCol.add(bigPriceCard("PREMIUM  (+8%)", "$" + formatMoney(pr),
                    "Max profit, qualified buyers", ACCENT_PURPLE, null, null));
            leftCol.add(Box.createVerticalStrut(28));
            leftCol.add(pricingDivider("DATA BEHIND THIS PREDICTION"));
            leftCol.add(Box.createVerticalStrut(12));
            String[][] rows = {
                    { "Exact car sales:", String.valueOf(exact.size()) },
                    { "Comparable sales:", String.valueOf(cat.size()) },
                    { "Total market sales:", String.valueOf(all.size()) },
                    { "Avg exact sale:", exact.isEmpty() ? "No data yet" : "$" + formatMoney(ae) },
                    { "Market average:", all.isEmpty() ? "No data yet" : "$" + formatMoney(am) },
            };
            for (String[] row : rows) {
                leftCol.add(dataRow(row[0], row[1]));
                leftCol.add(Box.createVerticalStrut(7));
            }
            leftCol.add(Box.createVerticalStrut(24));
            leftCol.add(pricingDivider("CONFIDENCE"));
            leftCol.add(Box.createVerticalStrut(16));
            JLabel cl = new JLabel("  " + confTxt);
            cl.setFont(new Font("Segoe UI", Font.BOLD, 17));
            cl.setForeground(confClr);
            cl.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftCol.add(cl);
            leftCol.revalidate();
            leftCol.repaint();
            drawArea.putClientProperty("vals", new double[] { qs, lo, lp, sug, hi, pr });
            drawArea.repaint();
        };

        carSelector.addActionListener(e -> updatePricing.run());
        updatePricing.run();
        dlg.setVisible(true);
    }

    // Pricing sub-component helpers
    private JPanel bigPriceCard(String title, String price, String sub, Color accent, String badge, Color badgeClr) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 65));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(accent);
                g2.fillRect(0, 10, 4, getHeight() - 20);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 20, 16, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tl.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pl = new JLabel(price);
        pl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        pl.setForeground(accent);
        pl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        subRow.setOpaque(false);
        subRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sl = new JLabel(sub);
        sl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sl.setForeground(TEXT_MUTED);
        subRow.add(sl);
        if (badge != null && badgeClr != null) {
            JLabel bdg = new JLabel("  " + badge + "  ");
            bdg.setFont(new Font("Segoe UI", Font.BOLD, 11));
            bdg.setForeground(badgeClr);
            bdg.setBackground(new Color(badgeClr.getRed(), badgeClr.getGreen(), badgeClr.getBlue(), 25));
            bdg.setOpaque(true);
            subRow.add(bdg);
        }
        card.add(tl);
        card.add(Box.createVerticalStrut(6));
        card.add(pl);
        card.add(Box.createVerticalStrut(5));
        card.add(subRow);
        return card;
    }

    private JPanel compactPriceCard(String title, String price, Color accent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tl.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 185));
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pl = new JLabel(price);
        pl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        pl.setForeground(accent);
        pl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tl);
        card.add(Box.createVerticalStrut(5));
        card.add(pl);
        return card;
    }

    private JPanel pricingCard(String title, String sub, String value, Color titleClr, Color bg) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bg != null) {
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.setColor(new Color(200, 160, 30, 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        tl.setForeground(titleClr);
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sl = new JLabel(sub);
        sl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sl.setForeground(TEXT_MUTED);
        sl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tl);
        card.add(Box.createVerticalStrut(6));
        card.add(sl);
        return card;
    }

    private JLabel pricingDivider(String label) {
        JLabel l = new JLabel("  " + label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(145, 118, 48));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        l.setPreferredSize(new Dimension(0, 32));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(65, 55, 20)),
                new EmptyBorder(6, 0, 6, 0)));
        return l;
    }

    private JPanel dataRow(String label, String val) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setPreferredSize(new Dimension(0, 36));
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ll = new JLabel(label);
        ll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ll.setForeground(TEXT_MUTED);
        JLabel vl = new JLabel(val);
        vl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        vl.setForeground(TEXT_PRIMARY);
        vl.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(ll, BorderLayout.WEST);
        row.add(vl, BorderLayout.EAST);
        return row;
    }

    private JPanel explanCard(String title, Color accent, String bodyText) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(accent);
                g2.fillRect(0, 12, 3, getHeight() - 24);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tl.setForeground(accent);
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tl);
        card.add(Box.createVerticalStrut(8));
        JTextArea ta = makeExplanText(bodyText);
        card.add(ta);
        return card;
    }

    private JTextArea makeExplanText(String text) {
        JTextArea ta = new JTextArea(text);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ta.setForeground(new Color(165, 175, 205));
        ta.setBackground(new Color(0, 0, 0, 0));
        ta.setOpaque(false);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        return ta;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AI FEATURE 3 — CHURN PREDICTION
    // Identifies customers at risk of never returning, scores them, and
    // shows actionable follow-up suggestions ranked by urgency.
    // ═════════════════════════════════════════════════════════════════════════
    private void showChurnPredictionDialog() {
        List<Customer> customers = customerController.getAllCustomers();
        if (customers.isEmpty()) {
            showInfo("No customers found. Add customers first.");
            return;
        }

        // Full-screen non-modal
        JDialog dlg = new JDialog(this, "AutoElite  —  AI Churn Prediction Engine", false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(screen.width, screen.height);
        dlg.setLocation(0, 0);
        dlg.setResizable(true);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.getContentPane().setBackground(BG_DARK);
        Color CHURN_RED = new Color(255, 80, 60);
        Color CHURN_ORANGE = new Color(255, 160, 40);
        Color CHURN_GREEN = new Color(50, 220, 120);
        Color CHURN_ACCENT = new Color(255, 100, 50);

        // ── TOP BAR ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(80, 20, 0), getWidth(), 0, new Color(18, 10, 25));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(200, 80, 30, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 90));
        topBar.setBorder(new EmptyBorder(0, 36, 0, 36));

        JPanel titleBlock = new JPanel(new GridLayout(3, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel t1 = new JLabel("🚨  Churn Prediction Engine");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 24));
        t1.setForeground(CHURN_ACCENT);
        JLabel t2 = new JLabel("Which customers are at risk of never coming back?");
        t2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t2.setForeground(new Color(210, 130, 60));
        JLabel t3 = new JLabel(
                "Scores every customer by days since last contact, purchase history, test drives, budget match, and engagement — then ranks who needs attention most.");
        t3.setFont(FONT_SMALL);
        t3.setForeground(new Color(160, 100, 55));
        titleBlock.add(t1);
        titleBlock.add(t2);
        titleBlock.add(t3);

        // Risk filter in header
        JPanel filterWrap = new JPanel(new GridLayout(2, 1, 0, 6));
        filterWrap.setOpaque(false);
        filterWrap.setPreferredSize(new Dimension(320, 70));
        JLabel filterLbl = new JLabel("   Filter by risk level:");
        filterLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        filterLbl.setForeground(new Color(180, 120, 50));
        JComboBox<String> filterBox = darkComboBox(new String[] { "All Customers", "Critical Risk Only", "High Risk",
                "Medium Risk", "Low Risk (Healthy)" });
        filterBox.setFont(new Font("Segoe UI", Font.BOLD, 13));
        filterWrap.add(filterLbl);
        filterWrap.add(filterBox);

        topBar.add(titleBlock, BorderLayout.CENTER);
        topBar.add(filterWrap, BorderLayout.EAST);
        dlg.add(topBar, BorderLayout.NORTH);

        // ── BODY: LEFT summary panel | RIGHT customer cards ──────────────────
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(BG_DARK);

        // LEFT: aggregate stats
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setBackground(new Color(10, 12, 22));
        leftCol.setPreferredSize(new Dimension(290, 0));
        leftCol.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(40, 30, 20)));
        JScrollPane leftScroll = new JScrollPane(leftCol);
        leftScroll.setBorder(null);
        leftScroll.setBackground(new Color(10, 12, 22));
        leftScroll.getViewport().setBackground(new Color(10, 12, 22));
        styleScrollBar(leftScroll.getVerticalScrollBar());
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setPreferredSize(new Dimension(290, 0));

        // RIGHT: scrollable customer risk cards
        JPanel cardsCol = new JPanel();
        cardsCol.setLayout(new BoxLayout(cardsCol, BoxLayout.Y_AXIS));
        cardsCol.setBackground(BG_DARK);
        cardsCol.setBorder(new EmptyBorder(20, 28, 20, 28));
        JScrollPane cardsScroll = new JScrollPane(cardsCol);
        cardsScroll.setBorder(null);
        cardsScroll.setBackground(BG_DARK);
        cardsScroll.getViewport().setBackground(BG_DARK);
        styleScrollBar(cardsScroll.getVerticalScrollBar());

        body.add(leftScroll, BorderLayout.WEST);
        body.add(cardsScroll, BorderLayout.CENTER);
        dlg.add(body, BorderLayout.CENTER);

        // ── BOTTOM BAR ───────────────────────────────────────────────────────
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(8, 10, 18));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 30, 20)),
                new EmptyBorder(12, 36, 12, 30)));
        JLabel powered = new JLabel(
                "Powered by AutoElite AI Engine  |  Churn model v1.0  |  Based on days since last activity, purchases, test drives, and budget data");
        powered.setFont(FONT_SMALL);
        powered.setForeground(new Color(100, 70, 30));
        JButton closeBtn = glowButton("Close", CHURN_ACCENT, e -> dlg.dispose());
        bottomBar.add(powered, BorderLayout.WEST);
        bottomBar.add(closeBtn, BorderLayout.EAST);
        dlg.add(bottomBar, BorderLayout.SOUTH);

        // ── CHURN SCORING ENGINE ─────────────────────────────────────────────
        // Score each customer 0–100 where higher = MORE at risk of churning
        //
        // Factors:
        // Days since any activity (50 pts max) — core churn signal
        // No purchases ever (20 pts) — never converted
        // No test drives (10 pts) — no engagement
        // Budget mismatch vs inventory(10 pts) — may not find a car
        // Multiple cancelled interactions(10pt) — bad experience signals
        // ─────────────────────────────────────────────────────────────────────
        List<Sale> allSales = saleController.getAllSales();
        List<TestDrive> allDrives = testDriveController.getAllTestDrives();
        long nowMs = System.currentTimeMillis();

        // Build lookup maps: customerId -> last activity date, purchase count, drive
        // count
        Map<Integer, Long> lastActivity = new HashMap<>();
        Map<Integer, Integer> purchaseCount = new HashMap<>();
        Map<Integer, Integer> driveCount = new HashMap<>();
        Map<Integer, Integer> cancelCount = new HashMap<>();

        for (Sale s : allSales) {
            int cid = s.getCustomerId();
            purchaseCount.merge(cid, 1, Integer::sum);
            if (s.getSaleDate() != null) {
                long ts = s.getSaleDate().getTime();
                lastActivity.merge(cid, ts, Math::max);
            }
            if ("Cancelled".equalsIgnoreCase(s.getStatus()))
                cancelCount.merge(cid, 1, Integer::sum);
        }
        for (TestDrive td : allDrives) {
            int cid = td.getCustomerId();
            driveCount.merge(cid, 1, Integer::sum);
            if (td.getScheduledDate() != null)
                lastActivity.merge(cid, td.getScheduledDate().getTime(), Math::max);
            if ("Cancelled".equalsIgnoreCase(td.getStatus()))
                cancelCount.merge(cid, 1, Integer::sum);
        }
        // If customer has no activity recorded, use registration date as fallback
        // (not available here, so we default to 365 days ago for unknown)

        // Score each customer
        record ChurnResult(Customer customer, int score, int daysSinceActivity,
                String riskLevel, Color riskColor, String[] reasons, String[] actions) {
        }

        List<ChurnResult> results = new ArrayList<>();
        for (Customer c : customers) {
            int id = c.getId();
            int score = 0;
            List<String> reasons = new ArrayList<>();
            List<String> actions = new ArrayList<>();

            // Factor 1: Days since last activity (0–50 pts)
            int daysSince;
            if (lastActivity.containsKey(id)) {
                long diffMs = nowMs - lastActivity.get(id);
                daysSince = (int) (diffMs / (1000L * 60 * 60 * 24));
            } else {
                daysSince = 180; // assumed dormant if no record
            }
            int activityScore;
            if (daysSince >= 365) {
                activityScore = 50;
                reasons.add("No contact in " + daysSince + " days (over a year)");
            } else if (daysSince >= 180) {
                activityScore = 38;
                reasons.add("No contact in " + daysSince + " days (6+ months)");
            } else if (daysSince >= 90) {
                activityScore = 24;
                reasons.add("No contact in " + daysSince + " days (3+ months)");
            } else if (daysSince >= 45) {
                activityScore = 14;
                reasons.add("No contact in " + daysSince + " days (1.5+ months)");
            } else if (daysSince >= 14) {
                activityScore = 5;
                /* recent */ } else {
                activityScore = 0;
            }
            score += activityScore;

            // Factor 2: Never purchased (0–20 pts)
            int pc = purchaseCount.getOrDefault(id, 0);
            if (pc == 0) {
                score += 20;
                reasons.add("Has never made a purchase");
                actions.add("Send personalised vehicle offer matching their budget of $" + formatMoney(c.getBudget()));
            } else {
                if (pc == 1)
                    actions.add("One past purchase — offer a trade-in or second vehicle deal");
            }

            // Factor 3: No test drives (0–10 pts)
            int dc = driveCount.getOrDefault(id, 0);
            if (dc == 0) {
                score += 10;
                reasons.add("Never booked a test drive");
                actions.add("Invite to an exclusive test drive event");
            }

            // Factor 4: Budget mismatch — no cars available in their budget range
            double budget = c.getBudget();
            if (budget > 0) {
                List<Car> allCars = carController.getAllCars();
                long matchingCars = allCars.stream()
                        .filter(car -> car.isAvailable() && car.getPrice() <= budget * 1.10
                                && car.getPrice() >= budget * 0.60)
                        .count();
                if (matchingCars == 0) {
                    score += 10;
                    reasons.add("No current inventory matches their $" + formatMoney(budget) + " budget");
                    actions.add("Update inventory or negotiate financing options for their budget");
                }
            }

            // Factor 5: Cancellations (0–10 pts)
            int cc = cancelCount.getOrDefault(id, 0);
            if (cc >= 3) {
                score += 10;
                reasons.add(cc + " cancelled interactions — possible bad experience");
                actions.add("Assign senior sales rep for personal outreach call");
            } else if (cc >= 1) {
                score += 5;
                reasons.add(cc + " cancelled interaction(s)");
            }

            // Clamp 0–100
            score = Math.min(100, Math.max(0, score));

            // Classify
            String riskLevel;
            Color riskColor;
            if (score >= 75) {
                riskLevel = "CRITICAL";
                riskColor = CHURN_RED;
            } else if (score >= 50) {
                riskLevel = "HIGH";
                riskColor = new Color(255, 120, 40);
            } else if (score >= 25) {
                riskLevel = "MEDIUM";
                riskColor = CHURN_ORANGE;
            } else {
                riskLevel = "LOW";
                riskColor = CHURN_GREEN;
            }

            // Default action if none specific
            if (actions.isEmpty())
                actions.add("Continue regular touchpoints — customer appears healthy");

            results.add(new ChurnResult(c, score, daysSince, riskLevel, riskColor,
                    reasons.toArray(new String[0]), actions.toArray(new String[0])));
        }

        // Sort by score descending (highest risk first)
        results.sort((a, b) -> Integer.compare(b.score(), a.score()));

        // Aggregate stats for left panel
        long critical = results.stream().filter(r -> r.score() >= 75).count();
        long high = results.stream().filter(r -> r.score() >= 50 && r.score() < 75).count();
        long medium = results.stream().filter(r -> r.score() >= 25 && r.score() < 50).count();
        long low = results.stream().filter(r -> r.score() < 25).count();
        double avgScore = results.stream().mapToInt(ChurnResult::score).average().orElse(0);
        long neverBought = results.stream().filter(r -> purchaseCount.getOrDefault(r.customer().getId(), 0) == 0)
                .count();

        // ── Populate left column ─────────────────────────────────────────────
        Runnable buildLeftCol = () -> {
            leftCol.removeAll();
            leftCol.setBorder(new EmptyBorder(26, 22, 26, 22));

            JLabel lTitle = new JLabel("Portfolio Overview");
            lTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lTitle.setForeground(CHURN_ACCENT);
            lTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftCol.add(lTitle);
            leftCol.add(Box.createVerticalStrut(18));

            // Big score gauge
            JPanel gaugePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int W = getWidth(), H = getHeight();
                    int cx = W / 2, cy = H - 20, r = Math.min(W / 2 - 10, H - 30);
                    // Background arc
                    g2.setColor(new Color(30, 35, 55));
                    g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(cx - r, cy - r, 2 * r, 2 * r, 0, 180);
                    // Colored arc based on avgScore
                    int arcDeg = (int) (180 * avgScore / 100);
                    Color gc = avgScore >= 75 ? CHURN_RED
                            : avgScore >= 50 ? new Color(255, 130, 40) : avgScore >= 25 ? CHURN_ORANGE : CHURN_GREEN;
                    g2.setColor(gc);
                    g2.drawArc(cx - r, cy - r, 2 * r, 2 * r, 180, arcDeg);
                    g2.setStroke(new BasicStroke(1f));
                    // Labels
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                    g2.setColor(gc);
                    String avg = String.format("%.0f", avgScore);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(avg, cx - fm.stringWidth(avg) / 2, cy - 10);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g2.setColor(TEXT_MUTED);
                    String label = "avg risk score";
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(label, cx - fm2.stringWidth(label) / 2, cy + 10);
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(240, 140);
                }
            };
            gaugePanel.setOpaque(false);
            gaugePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            gaugePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
            leftCol.add(gaugePanel);
            leftCol.add(Box.createVerticalStrut(20));

            // Risk breakdown bars
            leftCol.add(churnSectionLabel("RISK BREAKDOWN"));
            leftCol.add(Box.createVerticalStrut(12));

            int total = customers.size();
            leftCol.add(riskCountBar("CRITICAL", (int) critical, total, CHURN_RED));
            leftCol.add(Box.createVerticalStrut(8));
            leftCol.add(riskCountBar("HIGH", (int) high, total, new Color(255, 120, 40)));
            leftCol.add(Box.createVerticalStrut(8));
            leftCol.add(riskCountBar("MEDIUM", (int) medium, total, CHURN_ORANGE));
            leftCol.add(Box.createVerticalStrut(8));
            leftCol.add(riskCountBar("LOW", (int) low, total, CHURN_GREEN));
            leftCol.add(Box.createVerticalStrut(22));

            leftCol.add(churnSectionLabel("KEY STATS"));
            leftCol.add(Box.createVerticalStrut(12));
            leftCol.add(churnStatRow("Total customers:", String.valueOf(total)));
            leftCol.add(Box.createVerticalStrut(6));
            leftCol.add(churnStatRow("Never purchased:",
                    neverBought + " (" + String.format("%.0f", neverBought * 100.0 / Math.max(total, 1)) + "%)"));
            leftCol.add(Box.createVerticalStrut(6));
            leftCol.add(churnStatRow("Need urgent action:", String.valueOf(critical + high)));
            leftCol.add(Box.createVerticalStrut(22));

            leftCol.add(churnSectionLabel("HOW CHURN IS SCORED"));
            leftCol.add(Box.createVerticalStrut(12));

            String[][] factors = {
                    { "Days since last contact", "50 pts max — the strongest signal" },
                    { "No purchase ever made", "20 pts — never converted" },
                    { "No test drive booked", "10 pts — low engagement" },
                    { "Budget vs inventory gap", "10 pts — nothing they can afford" },
                    { "Cancelled interactions", "10 pts — possible bad experience" },
            };
            for (String[] f : factors) {
                JPanel fr = new JPanel(new BorderLayout(6, 0));
                fr.setOpaque(false);
                fr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                fr.setAlignmentX(Component.LEFT_ALIGNMENT);
                fr.setBorder(new EmptyBorder(0, 0, 6, 0));
                JLabel fk = new JLabel(f[0]);
                fk.setFont(new Font("Segoe UI", Font.BOLD, 11));
                fk.setForeground(new Color(200, 155, 80));
                JLabel fv = new JLabel("<html><font color='#788098'>" + f[1] + "</font></html>");
                fv.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                fr.add(fk, BorderLayout.NORTH);
                fr.add(fv, BorderLayout.CENTER);
                leftCol.add(fr);
            }
            leftCol.revalidate();
            leftCol.repaint();
        };
        buildLeftCol.run();

        // ── Build customer cards ──────────────────────────────────────────────
        Runnable buildCards = () -> {
            cardsCol.removeAll();
            String filterVal = (String) filterBox.getSelectedItem();

            List<ChurnResult> filtered = results.stream().filter(r -> {
                if ("All Customers".equals(filterVal))
                    return true;
                if ("Critical Risk Only".equals(filterVal))
                    return r.score() >= 75;
                if ("High Risk".equals(filterVal))
                    return r.score() >= 50 && r.score() < 75;
                if ("Medium Risk".equals(filterVal))
                    return r.score() >= 25 && r.score() < 50;
                if ("Low Risk (Healthy)".equals(filterVal))
                    return r.score() < 25;
                return true;
            }).collect(java.util.stream.Collectors.toList());

            if (filtered.isEmpty()) {
                JPanel empty = new JPanel(new GridBagLayout());
                empty.setBackground(BG_DARK);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel el = new JLabel("No customers in this risk category.");
                el.setFont(FONT_LABEL);
                el.setForeground(TEXT_MUTED);
                empty.add(el);
                cardsCol.add(empty);
            }

            // Column header
            JPanel colHeader = new JPanel(new BorderLayout());
            colHeader.setOpaque(false);
            colHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            colHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            colHeader.setBorder(new EmptyBorder(0, 0, 12, 0));
            JLabel colTitle = new JLabel("Showing " + filtered.size() + " customer" + (filtered.size() == 1 ? "" : "s")
                    + " — sorted by risk score (highest first)");
            colTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            colTitle.setForeground(TEXT_MUTED);
            colHeader.add(colTitle, BorderLayout.WEST);
            cardsCol.add(colHeader);

            for (ChurnResult r : filtered) {
                Color rc = r.riskColor();
                int sc = r.score();

                JPanel card = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(BG_CARD);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                        g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 40));
                        g2.setStroke(new BasicStroke(1.2f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                        // Top accent bar
                        GradientPaint gp = new GradientPaint(0, 0,
                                new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 120), getWidth() / 2, 0,
                                new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 0));
                        g2.setPaint(gp);
                        g2.fillRect(14, 0, getWidth() - 28, 3);
                    }
                };
                card.setLayout(new BorderLayout(20, 0));
                card.setBorder(new EmptyBorder(20, 24, 20, 24));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setOpaque(false);

                // LEFT: customer info + score gauge
                JPanel leftSide = new JPanel(new GridLayout(5, 1, 0, 4));
                leftSide.setOpaque(false);
                leftSide.setPreferredSize(new Dimension(260, 160));

                // Risk badge
                JLabel badge = new JLabel("  " + r.riskLevel() + " RISK  ");
                badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                badge.setForeground(rc);
                badge.setBackground(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 20));
                badge.setOpaque(true);

                JLabel cName = new JLabel(r.customer().getName());
                cName.setFont(new Font("Segoe UI", Font.BOLD, 17));
                cName.setForeground(TEXT_PRIMARY);
                JLabel cId = new JLabel(
                        r.customer().getCustomerId() + "  |  Budget: $" + formatMoney(r.customer().getBudget())
                                + "  |  Prefers: " + r.customer().getPreferredCarType());
                cId.setFont(FONT_SMALL);
                cId.setForeground(TEXT_MUTED);
                JLabel cContact = new JLabel("Last seen: "
                        + (r.daysSinceActivity() >= 360 ? "Over 1 year ago" : r.daysSinceActivity() + " days ago") +
                        "   |   Purchases: " + purchaseCount.getOrDefault(r.customer().getId(), 0) +
                        "   |   Test Drives: " + driveCount.getOrDefault(r.customer().getId(), 0));
                cContact.setFont(FONT_SMALL);
                cContact.setForeground(new Color(160, 140, 100));

                leftSide.add(badge);
                leftSide.add(cName);
                leftSide.add(cId);
                leftSide.add(cContact);
                leftSide.add(new JLabel()); // spacer

                // CENTER: Score bar + Reasons
                JPanel centerSide = new JPanel();
                centerSide.setLayout(new BoxLayout(centerSide, BoxLayout.Y_AXIS));
                centerSide.setOpaque(false);

                JLabel scoreLabel = new JLabel("Churn Risk Score:  " + sc + " / 100");
                scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                scoreLabel.setForeground(rc);
                scoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Score bar
                JPanel scoreBar = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(30, 35, 55));
                        g2.fillRoundRect(0, 4, getWidth(), 14, 14, 14);
                        int filled = (int) (getWidth() * sc / 100.0);
                        GradientPaint gp = new GradientPaint(0, 0, CHURN_GREEN, getWidth() * 0.5f, 0, CHURN_ORANGE);
                        // Override with red if high
                        Color fc = sc >= 75 ? CHURN_RED
                                : sc >= 50 ? new Color(255, 130, 40) : sc >= 25 ? CHURN_ORANGE : CHURN_GREEN;
                        g2.setColor(fc);
                        g2.fillRoundRect(0, 4, filled, 14, 14, 14);
                    }

                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(100, 22);
                    }
                };
                scoreBar.setOpaque(false);
                scoreBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                scoreBar.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel reasonsTitle = new JLabel("Why this score:");
                reasonsTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
                reasonsTitle.setForeground(TEXT_MUTED);
                reasonsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

                centerSide.add(scoreLabel);
                centerSide.add(Box.createVerticalStrut(6));
                centerSide.add(scoreBar);
                centerSide.add(Box.createVerticalStrut(12));
                centerSide.add(reasonsTitle);
                centerSide.add(Box.createVerticalStrut(4));

                for (String reason : r.reasons()) {
                    JLabel rl = new JLabel("  •  " + reason);
                    rl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    rl.setForeground(new Color(200, 160, 100));
                    rl.setAlignmentX(Component.LEFT_ALIGNMENT);
                    centerSide.add(rl);
                    centerSide.add(Box.createVerticalStrut(3));
                }

                // RIGHT: Recommended actions
                JPanel rightSide = new JPanel();
                rightSide.setLayout(new BoxLayout(rightSide, BoxLayout.Y_AXIS));
                rightSide.setOpaque(false);
                rightSide.setPreferredSize(new Dimension(330, 0));
                rightSide.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0,
                                new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 30)),
                        new EmptyBorder(4, 20, 4, 8)));

                JLabel actTitle = new JLabel("Recommended Actions:");
                actTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
                actTitle.setForeground(new Color(100, 200, 150));
                actTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                rightSide.add(actTitle);
                rightSide.add(Box.createVerticalStrut(8));

                int actionNum = 1;
                for (String action : r.actions()) {
                    JPanel ap = new JPanel(new BorderLayout(8, 4));
                    ap.setOpaque(false);
                    ap.setAlignmentX(Component.LEFT_ALIGNMENT);
                    ap.setBorder(new EmptyBorder(2, 0, 10, 4));
                    JLabel num = new JLabel(actionNum + ".");
                    num.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    num.setForeground(CHURN_GREEN);
                    num.setVerticalAlignment(SwingConstants.TOP);
                    num.setPreferredSize(new Dimension(22, 20));
                    // JTextArea wraps naturally without needing an HTML width hint
                    JTextArea at = new JTextArea(action);
                    at.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    at.setForeground(TEXT_PRIMARY);
                    at.setBackground(new Color(0, 0, 0, 0));
                    at.setOpaque(false);
                    at.setEditable(false);
                    at.setLineWrap(true);
                    at.setWrapStyleWord(true);
                    at.setFocusable(false);
                    ap.add(num, BorderLayout.WEST);
                    ap.add(at, BorderLayout.CENTER);
                    rightSide.add(ap);
                    actionNum++;
                }

                card.add(leftSide, BorderLayout.WEST);
                card.add(centerSide, BorderLayout.CENTER);
                card.add(rightSide, BorderLayout.EAST);
                cardsCol.add(card);
                cardsCol.add(Box.createVerticalStrut(16));
            }
            cardsCol.revalidate();
            cardsCol.repaint();
        };

        filterBox.addActionListener(e -> buildCards.run());
        buildCards.run();
        dlg.setVisible(true);
    }

    // Churn panel helpers
    private JLabel churnSectionLabel(String text) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(new Color(160, 110, 50));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 50, 20)));
        return l;
    }

    private JPanel churnStatRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ll = new JLabel(label);
        ll.setFont(FONT_SMALL);
        ll.setForeground(TEXT_MUTED);
        JLabel vl = new JLabel(value);
        vl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        vl.setForeground(TEXT_PRIMARY);
        vl.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(ll, BorderLayout.WEST);
        row.add(vl, BorderLayout.EAST);
        return row;
    }

    private JPanel riskCountBar(String label, int count, int total, Color color) {
        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background track
                int trackY = getHeight() - 10;
                int trackH = 8;
                g2.setColor(new Color(30, 35, 55));
                g2.fillRoundRect(0, trackY, getWidth(), trackH, 8, 8);
                // Filled portion
                int filled = total > 0 ? (int) (getWidth() * (double) count / total) : 0;
                g2.setColor(color);
                g2.fillRoundRect(0, trackY, filled, trackH, 8, 8);
                // Labels
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(color);
                g2.drawString(label, 0, 14);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(TEXT_PRIMARY);
                String cnt = count + " customers";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(cnt, getWidth() - fm.stringWidth(cnt), 14);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        row.setPreferredSize(new Dimension(240, 35));
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AI FEATURE 4 — AI SALES COACH
    // After every cancelled sale, the AI diagnoses WHY it likely failed and
    // gives the employee concrete, specific improvement advice.
    // Also surfaces patterns across all employees to identify coaching gaps.
    // ═════════════════════════════════════════════════════════════════════════
    private void showSalesCoachDialog() {
        List<Sale> allSales = saleController.getAllSales();
        List<Employee> employees = employeeController.getAllEmployees();
        List<TestDrive> allDrives = testDriveController.getAllTestDrives();

        JDialog dlg = new JDialog(this, "AutoElite  —  AI Sales Coach", false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(screen.width, screen.height);
        dlg.setLocation(0, 0);
        dlg.setResizable(true);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.getContentPane().setBackground(BG_DARK);
        Color COACH_TEAL = new Color(40, 210, 160);
        Color COACH_BLUE = new Color(60, 160, 255);
        Color COACH_YELLOW = new Color(255, 220, 60);
        Color COACH_DARK = new Color(10, 14, 28);

        // ── TOP BAR ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 50, 40), getWidth(), 0, new Color(10, 15, 30));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(40, 180, 130, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 90));
        topBar.setBorder(new EmptyBorder(0, 36, 0, 36));

        JPanel titleBlock = new JPanel(new GridLayout(3, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel t1 = new JLabel("🧠  AI Sales Coach");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 24));
        t1.setForeground(COACH_TEAL);
        JLabel t2 = new JLabel("Why did this sale fall through — and what can we do differently next time?");
        t2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t2.setForeground(new Color(60, 190, 140));
        JLabel t3 = new JLabel(
                "Diagnoses every cancelled sale, identifies root causes (pricing, timing, test drive, follow-up), scores each employee, and gives specific coaching advice.");
        t3.setFont(FONT_SMALL);
        t3.setForeground(new Color(50, 140, 110));
        titleBlock.add(t1);
        titleBlock.add(t2);
        titleBlock.add(t3);

        // Tab selector: "Cancelled Sales" or "Employee Scorecards"
        JPanel tabWrap = new JPanel(new GridLayout(2, 1, 0, 6));
        tabWrap.setOpaque(false);
        tabWrap.setPreferredSize(new Dimension(300, 70));
        JLabel tabLbl = new JLabel("   View:");
        tabLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabLbl.setForeground(new Color(50, 160, 120));
        JComboBox<String> viewSelector = darkComboBox(
                new String[] { "Cancelled Sales Analysis", "Employee Scorecards" });
        viewSelector.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabWrap.add(tabLbl);
        tabWrap.add(viewSelector);

        topBar.add(titleBlock, BorderLayout.CENTER);
        topBar.add(tabWrap, BorderLayout.EAST);
        dlg.add(topBar, BorderLayout.NORTH);

        // ── BODY ─────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(BG_DARK);

        // LEFT: team stats sidebar
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setBackground(COACH_DARK);
        leftCol.setPreferredSize(new Dimension(290, 0));
        leftCol.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(20, 50, 45)));
        JScrollPane leftScroll = new JScrollPane(leftCol);
        leftScroll.setBorder(null);
        leftScroll.setBackground(COACH_DARK);
        leftScroll.getViewport().setBackground(COACH_DARK);
        styleScrollBar(leftScroll.getVerticalScrollBar());
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setPreferredSize(new Dimension(290, 0));

        // RIGHT: main content (swapped by view selector)
        JPanel mainContent = new JPanel(new BorderLayout(0, 0));
        mainContent.setBackground(BG_DARK);

        body.add(leftScroll, BorderLayout.WEST);
        body.add(mainContent, BorderLayout.CENTER);
        dlg.add(body, BorderLayout.CENTER);

        // ── BOTTOM BAR ───────────────────────────────────────────────────────
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(8, 10, 18));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 50, 45)),
                new EmptyBorder(12, 36, 12, 30)));
        JLabel powered = new JLabel(
                "Powered by AutoElite AI Engine  |  Sales Coach v1.0  |  Analyses every completed and cancelled sale in your database");
        powered.setFont(FONT_SMALL);
        powered.setForeground(new Color(30, 110, 85));
        JButton closeBtn = glowButton("Close", COACH_TEAL, e -> dlg.dispose());
        bottomBar.add(powered, BorderLayout.WEST);
        bottomBar.add(closeBtn, BorderLayout.EAST);
        dlg.add(bottomBar, BorderLayout.SOUTH);

        // ── DATA PROCESSING ───────────────────────────────────────────────────
        List<Sale> cancelled = allSales.stream().filter(s -> "Cancelled".equalsIgnoreCase(s.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        List<Sale> completed = allSales.stream().filter(s -> "Completed".equalsIgnoreCase(s.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        List<Sale> pending = allSales.stream().filter(s -> "Pending".equalsIgnoreCase(s.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Per-employee stats
        record EmpStats(Employee emp, int total, int comp, int canc, int pend,
                double convRate, double cancRate, double avgSaleVal, int coachScore) {
        }

        List<EmpStats> empStatsList = new ArrayList<>();
        for (Employee emp : employees) {
            int id = emp.getId();
            long tot = allSales.stream().filter(s -> s.getEmployeeId() == id).count();
            long comp2 = allSales.stream()
                    .filter(s -> s.getEmployeeId() == id && "Completed".equalsIgnoreCase(s.getStatus())).count();
            long canc2 = allSales.stream()
                    .filter(s -> s.getEmployeeId() == id && "Cancelled".equalsIgnoreCase(s.getStatus())).count();
            long pend2 = allSales.stream()
                    .filter(s -> s.getEmployeeId() == id && "Pending".equalsIgnoreCase(s.getStatus())).count();
            double conv = tot > 0 ? comp2 * 100.0 / tot : 0;
            double cancR = tot > 0 ? canc2 * 100.0 / tot : 0;
            double avgVal = allSales.stream()
                    .filter(s -> s.getEmployeeId() == id && "Completed".equalsIgnoreCase(s.getStatus()))
                    .mapToDouble(Sale::getSalePrice).average().orElse(0);
            // Coach score: higher is better
            // conversion rate (0–60) + low cancellation bonus (0–25) + activity bonus
            // (0–15)
            int cScore = (int) Math.min(60, conv * 0.60)
                    + (int) Math.max(0, 25 - cancR * 0.5)
                    + (int) Math.min(15, tot * 1.5);
            empStatsList.add(
                    new EmpStats(emp, (int) tot, (int) comp2, (int) canc2, (int) pend2, conv, cancR, avgVal, cScore));
        }
        empStatsList.sort((a, b) -> Integer.compare(b.coachScore(), a.coachScore()));

        // ── Populate left column ─────────────────────────────────────────────
        leftCol.removeAll();
        leftCol.setBorder(new EmptyBorder(26, 22, 26, 22));

        JLabel lTitle = new JLabel("Team Overview");
        lTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lTitle.setForeground(COACH_TEAL);
        lTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftCol.add(lTitle);
        leftCol.add(Box.createVerticalStrut(18));

        // Donut-style summary chart
        JPanel donut = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight(), cx = W / 2, cy = H / 2, r = Math.min(cx, cy) - 16, inner = r - 28;
                int total2 = allSales.size();
                if (total2 == 0) {
                    g2.setColor(new Color(30, 35, 55));
                    g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                    g2.setColor(BG_DARK);
                    g2.fillOval(cx - inner, cy - inner, 2 * inner, 2 * inner);
                    g2.setFont(FONT_SMALL);
                    g2.setColor(TEXT_MUTED);
                    g2.drawString("No data", cx - 25, cy + 5);
                    return;
                }
                int compDeg = (int) (360.0 * completed.size() / total2);
                int cancDeg = (int) (360.0 * cancelled.size() / total2);
                int pendDeg = 360 - compDeg - cancDeg;
                // Segments
                g2.setColor(ACCENT_GREEN);
                g2.fillArc(cx - r, cy - r, 2 * r, 2 * r, 90, -compDeg);
                g2.setColor(ACCENT_PINK);
                g2.fillArc(cx - r, cy - r, 2 * r, 2 * r, 90 - compDeg, -cancDeg);
                g2.setColor(ACCENT_ORANGE);
                g2.fillArc(cx - r, cy - r, 2 * r, 2 * r, 90 - compDeg - cancDeg, -pendDeg);
                // Inner hole
                g2.setColor(COACH_DARK);
                g2.fillOval(cx - inner, cy - inner, 2 * inner, 2 * inner);
                // Center text
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(ACCENT_GREEN);
                String pct = String.format("%.0f%%", completed.size() * 100.0 / total2);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(pct, cx - fm.stringWidth(pct) / 2, cy + 6);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(TEXT_MUTED);
                g2.drawString("win rate", cx - 22, cy + 20);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 200);
            }
        };
        donut.setOpaque(false);
        donut.setAlignmentX(Component.CENTER_ALIGNMENT);
        donut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        leftCol.add(donut);
        leftCol.add(Box.createVerticalStrut(8));

        // Legend
        Color[] legClrs = { ACCENT_GREEN, ACCENT_PINK, ACCENT_ORANGE };
        String[] legLbls = { "Completed: " + completed.size(), "Cancelled: " + cancelled.size(),
                "Pending: " + pending.size() };
        for (int i = 0; i < 3; i++) {
            JPanel leg = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            leg.setOpaque(false);
            leg.setAlignmentX(Component.LEFT_ALIGNMENT);
            leg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            dot.setForeground(legClrs[i]);
            JLabel lbl2 = new JLabel(legLbls[i]);
            lbl2.setFont(FONT_SMALL);
            lbl2.setForeground(TEXT_MUTED);
            leg.add(dot);
            leg.add(lbl2);
            leftCol.add(leg);
        }
        leftCol.add(Box.createVerticalStrut(22));

        coachSectionLabel(leftCol, "TOP PERFORMERS");
        leftCol.add(Box.createVerticalStrut(12));
        for (int i = 0; i < Math.min(5, empStatsList.size()); i++) {
            EmpStats es = empStatsList.get(i);
            Color rankClr = i == 0 ? new Color(255, 200, 40)
                    : i == 1 ? new Color(192, 192, 192) : i == 2 ? new Color(205, 127, 50) : TEXT_MUTED;
            JPanel ep = new JPanel(new BorderLayout(8, 0));
            ep.setOpaque(false);
            ep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            ep.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel eName = new JLabel((i + 1) + ".  " + es.emp().getName());
            eName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            eName.setForeground(rankClr);
            JLabel eStat = new JLabel(String.format("%.0f%%", es.convRate()) + " conv");
            eStat.setFont(FONT_SMALL);
            eStat.setForeground(TEXT_MUTED);
            eStat.setHorizontalAlignment(SwingConstants.RIGHT);
            ep.add(eName, BorderLayout.WEST);
            ep.add(eStat, BorderLayout.EAST);
            leftCol.add(ep);
            leftCol.add(Box.createVerticalStrut(6));
        }
        leftCol.add(Box.createVerticalStrut(20));

        coachSectionLabel(leftCol, "COMMON FAILURE REASONS");
        leftCol.add(Box.createVerticalStrut(12));
        String[][] patterns = {
                { "No prior test drive", String.valueOf(cancelled.stream()
                        .filter(s -> allDrives.stream().noneMatch(td -> td.getCustomerId() == s.getCustomerId()))
                        .count()) },
                { "High-value deals lost",
                        String.valueOf(cancelled.stream().filter(s -> s.getSalePrice() > 40000).count()) },
                { "Single-interaction loss",
                        String.valueOf(cancelled.stream()
                                .filter(s -> completed.stream().noneMatch(c -> c.getCustomerId() == s.getCustomerId()))
                                .count()) },
        };
        for (String[] pat : patterns) {
            JPanel pr2 = new JPanel(new BorderLayout(8, 0));
            pr2.setOpaque(false);
            pr2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            pr2.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel pk = new JLabel(pat[0]);
            pk.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            pk.setForeground(TEXT_MUTED);
            JLabel pv = new JLabel(pat[1] + " cases");
            pv.setFont(new Font("Segoe UI", Font.BOLD, 11));
            pv.setForeground(ACCENT_ORANGE);
            pv.setHorizontalAlignment(SwingConstants.RIGHT);
            pr2.add(pk, BorderLayout.WEST);
            pr2.add(pv, BorderLayout.EAST);
            leftCol.add(pr2);
            leftCol.add(Box.createVerticalStrut(6));
        }
        leftCol.revalidate();
        leftCol.repaint();

        // ── MAIN CONTENT BUILDER ──────────────────────────────────────────────
        Runnable buildMain = () -> {
            mainContent.removeAll();
            String view = (String) viewSelector.getSelectedItem();

            if ("Cancelled Sales Analysis".equals(view)) {
                // ── CANCELLED SALES VIEW ─────────────────────────────────────
                JPanel cardsPanel = new JPanel();
                cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
                cardsPanel.setBackground(BG_DARK);
                cardsPanel.setBorder(new EmptyBorder(22, 30, 22, 30));
                JScrollPane scroll = new JScrollPane(cardsPanel);
                scroll.setBorder(null);
                scroll.setBackground(BG_DARK);
                scroll.getViewport().setBackground(BG_DARK);
                styleScrollBar(scroll.getVerticalScrollBar());
                mainContent.add(scroll, BorderLayout.CENTER);

                if (cancelled.isEmpty()) {
                    JPanel ep = new JPanel(new GridBagLayout());
                    ep.setBackground(BG_DARK);
                    ep.setAlignmentX(Component.LEFT_ALIGNMENT);
                    JLabel el = new JLabel(
                            "<html><center>🎉<br><br>No cancelled sales found!<br><font color='#506080'>All recorded sales are either completed or pending.</font></center></html>");
                    el.setFont(FONT_LABEL);
                    el.setHorizontalAlignment(SwingConstants.CENTER);
                    el.setForeground(COACH_TEAL);
                    ep.add(el);
                    cardsPanel.add(ep);
                } else {
                    JLabel hdr = new JLabel(cancelled.size() + " cancelled sale" + (cancelled.size() == 1 ? "" : "s")
                            + " — AI diagnosis for each");
                    hdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    hdr.setForeground(TEXT_MUTED);
                    hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
                    cardsPanel.add(hdr);
                    cardsPanel.add(Box.createVerticalStrut(18));

                    for (Sale s : cancelled) {
                        // ── Diagnose each cancelled sale ──────────────────────
                        List<String> diagnoses = new ArrayList<>();
                        List<String> coaching = new ArrayList<>();
                        String severity;
                        Color sevColor;

                        // Diagnosis 1: No test drive before sale attempt
                        boolean hadTestDrive = allDrives.stream().anyMatch(
                                td -> td.getCustomerId() == s.getCustomerId() && td.getCarId() == s.getCarId());
                        if (!hadTestDrive) {
                            diagnoses.add("Customer was never offered a test drive for this vehicle");
                            coaching.add(
                                    "Always secure a test drive before quoting — buyers who drive are 3x more likely to close.");
                        }

                        // Diagnosis 2: Price might be over customer's typical budget
                        List<Customer> matchCust = customerController.getAllCustomers().stream()
                                .filter(c -> c.getId() == s.getCustomerId())
                                .collect(java.util.stream.Collectors.toList());
                        if (!matchCust.isEmpty()) {
                            double budget = matchCust.get(0).getBudget();
                            if (budget > 0 && s.getSalePrice() > budget * 1.05) {
                                diagnoses.add(String.format(
                                        "Sale price ($%s) exceeded customer's stated budget ($%s) by %.0f%%",
                                        formatMoney(s.getSalePrice()), formatMoney(budget),
                                        (s.getSalePrice() / budget - 1) * 100));
                                coaching.add(
                                        "Offer financing or a cheaper equivalent before losing the deal entirely.");
                            }
                        }

                        // Diagnosis 3: Customer has other completed sales (relationship exists but this
                        // deal failed)
                        boolean hasOtherSales = completed.stream()
                                .anyMatch(c -> c.getCustomerId() == s.getCustomerId());
                        if (hasOtherSales) {
                            diagnoses.add(
                                    "Returning customer — this cancellation is a regression from a previously successful relationship");
                            coaching.add(
                                    "Reach out personally. A returning customer who cancels usually has a specific grievance — find it.");
                        }

                        // Diagnosis 4: High-value deal (>$50k) — different approach needed
                        if (s.getSalePrice() > 50000) {
                            diagnoses.add("Premium deal ($" + formatMoney(s.getSalePrice())
                                    + ") — these require extended nurturing and executive involvement");
                            coaching.add(
                                    "Escalate high-value deals to senior sales staff and schedule a second consultation with management.");
                        }

                        // Diagnosis 5: Payment method flag
                        if ("Bank Finance".equalsIgnoreCase(s.getPaymentMethod())
                                || "Lease".equalsIgnoreCase(s.getPaymentMethod())) {
                            diagnoses.add("Finance/lease deal — approval delays often kill momentum");
                            coaching.add(
                                    "Pre-qualify the customer's finance eligibility before presenting the car, not after.");
                        }

                        if (diagnoses.isEmpty()) {
                            diagnoses.add(
                                    "No specific pattern identified — may be personal circumstances outside sales control");
                            coaching.add(
                                    "Follow up in 30 days with a 'We found something you might love' message to reopen the door.");
                        }

                        // Severity
                        if (diagnoses.size() >= 3) {
                            severity = "MULTIPLE ISSUES";
                            sevColor = ACCENT_PINK;
                        } else if (diagnoses.size() == 2) {
                            severity = "2 FACTORS";
                            sevColor = ACCENT_ORANGE;
                        } else {
                            severity = "SINGLE CAUSE";
                            sevColor = COACH_YELLOW;
                        }

                        // Build card
                        Color sCol = sevColor;
                        JPanel card = new JPanel() {
                            @Override
                            protected void paintComponent(Graphics g) {
                                Graphics2D g2 = (Graphics2D) g;
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2.setColor(BG_CARD);
                                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                                g2.setColor(new Color(sCol.getRed(), sCol.getGreen(), sCol.getBlue(), 35));
                                g2.setStroke(new BasicStroke(1.2f));
                                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                                GradientPaint gp2 = new GradientPaint(0, 0,
                                        new Color(sCol.getRed(), sCol.getGreen(), sCol.getBlue(), 80), getWidth() / 3,
                                        0, new Color(sCol.getRed(), sCol.getGreen(), sCol.getBlue(), 0));
                                g2.setPaint(gp2);
                                g2.fillRect(14, 0, getWidth() - 28, 3);
                            }
                        };
                        card.setLayout(new BorderLayout(24, 0));
                        card.setBorder(new EmptyBorder(22, 26, 22, 26));
                        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
                        card.setAlignmentX(Component.LEFT_ALIGNMENT);
                        card.setOpaque(false);

                        // LEFT: sale summary
                        JPanel ls = new JPanel(new GridLayout(5, 1, 0, 5));
                        ls.setOpaque(false);
                        ls.setPreferredSize(new Dimension(240, 180));
                        JLabel sevBadge = new JLabel("  " + severity + "  ");
                        sevBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        sevBadge.setForeground(sevColor);
                        sevBadge.setBackground(
                                new Color(sevColor.getRed(), sevColor.getGreen(), sevColor.getBlue(), 20));
                        sevBadge.setOpaque(true);
                        JLabel saleLbl = new JLabel((s.getCarName() != null ? s.getCarName() : "Car #" + s.getCarId()));
                        saleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        saleLbl.setForeground(TEXT_PRIMARY);
                        JLabel custLbl = new JLabel("Customer: "
                                + (s.getCustomerName() != null ? s.getCustomerName() : "ID #" + s.getCustomerId()));
                        custLbl.setFont(FONT_SMALL);
                        custLbl.setForeground(TEXT_MUTED);
                        JLabel empLbl = new JLabel("Employee: "
                                + (s.getEmployeeName() != null ? s.getEmployeeName() : "ID #" + s.getEmployeeId()));
                        empLbl.setFont(FONT_SMALL);
                        empLbl.setForeground(TEXT_MUTED);
                        JLabel priceLbl = new JLabel("Lost deal value:  $" + formatMoney(s.getSalePrice()));
                        priceLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        priceLbl.setForeground(ACCENT_PINK);
                        ls.add(sevBadge);
                        ls.add(saleLbl);
                        ls.add(custLbl);
                        ls.add(empLbl);
                        ls.add(priceLbl);

                        // CENTER: diagnoses
                        JPanel cs2 = new JPanel();
                        cs2.setLayout(new BoxLayout(cs2, BoxLayout.Y_AXIS));
                        cs2.setOpaque(false);
                        JLabel dTitle = new JLabel("🔍  Root Cause Analysis:");
                        dTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        dTitle.setForeground(new Color(200, 180, 80));
                        dTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                        cs2.add(dTitle);
                        cs2.add(Box.createVerticalStrut(8));
                        for (String d : diagnoses) {
                            JLabel dl = new JLabel("<html><body style='width:280px'>⚠  " + d + "</body></html>");
                            dl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            dl.setForeground(new Color(220, 180, 100));
                            dl.setAlignmentX(Component.LEFT_ALIGNMENT);
                            cs2.add(dl);
                            cs2.add(Box.createVerticalStrut(5));
                        }

                        // RIGHT: coaching advice
                        JPanel rs2 = new JPanel();
                        rs2.setLayout(new BoxLayout(rs2, BoxLayout.Y_AXIS));
                        rs2.setOpaque(false);
                        rs2.setPreferredSize(new Dimension(300, 180));
                        rs2.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(40, 180, 120, 30)),
                                new EmptyBorder(0, 20, 0, 0)));
                        JLabel cTitle = new JLabel("✅  What to do next time:");
                        cTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        cTitle.setForeground(COACH_TEAL);
                        cTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                        rs2.add(cTitle);
                        rs2.add(Box.createVerticalStrut(8));
                        int cn = 1;
                        for (String coa : coaching) {
                            JLabel cl = new JLabel("<html><body style='width:270px'><b style='color:#28D49C'>" + cn
                                    + ".</b>  " + coa + "</body></html>");
                            cl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            cl.setForeground(TEXT_PRIMARY);
                            cl.setAlignmentX(Component.LEFT_ALIGNMENT);
                            rs2.add(cl);
                            rs2.add(Box.createVerticalStrut(6));
                            cn++;
                        }

                        card.add(ls, BorderLayout.WEST);
                        card.add(cs2, BorderLayout.CENTER);
                        card.add(rs2, BorderLayout.EAST);
                        cardsPanel.add(card);
                        cardsPanel.add(Box.createVerticalStrut(16));
                    }
                }

            } else {
                // ── EMPLOYEE SCORECARDS VIEW ─────────────────────────────────
                JPanel scPanel = new JPanel();
                scPanel.setLayout(new BoxLayout(scPanel, BoxLayout.Y_AXIS));
                scPanel.setBackground(BG_DARK);
                scPanel.setBorder(new EmptyBorder(22, 30, 22, 30));
                JScrollPane scroll2 = new JScrollPane(scPanel);
                scroll2.setBorder(null);
                scroll2.setBackground(BG_DARK);
                scroll2.getViewport().setBackground(BG_DARK);
                styleScrollBar(scroll2.getVerticalScrollBar());
                mainContent.add(scroll2, BorderLayout.CENTER);

                JLabel hdr2 = new JLabel("Individual employee scorecards — ranked by coaching score");
                hdr2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                hdr2.setForeground(TEXT_MUTED);
                hdr2.setAlignmentX(Component.LEFT_ALIGNMENT);
                scPanel.add(hdr2);
                scPanel.add(Box.createVerticalStrut(18));

                if (empStatsList.isEmpty()) {
                    JLabel el = new JLabel("No employees found. Add employees first.");
                    el.setFont(FONT_LABEL);
                    el.setForeground(TEXT_MUTED);
                    el.setAlignmentX(Component.LEFT_ALIGNMENT);
                    scPanel.add(el);
                }

                Color[] rankColors2 = { new Color(255, 200, 40), new Color(192, 192, 192), new Color(205, 127, 50) };
                for (int i = 0; i < empStatsList.size(); i++) {
                    EmpStats es = empStatsList.get(i);
                    Color rankClr2 = i < 3 ? rankColors2[i] : TEXT_MUTED;
                    int rank = i;

                    // Coaching tips per employee
                    List<String> tips = new ArrayList<>();
                    if (es.cancRate() > 40)
                        tips.add("High cancellation rate (" + String.format("%.0f", es.cancRate())
                                + "%) — focus on qualification: ensure customer is serious before investing hours in negotiation.");
                    if (es.convRate() < 30 && es.total() > 3)
                        tips.add("Below-average conversion (" + String.format("%.0f", es.convRate())
                                + "%) — review the follow-up process. Most deals are won on the 3rd or 4th contact.");
                    if (es.total() == 0)
                        tips.add(
                                "No sales activity recorded yet — assign this employee to shadow a top performer for one week.");
                    if (es.canc() > es.comp() && es.total() > 2)
                        tips.add(
                                "More cancellations than completions — book a one-on-one session to review objection handling techniques.");
                    if (es.avgSaleVal() > 0 && es.avgSaleVal() < 25000)
                        tips.add(
                                "Average deal value is below market — encourage upselling warranty, accessories, and service packages.");
                    if (es.comp() >= 5 && es.convRate() > 60)
                        tips.add(
                                "Strong performer — consider as a mentor for newer staff. Their close rate is above team average.");
                    if (tips.isEmpty())
                        tips.add(
                                "Solid overall performance. Continue current approach and aim to increase deal volume.");

                    Color sc2 = es.coachScore() >= 70 ? COACH_TEAL : es.coachScore() >= 40 ? COACH_YELLOW : ACCENT_PINK;

                    JPanel card = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(BG_CARD);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                            g2.setColor(new Color(rankClr2.getRed(), rankClr2.getGreen(), rankClr2.getBlue(), 30));
                            g2.setStroke(new BasicStroke(1.2f));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                            GradientPaint gp3 = new GradientPaint(0, 0,
                                    new Color(rankClr2.getRed(), rankClr2.getGreen(), rankClr2.getBlue(), 80),
                                    getWidth() / 3, 0, new Color(0, 0, 0, 0));
                            g2.setPaint(gp3);
                            g2.fillRect(14, 0, getWidth() - 28, 3);
                        }
                    };
                    card.setLayout(new BorderLayout(22, 0));
                    card.setBorder(new EmptyBorder(22, 26, 22, 26));
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    card.setOpaque(false);

                    // LEFT: employee info + rank
                    JPanel ls2 = new JPanel(new GridLayout(5, 1, 0, 5));
                    ls2.setOpaque(false);
                    ls2.setPreferredSize(new Dimension(220, 170));
                    JLabel rankBadge = new JLabel("  RANK #" + (rank + 1) + "  ");
                    rankBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    rankBadge.setForeground(rankClr2);
                    rankBadge.setBackground(new Color(rankClr2.getRed(), rankClr2.getGreen(), rankClr2.getBlue(), 20));
                    rankBadge.setOpaque(true);
                    JLabel eName2 = new JLabel(es.emp().getName());
                    eName2.setFont(new Font("Segoe UI", Font.BOLD, 17));
                    eName2.setForeground(TEXT_PRIMARY);
                    JLabel eRole = new JLabel(es.emp().getDesignation() + "  |  " + es.emp().getDepartment());
                    eRole.setFont(FONT_SMALL);
                    eRole.setForeground(TEXT_MUTED);
                    JLabel eStats = new JLabel(
                            es.total() + " deals  ·  " + es.comp() + " closed  ·  " + es.canc() + " cancelled");
                    eStats.setFont(FONT_SMALL);
                    eStats.setForeground(new Color(100, 150, 130));
                    // Coach score bar
                    int cs3 = es.coachScore();
                    Color barColor = sc2;
                    JPanel csBar = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(25, 32, 52));
                            g2.fillRoundRect(0, 5, getWidth(), 10, 10, 10);
                            int f2 = (int) (getWidth() * Math.min(cs3, 100) / 100.0);
                            GradientPaint gp4 = new GradientPaint(0, 0, barColor, f2, 0,
                                    new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 80));
                            g2.setPaint(gp4);
                            g2.fillRoundRect(0, 5, f2, 10, 10, 10);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                            g2.setColor(barColor);
                            g2.drawString("Coach Score: " + cs3 + "/100", 0, 3);
                        }

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(200, 22);
                        }
                    };
                    csBar.setOpaque(false);
                    ls2.add(rankBadge);
                    ls2.add(eName2);
                    ls2.add(eRole);
                    ls2.add(eStats);
                    ls2.add(csBar);

                    // CENTER: stats grid
                    JPanel cs4 = new JPanel(new GridLayout(3, 2, 16, 10));
                    cs4.setOpaque(false);
                    String[][] statPairs = {
                            { "Conversion Rate", String.format("%.1f%%", es.convRate()) },
                            { "Cancellation Rate", String.format("%.1f%%", es.cancRate()) },
                            { "Avg Deal Value", es.avgSaleVal() > 0 ? "$" + formatMoney(es.avgSaleVal()) : "N/A" },
                            { "Completed Sales", String.valueOf(es.comp()) },
                            { "Pending Deals", String.valueOf(es.pend()) },
                            { "Total Activity", String.valueOf(es.total()) },
                    };
                    for (String[] sp : statPairs) {
                        JPanel sp2 = new JPanel(new GridLayout(2, 1, 0, 2));
                        sp2.setOpaque(false);
                        sp2.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(30, 40, 65)),
                                new EmptyBorder(6, 10, 6, 10)));
                        JLabel sk = new JLabel(sp[0]);
                        sk.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        sk.setForeground(TEXT_MUTED);
                        JLabel sv = new JLabel(sp[1]);
                        sv.setFont(new Font("Segoe UI", Font.BOLD, 15));
                        sv.setForeground(COACH_TEAL);
                        sp2.add(sk);
                        sp2.add(sv);
                        cs4.add(sp2);
                    }

                    // RIGHT: coaching tips
                    JPanel rs3 = new JPanel();
                    rs3.setLayout(new BoxLayout(rs3, BoxLayout.Y_AXIS));
                    rs3.setOpaque(false);
                    rs3.setPreferredSize(new Dimension(320, 170));
                    rs3.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(40, 180, 120, 30)),
                            new EmptyBorder(0, 20, 0, 0)));
                    JLabel ctTitle = new JLabel("🎯  Coaching Advice:");
                    ctTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    ctTitle.setForeground(COACH_TEAL);
                    ctTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                    rs3.add(ctTitle);
                    rs3.add(Box.createVerticalStrut(8));
                    int tn = 1;
                    for (String tip : tips) {
                        JLabel tl2 = new JLabel("<html><body style='width:290px'><b style='color:#28D49C'>" + tn
                                + ".</b>  " + tip + "</body></html>");
                        tl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        tl2.setForeground(TEXT_PRIMARY);
                        tl2.setAlignmentX(Component.LEFT_ALIGNMENT);
                        rs3.add(tl2);
                        rs3.add(Box.createVerticalStrut(6));
                        tn++;
                    }

                    card.add(ls2, BorderLayout.WEST);
                    card.add(cs4, BorderLayout.CENTER);
                    card.add(rs3, BorderLayout.EAST);
                    scPanel.add(card);
                    scPanel.add(Box.createVerticalStrut(16));
                }
            }

            mainContent.revalidate();
            mainContent.repaint();
        };

        viewSelector.addActionListener(e -> buildMain.run());
        buildMain.run();
        dlg.setVisible(true);
    }

    private void coachSectionLabel(JPanel panel, String text) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(new Color(50, 160, 120));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(20, 65, 55)));
        panel.add(l);
    }

    // =========================================================================
    // FEATURE: CAR PHOTO GALLERY
    // Opens when salesman clicks "Photos" on a selected car.
    // Shows the stored photo full-size with car details overlay.
    // =========================================================================

    // =========================================================================
    // MULTI-PHOTO PANEL BUILDER — used in both Add and Edit car dialogs
    // Displays a horizontal strip of thumbnail slots. Click any slot to pick
    // a photo. Each slot shows the thumbnail + a remove (×) button.
    // Paths are stored pipe-separated in car.getImagePath().
    // =========================================================================
    private JPanel buildMultiPhotoPanel(JDialog parent, java.util.List<String> paths) {
        int MAX_PHOTOS = 5;
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        // Thumbnail strip
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        strip.setOpaque(false);
        JLabel countLbl = new JLabel("0 / " + MAX_PHOTOS + " photos");
        countLbl.setFont(FONT_SMALL);
        countLbl.setForeground(TEXT_MUTED);

        Runnable refreshStrip = new Runnable() {
            public void run() {
                strip.removeAll();
                countLbl.setText(paths.size() + " / " + MAX_PHOTOS + " photos");
                for (int i = 0; i < paths.size(); i++) {
                    final int idx = i;
                    String path = paths.get(i);
                    JPanel slot = new JPanel(new BorderLayout(0, 2)) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(14, 20, 40));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                            g2.setColor(new Color(40, 60, 100));
                            g2.setStroke(new BasicStroke(1f));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                        }
                    };
                    slot.setOpaque(false);
                    slot.setPreferredSize(new Dimension(110, 90));
                    // Thumbnail
                    JLabel thumb = new JLabel("", SwingConstants.CENTER);
                    thumb.setFont(FONT_SMALL);
                    thumb.setForeground(TEXT_MUTED);
                    if (path.startsWith("http")) {
                        thumb.setText("<html><center>🌐<br><font size='1'>"
                                + path.substring(path.lastIndexOf('/') + 1).substring(0,
                                        Math.min(12, path.length() - path.lastIndexOf('/') - 1))
                                + "</font></center></html>");
                    } else {
                        try {
                            java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(new java.io.File(path));
                            if (bi != null)
                                thumb.setIcon(new ImageIcon(bi.getScaledInstance(106, 70, Image.SCALE_SMOOTH)));
                            else
                                thumb.setText("?");
                        } catch (Exception ex) {
                            thumb.setText("?");
                        }
                    }
                    // Remove button
                    JButton rem = new JButton("×");
                    rem.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    rem.setForeground(new Color(255, 80, 80));
                    rem.setBackground(new Color(30, 20, 40));
                    rem.setOpaque(true);
                    rem.setBorderPainted(false);
                    rem.setFocusPainted(false);
                    rem.setPreferredSize(new Dimension(110, 18));
                    rem.addActionListener(e -> {
                        paths.remove(idx);
                        this.run();
                    });
                    slot.add(thumb, BorderLayout.CENTER);
                    slot.add(rem, BorderLayout.SOUTH);
                    strip.add(slot);
                }
                // Add button (if under limit)
                if (paths.size() < MAX_PHOTOS) {
                    JButton addSlot = new JButton(
                            "<html><center>+<br><font size='1'>Add photo</font></center></html>") {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(14, 20, 40));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                            g2.setColor(new Color(40, 100, 200, 100));
                            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                                    new float[] { 5, 4 }, 0));
                            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                            super.paintComponent(g);
                        }
                    };
                    addSlot.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    addSlot.setForeground(new Color(80, 160, 255));
                    addSlot.setOpaque(false);
                    addSlot.setBorderPainted(false);
                    addSlot.setFocusPainted(false);
                    addSlot.setPreferredSize(new Dimension(110, 90));
                    addSlot.addActionListener(e -> {
                        JFileChooser fc = new JFileChooser();
                        fc.setDialogTitle("Add Vehicle Photo");
                        fc.setMultiSelectionEnabled(true);
                        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                                "Images (JPG, PNG, WEBP, GIF)", "jpg", "jpeg", "png", "webp", "gif"));
                        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                            for (java.io.File f2 : fc.getSelectedFiles()) {
                                if (paths.size() < MAX_PHOTOS)
                                    paths.add(f2.getAbsolutePath());
                            }
                            this.run();
                        }
                    });
                    strip.add(addSlot);
                }
                strip.revalidate();
                strip.repaint();
            }
        };
        refreshStrip.run();

        wrapper.add(strip, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(countLbl);
        JLabel hint = new JLabel(
                "  Select up to 5 photos. First photo is the main display image. You can also drag & drop images here.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        hint.setForeground(new Color(80, 100, 150));
        bottom.add(hint);
        wrapper.add(bottom, BorderLayout.SOUTH);
        wrapper.setPreferredSize(new Dimension(0, 115));
        // Enable drag-and-drop onto the strip
        return wrapper;
    }

    // =========================================================================
    // CAR PHOTO GALLERY — shows all uploaded photos with navigation arrows
    // Falls back to the built-in car image library for demo DB cars.
    // =========================================================================
    private void showCarGalleryDialog(Car car) {
        // Collect uploaded photos (pipe-separated paths)
        java.util.List<String> photos = new java.util.ArrayList<>();
        if (car.getImagePath() != null && !car.getImagePath().isBlank()) {
            for (String p : car.getImagePath().split("\\|")) {
                if (!p.isBlank())
                    photos.add(p.trim());
            }
        }
        if (photos.isEmpty())
            photos.add("__no_photo__");

        JDialog dlg = new JDialog(this, "Vehicle Gallery  —  " + car.getBrand() + " " + car.getModel(), true);
        dlg.setSize(960, 680);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(true);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.getContentPane().setBackground(BG_DARK);

        // Header
        JPanel header = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 60, 130), getWidth(), 0, new Color(10, 14, 28));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 120, 220, 50));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 28, 0, 24));
        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel ht = new JLabel("📷  " + car.getBrand() + " " + car.getModel() + " (" + car.getYear() + ")");
        ht.setFont(new Font("Segoe UI", Font.BOLD, 20));
        ht.setForeground(new Color(60, 180, 255));
        JLabel hs = new JLabel(car.getCategory() + "  ·  " + car.getColor() + "  ·  $" + formatMoney(car.getPrice())
                + "  |  " + photos.size() + " photo" + (photos.size() == 1 ? "" : "s"));
        hs.setFont(FONT_SMALL);
        hs.setForeground(TEXT_MUTED);
        titleBlock.add(ht);
        titleBlock.add(hs);
        header.add(titleBlock, BorderLayout.CENTER);
        header.add(glowButton("Close", new Color(60, 160, 255), e -> dlg.dispose()), BorderLayout.EAST);
        dlg.add(header, BorderLayout.NORTH);

        // Main photo viewer
        final int[] current = { 0 };
        JPanel photoArea = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(6, 8, 18));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        photoArea.setOpaque(false);

        JLabel photoLabel = new JLabel("", SwingConstants.CENTER);
        photoLabel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel photoCounter = new JLabel("1 / " + photos.size(), SwingConstants.CENTER);
        photoCounter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        photoCounter.setForeground(TEXT_MUTED);
        photoCounter.setBorder(new EmptyBorder(6, 0, 6, 0));

        JButton prevBtn = new JButton("‹");
        prevBtn.setFont(new Font("Segoe UI", Font.BOLD, 32));
        prevBtn.setForeground(new Color(150, 180, 255));
        prevBtn.setOpaque(false);
        prevBtn.setBorderPainted(false);
        prevBtn.setFocusPainted(false);
        prevBtn.setPreferredSize(new Dimension(60, 0));

        JButton nextBtn = new JButton("›");
        nextBtn.setFont(new Font("Segoe UI", Font.BOLD, 32));
        nextBtn.setForeground(new Color(150, 180, 255));
        nextBtn.setOpaque(false);
        nextBtn.setBorderPainted(false);
        nextBtn.setFocusPainted(false);
        nextBtn.setPreferredSize(new Dimension(60, 0));

        Runnable loadPhoto = () -> {
            String src = photos.get(current[0]);
            photoCounter.setText((current[0] + 1) + " / " + photos.size());
            prevBtn.setEnabled(current[0] > 0);
            nextBtn.setEnabled(current[0] < photos.size() - 1);
            if ("__no_photo__".equals(src)) {
                photoLabel.setIcon(null);
                photoLabel.setText("<html><center><font size='5' color='#304060'>📷</font><br><br>" +
                        "<font color='#405070'>No photo uploaded for this vehicle.</font><br>" +
                        "<font color='#2a3a50' size='2'>Select the car → Edit → Add photos</font></center></html>");
                return;
            }
            photoLabel.setText("Loading...");
            photoLabel.setForeground(TEXT_MUTED);
            photoLabel.setIcon(null);
            // Load in background thread to not freeze UI
            new Thread(() -> {
                try {
                    java.awt.image.BufferedImage img;
                    if (src.startsWith("http")) {
                        // Network disabled — skip URL images
                        SwingUtilities.invokeLater(() -> photoLabel.setText(
                                "URL images not supported in this environment. Please upload a local photo via Edit."));
                        return;
                    } else {
                        img = javax.imageio.ImageIO.read(new java.io.File(src));
                    }
                    if (img != null) {
                        int aW = photoArea.getWidth() - 120, aH = photoArea.getHeight() - 60;
                        if (aW < 100)
                            aW = 800;
                        if (aH < 100)
                            aH = 520;
                        double ratio = Math.min((double) aW / img.getWidth(), (double) aH / img.getHeight());
                        int sw = (int) (img.getWidth() * ratio), sh = (int) (img.getHeight() * ratio);
                        final ImageIcon icon = new ImageIcon(img.getScaledInstance(sw, sh, Image.SCALE_SMOOTH));
                        SwingUtilities.invokeLater(() -> {
                            photoLabel.setIcon(icon);
                            photoLabel.setText("");
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> photoLabel.setText("Could not load image."));
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> photoLabel.setText("Error: " + ex.getMessage()));
                }
            }).start();
        };

        prevBtn.addActionListener(e -> {
            if (current[0] > 0) {
                current[0]--;
                loadPhoto.run();
            }
        });
        nextBtn.addActionListener(e -> {
            if (current[0] < photos.size() - 1) {
                current[0]++;
                loadPhoto.run();
            }
        });

        photoArea.add(prevBtn, BorderLayout.WEST);
        photoArea.add(photoLabel, BorderLayout.CENTER);
        photoArea.add(nextBtn, BorderLayout.EAST);

        // Thumbnail strip at bottom
        JPanel thumbStrip = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        thumbStrip.setBackground(new Color(8, 10, 20));
        thumbStrip.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 40, 80)));
        thumbStrip.setPreferredSize(new Dimension(0, 80));
        for (int i = 0; i < photos.size(); i++) {
            final int idx = i;
            JButton tb = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(current[0] == idx ? new Color(60, 140, 255) : new Color(20, 28, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    if (getIcon() != null) {
                        super.paintComponent(g);
                        return;
                    }
                    g2.setColor(TEXT_MUTED);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    g2.drawString((idx + 1) + "", getWidth() / 2 - 4, getHeight() / 2 + 4);
                }
            };
            tb.setPreferredSize(new Dimension(90, 62));
            tb.setBorderPainted(false);
            tb.setFocusPainted(false);
            tb.setOpaque(false);
            tb.addActionListener(e -> {
                current[0] = idx;
                loadPhoto.run();
                thumbStrip.repaint();
            });
            // Load tiny thumbnail in bg
            final JButton tbRef = tb;
            String src2 = photos.get(i);
            new Thread(() -> {
                try {
                    java.awt.image.BufferedImage bi2 = null;
                    if (src2.startsWith("http")) {
                        /* skip URL thumbnails */ } else
                        bi2 = javax.imageio.ImageIO.read(new java.io.File(src2));
                    if (bi2 != null) {
                        ImageIcon tic = new ImageIcon(bi2.getScaledInstance(86, 58, Image.SCALE_SMOOTH));
                        SwingUtilities.invokeLater(() -> {
                            tbRef.setIcon(tic);
                            tbRef.repaint();
                        });
                    }
                } catch (Exception ignored) {
                }
            }).start();
            thumbStrip.add(tb);
        }

        // Spec bar
        JPanel specBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 10));
        specBar.setBackground(new Color(10, 13, 24));
        specBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(25, 40, 75)));
        String[][] specs = {
                { "Engine", car.getEngineSize() > 0 ? car.getEngineSize() + "L" : "—" },
                { "Fuel", car.getFuelType() != null ? car.getFuelType() : "—" },
                { "Trans.", car.getTransmission() != null ? car.getTransmission() : "—" },
                { "Stock", String.valueOf(car.getStock()) },
                { "Status", car.isAvailable() ? "Available" : "Unavailable" },
        };
        for (String[] sp : specs) {
            JPanel s = new JPanel(new GridLayout(2, 1, 0, 2));
            s.setOpaque(false);
            JLabel sk = new JLabel(sp[0], SwingConstants.CENTER);
            sk.setFont(new Font("Segoe UI", Font.BOLD, 9));
            sk.setForeground(TEXT_MUTED);
            JLabel sv = new JLabel(sp[1], SwingConstants.CENTER);
            sv.setFont(new Font("Segoe UI", Font.BOLD, 13));
            sv.setForeground(new Color(60, 180, 255));
            s.add(sk);
            s.add(sv);
            specBar.add(s);
        }

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(photoCounter, BorderLayout.NORTH);
        south.add(thumbStrip, BorderLayout.CENTER);
        south.add(specBar, BorderLayout.SOUTH);

        dlg.add(photoArea, BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);

        loadPhoto.run();
        dlg.setVisible(true);
    }

    // Returns a list of real press image URLs for well-known car models
    // (placeholder - network disabled)

    // =========================================================================
    // CAR CONFIGURATOR — fully Java2D rendered, no network required
    // Draws a photorealistic car silhouette per category with:
    // • Metallic paint with environment reflection band + specular highlights
    // • Category-specific body shapes (Sedan/SUV/Sports/Hatchback/Electric)
    // • 4 wheel designs (Standard / Sport / Racing / Off-Road)
    // • Panoramic & Convertible roof variants
    // • Interior colour visible through glass
    // • Studio floor reflection
    // =========================================================================
    private void showCarCustomizerDialog(Car car) {
        JDialog dlg = new JDialog(this, "AutoElite  —  Car Configurator: " + car.getBrand() + " " + car.getModel(),
                false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(screen.width, screen.height);
        dlg.setLocation(0, 0);
        dlg.setResizable(true);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.getContentPane().setBackground(new Color(8, 4, 18));
        Color ACC = new Color(180, 80, 255);

        // ── State ─────────────────────────────────────────────────────────────
        final Color[] bodyColor = { parseCarColor(car.getColor()) };
        final String[] wheelStyle = { "Sport" };
        final String[] roofStyle = { "Standard" };
        final String[] interiorTone = { "Dark" };
        final boolean[] showReflect = { true };
        final int[] viewAngle = { 0 }; // 0=side 1=front 2=rear 3=top

        // ── Canvas ─────────────────────────────────────────────────────────────
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                int W = getWidth(), H = getHeight();
                drawStudio(g2, W, H, bodyColor[0]);
                drawCar(g2, W, H, bodyColor[0], wheelStyle[0], roofStyle[0], interiorTone[0], car.getCategory(),
                        showReflect[0]);
                drawHUD(g2, W, H, car, bodyColor[0], wheelStyle[0], roofStyle[0], interiorTone[0]);
            }
        };
        canvas.setBackground(new Color(8, 4, 18));
        canvas.setOpaque(true);

        // ── TOP BAR ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 0, 60), getWidth(), 0, new Color(8, 4, 18));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(140, 50, 220, 80));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 80));
        topBar.setBorder(new EmptyBorder(0, 32, 0, 28));

        JPanel titleBlock = new JPanel(new GridLayout(3, 1, 0, 2));
        titleBlock.setOpaque(false);
        JLabel t1 = new JLabel("🎨  Car Configurator");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t1.setForeground(ACC);
        JLabel t2 = new JLabel(car.getBrand() + " " + car.getModel() + " (" + car.getYear() + ")  ·  "
                + (car.getCategory() != null ? car.getCategory() : ""));
        t2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t2.setForeground(new Color(190, 120, 255));
        JLabel t3 = new JLabel("Personalise your vehicle — the studio preview updates live as you choose each option.");
        t3.setFont(FONT_SMALL);
        t3.setForeground(new Color(110, 65, 175));
        titleBlock.add(t1);
        titleBlock.add(t2);
        titleBlock.add(t3);
        topBar.add(titleBlock, BorderLayout.CENTER);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 22));
        topRight.setOpaque(false);
        JButton closeBtn = glowButton("Close", ACC, e -> dlg.dispose());
        topRight.add(closeBtn);
        topBar.add(topRight, BorderLayout.EAST);
        dlg.add(topBar, BorderLayout.NORTH);

        // ── BODY ─────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(new Color(8, 4, 18));

        // LEFT: options
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(10, 5, 22));
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 20, 90)));
        leftScroll.setBackground(new Color(10, 5, 22));
        leftScroll.getViewport().setBackground(new Color(10, 5, 22));
        styleScrollBar(leftScroll.getVerticalScrollBar());
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setPreferredSize(new Dimension(296, 0));
        leftPanel.setBorder(new EmptyBorder(22, 20, 24, 20));

        // ── Colour Swatches ───────────────────────────────────────────────────
        leftPanel.add(configSection("BODY COLOUR", ACC));
        leftPanel.add(Box.createVerticalStrut(14));

        JLabel selLabel = new JLabel("Selected: " + car.getColor());
        selLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        selLabel.setForeground(ACC);
        selLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        Object[][] swatches = {
                { "Crimson", new Color(200, 22, 22) }, { "Sunset", new Color(220, 75, 10) },
                { "Gold", new Color(210, 165, 10) }, { "Emerald", new Color(20, 155, 65) },
                { "Sapphire", new Color(20, 90, 200) }, { "Arctic", new Color(10, 185, 205) },
                { "Ultraviolet", new Color(110, 25, 210) }, { "Fuchsia", new Color(195, 25, 170) },
                { "Pearl", new Color(238, 238, 242) }, { "Silver", new Color(168, 172, 178) },
                { "Graphite", new Color(58, 60, 65) }, { "Midnight", new Color(18, 18, 22) },
        };

        for (int row = 0; row < 3; row++) {
            JPanel swRow = new JPanel(new GridLayout(1, 4, 8, 0));
            swRow.setOpaque(false);
            swRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            swRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            for (int col = 0; col < 4; col++) {
                String name = (String) swatches[row * 4 + col][0];
                Color swc = (Color) swatches[row * 4 + col][1];
                JButton sw = new JButton() {
                    boolean hover = false;
                    {
                        addMouseListener(new java.awt.event.MouseAdapter() {
                            public void mouseEntered(java.awt.event.MouseEvent e) {
                                hover = true;
                                repaint();
                            }

                            public void mouseExited(java.awt.event.MouseEvent e) {
                                hover = false;
                                repaint();
                            }
                        });
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        // metallic swatch gradient
                        GradientPaint gp = new GradientPaint(0, 0, lighter(swc, 0.22f), getWidth(), getHeight(),
                                darker(swc, 0.28f));
                        g2.setPaint(gp);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        // shine
                        g2.setColor(new Color(255, 255, 255, 55));
                        g2.fillRoundRect(3, 2, getWidth() - 6, getHeight() / 2 - 2, 8, 8);
                        if (bodyColor[0].equals(swc) || hover) {
                            g2.setColor(
                                    hover && !bodyColor[0].equals(swc) ? new Color(255, 255, 255, 140) : Color.WHITE);
                            g2.setStroke(new BasicStroke(bodyColor[0].equals(swc) ? 2.5f : 1.5f));
                            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 9, 9);
                        }
                    }
                };
                sw.setOpaque(false);
                sw.setBorderPainted(false);
                sw.setFocusPainted(false);
                sw.setToolTipText(name);
                sw.addActionListener(e -> {
                    bodyColor[0] = swc;
                    selLabel.setText("Selected: " + name);
                    canvas.repaint();
                    swRow.repaint();
                });
                swRow.add(sw);
            }
            leftPanel.add(swRow);
            leftPanel.add(Box.createVerticalStrut(7));
        }

        JButton customBtn = smallButton("🎨  Custom colour…", ACC);
        customBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        customBtn.addActionListener(e -> {
            Color picked = JColorChooser.showDialog(dlg, "Choose Body Colour", bodyColor[0]);
            if (picked != null) {
                bodyColor[0] = picked;
                selLabel.setText("Selected: Custom");
                canvas.repaint();
            }
        });
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(customBtn);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(selLabel);
        leftPanel.add(Box.createVerticalStrut(26));

        // ── Roof ─────────────────────────────────────────────────────────────
        leftPanel.add(configSection("ROOF STYLE", ACC));
        leftPanel.add(Box.createVerticalStrut(10));
        ButtonGroup roofGrp = new ButtonGroup();
        for (String[] opt : new String[][] {
                { "Standard", "Full metal roof" },
                { "Panoramic", "Glass panoramic sunroof" },
                { "Convertible", "Open-top cabriolet" } }) {
            leftPanel.add(configRadio(opt[0], opt[1], opt[0].equals("Standard"), roofGrp, v -> {
                roofStyle[0] = v;
                canvas.repaint();
            }));
            leftPanel.add(Box.createVerticalStrut(2));
        }
        leftPanel.add(Box.createVerticalStrut(22));

        // ── Wheels ───────────────────────────────────────────────────────────
        leftPanel.add(configSection("WHEEL DESIGN", ACC));
        leftPanel.add(Box.createVerticalStrut(10));
        ButtonGroup wheelGrp = new ButtonGroup();
        for (String[] opt : new String[][] {
                { "Standard", "Steel hub caps" },
                { "Sport", "5-spoke alloy" },
                { "Racing", "Multi-spoke forged" },
                { "Off-Road", "Rugged off-road" } }) {
            leftPanel.add(configRadio(opt[0], opt[1], opt[0].equals("Sport"), wheelGrp, v -> {
                wheelStyle[0] = v;
                canvas.repaint();
            }));
            leftPanel.add(Box.createVerticalStrut(2));
        }
        leftPanel.add(Box.createVerticalStrut(22));

        // ── Interior ─────────────────────────────────────────────────────────
        leftPanel.add(configSection("INTERIOR", ACC));
        leftPanel.add(Box.createVerticalStrut(10));
        ButtonGroup intGrp = new ButtonGroup();
        for (String[] opt : new String[][] {
                { "Light", "Cream / Ivory leather" },
                { "Dark", "Black full leather" },
                { "Two-Tone", "Black & cream split" },
                { "Red Sport", "Red Alcantara sport" } }) {
            leftPanel.add(configRadio(opt[0], opt[1], opt[0].equals("Dark"), intGrp, v -> {
                interiorTone[0] = v;
                canvas.repaint();
            }));
            leftPanel.add(Box.createVerticalStrut(2));
        }
        leftPanel.add(Box.createVerticalStrut(22));

        // ── Studio options ────────────────────────────────────────────────────
        leftPanel.add(configSection("STUDIO", ACC));
        leftPanel.add(Box.createVerticalStrut(10));
        JCheckBox reflectCB = new JCheckBox("Show floor reflection", true);
        reflectCB.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reflectCB.setForeground(TEXT_PRIMARY);
        reflectCB.setOpaque(false);
        reflectCB.setFocusPainted(false);
        reflectCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        reflectCB.addActionListener(e -> {
            showReflect[0] = reflectCB.isSelected();
            canvas.repaint();
        });
        leftPanel.add(reflectCB);

        // RIGHT: Vehicle Specs
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(10, 5, 22));
        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(50, 20, 90)));
        rightScroll.setBackground(new Color(10, 5, 22));
        rightScroll.getViewport().setBackground(new Color(10, 5, 22));
        styleScrollBar(rightScroll.getVerticalScrollBar());
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.setPreferredSize(new Dimension(262, 0));
        rightPanel.setBorder(new EmptyBorder(22, 18, 24, 18));

        JLabel specTitle = new JLabel("Vehicle Specs");
        specTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        specTitle.setForeground(ACC);
        specTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(specTitle);
        rightPanel.add(Box.createVerticalStrut(16));

        String[][] specRows = {
                { "Brand", car.getBrand() },
                { "Model", car.getModel() },
                { "Year", "" + car.getYear() },
                { "Category", car.getCategory() != null ? car.getCategory() : "—" },
                { "Fuel", car.getFuelType() != null ? car.getFuelType() : "—" },
                { "Transmission", car.getTransmission() != null ? car.getTransmission() : "—" },
                { "Engine", car.getEngineSize() > 0 ? car.getEngineSize() + "L" : "—" },
                { "Stock", "" + car.getStock() },
                { "List Price", "$" + formatMoney(car.getPrice()) },
        };
        for (String[] sr : specRows) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 15, 55)));
            JLabel k = new JLabel(sr[0]);
            k.setFont(FONT_SMALL);
            k.setForeground(TEXT_MUTED);
            JLabel v = new JLabel(sr[1]);
            v.setFont(new Font("Segoe UI", Font.BOLD, 12));
            v.setForeground(TEXT_PRIMARY);
            v.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(k, BorderLayout.WEST);
            row.add(v, BorderLayout.EAST);
            rightPanel.add(row);
            rightPanel.add(Box.createVerticalStrut(6));
        }
        rightPanel.add(Box.createVerticalStrut(20));

        if (car.getFeatures() != null && !car.getFeatures().isEmpty()) {
            rightPanel.add(configSection("FEATURES", ACC));
            rightPanel.add(Box.createVerticalStrut(10));
            for (String feat : car.getFeatures()) {
                JLabel fl = new JLabel("•  " + feat.trim());
                fl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                fl.setForeground(new Color(175, 130, 255));
                fl.setAlignmentX(Component.LEFT_ALIGNMENT);
                rightPanel.add(fl);
                rightPanel.add(Box.createVerticalStrut(4));
            }
            rightPanel.add(Box.createVerticalStrut(20));
        }

        rightPanel.add(configSection("HOW TO USE", ACC));
        rightPanel.add(Box.createVerticalStrut(10));
        for (String tip : new String[] {
                "Click a colour swatch to repaint the car instantly.",
                "Use Custom Colour for any exact shade.",
                "Roof Style switches between solid, panoramic glass, and open-top.",
                "Wheel Design changes the rim style on both axles.",
                "Interior tints the cabin visible through the glass." }) {
            JTextArea ta = new JTextArea("• " + tip);
            ta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            ta.setForeground(new Color(130, 100, 195));
            ta.setBackground(new Color(0, 0, 0, 0));
            ta.setOpaque(false);
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setFocusable(false);
            ta.setAlignmentX(Component.LEFT_ALIGNMENT);
            ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            rightPanel.add(ta);
            rightPanel.add(Box.createVerticalStrut(7));
        }

        body.add(leftScroll, BorderLayout.WEST);
        body.add(canvas, BorderLayout.CENTER);
        body.add(rightScroll, BorderLayout.EAST);
        dlg.add(body, BorderLayout.CENTER);

        // Bottom bar
        JPanel btm = new JPanel(new BorderLayout());
        btm.setBackground(new Color(6, 3, 14));
        btm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 20, 90)),
                new EmptyBorder(9, 32, 9, 28)));
        JLabel btmLbl = new JLabel(
                "AutoElite Configurator  |  Studio rendering powered by Java2D — no network required");
        btmLbl.setFont(FONT_SMALL);
        btmLbl.setForeground(new Color(70, 40, 120));
        btm.add(btmLbl, BorderLayout.WEST);
        btm.add(glowButton("Close", ACC, e -> dlg.dispose()), BorderLayout.EAST);
        dlg.add(btm, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // ── Studio background ──────────────────────────────────────────────────────
    private void drawStudio(Graphics2D g2, int W, int H, Color bodyCol) {
        // Deep dark gradient studio bg
        GradientPaint bg = new GradientPaint(0, 0, new Color(10, 6, 22), W, H, new Color(6, 3, 14));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);
        // Subtle vignette
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(-W / 4, -H / 4, (int) (W * 1.5), (int) (H * 1.5));
        // Floor gradient
        int floorY = (int) (H * 0.74);
        GradientPaint floor = new GradientPaint(0, floorY, new Color(18, 10, 35), 0, H, new Color(4, 2, 10));
        g2.setPaint(floor);
        g2.fillRect(0, floorY, W, H - floorY);
        // Floor horizon line
        g2.setColor(new Color(bodyCol.getRed(), bodyCol.getGreen(), bodyCol.getBlue(), 18));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, floorY, W, floorY);
        g2.setStroke(new BasicStroke(1f));
        // Spotlight from top-centre
        RadialGradientPaint spot = new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(W / 2f, 0),
                W * 0.7f,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 255, 255, 14), new Color(0, 0, 0, 0) });
        g2.setPaint(spot);
        g2.fillRect(0, 0, W, H);
        // Coloured ambient glow behind car
        RadialGradientPaint glow = new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(W / 2f, (float) (H * 0.5)),
                (float) (W * 0.4),
                new float[] { 0f, 1f },
                new Color[] { new Color(bodyCol.getRed(), bodyCol.getGreen(), bodyCol.getBlue(), 22),
                        new Color(0, 0, 0, 0) });
        g2.setPaint(glow);
        g2.fillRect(0, 0, W, H);
    }

    // ── Main car drawing dispatcher ────────────────────────────────────────────
    private void drawCar(Graphics2D g2, int W, int H, Color body, String wheels,
            String roof, String interior, String cat, boolean reflect) {
        String c = cat != null ? cat.toLowerCase() : "sedan";
        int carW = (int) (W * 0.66);
        int cx = W / 2;
        int floorY = (int) (H * 0.74);
        int x0 = cx - carW / 2;

        if (c.contains("suv") || c.contains("luxury"))
            drawSUV(g2, x0, carW, floorY, body, wheels, roof, interior, reflect);
        else if (c.contains("sport"))
            drawSports(g2, x0, carW, floorY, body, wheels, roof, interior, reflect);
        else if (c.contains("hatch"))
            drawHatchback(g2, x0, carW, floorY, body, wheels, roof, interior, reflect);
        else if (c.contains("elec"))
            drawElectric(g2, x0, carW, floorY, body, wheels, roof, interior, reflect);
        else
            drawSedan(g2, x0, carW, floorY, body, wheels, roof, interior, reflect);
    }

    // ── Sedan ─────────────────────────────────────────────────────────────────
    private void drawSedan(Graphics2D g2, int x0, int carW, int floorY,
            Color body, String wheels, String roof, String interior, boolean reflect) {
        int bH = (int) (carW * 0.26);
        int tyreR = (int) (carW * 0.110);
        int bodyY = floorY - tyreR - bH + (int) (tyreR * 0.55);
        int frontX = x0 + carW - 15;
        int rearX = x0 + 15;
        int lWX = x0 + (int) (carW * 0.195);
        int rWX = x0 + (int) (carW * 0.795);

        java.awt.geom.GeneralPath bodyPath = new java.awt.geom.GeneralPath();
        bodyPath.moveTo(rearX, floorY - tyreR / 2 - 4);
        bodyPath.curveTo(rearX - 8, bodyY + bH * 0.6, rearX - 4, bodyY + bH * 0.2, rearX + 20, bodyY + bH * 0.14);
        bodyPath.curveTo(x0 + carW * 0.15, bodyY, x0 + carW * 0.22, bodyY, x0 + carW * 0.29, bodyY + 2);
        bodyPath.lineTo(x0 + carW * 0.75, bodyY + 2);
        bodyPath.curveTo(x0 + carW * 0.82, bodyY, x0 + carW * 0.90, bodyY + bH * 0.10, frontX + 5, bodyY + bH * 0.18);
        bodyPath.curveTo(frontX + 12, bodyY + bH * 0.55, frontX + 12, floorY - tyreR * 0.6, frontX,
                floorY - tyreR / 2 - 4);
        // bottom with wheel arches
        bodyPath.lineTo(rWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(rWX + tyreR + 4, floorY - 4, rWX, floorY - 4);
        bodyPath.quadTo(rWX - tyreR - 4, floorY - 4, rWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(lWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(lWX + tyreR + 4, floorY - 4, lWX, floorY - 4);
        bodyPath.quadTo(lWX - tyreR - 4, floorY - 4, lWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rearX, floorY - tyreR / 2 - 4);
        bodyPath.closePath();
        fillBodyPanel(g2, bodyPath, body, x0, bodyY, carW, bH);

        // Cabin / roofline
        int rX0 = x0 + (int) (carW * 0.26);
        int rX1 = x0 + (int) (carW * 0.77);
        int rY = bodyY - (int) (bH * 0.54);
        if (!"Convertible".equals(roof)) {
            java.awt.geom.GeneralPath roofPath = new java.awt.geom.GeneralPath();
            roofPath.moveTo(rX0, bodyY + 2);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.06, rY + 6, rX0 + (rX1 - rX0) * 0.25, rY, rX0 + (rX1 - rX0) * 0.5,
                    rY - 3);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.75, rY, rX1 - (rX1 - rX0) * 0.06, rY + 6, rX1, bodyY + 2);
            roofPath.closePath();
            fillRoof(g2, roofPath, body, roof);
        }
        drawGlass(g2, rX0, rX1, bodyY, rY, interior, "Convertible".equals(roof));
        drawBodyDetails(g2, x0, carW, bodyY, bH, floorY, body, "sedan");
        drawWheels(g2, lWX, rWX, floorY, tyreR, wheels);
        if (reflect)
            drawReflection(g2, x0, carW, floorY, tyreR, body);
    }

    // ── SUV ───────────────────────────────────────────────────────────────────
    private void drawSUV(Graphics2D g2, int x0, int carW, int floorY,
            Color body, String wheels, String roof, String interior, boolean reflect) {
        int bH = (int) (carW * 0.31);
        int tyreR = (int) (carW * 0.126);
        int bodyY = floorY - tyreR - bH + (int) (tyreR * 0.50);
        int frontX = x0 + carW - 14;
        int rearX = x0 + 12;
        int lWX = x0 + (int) (carW * 0.200);
        int rWX = x0 + (int) (carW * 0.800);

        java.awt.geom.GeneralPath bodyPath = new java.awt.geom.GeneralPath();
        bodyPath.moveTo(rearX, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rearX - 2, bodyY + bH * 0.18);
        bodyPath.lineTo(rearX + 10, bodyY + 4);
        bodyPath.lineTo(x0 + carW * 0.26, bodyY);
        bodyPath.lineTo(x0 + carW * 0.78, bodyY);
        bodyPath.curveTo(x0 + carW * 0.88, bodyY, frontX + 10, bodyY + bH * 0.14, frontX + 12, bodyY + bH * 0.30);
        bodyPath.lineTo(frontX + 12, floorY - tyreR * 0.6);
        bodyPath.lineTo(frontX, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rWX + tyreR + 5, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(rWX + tyreR + 5, floorY - 4, rWX, floorY - 4);
        bodyPath.quadTo(rWX - tyreR - 5, floorY - 4, rWX - tyreR - 5, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(lWX + tyreR + 5, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(lWX + tyreR + 5, floorY - 4, lWX, floorY - 4);
        bodyPath.quadTo(lWX - tyreR - 5, floorY - 4, lWX - tyreR - 5, floorY - tyreR / 2 - 4);
        bodyPath.closePath();
        fillBodyPanel(g2, bodyPath, body, x0, bodyY, carW, bH);

        int rX0 = x0 + (int) (carW * 0.18);
        int rX1 = x0 + (int) (carW * 0.85);
        int rY = bodyY - (int) (bH * 0.65);
        if (!"Convertible".equals(roof)) {
            java.awt.geom.GeneralPath roofPath = new java.awt.geom.GeneralPath();
            roofPath.moveTo(rX0, bodyY);
            roofPath.lineTo(rX0, rY + 4);
            roofPath.lineTo(rX0 + 14, rY);
            roofPath.lineTo(rX1 - 14, rY);
            roofPath.lineTo(rX1, rY + 4);
            roofPath.lineTo(rX1, bodyY);
            roofPath.closePath();
            fillRoof(g2, roofPath, body, roof);
        }
        drawGlass(g2, rX0, rX1, bodyY, rY, interior, "Convertible".equals(roof));
        drawBodyDetails(g2, x0, carW, bodyY, bH, floorY, body, "suv");
        drawWheels(g2, lWX, rWX, floorY, tyreR, wheels);
        if (reflect)
            drawReflection(g2, x0, carW, floorY, tyreR, body);
    }

    // ── Sports Car ────────────────────────────────────────────────────────────
    private void drawSports(Graphics2D g2, int x0, int carW, int floorY,
            Color body, String wheels, String roof, String interior, boolean reflect) {
        int bH = (int) (carW * 0.21);
        int tyreR = (int) (carW * 0.108);
        int bodyY = floorY - tyreR - bH + (int) (tyreR * 0.60);
        int frontX = x0 + carW - 8;
        int rearX = x0 + 18;
        int lWX = x0 + (int) (carW * 0.200);
        int rWX = x0 + (int) (carW * 0.800);

        java.awt.geom.GeneralPath bodyPath = new java.awt.geom.GeneralPath();
        bodyPath.moveTo(rearX, floorY - tyreR / 2 - 4);
        bodyPath.curveTo(rearX - 10, bodyY + bH * 0.7, rearX, bodyY + bH * 0.3, rearX + 28, bodyY + bH * 0.20);
        bodyPath.lineTo(x0 + carW * 0.30, bodyY + bH * 0.12);
        bodyPath.curveTo(x0 + carW * 0.55, bodyY - 8, x0 + carW * 0.72, bodyY + bH * 0.04, x0 + carW * 0.82,
                bodyY + bH * 0.14);
        bodyPath.curveTo(frontX + 8, bodyY + bH * 0.32, frontX + 10, bodyY + bH * 0.60, frontX + 6, bodyY + bH * 0.78);
        bodyPath.lineTo(frontX, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(rWX + tyreR + 4, floorY - 4, rWX, floorY - 4);
        bodyPath.quadTo(rWX - tyreR - 4, floorY - 4, rWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(lWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(lWX + tyreR + 4, floorY - 4, lWX, floorY - 4);
        bodyPath.quadTo(lWX - tyreR - 4, floorY - 4, lWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.closePath();
        fillBodyPanel(g2, bodyPath, body, x0, bodyY, carW, bH);

        int rX0 = x0 + (int) (carW * 0.28);
        int rX1 = x0 + (int) (carW * 0.78);
        int rY = bodyY - (int) (bH * 0.42);
        if (!"Convertible".equals(roof)) {
            java.awt.geom.GeneralPath roofPath = new java.awt.geom.GeneralPath();
            roofPath.moveTo(rX0, bodyY + bH * 0.12);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.10, rY + 2, rX0 + (rX1 - rX0) * 0.35, rY - 2,
                    rX0 + (rX1 - rX0) * 0.52, rY - 4);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.72, rY, rX1 - (rX1 - rX0) * 0.08, rY + 6, rX1, bodyY + bH * 0.14);
            roofPath.closePath();
            fillRoof(g2, roofPath, body, roof);
        }
        drawGlass(g2, rX0, rX1, (int) (bodyY + bH * 0.12), rY, interior, "Convertible".equals(roof));
        drawBodyDetails(g2, x0, carW, bodyY, bH, floorY, body, "sports");
        drawWheels(g2, lWX, rWX, floorY, tyreR, wheels);
        if (reflect)
            drawReflection(g2, x0, carW, floorY, tyreR, body);
    }

    // ── Hatchback ─────────────────────────────────────────────────────────────
    private void drawHatchback(Graphics2D g2, int x0, int carW, int floorY,
            Color body, String wheels, String roof, String interior, boolean reflect) {
        int bH = (int) (carW * 0.255);
        int tyreR = (int) (carW * 0.108);
        int bodyY = floorY - tyreR - bH + (int) (tyreR * 0.55);
        int frontX = x0 + carW - 14;
        int rearX = x0 + 14;
        int lWX = x0 + (int) (carW * 0.190);
        int rWX = x0 + (int) (carW * 0.790);

        java.awt.geom.GeneralPath bodyPath = new java.awt.geom.GeneralPath();
        bodyPath.moveTo(rearX, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rearX - 4, bodyY + bH * 0.2);
        bodyPath.lineTo(rearX + 14, bodyY + 2);
        bodyPath.lineTo(x0 + carW * 0.26, bodyY);
        bodyPath.lineTo(x0 + carW * 0.76, bodyY);
        bodyPath.curveTo(x0 + carW * 0.86, bodyY, frontX + 8, bodyY + bH * 0.18, frontX + 10, bodyY + bH * 0.38);
        bodyPath.lineTo(frontX + 10, floorY - tyreR * 0.55);
        bodyPath.lineTo(frontX, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(rWX + tyreR + 4, floorY - 4, rWX, floorY - 4);
        bodyPath.quadTo(rWX - tyreR - 4, floorY - 4, rWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(lWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(lWX + tyreR + 4, floorY - 4, lWX, floorY - 4);
        bodyPath.quadTo(lWX - tyreR - 4, floorY - 4, lWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.closePath();
        fillBodyPanel(g2, bodyPath, body, x0, bodyY, carW, bH);

        int rX0 = x0 + (int) (carW * 0.215);
        int rX1 = x0 + (int) (carW * 0.780);
        int rY = bodyY - (int) (bH * 0.52);
        if (!"Convertible".equals(roof)) {
            java.awt.geom.GeneralPath roofPath = new java.awt.geom.GeneralPath();
            roofPath.moveTo(rX0, bodyY + 2);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.07, rY + 4, rX0 + (rX1 - rX0) * 0.28, rY, rX0 + (rX1 - rX0) * 0.52,
                    rY - 2);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.76, rY, rX1 - 8, rY + 8, rX1 - 4, bodyY + 2);
            roofPath.closePath();
            fillRoof(g2, roofPath, body, roof);
        }
        drawGlass(g2, rX0, rX1, bodyY, rY, interior, "Convertible".equals(roof));
        drawBodyDetails(g2, x0, carW, bodyY, bH, floorY, body, "hatchback");
        drawWheels(g2, lWX, rWX, floorY, tyreR, wheels);
        if (reflect)
            drawReflection(g2, x0, carW, floorY, tyreR, body);
    }

    // ── Electric (sleek fastback) ─────────────────────────────────────────────
    private void drawElectric(Graphics2D g2, int x0, int carW, int floorY,
            Color body, String wheels, String roof, String interior, boolean reflect) {
        int bH = (int) (carW * 0.235);
        int tyreR = (int) (carW * 0.108);
        int bodyY = floorY - tyreR - bH + (int) (tyreR * 0.58);
        int frontX = x0 + carW - 12;
        int rearX = x0 + 14;
        int lWX = x0 + (int) (carW * 0.195);
        int rWX = x0 + (int) (carW * 0.800);

        java.awt.geom.GeneralPath bodyPath = new java.awt.geom.GeneralPath();
        bodyPath.moveTo(rearX + 4, floorY - tyreR / 2 - 4);
        bodyPath.curveTo(rearX - 6, bodyY + bH * 0.55, rearX + 6, bodyY + bH * 0.22, rearX + 30, bodyY + bH * 0.16);
        bodyPath.curveTo(x0 + carW * 0.18, bodyY + 2, x0 + carW * 0.25, bodyY, x0 + carW * 0.30, bodyY);
        bodyPath.lineTo(x0 + carW * 0.72, bodyY);
        bodyPath.curveTo(x0 + carW * 0.82, bodyY - 4, x0 + carW * 0.88, bodyY + bH * 0.08, frontX + 4,
                bodyY + bH * 0.22);
        bodyPath.curveTo(frontX + 12, bodyY + bH * 0.48, frontX + 12, floorY - tyreR * 0.65, frontX,
                floorY - tyreR / 2 - 4);
        bodyPath.lineTo(rWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(rWX + tyreR + 4, floorY - 4, rWX, floorY - 4);
        bodyPath.quadTo(rWX - tyreR - 4, floorY - 4, rWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.lineTo(lWX + tyreR + 4, floorY - tyreR / 2 - 4);
        bodyPath.quadTo(lWX + tyreR + 4, floorY - 4, lWX, floorY - 4);
        bodyPath.quadTo(lWX - tyreR - 4, floorY - 4, lWX - tyreR - 4, floorY - tyreR / 2 - 4);
        bodyPath.closePath();
        fillBodyPanel(g2, bodyPath, body, x0, bodyY, carW, bH);

        int rX0 = x0 + (int) (carW * 0.27);
        int rX1 = x0 + (int) (carW * 0.78);
        int rY = bodyY - (int) (bH * 0.50);
        if (!"Convertible".equals(roof)) {
            java.awt.geom.GeneralPath roofPath = new java.awt.geom.GeneralPath();
            roofPath.moveTo(rX0, bodyY + 2);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.08, rY + 3, rX0 + (rX1 - rX0) * 0.30, rY - 2,
                    rX0 + (rX1 - rX0) * 0.54, rY - 4);
            roofPath.curveTo(rX0 + (rX1 - rX0) * 0.78, rY, rX1 - (rX1 - rX0) * 0.06, rY + 8, rX1 - 8, bodyY + 2);
            roofPath.closePath();
            fillRoof(g2, roofPath, body, roof);
        }
        drawGlass(g2, rX0, rX1, bodyY, rY, interior, "Convertible".equals(roof));
        drawBodyDetails(g2, x0, carW, bodyY, bH, floorY, body, "electric");
        // LED strip (electric cars have light bar)
        g2.setColor(new Color(100, 220, 255, 180));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(frontX - 22, (int) (bodyY + bH * 0.30), frontX + 8, (int) (bodyY + bH * 0.30));
        g2.drawLine(rearX - 8, (int) (bodyY + bH * 0.30), rearX + 22, (int) (bodyY + bH * 0.30));
        g2.setStroke(new BasicStroke(1f));
        drawWheels(g2, lWX, rWX, floorY, tyreR, wheels);
        if (reflect)
            drawReflection(g2, x0, carW, floorY, tyreR, body);
    }

    // ── Fill body panel with metallic paint ───────────────────────────────────
    private void fillBodyPanel(Graphics2D g2, java.awt.geom.GeneralPath path,
            Color body, int x0, int bodyY, int carW, int bH) {
        // Base metallic gradient
        GradientPaint base = new GradientPaint(x0, bodyY, lighter(body, 0.30f), x0, bodyY + bH, darker(body, 0.38f));
        g2.setPaint(base);
        g2.fill(path);
        // Environment reflection band (horizontal bright stripe across mid-body)
        java.awt.geom.Area area = new java.awt.geom.Area(path);
        int reflY = (int) (bodyY + bH * 0.38);
        java.awt.geom.Rectangle2D reflRect = new java.awt.geom.Rectangle2D.Float(x0, reflY, carW, (int) (bH * 0.22));
        java.awt.geom.Area reflArea = new java.awt.geom.Area(reflRect);
        reflArea.intersect(area);
        GradientPaint reflGrad = new GradientPaint(0, reflY, new Color(255, 255, 255, 0), 0, reflY + bH * 0.11f,
                new Color(255, 255, 255, 36));
        g2.setPaint(reflGrad);
        g2.fill(reflArea);
        // Upper highlight (top specular)
        java.awt.geom.Rectangle2D topRect = new java.awt.geom.Rectangle2D.Float(x0, bodyY, carW, (int) (bH * 0.28));
        java.awt.geom.Area topArea = new java.awt.geom.Area(topRect);
        topArea.intersect(new java.awt.geom.Area(path));
        g2.setColor(new Color(255, 255, 255, 28));
        g2.fill(topArea);
        // Dark lower shadow
        java.awt.geom.Rectangle2D botRect = new java.awt.geom.Rectangle2D.Float(x0, bodyY + bH * 0.72f, carW,
                (int) (bH * 0.35));
        java.awt.geom.Area botArea = new java.awt.geom.Area(botRect);
        botArea.intersect(new java.awt.geom.Area(path));
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(botArea);
        // Outline
        g2.setColor(darker(body, 0.55f));
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(path);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Roof fill ─────────────────────────────────────────────────────────────
    private void fillRoof(Graphics2D g2, java.awt.geom.GeneralPath roofPath, Color body, String roof) {
        Color roofBase = darker(body, 0.22f);
        GradientPaint rg = new GradientPaint(0, 0, lighter(roofBase, 0.12f), 0, 400, darker(roofBase, 0.32f));
        g2.setPaint(rg);
        g2.fill(roofPath);
        if ("Panoramic".equals(roof)) {
            // Glass strip on roof
            java.awt.geom.Area ra = new java.awt.geom.Area(roofPath);
            g2.setColor(new Color(120, 200, 255, 55));
            g2.fill(ra);
            g2.setColor(new Color(200, 240, 255, 80));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(roofPath);
        } else {
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fill(roofPath);
            g2.setColor(darker(roofBase, 0.48f));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(roofPath);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Glass / windows ───────────────────────────────────────────────────────
    private void drawGlass(Graphics2D g2, int rX0, int rX1, int bodyY, int rY, String interior, boolean convertible) {
        Color intC = switch (interior) {
            case "Light" -> new Color(210, 190, 155, 80);
            case "Two-Tone" -> new Color(130, 110, 85, 80);
            case "Red Sport" -> new Color(150, 25, 25, 80);
            default -> new Color(20, 18, 28, 95);
        };
        Color glassC = new Color(140, 200, 240, 75);
        Color shimmer = new Color(220, 240, 255, 50);
        int rW = rX1 - rX0;

        if (convertible) {
            // Just windshield frame
            java.awt.geom.GeneralPath ws = new java.awt.geom.GeneralPath();
            ws.moveTo(rX0, bodyY + 2);
            ws.curveTo(rX0 + rW * 0.04, rY + 3, rX0 + rW * 0.20, rY + 1, rX0 + rW * 0.30, rY + 2);
            ws.lineTo(rX0 + rW * 0.34, bodyY + 2);
            ws.closePath();
            g2.setPaint(new GradientPaint(rX0, rY, glassC, rX0 + rW * 0.34f, bodyY + 2, intC));
            g2.fill(ws);
            g2.setColor(shimmer);
            g2.fill(ws);
            return;
        }

        // Windshield
        java.awt.geom.GeneralPath ws = new java.awt.geom.GeneralPath();
        ws.moveTo(rX0, bodyY + 2);
        ws.curveTo(rX0 + rW * 0.04, rY + 4, rX0 + rW * 0.22, rY + 1, rX0 + rW * 0.34, rY + 2);
        ws.lineTo(rX0 + rW * 0.38, bodyY + 2);
        ws.closePath();
        g2.setPaint(new GradientPaint(rX0, rY, glassC, rX0 + rW * 0.38f, bodyY + 2, intC));
        g2.fill(ws);
        g2.setColor(shimmer);
        g2.fill(ws);

        // Rear window
        java.awt.geom.GeneralPath rw = new java.awt.geom.GeneralPath();
        rw.moveTo(rX1, bodyY + 2);
        rw.curveTo(rX1 - rW * 0.04, rY + 4, rX1 - rW * 0.22, rY + 1, rX1 - rW * 0.34, rY + 2);
        rw.lineTo(rX1 - rW * 0.38, bodyY + 2);
        rw.closePath();
        g2.setPaint(new GradientPaint(rX1, rY, glassC, rX1 - rW * 0.38f, bodyY + 2, intC));
        g2.fill(rw);
        g2.setColor(shimmer);
        g2.fill(rw);

        // Side windows
        java.awt.geom.GeneralPath sw = new java.awt.geom.GeneralPath();
        sw.moveTo(rX0 + rW * 0.38, bodyY + 2);
        sw.lineTo(rX0 + rW * 0.34, rY + 2);
        sw.lineTo(rX1 - rW * 0.34, rY + 2);
        sw.lineTo(rX1 - rW * 0.38, bodyY + 2);
        sw.closePath();
        g2.setColor(intC);
        g2.fill(sw);
        g2.setColor(glassC);
        g2.fill(sw);
        g2.setColor(shimmer);
        g2.fill(sw);

        // Pillar lines
        g2.setColor(new Color(0, 0, 0, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(rX0 + (int) (rW * 0.38), bodyY + 2, rX0 + (int) (rW * 0.34), rY + 2);
        g2.drawLine(rX1 - (int) (rW * 0.38), bodyY + 2, rX1 - (int) (rW * 0.34), rY + 2);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Body details: lights, door lines, grille ───────────────────────────────
    private void drawBodyDetails(Graphics2D g2, int x0, int carW, int bodyY, int bH,
            int floorY, Color body, String cat) {
        int frontX = x0 + carW;
        int rearX = x0;
        boolean suv = cat.equals("suv");
        boolean sport = cat.equals("sports");

        // Door crease line
        g2.setColor(new Color(0, 0, 0, 35));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x0 + (int) (carW * 0.23), (int) (bodyY + bH * 0.58), x0 + (int) (carW * 0.81),
                (int) (bodyY + bH * 0.60));
        g2.setStroke(new BasicStroke(1f));

        // Door handles
        g2.setColor(new Color(255, 255, 255, 65));
        int hY = (int) (bodyY + bH * 0.50);
        g2.fillRoundRect(x0 + (int) (carW * 0.35), hY, (int) (carW * 0.11), 5, 3, 3);
        g2.fillRoundRect(x0 + (int) (carW * 0.54), hY, (int) (carW * 0.11), 5, 3, 3);

        // Front headlight assembly
        g2.setColor(new Color(255, 245, 200, 200));
        g2.fillRoundRect(frontX - 22, (int) (bodyY + bH * (sport ? 0.26 : 0.28)), 18, sport ? 7 : 10, 4, 4);
        // DRL strip
        g2.setColor(new Color(255, 255, 230, 160));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(frontX - 26, (int) (bodyY + bH * (sport ? 0.20 : 0.22)), frontX - 4,
                (int) (bodyY + bH * (sport ? 0.22 : 0.24)));
        // Headlight glow
        RadialGradientPaint hlGlow = new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(frontX - 14, (float) (bodyY + bH * 0.30)),
                50, new float[] { 0f, 1f }, new Color[] { new Color(255, 245, 180, 55), new Color(0, 0, 0, 0) });
        g2.setPaint(hlGlow);
        g2.fillOval(frontX - 60, (int) (bodyY + bH * 0.08), 80, 55);

        // Rear taillight
        g2.setColor(new Color(255, 30, 30, 220));
        g2.fillRoundRect(rearX + 5, (int) (bodyY + bH * 0.28), 16, 9, 4, 4);
        g2.setColor(new Color(180, 20, 20, 120));
        g2.fillRoundRect(rearX + 5, (int) (bodyY + bH * 0.42), 12, 6, 3, 3);

        // Front grille
        g2.setColor(darker(body, 0.62f));
        if (sport) {
            g2.fillRoundRect(frontX - 8, (int) (bodyY + bH * 0.55), 6, (int) (bH * 0.26), 2, 2);
        } else {
            for (int gi = 0; gi < 4; gi++) {
                g2.fillRoundRect(frontX - 10, (int) (bodyY + bH * (0.46 + gi * 0.07)), 8, 4, 2, 2);
            }
        }
        // Fuel/charge port
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(rearX + (int) (carW * 0.82), (int) (bodyY + bH * 0.44), 8, 6);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Wheels ────────────────────────────────────────────────────────────────
    private void drawWheels(Graphics2D g2, int lWX, int rWX, int floorY, int r, String style) {
        drawSingleWheel(g2, lWX, floorY, r, style);
        drawSingleWheel(g2, rWX, floorY, r, style);
    }

    private void drawSingleWheel(Graphics2D g2, int cx, int cy, int r, String style) {
        // Tyre shadow
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(cx - r - 2, cy - r + 6, (r + 2) * 2, (r + 2) * 2);
        // Tyre
        GradientPaint tyrePaint = new GradientPaint(cx - r, cy - r, new Color(38, 36, 40), cx + r, cy + r,
                new Color(20, 18, 22));
        g2.setPaint(tyrePaint);
        g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        // Tyre highlight
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillOval(cx - r + 2, cy - r + 2, r, r / 2);
        // Tyre sidewall ring
        g2.setColor(new Color(60, 58, 64));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(cx - r + 3, cy - r + 3, (r - 3) * 2, (r - 3) * 2);
        g2.setStroke(new BasicStroke(1f));

        int ri = (int) (r * 0.73);
        // Rim base gradient
        GradientPaint rimBase = new GradientPaint(cx - ri, cy - ri, new Color(215, 218, 225), cx + ri, cy + ri,
                new Color(140, 144, 152));
        g2.setPaint(rimBase);
        g2.fillOval(cx - ri, cy - ri, 2 * ri, 2 * ri);

        switch (style) {
            case "Standard" -> {
                // Hub cap — spoke cross
                g2.setColor(new Color(190, 193, 200));
                g2.fillOval(cx - ri + 3, cy - ri + 3, (ri - 3) * 2, (ri - 3) * 2);
                g2.setColor(new Color(130, 133, 140));
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(i * 45);
                    g2.drawLine(cx, cy, cx + (int) (Math.cos(a) * ri * 0.88), cy + (int) (Math.sin(a) * ri * 0.88));
                }
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(100, 103, 110));
                g2.fillOval(cx - 7, cy - 7, 14, 14);
            }
            case "Sport" -> {
                // 5-spoke alloy with concave face
                g2.setColor(new Color(230, 233, 240));
                for (int i = 0; i < 5; i++) {
                    double ang = Math.toRadians(i * 72 - 90);
                    double a2 = Math.toRadians(i * 72 - 90 + 36), a3 = Math.toRadians(i * 72 - 90 - 36);
                    int x1 = (int) (Math.cos(ang) * ri * 0.90), y1 = (int) (Math.sin(ang) * ri * 0.90);
                    int x2 = (int) (Math.cos(a2) * ri * 0.30), y2 = (int) (Math.sin(a2) * ri * 0.30);
                    int x3 = (int) (Math.cos(a3) * ri * 0.30), y3 = (int) (Math.sin(a3) * ri * 0.30);
                    java.awt.geom.GeneralPath sp = new java.awt.geom.GeneralPath();
                    sp.moveTo(cx + x1, cy + y1);
                    sp.lineTo(cx + x2, cy + y2);
                    sp.lineTo(cx + x3, cy + y3);
                    sp.closePath();
                    GradientPaint sp2 = new GradientPaint(cx + x1, cy + y1, new Color(240, 243, 250), cx, cy,
                            new Color(180, 183, 190));
                    g2.setPaint(sp2);
                    g2.fill(sp);
                    g2.setColor(new Color(100, 103, 110));
                    g2.draw(sp);
                }
                // Centre bolt circle
                g2.setColor(new Color(70, 72, 78));
                g2.fillOval(cx - 8, cy - 8, 16, 16);
                g2.setColor(new Color(200, 202, 208));
                g2.fillOval(cx - 4, cy - 4, 8, 8);
            }
            case "Racing" -> {
                // 10-spoke forged
                g2.setColor(new Color(235, 238, 245));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(i * 36);
                    g2.drawLine(cx + (int) (Math.cos(a) * ri * 0.22), cy + (int) (Math.sin(a) * ri * 0.22),
                            cx + (int) (Math.cos(a) * ri * 0.91), cy + (int) (Math.sin(a) * ri * 0.91));
                }
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(60, 62, 68));
                g2.fillOval(cx - 7, cy - 7, 14, 14);
                // Gold centre cap
                GradientPaint goldCap = new GradientPaint(cx - 4, cy - 4, new Color(255, 215, 60), cx + 4, cy + 4,
                        new Color(200, 155, 20));
                g2.setPaint(goldCap);
                g2.fillOval(cx - 4, cy - 4, 8, 8);
            }
            case "Off-Road" -> {
                // Beefy 6-spoke
                g2.setColor(new Color(195, 190, 180));
                g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    g2.drawLine(cx + (int) (Math.cos(a) * ri * 0.18), cy + (int) (Math.sin(a) * ri * 0.18),
                            cx + (int) (Math.cos(a) * ri * 0.89), cy + (int) (Math.sin(a) * ri * 0.89));
                }
                g2.setStroke(new BasicStroke(1f));
                // Knobby tread marks
                g2.setColor(new Color(55, 52, 55));
                g2.setStroke(new BasicStroke(4f));
                for (int i = 0; i < 14; i++) {
                    double a = Math.toRadians(i * (360.0 / 14));
                    g2.drawLine(cx + (int) (Math.cos(a) * (r - 5)), cy + (int) (Math.sin(a) * (r - 5)),
                            cx + (int) (Math.cos(a) * r), cy + (int) (Math.sin(a) * r));
                }
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(85, 80, 75));
                g2.fillOval(cx - 7, cy - 7, 14, 14);
            }
        }
        // Centre point
        g2.setColor(new Color(35, 33, 38));
        g2.fillOval(cx - 3, cy - 3, 6, 6);
    }

    // ── Floor reflection ──────────────────────────────────────────────────────
    private void drawReflection(Graphics2D g2, int x0, int carW, int floorY, int tyreR, Color body) {
        // Mirror of the body silhouette, faded below the floor line
        int reflH = (int) (carW * 0.14);
        GradientPaint fade = new GradientPaint(0, floorY, new Color(body.getRed(), body.getGreen(), body.getBlue(), 28),
                0, floorY + reflH, new Color(0, 0, 0, 0));
        g2.setPaint(fade);
        java.awt.geom.Rectangle2D reflRect = new java.awt.geom.Rectangle2D.Float(x0, floorY, carW, reflH);
        g2.fill(reflRect);
        // Floor line gloss
        GradientPaint lineGlow = new GradientPaint(x0, floorY, new Color(255, 255, 255, 0), x0 + carW / 2, floorY,
                new Color(255, 255, 255, 20));
        g2.setPaint(lineGlow);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x0 + 20, floorY, x0 + carW - 20, floorY);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── HUD overlay (model name + RGB) ─────────────────────────────────────────
    private void drawHUD(Graphics2D g2, int W, int H, Car car, Color body, String wheels, String roof,
            String interior) {
        // Car name bottom centre
        String label = car.getBrand() + " " + car.getModel() + "  ·  " + wheels + " wheels  ·  " + roof + " roof  ·  "
                + interior + " interior";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(label);
        int lx = (W - lw) / 2, ly = H - 14;
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRoundRect(lx - 10, ly - 14, lw + 20, 20, 6, 6);
        g2.setColor(new Color(180, 150, 230, 190));
        g2.drawString(label, lx, ly);
        // Colour swatch chip top-right
        GradientPaint chip = new GradientPaint(W - 52, 14, lighter(body, 0.25f), W - 22, 44, darker(body, 0.28f));
        g2.setPaint(chip);
        g2.fillOval(W - 52, 14, 30, 30);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(W - 52, 14, 30, 30);
        // RGB label
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        g2.setColor(new Color(130, 100, 190, 180));
        String rgb = "RGB " + body.getRed() + "," + body.getGreen() + "," + body.getBlue();
        g2.drawString(rgb, W - 52, 56);
    }

    // ── Shared helpers ────────────────────────────────────────────────────────
    private JPanel configSection(String label, Color accent) {
        JLabel l = new JLabel("  " + label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50)));
        p.add(l);
        return p;
    }

    private JPanel configRadio(String label, String desc, boolean selected, ButtonGroup grp,
            java.util.function.Consumer<String> onChange) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        JRadioButton rb = new JRadioButton(label, selected);
        rb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        rb.setForeground(TEXT_PRIMARY);
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        grp.add(rb);
        JLabel dl = new JLabel(desc);
        dl.setFont(FONT_SMALL);
        dl.setForeground(TEXT_MUTED);
        rb.addActionListener(e -> onChange.accept(label));
        row.add(rb, BorderLayout.WEST);
        row.add(dl, BorderLayout.CENTER);
        return row;
    }

    private Color parseCarColor(String s) {
        if (s == null)
            return new Color(168, 172, 178);
        String c = s.toLowerCase();
        if (c.contains("red") || c.contains("crimson") || c.contains("scarlet"))
            return new Color(200, 22, 22);
        if (c.contains("blue") || c.contains("navy") || c.contains("sapphire"))
            return new Color(20, 90, 200);
        if (c.contains("green") || c.contains("emerald"))
            return new Color(20, 155, 65);
        if (c.contains("yellow") || c.contains("gold") || c.contains("amber"))
            return new Color(210, 165, 10);
        if (c.contains("orange"))
            return new Color(210, 85, 15);
        if (c.contains("purple") || c.contains("violet") || c.contains("mauve"))
            return new Color(110, 25, 210);
        if (c.contains("white") || c.contains("pearl") || c.contains("ivory"))
            return new Color(238, 238, 242);
        if (c.contains("silver") || c.contains("grey") || c.contains("gray") || c.contains("quartz"))
            return new Color(168, 172, 178);
        if (c.contains("black") || c.contains("midnight") || c.contains("obsidian"))
            return new Color(18, 18, 22);
        if (c.contains("brown") || c.contains("bronze") || c.contains("copper"))
            return new Color(130, 80, 35);
        if (c.contains("xanto") || c.contains("arancio"))
            return new Color(220, 90, 10);
        return new Color(168, 172, 178);
    }

    private Color lighter(Color c, float amt) {
        return new Color(Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * amt)),
                Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * amt)),
                Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * amt)));
    }

    private Color darker(Color c, float amt) {
        return new Color(Math.max(0, (int) (c.getRed() * (1 - amt))),
                Math.max(0, (int) (c.getGreen() * (1 - amt))),
                Math.max(0, (int) (c.getBlue() * (1 - amt))));
    }

    /** KPI panel label helpers */
    private JLabel kpiLabel(String text, Color color) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel kpiBig(String text, Color color) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel kpiSmall(String text, Color color) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JSeparator kpiDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 50, 80));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🌐 AutoElite 3D Configurator
    //
    // HOW IT WORKS (no CORS issues, no auth needed):
    // 1. Java calls Sketchfab public search REST API server-side
    // 2. Picks the best-matching embeddable 3D model for the car
    // 3. Bakes the Sketchfab UID directly into the generated HTML
    // 4. Browser just loads <iframe src="sketchfab.com/models/{uid}/embed">
    // → full 3D orbit/zoom of the REAL car model
    // 5. Falls back to Three.js category geometry if offline / no model found
    // ═══════════════════════════════════════════════════════════════════════════

    /** Entry point – called when user clicks "🌐 Customizer" button */
    private void show3DConfigurator(Car car) {
        try {
            // Build features JSON
            StringBuilder feat = new StringBuilder("[");
            if (car.getFeatures() != null) {
                for (int i = 0; i < car.getFeatures().size(); i++) {
                    if (i > 0)
                        feat.append(",");
                    feat.append("\"")
                            .append(car.getFeatures().get(i)
                                    .replace("\\", "").replace("\"", "'"))
                            .append("\"");
                }
            }
            feat.append("]");

            // ── Step 1: Search Sketchfab from Java (server-side, no CORS) ──
            setStatus("🔍 Searching for real 3D model of " + car.getBrand() + " " + car.getModel() + "…");
            String sketchfabUid = findSketchfabModel(car.getBrand(), car.getModel(), car.getYear(), car.getCategory());
            if (sketchfabUid != null) {
                setStatus("✅ Found real model: " + car.getBrand() + " " + car.getModel());
            } else {
                setStatus("ℹ No Sketchfab model found — using Three.js shape");
            }

            // ── Step 2: Build HTML with UID already baked in ────────────────
            String html = build3DConfiguratorHTML(car, feat.toString(), sketchfabUid);

            // ── Step 3: Write and open ───────────────────────────────────────
            java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            java.io.File htmlFile = new java.io.File(tmpDir, "autoelite_3d_" + car.getId() + ".html");
            try (java.io.FileWriter fw = new java.io.FileWriter(htmlFile, java.nio.charset.StandardCharsets.UTF_8)) {
                fw.write(html);
            }

            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(htmlFile.toURI());
                setStatus("✅ 3D Configurator opened — " + car.getBrand() + " " + car.getModel()
                        + (sketchfabUid != null ? " (real model)" : " (3D shape)"));
            } else {
                JOptionPane.showMessageDialog(this,
                        "Saved to:\n" + htmlFile.getAbsolutePath() + "\n\nOpen in Chrome or Edge.",
                        "3D Configurator", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showError("3D Configurator error:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sketchfab public search — no API key required for GET /v3/search
    // Returns the best-match model UID, or null if none found / offline
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Searches Sketchfab and returns a GUARANTEED-WORKING embed URL.
     *
     * KEY FIX: Extracts Sketchfab's own "embedUrl" field from the search JSON
     * instead of constructing one from the UID. The constructed URL caused 404s
     * when models were private, unpublished, or the UID was parsed incorrectly.
     *
     * Tries 4 progressively broader queries; returns null if offline / no match.
     */
    private String findSketchfabModel(String brand, String model, int year, String category) {
        String[] queries = {
                brand + " " + model + " " + year,
                brand + " " + model,
                brand + " " + model + " car",
                brand + " " + (category != null ? category : "sedan") + " car"
        };

        for (String q : queries) {
            try {
                String encoded = java.net.URLEncoder.encode(q.trim(), "UTF-8");
                // published=true ensures only live, accessible models are returned
                String apiUrl = "https://api.sketchfab.com/v3/search"
                        + "?type=models&q=" + encoded
                        + "&count=24&sort_by=-likeCount"
                        + "&published=true"; // ← only published (non-404) models

                java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL(apiUrl).openConnection();
                con.setConnectTimeout(6000);
                con.setReadTimeout(10000);
                con.setRequestProperty("User-Agent", "AutoElite-CarShowroom/1.0");

                if (con.getResponseCode() != 200) {
                    con.disconnect();
                    continue;
                }

                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                                con.getInputStream(),
                                java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder json = new StringBuilder();
                String ln;
                while ((ln = br.readLine()) != null)
                    json.append(ln);
                br.close();
                con.disconnect();

                // Extract the embedUrl field (not UID) — guaranteed to work
                String embedUrl = pickBestEmbedUrl(json.toString(), brand, model);
                if (embedUrl != null) {
                    System.out.println("[3D] Embed URL from query '" + q + "': " + embedUrl);
                    return embedUrl;
                }

            } catch (Exception e) {
                System.out.println("[3D] Query failed for '" + q + "': " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Parses Sketchfab search JSON and returns the embedUrl of the best car model.
     *
     * FIXED: Uses "embedUrl" directly from the JSON (Sketchfab guarantees these
     * are live and embeddable). No more constructing URLs from UIDs.
     *
     * Each result block looks like:
     * { "uid":"abc", "name":"BMW 3 Series",
     * "embedUrl":"https://sketchfab.com/models/abc/embed", ... }
     */
    private String pickBestEmbedUrl(String json, String brand, String model) {
        String bl = brand.toLowerCase().trim();
        String ml = model.toLowerCase().trim();

        String bestEmbedUrl = null;
        int bestScore = 50; // minimum threshold to avoid junk

        int pos = 0;
        while (true) {
            // Find the next "embedUrl" field in the JSON
            int ei = json.indexOf("\"embedUrl\"", pos);
            if (ei < 0)
                break;

            // Extract the URL value
            int evs = json.indexOf("\"", ei + 11) + 1;
            int eve = json.indexOf("\"", evs);
            if (evs <= 0 || eve <= evs) {
                pos = ei + 10;
                continue;
            }
            String embedUrl = json.substring(evs, eve);

            // Must be a real Sketchfab embed URL (skip nulls / empty)
            if (!embedUrl.contains("sketchfab.com")) {
                pos = eve;
                continue;
            }

            // Find the "name" field in the surrounding ~800 chars (search backwards +
            // forwards)
            int searchStart = Math.max(0, ei - 800);
            int searchEnd = Math.min(json.length(), ei + 400);
            String block = json.substring(searchStart, searchEnd);
            String name = "";
            int ni = block.lastIndexOf("\"name\"");
            if (ni >= 0) {
                int ns = block.indexOf("\"", ni + 7) + 1;
                int ne = block.indexOf("\"", ns);
                if (ns > 0 && ne > ns)
                    name = block.substring(ns, ne).toLowerCase();
            }

            // Find likeCount in same block
            int likes = 0;
            int lci = block.lastIndexOf("\"likeCount\"");
            if (lci >= 0) {
                int ls = block.indexOf(":", lci) + 1;
                int le = block.indexOf(",", ls);
                if (ls > 0 && le > ls) {
                    try {
                        likes = Integer.parseInt(block.substring(ls, le).trim());
                    } catch (Exception ignored) {
                    }
                }
            }

            // Check isPublished (skip unpublished models)
            if (block.contains("\"isPublished\":false")) {
                pos = eve;
                continue;
            }

            // Score
            int score = Math.min(likes / 20, 150); // popularity (capped)
            if (name.contains(bl))
                score += 700;
            if (name.contains(ml))
                score += 1000;
            if (name.contains("car") || name.contains("vehicle") || name.contains("auto"))
                score += 80;
            // Penalise non-exterior models
            if (name.contains("interior") && !name.contains(bl))
                score -= 500;
            if (name.contains("wheel") || name.contains("rim"))
                score -= 800;
            if (name.contains("engine") || name.contains("motor"))
                score -= 700;
            if (name.contains("logo") || name.contains("emblem"))
                score -= 900;
            if (name.contains("seat") || name.contains("dash"))
                score -= 700;
            if (name.contains("part") || name.contains("component"))
                score -= 500;

            System.out.println("[3D]   name='" + name + "' likes=" + likes
                    + " score=" + score + " url=" + embedUrl);

            if (score > bestScore) {
                bestScore = score;
                bestEmbedUrl = embedUrl;
            }
            pos = eve;
        }

        if (bestEmbedUrl != null)
            System.out.println("[3D] Best embed URL (score=" + bestScore + "): " + bestEmbedUrl);
        return bestEmbedUrl;
    }

    /** Map colour name → CSS hex for the Three.js fallback initial colour */
    private String parseCarColorHex(String s) {
        if (s == null || s.isEmpty())
            return "#a8acb2";
        String c = s.toLowerCase();
        if (c.contains("red") || c.contains("crimson") || c.contains("scarlet") || c.contains("maroon")
                || c.contains("ruby"))
            return "#c81616";
        if (c.contains("blue") || c.contains("navy") || c.contains("sapphire") || c.contains("cobalt")
                || c.contains("azure"))
            return "#1460c8";
        if (c.contains("green") || c.contains("emerald") || c.contains("forest") || c.contains("olive"))
            return "#149b41";
        if (c.contains("yellow") || c.contains("gold") || c.contains("amber") || c.contains("champagne"))
            return "#d2a50a";
        if (c.contains("orange") || c.contains("sunset") || c.contains("coral"))
            return "#dc4b0a";
        if (c.contains("purple") || c.contains("violet") || c.contains("mauve") || c.contains("lavender"))
            return "#6e19d2";
        if (c.contains("pink") || c.contains("rose") || c.contains("fuchsia") || c.contains("magenta"))
            return "#d46080";
        if (c.contains("white") || c.contains("pearl") || c.contains("ivory") || c.contains("polar"))
            return "#f0f0f4";
        if (c.contains("silver") || c.contains("quartz") || c.contains("chrome"))
            return "#a8acb2";
        if (c.contains("grey") || c.contains("gray") || c.contains("graphite") || c.contains("space")
                || c.contains("steel"))
            return "#5a5c66";
        if (c.contains("black") || c.contains("midnight") || c.contains("obsidian") || c.contains("onyx")
                || c.contains("abyss"))
            return "#0a0a12";
        if (c.contains("brown") || c.contains("bronze") || c.contains("copper") || c.contains("tan"))
            return "#9c5a14";
        if (c.contains("beige") || c.contains("sand") || c.contains("aurora"))
            return "#d2c8a0";
        if (c.contains("teal") || c.contains("turquoise") || c.contains("cyan"))
            return "#1a9b9b";
        return "#a8acb2";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build the complete HTML configurator page.
    // sketchfabUid: if non-null → embed Sketchfab iframe (real car model)
    // if null → render Three.js procedural geometry
    // ─────────────────────────────────────────────────────────────────────────
    private String build3DConfiguratorHTML(Car car, String featJson, String sketchfabUid) {
        String brand = car.getBrand() != null ? car.getBrand().replace("\"", "'") : "Unknown";
        String model = car.getModel() != null ? car.getModel().replace("\"", "'") : "Unknown";
        String category = car.getCategory() != null ? car.getCategory() : "Sedan";
        String fuel = car.getFuelType() != null ? car.getFuelType() : "—";
        String trans = car.getTransmission() != null ? car.getTransmission() : "—";
        String engine = car.getEngineSize() > 0 ? car.getEngineSize() + "L" : "—";
        String colorName = car.getColor() != null ? car.getColor() : "Silver";
        String initHex = parseCarColorHex(colorName);
        long price = (long) car.getPrice();
        int stock = car.getStock();
        int year = car.getYear();
        String catLow = category.toLowerCase();

        // ── KEY FIX: sketchfabUid is now the FULL embed URL from Sketchfab's JSON.
        // We no longer construct the URL from a UID — that caused 404s when
        // the model was private, deleted, or the UID was misread.
        boolean hasSF = (sketchfabUid != null && !sketchfabUid.isEmpty()
                && sketchfabUid.contains("sketchfab.com"));

        // sfEmbed: add display params to the ready-made URL
        String sfEmbed = "";
        String sfUid = ""; // extracted UID for Viewer API
        if (hasSF) {
            String base = sketchfabUid.contains("?")
                    ? sketchfabUid + "&"
                    : sketchfabUid + "?";
            sfEmbed = base + "autostart=1&ui_controls=1&ui_infos=0"
                    + "&ui_watermark=1&ui_ar=0&ui_vr=0&preload=1&ui_theme=dark";
            // Extract the UID segment for the Viewer API init
            int mi = sketchfabUid.indexOf("/models/");
            if (mi >= 0) {
                int s = mi + 8;
                int e = sketchfabUid.indexOf("/", s);
                sfUid = (e > s) ? sketchfabUid.substring(s, e) : sketchfabUid.substring(s);
                if (sfUid.contains("?"))
                    sfUid = sfUid.substring(0, sfUid.indexOf("?"));
            }
        }

        StringBuilder h = new StringBuilder(65536);

        h.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
                .append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
                .append("<title>AutoElite 3D — ").append(year).append(" ").append(brand).append(" ").append(model)
                .append("</title>\n");

        // Three.js only needed for fallback
        if (!hasSF) {
            h.append(
                    "<script type=\"importmap\">{\"imports\":{\"three\":\"https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js\",\"three/addons/\":\"https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/\"}}</script>\n");
        }

        h.append("<style>\n")
                .append("*{margin:0;padding:0;box-sizing:border-box}\n")
                .append(":root{--bg:#08080e;--side:#090c1a;--cyan:#00d4ff;--purple:#8a2be2;--green:#39ff14;--text:#e6f0ff;--muted:#6478a0;--border:rgba(0,212,255,.18)}\n")
                .append("html,body{width:100%;height:100%;overflow:hidden;background:var(--bg);font-family:'Segoe UI',system-ui,sans-serif;color:var(--text)}\n")
                .append("#root{display:flex;flex-direction:column;height:100vh}\n")
                // Topbar
                .append("#topbar{height:62px;flex-shrink:0;display:flex;align-items:center;justify-content:space-between;padding:0 22px;background:linear-gradient(90deg,#1a0038,#06030f);border-bottom:1px solid rgba(140,50,220,.4)}\n")
                .append(".logo{display:flex;align-items:center;gap:12px}\n")
                .append(".hex{width:38px;height:38px;background:linear-gradient(135deg,var(--cyan),var(--purple));clip-path:polygon(50% 0%,100% 25%,100% 75%,50% 100%,0% 75%,0% 25%);display:flex;align-items:center;justify-content:center;font-weight:900;font-size:12px;color:#000}\n")
                .append(".title{font-size:16px;font-weight:800;color:var(--cyan)}\n")
                .append(".subtitle{font-size:10px;color:rgba(190,120,255,.85);margin-top:2px}\n")
                .append(".acts{display:flex;gap:8px}\n")
                // Layout
                .append("#main{display:flex;flex:1;overflow:hidden}\n")
                // Left panel
                .append("#left{width:274px;min-width:240px;background:var(--side);border-right:1px solid rgba(50,20,90,.8);overflow-y:auto;flex-shrink:0}\n")
                .append("#left::-webkit-scrollbar{width:4px}#left::-webkit-scrollbar-thumb{background:rgba(0,212,255,.3);border-radius:2px}\n")
                // Canvas
                .append("#canvas-wrap{flex:1;position:relative;overflow:hidden;background:#050509}\n")
                .append("#sf-frame{width:100%;height:100%;border:none;display:").append(hasSF ? "block" : "none")
                .append("}\n")
                .append("#three-wrap{position:absolute;inset:0;display:").append(hasSF ? "none" : "block").append("}\n")
                .append("#three-wrap canvas{width:100%!important;height:100%!important}\n")
                // Loading (only shown for Three.js init)
                .append("#loading{position:absolute;inset:0;display:").append(hasSF ? "none" : "flex")
                .append(";flex-direction:column;align-items:center;justify-content:center;z-index:20;background:var(--bg);gap:14px}\n")
                .append(".sp{width:54px;height:54px;border:3px solid rgba(0,212,255,.15);border-top-color:var(--cyan);border-radius:50%;animation:spin .8s linear infinite}\n")
                .append("@keyframes spin{to{transform:rotate(360deg)}}\n")
                .append(".prog{color:var(--cyan);font-size:20px;font-weight:800}\n")
                // HUD / badge
                .append("#hud{position:absolute;top:11px;left:13px;font-size:10px;color:rgba(0,212,255,.45);pointer-events:none;z-index:5;line-height:1.7}\n")
                .append("#badge{position:absolute;top:11px;right:13px;padding:4px 12px;font-size:10px;font-weight:700;border-radius:20px;background:rgba(0,0,0,.7);border:1px solid var(--border);color:")
                .append(hasSF ? "var(--cyan)" : "var(--muted)")
                .append(";z-index:5;pointer-events:none;max-width:360px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}\n")
                // View buttons
                .append("#view-btns{position:absolute;bottom:12px;left:50%;transform:translateX(-50%);display:flex;gap:6px;z-index:5}\n")
                .append("#view-btns button{padding:6px 15px;font-size:11px;font-weight:700;border:1px solid var(--border);background:rgba(8,4,18,.88);color:var(--muted);border-radius:20px;cursor:pointer;backdrop-filter:blur(8px);transition:all .2s}\n")
                .append("#view-btns button:hover,#view-btns button.on{color:var(--cyan);border-color:var(--cyan);background:rgba(0,212,255,.09)}\n")
                // Right panel
                .append("#right{width:256px;min-width:220px;background:var(--side);border-left:1px solid rgba(50,20,90,.8);overflow-y:auto;flex-shrink:0;padding:16px 14px}\n")
                .append("#right::-webkit-scrollbar{width:4px}#right::-webkit-scrollbar-thumb{background:rgba(0,212,255,.3);border-radius:2px}\n")
                // UI components
                .append(".sec{font-size:9px;font-weight:900;letter-spacing:2px;color:var(--muted);margin:18px 0 8px;padding:0 12px;text-transform:uppercase;display:flex;align-items:center;gap:8px}\n")
                .append(".sec::after{content:'';flex:1;height:1px;background:rgba(50,20,90,.7)}\n")
                .append(".sec:first-child{margin-top:10px}\n")
                .append(".sg{display:grid;grid-template-columns:repeat(6,1fr);gap:5px;padding:0 10px}\n")
                .append(".sw{width:100%;aspect-ratio:1;border-radius:50%;cursor:pointer;border:2px solid transparent;transition:transform .15s,border-color .15s;outline:none}\n")
                .append(".sw:hover{transform:scale(1.18)}.sw.on{border-color:#fff!important;box-shadow:0 0 0 3px rgba(255,255,255,.2)}\n")
                .append(".slbl{text-align:center;margin:6px 0 3px;font-size:11px;color:var(--muted);padding:0 10px}\n")
                .append(".cc{display:flex;align-items:center;gap:8px;padding:5px 10px}\n")
                .append(".cc input[type=color]{width:32px;height:32px;border:2px solid rgba(100,130,200,.5);border-radius:7px;padding:2px;background:#e8ecf8;cursor:pointer}\n")
                .append(".cc span{font-size:11px;color:var(--muted)}\n")
                .append(".opt{display:flex;align-items:flex-start;gap:9px;padding:6px 10px;cursor:pointer;border-radius:6px;margin:2px 5px;transition:background .15s}\n")
                .append(".opt:hover{background:rgba(0,212,255,.05)}.opt.on{background:rgba(0,212,255,.09);border:1px solid rgba(0,212,255,.18)}\n")
                .append(".rb{width:14px;height:14px;border-radius:50%;border:2px solid var(--muted);flex-shrink:0;margin-top:2px;position:relative;transition:border-color .15s}\n")
                .append(".opt.on .rb{border-color:var(--cyan)}.opt.on .rb::after{content:'';position:absolute;inset:3px;border-radius:50%;background:var(--cyan)}\n")
                .append(".nm{font-size:11px;font-weight:700;color:var(--text)}.ds{font-size:10px;color:var(--muted);margin-top:1px}.pr{font-size:10px;color:var(--cyan);margin-top:1px;font-weight:700}\n")
                .append(".sld{display:flex;align-items:center;gap:8px;padding:4px 10px}\n")
                .append(".sld label{font-size:10px;color:var(--muted);width:68px;flex-shrink:0}\n")
                .append(".sld input[type=range]{flex:1;accent-color:var(--cyan)}\n")
                .append(".sld .v{font-size:11px;color:var(--cyan);width:34px;text-align:right;font-weight:700}\n")
                .append(".sf-note{margin:8px 10px;padding:8px 12px;background:rgba(0,212,255,.06);border:1px solid rgba(0,212,255,.2);border-radius:8px;font-size:10px;color:var(--muted);line-height:1.6}\n")
                .append(".sf-note b{color:var(--cyan)}\n")
                // Right panel
                .append(".row2{display:flex;justify-content:space-between;padding:5px 0;border-bottom:1px solid rgba(30,15,55,.7)}\n")
                .append(".rk{font-size:10px;color:var(--muted)}.rv{font-size:11px;font-weight:700;color:var(--text)}\n")
                .append(".rv.g{color:var(--green);font-size:13px}.rv.c{color:var(--cyan)}\n")
                .append(".rt{font-size:12px;font-weight:800;color:#b06aff;margin:14px 0 8px}\n")
                .append(".fp{padding:3px 9px;border-radius:20px;font-size:9px;font-weight:700;border:1px solid rgba(180,100,255,.35);color:rgba(180,130,255,.85);background:rgba(130,50,200,.1)}\n")
                .append(".fw{padding:0 2px;display:flex;flex-wrap:wrap;gap:5px;margin-bottom:6px}\n")
                // Buttons
                .append(".btn{display:inline-flex;align-items:center;gap:5px;padding:6px 15px;border-radius:7px;font-size:11px;font-weight:700;cursor:pointer;border:none;transition:all .2s}\n")
                .append(".bc{background:rgba(0,212,255,.1);color:var(--cyan);border:1px solid rgba(0,212,255,.35)}.bc:hover{background:var(--cyan);color:#000}\n")
                .append(".bp{background:rgba(140,50,220,.12);color:#b06aff;border:1px solid rgba(140,50,220,.35)}.bp:hover{background:rgba(140,50,220,.3)}\n")
                .append(".bg{background:rgba(57,255,20,.1);color:var(--green);border:1px solid rgba(57,255,20,.3)}.bg:hover{background:var(--green);color:#000}\n")
                // Summary bar
                .append("#sumbar{height:50px;flex-shrink:0;display:flex;align-items:center;justify-content:space-between;padding:0 20px;background:rgba(6,3,14,.98);border-top:1px solid rgba(50,20,90,.7);gap:10px}\n")
                .append(".si{font-size:9px;color:var(--muted);display:flex;flex-direction:column;gap:1px}\n")
                .append(".si strong{font-size:11px;color:var(--text)}\n")
                .append(".sa{display:flex;gap:8px}\n")
                .append("</style></head><body>\n")
                .append("<div id=\"root\">\n")

                // ── TOPBAR ────────────────────────────────────────────────────────
                .append("<div id=\"topbar\">\n")
                .append("  <div class=\"logo\">\n")
                .append("    <div class=\"hex\">AE</div>\n")
                .append("    <div><div class=\"title\">🎨 AutoElite 3D Configurator</div>\n")
                .append("    <div class=\"subtitle\">").append(year).append(" ").append(brand).append(" ").append(model)
                .append(" &middot; ").append(category).append("</div></div>\n")
                .append("  </div>\n")
                .append("  <div class=\"acts\">\n")
                .append("    <button class=\"btn bp\" onclick=\"location.reload()\">↺ Reset</button>\n")
                .append("    <button class=\"btn bc\" id=\"rotBtn\" onclick=\"toggleRot()\">▶ Rotate</button>\n")
                .append("    <button class=\"btn bg\" onclick=\"doShot()\">📷 Screenshot</button>\n")
                .append("  </div>\n")
                .append("</div>\n")

                // ── MAIN ──────────────────────────────────────────────────────────
                .append("<div id=\"main\">\n")

                // ── LEFT PANEL ────────────────────────────────────────────────────
                .append("<div id=\"left\">\n")
                .append("  <div class=\"sec\">🎨 Body Colour</div>\n")
                .append("  <div class=\"sg\" id=\"swG\"></div>\n")
                .append("  <div class=\"slbl\" id=\"clrLbl\">").append(colorName).append("</div>\n")
                .append("  <div class=\"cc\"><input type=\"color\" id=\"cClr\" value=\"").append(initHex)
                .append("\"><span>Custom colour</span></div>\n");

        if (hasSF) {
            h.append(
                    "  <div class=\"sf-note\"><b>✅ Real Sketchfab Model</b><br>Use the colour swatches to apply paint.<br>Drag / scroll in the 3D view to explore.</div>\n");
        }

        h.append("  <div class=\"sec\">✨ Paint Finish</div>\n")
                .append("  <div id=\"finOpts\"></div>\n");

        if (!hasSF) {
            h.append("  <div class=\"sec\">⚙ Material Sliders</div>\n")
                    .append("  <div class=\"sld\"><label>Metallic</label><input type=\"range\" id=\"sl-m\" min=\"0\" max=\"100\" value=\"82\" oninput=\"updMat()\"><span class=\"v\" id=\"v-m\">0.82</span></div>\n")
                    .append("  <div class=\"sld\"><label>Roughness</label><input type=\"range\" id=\"sl-r\" min=\"0\" max=\"100\" value=\"12\" oninput=\"updMat()\"><span class=\"v\" id=\"v-r\">0.12</span></div>\n")
                    .append("  <div class=\"sld\"><label>Clearcoat</label><input type=\"range\" id=\"sl-c\" min=\"0\" max=\"100\" value=\"92\" oninput=\"updMat()\"><span class=\"v\" id=\"v-c\">0.92</span></div>\n");
        }

        h.append("  <div class=\"sec\">🔧 Wheel Style</div>\n")
                .append("  <div id=\"whlOpts\"></div>\n")
                .append("  <div class=\"sec\">⬤ Rim Colour</div>\n")
                .append("  <div id=\"rimOpts\"></div>\n")
                .append("  <div class=\"sec\">🔴 Brake Calipers</div>\n")
                .append("  <div id=\"calOpts\"></div>\n")
                .append("  <div class=\"sec\">🏠 Roof Style</div>\n")
                .append("  <div id=\"roofOpts\"></div>\n")
                .append("  <div class=\"sec\">🪑 Interior</div>\n")
                .append("  <div id=\"intOpts\"></div>\n")
                .append("  <div class=\"sec\">🪟 Window Tint</div>\n")
                .append("  <div class=\"sld\"><label>Darkness</label><input type=\"range\" id=\"sl-t\" min=\"5\" max=\"85\" value=\"32\" oninput=\"updTint()\"><span class=\"v\" id=\"v-t\">32%</span></div>\n")
                .append("  <div id=\"tintOpts\"></div>\n")
                .append("  <div class=\"sec\">💡 Lighting</div>\n")
                .append("  <div id=\"lghtOpts\"></div>\n")
                .append("  <div style=\"height:18px\"></div>\n")
                .append("</div>\n")

                // ── 3D CANVAS ─────────────────────────────────────────────────────
                .append("<div id=\"canvas-wrap\">\n")
                .append("  <div id=\"hud\">").append(year).append(" ").append(brand).append(" ").append(model)
                .append(" &middot; ").append(category).append("<br>");

        if (hasSF) {
            h.append("Drag to orbit &middot; Scroll to zoom &middot; Real 3D model from Sketchfab");
        } else {
            h.append("Drag to orbit &middot; Scroll to zoom &middot; 3D Preview mode");
        }
        h.append("</div>\n")
                .append("  <div id=\"badge\">")
                .append(hasSF ? "✅ Real model: " + brand + " " + model : "🔷 3D Preview mode (offline)")
                .append("</div>\n");

        if (hasSF) {
            h.append("  <iframe id=\"sf-frame\" src=\"").append(sfEmbed).append("\"\n")
                    .append("    allow=\"autoplay; fullscreen; xr-spatial-tracking\" allowfullscreen></iframe>\n");
        }

        h.append("  <div id=\"three-wrap\"></div>\n")
                .append("  <div id=\"loading\">\n")
                .append("    <div class=\"sp\"></div>\n")
                .append("    <div class=\"prog\">Building 3D model…</div>\n")
                .append("  </div>\n")
                .append("  <div id=\"view-btns\">\n")
                .append("    <button class=\"on\" onclick=\"setView('reset')\">Default</button>\n")
                .append("    <button onclick=\"setView('front')\">Front</button>\n")
                .append("    <button onclick=\"setView('side')\">Side</button>\n")
                .append("    <button onclick=\"setView('rear')\">Rear</button>\n")
                .append("    <button onclick=\"setView('top')\">Top</button>\n")
                .append("  </div>\n")
                .append("</div>\n")

                // ── RIGHT PANEL ───────────────────────────────────────────────────
                .append("<div id=\"right\">\n")
                .append("  <div class=\"rt\">Vehicle Specs</div><div id=\"specTbl\"></div>\n")
                .append("  <div class=\"rt\">Your Configuration</div><div id=\"cfgTbl\"></div>\n")
                .append("  <div class=\"rt\">Features</div><div class=\"fw\" id=\"featList\"></div>\n")
                .append("  <div class=\"rt\">Price Breakdown</div><div id=\"priceTbl\"></div>\n")
                .append("</div>\n")
                .append("</div><!-- #main -->\n")

                // ── SUMMARY BAR ───────────────────────────────────────────────────
                .append("<div id=\"sumbar\">\n")
                .append("  <div class=\"si\"><span>Vehicle</span><strong>").append(brand).append(" ").append(model)
                .append("</strong></div>\n")
                .append("  <div class=\"si\"><span>Paint</span><strong id=\"s-paint\">").append(colorName)
                .append("</strong></div>\n")
                .append("  <div class=\"si\"><span>Wheels</span><strong id=\"s-whl\">Sport 5-Spoke</strong></div>\n")
                .append("  <div class=\"si\"><span>Interior</span><strong id=\"s-int\">Black Leather</strong></div>\n")
                .append("  <div class=\"si\"><span>Base</span><strong>$").append(String.format("%,d", price))
                .append("</strong></div>\n")
                .append("  <div class=\"si\"><span>Total</span><strong id=\"s-total\" style=\"color:var(--green);font-size:14px\">$")
                .append(String.format("%,d", price)).append("</strong></div>\n")
                .append("  <div class=\"sa\"><button class=\"btn bg\" onclick=\"window.print()\">🖨 Print</button></div>\n")
                .append("</div>\n")
                .append("</div><!-- #root -->\n\n");

        // ══════════════════════════════════════════════════════════════════════
        // JAVASCRIPT
        // ══════════════════════════════════════════════════════════════════════
        h.append("<script").append(hasSF ? "" : " type=\"module\"").append(">\n");

        if (!hasSF) {
            h.append("import * as THREE from 'three';\n")
                    .append("import { OrbitControls } from 'three/addons/controls/OrbitControls.js';\n")
                    .append("import { RGBELoader }    from 'three/addons/loaders/RGBELoader.js';\n\n");
        }

        // Car data
        h.append("const CAR={brand:'").append(brand).append("',model:'").append(model).append("',year:").append(year)
                .append(",category:'").append(catLow).append("',fuel:'").append(fuel).append("',trans:'").append(trans)
                .append("',engine:'").append(engine).append("',color:'").append(colorName)
                .append("',price:").append(price).append(",stock:").append(stock).append(",features:").append(featJson)
                .append("};\n\n");

        // Prices
        h.append("const PRICES={finish:{Metallic:1500,Gloss:800,Matte:2200,'Satin Pearl':2800,'Carbon Fibre':4500},")
                .append("wheel:{'Standard Steel':0,'Sport 5-Spoke':1800,'Multi-Spoke Forged':3500,'Off-Road Rugged':2200,'Twin-Spoke Luxury':2600,'Split 10-Spoke':3200,'Turbine Mono':4100},")
                .append("rim:{'Gloss Silver':0,'Matte Black':420,'Gunmetal':640,'Gold':960,'Chrome':1150,'Bronze':780},")
                .append("cal:{Red:500,Yellow:500,Blue:500,Black:0,Silver:200,Orange:500},")
                .append("roof:{Standard:0,Panoramic:3200,Convertible:8500},")
                .append("int:{'Black Leather':0,'Cream Leather':1200,'Red Sport':2400,'Two-Tone':1800,'Alcantara':3800,'Carbon Sport':5200},")
                .append("tint:{Clear:0,'Dark Smoke':300,'Blue Tint':320,Privacy:520}};\n\n");

        // State
        h.append("const S={bodyHex:'").append(initHex).append("',bodyName:'").append(colorName)
                .append("',finish:'Metallic',wheel:'Sport 5-Spoke',rim:'Gloss Silver',cal:'Red',")
                .append("roof:'Standard',int:'Black Leather',tint:.32,tintClr:'Dark Smoke',light:'Studio'};\n\n");

        h.append(
                "function total(){return CAR.price+(PRICES.finish[S.finish]||0)+(PRICES.wheel[S.wheel]||0)+(PRICES.rim[S.rim]||0)+(PRICES.cal[S.cal]||0)+(PRICES.roof[S.roof]||0)+(PRICES.int[S.int]||0)+(PRICES.tint[S.tintClr]||0);}\n")
                .append("function fmt(n){return '$'+n.toLocaleString('en-US');}\n\n");

        if (hasSF) {
            // ── Sketchfab mode: colour via Viewer API ─────────────────────
            h.append("// ── Sketchfab Viewer API for live colour changes ──\n")
                    .append("const SF_UID='").append(sfUid).append("';\n")
                    .append("let sfApi=null,sfBodyMats=[];\n\n")
                    .append("// Load Sketchfab API script then init\n")
                    .append("(function(){\n")
                    .append("  const s=document.createElement('script');\n")
                    .append("  s.src='https://static.sketchfab.com/api/sketchfab-viewer-1.12.1.js';\n")
                    .append("  s.onload=initSFApi;\n")
                    .append("  document.head.appendChild(s);\n")
                    .append("})();\n\n")
                    .append("function initSFApi(){\n")
                    .append("  if(typeof Sketchfab==='undefined') return;\n")
                    .append("  const iframe=document.getElementById('sf-frame');\n")
                    .append("  // Re-initialise the same iframe with the API\n")
                    .append("  const client=new Sketchfab(iframe);\n")
                    .append("  client.init(SF_UID,{\n")
                    .append("    success(api){\n")
                    .append("      sfApi=api;\n")
                    .append("      api.start();\n")
                    .append("      api.addEventListener('viewerready',()=>{\n")
                    .append("        api.getMaterialList((err,mats)=>{\n")
                    .append("          if(err||!mats) return;\n")
                    .append("          // Find body/paint materials by name\n")
                    .append("          sfBodyMats=mats.filter(m=>{\n")
                    .append("            const n=m.name.toLowerCase();\n")
                    .append("            return n.includes('body')||n.includes('paint')||n.includes('exterior')\n")
                    .append("                 ||n.includes('shell')||n.includes('hood')||n.includes('door')\n")
                    .append("                 ||n.includes('fender')||n.includes('bonnet')||n.includes('car_');\n")
                    .append("          });\n")
                    .append("          // Fallback: use first material if nothing matched by name\n")
                    .append("          if(!sfBodyMats.length&&mats.length) sfBodyMats=[mats[0]];\n")
                    .append("          applySFColor(S.bodyHex);\n")
                    .append("        });\n")
                    .append("      });\n")
                    .append("    },\n")
                    .append("    error(){console.warn('SF API init error');},\n")
                    .append("    autostart:1,ui_controls:1,ui_infos:0,ui_watermark:1,preload:1,annotation:0\n")
                    .append("  });\n")
                    .append("}\n\n")
                    .append("function applySFColor(hex){\n")
                    .append("  if(!sfApi||!sfBodyMats.length) return;\n")
                    .append("  const r=parseInt(hex.slice(1,3),16)/255,g=parseInt(hex.slice(3,5),16)/255,b=parseInt(hex.slice(5,7),16)/255;\n")
                    .append("  sfBodyMats.forEach(mat=>{\n")
                    .append("    if(mat.channels.AlbedoPBR)    mat.channels.AlbedoPBR.color=[r,g,b];\n")
                    .append("    if(mat.channels.DiffuseColor)  mat.channels.DiffuseColor.color=[r,g,b];\n")
                    .append("    if(mat.channels.Diffuse)       mat.channels.Diffuse.color=[r,g,b];\n")
                    .append("    sfApi.setMaterial(mat,()=>{});\n")
                    .append("  });\n")
                    .append("}\n\n")
                    .append("function setColor(hex,name,el){\n")
                    .append("  S.bodyHex=hex;S.bodyName=name;\n")
                    .append("  document.getElementById('clrLbl').textContent=name;\n")
                    .append("  document.getElementById('cClr').value=hex;\n")
                    .append("  document.getElementById('s-paint').textContent=name;\n")
                    .append("  document.querySelectorAll('.sw').forEach(s=>s.classList.remove('on'));\n")
                    .append("  if(el) el.classList.add('on');\n")
                    .append("  applySFColor(hex);\n")
                    .append("  updSummary();\n")
                    .append("}\n\n")
                    .append("window.toggleRot=function(){document.getElementById('rotBtn').textContent='▶ Rotate';};\n")
                    .append("window.doShot=function(){alert('Right-click the 3D view → Save image, or use OS screenshot.');};\n")
                    .append("window.setView=function(v){\n")
                    .append("  document.querySelectorAll('#view-btns button').forEach(b=>b.classList.remove('on'));\n")
                    .append("  event.target.classList.add('on');\n")
                    .append("};\n")
                    .append("window.updMat=function(){};\n")
                    .append("window.updTint=function(){const t=+document.getElementById('sl-t').value/100;document.getElementById('v-t').textContent=Math.round(t*100)+'%';S.tint=t;updSummary();};\n\n");
        } else {
            // ── Three.js fallback mode ────────────────────────────────────
            h.append("// ── Three.js fallback renderer ──\n")
                    .append("const three={};\n")
                    .append("let autoRot=false;\n\n")
                    .append("(function initThree(){\n")
                    .append("  const wrap=document.getElementById('three-wrap');\n")
                    .append("  const W=wrap.clientWidth||800,H=wrap.clientHeight||600;\n")
                    .append("  const scene=new THREE.Scene();\n")
                    .append("  const camera=new THREE.PerspectiveCamera(40,W/H,.1,200);\n")
                    .append("  camera.position.set(4.8,1.6,4.8);\n")
                    .append("  const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});\n")
                    .append("  renderer.setPixelRatio(Math.min(devicePixelRatio,2));\n")
                    .append("  renderer.setSize(W,H);\n")
                    .append("  renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;\n")
                    .append("  renderer.outputColorSpace=THREE.SRGBColorSpace;\n")
                    .append("  renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.05;\n")
                    .append("  wrap.appendChild(renderer.domElement);\n")
                    .append("  const ctrl=new THREE.OrbitControls?new THREE.OrbitControls(camera,renderer.domElement):null;\n")
                    // Use dynamic import for OrbitControls
                    .append("  Object.assign(three,{scene,camera,renderer});\n")
                    .append("  // Floor\n")
                    .append("  const fl=new THREE.Mesh(new THREE.CircleGeometry(10,64),new THREE.MeshStandardMaterial({color:0x080810,metalness:.55,roughness:.45}));\n")
                    .append("  fl.rotation.x=-Math.PI/2;fl.receiveShadow=true;scene.add(fl);\n")
                    .append("  // Lights\n")
                    .append("  scene.add(new THREE.AmbientLight(0xffffff,.25));\n")
                    .append("  const kL=new THREE.DirectionalLight(0xffffff,2.4);kL.position.set(5,9,6);kL.castShadow=true;\n")
                    .append("  kL.shadow.mapSize.set(2048,2048);kL.shadow.camera.left=-8;kL.shadow.camera.right=8;kL.shadow.camera.bottom=-8;kL.shadow.camera.top=8;\n")
                    .append("  scene.add(kL);\n")
                    .append("  scene.add(new THREE.DirectionalLight(0x8866ff,.75).position.set(-5,4,-5)||new THREE.DirectionalLight(0x8866ff,.75));\n")
                    .append("  scene.add(new THREE.HemisphereLight(0x223344,0x060608,.4));\n")
                    .append("  three.kL=kL;\n")
                    .append("  // Materials\n")
                    .append("  const bMat=new THREE.MeshPhysicalMaterial({color:new THREE.Color('").append(initHex)
                    .append("'),metalness:.82,roughness:.12,clearcoat:.92,clearcoatRoughness:.04,envMapIntensity:1.8});\n")
                    .append("  const gMat=new THREE.MeshPhysicalMaterial({color:0x112233,transparent:true,opacity:.32,transmission:.68,metalness:0,roughness:0,side:THREE.DoubleSide});\n")
                    .append("  const rMat=new THREE.MeshStandardMaterial({color:0xbcc4cc,metalness:.95,roughness:.12});\n")
                    .append("  const tMat=new THREE.MeshStandardMaterial({color:0x111111,roughness:.88,metalness:.02});\n")
                    .append("  const trMat=new THREE.MeshStandardMaterial({color:0x111118,metalness:.45,roughness:.5});\n")
                    .append("  const hlM=new THREE.MeshStandardMaterial({color:0xffeedd,emissive:0xffffff,emissiveIntensity:.55});\n")
                    .append("  const tlM=new THREE.MeshStandardMaterial({color:0xff2200,emissive:0xff0000,emissiveIntensity:.45});\n")
                    .append("  const chrM=new THREE.MeshStandardMaterial({color:0xe0e8f0,metalness:.98,roughness:.05});\n")
                    .append("  three.bMat=bMat;three.gMat=gMat;three.rMat=rMat;\n")
                    .append("  // Build category geometry\n")
                    .append("  const grp=new THREE.Group();\n")
                    .append("  function box(w,h,d,x,y,z,m){const mesh=new THREE.Mesh(new THREE.BoxGeometry(w,h,d),m);mesh.position.set(x,y,z);mesh.castShadow=mesh.receiveShadow=true;grp.add(mesh);}\n")
                    .append("  const cat='").append(catLow).append("';\n")
                    .append("  let wPos;\n")
                    .append("  if(cat.includes('suv')||cat.includes('muv')||cat.includes('mpv')){\n")
                    .append("    wPos=[[-1.42,-.05,.98],[-1.42,-.05,-.98],[1.42,-.05,.98],[1.42,-.05,-.98]];\n")
                    .append("    box(4.4,.72,2.0,0,.46,0,bMat);box(3.0,.72,1.94,-.2,1.18,0,bMat);box(2.7,.6,1.84,-.2,1.22,0,gMat);\n")
                    .append("    [.96,-.96].forEach(z=>box(2.5,.07,.07,-.2,1.6,z,trMat));\n")
                    .append("    box(.1,.52,1.7,2.2,.6,0,trMat);box(.1,.62,1.95,-2.2,.56,0,trMat);\n")
                    .append("    [.78,-.78].forEach(z=>{box(.1,.18,.38,2.2,.72,z,hlM);box(.1,.18,.38,-2.2,.68,z,tlM);});\n")
                    .append("  }else if(cat.includes('hatch')||cat.includes('compact')){\n")
                    .append("    wPos=[[-1.18,-.04,.84],[-1.18,-.04,-.84],[1.18,-.04,.84],[1.18,-.04,-.84]];\n")
                    .append("    box(3.5,.5,1.7,0,.36,0,bMat);box(2.4,.5,1.62,.1,.84,0,bMat);box(2.1,.4,1.52,.08,.86,0,gMat);\n")
                    .append("    box(.22,.4,1.68,-1.44,.7,0,bMat);box(.52,.18,1.68,1.44,.54,0,bMat);box(.08,.22,1.4,1.74,.36,0,trMat);\n")
                    .append("    [.66,-.66].forEach(z=>{box(.08,.12,.25,1.72,.44,z,hlM);box(.08,.12,.25,-1.72,.62,z,tlM);});\n")
                    .append("  }else if(cat.includes('elec')){\n")
                    .append("    wPos=[[-1.4,-.04,.94],[-1.4,-.04,-.94],[1.4,-.04,.94],[1.4,-.04,-.94]];\n")
                    .append("    box(4.6,.5,1.88,0,.38,0,bMat);box(2.6,.52,1.8,-.1,.88,0,bMat);box(2.4,.44,1.7,-.1,.9,0,gMat);\n")
                    .append("    box(.88,.22,1.82,1.68,.56,0,bMat);\n")
                    .append("    box(.06,.04,1.72,2.2,.6,0,hlM);box(.06,.04,1.72,-2.2,.58,0,tlM);\n")
                    .append("    [-.96,.96].forEach(z=>box(3.9,.06,.08,0,.14,z,trMat));\n")
                    .append("  }else if(cat.includes('luxury')){\n")
                    .append("    wPos=[[-1.65,-.04,.94],[-1.65,-.04,-.94],[1.65,-.04,.94],[1.65,-.04,-.94]];\n")
                    .append("    box(5.0,.52,1.92,0,.38,0,bMat);box(2.5,.52,1.84,-.2,.88,0,bMat);box(2.24,.44,1.74,-.2,.9,0,gMat);\n")
                    .append("    box(1.0,.32,1.88,-1.9,.56,0,bMat);box(.95,.2,1.88,1.9,.54,0,bMat);\n")
                    .append("    [-.98,.98].forEach(z=>box(4.6,.04,.06,0,.64,z,chrM));\n")
                    .append("    box(.1,.42,.9,2.42,.46,0,trMat);\n")
                    .append("    [.76,-.76].forEach(z=>{box(.1,.16,.38,2.4,.54,z,hlM);box(.1,.16,.38,-2.42,.5,z,tlM);});\n")
                    .append("  }else if(cat.includes('sport')||cat.includes('coupe')){\n")
                    .append("    wPos=[[-1.32,-.04,.9],[-1.32,-.04,-.9],[1.32,-.04,.9],[1.32,-.04,-.9]];\n")
                    .append("    box(4.3,.44,1.8,0,.32,0,bMat);box(1.8,.4,1.68,-.1,.76,0,bMat);box(1.62,.32,1.58,-.08,.78,0,gMat);\n")
                    .append("    box(.9,.16,1.78,1.65,.42,0,bMat);box(.6,.22,1.74,-1.6,.48,0,bMat);\n")
                    .append("    box(.12,.16,1.6,-2.05,.7,0,bMat);\n")
                    .append("    [.68,-.68].forEach(z=>{box(.08,.1,.28,2.1,.38,z,hlM);box(.08,.1,.28,-2.1,.44,z,tlM);});\n")
                    .append("    [-.92,.92].forEach(z=>box(3.8,.06,.08,0,.12,z,trMat));\n")
                    .append("  }else{\n")
                    .append("    wPos=[[-1.38,-.05,.92],[-1.38,-.05,-.92],[1.38,-.05,.92],[1.38,-.05,-.92]];\n")
                    .append("    box(4.2,.52,1.82,0,.38,0,bMat);box(2.05,.5,1.72,-.05,.88,0,bMat);box(1.82,.42,1.62,-.05,.9,0,gMat);\n")
                    .append("    box(.78,.32,1.78,-1.62,.58,0,bMat);box(.82,.22,1.78,1.62,.56,0,bMat);\n")
                    .append("    box(.08,.28,1.5,2.06,.38,0,trMat);box(.08,.26,1.78,-2.08,.32,0,trMat);\n")
                    .append("    [-.94,.94].forEach(z=>box(3.6,.1,.12,0,.16,z,trMat));\n")
                    .append("    [-.5,.5].forEach(x=>[-.96,.96].forEach(z=>box(.22,.04,.04,x,.72,z,trMat)));\n")
                    .append("    [.72,-.72].forEach(z=>{box(.1,.14,.3,2.05,.5,z,hlM);box(.1,.14,.3,-2.07,.5,z,tlM);});\n")
                    .append("  }\n")
                    .append("  const wGeo=new THREE.CylinderGeometry(.36,.36,.24,32),rGeo=new THREE.CylinderGeometry(.26,.26,.26,16);\n")
                    .append("  wPos.forEach(p=>{\n")
                    .append("    const ty=new THREE.Mesh(wGeo,tMat);ty.rotation.x=Math.PI/2;ty.position.set(...p);ty.castShadow=true;grp.add(ty);\n")
                    .append("    const ri=new THREE.Mesh(rGeo,rMat);ri.rotation.x=Math.PI/2;ri.position.set(...p);grp.add(ri);\n")
                    .append("  });\n")
                    .append("  three.carGroup=grp;scene.add(grp);\n")
                    .append("  document.getElementById('loading').style.display='none';\n")
                    .append("  // Orbit controls via dynamic import\n")
                    .append("  import('three/addons/controls/OrbitControls.js').then(({OrbitControls})=>{\n")
                    .append("    const ctrl=new OrbitControls(camera,renderer.domElement);\n")
                    .append("    ctrl.enableDamping=true;ctrl.dampingFactor=.06;ctrl.minDistance=2.2;ctrl.maxDistance=16;\n")
                    .append("    ctrl.maxPolarAngle=Math.PI/2+.08;ctrl.target.set(0,.7,0);\n")
                    .append("    three.ctrl=ctrl;\n")
                    .append("    // Environment\n")
                    .append("    import('three/addons/loaders/RGBELoader.js').then(({RGBELoader})=>{\n")
                    .append("      const pmrem=new THREE.PMREMGenerator(renderer);\n")
                    .append("      pmrem.compileEquirectangularShader();\n")
                    .append("      new RGBELoader().load('https://threejs.org/examples/textures/equirectangular/venice_sunset_1k.hdr',\n")
                    .append("        t=>{scene.environment=pmrem.fromEquirectangular(t).texture;t.dispose();},\n")
                    .append("        undefined,()=>{scene.environment=pmrem.fromScene(new THREE.RoomEnvironment(),.04).texture;});\n")
                    .append("    });\n")
                    .append("    (function anim(){requestAnimationFrame(anim);ctrl.update();renderer.render(scene,camera);}());\n")
                    .append("    window.addEventListener('resize',()=>{const w=wrap.clientWidth,h=wrap.clientHeight;camera.aspect=w/h;camera.updateProjectionMatrix();renderer.setSize(w,h);});\n")
                    .append("    window.setView=function(v){\n")
                    .append("      document.querySelectorAll('#view-btns button').forEach(b=>b.classList.remove('on'));\n")
                    .append("      event.target.classList.add('on');\n")
                    .append("      const pos={reset:[4.8,1.6,4.8],front:[.02,1.3,5.5],side:[5,1.5,.02],rear:[.02,1.3,-5.5],top:[.02,9,.02]};\n")
                    .append("      const p=pos[v]||pos.reset;\n")
                    .append("      const s0=camera.position.clone(),t0=ctrl.target.clone();\n")
                    .append("      const ep=new THREE.Vector3(...p),et=new THREE.Vector3(...(v==='top'?[0,0,0]:[0,.7,0]));\n")
                    .append("      let tt=0;const id=setInterval(()=>{tt+=.04;if(tt>=1){tt=1;clearInterval(id);}const k=1-(1-tt)**3;camera.position.lerpVectors(s0,ep,k);ctrl.target.lerpVectors(t0,et,k);ctrl.update();},16);\n")
                    .append("    };\n")
                    .append("  });\n")
                    .append("})();\n\n")
                    .append("function setColor(hex,name,el){\n")
                    .append("  S.bodyHex=hex;S.bodyName=name;\n")
                    .append("  document.getElementById('clrLbl').textContent=name;\n")
                    .append("  document.getElementById('cClr').value=hex;\n")
                    .append("  document.getElementById('s-paint').textContent=name;\n")
                    .append("  document.querySelectorAll('.sw').forEach(s=>s.classList.remove('on'));\n")
                    .append("  if(el) el.classList.add('on');\n")
                    .append("  if(three.bMat){three.bMat.color.set(hex);three.bMat.needsUpdate=true;}\n")
                    .append("  updSummary();\n")
                    .append("}\n\n")
                    .append("window.updMat=function(){\n")
                    .append("  if(!three.bMat) return;\n")
                    .append("  const m=+document.getElementById('sl-m').value/100,r=+document.getElementById('sl-r').value/100,c=+document.getElementById('sl-c').value/100;\n")
                    .append("  document.getElementById('v-m').textContent=m.toFixed(2);document.getElementById('v-r').textContent=r.toFixed(2);document.getElementById('v-c').textContent=c.toFixed(2);\n")
                    .append("  three.bMat.metalness=m;three.bMat.roughness=r;three.bMat.clearcoat=c;three.bMat.needsUpdate=true;\n")
                    .append("};\n")
                    .append("window.updTint=function(){\n")
                    .append("  const t=+document.getElementById('sl-t').value/100;\n")
                    .append("  document.getElementById('v-t').textContent=Math.round(t*100)+'%';S.tint=t;\n")
                    .append("  if(three.gMat){const cs={Clear:0x334455,'Dark Smoke':0x080810,'Blue Tint':0x102030,Privacy:0x050505};three.gMat.opacity=t;three.gMat.color.set(cs[S.tintClr]||0x112233);three.gMat.needsUpdate=true;}\n")
                    .append("  updSummary();\n")
                    .append("};\n")
                    .append("window.toggleRot=function(){\n")
                    .append("  autoRot=!autoRot;if(three.ctrl)three.ctrl.autoRotate=autoRot;\n")
                    .append("  document.getElementById('rotBtn').textContent=autoRot?'⏸ Stop':'▶ Rotate';\n")
                    .append("};\n")
                    .append("window.doShot=function(){\n")
                    .append("  if(!three.renderer) return;\n")
                    .append("  three.renderer.render(three.scene,three.camera);\n")
                    .append("  const a=document.createElement('a');a.href=three.renderer.domElement.toDataURL('image/png');\n")
                    .append("  a.download=`${CAR.brand}_${CAR.model}_config.png`;a.click();\n")
                    .append("};\n")
                    .append("window.setView=function(v){\n")
                    .append("  document.querySelectorAll('#view-btns button').forEach(b=>b.classList.remove('on'));event.target.classList.add('on');\n")
                    .append("};\n\n");
        }

        // ── Shared: colour swatches, option builders, summary ─────────────
        h.append("const COLORS=[\n")
                .append("  {n:'Midnight Black',h:'#0a0a12'},{n:'Graphite',h:'#3a3c41'},\n")
                .append("  {n:'Arctic White',h:'#f0f0f4'},{n:'Pearl White',h:'#e8e8ec'},\n")
                .append("  {n:'Silver',h:'#a8acb2'},{n:'Space Grey',h:'#5a5c66'},\n")
                .append("  {n:'Crimson Red',h:'#c81616'},{n:'Sunset Orange',h:'#dc4b0a'},\n")
                .append("  {n:'Cobalt Blue',h:'#1460c8'},{n:'Sapphire Blue',h:'#145ab4'},\n")
                .append("  {n:'Emerald Green',h:'#149b41'},{n:'Forest Green',h:'#1a4a28'},\n")
                .append("  {n:'Royal Purple',h:'#6e1992'},{n:'Ultraviolet',h:'#6e19d2'},\n")
                .append("  {n:'Champagne',h:'#d2a50a'},{n:'Bronze',h:'#9c5a14'},\n")
                .append("  {n:'Rose Gold',h:'#d46080'},{n:'Teal',h:'#1a9b9b'},\n")
                .append("];\n\n")
                .append("function lgtn(h,a){const r=parseInt(h.slice(1,3),16),g=parseInt(h.slice(3,5),16),b=parseInt(h.slice(5,7),16);return`rgb(${Math.min(255,r+a)},${Math.min(255,g+a)},${Math.min(255,b+a)})`}\n")
                .append("function drkn(h,a){const r=parseInt(h.slice(1,3),16),g=parseInt(h.slice(3,5),16),b=parseInt(h.slice(5,7),16);return`rgb(${Math.max(0,r-a)},${Math.max(0,g-a)},${Math.max(0,b-a)})`}\n\n")
                .append("function buildUI(){\n")
                .append("  const sg=document.getElementById('swG');sg.innerHTML='';\n")
                .append("  COLORS.forEach(c=>{\n")
                .append("    const b=document.createElement('button');b.className='sw'+(c.h===S.bodyHex?' on':'');\n")
                .append("    b.style.background=`radial-gradient(circle at 35% 30%,${lgtn(c.h,38)},${c.h} 58%,${drkn(c.h,28)})`;\n")
                .append("    b.title=c.n;b.onclick=()=>setColor(c.h,c.n,b);sg.appendChild(b);\n")
                .append("  });\n")
                .append("  document.getElementById('cClr').value=S.bodyHex;\n")
                .append("  document.getElementById('cClr').addEventListener('input',e=>setColor(e.target.value,'Custom',null));\n")
                .append("  mkO('finOpts',['Metallic','Gloss','Matte','Satin Pearl','Carbon Fibre'],S.finish,v=>{S.finish=v;updSummary();});\n")
                .append("  mkD('whlOpts',[{n:'Standard Steel',d:'Classic hubcap',p:0},{n:'Sport 5-Spoke',d:'5-spoke alloy',p:1800},{n:'Multi-Spoke Forged',d:'19\\\" forged',p:3500},{n:'Off-Road Rugged',d:'Off-road rated',p:2200},{n:'Twin-Spoke Luxury',d:'Premium alloy',p:2600},{n:'Split 10-Spoke',d:'High-performance',p:3200},{n:'Turbine Mono',d:'Turbine disc',p:4100}],S.wheel,v=>{S.wheel=v;updSummary();});\n")
                .append("  mkD('rimOpts',[{n:'Gloss Silver',d:'Polished silver',p:0},{n:'Matte Black',d:'Stealth black',p:420},{n:'Gunmetal',d:'Dark metallic',p:640},{n:'Gold',d:'Brushed gold',p:960},{n:'Chrome',d:'Mirror chrome',p:1150},{n:'Bronze',d:'Warm bronze',p:780}],S.rim,v=>{S.rim=v;updSummary();});\n")
                .append("  mkD('calOpts',[{n:'Red',d:'Sport red',p:500},{n:'Yellow',d:'Racing yellow',p:500},{n:'Blue',d:'Track blue',p:500},{n:'Black',d:'Stealth black',p:0},{n:'Silver',d:'Classic silver',p:200},{n:'Orange',d:'Hot orange',p:500}],S.cal,v=>{S.cal=v;updSummary();});\n")
                .append("  mkD('roofOpts',[{n:'Standard',d:'Solid metal roof',p:0},{n:'Panoramic',d:'Full glass sunroof',p:3200},{n:'Convertible',d:'Electric soft-top',p:8500}],S.roof,v=>{S.roof=v;updSummary();});\n")
                .append("  mkD('intOpts',[{n:'Black Leather',d:'Nappa leather',p:0},{n:'Cream Leather',d:'Ivory Nappa',p:1200},{n:'Red Sport',d:'Red Alcantara',p:2400},{n:'Two-Tone',d:'Black & cream split',p:1800},{n:'Alcantara',d:'Full Alcantara',p:3800},{n:'Carbon Sport',d:'Carbon-fibre trim',p:5200}],S.int,v=>{S.int=v;updSummary();});\n")
                .append("  mkO('tintOpts',['Clear','Dark Smoke','Blue Tint','Privacy'],S.tintClr,v=>{S.tintClr=v;updTint();});\n")
                .append("  mkO('lghtOpts',['Studio','Sunset','Outdoor','Night'],S.light,v=>{S.light=v;});\n")
                .append("  const rows=[['Brand',CAR.brand],['Model',CAR.model],['Year',CAR.year],['Category',CAR.category],['Fuel',CAR.fuel],['Transmission',CAR.trans],['Engine',CAR.engine],['In Stock',CAR.stock+' units'],['Base Price','$'+CAR.price.toLocaleString()]];\n")
                .append("  document.getElementById('specTbl').innerHTML=rows.map(([k,v])=>`<div class=\"row2\"><span class=\"rk\">${k}</span><span class=\"rv ${k==='Base Price'?'g':''}\">${v||'—'}</span></div>`).join('');\n")
                .append("  document.getElementById('featList').innerHTML=CAR.features&&CAR.features.length?CAR.features.map(x=>`<span class=\"fp\">${x}</span>`).join(''):'<span style=\"color:var(--muted);font-size:10px\">No features listed</span>';\n")
                .append("  updSummary();\n")
                .append("}\n\n")
                .append("function mkO(id,opts,active,cb){\n")
                .append("  const el=document.getElementById(id);el.innerHTML='';\n")
                .append("  opts.forEach(o=>{const d=document.createElement('div');d.className='opt'+(o===active?' on':'');\n")
                .append("  d.innerHTML=`<div class=\"rb\"></div><div><div class=\"nm\">${o}</div></div>`;\n")
                .append("  d.onclick=()=>{el.querySelectorAll('.opt').forEach(x=>x.classList.remove('on'));d.classList.add('on');cb(o);};el.appendChild(d);});\n")
                .append("}\n")
                .append("function mkD(id,opts,active,cb){\n")
                .append("  const el=document.getElementById(id);el.innerHTML='';\n")
                .append("  opts.forEach(o=>{const d=document.createElement('div');d.className='opt'+(o.n===active?' on':'');\n")
                .append("  const pr=o.p>0?`<div class=\"pr\">+$${o.p.toLocaleString()}</div>`:`<div class=\"pr\" style=\"color:var(--green)\">Included</div>`;\n")
                .append("  d.innerHTML=`<div class=\"rb\"></div><div><div class=\"nm\">${o.n}</div><div class=\"ds\">${o.d}</div>${pr}</div>`;\n")
                .append("  d.onclick=()=>{el.querySelectorAll('.opt').forEach(x=>x.classList.remove('on'));d.classList.add('on');cb(o.n);};el.appendChild(d);});\n")
                .append("}\n\n")
                .append("function updSummary(){\n")
                .append("  const fp=PRICES.finish[S.finish]||0,wp=PRICES.wheel[S.wheel]||0,rp=PRICES.rim[S.rim]||0;\n")
                .append("  const cp=PRICES.cal[S.cal]||0,rfp=PRICES.roof[S.roof]||0,ip=PRICES.int[S.int]||0,tp=PRICES.tint[S.tintClr]||0;\n")
                .append("  const extras=fp+wp+rp+cp+rfp+ip+tp,tot=CAR.price+extras;\n")
                .append("  const rows=[['Colour',S.bodyName,'$0'],['Finish',S.finish,fmt(fp)],['Wheels',S.wheel,fmt(wp)],['Rim',S.rim,fmt(rp)],['Calipers',S.cal,fmt(cp)],['Roof',S.roof,fmt(rfp)],['Interior',S.int,fmt(ip)],['Tint',S.tintClr,fmt(tp)]];\n")
                .append("  document.getElementById('cfgTbl').innerHTML=rows.map(([k,v,p])=>`<div class=\"row2\"><span class=\"rk\">${k}<br><span style=\"color:var(--text);font-size:10px\">${v}</span></span><span class=\"rv c\">${p}</span></div>`).join('');\n")
                .append("  document.getElementById('priceTbl').innerHTML=`<div class=\"row2\"><span class=\"rk\">Base Vehicle</span><span class=\"rv\">${fmt(CAR.price)}</span></div><div class=\"row2\"><span class=\"rk\">Options & Extras</span><span class=\"rv c\">+${fmt(extras)}</span></div><div class=\"row2\" style=\"border-top:1px solid rgba(57,255,20,.2);margin-top:3px;padding-top:7px\"><span class=\"rk\" style=\"font-weight:800;color:var(--text);font-size:11px\">TOTAL</span><span class=\"rv g\">${fmt(tot)}</span></div>`;\n")
                .append("  document.getElementById('s-whl').textContent=S.wheel;\n")
                .append("  document.getElementById('s-int').textContent=S.int;\n")
                .append("  document.getElementById('s-total').textContent=fmt(tot);\n")
                .append("}\n\n")
                .append("// Kick off\n")
                .append("buildUI();\n")
                .append("</script>\n")
                .append("</body></html>\n");

        return h.toString();
    }

    // =========================================================================
    // ★ FEATURE 1 — PDF EXPORT (ZERO EXTERNAL LIBRARIES)
    //
    // Strategy: Generate a beautifully styled HTML invoice/report and write it
    // to a temp file, then open it in the system browser. The user can then
    // press Ctrl+P → "Save as PDF" with one click — works on ALL platforms
    // (macOS, Windows, Linux) with no extra JARs needed.
    //
    // For Sale Invoices: also offers Java2D print-to-printer (AWT printing API)
    // =========================================================================

    /** Export a Sale Invoice as a styled HTML file and open in browser. */
    private void exportInvoicePDF(String saleId, String vehicle, String customer,
            String employee, String date, String amount,
            String payment, String status) {
        try {
            String invNo = String.format("INV-%05d", Integer.parseInt(saleId.trim()));
            String html = buildInvoiceHTML(invNo, vehicle, customer, employee, date, amount, payment, status);
            openHtmlInBrowser(html, "AutoElite_Invoice_" + invNo + ".html",
                    "📄  Invoice HTML opened in browser — press  Ctrl+P → Save as PDF");
        } catch (Exception ex) {
            showError("Invoice export error: " + ex.getMessage());
        }
    }

    /** Export a Test Drive Confirmation as HTML and open in browser. */
    private void exportTestDrivePDF(int tdId, String vehicle, String customer,
            String date, String timeSlot, String status) {
        try {
            String html = buildTestDriveHTML(tdId, vehicle, customer, date, timeSlot, status);
            openHtmlInBrowser(html, "AutoElite_TestDrive_TD" + tdId + ".html",
                    "📄  Test Drive confirmation opened in browser — press  Ctrl+P → Save as PDF");
        } catch (Exception ex) {
            showError("Test Drive export error: " + ex.getMessage());
        }
    }

    /** Export the full Business Report as HTML and open in browser. */
    private void exportReportPDF() {
        try {
            String html = buildReportHTML();
            openHtmlInBrowser(html, "AutoElite_BusinessReport.html",
                    "📄  Business Report opened in browser — press  Ctrl+P → Save as PDF");
        } catch (Exception ex) {
            showError("Report export error: " + ex.getMessage());
        }
    }

    // ── HTML generation helpers ──────────────────────────────────────────────

    private String buildInvoiceHTML(String invNo, String vehicle, String customer,
            String employee, String date, String amount,
            String payment, String status) {
        // Format amount nicely
        String amtFmt = amount;
        try {
            long val = Long.parseLong(amount.replaceAll("[^0-9]", ""));
            amtFmt = "$" + String.format("%,d", val);
        } catch (Exception ignored) {
        }

        String statusColor = "Completed".equalsIgnoreCase(status) ? "#39ff14"
                : "Cancelled".equalsIgnoreCase(status) ? "#ff4444" : "#00d4ff";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>AutoElite Invoice " + invNo + "</title>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family: 'Segoe UI', Arial, sans-serif; background:#f4f6fb; padding:40px; }"
                + ".page { max-width:680px; margin:0 auto; background:#fff; border-radius:12px; "
                + "        box-shadow:0 4px 24px rgba(0,0,0,.12); overflow:hidden; }"
                + ".header { background:linear-gradient(135deg,#1a0040,#080412); color:#fff; padding:36px 40px; }"
                + ".header h1 { font-size:26px; color:#00d4ff; margin-bottom:4px; }"
                + ".header p  { color:rgba(190,120,255,.85); font-size:13px; }"
                + ".inv-num   { float:right; font-size:22px; font-weight:800; color:#fff; margin-top:-36px; }"
                + ".body { padding:36px 40px; }"
                + ".section-title { font-size:11px; font-weight:700; letter-spacing:2px; "
                + "                 text-transform:uppercase; color:#8a2be2; margin:24px 0 10px; }"
                + "table.info { width:100%; border-collapse:collapse; }"
                + "table.info td { padding:9px 0; font-size:13px; border-bottom:1px solid #eee; }"
                + "table.info td:first-child { color:#888; width:160px; }"
                + "table.info td:last-child  { font-weight:600; color:#111; }"
                + ".total-box { background:linear-gradient(135deg,#f0fff0,#e6fff0); border:2px solid #39ff14; "
                + "             border-radius:10px; padding:20px 28px; margin-top:28px; display:flex; "
                + "             justify-content:space-between; align-items:center; }"
                + ".total-label { font-size:13px; font-weight:700; text-transform:uppercase; color:#555; }"
                + ".total-amt   { font-size:32px; font-weight:900; color:#1a6b1a; }"
                + ".status-pill { display:inline-block; padding:4px 14px; border-radius:20px; "
                + "               font-size:12px; font-weight:700; color:" + statusColor + "; "
                + "               border:1px solid " + statusColor + "; }"
                + ".footer { background:#f9f9f9; border-top:1px solid #eee; padding:20px 40px; "
                + "          text-align:center; font-size:11px; color:#aaa; }"
                + ".print-hint { background:#fff8e1; border:1px solid #ffd54f; border-radius:8px; "
                + "              padding:12px 18px; margin-bottom:20px; font-size:12px; color:#795548; "
                + "              display:flex; align-items:center; gap:10px; }"
                + "@media print { .print-hint { display:none; } body { background:#fff; padding:0; } "
                + "               .page { box-shadow:none; border-radius:0; } }"
                + "</style></head><body>"
                + "<div class='print-hint'>🖨️ <strong>To save as PDF:</strong> Press <kbd>Ctrl+P</kbd> (Win/Linux) or <kbd>⌘+P</kbd> (Mac) → Select <em>\"Save as PDF\"</em> → Click Save</div>"
                + "<div class='page'>"
                + "<div class='header'>"
                + "  <h1>AutoElite Showroom</h1>"
                + "  <p>Official Sale Invoice / Receipt</p>"
                + "  <div class='inv-num'>" + invNo + "</div>"
                + "</div>"
                + "<div class='body'>"
                + "  <div class='section-title'>Transaction Details</div>"
                + "  <table class='info'>"
                + "    <tr><td>Invoice No.</td><td>" + invNo + "</td></tr>"
                + "    <tr><td>Date</td><td>" + date + "</td></tr>"
                + "    <tr><td>Vehicle</td><td>" + esc(vehicle) + "</td></tr>"
                + "    <tr><td>Customer</td><td>" + esc(customer) + "</td></tr>"
                + "    <tr><td>Sales Executive</td><td>" + esc(employee) + "</td></tr>"
                + "    <tr><td>Payment Method</td><td>" + esc(payment) + "</td></tr>"
                + "    <tr><td>Status</td><td><span class='status-pill'>" + esc(status) + "</span></td></tr>"
                + "  </table>"
                + "  <div class='total-box'>"
                + "    <div class='total-label'>Total Amount</div>"
                + "    <div class='total-amt'>" + amtFmt + "</div>"
                + "  </div>"
                + "</div>"
                + "<div class='footer'>AutoElite Premium Showroom &nbsp;|&nbsp; Official Invoice &nbsp;|&nbsp; "
                + "Generated " + java.time.LocalDate.now() + "</div>"
                + "</div></body></html>";
    }

    private String buildTestDriveHTML(int tdId, String vehicle, String customer,
            String date, String timeSlot, String status) {
        String statusColor = "Completed".equalsIgnoreCase(status) ? "#39ff14"
                : "Cancelled".equalsIgnoreCase(status) ? "#ff4444" : "#00d4ff";
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>AutoElite Test Drive #" + tdId + "</title>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family:'Segoe UI',Arial,sans-serif; background:#f4f6fb; padding:40px; }"
                + ".page { max-width:620px; margin:0 auto; background:#fff; border-radius:12px; "
                + "        box-shadow:0 4px 24px rgba(0,0,0,.12); overflow:hidden; }"
                + ".header { background:linear-gradient(135deg,#003366,#001a33); color:#fff; padding:32px 36px; }"
                + ".header h1 { font-size:22px; color:#00d4ff; }"
                + ".header p  { color:rgba(150,210,255,.75); font-size:12px; margin-top:4px; }"
                + ".body  { padding:30px 36px; }"
                + ".row   { display:flex; justify-content:space-between; padding:10px 0; "
                + "         border-bottom:1px solid #eee; font-size:13px; }"
                + ".row .lbl { color:#888; } .row .val { font-weight:600; color:#111; }"
                + ".pill  { padding:3px 12px; border-radius:20px; font-size:11px; font-weight:700; "
                + "         color:" + statusColor + "; border:1px solid " + statusColor + "; }"
                + ".footer { background:#f9f9f9; border-top:1px solid #eee; padding:16px 36px; "
                + "          text-align:center; font-size:11px; color:#aaa; }"
                + ".print-hint { background:#fff8e1; border:1px solid #ffd54f; border-radius:8px; "
                + "              padding:11px 16px; margin-bottom:18px; font-size:12px; color:#795548; }"
                + "@media print { .print-hint { display:none; } body { background:#fff; padding:0; } "
                + "               .page { box-shadow:none; } }"
                + "</style></head><body>"
                + "<div class='print-hint'>🖨️ <strong>Save as PDF:</strong> Press <kbd>Ctrl+P</kbd> → <em>Save as PDF</em></div>"
                + "<div class='page'>"
                + "<div class='header'><h1>🚗 Test Drive Confirmation</h1>"
                + "<p>AutoElite Premium Showroom — Booking #TD-" + String.format("%04d", tdId) + "</p></div>"
                + "<div class='body'>"
                + "<div class='row'><span class='lbl'>Booking ID</span><span class='val'>TD-"
                + String.format("%04d", tdId) + "</span></div>"
                + "<div class='row'><span class='lbl'>Vehicle</span><span class='val'>" + esc(vehicle) + "</span></div>"
                + "<div class='row'><span class='lbl'>Customer</span><span class='val'>" + esc(customer)
                + "</span></div>"
                + "<div class='row'><span class='lbl'>Date</span><span class='val'>" + esc(date) + "</span></div>"
                + "<div class='row'><span class='lbl'>Time Slot</span><span class='val'>" + esc(timeSlot)
                + "</span></div>"
                + "<div class='row'><span class='lbl'>Status</span><span class='val'><span class='pill'>" + esc(status)
                + "</span></span></div>"
                + "</div>"
                + "<div class='footer'>AutoElite Premium Showroom &nbsp;|&nbsp; Generated " + java.time.LocalDate.now()
                + "</div>"
                + "</div></body></html>";
    }

    private String buildReportHTML() {
        StringBuilder sb = new StringBuilder();
        // Collect data
        java.util.List<Sale> sales = new java.util.ArrayList<>();
        try {
            sales = saleController.getAllSales();
        } catch (Exception ignored) {
        }
        java.util.List<models.Car> cars = new java.util.ArrayList<>();
        try {
            cars = carController.getAllCars();
        } catch (Exception ignored) {
        }

        // Monthly revenue
        java.util.Map<String, Long> monthly = new java.util.TreeMap<>();
        for (Sale s : sales) {
            if (!"Completed".equalsIgnoreCase(s.getStatus()))
                continue;
            String mon = s.getSaleDate() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM").format(s.getSaleDate())
                    : "Unknown";
            monthly.merge(mon, (long) s.getSalePrice(), Long::sum);
        }

        // Category revenue
        java.util.Map<String, Long> byCat = new java.util.TreeMap<>();
        for (models.Car c : cars) {
            String cat = c.getCategory() != null ? c.getCategory() : "Other";
            byCat.merge(cat, (long) c.getPrice(), Long::sum); // price-per-unit contribution
        }

        long totalRev = monthly.values().stream().mapToLong(Long::longValue).sum();
        long totalSales = sales.stream().filter(s -> "Completed".equalsIgnoreCase(s.getStatus())).count();

        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
                .append("<title>AutoElite Business Report</title>")
                .append("<style>")
                .append("* { margin:0; padding:0; box-sizing:border-box; }")
                .append("body { font-family:'Segoe UI',Arial,sans-serif; background:#f4f6fb; padding:32px; }")
                .append("h1 { font-size:26px; color:#8a2be2; margin-bottom:6px; }")
                .append(".sub { color:#888; font-size:13px; margin-bottom:28px; }")
                .append(".grid { display:grid; grid-template-columns:repeat(3,1fr); gap:18px; margin-bottom:28px; }")
                .append(".card { background:#fff; border-radius:10px; padding:22px 24px; "
                        + "        box-shadow:0 2px 12px rgba(0,0,0,.08); border-left:4px solid #8a2be2; }")
                .append(".card .label { font-size:11px; color:#888; text-transform:uppercase; letter-spacing:1px; }")
                .append(".card .value { font-size:28px; font-weight:800; color:#1a0040; margin-top:4px; }")
                .append("section { background:#fff; border-radius:10px; padding:24px 28px; margin-bottom:22px; "
                        + "          box-shadow:0 2px 12px rgba(0,0,0,.08); }")
                .append("section h2 { font-size:14px; font-weight:700; color:#8a2be2; margin-bottom:16px; "
                        + "             text-transform:uppercase; letter-spacing:1px; }")
                // Bar chart via CSS
                .append(".bar-chart { display:flex; align-items:flex-end; gap:8px; height:160px; margin-bottom:6px; }")
                .append(".bar-col { display:flex; flex-direction:column; align-items:center; flex:1; }")
                .append(".bar { width:100%; border-radius:4px 4px 0 0; background:linear-gradient(180deg,#8a2be2,#00d4ff); "
                        + "       min-width:24px; transition:all .3s; }")
                .append(".bar-lbl { font-size:9px; color:#888; margin-top:4px; text-align:center; word-break:break-all; }")
                .append(".bar-val { font-size:9px; color:#555; margin-bottom:2px; font-weight:700; }")
                .append("table { width:100%; border-collapse:collapse; }")
                .append("th { background:#f0eaff; color:#8a2be2; font-size:11px; text-transform:uppercase; "
                        + "     letter-spacing:1px; padding:10px 12px; text-align:left; }")
                .append("td { padding:9px 12px; font-size:13px; border-bottom:1px solid #f0f0f0; }")
                .append("tr:last-child td { border-bottom:none; }")
                .append(".print-hint { background:#fff8e1; border:1px solid #ffd54f; border-radius:8px; "
                        + "              padding:12px 18px; margin-bottom:22px; font-size:12px; color:#795548; }")
                .append("@media print { .print-hint { display:none; } body { background:#fff; padding:16px; } "
                        + "               .card,.section { box-shadow:none; } }")
                .append("</style></head><body>")
                .append("<div class='print-hint'>🖨️ <strong>Save as PDF:</strong> Press <kbd>Ctrl+P</kbd> → <em>Save as PDF</em></div>")
                .append("<h1>📊 AutoElite Business Report</h1>")
                .append("<div class='sub'>Generated: ")
                .append(java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 16)).append("</div>")

                // KPI cards
                .append("<div class='grid'>")
                .append("<div class='card'><div class='label'>Total Revenue</div><div class='value'>$")
                .append(String.format("%,d", totalRev)).append("</div></div>")
                .append("<div class='card'><div class='label'>Completed Sales</div><div class='value'>")
                .append(totalSales).append("</div></div>")
                .append("<div class='card'><div class='label'>Vehicles in Stock</div><div class='value'>")
                .append(cars.size()).append("</div></div>")
                .append("</div>");

        // Monthly revenue bar chart
        sb.append("<section><h2>📈 Monthly Sales Revenue</h2>");
        if (!monthly.isEmpty()) {
            long maxVal = monthly.values().stream().mapToLong(Long::longValue).max().orElse(1);
            sb.append("<div class='bar-chart'>");
            for (java.util.Map.Entry<String, Long> e : monthly.entrySet()) {
                int pct = (int) (e.getValue() * 100 / maxVal);
                sb.append("<div class='bar-col'>")
                        .append("<div class='bar-val'>$").append(String.format("%,d", e.getValue() / 1000))
                        .append("k</div>")
                        .append("<div class='bar' style='height:").append(Math.max(pct, 4)).append("%'></div>")
                        .append("<div class='bar-lbl'>").append(e.getKey()).append("</div>")
                        .append("</div>");
            }
            sb.append("</div>");
        } else {
            sb.append("<p style='color:#aaa;font-size:13px'>No completed sales data yet.</p>");
        }
        sb.append("</section>");

        // Revenue by category bar chart
        sb.append("<section><h2>🏷️ Revenue by Category</h2>");
        if (!byCat.isEmpty()) {
            long maxVal = byCat.values().stream().mapToLong(Long::longValue).max().orElse(1);
            sb.append("<div class='bar-chart'>");
            String[] colors = { "#8a2be2", "#00d4ff", "#39ff14", "#ff8c00", "#ff4444", "#ffd700" };
            int ci = 0;
            for (java.util.Map.Entry<String, Long> e : byCat.entrySet()) {
                int pct = maxVal > 0 ? (int) (e.getValue() * 100 / maxVal) : 0;
                String c = colors[ci++ % colors.length];
                sb.append("<div class='bar-col'>")
                        .append("<div class='bar-val'>$").append(String.format("%,d", e.getValue() / 1000))
                        .append("k</div>")
                        .append("<div class='bar' style='height:").append(Math.max(pct, 4)).append("%;background:")
                        .append(c).append("'></div>")
                        .append("<div class='bar-lbl'>").append(esc(e.getKey())).append("</div>")
                        .append("</div>");
            }
            sb.append("</div>");
        } else {
            sb.append("<p style='color:#aaa;font-size:13px'>No category data yet.</p>");
        }
        sb.append("</section>");

        // Sales table
        sb.append("<section><h2>🧾 All Sales</h2>")
                .append("<table><tr><th>#</th><th>Vehicle</th><th>Customer</th><th>Amount</th><th>Payment</th><th>Status</th></tr>");
        for (Sale s : sales) {
            String sc = "Completed".equalsIgnoreCase(s.getStatus()) ? "#1a6b1a"
                    : "Cancelled".equalsIgnoreCase(s.getStatus()) ? "#8b0000" : "#003366";
            sb.append("<tr><td>").append(s.getId()).append("</td>")
                    .append("<td>").append(esc(s.getCarName() != null ? s.getCarName() : "Car #" + s.getCarId()))
                    .append("</td>")
                    .append("<td>").append(esc(s.getCustomerName())).append("</td>")
                    .append("<td>$").append(String.format("%,d", (long) s.getSalePrice())).append("</td>")
                    .append("<td>").append(esc(s.getPaymentMethod())).append("</td>")
                    .append("<td style='color:").append(sc).append(";font-weight:700'>").append(esc(s.getStatus()))
                    .append("</td></tr>");
        }
        sb.append("</table></section>");

        // Low stock table
        java.util.List<models.Car> lowStock = cars.stream()
                .filter(c -> c.getStock() <= 3)
                .sorted(java.util.Comparator.comparingInt(models.Car::getStock))
                .collect(java.util.stream.Collectors.toList());
        sb.append("<section><h2>⚠️ Low Stock (≤ 3 units)</h2>")
                .append("<table><tr><th>Brand</th><th>Model</th><th>Category</th><th>Stock</th></tr>");
        for (models.Car c : lowStock) {
            sb.append("<tr><td>").append(esc(c.getBrand())).append("</td>")
                    .append("<td>").append(esc(c.getModel())).append("</td>")
                    .append("<td>").append(esc(c.getCategory())).append("</td>")
                    .append("<td style='color:#c0392b;font-weight:800'>").append(c.getStock()).append("</td></tr>");
        }
        if (lowStock.isEmpty())
            sb.append("<tr><td colspan='4' style='color:#aaa'>All vehicles adequately stocked.</td></tr>");
        sb.append("</table></section>")
                .append("<div style='text-align:center;color:#aaa;font-size:11px;margin-top:10px'>")
                .append("AutoElite Premium Showroom — Confidential Business Report</div>")
                .append("</body></html>");

        return sb.toString();
    }

    /** Writes HTML to a temp file and opens it in the default browser. */
    private void openHtmlInBrowser(String html, String fileName, String statusMsg) throws Exception {
        java.io.File tmp = new java.io.File(System.getProperty("java.io.tmpdir"), fileName);
        try (java.io.FileWriter fw = new java.io.FileWriter(tmp, java.nio.charset.StandardCharsets.UTF_8)) {
            fw.write(html);
        }
        if (java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(tmp.toURI());
            setStatus(statusMsg);
            // Show a friendly tip dialog
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "<html><b>Document opened in your browser!</b><br><br>"
                                + "To save as PDF:<br>"
                                + "&nbsp;&nbsp;• <b>Windows/Linux:</b> Press <b>Ctrl+P</b> → Select <b>Save as PDF</b> → Click Save<br>"
                                + "&nbsp;&nbsp;• <b>macOS:</b> Press <b>⌘+P</b> → Click PDF dropdown → <b>Save as PDF</b><br><br>"
                                + "<small style='color:gray'>File saved at: " + tmp.getAbsolutePath()
                                + "</small></html>",
                        "💾 Save as PDF", JOptionPane.INFORMATION_MESSAGE);
            });
        } else {
            JOptionPane.showMessageDialog(this,
                    "File saved to:\n" + tmp.getAbsolutePath()
                            + "\n\nOpen it in Chrome or Edge, then press Ctrl+P → Save as PDF.",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** HTML-escape helper */
    private String esc(String s) {
        if (s == null)
            return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // =========================================================================
    // ★ FEATURE 3 — CHARTS IN REPORTS PANEL (ZERO EXTERNAL LIBRARIES)
    //
    // Uses Java2D (built into the JDK) to draw bar and pie charts directly
    // on a JPanel. No JFreeChart, no extra JARs required.
    // =========================================================================

    /** Builds a Java2D bar chart panel for monthly sales revenue. */
    private JPanel buildMonthlyRevenueChart() {
        java.util.List<Sale> sales = new java.util.ArrayList<>();
        try {
            sales = saleController.getAllSales();
        } catch (Exception ignored) {
        }

        // Aggregate by month
        java.util.Map<String, Long> monthly = new java.util.TreeMap<>();
        for (Sale s : sales) {
            if (!"Completed".equalsIgnoreCase(s.getStatus()))
                continue;
            String mon = s.getSaleDate() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM").format(s.getSaleDate())
                    : "Unknown";
            monthly.merge(mon, (long) s.getSalePrice(), Long::sum);
        }
        return buildJava2DBarChart("Monthly Sales Revenue", monthly, new java.awt.Color(0, 212, 255),
                new java.awt.Color(138, 43, 226));
    }

    /** Builds a Java2D bar chart panel for revenue by vehicle category. */
    private JPanel buildCategoryRevenueChart() {
        java.util.List<Sale> sales = new java.util.ArrayList<>();
        try {
            sales = saleController.getAllSales();
        } catch (Exception ignored) {
        }

        java.util.Map<String, Long> byCat = new java.util.TreeMap<>();
        for (Sale s : sales) {
            if (!"Completed".equalsIgnoreCase(s.getStatus()))
                continue;
            // Try to get category from car info via sale
            String cat = "Vehicle";
            try {
                java.util.List<models.Car> cars = carController.getAllCars();
                for (models.Car c : cars) {
                    if ((s.getCarName() != null && s.getCarName().toLowerCase().contains(c.getModel().toLowerCase()))) {
                        cat = c.getCategory() != null ? c.getCategory() : "Other";
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
            byCat.merge(cat, (long) s.getSalePrice(), Long::sum);
        }
        return buildJava2DBarChart("Revenue by Category", byCat, new java.awt.Color(57, 255, 20),
                new java.awt.Color(255, 140, 0));
    }

    /**
     * Renders a bar chart using pure Java2D — no external library needed.
     * Draws gradient bars, axis labels, value annotations, and a grid.
     */
    private JPanel buildJava2DBarChart(String title,
            java.util.Map<String, Long> data,
            java.awt.Color colorTop,
            java.awt.Color colorBottom) {
        return new JPanel() {
            {
                setBackground(new java.awt.Color(9, 12, 26));
                setPreferredSize(new java.awt.Dimension(380, 240));
            }

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int W = getWidth(), H = getHeight();
                int padL = 52, padR = 16, padT = 36, padB = 44;
                int chartW = W - padL - padR, chartH = H - padT - padB;

                // Background
                g2.setColor(new java.awt.Color(9, 12, 26));
                g2.fillRect(0, 0, W, H);

                // Title
                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
                g2.setColor(new java.awt.Color(138, 43, 226));
                g2.drawString(title, padL, padT - 14);

                if (data == null || data.isEmpty()) {
                    g2.setColor(new java.awt.Color(100, 120, 160));
                    g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
                    g2.drawString("No data available", padL + chartW / 2 - 50, padT + chartH / 2);
                    return;
                }

                long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1);

                // Grid lines (5 horizontal)
                g2.setStroke(new java.awt.BasicStroke(0.6f, java.awt.BasicStroke.CAP_BUTT,
                        java.awt.BasicStroke.JOIN_MITER, 10f, new float[] { 4f, 4f }, 0f));
                for (int i = 0; i <= 5; i++) {
                    int y = padT + chartH - (i * chartH / 5);
                    g2.setColor(new java.awt.Color(255, 255, 255, 20));
                    g2.drawLine(padL, y, padL + chartW, y);
                    // Y-axis label
                    long labelVal = (long) (maxVal * i / 5);
                    String label = labelVal >= 1_000_000 ? String.format("$%.1fM", labelVal / 1_000_000.0)
                            : labelVal >= 1_000 ? String.format("$%dk", labelVal / 1000)
                                    : "$" + labelVal;
                    g2.setColor(new java.awt.Color(100, 120, 160));
                    g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 8));
                    g2.drawString(label, 2, y + 4);
                }

                // Bars
                g2.setStroke(new java.awt.BasicStroke(1f));
                String[] keys = data.keySet().toArray(new String[0]);
                int n = keys.length;
                int barGap = Math.max(3, chartW / (n * 5));
                int barW = Math.max(8, (chartW - barGap * (n + 1)) / n);

                for (int i = 0; i < n; i++) {
                    long val = data.get(keys[i]);
                    int barH = (int) (val * chartH / maxVal);
                    int x = padL + barGap + i * (barW + barGap);
                    int y = padT + chartH - barH;

                    // Gradient bar
                    java.awt.GradientPaint gp = new java.awt.GradientPaint(
                            x, y, colorTop, x, padT + chartH, colorBottom);
                    g2.setPaint(gp);
                    g2.fillRoundRect(x, y, barW, barH, 4, 4);

                    // Bar border
                    g2.setColor(colorTop.brighter());
                    g2.setStroke(new java.awt.BasicStroke(0.8f));
                    g2.drawRoundRect(x, y, barW, barH, 4, 4);

                    // Value above bar
                    g2.setColor(new java.awt.Color(220, 230, 255));
                    g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 7));
                    String valStr = val >= 1_000_000 ? String.format("%.1fM", val / 1_000_000.0)
                            : val >= 1_000 ? String.format("%dk", val / 1000) : String.valueOf(val);
                    int tw = g2.getFontMetrics().stringWidth(valStr);
                    if (y - 3 > padT)
                        g2.drawString(valStr, x + barW / 2 - tw / 2, y - 3);

                    // X-axis label
                    g2.setColor(new java.awt.Color(100, 120, 160));
                    g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 7));
                    String xLbl = keys[i].length() > 8 ? keys[i].substring(keys[i].length() - 5) : keys[i];
                    int lw = g2.getFontMetrics().stringWidth(xLbl);
                    g2.drawString(xLbl, x + barW / 2 - lw / 2, padT + chartH + 13);
                }

                // Axes
                g2.setColor(new java.awt.Color(80, 100, 140));
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                g2.drawLine(padL, padT, padL, padT + chartH);
                g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);
            }
        };
    }

    /**
     * Enhanced Reports panel — uses Java2D charts instead of JFreeChart.
     * No external libraries required.
     */
    private JPanel buildReportsPanelEnhanced() {
        Object object22;
        List<Car> list;
        String string;
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(BG_DARK);
        JPanel jPanel2 = new JPanel(new BorderLayout());
        jPanel2.setOpaque(false);
        jPanel2.setBorder(new EmptyBorder(0, 0, 10, 0));
        jPanel2.add((Component) this.sectionTitle("\ud83d\udcca  Business Reports", new Color(255, 140, 0)), "West");
        JButton jButton = this.glowButton("\ud83d\udcc4 Export Report PDF", ACCENT_CYAN, null);
        jButton.addActionListener(
                actionEvent -> new Thread(() -> SwingUtilities.invokeLater(this::exportReportPDF)).start());
        JButton jButton2 = this.glowButton("\u21bb Refresh", BG_PANEL, null);
        JPanel jPanel3 = new JPanel(new FlowLayout(2, 8, 0));
        jPanel3.setOpaque(false);
        jPanel3.add(jButton);
        jPanel3.add(jButton2);
        jPanel2.add((Component) jPanel3, "East");
        jPanel.add((Component) jPanel2, "North");
        JPanel jPanel4 = new JPanel();
        jPanel4.setBackground(BG_DARK);
        jPanel4.setLayout(new BoxLayout(jPanel4, 1));
        jPanel4.setBorder(new EmptyBorder(0, 0, 20, 0));
        Map<String, Long> map = this.getSalesByMonth();
        Map<String, Long> map2 = this.getSalesCountByMonth();
        ArrayList<String> arrayList = new ArrayList<String>(map.keySet());
        long l2 = 0L;
        long l3 = 0L;
        int n3 = 0;
        int n4 = 0;
        if (!arrayList.isEmpty()) {
            string = (String) arrayList.get(arrayList.size() - 1);
            l2 = map.getOrDefault(string, 0L);
            n3 = map2.getOrDefault(string, 0L).intValue();
        }
        if (arrayList.size() >= 2) {
            string = (String) arrayList.get(arrayList.size() - 2);
            l3 = map.getOrDefault(string, 0L);
            n4 = map2.getOrDefault(string, 0L).intValue();
        }
        double d = l3 > 0L ? (double) (l2 - l3) * 100.0 / (double) l3 : 0.0;
        long l4 = map.values().stream().mapToLong(l -> l).sum();
        JPanel jPanel5 = this.buildReportCard("\ud83d\udcc8 Monthly Sales Revenue \u2014 Trend", new Color(0, 212, 255),
                this.buildLineChart("Revenue", "Prev", map, new Color(0, 212, 255), new Color(138, 43, 226)),
                new String[][] { { "Current Month", "$" + this.formatMoney(l2) },
                        { "Previous Month", "$" + this.formatMoney(l3) },
                        { "Month-on-Month", (d >= 0.0 ? "\u25b2 " : "\u25bc ") + String.format("%.1f%%", Math.abs(d)) },
                        { "", "" }, { "Sales This Month", String.valueOf(n3) },
                        { "Sales Prev Month", String.valueOf(n4) }, { "", "" },
                        { "Total All-Time", "$" + this.formatMoney(l4) },
                        { "Months Tracked", String.valueOf(arrayList.size()) } },
                new Color[] { null, null, d >= 0.0 ? new Color(57, 255, 20) : new Color(255, 80, 80), null, null, null,
                        null, null, null });
        jPanel5.setAlignmentX(0.0f);
        jPanel4.add(jPanel5);
        jPanel4.add(Box.createVerticalStrut(12));
        TreeMap<String, Long> treeMap = new TreeMap<String, Long>();
        try {
            for (Sale object32 : this.saleController.getAllSales()) {
                if (!"Completed".equalsIgnoreCase(object32.getStatus()))
                    continue;
                String catStr = "Other";
                try {
                    for (Car exception : this.carController.getAllCars()) {
                        if (object32.getCarName() == null
                                || !object32.getCarName().toLowerCase().contains(exception.getModel().toLowerCase()))
                            continue;
                        catStr = exception.getCategory() != null ? exception.getCategory() : "Other";
                        break;
                    }
                } catch (Exception n5) {
                    // empty catch block
                }
                treeMap.merge(catStr, (long) object32.getSalePrice(), Long::sum);
            }
        } catch (Exception exception) {
            // empty catch block
        }
        String string2 = treeMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse("N/A");
        JPanel jPanel6 = this.buildReportCard("\ud83c\udff7\ufe0f Revenue by Category", new Color(57, 255, 20),
                this.buildHorizontalBarChart(treeMap, new Color(57, 255, 20)),
                new String[][] { { "Top Category", string2 },
                        { "Top Revenue", "$" + this.formatMoney(treeMap.getOrDefault(string2, 0L).longValue()) },
                        { "Categories", String.valueOf(treeMap.size()) }, { "", "" },
                        { "SUV", "$" + this.formatMoney(treeMap.getOrDefault("SUV", 0L).longValue()) },
                        { "Sedan", "$" + this.formatMoney(treeMap.getOrDefault("Sedan", 0L).longValue()) },
                        { "Electric", "$" + this.formatMoney(treeMap.getOrDefault("Electric", 0L).longValue()) },
                        { "Sports", "$" + this.formatMoney(treeMap.getOrDefault("Sports", 0L).longValue()) },
                        { "Luxury", "$" + this.formatMoney(treeMap.getOrDefault("Luxury", 0L).longValue()) } },
                null);
        jPanel6.setAlignmentX(0.0f);
        jPanel4.add(jPanel6);
        jPanel4.add(Box.createVerticalStrut(12));
        list = new ArrayList();
        int n5 = 0;
        try {
            list = this.carController.getLowStockCars(3);
        } catch (Exception exception) {
            // empty catch block
        }
        try {
            n5 = this.carController.getAllCars().size();
        } catch (Exception n6) {
            // empty catch block
        }
        int n6 = (int) list.stream().filter(car -> car.getStock() == 0).count();
        int n7 = list.size() - n6;
        Object[] objectArray = new String[] { "Brand", "Model", "Category", "Stock", "Status" };
        DefaultTableModel defaultTableModel = new DefaultTableModel(objectArray, 0) {

            @Override
            public boolean isCellEditable(int n, int n2) {
                return false;
            }
        };
        for (Car n8 : list) {
            defaultTableModel.addRow(new Object[] { n8.getBrand(), n8.getModel(), n8.getCategory(), n8.getStock(),
                    n8.getStock() == 0 ? "OUT OF STOCK" : "CRITICAL" });
        }
        JTable jTable2 = this.buildFancyTable(defaultTableModel);
        int jScrollPane = 3;
        while (jScrollPane <= 4) {
            int jPanel62 = jScrollPane++;
            jTable2.getColumnModel().getColumn(jPanel62).setCellRenderer((jTable, object, bl, bl2, n, n2) -> {
                JLabel jLabel = new JLabel(String.valueOf(object), 0);
                jLabel.setFont(jLabel.getFont().deriveFont(1));
                boolean bl3 = String.valueOf(defaultTableModel.getValueAt(n, 3)).equals("0");
                jLabel.setForeground(bl3 ? new Color(255, 60, 60) : new Color(255, 160, 40));
                jLabel.setOpaque(true);
                jLabel.setBackground(bl ? new Color(50, 30, 90) : BG_PANEL);
                return jLabel;
            });
        }
        JScrollPane jScrollPane2 = new JScrollPane(jTable2);
        jScrollPane2.setBorder(BorderFactory.createLineBorder(new Color(50, 30, 90)));
        jScrollPane2.getViewport().setBackground(BG_PANEL);
        jScrollPane2.setBackground(BG_PANEL);
        this.styleScrollBar(jScrollPane2.getVerticalScrollBar());
        JPanel jPanel7 = new JPanel(new BorderLayout());
        jPanel7.setOpaque(false);
        jPanel7.add(jScrollPane2);
        int n8 = n5;
        int n9 = list.size();
        JPanel jPanel8 = this.buildReportCard("\u26a0\ufe0f  Low Stock Alert  (" + list.size() + " vehicles)",
                new Color(255, 160, 0), jPanel7,
                new String[][] { { "Low Stock Total", String.valueOf(n9) }, { "Out of Stock", String.valueOf(n6) },
                        { "Critical (\u22642)", String.valueOf(n7) }, { "", "" },
                        { "Total Inventory", String.valueOf(n8) },
                        { "Low Stock %", n8 > 0 ? String.format("%.1f%%", (double) n9 * 100.0 / (double) n8) : "N/A" },
                        { "", "" }, { "Action Required", n9 > 0 ? "Restock Soon" : "All Good" } },
                new Color[] { new Color(255, 160, 40), new Color(255, 60, 60), new Color(255, 120, 40), null, null,
                        null, null, n9 > 0 ? new Color(255, 160, 40) : new Color(57, 255, 20) });
        jPanel8.setAlignmentX(0.0f);
        jPanel4.add(jPanel8);
        jPanel4.add(Box.createVerticalStrut(12));
        Object[] objectArray2 = new String[] { "Department", "Headcount", "Monthly Salary" };
        DefaultTableModel defaultTableModel2 = new DefaultTableModel(objectArray2, 0) {

            @Override
            public boolean isCellEditable(int n, int n2) {
                return false;
            }
        };
        long l5 = 0L;
        List<String[]> list2 = new ArrayList<>();
        try {
            list2 = this.employeeController.getPayrollSummary();
            for (String[] payRow : list2) {
                defaultTableModel2.addRow(payRow);
                try {
                    l5 += Long.parseLong(payRow[2].replaceAll("[^0-9]", ""));
                } catch (Exception jPanel82) {
                }
            }
            if (list2.isEmpty()) {
                defaultTableModel2.addRow(new String[] { "No data", "-", "-" });
            }
        } catch (Exception exception) {
            // empty catch block
        }
        JTable exception = this.buildFancyTable(defaultTableModel2);
        exception.getColumnModel().getColumn(2).setCellRenderer((jTable, object, bl, bl2, n, n2) -> {
            JLabel jLabel = new JLabel(String.valueOf(object), 4);
            jLabel.setFont(jLabel.getFont().deriveFont(1));
            jLabel.setForeground(new Color(57, 255, 20));
            jLabel.setOpaque(true);
            jLabel.setBackground(bl ? new Color(50, 30, 90) : BG_PANEL);
            jLabel.setBorder(new EmptyBorder(0, 0, 0, 8));
            return jLabel;
        });
        object22 = new JScrollPane(exception);
        ((JComponent) object22).setBorder(BorderFactory.createLineBorder(new Color(50, 30, 90)));
        ((JScrollPane) object22).getViewport().setBackground(BG_PANEL);
        ((JComponent) object22).setBackground(BG_PANEL);
        this.styleScrollBar(((JScrollPane) object22).getVerticalScrollBar());
        JPanel jPanel9 = new JPanel(new BorderLayout());
        jPanel9.setOpaque(false);
        jPanel9.add((Component) object22);
        int n10 = list2.size();
        int n11 = 0;
        for (String[] stringArray : list2) {
            try {
                n11 += Integer.parseInt(stringArray[1].replaceAll("[^0-9]", ""));
            } catch (Exception l7) {
            }
        }
        long l6 = n10 > 0 ? l5 / (long) Math.max(n10, 1) : 0L;
        long l7 = n11 > 0 ? l5 / (long) Math.max(n11, 1) : 0L;
        long l8 = l5;
        JPanel jPanel10 = this.buildReportCard("\ud83d\udcb0 Monthly Payroll", new Color(57, 255, 20), jPanel9,
                new String[][] { { "Total Monthly", "$" + String.format("%,d", l8) },
                        { "Departments", String.valueOf(n10) }, { "Total Headcount", String.valueOf(n11) }, { "", "" },
                        { "Avg / Department", "$" + String.format("%,d", l6) },
                        { "Avg / Employee", "$" + String.format("%,d", l7) }, { "", "" }, { "Pay Cycle", "Monthly" } },
                new Color[] { new Color(57, 255, 20), null, null, null, null, null, null, new Color(150, 170, 220) });
        jPanel10.setAlignmentX(0.0f);
        jPanel4.add(jPanel10);
        jPanel4.add(Box.createVerticalStrut(12));
        int n12 = LocalDate.now().getYear();
        int n13 = n12 - 1;
        LinkedHashMap<String, Long>[] linkedHashMapArray = this.saleController.getQuarterlyByYear();
        LinkedHashMap<String, Long> linkedHashMap = linkedHashMapArray[0];
        LinkedHashMap<String, Long> linkedHashMap2 = linkedHashMapArray[1];
        long l9 = linkedHashMap.values().stream().mapToLong(Long::longValue).sum();
        long l10 = linkedHashMap2.values().stream().mapToLong(Long::longValue).sum();
        String string3 = linkedHashMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse("N/A");
        long l11 = linkedHashMap.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        double d2 = l10 > 0L ? (double) (l9 - l10) * 100.0 / (double) l10 : 0.0;
        JPanel jPanel11 = this.buildDualQuarterlyChart(linkedHashMap, linkedHashMap2, n12, n13);
        JPanel jPanel12 = this.buildReportCard("\ud83d\udcc5 Quarterly Revenue Trend", new Color(255, 180, 0), jPanel11,
                new String[][] { { n12 + " Revenue", "$" + String.format("%,d", l9) },
                        { n13 + " Revenue", "$" + String.format("%,d", l10) },
                        { "YoY Change", (d2 >= 0.0 ? "\u25b2" : "\u25bc") + String.format("%.1f%%", Math.abs(d2)) },
                        { "", "" }, { "Best Quarter", string3 }, { "Best Revenue", "$" + String.format("%,d", l11) },
                        { "", "" }, { "Trend", "Amber=" + n12 + "  Grey=" + n13 } },
                new Color[] { new Color(255, 200, 50), new Color(160, 160, 200),
                        d2 >= 0.0 ? new Color(57, 255, 20) : new Color(255, 80, 80), null, new Color(255, 200, 50),
                        new Color(57, 255, 20), null, new Color(140, 150, 180) });
        jPanel12.setAlignmentX(0.0f);
        jPanel4.add(jPanel12);
        jPanel4.add(Box.createVerticalStrut(12));
        List<String[]> list3 = this.saleController.getYearlySummary();
        LinkedHashMap<String, Long> linkedHashMap3 = new LinkedHashMap<String, Long>();
        long l12 = 0L;
        for (String[] l14 : list3) {
            long l13 = (long) Double.parseDouble(l14[2].replaceAll("[^0-9.]", ""));
            linkedHashMap3.put(l14[0], l13);
            l12 += Long.parseLong(l14[1]);
        }
        String string4 = linkedHashMap3.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse("N/A");
        long l14 = linkedHashMap3.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        long l15 = linkedHashMap3.values().stream().mapToLong(Long::longValue).sum();
        JPanel jPanel13 = this.buildSimpleLineChart(linkedHashMap3, new Color(0, 200, 150), "Yearly Revenue");
        JPanel jPanel14 = this.buildReportCard("\ud83d\udcca Yearly Revenue Trend", new Color(0, 200, 150), jPanel13,
                new String[][] { { "Years Tracked", String.valueOf(linkedHashMap3.size()) }, { "Best Year", string4 },
                        { "Best Revenue", "$" + String.format("%,d", l14) }, { "", "" },
                        { "Total Revenue", "$" + String.format("%,d", l15) }, { "Total Sales", String.valueOf(l12) },
                        { "", "" },
                        { "Growth", linkedHashMap3.size() >= 2 ? "\ud83d\udcc8 Multi-year" : "Awaiting data" } },
                new Color[] { null, new Color(0, 220, 180), new Color(57, 255, 20), null, new Color(0, 200, 150), null,
                        null, new Color(150, 200, 255) });
        jPanel14.setAlignmentX(0.0f);
        jPanel4.add(jPanel14);
        JScrollPane jScrollPane3 = new JScrollPane(jPanel4);
        jScrollPane3.setBorder(null);
        jScrollPane3.getViewport().setBackground(BG_DARK);
        jScrollPane3.getVerticalScrollBar().setUnitIncrement(16);
        this.styleScrollBar(jScrollPane3.getVerticalScrollBar());
        jButton2.addActionListener(actionEvent -> {
            jPanel.remove(jScrollPane2);
            jPanel.add((Component) this.buildReportsPanelEnhanced(), "Center");
            jPanel.revalidate();
            jPanel.repaint();
        });
        jPanel.add((Component) jScrollPane3, "Center");
        return jPanel;
    }

    private JPanel buildChartCard(String title, JPanel chart) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_PANEL);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(50, 30, 90), 1, true),
                javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        titleLbl.setForeground(new java.awt.Color(255, 140, 0));
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    /** Low-stock alert table card. */
    private JPanel buildLowStockCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_PANEL);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(50, 30, 90), 1, true),
                javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel lbl = new JLabel("⚠  Low Stock (≤ 3)");
        lbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lbl.setForeground(new java.awt.Color(255, 200, 0));
        card.add(lbl, BorderLayout.NORTH);

        String[] cols = { "Brand", "Model", "Category", "Stock" };
        javax.swing.table.DefaultTableModel tm = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        try {
            java.util.List<models.Car> lowStock = carController.getLowStockCars(3);
            for (models.Car c : lowStock)
                tm.addRow(new Object[] { c.getBrand(), c.getModel(), c.getCategory(), c.getStock() });
        } catch (Exception ignored) {
        }

        JTable tbl = buildFancyTable(tm);
        tbl.getColumnModel().getColumn(3).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            JLabel l = new JLabel(String.valueOf(value), SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
            l.setForeground(new java.awt.Color(255, 80, 80));
            l.setOpaque(true);
            l.setBackground(isSelected ? new java.awt.Color(50, 30, 90) : BG_PANEL);
            return l;
        });
        card.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return card;
    }

    /** Monthly payroll card (reuses existing logic). */
    private JPanel buildPayrollCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_PANEL);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(50, 30, 90), 1, true),
                javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel lbl = new JLabel("💰 Monthly Payroll");
        lbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lbl.setForeground(new java.awt.Color(57, 255, 20));
        card.add(lbl, BorderLayout.NORTH);

        // Build payroll table
        String[] cols = { "Department", "Headcount", "Total Salary/Month" };
        javax.swing.table.DefaultTableModel tm = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        long grandTotal = 0;
        try {
            java.util.List<String[]> payroll = employeeController.getPayrollSummary();
            for (String[] row : payroll) {
                // row typically: [dept, count, total]
                tm.addRow(row);
                try {
                    grandTotal += Long.parseLong(row[2].replaceAll("[^0-9]", ""));
                } catch (Exception ignored2) {
                }
            }
            if (payroll.isEmpty())
                tm.addRow(new String[] { "No data", "-", "-" });
        } catch (Exception ignored) {
        }
        tm.addRow(new Object[] { "──────", "──", "──────────" });
        tm.addRow(new Object[] { "TOTAL", "–", "$" + String.format("%,d", grandTotal) });

        JTable tbl = buildFancyTable(tm);
        card.add(new JScrollPane(tbl), BorderLayout.CENTER);

        long fGrand = grandTotal;
        JLabel totalLbl = new JLabel("Monthly Payroll: $" + String.format("%,d", fGrand));
        totalLbl.setForeground(new java.awt.Color(57, 255, 20));
        totalLbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        totalLbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        card.add(totalLbl, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================================
    // ★ FEATURE 5 — EMAIL CUSTOMER (ZERO EXTERNAL LIBRARIES)
    //
    // Uses java.awt.Desktop.mail() to open the user's default email client
    // (Outlook, Mail, Thunderbird, etc.) with the message pre-filled.
    // No JavaMail, no Jakarta Mail, no extra JARs needed.
    // =========================================================================

    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private String smtpUser = "";
    private String smtpPass = "";

    /**
     * Opens the system's default email client with a pre-composed message.
     * Uses java.awt.Desktop.mail() — works on Windows, macOS, Linux.
     * Allows the user to review and send from their own email account.
     */
    private void emailCustomerViaDesktop(String customerName, String subject, String body) {
        try {
            // Find customer email
            String email = lookupCustomerEmail(customerName);
            if (email == null || email.isBlank()) {
                // No email found — prompt user
                email = JOptionPane.showInputDialog(this,
                        "No email found for " + customerName + ".\nEnter email address:",
                        "Email Address", JOptionPane.QUESTION_MESSAGE);
                if (email == null || email.isBlank())
                    return;
            }

            // Build mailto: URI
            String encSubject = java.net.URLEncoder.encode(subject, "UTF-8").replace("+", "%20");
            String encBody = java.net.URLEncoder.encode(body, "UTF-8").replace("+", "%20");
            java.net.URI mailto = new java.net.URI(
                    "mailto:" + email + "?subject=" + encSubject + "&body=" + encBody);

            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MAIL)) {
                java.awt.Desktop.getDesktop().mail(mailto);
                setStatus("✓ Email client opened for " + customerName + " (" + email + ")");
            } else {
                // Desktop.mail not supported — show copyable content
                showEmailFallbackDialog(email, subject, body);
            }
        } catch (Exception ex) {
            showError("Could not open email client: " + ex.getMessage());
        }
    }

    /**
     * Shows a dialog with the email content so the user can copy and send manually.
     */
    private void showEmailFallbackDialog(String email, String subject, String body) {
        JDialog dlg = styledDialog("📧 Send Email", 540, 420);
        dlg.setLayout(new BorderLayout(0, 8));
        dlg.add(buildDialogHeader("📧", "Send Email to Customer", new java.awt.Color(255, 165, 0)), BorderLayout.NORTH);

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        // To/Subject labels
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel toLbl = new JLabel("To:");
        toLbl.setFont(FONT_LABEL);
        toLbl.setForeground(TEXT_MUTED);
        form.add(toLbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JLabel toVal = new JLabel(email);
        toVal.setFont(FONT_LABEL);
        toVal.setForeground(TEXT_PRIMARY);
        form.add(toVal, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel subLbl2 = new JLabel("Subject:");
        subLbl2.setFont(FONT_LABEL);
        subLbl2.setForeground(TEXT_MUTED);
        form.add(subLbl2, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JLabel subVal = new JLabel(subject);
        subVal.setFont(FONT_LABEL);
        subVal.setForeground(TEXT_PRIMARY);
        form.add(subVal, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Body:") {
            {
                setFont(FONT_LABEL);
                setForeground(TEXT_MUTED);
            }
        }, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        JTextArea ta = new JTextArea(body, 6, 30);
        ta.setBackground(BG_DARK);
        ta.setForeground(TEXT_PRIMARY);
        ta.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(50, 30, 90)),
                javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        form.add(new JScrollPane(ta), gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 10));
        btns.setBackground(BG_PANEL);
        JButton copyBtn = glowButton("📋 Copy Email", ACCENT_CYAN, null);
        JButton closeBtn = glowButton("✕ Close", new java.awt.Color(60, 30, 90), null);
        copyBtn.addActionListener(e -> {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(
                            "To: " + email + "\nSubject: " + subject + "\n\n" + body), null);
            setStatus("✓ Email content copied to clipboard");
        });
        closeBtn.addActionListener(e -> dlg.dispose());
        btns.add(copyBtn);
        btns.add(closeBtn);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Composes a test drive confirmation email and opens the email client. */
    private void emailTestDriveConfirmation(String customerName, String vehicle, String date, String timeSlot) {
        String subject = "Your Test Drive Booking — AutoElite Premium Showroom";
        String body = "Dear " + customerName + ",\n\n"
                + "Your test drive has been confirmed!\n\n"
                + "Vehicle   : " + vehicle + "\n"
                + "Date      : " + date + "\n"
                + "Time      : " + timeSlot + "\n\n"
                + "Please arrive 10 minutes early.\n"
                + "Bring your valid driver's licence.\n\n"
                + "We look forward to seeing you!\n\n"
                + "Best regards,\n"
                + "AutoElite Premium Showroom Team";
        emailCustomerViaDesktop(customerName, subject, body);
    }

    /** Shows the email composition dialog for any customer/context. */
    private void showEmailDialog(String prefilledCustomer, String prefilledSubject, String prefilledBody) {
        JDialog dlg = styledDialog("📧 Email Customer", 540, 460);
        dlg.setLayout(new BorderLayout(0, 8));
        dlg.add(buildDialogHeader("📧", "Email Customer", new java.awt.Color(255, 165, 0)), BorderLayout.NORTH);

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        // Customer field
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Customer:") {
            {
                setFont(FONT_LABEL);
                setForeground(TEXT_MUTED);
            }
        }, gbc);
        JTextField custField = darkTextField(prefilledCustomer != null ? prefilledCustomer : "", 20);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(custField, gbc);

        // Subject
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Subject:") {
            {
                setFont(FONT_LABEL);
                setForeground(TEXT_MUTED);
            }
        }, gbc);
        JTextField subjField = darkTextField(prefilledSubject != null ? prefilledSubject : "", 20);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(subjField, gbc);

        // Body
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        form.add(new JLabel("Message:") {
            {
                setFont(FONT_LABEL);
                setForeground(TEXT_MUTED);
            }
        }, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        JTextArea bodyArea = new JTextArea(prefilledBody != null ? prefilledBody : "", 8, 32);
        bodyArea.setBackground(BG_DARK);
        bodyArea.setForeground(TEXT_PRIMARY);
        bodyArea.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(50, 30, 90)),
                javax.swing.BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        JScrollPane sp = new JScrollPane(bodyArea);
        form.add(sp, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 10));
        btns.setBackground(BG_PANEL);
        JButton sendBtn = glowButton("📤 Open in Email App", new java.awt.Color(255, 165, 0), null);
        JButton closeBtn = glowButton("✕ Cancel", new java.awt.Color(60, 30, 90), null);
        sendBtn.addActionListener(e -> {
            dlg.dispose();
            emailCustomerViaDesktop(custField.getText().trim(), subjField.getText().trim(), bodyArea.getText().trim());
        });
        closeBtn.addActionListener(e -> dlg.dispose());
        btns.add(sendBtn);
        btns.add(closeBtn);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Shows the Email Settings panel (SMTP info for reference). */
    private void showEmailSettings() {
        JDialog dlg = styledDialog("⚙ Email Settings", 480, 320);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("⚙", "Email Settings", new java.awt.Color(255, 165, 0)), BorderLayout.NORTH);

        JPanel p = new JPanel(new java.awt.GridBagLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 22, 10, 22));
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        JLabel info = new JLabel("<html><div style='max-width:380px;color:#aab;font-size:12px;line-height:1.6'>"
                + "<b style='color:#00d4ff'>ℹ How Email Works</b><br><br>"
                + "AutoElite uses your device's <b>default email app</b> (Outlook, Mail, Thunderbird, etc.)<br>"
                + "to open pre-composed emails. No SMTP credentials or extra setup required.<br><br>"
                + "<b>How to use:</b><br>"
                + "1. Select a test drive or customer<br>"
                + "2. Click <b>Email Customer</b><br>"
                + "3. Your email app opens with the message ready<br>"
                + "4. Review and click <b>Send</b>"
                + "</div></html>");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        p.add(info, gbc);

        dlg.add(p, BorderLayout.CENTER);
        JPanel btns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 10));
        btns.setBackground(BG_PANEL);
        JButton ok = glowButton("✓ Got it", ACCENT_CYAN, null);
        ok.addActionListener(e -> dlg.dispose());
        btns.add(ok);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Looks up a customer's email from the database. */
    private String lookupCustomerEmail(String customerName) {
        try {
            return customerController.getAllCustomers().stream()
                    .filter(c -> c.getName() != null && c.getName().equals(customerName))
                    .map(models.Customer::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // ★ ENHANCED INVOICE DIALOG — invoice with PDF Export + Email buttons
    // =========================================================================

    private void showInvoiceDialogEnhanced(DefaultTableModel model, int row) {
        String saleId = String.valueOf(model.getValueAt(row, 0));
        String vehicle = String.valueOf(model.getValueAt(row, 1));
        String customer = String.valueOf(model.getValueAt(row, 2));
        String employee = String.valueOf(model.getValueAt(row, 3));
        String date = String.valueOf(model.getValueAt(row, 4));
        String amount = String.valueOf(model.getValueAt(row, 5));
        String payment = String.valueOf(model.getValueAt(row, 6));
        String status = String.valueOf(model.getValueAt(row, 7));

        JDialog dlg = styledDialog("Sale Invoice  —  #" + saleId, 520, 600);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("🧾", "Sale Invoice  ·  INV-" + String.format("%05d", Integer.parseInt(saleId)),
                ACCENT_CYAN), BorderLayout.NORTH);

        JPanel body = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 212, 255, 25), getWidth(), getHeight(),
                        new Color(138, 43, 226, 15));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(24, 32, 24, 32));
        JLabel logo = new JLabel("AutoElite Showroom");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT_CYAN);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Official Sale Invoice / Receipt");
        subtitle.setFont(FONT_LABEL);
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0, 212, 255, 60));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        String[][] fields = { { "Invoice #", "INV-" + String.format("%05d", Integer.parseInt(saleId)) },
                { "Date", date }, { "Status", status }, { "", "" }, { "Vehicle", vehicle }, { "", "" },
                { "Customer", customer }, { "Handled By", employee }, { "", "" }, { "Payment Method", payment },
                { "Sale Amount", amount } };
        JPanel grid = new JPanel(new GridLayout(fields.length, 2, 8, 6));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        for (String[] f : fields) {
            JLabel k = new JLabel(f[0]);
            k.setFont(FONT_SMALL);
            k.setForeground(TEXT_MUTED);
            JLabel v = new JLabel(f[1]);
            boolean isMoney = f[0].contains("Amount");
            boolean isStatus = f[0].equals("Status");
            v.setFont(isMoney ? new Font("Segoe UI", Font.BOLD, 15) : FONT_LABEL);
            v.setForeground(isMoney ? ACCENT_GREEN
                    : isStatus && status.equals("Completed") ? ACCENT_GREEN
                            : isStatus && status.equals("Pending") ? ACCENT_ORANGE : TEXT_PRIMARY);
            grid.add(k);
            grid.add(v);
        }
        JPanel totalBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(57, 255, 20, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(57, 255, 20, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
        };
        totalBox.setLayout(new BorderLayout());
        totalBox.setBorder(new EmptyBorder(12, 16, 12, 16));
        totalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        totalBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tL = new JLabel("TOTAL AMOUNT");
        tL.setFont(FONT_SMALL);
        tL.setForeground(TEXT_MUTED);
        JLabel tV = new JLabel(amount);
        tV.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tV.setForeground(ACCENT_GREEN);
        totalBox.add(tL, BorderLayout.WEST);
        totalBox.add(tV, BorderLayout.EAST);
        JLabel footer = new JLabel("Thank you for choosing AutoElite!");
        footer.setFont(FONT_SMALL);
        footer.setForeground(TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(logo);
        body.add(Box.createVerticalStrut(4));
        body.add(subtitle);
        body.add(Box.createVerticalStrut(12));
        body.add(sep);
        body.add(Box.createVerticalStrut(14));
        body.add(grid);
        body.add(Box.createVerticalStrut(16));
        body.add(totalBox);
        body.add(Box.createVerticalStrut(16));
        body.add(footer);
        dlg.add(new JScrollPane(body) {
            {
                setBorder(null);
                getViewport().setBackground(BG_PANEL);
            }
        }, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        btnPanel.setBackground(new Color(10, 13, 24));
        btnPanel.add(glowButton("Close", ACCENT_CYAN, e -> dlg.dispose()));
        JButton pdfBtnI = glowButton("📄 Export PDF", new Color(0, 180, 220), null);
        pdfBtnI.setToolTipText("Export invoice as HTML → Ctrl+P to save PDF");
        pdfBtnI.addActionListener(
                e -> exportInvoicePDF(saleId, vehicle, customer, employee, date, amount, payment, status));
        btnPanel.add(pdfBtnI);
        // Email always available via Desktop.mail()
        {
            String custEmail = lookupCustomerEmail(customer);
            JButton emailBtnI = glowButton("📧 Email Customer", ACCENT_ORANGE, null);
            emailBtnI.addActionListener(e -> {
                String emailSubj = "AutoElite Sale Confirmation — " + vehicle;
                String emailBody = "Dear " + customer + ",\n\nThank you for your purchase!\n\nVehicle: " + vehicle
                        + "\nAmount: " + amount + "\nPayment: " + payment + "\n\nBest regards,\nAutoElite Team";
                emailCustomerViaDesktop(customer, emailSubj, emailBody);
            });
            btnPanel.add(emailBtnI);
        }
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // =========================================================================
    // ★ ENHANCED SALES PANEL — wires in the new invoice dialog
    // =========================================================================

    private JPanel buildSalesPanelEnhanced() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("💰  Sales Management", ACCENT_GREEN), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("\uD83E\uDDE0 AI Sales Coach", new Color(50, 200, 150), e -> showSalesCoachDialog()));
        hr.add(glowButton("+ Record Sale", ACCENT_GREEN, e -> showAddSaleDialog()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);
        double rev = saleController.getTotalRevenue();
        int sc = saleController.getTotalSales();
        JPanel summary = new JPanel(new GridLayout(1, 3, 12, 0));
        summary.setOpaque(false);
        summary.setPreferredSize(new Dimension(0, 85));
        summary.add(miniStatCard("Total Revenue", "$" + formatMoney(rev), ACCENT_GREEN));
        summary.add(miniStatCard("Completed Sales", String.valueOf(sc), ACCENT_CYAN));
        summary.add(miniStatCard("Avg. Sale Price", sc > 0 ? "$" + formatMoney(rev / sc) : "N/A", ACCENT_PURPLE));
        String[] cols = { "ID", "Vehicle", "Customer", "Employee", "Date", "Amount", "Payment", "Status" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = buildFancyTable(model);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setOpaque(false);
        topContainer.add(summary, BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setOpaque(false);
        JTextField searchField = darkTextField("Search sales...", 28);
        filterBar.add(new JLabel("  🔍"));
        filterBar.add(searchField);
        topContainer.add(filterBar, BorderLayout.SOUTH);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filter();
            }

            private void filter() {
                String text = searchField.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(
                            javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });
        Runnable load = () -> {
            model.setRowCount(0);
            for (Sale s : saleController.getAllSales())
                model.addRow(new Object[] { s.getId(), s.getCarName() != null ? s.getCarName() : "Car#" + s.getCarId(),
                        s.getCustomerName() != null ? s.getCustomerName() : "Cust#" + s.getCustomerId(),
                        s.getEmployeeName() != null ? s.getEmployeeName() : "Emp#" + s.getEmployeeId(), s.getSaleDate(),
                        "$" + formatMoney(s.getSalePrice()), s.getPaymentMethod(), s.getStatus() });
            setStatus("Loaded " + model.getRowCount() + " sales");
        };
        load.run();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        actions.add(glowButton("✓ Mark Complete", ACCENT_GREEN, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            if (saleController.updateSaleStatus(id, "Completed")) {
                load.run();
                showSuccess("Sale marked complete.");
            }
        }));
        actions.add(glowButton("✕ Cancel Sale", ACCENT_PINK, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            int id = (int) model.getValueAt(row, 0);
            if (saleController.updateSaleStatus(id, "Cancelled")) {
                load.run();
                showSuccess("Sale cancelled.");
            }
        }));
        actions.add(glowButton("🧾 View Invoice", ACCENT_CYAN, e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0)
                return;
            int row = table.convertRowIndexToModel(viewRow);

            showInvoiceDialogEnhanced(model, row);
        }));
        JButton salesRefreshBtn = smallButton("↻ Refresh", TEXT_MUTED);
        salesRefreshBtn.addActionListener(ev -> load.run());
        actions.add(salesRefreshBtn);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(topContainer, BorderLayout.NORTH);
        center.add(styledScrollPane(table), BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    // =========================================================================
    // ★ ENHANCED TEST DRIVES PANEL — PDF + Email buttons wired in
    // =========================================================================

    private JPanel buildTestDrivesPanelEnhanced() {
        JPanel page = darkPage();
        page.setLayout(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("🔑  Test Drive Scheduler", ACCENT_ORANGE), BorderLayout.WEST);
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hr.setOpaque(false);
        hr.add(glowButton("⚙ Email Settings", TEXT_MUTED, e -> showEmailSettings()));

        hr.add(glowButton("+ Schedule Test Drive", ACCENT_ORANGE, e -> showAddTestDriveDialogEnhanced()));
        header.add(hr, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);
        String[] cols = { "ID", "Vehicle", "Customer", "Date", "Time Slot", "Status", "Feedback" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = buildFancyTable(model);
        Runnable load = () -> {
            model.setRowCount(0);
            for (TestDrive td : testDriveController.getAllTestDrives())
                model.addRow(
                        new Object[] { td.getId(), td.getCarName() != null ? td.getCarName() : "Car#" + td.getCarId(),
                                td.getCustomerName() != null ? td.getCustomerName() : "Cust#" + td.getCustomerId(),
                                td.getScheduledDate(), td.getTimeSlot(), td.getStatus(),
                                td.getFeedback() != null ? td.getFeedback() : "" });
            setStatus("Loaded " + model.getRowCount() + " test drives");
        };
        load.run();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setOpaque(false);
        actions.add(glowButton("✓ Mark Completed", ACCENT_GREEN, e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            String fb = JOptionPane.showInputDialog(this, "Feedback (optional):", "Feedback",
                    JOptionPane.PLAIN_MESSAGE);
            if (testDriveController.updateStatus(id, "Completed", fb)) {
                load.run();
                showSuccess("Test drive completed.");
            }
        }));
        actions.add(glowButton("✕ Cancel", ACCENT_PINK, e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            if (testDriveController.updateStatus(id, "Cancelled", null)) {
                load.run();
                showSuccess("Cancelled.");
            }
        }));
        actions.add(glowButton("🗑 Delete", new Color(150, 30, 50), e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            if (testDriveController.deleteTestDrive(id)) {
                load.run();
                showSuccess("Deleted.");
            }
        }));
        JButton pdfBtnTD = glowButton("📄 Export PDF", new Color(0, 180, 220), null);
        pdfBtnTD.setToolTipText("Export confirmation HTML → Ctrl+P to save PDF");
        pdfBtnTD.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            exportTestDrivePDF((int) model.getValueAt(row, 0), String.valueOf(model.getValueAt(row, 1)),
                    String.valueOf(model.getValueAt(row, 2)), String.valueOf(model.getValueAt(row, 3)),
                    String.valueOf(model.getValueAt(row, 4)), String.valueOf(model.getValueAt(row, 5)));
        });
        JButton emailBtnTD = glowButton("📧 Email Customer", ACCENT_ORANGE, null);
        emailBtnTD.setToolTipText("Send confirmation email via your email app");
        emailBtnTD.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                showInfo("Select a test drive.");
                return;
            }
            String custName = String.valueOf(model.getValueAt(row, 2));
            emailTestDriveConfirmation(custName, String.valueOf(model.getValueAt(row, 1)),
                    String.valueOf(model.getValueAt(row, 3)), String.valueOf(model.getValueAt(row, 4)));
        });
        actions.add(pdfBtnTD);
        actions.add(emailBtnTD);
        JButton tdRefreshBtn = smallButton("↻ Refresh", TEXT_MUTED);
        tdRefreshBtn.addActionListener(ev -> load.run());
        actions.add(tdRefreshBtn);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(styledScrollPane(table), BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    /** Enhanced "Schedule Test Drive" dialog with email notification checkbox. */
    private void showAddTestDriveDialogEnhanced() {
        JDialog dlg = styledDialog("Schedule Test Drive", 480, 460);
        dlg.setLayout(new BorderLayout());
        dlg.add(buildDialogHeader("🔑", "Schedule Test Drive", ACCENT_ORANGE), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = formGbc();
        List<Car> cars = carController.getAllCars();
        List<models.Customer> custs = customerController.getAllCustomers();
        JComboBox<String> carBox = darkComboBox(
                cars.stream().map(c -> c.getId() + ": " + c.getBrand() + " " + c.getModel()).toArray(String[]::new));
        JComboBox<String> custBox = darkComboBox(
                custs.stream().map(c -> c.getId() + ": " + c.getName()).toArray(String[]::new));
        JTextField dateF = darkTextField("YYYY-MM-DD", 18);
        String[] slots = { "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM",
                "04:00 PM" };
        JComboBox<String> slotBox = darkComboBox(slots);
        JCheckBox sendEmailChk = new JCheckBox("Send confirmation email to customer");
        sendEmailChk.setFont(FONT_LABEL);
        sendEmailChk.setForeground(TEXT_PRIMARY);
        sendEmailChk.setOpaque(false);
        sendEmailChk.setSelected(true);
        sendEmailChk.setEnabled(true);
        sendEmailChk.setToolTipText("Opens your default email app with message ready");
        addFormRow(form, gbc, "Vehicle *", carBox, 0);
        addFormRow(form, gbc, "Customer *", custBox, 1);
        addFormRow(form, gbc, "Date (YYYY-MM-DD) *", dateF, 2);
        addFormRow(form, gbc, "Time Slot", slotBox, 3);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        form.add(sendEmailChk, gbc);
        gbc.gridwidth = 1;
        dlg.add(form, BorderLayout.CENTER);
        JButton cancel = smallButton("Cancel", TEXT_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = glowButton("Schedule Drive", ACCENT_ORANGE, e -> {
            try {
                if (carBox.getItemCount() == 0 || custBox.getItemCount() == 0) {
                    showError("No data.");
                    return;
                }
                int carId = Integer.parseInt(((String) carBox.getSelectedItem()).split(":")[0].trim());
                int custId = Integer.parseInt(((String) custBox.getSelectedItem()).split(":")[0].trim());
                java.sql.Date date = java.sql.Date.valueOf(dateF.getText().trim());
                TestDrive td = new TestDrive();
                td.setCarId(carId);
                td.setCustomerId(custId);
                td.setScheduledDate(date);
                td.setTimeSlot((String) slotBox.getSelectedItem());
                td.setStatus("Scheduled");
                if (testDriveController.addTestDrive(td)) {
                    dlg.dispose();
                    showPanel("TESTDRIVES");
                    showSuccess("Test drive scheduled!");
                    if (sendEmailChk.isSelected()) {
                        int ci = custBox.getSelectedIndex();
                        if (ci >= 0 && ci < custs.size()) {
                            models.Customer cust = custs.get(ci);
                            int carIdx = carBox.getSelectedIndex();
                            String cName = carIdx >= 0 && carIdx < cars.size()
                                    ? cars.get(carIdx).getBrand() + " " + cars.get(carIdx).getModel()
                                    : "Vehicle";
                            emailTestDriveConfirmation(cust.getName(), cName, dateF.getText().trim(),
                                    (String) slotBox.getSelectedItem());
                        }
                    }
                } else
                    showError("Failed to schedule.");
            } catch (Exception ex) {
                showError("Invalid date. Use YYYY-MM-DD");
            }
        });
        dlg.add(buildDialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void doOtpPasswordReset(JPasswordField jPasswordField,
            JPasswordField jPasswordField2, JLabel jLabel, String string, JDialog jDialog, ActionEvent actionEvent) {
        String string2 = new String(jPasswordField.getPassword());
        String string3 = new String(jPasswordField2.getPassword());
        if (string2.length() < 4) {
            jLabel.setText("Password must be at least 4 characters.");
            return;
        }
        if (!string2.equals(string3)) {
            jLabel.setText("Passwords do not match.");
            return;
        }
        if (DatabaseManager.getInstance().resetAppUserPassword(string, string2)) {
            this.showSuccess("\u2705 Password reset for " + string);
            AuditLogger.log("UPDATE", "AppUser " + string, "Password reset by " + SessionManager.getUser());
            jDialog.dispose();
        } else {
            jLabel.setText("Reset failed \u2014 please try again.");
        }
    }

    private void doOtpVerify(JTextField jTextField, boolean[] blArray,
            JDialog jDialog, JLabel jLabel, ActionEvent actionEvent) {
        if (OtpService.verify(SessionManager.getUser(), jTextField.getText().trim())) {
            blArray[0] = true;
            jDialog.dispose();
        } else {
            jLabel.setText("Invalid or expired OTP \u2014 try again.");
        }
    }

    /** Toggle Auth-Token show/hide */
    private void doTokenToggle(JPasswordField passField, JButton btn, java.awt.event.ActionEvent e) {
        if (passField.getEchoChar() == 0) {
            passField.setEchoChar('\u25cf');
            btn.setText("Show");
        } else {
            passField.setEchoChar((char) 0);
            btn.setText("Hide");
        }
    }

    /** Save SMS (Twilio) settings */
    private void doSmsSave(JTextField sidField, JPasswordField tokenField,
            JTextField fromField, JCheckBox enabledBox, JLabel badge, java.awt.event.ActionEvent e) {
        String sid = sidField.getText().trim();
        String token = new String(tokenField.getPassword()).trim();
        String from = fromField.getText().trim();
        if (enabledBox.isSelected() && (sid.isBlank() || token.isBlank() || from.isBlank())) {
            showError("Please fill in Account SID, Auth Token and From Number before enabling SMS.");
            return;
        }
        TwilioService.setEnabled(enabledBox.isSelected());
        TwilioService.setAccountSid(sid);
        TwilioService.setAuthToken(token);
        TwilioService.setFromNumber(from);
        boolean saved = !sid.isBlank() && !token.isBlank() && !from.isBlank();
        badge.setText(saved ? "  \u2713  Credentials saved \u2014 will be restored on next launch"
                : "  \u26a0  No credentials saved yet \u2014 enter and click Save");
        badge.setForeground(saved ? ACCENT_GREEN : ACCENT_ORANGE);
        setStatus("\u2713  Twilio settings saved");
        showSuccess("Settings saved! Credentials stored and will be restored on next launch.");
    }

    /** Clear SMS (Twilio) credentials */
    private void doSmsClear(JTextField sidField, JPasswordField tokenField,
            JTextField fromField, JCheckBox enabledBox, JLabel badge, java.awt.event.ActionEvent e) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Clear all saved Twilio credentials? This cannot be undone.",
                "Clear Credentials", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION)
            return;
        TwilioService.setAccountSid("");
        TwilioService.setAuthToken("");
        TwilioService.setFromNumber("");
        TwilioService.setEnabled(false);
        sidField.setText("");
        tokenField.setText("");
        fromField.setText("");
        enabledBox.setSelected(false);
        badge.setText("  \u26a0  No credentials saved yet \u2014 enter and click Save");
        badge.setForeground(ACCENT_ORANGE);
        setStatus("Twilio credentials cleared");
    }

    /** Send Test SMS */
    private void doSmsTest(JCheckBox enabledBox, JTextField sidField,
            JPasswordField tokenField, JTextField fromField, java.awt.event.ActionEvent e) {
        String sid = sidField.getText().trim();
        String token = new String(tokenField.getPassword()).trim();
        String from = fromField.getText().trim();
        TwilioService.setEnabled(enabledBox.isSelected());
        TwilioService.setAccountSid(sid);
        TwilioService.setAuthToken(token);
        TwilioService.setFromNumber(from);
        String phone = JOptionPane.showInputDialog(this, "Send test to phone (+91xxxxxxxxxx):",
                "Test SMS", JOptionPane.QUESTION_MESSAGE);
        if (phone != null && !phone.isBlank())
            TwilioService.sendSmsAsync(phone, "\u2705 AutoElite SMS test successful!");
    }

    /** Save 2FA settings */
    private void doSave2FA(JCheckBox enabledBox, JTextField phoneField, java.awt.event.ActionEvent e) {
        boolean enabled = enabledBox.isSelected();
        String phone = phoneField.getText().trim();
        if (enabled && phone.isBlank()) {
            showError("Enter a phone number for OTP delivery.");
            return;
        }
        if (DatabaseManager.getInstance().update2fa(SessionManager.getUser(), enabled, phone)) {
            AuditLogger.log("UPDATE", "2FA", enabled
                    ? "Enabled for " + SessionManager.getUser()
                    : "Disabled");
            showSuccess("2FA settings saved.");
        } else {
            showError("Could not save 2FA settings.");
        }
    }

}
