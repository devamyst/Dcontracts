package me.devamy.contracts.utils;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.guiframework.InventoryGUI;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static me.devamy.contracts.Contracts.plugin;
import static me.devamy.contracts.config.Config.config;

public class PlayerUtils {

    public static void give(Player p, Collection<ItemStack> items, boolean safe) {
        if (!safe) {
            p.give(items, true);
            return;
        }
        Location location = p.getLocation();
        World world = location.getWorld();
        p.getScheduler().run(plugin, task -> p.give(items, true), () ->
                Bukkit.getRegionScheduler().run(plugin, location, task ->
                        items.forEach(item ->
                                world.dropItem(location, item)
                        )
                )
        );

    }

    public static void give(Player p, ItemStack item, boolean safe) {
        PlayerUtils.give(p, Collections.singleton(item), safe);
    }

    // Give a specific amount, overrides the item stack's amount
    public static void give(Player p, ItemStack item, int amount, boolean safe) {
        final int maxStackSize = item.getMaxStackSize();

        final ItemStack copy = item.clone();
        copy.setAmount(maxStackSize);
        final int fullStackAmount = amount / maxStackSize;
        final List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < fullStackAmount; i++) {
            items.add(copy.clone());
        }
        final int rem = amount % maxStackSize;

        if (rem > 0) {
            copy.setAmount(rem);
            items.add(copy);
        }
        PlayerUtils.give(p, items, safe);
    }

    public static void playSound(Player p, Sound s) {
        p.playSound(s);
    }

    public static void openGUI(@NotNull HumanEntity player, @NotNull InventoryGUI gui, boolean safe) {
        if (!safe) {
            gui.open(player);
            return;
        }

        DispatchUtil.entity(player, () -> gui.open(player));
    }

    public static void openDialog(Player p, Dialog dialog) {
        DispatchUtil.entity(p, () -> p.showDialog(dialog));
    }

    public static void closeInv(Player p) {
        DispatchUtil.entity(p, () -> p.closeInventory());
    }

    public static void clickNext(InventoryClickEvent e, InventoryGUI nextPage) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        PlayerUtils.openGUI(p, nextPage, false);
        PlayerUtils.playSound(p, config.nextPageSound);
    }

    public static void clickBack(InventoryClickEvent e, InventoryGUI previousPage) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        PlayerUtils.openGUI(p, previousPage, false);
        PlayerUtils.playSound(p, config.previousPageSound);
    }
}
