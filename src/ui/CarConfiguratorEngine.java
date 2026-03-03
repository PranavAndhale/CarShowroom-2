package ui;

import models.Car;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * CarConfiguratorEngine — Launches the Sketchfab-powered web configurator in
 * the browser,
 * passing the selected car's details (model ID, brand, price, etc.) as URL
 * parameters.
 */
public class CarConfiguratorEngine extends JPanel {

    private static final Color BG_DARK = new Color(6, 8, 16);
    private static final Color ACCENT_CYAN = new Color(0, 212, 255);
    private static final Color ACCENT_PURP = new Color(138, 43, 226);

    // Default Sketchfab model ID used when no car-specific model is set
    private static final String DEFAULT_SKETCHFAB_ID = "58a9995cc9f94878aa28d00e3f77b26a"; // BMW 3 Series 2019

    private Car carData;

    public CarConfiguratorEngine(Car car) {
        this.carData = car;
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setOpaque(true);
        add(buildLaunchPanel(), BorderLayout.CENTER);
    }

    private JPanel buildLaunchPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                g2.setPaint(new RadialGradientPaint(W * .5f, H * .4f, Math.max(W, H) * .75f,
                        new float[] { 0f, .55f, 1f },
                        new Color[] { new Color(22, 18, 45), new Color(10, 8, 22), new Color(4, 4, 10) }));
                g2.fillRect(0, 0, W, H);
                g2.setPaint(new RadialGradientPaint(W * .85f, H * .12f, W * .35f,
                        new float[] { 0f, 1f }, new Color[] { new Color(0, 212, 255, 22), new Color(0, 0, 0, 0) }));
                g2.fillRect(0, 0, W, H);
                g2.setPaint(new RadialGradientPaint(W * .1f, H * .88f, W * .28f,
                        new float[] { 0f, 1f }, new Color[] { new Color(138, 43, 226, 18), new Color(0, 0, 0, 0) }));
                g2.fillRect(0, 0, W, H);
                int fy = (int) (H * .65);
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, fy, W, H - fy);
                g2.setStroke(new BasicStroke(.5f));
                for (int i = -14; i <= 14; i++) {
                    int a = Math.max(0, 22 - Math.abs(i) * 2);
                    g2.setColor(new Color(0, 180, 220, a));
                    g2.drawLine(W / 2 + i * 70, H, W / 2 + i * 14, fy);
                }
                g2.setPaint(new LinearGradientPaint(0, 0, W, 0, new float[] { 0f, .3f, .5f, .7f, 1f },
                        new Color[] { new Color(0, 0, 0, 0), new Color(0, 180, 220, 35), new Color(0, 212, 255, 70),
                                new Color(0, 180, 220, 35), new Color(0, 0, 0, 0) }));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, fy, W, fy);
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 16, 0);

        JLabel icon = new JLabel("\u2b21");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 68));
        icon.setForeground(ACCENT_CYAN);
        panel.add(icon, gbc);

        JLabel title = new JLabel(
                "<html><center><span style='color:#00D4FF;font-size:22px;font-weight:800;letter-spacing:4px'>3D CAR CONFIGURATOR</span></center></html>");
        panel.add(title, gbc);

        JLabel sub = new JLabel(
                "<html><center><span style='color:#5A7098;font-size:13px'>Real car models \u00b7 Real-time colour & options \u00b7 Powered by Sketchfab</span></center></html>");
        panel.add(sub, gbc);

        if (carData != null) {
            String sketchfabId = carData.getSketchfabModelId();
            String modelStatus = (sketchfabId != null && !sketchfabId.isBlank())
                    ? "<span style='color:#39FF14;font-size:10px'>\u25cf Real 3D model available</span>"
                    : "<span style='color:#FF8C00;font-size:10px'>\u25cb Using default model &mdash; add Sketchfab ID in Inventory to customise</span>";
            JLabel carInfo = new JLabel("<html><center><span style='color:#E6F0FF;font-size:14px;font-weight:600'>"
                    + carData.getBrand() + " " + carData.getModel() + " " + carData.getYear()
                    + "</span><br>" + modelStatus + "</center></html>");
            panel.add(carInfo, gbc);
        }

        panel.add(Box.createVerticalStrut(6), gbc);

        JButton btn = createGlowButton("  \u2b21  OPEN 3D CONFIGURATOR  \u2192");
        btn.addActionListener(e -> launchConfigurator());
        panel.add(btn, gbc);

        JLabel hint = new JLabel(
                "<html><center><span style='color:#3A4A60;font-size:10px'>Opens in your browser \u2014 Chrome or Firefox recommended</span></center></html>");
        panel.add(hint, gbc);
        return panel;
    }

    private void launchConfigurator() {
        try {
            File f = new File("web-configurator/index.html");
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this,
                        "Web configurator not found at:\n" + f.getAbsolutePath(),
                        "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            StringBuilder q = new StringBuilder();
            q.append("?model=").append(enc(resolveSketchfabId()));
            if (carData != null) {
                q.append("&brand=").append(enc(carData.getBrand()));
                q.append("&car=").append(enc(carData.getModel()));
                q.append("&year=").append(carData.getYear());
                q.append("&cat=").append(enc(carData.getCategory()));
                q.append("&fuel=").append(enc(carData.getFuelType() != null ? carData.getFuelType() : "Gasoline"));
                q.append("&trans=").append(enc(carData.getTransmission() != null ? carData.getTransmission() : "Auto"));
                q.append("&engine=").append(carData.getEngineSize() > 0 ? carData.getEngineSize() + "L" : "N/A");
                q.append("&stock=").append(carData.getStock());
                q.append("&price=").append((long) carData.getPrice());
            }
            java.net.URI uri = new java.net.URI(f.toURI().toString() + q);
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open browser:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String resolveSketchfabId() {
        if (carData != null) {
            String sid = carData.getSketchfabModelId();
            if (sid != null && !sid.isBlank())
                return sid;
        }
        return DEFAULT_SKETCHFAB_ID;
    }

    private String enc(String s) {
        if (s == null)
            return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private JButton createGlowButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = hovered ? ACCENT_CYAN : new Color(0, 212, 255, 200);
                Color c2 = hovered ? ACCENT_PURP : new Color(138, 43, 226, 200);
                g2.setPaint(
                        new LinearGradientPaint(0, 0, getWidth(), 0, new float[] { 0f, 1f }, new Color[] { c1, c2 }));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 28));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 14, 14);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(320, 52));
        btn.setOpaque(false);
        return btn;
    }
}