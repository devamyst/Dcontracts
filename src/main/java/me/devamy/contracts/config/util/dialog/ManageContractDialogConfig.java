package me.devamy.contracts.config.util.dialog;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.devamy.contracts.config.util.GUIConfigFile;
import me.devamy.contracts.config.util.IConfigFile;
import me.devamy.contracts.config.util.component.dialog.DialogButtonConfig;
import me.devamy.contracts.config.util.component.dialog.ItemlessItemDialogBodyConfig;
import me.devamy.contracts.config.util.component.dialog.TextDialogInputConfig;
import me.devamy.contracts.utils.Log;
import me.devamy.contracts.utils.Values;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ManageContractDialogConfig extends GUIConfigFile {
    public final ManageContract manageContract = new ManageContract(this);
    public final CollectItems collectItems = new CollectItems(this);
    public final CancelContract cancelContract = new CancelContract(this);

    public ManageContractDialogConfig() {
        super("manage-contract");
    }

    @Override
    public void reload() {
        manageContract.reload();
        collectItems.reload();
        cancelContract.reload();
    }

    @Override
    public void save() {
        manageContract.save();
        collectItems.save();
        cancelContract.save();
    }

    @Override
    public void setDefault() {
        manageContract.setDefault();
        collectItems.setDefault();
        cancelContract.setDefault();
    }

    @Override
    public void migrateV5(@NotNull ConfigFile oldConfig) {
        manageContract.migrateV5(oldConfig);
        collectItems.migrateV5(oldConfig);
        cancelContract.migrateV5(oldConfig);

        save();

        try {
            config.save();
        } catch (Exception e) {
            Log.error("Failed to migrate config", e);
        }
    }

    @Override
    public void applyDefaultValues() {
        manageContract.applyDefaultValues();
        collectItems.applyDefaultValues();
        cancelContract.applyDefaultValues();
    }

    public static class ManageContract implements IConfigFile {
        private final ManageContractDialogConfig parent;
        public String title;
        public boolean canCloseWithEsc;
        public final @NotNull DialogButtonConfig collectItemsButtonConfig = new DialogButtonConfig("manage-contract.buttons.collect-items");
        public final @NotNull DialogButtonConfig cancelContractButtonConfig = new DialogButtonConfig("manage-contract.buttons.cancel-contract");

        public ManageContract(final @NotNull ManageContractDialogConfig parent) {
            this.parent = parent;
        }

        public @NotNull Dialog dialog(
                final @NotNull DialogActionCallback clickCollectItemsButtonAction,
                final @NotNull DialogActionCallback clickCancelContractButtonAction
        ) {
            return Dialog.create(builder -> builder.empty()
                    .type(DialogType.multiAction(List.of(
                            collectItemsButtonConfig.button(clickCollectItemsButtonAction),
                            cancelContractButtonConfig.button(clickCancelContractButtonAction)
                    )).build())
                    .base(DialogBase.builder(Values.minimessage.deserialize(title))
                            .pause(false)
                            .afterAction(DialogBase.DialogAfterAction.NONE)
                            .canCloseWithEscape(canCloseWithEsc)
                            .build()
                    )
            );
        }

        @Override
        public void reload() {
            title = parent.config.getString("manage-contract.title");
            canCloseWithEsc = parent.config.getBoolean("manage-contract.can-close-with-escape");
            collectItemsButtonConfig.reload(parent.config);
            cancelContractButtonConfig.reload(parent.config);
        }

        @Override
        public void save() {
            parent.config.set("manage-contract.title", title);
            parent.config.set("manage-contract.can-close-with-escape", canCloseWithEsc);
            collectItemsButtonConfig.save(parent.config);
            cancelContractButtonConfig.save(parent.config);
        }

        @Override
        public void setDefault() {
            parent.config.addDefault("manage-contract.title", title);
            parent.config.addDefault("manage-contract.can-close-with-escape", canCloseWithEsc);
            collectItemsButtonConfig.setDefault(parent.config);
            cancelContractButtonConfig.setDefault(parent.config);
        }

        @Override
        public void migrateV5(@NotNull ConfigFile oldConfig) {
            title = oldConfig.getString("gui.manage-order.title");
            canCloseWithEsc = true;
            collectItemsButtonConfig.migrateV5(oldConfig, "gui.manage-order.collect-items");
            cancelContractButtonConfig.migrateV5(oldConfig, "gui.manage-order.cancel-order");
        }

        @Override
        public void applyDefaultValues() {
            canCloseWithEsc = true;
            title = "Manage Contract";
            collectItemsButtonConfig.label = "Collect Items";
            collectItemsButtonConfig.tooltip = "Click to collect items from this contract";
            collectItemsButtonConfig.width = 150;
            cancelContractButtonConfig.label = "Cancel Contract";
            cancelContractButtonConfig.tooltip = "Click to cancel the contract";
            cancelContractButtonConfig.width = 150;
        }
    }

    public static class CollectItems implements IConfigFile {
        private final ManageContractDialogConfig parent;
        public String title;
        public boolean canCloseWithEsc;
        public final @NotNull DialogButtonConfig yesButton = new DialogButtonConfig("collect-items.buttons.confirm");
        public final @NotNull DialogButtonConfig noButton = new DialogButtonConfig("collect-items.buttons.cancel");
        public final @NotNull ItemlessItemDialogBodyConfig body = new ItemlessItemDialogBodyConfig("collect-items.body");
        public final @NotNull TextDialogInputConfig amountInputConfig = new TextDialogInputConfig("collect-items.amount-input");

        public CollectItems(final @NotNull ManageContractDialogConfig parent) {
            this.parent = parent;
        }

        public @NotNull Dialog dialog(final @NotNull ItemStack item, final @NotNull TagResolver @NotNull [] placeholders, final @Nullable DialogActionCallback yesAction, final @Nullable DialogActionCallback noAction) {
            return Dialog.create(builder -> builder.empty()
                    .type(DialogType.confirmation(
                            yesButton.button(yesAction),
                            noButton.button(noAction)
                    ))
                    .base(DialogBase.builder(Values.minimessage.deserialize(title))
                            .canCloseWithEscape(canCloseWithEsc)
                            .body(List.of(body.body(item, placeholders)))
                            .inputs(List.of(amountInputConfig.input("amount")))
                            .build()
                    )
            );
        }

        @Override
        public void reload() {
            title = parent.config.getString("collect-items.title");
            canCloseWithEsc = parent.config.getBoolean("collect-items.can-close-with-escape");
            body.reload(parent.config);
            yesButton.reload(parent.config);
            noButton.reload(parent.config);
            amountInputConfig.reload(parent.config);
        }

        @Override
        public void save() {
            parent.config.set("collect-items.title", title);
            parent.config.set("collect-items.can-close-with-escape", canCloseWithEsc);
            body.save(parent.config);
            yesButton.save(parent.config);
            noButton.save(parent.config);
            amountInputConfig.save(parent.config);
        }

        @Override
        public void setDefault() {
            parent.config.addDefault("collect-items.title", title);
            parent.config.addDefault("collect-items.can-close-with-escape", canCloseWithEsc);
            body.setDefault(parent.config);
            yesButton.setDefault(parent.config);
            noButton.setDefault(parent.config);
            amountInputConfig.setDefault(parent.config);
        }

        @Override
        public void migrateV5(@NotNull ConfigFile oldConfig) {
            title = oldConfig.getString("gui.collect-items.title");
            canCloseWithEsc = true;
            body.migrateV5(oldConfig, "gui.collect-items.body");
            yesButton.migrateV5(oldConfig, "gui.collect-items.confirm");
            noButton.migrateV5(oldConfig, "gui.collect-items.cancel");
            amountInputConfig.migrateV5(oldConfig, "gui.collect-items.amount-label");
        }

        @Override
        public void applyDefaultValues() {
            title = "Collect Items";
            canCloseWithEsc = true;
            yesButton.label = "<green>Confirm";
            yesButton.tooltip = "Click to confirm";
            yesButton.width = 150;
            noButton.label = "<red>Cancel";
            noButton.tooltip = "Click to cancel";
            noButton.width = 150;
            body.description.contents = "You are collecting items from this contract. You can collect up to <aqua><in-storage> <item>";
            body.description.width = 200;
            body.width = 16;
            body.height = 16;
            body.showDecoration = true;
            body.showTooltip = true;
            amountInputConfig.label = "Amount";
            amountInputConfig.width = 200;
            amountInputConfig.labelVisible = true;
            amountInputConfig.initial = "1";
            amountInputConfig.maxLength = 32;
        }
    }

    public static class CancelContract implements IConfigFile {
        private final ManageContractDialogConfig parent;
        public String title;
        public boolean canCloseWithEsc;
        public final @NotNull DialogButtonConfig yesButton = new DialogButtonConfig("collect-items.buttons.confirm");
        public final @NotNull DialogButtonConfig noButton = new DialogButtonConfig("collect-items.buttons.cancel");
        public final @NotNull ItemlessItemDialogBodyConfig body = new ItemlessItemDialogBodyConfig("cancel-contract.body");

        public CancelContract(final @NotNull ManageContractDialogConfig parent) {
            this.parent = parent;
        }

        public @NotNull Dialog dialog(final @NotNull ItemStack item, final @NotNull DialogActionCallback yesAction, final @Nullable DialogActionCallback noAction) {
            return Dialog.create(builder -> builder.empty()
                    .type(DialogType.confirmation(
                            yesButton.button(yesAction),
                            noButton.button(noAction)
                    ))
                    .base(DialogBase.builder(Values.minimessage.deserialize(title))
                            .canCloseWithEscape(canCloseWithEsc)
                            .pause(false)
                            .afterAction(DialogBase.DialogAfterAction.NONE)
                            .body(List.of(body.body(item)))
                            .build()
                    )
            );
        }

        @Override
        public void reload() {
            title = parent.config.getString("cancel-contract.title");
            canCloseWithEsc = parent.config.getBoolean("cancel-contract.can-close-with-escape");
            body.reload(parent.config);
            yesButton.reload(parent.config);
            noButton.reload(parent.config);
        }

        @Override
        public void save() {
            parent.config.set("cancel-contract.title", title);
            parent.config.set("cancel-contract.can-close-with-escape", canCloseWithEsc);
            body.save(parent.config);
            yesButton.save(parent.config);
            noButton.save(parent.config);
        }

        @Override
        public void setDefault() {
            parent.config.addDefault("cancel-contract.title", title);
            parent.config.addDefault("cancel-contract.can-close-with-escape", canCloseWithEsc);
            body.setDefault(parent.config);
            yesButton.setDefault(parent.config);
            noButton.setDefault(parent.config);
        }

        @Override
        public void migrateV5(@NotNull ConfigFile oldConfig) {
            title = oldConfig.getString("gui.cancel-order.title");
            canCloseWithEsc = true;
            body.migrateV5(oldConfig, "gui.cancel-order.body");
            yesButton.migrateV5(oldConfig, "gui.cancel-order.confirm");
            noButton.migrateV5(oldConfig, "gui.cancel-order.cancel");
        }

        @Override
        public void applyDefaultValues() {
            title = "Cancel Contract";
            canCloseWithEsc = true;
            yesButton.label = "<green>Confirm";
            yesButton.tooltip = "Click to confirm the cancellation of this contract";
            yesButton.width = 150;
            noButton.label = "<red>Cancel";
            noButton.tooltip = "Click to cancel the cancellation of this contract";
            noButton.width = 150;
            body.description.contents = "You are cancelling this contract. It will be expired";
            body.description.width = 250;
            body.width = 16;
            body.height = 16;
            body.showDecoration = true;
            body.showTooltip = true;
        }
    }
}
