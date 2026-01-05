package com.mmog;

import java.io.*;
import java.util.Properties;

/**
 * Configuration manager for game settings.
 * Allows server address to be configured without recompilation.
 */
public class GameConfig {
    private static final String CONFIG_FILE = "game.properties";
    private static Properties properties = new Properties();

    // Default values
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 7077;
    private static final boolean DEFAULT_USE_RELAY = true;
    private static final String DEFAULT_RELAY_URL = "wss://betweenus-iuh6.onrender.com";

    private static String serverHost = DEFAULT_SERVER_HOST;
    private static int serverPort = DEFAULT_SERVER_PORT;
    private static boolean useRelay = DEFAULT_USE_RELAY;
    private static String relayUrl = DEFAULT_RELAY_URL;

    static {
        loadConfig();
    }

    /**
     * Load configuration from file, or create default if not exists
     */
    public static void loadConfig() {
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                serverHost = properties.getProperty("server.host", DEFAULT_SERVER_HOST);
                serverPort = Integer.parseInt(properties.getProperty("server.port", String.valueOf(DEFAULT_SERVER_PORT)));
                useRelay = Boolean.parseBoolean(properties.getProperty("use.relay", String.valueOf(DEFAULT_USE_RELAY)));
                relayUrl = properties.getProperty("relay.url", DEFAULT_RELAY_URL);
                System.out.println("Loaded config: host=" + serverHost + ", port=" + serverPort + ", useRelay=" + useRelay);
            } catch (IOException e) {
                System.out.println("Could not load config, using defaults: " + e.getMessage());
                createDefaultConfig();
            }
        } else {
            createDefaultConfig();
        }
    }

    /**
     * Create default configuration file
     */
    private static void createDefaultConfig() {
        properties.setProperty("server.host", DEFAULT_SERVER_HOST);
        properties.setProperty("server.port", String.valueOf(DEFAULT_SERVER_PORT));
        properties.setProperty("use.relay", String.valueOf(DEFAULT_USE_RELAY));
        properties.setProperty("relay.url", DEFAULT_RELAY_URL);
        saveConfig();
    }

    /**
     * Save current configuration to file
     */
    public static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.setProperty("server.host", serverHost);
            properties.setProperty("server.port", String.valueOf(serverPort));
            properties.setProperty("use.relay", String.valueOf(useRelay));
            properties.setProperty("relay.url", relayUrl);
            properties.store(fos, "BetweenUs Game Configuration");
            System.out.println("Config saved successfully");
        } catch (IOException e) {
            System.out.println("Could not save config: " + e.getMessage());
        }
    }

    // Getters
    public static String getServerHost() {
        return serverHost;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static boolean useRelay() {
        return useRelay;
    }

    public static String getRelayUrl() {
        return relayUrl;
    }

    // Setters
    public static void setServerHost(String host) {
        serverHost = host;
        properties.setProperty("server.host", host);
    }

    public static void setServerPort(int port) {
        serverPort = port;
        properties.setProperty("server.port", String.valueOf(port));
    }

    public static void setUseRelay(boolean use) {
        useRelay = use;
        properties.setProperty("use.relay", String.valueOf(use));
    }

    public static void setRelayUrl(String url) {
        relayUrl = url;
        properties.setProperty("relay.url", url);
    }
}
