package me.devamy.contracts.utils;

import net.kyori.adventure.text.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

import static me.devamy.contracts.Contracts.plugin;

public class Log {
    private static final Logger logger = Logger.getLogger("Dcontracts");

    public static void error(String message, Throwable exception) {
        logger.log(Level.SEVERE, message, exception);
    }

    public static void info(String message) {
        logger.log(Level.INFO, message);
    }

    public static void info(Component message) {
        plugin.getComponentLogger().info(message);
    }

    public static void warn(String message) {
        logger.log(Level.WARNING, message);
    }

    public static void severe(String message) { logger.log(Level.SEVERE, message); }
}
