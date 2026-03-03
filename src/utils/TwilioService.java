package utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * Lightweight Twilio SMS client — uses only java.net.HttpURLConnection,
 * no third-party JARs required.
 *
 * Credentials are stored in Java Preferences (per-user, OS keyring-backed
 * on macOS) and can be set from the Settings panel.
 */
public class TwilioService {

    private static final String PREF_NODE = "com.autoelite.twilio";
    private static final Preferences prefs = Preferences.userRoot().node(PREF_NODE);

    // ── Preference keys ──────────────────────────────────────────────────────
    private static final String KEY_SID = "account_sid";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_FROM = "from_number";
    private static final String KEY_ENABLED = "enabled";

    // ── Getters / setters (called by Settings panel) ─────────────────────────
    public static String getAccountSid() {
        return prefs.get(KEY_SID, "");
    }

    public static String getAuthToken() {
        return prefs.get(KEY_TOKEN, "");
    }

    public static String getFromNumber() {
        return prefs.get(KEY_FROM, "");
    }

    public static boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public static void setAccountSid(String v) {
        prefs.put(KEY_SID, v);
    }

    public static void setAuthToken(String v) {
        prefs.put(KEY_TOKEN, v);
    }

    public static void setFromNumber(String v) {
        prefs.put(KEY_FROM, v);
    }

    public static void setEnabled(boolean v) {
        prefs.putBoolean(KEY_ENABLED, v);
    }

    /**
     * Sends a plain SMS to {@code toNumber}.
     * Returns {@code true} on success (HTTP 2xx), {@code false} otherwise.
     * All errors are logged to stderr — never throws.
     *
     * Runs asynchronously on a daemon thread so the UI is never blocked.
     */
    public static void sendSmsAsync(String toNumber, String message) {
        if (!isEnabled())
            return;
        String sid = getAccountSid();
        String token = getAuthToken();
        String from = getFromNumber();
        if (sid.isBlank() || token.isBlank() || from.isBlank() || toNumber == null || toNumber.isBlank()) {
            System.err.println("[Twilio] SMS skipped — credentials or destination not configured.");
            return;
        }
        Thread t = new Thread(() -> sendSmsSync(sid, token, from, toNumber, message), "twilio-sms");
        t.setDaemon(true);
        t.start();
    }

    private static boolean sendSmsSync(String sid, String token, String from,
                                       String to, String body) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json";
            String creds = Base64.getEncoder().encodeToString(
                    (sid + ":" + token).getBytes(StandardCharsets.UTF_8));

            String params = "From=" + URLEncoder.encode(from, StandardCharsets.UTF_8)
                    + "&To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Basic " + creds);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("[Twilio] SMS sent → " + to);
                return true;
            } else {
                InputStream err = conn.getErrorStream();
                String resp = err != null ? new String(err.readAllBytes()) : "(no body)";
                System.err.println("[Twilio] HTTP " + code + " → " + resp);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[Twilio] Error: " + e.getMessage());
            return false;
        }
    }

    // ── Convenience message builders ─────────────────────────────────────────

    public static void notifyTestDriveBooked(String toPhone, String customerName,
                                             String carBrand, String carModel,
                                             String date, String timeSlot) {
        String msg = "Hi " + customerName + "! Your test drive for the "
                + carBrand + " " + carModel + " is confirmed on "
                + date + " at " + timeSlot
                + ". — AutoElite Management";
        sendSmsAsync(toPhone, msg);
    }

    public static void notifySaleCompleted(String toPhone, String customerName,
                                           String carBrand, String carModel,
                                           double salePrice) {
        String msg = "Congratulations " + customerName + "! 🎉 Your purchase of the "
                + carBrand + " " + carModel
                + " is complete. Total: $" + String.format("%,.0f", salePrice)
                + ". Thank you for choosing AutoElite!";
        sendSmsAsync(toPhone, msg);
    }
}