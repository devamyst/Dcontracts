package me.devamy.contracts.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.devamy.contracts.config.util.GUIConfigFile;
import me.devamy.contracts.config.util.component.ButtonConfig;
import me.devamy.contracts.config.util.component.OrderConfig;
import me.devamy.contracts.config.util.component.SortButtonConfig;
import me.devamy.contracts.config.util.component.SortsOrderConfig;
import me.devamy.contracts.obj.SortType;
import me.devamy.contracts.utils.DecoratedText;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

public class ContractAdminGUIConfig extends GUIConfigFile {

    public String title;
    public int rows;
    public final @NotNull OrderConfig orderConfig = new OrderConfig("order-item");
    public final @NotNull SortsOrderConfig sortsOrderConfig = new SortsOrderConfig("sort-button.order");
    public final @NotNull SortButtonConfig sortButton = new SortButtonConfig("sort-button");
    public final @NotNull ButtonConfig refreshButton = new ButtonConfig("refresh-button");
    public final @NotNull ButtonConfig searchButton = new ButtonConfig("search-button");
    public final @NotNull ButtonConfig closeButton = new ButtonConfig("close-button");
    public final @NotNull ButtonConfig backButton = new ButtonConfig("back-button");
    public final @NotNull ButtonConfig nextButton = new ButtonConfig("next-button");
    public final @NotNull ButtonConfig filterButton = new ButtonConfig("filter-button");

    public ContractAdminGUIConfig() {
        super("admin");
    }

    @Override
    public void applyDefaultValues() {
        title = "<dark_gray>[</dark_gray><red>Admin</red><dark_gray>]</dark_gray> <white>Contracts</white>";
        rows = 6;

        orderConfig.lore.add("");
        orderConfig.lore.add("<gray>ID: <white>#<id>");
        orderConfig.lore.add("<gray>Owner: <yellow><player>");
        orderConfig.lore.add("<gray>Status: <order-status>");
        orderConfig.lore.add("");
        orderConfig.lore.add("<gray>Price/ea: <green>$<money-per>");
        orderConfig.lore.add("<gray>Total: <green>$<total>");
        orderConfig.lore.add("<gray>Amount: <white><amount>");
        orderConfig.lore.add("<gray>Delivered: <white><delivered>");
        orderConfig.lore.add("<gray>In Storage: <white><in-storage>");
        orderConfig.lore.add("");
        orderConfig.lore.add("<yellow>Left-Click <gray>to <green>Edit</green>");
        orderConfig.lore.add("<red>Right-Click <gray>to <red>Cancel</red> <gray>(refund)");
        orderConfig.lore.add("<dark_red>Shift+Right <gray>to <dark_red>Force-Delete</dark_red>");
        orderConfig.slots.addAll(IntStream.range(0, 45).boxed().toList());

        sortsOrderConfig.orderArray.add(SortType.RECENTLY_LISTED);
        sortsOrderConfig.orderArray.add(SortType.OLDEST);
        sortsOrderConfig.orderArray.add(SortType.PRICIEST);
        sortsOrderConfig.orderArray.add(SortType.CHEAPEST);
        sortsOrderConfig.orderArray.add(SortType.MOST_MONEY_PER_ITEM);
        sortsOrderConfig.orderArray.add(SortType.MOST_DELIVERED);
        sortsOrderConfig.orderArray.add(SortType.MOST_PAID);

        sortButton.slot = 40;
        sortButton.itemStack = ItemStack.of(Material.COMPARATOR);
        sortButton.itemStack.editMeta(meta -> meta.displayName(DecoratedText.buttonName("Sort")));
        sortButton.lore.add("");
        sortButton.lore.add("<!i><white> • <recently-listed>");
        sortButton.lore.add("<!i><white> • <oldest>");
        sortButton.lore.add("<!i><white> • <priciest>");
        sortButton.lore.add("<!i><white> • <cheapest>");
        sortButton.lore.add("<!i><white> • <most-money-per-item>");
        sortButton.lore.add("<!i><white> • <most-delivered>");
        sortButton.lore.add("<!i><white> • <most-paid>");

        refreshButton.slot = 41;
        refreshButton.itemStack = ItemStack.of(Material.SLIME_BALL);
        refreshButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Refresh"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to refresh")));
        });

        searchButton.slot = 42;
        searchButton.itemStack = ItemStack.of(Material.COMPASS);
        searchButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Search"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to search orders")));
        });

        closeButton.slot = 44;
        closeButton.itemStack = ItemStack.of(Material.BARRIER);
        closeButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Close"));
            meta.lore(List.of(DecoratedText.buttonLore("Close the admin panel")));
        });

        backButton.slot = 36;
        backButton.itemStack = ItemStack.of(Material.ARROW);
        backButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Back"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to go to the previous page")));
        });

        nextButton.slot = 44;
        nextButton.itemStack = ItemStack.of(Material.ARROW);
        nextButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Next"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to go to the next page")));
        });

        filterButton.slot = 38;
        filterButton.itemStack = ItemStack.of(Material.HOPPER);
        filterButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("Filter"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to cycle filter")));
        });
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        orderConfig.setDefault(config);
        sortsOrderConfig.setDefault(config);
        sortButton.setDefault(config);
        refreshButton.setDefault(config);
        searchButton.setDefault(config);
        closeButton.setDefault(config);
        backButton.setDefault(config);
        nextButton.setDefault(config);
        filterButton.setDefault(config);
    }

    @Override
    public void reload() {
        orderConfig.reload(config);
        sortsOrderConfig.reload(config);
        sortButton.reload(config);
        refreshButton.reload(config);
        searchButton.reload(config);
        closeButton.reload(config);
        backButton.reload(config);
        nextButton.reload(config);
        filterButton.reload(config);
        title = config.getString("title");
        rows = config.getInteger("rows");
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        orderConfig.save(config);
        sortsOrderConfig.save(config);
        sortButton.save(config);
        refreshButton.save(config);
        searchButton.save(config);
        closeButton.save(config);
        backButton.save(config);
        nextButton.save(config);
        filterButton.save(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        try {
            setDefault();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
