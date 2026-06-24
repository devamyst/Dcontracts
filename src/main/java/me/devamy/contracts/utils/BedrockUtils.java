package me.devamy.contracts.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

// GeyserMC/Floodgate helpers. Safe to call even if neither is installed.
public class BedrockUtils {

    private static final AtomicBoolean geyserChecked = new AtomicBoolean(false);
    private static final AtomicBoolean floodgateChecked = new AtomicBoolean(false);
    private static volatile boolean hasGeyser = false;
    private static volatile boolean hasFloodgate = false;
    private static volatile Object floodgateApiInstance = null;
    private static volatile java.lang.reflect.Method isFloodgatePlayerMethod = null;
    private static volatile java.lang.reflect.Method getFloodgatePlayerMethod = null;
    private static volatile java.lang.reflect.Method getUsernameMethod = null;

    private static void checkGeyser() {
        if (!geyserChecked.get()) {
            synchronized (BedrockUtils.class) {
                if (!geyserChecked.get()) {
                    hasGeyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;
                    geyserChecked.set(true);
                }
            }
        }
    }

    private static void checkFloodgate() {
        if (!floodgateChecked.get()) {
            synchronized (BedrockUtils.class) {
                if (!floodgateChecked.get()) {
                    try {
                        hasFloodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
                        if (hasFloodgate) {
                            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                            java.lang.reflect.Method getInstance = floodgateApiClass.getMethod("getInstance");
                            floodgateApiInstance = getInstance.invoke(null);
                            isFloodgatePlayerMethod = floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class);
                            getFloodgatePlayerMethod = floodgateApiClass.getMethod("getPlayer", UUID.class);
                        }
                    } catch (Exception e) {
                        hasFloodgate = false;
                    }
                    floodgateChecked.set(true);
                }
            }
        }
    }

    private static Object getFloodgatePlayer(UUID uuid) throws Exception {
        if (getFloodgatePlayerMethod == null) return null;
        return getFloodgatePlayerMethod.invoke(floodgateApiInstance, uuid);
    }

    public static boolean isBedrockPlayer(@NotNull Player player) {
        checkFloodgate();
        if (!hasFloodgate || isFloodgatePlayerMethod == null) return false;
        try {
            return (boolean) isFloodgatePlayerMethod.invoke(floodgateApiInstance, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBedrockPlayer(@NotNull UUID uuid) {
        checkFloodgate();
        if (!hasFloodgate || isFloodgatePlayerMethod == null) return false;
        try {
            return (boolean) isFloodgatePlayerMethod.invoke(floodgateApiInstance, uuid);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasGeyser() {
        checkGeyser();
        return hasGeyser;
    }

    public static boolean hasFloodgate() {
        checkFloodgate();
        return hasFloodgate;
    }

    // Bedrock names from Floodgate have a "." prefix — strip it for display
    @NotNull
    public static String getDisplayName(@NotNull Player player) {
        if (isBedrockPlayer(player)) {
            // Bedrock player names from Floodgate include a prefix like "."
            // We strip it for cleaner display
            String name = player.getName();
            if (name != null && name.startsWith(".") && name.length() > 1) {
                return name.substring(1);
            }
        }
        String name = player.getName();
        return name != null ? name : "Unknown";
    }

    // Same as above but works for offline players too
    @NotNull
    public static String getOfflineDisplayName(@NotNull UUID uuid) {
        if (isBedrockPlayer(uuid)) {
            try {
                Object player = getFloodgatePlayer(uuid);
                if (player != null) {
                    if (getUsernameMethod == null) {
                        getUsernameMethod = player.getClass().getMethod("getUsername");
                    }
                    String username = (String) getUsernameMethod.invoke(player);
                    if (username != null) return username;
                }
            } catch (Exception ignored) {}
        }
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }
}
