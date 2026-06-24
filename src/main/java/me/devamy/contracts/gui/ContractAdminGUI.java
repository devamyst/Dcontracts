package me.devamy.contracts.gui;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.config.util.chestgui.ContractAdminGUIConfig;
import me.devamy.contracts.data.DataCache;
import me.devamy.contracts.guiframework.InteractLocation;
import me.devamy.contracts.guiframework.InventoryGUI;
import me.devamy.contracts.guiframework.InventoryItem;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.obj.SortType;
import me.devamy.contracts.utils.BedrockUtils;
import me.devamy.contracts.utils.ConvertUtils;
import me.devamy.contracts.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static me.devamy.contracts.Contracts.plugin;

// Admin GUI — shows ALL contracts (including expired/completed) with edit
// and force-delete. Works for any player, including offline ones.
@SuppressWarnings("UnstableApiUsage")
public class ContractAdminGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public enum FilterMode {
        ALL, ACTIVE, EXPIRED, COMPLETED
    }

    public static void open(Player admin, int page) {
        open(admin, page, FilterMode.ALL, SortType.RECENTLY_LISTED, "");
    }

    public static void open(Player admin, int page, FilterMode filter, SortType sortType) {
        open(admin, page, filter, sortType, "");
    }

    public static void open(Player admin, int page, FilterMode filter, SortType sortType, String search) {
        new ContractAdminGUI(admin, page, filter, sortType, search).show();
    }

    private final Player admin;
    private final int page;
    private final FilterMode filter;
    private final SortType sortType;
    private final String search;

    private ContractAdminGUI(Player admin, int page, FilterMode filter, SortType sortType, String search) {
        this.admin = admin;
        this.page = page;
        this.filter = filter;
        this.sortType = sortType;
        this.search = search;
    }

    private void show() {
        DataCache cache = plugin.getDataCache();
        Config config = Config.config;

        Collection<Order> allOrders = cache.getSortedOrders(sortType);
        List<Order> filtered = applyFilter(allOrders);

        // Apply search if present
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase().trim();
            filtered = filtered.stream()
                    .filter(o -> {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(o.getOwnerUniqueId());
                        String name = op.getName();
                        return (name != null && name.toLowerCase().contains(q))
                                || String.valueOf(o.getId()).contains(q);
                    })
                    .collect(Collectors.toList());
        }

        int itemsPerPage = config.contractAdminGUIConfig.contractConfig.slots.size();
        int totalPages = Math.max(1, ConvertUtils.ceil_div(filtered.size(), itemsPerPage));
        int currentPage = Math.min(page, totalPages - 1);
        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, filtered.size());
        List<Order> pageOrders = filtered.subList(startIdx, endIdx);

        String titleStr = config.contractAdminGUIConfig.title
                .replace("%page%", String.valueOf(currentPage + 1))
                .replace("%total_pages%", String.valueOf(totalPages))
                .replace("%filter%", filterLabel())
                .replace("%sort%", sortLabel(sortType));

        InventoryGUI gui = new InventoryGUI(config.contractAdminGUIConfig.rows, MM.deserialize(titleStr));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);

        // Fill contract items using configured slots
        List<Integer> slots = config.contractAdminGUIConfig.contractConfig.slots;
        int slotIdx = 0;
        for (Order order : pageOrders) {
            if (slotIdx >= slots.size()) break;
            gui.addItem(buildOrderItem(order, currentPage, filter, sortType, search), slots.get(slotIdx++));
        }

        // Fill remaining order slots with filler
        ItemStack filler = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(m -> m.displayName(Component.empty()));
        for (int i = slotIdx; i < slots.size(); i++) {
            gui.addItem(new InventoryItem(filler.clone(), e -> {}), slots.get(i));
        }

        // Navigation buttons
        addNavButtons(gui, currentPage, totalPages);

        PlayerUtils.openGUI(admin, gui, false);
    }

    private void addNavButtons(InventoryGUI gui, int currentPage, int totalPages) {
        ContractAdminGUIConfig cfg = Config.config.contractAdminGUIConfig;

        // Back button
        if (currentPage > 0) {
            final int prev = currentPage - 1;
            gui.addItem(cfg.backButton.item(e -> open(admin, prev, filter, sortType, search)), cfg.backButton.slot);
        }

        // Next button
        if (currentPage + 1 < totalPages) {
            final int next = currentPage + 1;
            gui.addItem(cfg.nextButton.item(e -> open(admin, next, filter, sortType, search)), cfg.nextButton.slot);
        }

        // Filter button
        FilterMode nextFilter = nextFilter();
        gui.addItem(new InventoryItem(cfg.filterButton.itemStack.clone(), e -> open(admin, 0, nextFilter, sortType, search)), cfg.filterButton.slot);

        // Sort button
        SortType nextSort = nextSort();
        gui.addItem(cfg.sortButton.item(e -> open(admin, 0, filter, nextSort, search), sortType), cfg.sortButton.slot);

        // Refresh button
        gui.addItem(cfg.refreshButton.item(e -> open(admin, currentPage, filter, sortType, search)), cfg.refreshButton.slot);

        // Search button
        gui.addItem(cfg.searchButton.item(e -> {
            SignGUI.newSession(admin, s -> open(admin, 0, filter, sortType, s),
                    Config.config.signGUIConfig.signLines, Config.config.signGUIConfig.signType(), Config.config.signGUIConfig.queryLine);
        }), cfg.searchButton.slot);

        // Close button
        gui.addItem(cfg.closeButton.item(e -> admin.closeInventory()), cfg.closeButton.slot);
    }

    private InventoryItem buildOrderItem(Order order, int currentPage, FilterMode filter, SortType sortType, String search) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(order.getOwnerUniqueId());
        String ownerName;
        if (owner.getName() != null) {
            ownerName = owner.getName();
        } else if (BedrockUtils.hasFloodgate()) {
            ownerName = BedrockUtils.getOfflineDisplayName(order.getOwnerUniqueId());
        } else {
            ownerName = order.getOwnerUniqueId().toString().substring(0, 8);
        }

        ItemStack display = order.getItem().clone();
        display.editMeta(meta -> {
            Component origName = meta.hasCustomName() ? meta.customName()
                    : Component.translatable(display.getType().getItemTranslationKey() != null
                    ? display.getType().getItemTranslationKey() : display.getType().name().toLowerCase());
            meta.displayName(Component.text()
                    .append(MM.deserialize("<dark_red>[Admin] </dark_red>"))
                    .append(MM.deserialize("<gold>" + ownerName + "'s </gold>"))
                    .append(origName != null ? origName : Component.empty())
                    .build());

            List<String> loreLines = Config.config.contractAdminGUIConfig.contractConfig.lore;
            if (loreLines != null && !loreLines.isEmpty()) {
                List<Component> lore = loreLines.stream()
                        .map(line -> order.deserializeText(line))
                        .collect(Collectors.toList());
                meta.lore(lore);
            }
        });

        return new InventoryItem(display, event -> {
            if (!admin.hasPermission("contracts.admin")) return;

            if (event.getClick() == ClickType.LEFT && admin.hasPermission("contracts.admin.edit-contracts")) {
                Dialog editDialog = AdminToolGUI.createEditOrder(order);
                admin.showDialog(editDialog);

            } else if (event.getClick() == ClickType.RIGHT && admin.hasPermission("contracts.admin.delete-contracts")) {
                admin.sendRichMessage("<gray>Cancelling contract #" + order.getId() + " (owner will be refunded)...");
                plugin.getStorage().cancelOrder(order).thenAccept(payback -> {
                    if (payback == -1.0) {
                        admin.sendRichMessage("<red>Contract not found or already expired.");
                    } else {
                        if (payback > 0) {
                            net.milkbowl.vault.economy.Economy eco = plugin.getEconomy();
                            if (eco != null) eco.depositPlayer(owner, payback);
                        }
                        admin.sendRichMessage("<green>Contract #" + order.getId() + " cancelled. Owner refunded <yellow>$" + ConvertUtils.formatNumber(payback));
                    }
                    open(admin, currentPage, filter, sortType, search);
                });

            } else if (event.getClick() == ClickType.SHIFT_RIGHT && admin.hasPermission("contracts.admin.delete-contracts")) {
                admin.sendRichMessage("<red>Force-deleting contract #" + order.getId() + " — no refund issued.");
                plugin.getStorage().deleteOrder(order).thenAccept(v -> {
                    admin.sendRichMessage("<green>Contract #" + order.getId() + " force-deleted.");
                    open(admin, currentPage, filter, sortType, search);
                });
            }
        });
    }

    private List<Order> applyFilter(Collection<Order> orders) {
        return orders.stream().filter(o -> switch (filter) {
            case ACTIVE -> o.isActive();
            case EXPIRED -> o.getExpiresAt() < System.currentTimeMillis() && o.getDelivered() < o.getAmount();
            case COMPLETED -> o.getDelivered() >= o.getAmount();
            case ALL -> true;
        }).collect(Collectors.toList());
    }

    private FilterMode nextFilter() {
        FilterMode[] values = FilterMode.values();
        return values[(filter.ordinal() + 1) % values.length];
    }

    private SortType nextSort() {
        SortType[] sorts = Config.config.contractAdminGUIConfig.sortsOrderConfig.orderArray.toArray(new SortType[0]);
        if (sorts.length == 0) return SortType.RECENTLY_LISTED;
        for (int i = 0; i < sorts.length; i++) {
            if (sorts[i] == sortType) return sorts[(i + 1) % sorts.length];
        }
        return sorts[0];
    }

    private String filterLabel() { return filterModeLabel(filter); }

    private static String filterModeLabel(FilterMode m) {
        return switch (m) {
            case ALL -> "All";
            case ACTIVE -> "Active";
            case EXPIRED -> "Expired";
            case COMPLETED -> "Completed";
        };
    }

    private static String sortLabel(SortType s) {
        return switch (s) {
            case RECENTLY_LISTED -> "Newest";
            case OLDEST -> "Oldest";
            case PRICIEST, MOST_MONEY_PER_ITEM -> "Priciest";
            case CHEAPEST -> "Cheapest";
            case MOST_DELIVERED -> "Most Delivered";
            case MOST_PAID -> "Most Paid";
            case A_Z -> "A-Z";
            case Z_A -> "Z-A";
        };
    }
}
