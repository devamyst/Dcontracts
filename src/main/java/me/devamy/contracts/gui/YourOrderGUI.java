package me.devamy.contracts.gui;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.guiframework.InteractLocation;
import me.devamy.contracts.guiframework.InventoryGUI;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static me.devamy.contracts.Contracts.plugin;

public class YourOrderGUI {
    public static void open(Player player) {
        open(player, false);
    }

    public static void open(Player p, boolean isAsync) {
        final Config config = Config.config;
        final UUID pUUID = p.getUniqueId();
        final List<Order> orders = plugin.getDataCache().getOrders(pUUID, isAsync);
        final MiniMessage mm = plugin.mm;
        final InventoryGUI gui = new InventoryGUI(config.yourOrdersGUIConfig.rows, mm.deserialize(config.yourOrdersGUIConfig.title));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        final List<String> rawLore = config.yourOrdersGUIConfig.orderConfig.lore;
        int currentSlotIndex = 0;
        final int slotCount = config.yourOrdersGUIConfig.orderConfig.slots.size();
        for (Order order : orders) {
            if (currentSlotIndex >= slotCount) break;
            gui.addItem(order.item(rawLore, event -> {
                Dialog dialog = ManageOrderDialog.getDialog(order);
                PlayerUtils.openDialog(p, dialog);
            }), config.yourOrdersGUIConfig.orderConfig.slots.get(currentSlotIndex++));
        }

        if (orders.size() < config.yourOrdersGUIConfig.rows * 9) {
            gui.addItem(
                    config.yourOrdersGUIConfig.newOrderButton.item(event -> {
                        InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
                        PlayerUtils.openGUI(p, chooseItemGUI, false);
                    }), config.yourOrdersGUIConfig.newOrderButton.slot
            );
        }

        PlayerUtils.openGUI(p, gui, isAsync);
    }
}
