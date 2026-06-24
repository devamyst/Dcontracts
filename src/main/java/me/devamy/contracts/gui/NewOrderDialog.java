package me.devamy.contracts.gui;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.guiframework.InventoryGUI;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.obj.orderitem.OrderItem;
import me.devamy.contracts.obj.orderitem.SearchableItem;
import me.devamy.contracts.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class NewOrderDialog {

    // Opens the contract creation flow — sends the player to the item picker first.
    public static void open(Player player) {
        InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
        PlayerUtils.openGUI(player, chooseItemGUI, false);
    }

    public static Dialog getDialog(OrderItem orderItem) {
        final Config config = Config.config;
        final ItemStack item = orderItem instanceof SearchableItem searchableItem ? searchableItem.getParsedItemStack() : orderItem.getItemStack();

        return config.newContractDialogConfig.dialog(
                item,
                (view, player) -> {
                    if (!(player instanceof Player p)) {
                        return;
                    }

                    PlayerUtils.closeInv(p);

                    // Create new contract
                    final Order.Response response = Order.create(p, item, view.getText("money_per"), view.getText("amount"));

                    switch (response) {
                        case INVALID -> p.sendRichMessage(config.invalidInput);
                        case FAIL -> p.sendRichMessage(config.notEnoughMoney);
                        case SUCCESS -> {
                            p.sendRichMessage(config.contractCreationSuccessful);
                            PlayerUtils.playSound(p, config.newContractSound);
                        }
                    }

                },
                (view, player) -> {
                    if (!(player instanceof Player p)) return;
                    InventoryGUI chooseItemGUI = ChooseItemGUI.getGUI(0, 0);
                    PlayerUtils.openGUI(p, chooseItemGUI, false);
                }
        );
    }
}
