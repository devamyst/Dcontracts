package me.devamy.contracts.gui;

import io.papermc.paper.dialog.Dialog;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.data.DataCache;
import me.devamy.contracts.guiframework.InteractLocation;
import me.devamy.contracts.guiframework.InventoryGUI;
import me.devamy.contracts.guiframework.InventoryItem;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.obj.SortType;
import me.devamy.contracts.utils.ConvertUtils;
import me.devamy.contracts.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static me.devamy.contracts.Contracts.plugin;

/**
 * Admin GUI that shows ALL contracts (including expired / completed) with
 * edit and force-delete capabilities for any player — including offline players.
 */
@SuppressWarnings("UnstableApiUsage")
public class ContractAdminGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Available filter modes for the admin GUI
    public enum FilterMode {
        ALL, ACTIVE, EXPIRED, COMPLETED
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static entry-point
    // ─────────────────────────────────────────────────────────────────────────

    /** Opens the admin GUI for {@code admin} at page {@code page} with FilterMode.ALL */
    public static void open(Player admin, int page) {
        open(admin, page, FilterMode.ALL, SortType.RECENTLY_LISTED);
    }

    public static void open(Player admin, int page, FilterMode filter, SortType sortType) {
        new ContractAdminGUI(admin, page, filter, sortType).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Instance
    // ─────────────────────────────────────────────────────────────────────────

    private final Player admin;
    private final int page;
    private final FilterMode filter;
    private final SortType sortType;

    /** Slots 0-44: contract entries (5 rows × 9 = 45 slots for items, row 6 = controls) */
    private static final int ITEMS_PER_PAGE = 45;
    private static final int ROW_CONTROL = 5; // 0-indexed; row 6 = slots 45-53

    private ContractAdminGUI(Player admin, int page, FilterMode filter, SortType sortType) {
        this.admin = admin;
        this.page = page;
        this.filter = filter;
        this.sortType = sortType;
    }

    private void show() {
        DataCache cache = plugin.getDataCache();

        // Collect all contracts (sorted by chosen sort type) and apply filter
        Collection<Order> allOrders = cache.getSortedOrders(sortType);
        List<Order> filtered = applyFilter(allOrders);

        int totalPages = Math.max(1, ConvertUtils.ceil_div(filtered.size(), ITEMS_PER_PAGE));
        int currentPage = Math.min(page, totalPages - 1);
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, filtered.size());
        List<Order> pageOrders = filtered.subList(startIdx, endIdx);

        InventoryGUI gui = new InventoryGUI(6,
                MM.deserialize("<dark_gray>[</dark_gray><red>Admin</red><dark_gray>]</dark_gray> <white>Contracts</white> "
                        + "<gray>- " + filterLabel() + " <yellow>(" + (currentPage + 1) + "/" + totalPages + ")"));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);

        // Fill contract items
        int slot = 0;
        for (Order order : pageOrders) {
            gui.addItem(buildOrderItem(order, currentPage, filter, sortType), slot++);
        }

        // Fill empty slots with gray glass
        ItemStack filler = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(m -> m.displayName(Component.empty()));
        for (int s = slot; s < ITEMS_PER_PAGE; s++) gui.addItem(new InventoryItem(filler, e -> {}), s);

        // ─── Control row (row 6, slots 45-53) ───────────────────────────────

        // Previous page
        if (currentPage > 0) {
            final int prevPage = currentPage - 1;
            gui.addItem(buildNavButton(Material.ARROW, "<white>◀ Previous Page",
                    "<gray>Go to page <yellow>" + currentPage,
                    e -> open(admin, prevPage, filter, sortType)), 45);
        }

        // Filter cycle button
        gui.addItem(buildFilterButton(currentPage, sortType), 48);

        // Sort cycle button
        gui.addItem(buildSortButton(currentPage, filter), 49);

        // Next page
        if (currentPage + 1 < totalPages) {
            final int nextPage = currentPage + 1;
            gui.addItem(buildNavButton(Material.ARROW, "<white>Next Page ▶",
                    "<gray>Go to page <yellow>" + (currentPage + 2),
                    e -> open(admin, nextPage, filter, sortType)), 53);
        }

        // Close / back button
        gui.addItem(buildNavButton(Material.BARRIER, "<red>Close", "", e -> admin.closeInventory()), 50);

        PlayerUtils.openGUI(admin, gui, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contract item builder
    // ─────────────────────────────────────────────────────────────────────────

    private InventoryItem buildOrderItem(Order order, int currentPage, FilterMode filter, SortType sortType) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(order.getOwnerUniqueId());
        String ownerName = owner.getName() != null ? owner.getName() : order.getOwnerUniqueId().toString();

        ItemStack display = order.getItem().clone();
        display.editMeta(meta -> {
            // Display name: owner + item
            Component origName = meta.hasCustomName() ? meta.customName()
                    : Component.translatable(display.getType().getItemTranslationKey() != null
                    ? display.getType().getItemTranslationKey() : display.getType().name().toLowerCase());
            meta.displayName(Component.text("[Admin] ", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(ownerName + "'s ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                    .append(origName != null ? origName.decoration(TextDecoration.ITALIC, false) : Component.empty()));

            // Lore with full contract details
            String statusColor = order.isActive() ? "<green>" : (order.getDelivered() >= order.getAmount() ? "<aqua>" : "<red>");
            long msLeft = order.getExpiresAt() - System.currentTimeMillis();
            String timeLeft = msLeft > 0 ? formatTime(msLeft) : "<red>Expired";

            List<Component> lore = Arrays.asList(
                    Component.empty(),
                    mm("<gray>ID: <white>#" + order.getId()),
                    mm("<gray>Owner: <yellow>" + ownerName),
                    mm("<gray>Status: " + statusColor + getStatusLabel(order)),
                    Component.empty(),
                    mm("<gray>Price/ea: <green>$" + ConvertUtils.formatNumber(order.getMoneyPer())),
                    mm("<gray>Total Value: <green>$" + ConvertUtils.formatNumber(order.getMoneyPer() * order.getAmount())),
                    mm("<gray>Amount: <white>" + order.getAmount()),
                    mm("<gray>Delivered: <white>" + order.getDelivered()),
                    mm("<gray>In Storage: <white>" + order.getInStorage()),
                    mm("<gray>Expires In: <white>" + timeLeft),
                    Component.empty(),
                    mm("<yellow>Left-Click <gray>to <green>Edit"),
                    mm("<red>Right-Click <gray>to <red>Delete"),
                    mm("<gray>(Shift+Right) <gray>to <dark_red>Force-Cancel <gray>(no refund)")
            );
            meta.lore(lore);
        });

        return new InventoryItem(display, event -> {
            if (!admin.hasPermission("contracts.admin")) return;

            if (event.getClick() == ClickType.LEFT && admin.hasPermission("contracts.admin.edit-contracts")) {
                Dialog editDialog = AdminToolGUI.createEditOrder(order);
                admin.showDialog(editDialog);

            } else if (event.getClick() == ClickType.RIGHT && admin.hasPermission("contracts.admin.delete-contracts")) {
                // Soft cancel (expire) with payback
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
                    open(admin, currentPage, filter, sortType);
                });

            } else if (event.getClick() == ClickType.SHIFT_RIGHT && admin.hasPermission("contracts.admin.delete-contracts")) {
                // Hard delete — no refund
                admin.sendRichMessage("<red>Force-deleting contract #" + order.getId() + " — no refund issued.");
                plugin.getStorage().deleteOrder(order).thenAccept(v -> {
                    admin.sendRichMessage("<green>Contract #" + order.getId() + " force-deleted.");
                    open(admin, currentPage, filter, sortType);
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Control button builders
    // ─────────────────────────────────────────────────────────────────────────

    private InventoryItem buildFilterButton(int currentPage, SortType sortType) {
        FilterMode next = nextFilter();
        ItemStack item = ItemStack.of(Material.HOPPER);
        item.editMeta(meta -> {
            meta.displayName(mm("<aqua>Filter: <white>" + filterLabel()).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(
                    Component.empty(),
                    mm("<gray>Current: <yellow>" + filterLabel()),
                    mm("<gray>Click for next: <yellow>" + filterModeLabel(next)),
                    Component.empty(),
                    mm("<dark_gray>ALL / ACTIVE / EXPIRED / COMPLETED")
            ));
        });
        return new InventoryItem(item, e -> open(admin, 0, next, sortType));
    }

    private InventoryItem buildSortButton(int currentPage, FilterMode filter) {
        SortType next = nextSort();
        ItemStack item = ItemStack.of(Material.COMPARATOR);
        item.editMeta(meta -> {
            meta.displayName(mm("<aqua>Sort: <white>" + sortLabel(sortType)).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(
                    Component.empty(),
                    mm("<gray>Current: <yellow>" + sortLabel(sortType)),
                    mm("<gray>Click for next: <yellow>" + sortLabel(next)),
                    Component.empty(),
                    mm("<dark_gray>NEWEST / OLDEST / PRICIEST / CHEAPEST / MOST_MONEY / RECENTLY_LISTED")
            ));
        });
        return new InventoryItem(item, e -> open(admin, 0, filter, next));
    }

    private static InventoryItem buildNavButton(Material mat, String name, String lore,
                                                 java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        ItemStack item = ItemStack.of(mat);
        item.editMeta(meta -> {
            meta.displayName(mm(name).decoration(TextDecoration.ITALIC, false));
            if (!lore.isEmpty()) meta.lore(List.of(mm(lore).decoration(TextDecoration.ITALIC, false)));
        });
        return new InventoryItem(item, action);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

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
        // Cycle only through admin-relevant sorts
        SortType[] sorts = {SortType.RECENTLY_LISTED, SortType.OLDEST, SortType.PRICIEST, SortType.CHEAPEST, SortType.MOST_MONEY_PER_ITEM};
        for (int i = 0; i < sorts.length; i++) {
            if (sorts[i] == sortType) return sorts[(i + 1) % sorts.length];
        }
        return SortType.RECENTLY_LISTED;
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

    private static String getStatusLabel(Order o) {
        if (o.getDelivered() >= o.getAmount()) return "Completed";
        if (o.getExpiresAt() < System.currentTimeMillis()) return "Expired";
        return "Active";
    }

    private static String formatTime(long ms) {
        long sec = ms / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long day = hour / 24;
        hour %= 24; min %= 60; sec %= 60;
        if (day > 0) return day + "d " + hour + "h";
        if (hour > 0) return hour + "h " + min + "m";
        if (min > 0) return min + "m " + sec + "s";
        return sec + "s";
    }

    private static Component mm(String s) {
        return MM.deserialize(s);
    }
}
