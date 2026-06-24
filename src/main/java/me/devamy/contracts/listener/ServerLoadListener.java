package me.devamy.contracts.listener;

import me.devamy.contracts.utils.Log;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.NotNull;

import static me.devamy.contracts.Contracts.plugin;

public class ServerLoadListener implements Listener {

    // ServerLoadEvent fires after all plugins are enabled, so we can
    // safely hook Vault here (economy might load after us).
    @EventHandler
    public void onPostWorldInitialization(final @NotNull ServerLoadEvent event) {
        if (event.getType().equals(ServerLoadEvent.LoadType.RELOAD)) {
            Log.warn("Bukkit reloading is NOT supported. Expect bugs and errors.");
        }
        plugin.setupEconomy();

        plugin.postEconomyRegistration();
    }
}
