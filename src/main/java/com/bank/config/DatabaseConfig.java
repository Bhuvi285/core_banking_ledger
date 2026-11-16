package com.bank.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {
    private final String url;
    private final String username;
    private final String password;
    private final String driver;

    private DatabaseConfig(String url, String username, String password, String driver) {
        this.url = requireValue("DB_URL", url);
        this.username = requireValue("DB_USERNAME", username);
        this.password = requireValue("DB_PASSWORD", password);
        this.driver = requireValue("db.driver", driver);
    }

    public static DatabaseConfig load() {
        Dotenv dotenv = Dotenv.configure()
                .directory(findEnvironmentDirectory().toString())
                .ignoreIfMissing()
                .load();
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties is missing from the application classpath.");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read database configuration.", exception);
        }

        return new DatabaseConfig(
                configuredValue("DB_URL", dotenv, properties),
                configuredValue("DB_USERNAME", dotenv, properties),
                configuredValue("DB_PASSWORD", dotenv, properties),
                properties.getProperty("db.driver"));
    }

    private static Path findEnvironmentDirectory() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path candidate = directory; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve(".env"))) {
                return candidate;
            }
        }
        return directory;
    }

    public static DatabaseConfig fromValues(String url, String username, String password) {
        return new DatabaseConfig(url, username, password, "org.postgresql.Driver");
    }

    private static String configuredValue(String name, Dotenv dotenv, Properties properties) {
        String systemValue = System.getenv(name);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String dotenvValue = dotenv.get(name);
        if (dotenvValue != null && !dotenvValue.isBlank()) {
            return dotenvValue;
        }
        String propertyValue = properties.getProperty("db." + name.substring(3).toLowerCase());
        if (propertyValue != null && propertyValue.startsWith("${") && propertyValue.endsWith("}")) {
            return null;
        }
        return propertyValue;
    }

    private static String requireValue(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required database configuration: " + name
                    + ". Set it in the environment or project-root .env file.");
        }
        return value;
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDriver() { return driver; }
}
