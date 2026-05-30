package me.karven.orderium.config;

import com.google.common.io.Files;
import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.karven.orderium.config.util.SignGUIConfig;
import me.karven.orderium.config.util.dialog.ConfirmDeliveryDialogConfig;
import me.karven.orderium.config.util.dialog.NewOrderDialogConfig;
import me.karven.orderium.config.util.dialog.mangeorderdialog.CancelOrderDialogConfig;
import me.karven.orderium.config.util.dialog.mangeorderdialog.CollectItemsDialogConfig;
import me.karven.orderium.config.util.dialog.mangeorderdialog.ManageOrderDialogConfig;
import me.karven.orderium.config.util.gui.*;
import me.karven.orderium.utils.Log;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Config {
    public static final File dataFolder = new File("plugins" + File.separator + "Orderium");

    public final ConfigFile configFile;

    public final MainGUIConfig mainGUIConfig = new MainGUIConfig();
    public final YourOrdersGUIConfig yourOrdersGUIConfig = new YourOrdersGUIConfig();
    public final ChooseItemGUIConfig chooseItemGUIConfig = new ChooseItemGUIConfig();
    public final SignGUIConfig signGUIConfig = new SignGUIConfig();
    public final EnchantGUIConfig enchantGUIConfig = new EnchantGUIConfig();
    public final DeliverGUIConfig deliverGUIConfig = new DeliverGUIConfig();
    public final NewOrderDialogConfig newOrderDialogConfig = new NewOrderDialogConfig();
    public final ConfirmDeliveryDialogConfig confirmDeliveryDialogConfig = new ConfirmDeliveryDialogConfig();
    public final ManageOrderDialogConfig manageOrderDialogConfig = new ManageOrderDialogConfig();
    public final CollectItemsDialogConfig collectItemsDialogConfig = new CollectItemsDialogConfig();
    public final CancelOrderDialogConfig cancelOrderDialogConfig = new CancelOrderDialogConfig();

    public Config() {
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            dataFolder.mkdirs();
        }
        try {
            configFile = ConfigFile.loadConfig(new File(dataFolder, "config.yml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final int configVersion = configFile.getInteger("config-version", -1);

        if (configFile.isNew() || configVersion >= 5) {
            setDefaults();
            return;
        }

        // Migrating old config file
        final File backupConfig = new File(dataFolder, "config.yml.old");
        try {
            Files.copy(new File(dataFolder, "config.yml"), backupConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Add missing values to config
        ConfigMigration.migrateV4(configFile);

        // Convert GUI config sections to their respective files
        mainGUIConfig.migrateV5(configFile);
        yourOrdersGUIConfig.migrateV5(configFile);
        chooseItemGUIConfig.migrateV5(configFile);
        signGUIConfig.migrateV5(configFile);
        enchantGUIConfig.migrateV5(configFile);
        deliverGUIConfig.migrateV5(configFile);
        newOrderDialogConfig.migrateV5(configFile);
        confirmDeliveryDialogConfig.migrateV5(configFile);

        // Manage Order Dialogs
        manageOrderDialogConfig.migrateV5(configFile);
        collectItemsDialogConfig.migrateV5(configFile);
        cancelOrderDialogConfig.migrateV5(configFile);

        // Remove the gui section entirely after migration
        configFile.set("gui", null);

        try {
            configFile.save();
        } catch (Exception e) {
            Log.error("Failed to migrate config", e);
        }
    }

    private void setDefaults() {
        mainGUIConfig.applyDefaultValues();
        yourOrdersGUIConfig.applyDefaultValues();
        chooseItemGUIConfig.applyDefaultValues();
        signGUIConfig.applyDefaultValues();
        enchantGUIConfig.applyDefaultValues();
        deliverGUIConfig.applyDefaultValues();
        newOrderDialogConfig.applyDefaultValues();
        confirmDeliveryDialogConfig.applyDefaultValues();
        manageOrderDialogConfig.applyDefaultValues();
        collectItemsDialogConfig.applyDefaultValues();
        cancelOrderDialogConfig.applyDefaultValues();
    }
}
