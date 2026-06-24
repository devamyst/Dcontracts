package me.devamy.contracts.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.devamy.contracts.obj.StorageMethod;
import me.devamy.contracts.utils.Log;

import java.io.File;

import static me.devamy.contracts.Contracts.plugin;

public class DatabaseConfig {

    private static DatabaseConfig instance;
    private final ConfigFile configFile;

    public StorageMethod storageMethod;
    public String host;
    public int port;
    public String database;
    public String username;
    public String password;
    public int maximumPoolSize;
    public int minimumIdle;
    public long connectionTimeout;
    public long idleTimeout;
    public long maxLifetime;

    private DatabaseConfig() {
        File file = new File(plugin.getDataFolder(), "database.yml");
        try {
            configFile = ConfigFile.loadConfig(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database.yml", e);
        }

        if (configFile.isNew()) {
            setDefaults();
            try {
                configFile.save();
            } catch (Exception e) {
                Log.error("Failed to save database.yml", e);
            }
        } else if (!ConfigMigrator.findMissingKeys("default-database.yml", file).isEmpty()) {
            setDefaults();
            try {
                configFile.save();
            } catch (Exception e) {
                Log.error("Failed to save database.yml", e);
            }
        }
        load();
    }

    public static synchronized DatabaseConfig get() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public static void reload() {
        instance = new DatabaseConfig();
    }

    private void setDefaults() {
        configFile.addDefault("storage-method", "SQLITE", "Storage backend: SQLITE, MYSQL, or H2");
        configFile.addDefault("host", "localhost", "Database host (MySQL/H2 only)");
        configFile.addDefault("port", 3306, "Database port (MySQL/H2 only)");
        configFile.addDefault("database", "dcontracts", "Database name (MySQL/H2 only)");
        configFile.addDefault("username", "root", "Database username (MySQL/H2 only)");
        configFile.addDefault("password", "", "Database password (MySQL/H2 only)");
        configFile.addDefault("pool-settings.maximum-pool-size", 10, "Maximum connections in the HikariCP pool");
        configFile.addDefault("pool-settings.minimum-idle", 2, "Minimum idle connections to maintain");
        configFile.addDefault("pool-settings.connection-timeout", 5000, "Maximum wait (ms) for a connection from the pool");
        configFile.addDefault("pool-settings.idle-timeout", 600000, "Maximum time (ms) a connection may sit idle");
        configFile.addDefault("pool-settings.max-lifetime", 1800000, "Maximum lifetime (ms) of a connection in the pool");
    }

    private void load() {
        String method = configFile.getString("storage-method", "SQLITE");
        try {
            this.storageMethod = StorageMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.warn("Invalid storage-method '" + method + "'. Defaulting to SQLITE.");
            this.storageMethod = StorageMethod.SQLITE;
        }

        this.host = configFile.getString("host", "localhost");
        this.port = configFile.getInteger("port", 3306);
        this.database = configFile.getString("database", "dcontracts");
        this.username = configFile.getString("username", "root");
        this.password = configFile.getString("password", "");
        this.maximumPoolSize = configFile.getInteger("pool-settings.maximum-pool-size", 10);
        this.minimumIdle = configFile.getInteger("pool-settings.minimum-idle", 2);
        this.connectionTimeout = configFile.getLong("pool-settings.connection-timeout", 5000);
        this.idleTimeout = configFile.getLong("pool-settings.idle-timeout", 600000);
        this.maxLifetime = configFile.getLong("pool-settings.max-lifetime", 1800000);
    }

    public String getJdbcUrl() {
        return switch (storageMethod) {
            case SQLITE -> "jdbc:sqlite:" + new File(plugin.getDataFolder(), "data.db").getAbsolutePath();
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8";
            case H2 -> "jdbc:h2:" + new File(plugin.getDataFolder(), "data-h2").getAbsolutePath() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        };
    }

    public boolean useAuth() {
        return storageMethod != StorageMethod.SQLITE;
    }
}
