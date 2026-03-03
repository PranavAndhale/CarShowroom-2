package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ModernSidebar — a high-fidelity, animated navigation sidebar for AutoElite.
 *
 * Features:
 * • Deep charcoal gradient background with noise texture overlay
 * • LinearGradientPaint hexagonal 'AE' logo with inner-shadow
 * • Per-item hover-glow animation via javax.swing.Timer (60fps-ish at 16ms)
 * • Active item: neon-cyan 4px vertical accent bar + radial background glow
 * • Animated version badge and DB-status pill at the bottom
 *
 * Integration:
 * Replace your existing buildSidebar() call in buildFrame() with:
 *
 * ModernSidebar sidebar = new ModernSidebar(navButtons, this::showPanel,
 * dbConnected, () -> currentPanel);
 * root.add(sidebar, BorderLayout.WEST);
 */
public class ModernSidebar extends JPanel {

    // ── Palette (mirrors CarShowroomApp constants) ────────────────────────────
    private static final Color BG_TOP = new Color(6, 8, 16);
    private static final Color BG_BOT = new Color(14, 8, 32);
    private static final Color ACCENT_CYAN = new Color(0, 212, 255);
    private static final Color ACCENT_PURP = new Color(138, 43, 226);
    private static final Color ACCENT_GOLD = new Color(255, 200, 60);
    private static final Color TEXT_HI = new Color(230, 240, 255);
    private static final Color TEXT_DIM = new Color(80, 100, 140);
    private static final Color GLOW_CYAN = new Color(0, 212, 255, 35);
    private static final Color BORDER_LINE = new Color(0, 212, 255, 50);

    // ── Nav items ─────────────────────────────────────────────────────────────
    private static final String[][] NAV_ITEMS = {
            { "DASHBOARD", "\u29C1", "Dashboard", "Overview & KPIs" },
            { "CARS", "\uD83D\uDE97", "Inventory", "Fleet management" },
            { "CUSTOMERS", "\uD83D\uDC64", "Customers", "Client profiles" },
            { "EMPLOYEES", "\uD83D\uDC54", "Team", "Staff & payroll" },
            { "SALES", "\uD83D\uDCB0", "Sales", "Revenue & invoices" },
            { "COMMISSIONS", "\uD83C\uDFC6", "Commissions", "Earnings & payouts" },
            { "TESTDRIVES", "\uD83D\uDD11", "Test Drives", "Booking scheduler" },
            { "DEALERSHIPS", "\uD83D\uDCCD", "Locations", "Dealership map" },
            { "REVIEWS", "\u2B50", "Reviews", "Customer feedback" },
            { "REPORTS", "\uD83D\uDCCA", "Reports", "Analytics & PDF" },
            { "AUDIT", "\uD83D\uDD0D", "Audit Trail", "Admin audit log" },
            { "SETTINGS", "\u2699", "Settings", "Config & SMS" },
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final Map<String, JButton> navButtons;
    private final Consumer<String> onNavigate;
    private final java.util.function.Supplier<String> currentPanelGetter;
    private final boolean dbConnected;

    // Per-nav-item hover-alpha (0.0 → 1.0) used by the glow animation
    private final float[] hoverAlpha = new float[NAV_ITEMS.length];
    // Background noise texture, generated once
    private BufferedImage noiseTexture;
    private Timer animTimer;

    // ── Constructor ───────────────────────────────────────────────────────────
    public ModernSidebar(Map<String, JButton> navButtonsRef,
            Consumer<String> onNavigate,
            boolean dbConnected,
            java.util.function.Supplier<String> currentPanelGetter) {
        this.navButtons = navButtonsRef;
        this.onNavigate = onNavigate;
        this.dbConnected = dbConnected;
        this.currentPanelGetter = currentPanelGetter;

        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 0));
        setMinimumSize(new Dimension(230, 0));

        noiseTexture = buildNoiseTexture(230, 900);

        add(buildLogo(), BorderLayout.NORTH);
        add(buildNav(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        startAnimationTimer();
    }

    // ── Background painting ───────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int W = getWidth(), H = getHeight();

        // Deep gradient body
        LinearGradientPaint bg = new LinearGradientPaint(
                0, 0, 0, H,
                new float[] { 0f, 0.5f, 1f },
                new Color[] { BG_TOP, new Color(10, 8, 24), BG_BOT });
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        // Subtle noise overlay for depth/texture
        if (noiseTexture != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
            for (int tx = 0; tx < W; tx += noiseTexture.getWidth())
                for (int ty = 0; ty < H; ty += noiseTexture.getHeight())
                    g2.drawImage(noiseTexture, tx, ty, null);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // Purple radial ambient at top-left corner
        RadialGradientPaint ambient = new RadialGradientPaint(
                0, 0, 280,
                new float[] { 0f, 1f },
                new Color[] { new Color(138, 43, 226, 40), new Color(0, 0, 0, 0) });
        g2.setPaint(ambient);
        g2.fillRect(0, 0, W, H);

        // Right-edge glow border
        g2.setComposite(AlphaComposite.SrcOver);
        GradientPaint border = new GradientPaint(W - 8, 0, new Color(0, 212, 255, 0),
                W, 0, new Color(0, 212, 255, 60));
        g2.setPaint(border);
        g2.fillRect(W - 8, 0, 8, H);
        g2.setColor(BORDER_LINE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(W - 1, 0, W - 1, H);

        g2.dispose();
    }

    // ── Logo section ──────────────────────────────────────────────────────────
    private JPanel buildLogo() {
        // Load the logo once outside the render loop
        java.awt.Image loadedLogo = null;
        try {
            java.net.URL logoUrl = ui.CarShowroomApp.class.getClassLoader().getResource("autoelite_logo.png");
            if (logoUrl == null) {
                java.io.File logoFile = new java.io.File("src/resources/autoelite_logo.png");
                logoUrl = logoFile.exists() ? logoFile.toURI().toURL() : null;
            }
            if (logoUrl != null) {
                ImageIcon rawIcon = new ImageIcon(logoUrl);
                loadedLogo = rawIcon.getImage().getScaledInstance(45, 45, java.awt.Image.SCALE_SMOOTH);
                // Force sync load
                new ImageIcon(loadedLogo);
            }
        } catch (Exception ignored) {
        }

        final java.awt.Image finalLogo = loadedLogo;

        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int W = getWidth(), H = getHeight();
                g2.setColor(new Color(0, 0, 0, 0)); // transparent

                // ── AutoElite Logo Image ──────────────────────────────────────
                if (finalLogo != null) {
                    g2.drawImage(finalLogo, 18, 12, null);
                }

                // Bottom separator line
                g2.setColor(new Color(0, 212, 255, 25));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(16, H - 1, W - 16, H - 1);

                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(230, 86));

        // Brand text labels
        JLabel brand = new JLabel("AutoElite");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brand.setForeground(ACCENT_CYAN);
        brand.setBounds(76, 22, 140, 22);
        panel.add(brand);

        JLabel sub = new JLabel("PREMIUM SHOWROOM");
        sub.setFont(new Font("Segoe UI", Font.BOLD, 9));
        sub.setForeground(new Color(80, 120, 160));
        sub.setBounds(77, 44, 140, 14);
        panel.add(sub);

        return panel;
    }

    // ── Navigation section ────────────────────────────────────────────────────
    private JPanel buildNav() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(new EmptyBorder(8, 0, 8, 0));

        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final int idx = i;
            final String key = NAV_ITEMS[i][0];
            final String icon = NAV_ITEMS[i][1];
            final String label = NAV_ITEMS[i][2];
            final String sublbl = NAV_ITEMS[i][3];

            SidebarNavItem item = new SidebarNavItem(key, icon, label, sublbl, idx);
            navButtons.put(key, item); // Register so CarShowroomApp can repaint it
            container.add(item);
            container.add(Box.createVerticalStrut(2));
        }

        return container;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 212, 255, 20));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(16, 0, getWidth() - 16, 0);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(10, 14, 14, 14));

        // DB status pill
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = dbConnected ? new Color(57, 255, 20, 30) : new Color(255, 60, 60, 30);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                Color bc = dbConnected ? new Color(57, 255, 20, 80) : new Color(255, 60, 60, 80);
                g2.setColor(bc);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setMaximumSize(new Dimension(200, 28));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        dot.setForeground(dbConnected ? new Color(57, 255, 20) : new Color(255, 80, 80));

        JLabel status = new JLabel(dbConnected ? "Connected" : "Offline Mode");
        status.setFont(new Font("Segoe UI", Font.BOLD, 11));
        status.setForeground(dbConnected ? new Color(57, 255, 20) : new Color(255, 80, 80));

        pill.add(dot);
        pill.add(status);
        footer.add(pill);
        footer.add(Box.createVerticalStrut(6));

        // Version tag
        JLabel version = new JLabel("AutoElite v17  ·  Java Swing");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        version.setForeground(new Color(50, 70, 100));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(version);

        return footer;
    }

    // ── Animation timer ───────────────────────────────────────────────────────
    private void startAnimationTimer() {
        animTimer = new Timer(16, e -> repaintNavItems());
        animTimer.setCoalesce(true);
        animTimer.start();
    }

    private void repaintNavItems() {
        // Repaint all SidebarNavItem children in the nav panel
        Component navPanel = getComponent(1); // CENTER = index 1
        if (navPanel instanceof JPanel) {
            for (Component c : ((JPanel) navPanel).getComponents()) {
                if (c instanceof SidebarNavItem)
                    c.repaint();
            }
        }
    }

    /** Stop the animation timer (call when window closes). */
    public void dispose() {
        if (animTimer != null)
            animTimer.stop();
    }

    // ── Noise texture ─────────────────────────────────────────────────────────
    private BufferedImage buildNoiseTexture(int w, int h) {
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        java.util.Random rng = new java.util.Random(42);
        for (int y = 0; y < 128; y++)
            for (int x = 0; x < 128; x++) {
                int v = rng.nextInt(255);
                img.setRGB(x, y, new Color(v, v, v, 8).getRGB());
            }
        return img;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inner class: SidebarNavItem (individual animated nav button)
    // ══════════════════════════════════════════════════════════════════════════
    private class SidebarNavItem extends JButton {
        private final String panelKey;
        private final String iconText;
        private final String labelText;
        private final String subText;
        private final int itemIndex;

        // Smooth animation state
        private float hoverAlpha = 0f;
        private float targetAlpha = 0f;
        private boolean hovered = false;

        SidebarNavItem(String panelKey, String iconText, String labelText,
                String subText, int itemIndex) {
            this.panelKey = panelKey;
            this.iconText = iconText;
            this.labelText = labelText;
            this.subText = subText;
            this.itemIndex = itemIndex;

            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setText("");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(230, 54));
            setPreferredSize(new Dimension(230, 54));
            setMinimumSize(new Dimension(230, 54));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    targetAlpha = 1f;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    targetAlpha = 0f;
                }
            });

            addActionListener(e -> onNavigate.accept(panelKey));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int W = getWidth(), H = getHeight();
            boolean active = panelKey.equals(currentPanelGetter.get());

            // Smooth alpha transition
            float speed = 0.12f;
            hoverAlpha += (targetAlpha - hoverAlpha) * speed;

            // ── Active glow background ────────────────────────────────────────
            if (active) {
                // Full-width radial glow
                RadialGradientPaint activeGlow = new RadialGradientPaint(
                        W * 0.5f, H * 0.5f, W * 0.75f,
                        new float[] { 0f, 1f },
                        new Color[] { new Color(0, 212, 255, 45), new Color(0, 0, 0, 0) });
                g2.setPaint(activeGlow);
                g2.fillRoundRect(4, 2, W - 8, H - 4, 10, 10);

                // Solid bg strip
                LinearGradientPaint strip = new LinearGradientPaint(
                        0, 0, W, 0,
                        new float[] { 0f, 0.6f, 1f },
                        new Color[] { new Color(0, 212, 255, 55), new Color(80, 43, 180, 25), new Color(0, 0, 0, 0) });
                g2.setPaint(strip);
                g2.fillRoundRect(4, 2, W - 8, H - 4, 10, 10);

            } else if (hoverAlpha > 0.01f) {
                // Hover glow
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverAlpha * 0.5f));
                RadialGradientPaint hgl = new RadialGradientPaint(
                        W * 0.5f, H * 0.5f, W * 0.6f,
                        new float[] { 0f, 1f },
                        new Color[] { new Color(0, 212, 255, 30), new Color(0, 0, 0, 0) });
                g2.setPaint(hgl);
                g2.fillRoundRect(4, 2, W - 8, H - 4, 10, 10);
                g2.setComposite(AlphaComposite.SrcOver);
            }

            // ── Neon cyan vertical accent bar ─────────────────────────────────
            if (active) {
                // Multi-layer glow bar
                for (int gl = 8; gl >= 2; gl -= 2) {
                    int a = (int) (255 * (1f - (float) gl / 8f) * 0.6f);
                    g2.setColor(new Color(0, 212, 255, a));
                    g2.setStroke(new BasicStroke(gl, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(2, 8, 2, H - 8);
                }
                // Solid cyan bar
                g2.setColor(ACCENT_CYAN);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(3, 8, 3, H - 8);

                // Bright tip dots
                g2.setColor(new Color(180, 240, 255));
                g2.fillOval(1, 7, 5, 5);
                g2.fillOval(1, H - 12, 5, 5);
            }

            // ── Icon ──────────────────────────────────────────────────────────
            int iconX = 20, iconY = H / 2;
            Color iconColor = active ? ACCENT_CYAN : (hovered ? new Color(160, 200, 240) : TEXT_DIM);

            // Glow behind icon when active
            if (active) {
                g2.setColor(new Color(0, 212, 255, 50));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 20));
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        g2.drawString(iconText, iconX + dx, iconY + 6 + dy);
            }

            g2.setColor(iconColor);
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
            FontMetrics emFm = g2.getFontMetrics();
            g2.drawString(iconText, iconX, iconY + emFm.getAscent() / 2 - 1);

            // ── Label ─────────────────────────────────────────────────────────
            int textX = 52;
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(active ? TEXT_HI : (hoverAlpha > 0.3f ? new Color(180, 200, 240) : new Color(130, 150, 190)));
            g2.drawString(labelText, textX, H / 2 - 2);

            // Sub-label
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(active ? new Color(80, 160, 200) : new Color(55, 75, 110));
            g2.drawString(subText, textX, H / 2 + 13);

            // ── Active "LIVE" dot indicator ───────────────────────────────────
            if (active) {
                int dotX = W - 22, dotY = H / 2 - 4;
                // Pulsing glow (uses system time for animation)
                double pulse = 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 500.0);
                g2.setColor(new Color(0, 212, 255, (int) (pulse * 60)));
                g2.fillOval(dotX - 4, dotY - 4, 16, 16);
                g2.setColor(ACCENT_CYAN);
                g2.fillOval(dotX, dotY, 8, 8);
            }

            g2.dispose();
        }
    }
}