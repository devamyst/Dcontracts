package me.devamy.contracts.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.devamy.contracts.config.util.SignGUIConfig;
import me.devamy.contracts.config.util.chestgui.*;
import me.devamy.contracts.config.util.dialog.ConfirmDeliveryDialogConfig;
import me.devamy.contracts.config.util.dialog.ManageContractDialogConfig;
import me.devamy.contracts.config.util.dialog.NewContractDialogConfig;
import me.devamy.contracts.gui.ContractAdminGUI;
import me.devamy.contracts.gui.AdminToolGUI;
import me.devamy.contracts.gui.ChooseItemGUI;
import me.devamy.contracts.obj.OrderStatus;
import me.devamy.contracts.obj.SortType;
import me.devamy.contracts.utils.DispatchUtil;
import me.devamy.contracts.utils.Log;
import me.devamy.contracts.utils.ServerSoftware;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.devamy.contracts.Contracts.plugin;

public class Config {
    private static final AtomicBoolean reloading = new AtomicBoolean(false);
    public static volatile Config config;
    public static final int CURRENT_CONFIG_VERSION = 7;
    public final File javaConfigFile = new File(plugin.getDataFolder(), "config.yml");
    public final File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

    public ConfigFile configFile;
    public ConfigFile messagesConfig;

    public final MainGUIConfig mainGUIConfig = new MainGUIConfig();
    public final YourContractsGUIConfig yourContractsGUIConfig = new YourContractsGUIConfig();
    public final ChooseItemGUIConfig chooseItemGUIConfig = new ChooseItemGUIConfig();
    public final SignGUIConfig signGUIConfig = new SignGUIConfig();
    public final EnchantGUIConfig enchantGUIConfig = new EnchantGUIConfig();
    public final DeliverGUIConfig deliverGUIConfig = new DeliverGUIConfig();
    public final NewContractDialogConfig newContractDialogConfig = new NewContractDialogConfig();
    public final ConfirmDeliveryDialogConfig confirmDeliveryDialogConfig = new ConfirmDeliveryDialogConfig();
    public final ManageContractDialogConfig manageContractDialogConfig = new ManageContractDialogConfig();
    public final ContractAdminGUIConfig contractAdminGUIConfig = new ContractAdminGUIConfig();

    public boolean bStats;
    public boolean checkForUpdates;

    public String invalidInput;
    public String contractCreationSuccessful;
    public String deliver;
    public String receiveDelivery;
    public String notEnoughMoney;
    public String deliverSelf;
    public String collectingTooFast;
    public String exceedMaxCollect;
    public String contractCreationBroadcast;

    public Sound nextPageSound;
    public Sound previousPageSound;
    public Sound refreshSound;
    public Sound sortSound;
    public Sound newContractSound;
    public Sound deliverSound;

    public boolean logTransactions;
    public long expiresAfter;
    public double minPrice;
    public int maxCollectPerMinute;
    public int maxCollect;
    public boolean shulkerDelivering;
    public boolean broadcastContractCreation;
    public boolean parallelProcessing;

    public final List<NamespacedKey> similarityCheck = new ArrayList<>();

    public Config() throws Exception {
        try {
            configFile = ConfigFile.loadConfig(javaConfigFile);
            messagesConfig = ConfigFile.loadConfig(messagesFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (configFile.isNew()) {
            setDefaults();
            save();
            load();
        } else {
            if (!ConfigMigrator.findMissingKeys("default-config.yml", javaConfigFile).isEmpty()) {
                setDefaults();
                save();
            }
            if (messagesConfig.isNew() || !ConfigMigrator.findMissingKeys("default-messages.yml", messagesFile).isEmpty()) {
                setDefaultMessages();
                try {
                    messagesConfig.save();
                } catch (Exception e) {
                    Log.error("Failed to save messages.yml", e);
                }
            }
            ContractsMigration.perform(this);
        }
    }

    public void save() throws Exception {
        configFile.set("config-version", CURRENT_CONFIG_VERSION);
        configFile.save();
        try {
            messagesConfig.save();
        } catch (Exception e) {
            Log.error("Failed to save messages.yml", e);
        }
        mainGUIConfig.saveToFile();
        yourContractsGUIConfig.saveToFile();
        chooseItemGUIConfig.saveToFile();
        signGUIConfig.saveToFile();
        enchantGUIConfig.saveToFile();
        deliverGUIConfig.saveToFile();
        newContractDialogConfig.saveToFile();
        confirmDeliveryDialogConfig.saveToFile();
        manageContractDialogConfig.saveToFile();
        contractAdminGUIConfig.saveToFile();
    }

    public void setDefaults() {
        mainGUIConfig.applyDefaultValues();
        yourContractsGUIConfig.applyDefaultValues();
        chooseItemGUIConfig.applyDefaultValues();
        signGUIConfig.applyDefaultValues();
        enchantGUIConfig.applyDefaultValues();
        deliverGUIConfig.applyDefaultValues();
        newContractDialogConfig.applyDefaultValues();
        confirmDeliveryDialogConfig.applyDefaultValues();
        manageContractDialogConfig.applyDefaultValues();
        contractAdminGUIConfig.applyDefaultValues();

        mainGUIConfig.setDefault();
        yourContractsGUIConfig.setDefault();
        chooseItemGUIConfig.setDefault();
        signGUIConfig.setDefault();
        enchantGUIConfig.setDefault();
        deliverGUIConfig.setDefault();
        newContractDialogConfig.setDefault();
        confirmDeliveryDialogConfig.setDefault();
        manageContractDialogConfig.setDefault();
        contractAdminGUIConfig.setDefault();

        configFile.addDefault("bstats", true, "Whether to let bStats collect anonymous usage statistics");
        configFile.addDefault("check-for-updates", true, "Whether to check for plugin updates on startup");
        configFile.addDefault("log-transactions", true, "Whether to log player money transactions (Vault) to console");
        configFile.addDefault("expires-after", 7L * 24L * 60L * 60L * 1000L, "After how many milliseconds a contract expires (default: 7 days)");
        configFile.addDefault("minimum-price", 0.1, "Minimum price per item a player can set when creating a contract");
        configFile.addDefault("max-collect", 1000, "Maximum items a player can collect from a single contract at once");
        configFile.addDefault("max-collect-per-minute", 1000,
                """
                Global rate limit: max items collected per minute across all contracts.
                Setting this too high may let players lag the server on large deliveries.
                """
        );
        configFile.addDefault("similarity-check", List.of(
                "minecraft:enchantments",
                "minecraft:bundle_contents",
                "minecraft:container",
                "minecraft:fireworks",
                "minecraft:instrument",
                "minecraft:potion_contents",
                "minecraft:stored_enchantments",
                "minecraft:max_stack_size",
//                "minecraft:custom_data", // Custom data doesn't exist in the registry for some reason?
                "minecraft:custom_model_data",
                "minecraft:ominous_bottle_amplifier",
                "minecraft:damage",
                "minecraft:custom_name",
                "minecraft:item_model",
                "minecraft:damage_type",
                "minecraft:consumable"
        ),
                """
                Defines how two items are considered "similar" for delivery matching.
                If all the listed data components match between items (regardless of item type),
                they are treated as the same item for contract delivery.
                See: https://minecraft.wiki/w/Data_component_format#List_of_components
                """
        );

        configFile.addDefault("shulker-delivering", true, "Whether players can deliver contracts using shulker boxes containing items");
        configFile.addDefault("broadcast-contract-creation", false, "Whether to broadcast a server-wide message when a contract is created");
        configFile.addDefault("parallel-processing", ServerSoftware.isParallelSupported(),
                """
                Whether to use parallel processing for expensive operations (loading, GUI building, etc.).
                Enabled by default on Paper, Purpur, Pufferfish, and Folia.
                Disable if you experience stability issues.
                """);

        for (final SortType sort : SortType.values()) {
            configFile.addDefault("sorts-display.active." + sort.getIdentifier(), sort.getDisplayActive());
            configFile.addDefault("sorts-display.inactive." + sort.getIdentifier(), sort.getDisplayInactive());
        }

        for (final OrderStatus status : OrderStatus.values()) {
            configFile.addDefault("contract-status." + status.getIdentifier(), status.getText());
        }

        setDefaultMessages();
        setDefaultSounds();
    }
    public static CompletableFuture<Void> reloadAsync() {
        if (!reloading.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        final CompletableFuture<Void> future = new CompletableFuture<>();
        DispatchUtil.async(() -> {

            try {
                reload();
            } catch (Exception e) {
                Log.error("Failed to reload config", e);
                future.completeExceptionally(e);
                reloading.set(false);
                return;
            }
            future.complete(null);
            reloading.set(false);
        });
        return future;
    }

    public static void reload() throws Exception {
        config = new Config();

        plugin.reloadBStats(config);
        ChooseItemGUI.init();

        AdminToolGUI.createBlacklist();
        AdminToolGUI.createCustomItems();
    }

    public void reloadGUIs() {
        mainGUIConfig.reload();
        yourContractsGUIConfig.reload();
        chooseItemGUIConfig.reload();
        signGUIConfig.reload();
        enchantGUIConfig.reload();
        deliverGUIConfig.reload();
        newContractDialogConfig.reload();
        confirmDeliveryDialogConfig.reload();
        manageContractDialogConfig.reload();
        contractAdminGUIConfig.reload();
    }

    public void reloadGUIsFromFile() {
        mainGUIConfig.reloadFromFile();
        yourContractsGUIConfig.reloadFromFile();
        chooseItemGUIConfig.reloadFromFile();
        signGUIConfig.reloadFromFile();
        enchantGUIConfig.reloadFromFile();
        deliverGUIConfig.reloadFromFile();
        newContractDialogConfig.reloadFromFile();
        confirmDeliveryDialogConfig.reloadFromFile();
        manageContractDialogConfig.reloadFromFile();
        contractAdminGUIConfig.reloadFromFile();
    }


    public void load() {
        for (final SortType sort : SortType.values()) {
            final String displayActive = configFile.getString("sorts-display.active." + sort.getIdentifier());
            final String displayInactive = configFile.getString("sorts-display.inactive." + sort.getIdentifier());
            if (displayActive != null) sort.setDisplayActive(displayActive);
            if (displayInactive != null) sort.setDisplayInactive(displayInactive);
        }

        for (final OrderStatus status : OrderStatus.values()) {
            final String display = configFile.getString("contract-status." + status.getIdentifier());
            if (display != null) status.setText(display);
        }

        bStats = configFile.getBoolean("bstats");
        checkForUpdates = configFile.getBoolean("check-for-updates");
        logTransactions = configFile.getBoolean("log-transactions");
        expiresAfter = configFile.getLong("expires-after");
        minPrice = configFile.getDouble("minimum-price");
        maxCollect = configFile.getInteger("max-collect");
        maxCollectPerMinute = configFile.getInteger("max-collect-per-minute");
        shulkerDelivering = configFile.getBoolean("shulker-delivering");
        broadcastContractCreation = configFile.getBoolean("broadcast-contract-creation");
        parallelProcessing = configFile.getBoolean("parallel-processing");

        final List<String> rawDataComponents = configFile.getStringList("similarity-check");
        similarityCheck.clear();
        for (final String s : rawDataComponents) {
            final String[] components = s.split(":");
            if (components.length != 2) {
                Log.warn("Invalid key for similarity-check: " + s);
                continue;
            }
            similarityCheck.add(new NamespacedKey(components[0], components[1]));
        }

        contractCreationSuccessful = messagesConfig.getString("create-contract-success");
        invalidInput = messagesConfig.getString("invalid-input");
        deliver = messagesConfig.getString("deliver");
        receiveDelivery = messagesConfig.getString("receive-delivery");
        notEnoughMoney = messagesConfig.getString("not-enough-money");
        deliverSelf = messagesConfig.getString("deliver-self");
        exceedMaxCollect = messagesConfig.getString("exceeded-max-collect");
        collectingTooFast = messagesConfig.getString("collecting-too-fast");
        contractCreationBroadcast = messagesConfig.getString("contract-creation-broadcast");

        nextPageSound = getSound("next-page");
        previousPageSound = getSound("previous-page");
        refreshSound = getSound("refresh");
        sortSound = getSound("sort");
        newContractSound = getSound("new-contract");
        deliverSound = getSound("deliver");

        plugin.reloadBStats(this);
    }

    private Sound getSound(String name) {
        final String soundKey = configFile.getString("sounds." + name + ".sound");
        if (soundKey == null) {
            Log.warn("Missing sound key for '" + name + "'. Using default click sound.");
            return Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1.0f, 1.0f);
        }
        final String[] components = soundKey.split(":");
        final String namespace, key;
        if (components.length != 2) {
            Log.warn("Invalid key for sound: " + soundKey + ". Using default: \"dcontracts:unknown_sound\"");
            namespace = "dcontracts";
            key = "unknown_sound";
        } else {
            namespace = components[0];
            key = components[1];
        }


        return Sound.sound(new NamespacedKey(namespace, key), Sound.Source.UI, configFile.getFloat("sounds." + name + ".volume"), configFile.getFloat("sounds." + name + ".pitch"));
    }

    private void setDefaultSound(String name, Sound sound) {
        configFile.addDefault("sounds." + name + ".sound", sound.name().asString(), "Sound effect for " + name);
        configFile.addDefault("sounds." + name + ".volume", sound.volume(), "Volume (0.0 - 1.0)");
        configFile.addDefault("sounds." + name + ".pitch", sound.pitch(), "Pitch (0.5 - 2.0)");
    }

    private void setDefaultSounds() {
        final Sound clickSound = Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.UI, 1, 1);
        setDefaultSound("next-page", clickSound);
        setDefaultSound("previous-page", clickSound);
        setDefaultSound("refresh", clickSound);
        setDefaultSound("sort", clickSound);
        setDefaultSound("new-contract", Sound.sound(Key.key("minecraft:entity.villager.work_cartographer"), Sound.Source.UI, 1, 1));
        setDefaultSound("deliver", Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.UI, 1, 2));
    }

    private void setDefaultMessages() {
        messagesConfig.addDefault("create-contract-success", "<gray>Your contract has been created", "Shown when a contract is created successfully");
        messagesConfig.addDefault("invalid-input", "<red>Invalid number or format", "Shown when a player enters invalid input");
        messagesConfig.addDefault("deliver", "<gray>You earned <green>$<money><gray> from delivering a contract", "Shown to the deliverer upon successful delivery");
        messagesConfig.addDefault("receive-delivery", "<aqua><deliverer> <gray>delivered you <aqua><amount> <item>", "Shown to the contract owner when someone delivers");
        messagesConfig.addDefault("not-enough-money", "<red>You do not have enough money", "Shown when a player lacks funds");
        messagesConfig.addDefault("deliver-self", "<red>You cannot deliver your own contract", "Shown when a player tries to deliver their own contract");
        messagesConfig.addDefault("exceeded-max-collect", "<red>You are collecting too many items", "Shown when exceeding max-collect limit");
        messagesConfig.addDefault("collecting-too-fast", "<red>You are collecting items too fast. Wait a minute...", "Shown when exceeding max-collect-per-minute rate limit");
        messagesConfig.addDefault("contract-creation-broadcast", "<green><player> <white>has just created a new contract for <green><item> <white>in <gray>/contracts", "Broadcast message when a contract is created");
    }
}
