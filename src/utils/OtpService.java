package utils;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates and verifies 6-digit OTPs for two-factor authentication.
 * OTPs expire after 5 minutes. Delivered via Twilio SMS.
 */
public class OtpService {

    private static final SecureRandom rng = new SecureRandom();
    private static final Map<String, long[]> otpStore = new HashMap<>();
    private static final long EXPIRY_MS = 5 * 60 * 1000L;

    private OtpService() {
    }

    /**
     * Generates a 6-digit OTP for {@code username}, stores it with a 5-min expiry,
     * and sends it via Twilio SMS to {@code phone}.
     *
     * @return the OTP string (so callers can display it if SMS is not configured)
     */
    public static String generateAndSend(String username, String phone) {
        int code = 100_000 + rng.nextInt(900_000);
        long expiry = System.currentTimeMillis() + EXPIRY_MS;
        otpStore.put(username, new long[] { code, expiry });

        String msg = "AutoElite 2FA: Your login OTP is " + code +
                ". Valid for 5 minutes. Do not share this code.";
        TwilioService.sendSmsAsync(phone, msg);
        System.out.println("[2FA] OTP generated for " + username + " → " + code);
        return String.valueOf(code);
    }

    /**
     * Verifies the OTP entered by the user.
     * Returns true on match (OTP is consumed); false if wrong or expired.
     */
    public static boolean verify(String username, String code) {
        if (!otpStore.containsKey(username))
            return false;
        long[] data = otpStore.get(username);
        if (System.currentTimeMillis() > data[1]) {
            otpStore.remove(username);
            return false; // expired
        }
        if (String.valueOf((long) data[0]).equals(code.trim())) {
            otpStore.remove(username);
            return true;
        }
        return false;
    }

    /** Returns remaining validity in milliseconds (0 if expired / not found). */
    public static long getRemainingMs(String username) {
        if (!otpStore.containsKey(username))
            return 0;
        return Math.max(0L, otpStore.get(username)[1] - System.currentTimeMillis());
    }

    public static boolean hasPending(String username) {
        return otpStore.containsKey(username) &&
                System.currentTimeMillis() < otpStore.get(username)[1];
    }
}
