package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final Properties properties = new Properties();

    static {
        try {
            // Load the external properties file from the root directory
            properties.load(new FileInputStream("application.properties"));
            System.out.println("ConfigManager: Loaded application.properties successfully.");
        } catch (IOException e) {
            System.err.println("ConfigManager: Warning - application.properties not found. Using safe defaults.");
        }
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
