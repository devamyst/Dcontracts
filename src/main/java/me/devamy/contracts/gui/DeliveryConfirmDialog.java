package me.devamy.contracts.gui;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.listener.DialogListener;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.utils.ConvertUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class DeliveryConfirmDialog {
    public static Dialog getDialog(Player p, Order order, int amount, double reward, Collection<ItemStack> items) {
        final Config config = Config.config;
        final Dialog dialog = config.confirmDeliveryDialogConfig.dialog(
                order.itemStack(config.mainGUIConfig.contractConfig.lore),
                ConvertUtils.formatNumber(amount),
                ConvertUtils.formatNumber(reward),
                (view, audience) -> {
                    DialogListener.removeItems(p);
                    order.deliver(p, items, false);
                },
                (view, audience) -> {
                    if (!(audience instanceof final Player player)) return;
                    DialogListener.onCancel(player);
                }
        );
        DialogListener.addItems(p, items);

        return dialog;
    }
}
