package me.devamy.contracts.utils;

import me.devamy.contracts.config.Config;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

import static me.devamy.contracts.Contracts.plugin;

public class EconUtils {

    private static net.milkbowl.vault.economy.Economy economy() {
        final net.milkbowl.vault.economy.Economy eco = plugin.getEconomy();
        if (eco == null) throw new IllegalStateException("Vault economy is not available");
        return eco;
    }

    public static void addMoney(OfflinePlayer p, double amount) {
        final Config config = Config.config;
        final UUID playerId = p.getUniqueId();
        final net.milkbowl.vault.economy.Economy eco = economy();
        final double before = config.logTransactions ? eco.getBalance(p) : 0;
        eco.depositPlayer(p, amount);
        if (config.logTransactions) {
            final double after = eco.getBalance(p);
            plugin.getStorage().logTransaction(playerId, before, amount, after);
        }
    }

    /// Returns `true` if the player has enough money to remove, otherwise `false`
    public static boolean removeMoney(Player p, double amount) {
        final net.milkbowl.vault.economy.Economy eco = economy();
        if (eco.getBalance(p) < amount) return false;
        final Config config = Config.config;
        final UUID playerId = p.getUniqueId();
        final double before = config.logTransactions ? eco.getBalance(p) : 0;
        eco.withdrawPlayer(p, amount);
        if (config.logTransactions) {
            final double after = eco.getBalance(p);
            plugin.getStorage().logTransaction(playerId, before, amount, after);
        }
        return true;
    }
}