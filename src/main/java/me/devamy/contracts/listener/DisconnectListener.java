package me.devamy.contracts.listener;

import me.devamy.contracts.gui.SignGUI;
import me.devamy.contracts.guiframework.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;

// Cleans up when a player quits — returns items, closes sign sessions, etc.
@SuppressWarnings("UnstableApiUsage")
public class DisconnectListener implements Listener {

    // Give items back if the player quits while in the delivery GUI
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        SignGUI.completeSession(p, ""); // Close the sign GUI manually if the player is in one

        DialogListener.onCancel(p); // Return items and remove pending delivery if any

        InventoryView view = p.getOpenInventory();
        if (view.getTopInventory().getHolder() instanceof InventoryGUI gui)
            gui.callOnClose(new InventoryCloseEvent(view)); // Close the inventory GUI manually if the player is in one

    }
}
