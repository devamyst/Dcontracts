package me.devamy.contracts.utils;

import org.bukkit.Bukkit;

import java.util.concurrent.ForkJoinPool;

public final class ServerSoftware {

    public enum Type {
        FOLIA,
        PURPUR,
        PUFFERFISH,
        PAPER,
        UNKNOWN
    }

    private static final Type DETECTED;
    private static final boolean PARALLEL_SUPPORTED;

    static {
        DETECTED = detect();
        PARALLEL_SUPPORTED = DETECTED != Type.UNKNOWN;
    }

    private static Type detect() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return Type.FOLIA;
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            return Type.PURPUR;
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("io.pufferfish.pufferfish.PufferfishConfig");
            return Type.PUFFERFISH;
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return Type.PAPER;
        } catch (ClassNotFoundException ignored) {}

        return Type.UNKNOWN;
    }

    public static Type getType() {
        return DETECTED;
    }

    public static boolean isParallelSupported() {
        return PARALLEL_SUPPORTED;
    }

    public static String getDisplayName() {
        return switch (DETECTED) {
            case FOLIA -> "Folia";
            case PURPUR -> "Purpur";
            case PUFFERFISH -> "Pufferfish";
            case PAPER -> "Paper";
            case UNKNOWN -> Bukkit.getName() + " " + Bukkit.getVersion();
        };
    }

    public static void logStatus() {
        if (PARALLEL_SUPPORTED) {
            Log.info("Detected " + getDisplayName() + " — parallel processing is available");
        } else {
            Log.info("Detected " + getDisplayName() + " — parallel processing is not available");
        }
    }
}
