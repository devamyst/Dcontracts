package me.devamy.contracts.gui;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class ManageOrderDialog {
    public static Dialog getDialog(Order order) {
        final Config config = Config.config;
        final ItemStack item = order.itemStack(config.yourContractsGUIConfig.contractConfig.lore);
        final Dialog collectItemsDialog = config.manageContractDialogConfig.collectItems.dialog(
                item,
                order.placeholders(),
                (view, audience) -> {
                    if (!(audience instanceof Player player)) return;

                    final String rawAmount = view.getText("amount");
                    order.collect(rawAmount);
                    player.closeInventory();
                },
                null
        );

        final Dialog cancelContractDialog = config.manageContractDialogConfig.cancelContract.dialog(
                item,
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    order.cancel(p);
                },
                (view, audience) -> {
                    if (!(audience instanceof Player player)) return;
                    YourOrderGUI.open(player);
                }
        );

        return config.manageContractDialogConfig.manageContract.dialog(
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    PlayerUtils.openDialog(p, collectItemsDialog);
                },
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    PlayerUtils.openDialog(p, cancelContractDialog);
                }
        );
    }
}
