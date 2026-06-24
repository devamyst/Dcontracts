package me.devamy.contracts.config.util.component;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.devamy.contracts.utils.ConvertUtils;
import me.devamy.contracts.utils.Log;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EnchantmentConfig {
    public String activeName = "<aqua><enchantment> <roman-numeral-level>";
    public String inactiveName = "<gray><enchantment>";
    public ItemStack itemRepresentation = ItemStack.of(Material.ENCHANTED_BOOK);
    public final @NotNull List<@NotNull Integer> slots = IntStream.range(18, 36).boxed().collect(Collectors.toCollection(ArrayList::new));

    public EnchantmentConfig(final @NotNull String path) {
        itemRepresentation.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
    }

    public void reload(@NotNull ConfigFile config) {
        activeName = config.getString("name.active");
        inactiveName = config.getString("name.inactive");
        try {
            itemRepresentation = ConvertUtils.deserializeItem(config.getConfigSection("item"));
        } catch (Exception e) {
            Log.error("Failed to deserialize item", e);
        }
        slots.clear();
        final List<?> rawSlots = config.getList("slots");
        if (rawSlots != null && !rawSlots.isEmpty()) {
            for (Object obj : rawSlots) {
                if (obj instanceof Number num) {
                    slots.add(num.intValue());
                }
            }
        }
        if (slots.isEmpty()) {
            slots.addAll(IntStream.range(18, 36).boxed().toList());
        }
    }

    public void save(@NotNull ConfigFile config) {
        config.set("name.active", activeName);
        config.set("name.inactive", inactiveName);
        config.set("item", ConvertUtils.serializeItem(itemRepresentation));
        config.set("slots", slots);
    }

    public void setDefault(@NotNull ConfigFile config) {
        config.addDefault("name.active", activeName);
        config.addDefault("name.inactive", inactiveName);
        config.addDefault("item", ConvertUtils.serializeItem(itemRepresentation));
        config.addDefault("slots", slots);
    }

    public void migrateV5(@NotNull ConfigFile oldConfig) {
        activeName = oldConfig.getString("gui.enchant-item.name-prefix.active") + "<enchantment> <level>";
        inactiveName = oldConfig.getString("gui.enchant-item.name-prefix.inactive") + "<enchantment>";
        final List<String> loreLines = oldConfig.getStringList("gui.enchant-item.lore");
        itemRepresentation = ItemStack.of(Material.ENCHANTED_BOOK);
        itemRepresentation.editMeta(meta -> {
            meta.lore(loreLines.stream().map(MiniMessage.miniMessage()::deserialize).toList());
            meta.setEnchantmentGlintOverride(true);
        });
        slots.clear();
        slots.addAll(IntStream.range(18, 36).boxed().toList());
    }
}
