package me.devamy.contracts.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.devamy.contracts.config.DatabaseConfig;
import me.devamy.contracts.guiframework.InteractLocation;
import me.devamy.contracts.guiframework.InventoryGUI;
import me.devamy.contracts.guiframework.InventoryItem;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.obj.StorageMethod;
import me.devamy.contracts.utils.ConvertUtils;
import me.devamy.contracts.utils.DispatchUtil;
import me.devamy.contracts.utils.Log;
import me.devamy.contracts.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static me.devamy.contracts.Contracts.plugin;

@SuppressWarnings("UnstableApiUsage")
public class DatabaseConverter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private DatabaseConverter() {}

    // ─────────────────────────────────────────────────────────────
    // Entry Point
    // ─────────────────────────────────────────────────────────────

    public static void openConverterGUI(Player admin) {
        showSourcePicker(admin);
    }

    // ─────────────────────────────────────────────────────────────
    // Step 1: Pick Source Storage
    // ─────────────────────────────────────────────────────────────

    private static void showSourcePicker(Player admin) {
        InventoryGUI gui = new InventoryGUI(3, MM.deserialize("<dark_gray>[</dark_gray><gold>DB Converter</gold><dark_gray>]</dark_gray> <gray>Step 1: Source Database"));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);

        gui.addItem(storageTypeItem(StorageMethod.SQLITE, "SQLite",
                "<gray>Stored in <yellow>plugins/Dcontracts/data.db</yellow>",
                e -> showTargetPicker(admin, StorageMethod.SQLITE)), 11);

        gui.addItem(storageTypeItem(StorageMethod.MYSQL, "MySQL",
                "<gray>Remote MySQL database",
                e -> showConnectionForm(admin, StorageMethod.MYSQL, true)), 13);

        gui.addItem(storageTypeItem(StorageMethod.H2, "H2",
                "<gray>Embedded H2 database",
                e -> showConnectionForm(admin, StorageMethod.H2, true)), 15);

        PlayerUtils.openGUI(admin, gui, false);
    }

    // ─────────────────────────────────────────────────────────────
    // Step 2: Pick Target Storage
    // ─────────────────────────────────────────────────────────────

    private static void showTargetPicker(Player admin, StorageMethod sourceMethod) {
        InventoryGUI gui = new InventoryGUI(3, MM.deserialize("<dark_gray>[</dark_gray><gold>DB Converter</gold><dark_gray>]</dark_gray> <gray>Step 2: Target Database"));
        gui.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        gui.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);

        gui.addItem(storageTypeItem(StorageMethod.SQLITE, "SQLite",
                "<gray>Stored in <yellow>plugins/Dcontracts/data.db</yellow>",
                e -> {
                    if (sourceMethod == StorageMethod.SQLITE) {
                        admin.sendRichMessage("<red>Source and target cannot be the same type.");
                        return;
                    }
                    performConversion(admin, sourceMethod, StorageMethod.SQLITE, "localhost", 3306, "contracts", "root", "");
                }), 11);

        gui.addItem(storageTypeItem(StorageMethod.MYSQL, "MySQL",
                "<gray>Remote MySQL database",
                e -> {
                    if (sourceMethod == StorageMethod.MYSQL) {
                        admin.sendRichMessage("<red>Source and target cannot be the same type.");
                        return;
                    }
                    showConnectionForm(admin, StorageMethod.MYSQL, false);
                }), 13);

        gui.addItem(storageTypeItem(StorageMethod.H2, "H2",
                "<gray>Embedded H2 database",
                e -> {
                    if (sourceMethod == StorageMethod.H2) {
                        admin.sendRichMessage("<red>Source and target cannot be the same type.");
                        return;
                    }
                    showConnectionForm(admin, StorageMethod.H2, false);
                }), 15);

        gui.addItem(buildBackItem(e -> showSourcePicker(admin)), 22);

        PlayerUtils.openGUI(admin, gui, false);
    }

    // ─────────────────────────────────────────────────────────────
    // Connection Form Dialog (for MySQL / H2)
    // ─────────────────────────────────────────────────────────────

    private static void showConnectionForm(Player admin, StorageMethod method, boolean isSource) {
        String title = isSource ? "Source " : "Target ";

        DialogBody body = DialogBody.plainMessage(Component.text("Enter " + method.name() + " connection details"));

        DialogInput hostInput = DialogInput.text("host", Component.text("Host")).build();
        DialogInput portInput = DialogInput.text("port", Component.text("Port")).build();
        DialogInput dbInput = DialogInput.text("database", Component.text("Database Name")).build();
        DialogInput userInput = DialogInput.text("username", Component.text("Username")).build();
        DialogInput passInput = DialogInput.text("password", Component.text("Password")).build();

        ActionButton confirm = ActionButton.builder(Component.text("Connect", NamedTextColor.GREEN))
                .action(DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    String host = view.getText("host");
                    String portStr = view.getText("port");
                    String db = view.getText("database");
                    String user = view.getText("username");
                    String pass = view.getText("password");

                    if (host == null || host.isEmpty()) host = "localhost";
                    int port;
                    try {
                        port = portStr != null && !portStr.isEmpty() ? Integer.parseInt(portStr) : 3306;
                    } catch (NumberFormatException e) {
                        p.sendRichMessage("<red>Invalid port number.");
                        return;
                    }
                    if (db == null || db.isEmpty()) {
                        p.sendRichMessage("<red>Database name is required.");
                        return;
                    }
                    if (user == null || user.isEmpty()) user = "root";
                    if (pass == null) pass = "";

                    if (isSource) {
                        showTargetPicker(p, method);
                    } else {
                        performConversion(p, method, method, host, port, db, user, pass);
                    }
                }, ClickCallback.Options.builder().build()))
                .build();

        ActionButton cancel = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED)).build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title + method.name() + " Connection"))
                        .body(List.of(body))
                        .inputs(List.of(hostInput, portInput, dbInput, userInput, passInput))
                        .build()
                )
                .type(DialogType.confirmation(confirm, cancel))
        );

        admin.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────
    // Conversion Logic
    // ─────────────────────────────────────────────────────────────

    private static void performConversion(Player admin, StorageMethod source, StorageMethod target,
                                          String targetHost, int targetPort, String targetDb,
                                          String targetUser, String targetPass) {
        InventoryGUI progressGUI = new InventoryGUI(6, MM.deserialize("<gold>Database Conversion In Progress..."));
        progressGUI.setOnClick(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        progressGUI.setOnDrag(e -> e.setCancelled(true), InteractLocation.GLOBAL);
        progressGUI.setOnClose(e -> {
            e.getPlayer().sendRichMessage("<yellow>Conversion running in background. Do not shut down the server.");
        });

        // Progress bar item
        ItemStack progressBarItem = ItemStack.of(Material.BLUE_STAINED_GLASS_PANE);
        progressBarItem.editMeta(m -> m.displayName(MM.deserialize("<aqua>Conversion Progress").decoration(TextDecoration.ITALIC, false)));

        // Status item
        ItemStack statusItem = ItemStack.of(Material.PAPER);
        statusItem.editMeta(m -> m.displayName(MM.deserialize("<yellow>Starting...").decoration(TextDecoration.ITALIC, false)));

        // Fill background
        ItemStack filler = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE);
        filler.editMeta(m -> m.displayName(Component.empty()));

        for (int i = 0; i < 54; i++) {
            if (i >= 9 && i <= 43) {
                progressGUI.addItem(new InventoryItem(filler.clone(), ev -> {}), i);
            } else {
                progressGUI.addItem(new InventoryItem(filler.clone(), ev -> {}), i);
            }
        }

        // Progress bar in slots 9-43 (35 slots)
        for (int i = 9; i < 12; i++) {
            progressGUI.addItem(new InventoryItem(progressBarItem.clone(), ev -> {}), i);
        }

        // Status at slot 22
        progressGUI.addItem(new InventoryItem(statusItem, ev -> {}), 22);

        PlayerUtils.openGUI(admin, progressGUI, false);

        // Run conversion async
        CompletableFuture.runAsync(() -> {
            try {
                HikariDataSource sourceDS = createSourceDataSource(source);
                HikariDataSource targetDS = createTargetDataSource(target, targetHost, targetPort, targetDb, targetUser, targetPass);

                if (sourceDS == null || targetDS == null) {
                    updateStatus(admin, progressGUI, "<red>Failed to connect to one or both databases.");
                    return;
                }

                try {
                    // Step 1: Create tables in target
                    updateStatus(admin, progressGUI, "<yellow>Creating tables in target database...");
                    updateProgress(admin, progressGUI, 5);
                    createTargetTables(targetDS, target);

                    // Step 2: Transfer orders
                    updateStatus(admin, progressGUI, "<yellow>Transferring orders...");
                    updateProgress(admin, progressGUI, 15);
                    int ordersTransferred = transferOrders(sourceDS, targetDS, target, admin, progressGUI);
                    updateProgress(admin, progressGUI, 50);

                    // Step 3: Transfer transactions
                    updateStatus(admin, progressGUI, "<yellow>Transferring transactions...");
                    updateProgress(admin, progressGUI, 55);
                    int transactionsTransferred = transferTransactions(sourceDS, targetDS, admin, progressGUI);
                    updateProgress(admin, progressGUI, 75);

                    // Step 4: Transfer custom items
                    updateStatus(admin, progressGUI, "<yellow>Transferring custom items...");
                    updateProgress(admin, progressGUI, 78);
                    transferCustomItems(sourceDS, targetDS, admin);
                    updateProgress(admin, progressGUI, 88);

                    // Step 5: Transfer blacklist
                    updateStatus(admin, progressGUI, "<yellow>Transferring blacklist...");
                    updateProgress(admin, progressGUI, 90);
                    transferBlacklist(sourceDS, targetDS, admin);
                    updateProgress(admin, progressGUI, 98);

                    // Done
                    updateProgress(admin, progressGUI, 100);
                    updateStatus(admin, progressGUI, "<green>Conversion Complete!");

                    // Reload plugin storage if target matches config
                    if (target == DatabaseConfig.get().storageMethod) {
                        plugin.getStorage().loadOrders().thenAccept(orders -> {
                            plugin.getDataCache().setOrders(orders);
                        });
                    }

                    admin.sendRichMessage("<green>Conversion completed successfully!");
                    admin.sendRichMessage("<gray>Orders: <yellow>" + ordersTransferred + " <gray>| Transactions: <yellow>" + transactionsTransferred);

                } finally {
                    sourceDS.close();
                    targetDS.close();
                }

            } catch (Exception e) {
                Log.error("Database conversion failed", e);
                updateStatus(admin, progressGUI, "<red>Conversion failed: " + e.getMessage());
                admin.sendRichMessage("<red>Conversion failed. Check console for details.");
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Data Source Creation
    // ─────────────────────────────────────────────────────────────

    private static HikariDataSource createSourceDataSource(StorageMethod method) {
        try {
            switch (method) {
                case SQLITE -> {
                    HikariConfig config = new HikariConfig();
                    config.setPoolName("converter-source");
                    config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "data.db").getAbsolutePath());
                    return new HikariDataSource(config);
                }
                case MYSQL -> {
                    DatabaseConfig db = DatabaseConfig.get();
                    return createMySQLDataSource("converter-source", db.host, db.port, db.database, db.username, db.password);
                }
                case H2 -> {
                    HikariConfig config = new HikariConfig();
                    config.setPoolName("converter-source");
                    config.setJdbcUrl("jdbc:h2:" + new File(plugin.getDataFolder(), "data-h2").getAbsolutePath() + ";MODE=MySQL");
                    config.setDriverClassName("dcontracts.h2.Driver");
                    config.setUsername("sa");
                    config.setPassword("");
                    return new HikariDataSource(config);
                }
                default -> { return null; }
            }
        } catch (Exception e) {
            Log.error("Failed to create source data source", e);
            return null;
        }
    }

    private static HikariDataSource createTargetDataSource(StorageMethod method, String host, int port, String database, String user, String pass) {
        try {
            switch (method) {
                case SQLITE -> {
                    HikariConfig config = new HikariConfig();
                    config.setPoolName("converter-target");
                    config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "data-converted.db").getAbsolutePath());
                    return new HikariDataSource(config);
                }
                case MYSQL -> {
                    return createMySQLDataSource("converter-target", host, port, database, user, pass);
                }
                case H2 -> {
                    HikariConfig config = new HikariConfig();
                    config.setPoolName("converter-target");
                    config.setJdbcUrl("jdbc:h2:" + new File(plugin.getDataFolder(), "data-converted-h2").getAbsolutePath() + ";MODE=MySQL");
                    config.setDriverClassName("dcontracts.h2.Driver");
                    config.setUsername(user.isEmpty() ? "sa" : user);
                    config.setPassword(pass);
                    return new HikariDataSource(config);
                }
                default -> { return null; }
            }
        } catch (Exception e) {
            Log.error("Failed to create target data source", e);
            return null;
        }
    }

    private static HikariDataSource createMySQLDataSource(String poolName, String host, int port, String database, String user, String pass) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setDriverClassName("dcontracts.mysql.cj.jdbc.Driver");
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(10000);
        return new HikariDataSource(config);
    }

    // ─────────────────────────────────────────────────────────────
    // Target Table Creation
    // ─────────────────────────────────────────────────────────────

    private static void createTargetTables(HikariDataSource ds, StorageMethod method) throws SQLException {
        String orderTableSQL;
        if (method == StorageMethod.SQLITE) {
            orderTableSQL = "CREATE TABLE IF NOT EXISTS contracts_orders (id INTEGER PRIMARY KEY, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
        } else {
            orderTableSQL = "CREATE TABLE IF NOT EXISTS contracts_orders (id INTEGER PRIMARY KEY AUTO_INCREMENT, owner_most BIGINT, owner_least BIGINT, item BLOB, money_per DOUBLE, amount INT, delivered INT DEFAULT 0, in_storage INT DEFAULT 0, expires_at BIGINT)";
        }
        String transactionTableSQL = "CREATE TABLE IF NOT EXISTS contracts_transactions (time BIGINT PRIMARY KEY, player_most BIGINT, player_least BIGINT, `before` DOUBLE, amount DOUBLE, `after` DOUBLE)";
        String customItemsTableSQL = "CREATE TABLE IF NOT EXISTS contracts_custom_items_v2 (item BLOB, search VARCHAR(65535))";
        String blacklistTableSQL = "CREATE TABLE IF NOT EXISTS contracts_blacklist (item BLOB)";

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(orderTableSQL)) { stmt.executeUpdate(); }
            try (PreparedStatement stmt = conn.prepareStatement(transactionTableSQL)) { stmt.executeUpdate(); }
            try (PreparedStatement stmt = conn.prepareStatement(customItemsTableSQL)) { stmt.executeUpdate(); }
            try (PreparedStatement stmt = conn.prepareStatement(blacklistTableSQL)) { stmt.executeUpdate(); }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Data Transfer Methods
    // ─────────────────────────────────────────────────────────────

    private static int transferOrders(HikariDataSource source, HikariDataSource target, StorageMethod targetMethod,
                                      Player admin, InventoryGUI progressGUI) throws SQLException {
        List<Order> orders = new ArrayList<>();

        try (Connection conn = source.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM contracts_orders");
             ResultSet rs = stmt.executeQuery()) {
            orders = ConvertUtils.convertOrders(rs);
        }

        if (orders.isEmpty()) {
            updateStatus(admin, progressGUI, "<gray>No orders to transfer.");
            return 0;
        }

        int total = orders.size();
        AtomicInteger processed = new AtomicInteger(0);

        String insertSQL;
        if (targetMethod == StorageMethod.SQLITE) {
            insertSQL = "INSERT OR REPLACE INTO contracts_orders (id, owner_most, owner_least, item, money_per, amount, delivered, in_storage, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            insertSQL = "REPLACE INTO contracts_orders (id, owner_most, owner_least, item, money_per, amount, delivered, in_storage, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = target.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            conn.setAutoCommit(false);

            for (Order order : orders) {
                stmt.setInt(1, order.getId());
                stmt.setLong(2, order.getOwnerUniqueId().getMostSignificantBits());
                stmt.setLong(3, order.getOwnerUniqueId().getLeastSignificantBits());
                stmt.setBytes(4, order.getItem().serializeAsBytes());
                stmt.setDouble(5, order.getMoneyPer());
                stmt.setInt(6, order.getAmount());
                stmt.setInt(7, order.getDelivered());
                stmt.setInt(8, order.getInStorage());
                stmt.setLong(9, order.getExpiresAt());
                stmt.addBatch();

                int p = processed.incrementAndGet();
                if (p % 100 == 0 || p == total) {
                    stmt.executeBatch();
                    conn.commit();
                    int pct = 15 + (p * 35 / total);
                    updateProgress(admin, progressGUI, Math.min(pct, 50));
                    updateStatus(admin, progressGUI, "<yellow>Transferring orders... <white>" + p + "/" + total);
                }
            }
            stmt.executeBatch();
            conn.commit();
        }

        return total;
    }

    private static int transferTransactions(HikariDataSource source, HikariDataSource target,
                                            Player admin, InventoryGUI progressGUI) throws SQLException {
        List<Object[]> transactions = new ArrayList<>();

        try (Connection conn = source.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM contracts_transactions");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                transactions.add(new Object[]{
                        rs.getLong("time"),
                        rs.getLong("player_most"),
                        rs.getLong("player_least"),
                        rs.getDouble("before"),
                        rs.getDouble("amount"),
                        rs.getDouble("after")
                });
            }
        }

        if (transactions.isEmpty()) {
            updateStatus(admin, progressGUI, "<gray>No transactions to transfer.");
            return 0;
        }

        try (Connection conn = target.getConnection();
             PreparedStatement stmt = conn.prepareStatement("REPLACE INTO contracts_transactions (time, player_most, player_least, `before`, amount, `after`) VALUES (?, ?, ?, ?, ?, ?)")) {
            conn.setAutoCommit(false);

            for (int i = 0; i < transactions.size(); i++) {
                Object[] t = transactions.get(i);
                stmt.setLong(1, (Long) t[0]);
                stmt.setLong(2, (Long) t[1]);
                stmt.setLong(3, (Long) t[2]);
                stmt.setDouble(4, (Double) t[3]);
                stmt.setDouble(5, (Double) t[4]);
                stmt.setDouble(6, (Double) t[5]);
                stmt.addBatch();

                if (i % 200 == 0 || i == transactions.size() - 1) {
                    stmt.executeBatch();
                    conn.commit();
                }
            }
        }

        return transactions.size();
    }

    private static void transferCustomItems(HikariDataSource source, HikariDataSource target, Player admin) throws SQLException {
        List<byte[]> items = new ArrayList<>();
        List<String> searches = new ArrayList<>();

        try (Connection conn = source.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM contracts_custom_items_v2");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(rs.getBytes("item"));
                searches.add(rs.getString("search"));
            }
        }

        if (items.isEmpty()) return;

        try (Connection conn = target.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO contracts_custom_items_v2 (item, search) VALUES (?, ?)")) {
            conn.setAutoCommit(false);
            for (int i = 0; i < items.size(); i++) {
                stmt.setBytes(1, items.get(i));
                stmt.setString(2, searches.get(i));
                stmt.addBatch();
                if (i % 200 == 0 || i == items.size() - 1) {
                    stmt.executeBatch();
                    conn.commit();
                }
            }
        }
    }

    private static void transferBlacklist(HikariDataSource source, HikariDataSource target, Player admin) throws SQLException {
        List<byte[]> items = new ArrayList<>();

        try (Connection conn = source.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM contracts_blacklist");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(rs.getBytes("item"));
            }
        }

        if (items.isEmpty()) return;

        try (Connection conn = target.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO contracts_blacklist (item) VALUES (?)")) {
            conn.setAutoCommit(false);
            for (int i = 0; i < items.size(); i++) {
                stmt.setBytes(1, items.get(i));
                stmt.addBatch();
                if (i % 200 == 0 || i == items.size() - 1) {
                    stmt.executeBatch();
                    conn.commit();
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Progress Bar & Status Updates
    // ─────────────────────────────────────────────────────────────

    private static void updateProgress(Player admin, InventoryGUI gui, int pct) {
        int filledSlots = (pct * 35) / 100;
        DispatchUtil.entity(admin, () -> {
            for (int i = 9; i < 9 + 35; i++) {
                if (i < 9 + filledSlots) {
                    ItemStack filled = ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);
                    filled.editMeta(m -> m.displayName(MM.deserialize("<green>" + pct + "%").decoration(TextDecoration.ITALIC, false)));
                    gui.addItem(new InventoryItem(filled, ev -> {}), i);
                } else {
                    ItemStack empty = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
                    empty.editMeta(m -> m.displayName(Component.empty()));
                    gui.addItem(new InventoryItem(empty, ev -> {}), i);
                }
            }
        });
    }

    private static void updateStatus(Player admin, InventoryGUI gui, String status) {
        DispatchUtil.entity(admin, () -> {
            ItemStack statusItem = ItemStack.of(Material.PAPER);
            statusItem.editMeta(m -> m.displayName(MM.deserialize(status).decoration(TextDecoration.ITALIC, false)));
            statusItem.editMeta(m -> {
                m.lore(List.of(
                        Component.empty(),
                        MM.deserialize("<gray>Do not close this GUI or shut down the server.").decoration(TextDecoration.ITALIC, false)
                ));
            });
            gui.addItem(new InventoryItem(statusItem, ev -> {}), 22);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // GUI Item Builders
    // ─────────────────────────────────────────────────────────────

    private static InventoryItem storageTypeItem(StorageMethod method, String name, String description,
                                                  Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        Material mat = switch (method) {
            case SQLITE -> Material.STONE;
            case MYSQL -> Material.CONDUIT;
            case H2 -> Material.COPPER_BLOCK;
        };

        ItemStack item = ItemStack.of(mat);
        item.editMeta(meta -> {
            meta.displayName(MM.deserialize("<gold>" + name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    MM.deserialize(description).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    MM.deserialize("<yellow>Click to select").decoration(TextDecoration.ITALIC, false)
            ));
        });
        return new InventoryItem(item, action);
    }

    private static InventoryItem buildBackItem(Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.editMeta(meta -> {
            meta.displayName(MM.deserialize("<red>Back").decoration(TextDecoration.ITALIC, false));
        });
        return new InventoryItem(item, action);
    }
}
