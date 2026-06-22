package me.devamy.contracts.utils;

import me.devamy.contracts.config.Config;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

import static me.devamy.contracts.Contracts.plugin;

public class EconUtils {

    public static void addMoney(OfflinePlayer p, double amount) {
        final Config config = Config.config;
        final UUID playerId = p.getUniqueId();
        final double before = config.logTransactions ? plugin.getEconomy().getBalance(p) : 0;
        plugin.getEconomy().depositPlayer(p, amount);
        if (config.logTransactions) {
            final double after = plugin.getEconomy().getBalance(p);
            plugin.getStorage().logTransaction(playerId, before, amount, after);
        }
    }

    /// Returns `true` if the player has enough money to remove, otherwise `false`
    public static boolean removeMoney(Player p, double amount) {
        if (plugin.getEconomy().getBalance(p) < amount) return false;
        final Config config = Config.config;
        final UUID playerId = p.getUniqueId();
        final double before = config.logTransactions ? plugin.getEconomy().getBalance(p) : 0;
        plugin.getEconomy().withdrawPlayer(p, amount);
        if (config.logTransactions) {
            final double after = plugin.getEconomy().getBalance(p);
            plugin.getStorage().logTransaction(playerId, before, amount, after);
        }
        return true;
    }
}