package me.karven.orderium.config.util.component;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.utils.ConvertUtils;
import me.karven.orderium.utils.Values;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static me.karven.orderium.config.Config.config;

// Sort buttons have placeholders in lore, we need a different way to handle them.
public class SortButtonConfig extends ButtonConfig {
    public final @NotNull List<@NotNull String> lore = new ArrayList<>();

    public SortButtonConfig(@NotNull String path) {
        super(path);
    }

    @Override
    public void save(final @NotNull ConfigFile config) {
        super.save(config);

        config.set(path + ".lore", lore);
    }

    @Override
    public void setDefault(final @NotNull ConfigFile config) {
        super.setDefault(config);

        config.set(path + ".lore", lore);
    }

    @Override
    public @NonNull InventoryItem item(final @NotNull Consumer<InventoryClickEvent> action) {
        final ItemStack item = itemStack.clone();
        final List<TagResolver> placeholders = new ArrayList<>(List.of(config.sortPlaceholders));

        item.editMeta(meta -> {
            final List<Component> parsedLore = lore.stream().map(line -> Values.minimessage.deserialize(line, TagResolver.resolver(placeholders))).toList();
            meta.lore(parsedLore);
        });

        return new InventoryItem(item, action);
    }

    public @NotNull InventoryItem item(final @NotNull Consumer<InventoryClickEvent> action, final @NotNull SortType sort) {
        final ItemStack item = itemStack.clone();
        final List<TagResolver> placeholders = new ArrayList<>(List.of(config.sortPlaceholders));
        @Subst("ignored")
        final String identifier = sort.getIdentifier();
        placeholders.add(Placeholder.parsed(identifier, sort.getDisplayActive()));

        item.editMeta(meta -> {
            final List<Component> parsedLore = lore.stream().map(line -> Values.minimessage.deserialize(line, TagResolver.resolver(placeholders))).toList();
            meta.lore(parsedLore);
        });

        return new InventoryItem(item, action);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile config, final @NotNull String oldPath) {
        final ItemStack item = ConvertUtils.getItemType(config.getString(oldPath + ".type"))
                .createItemStack();
        final List<String> loreLines = config.getStringList(oldPath + ".lore");
        final String displayNameString = config.getString(oldPath + ".display-name");
        final int slot = config.getInteger(oldPath + ".slot");
        item.editMeta(meta -> {
            if (displayNameString != null) meta.displayName(MiniMessage.miniMessage().deserialize(displayNameString));
        });

        this.itemStack = item;
        this.slot = slot;
        this.lore.clear();
        this.lore.addAll(loreLines);
    }
}
