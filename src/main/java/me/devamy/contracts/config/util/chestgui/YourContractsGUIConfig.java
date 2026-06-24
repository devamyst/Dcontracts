package me.devamy.contracts.config.util.chestgui;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.devamy.contracts.config.util.GUIConfigFile;
import me.devamy.contracts.config.util.component.ButtonConfig;
import me.devamy.contracts.config.util.component.ContractConfig;
import me.devamy.contracts.utils.DecoratedText;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

// TODO: Make this GUI paginated
// And scrollable: https://github.com/PaperMC/Paper/pull/13898
public class YourContractsGUIConfig extends GUIConfigFile {
    public String title;
    public int rows;
    public final @NotNull ContractConfig contractConfig = new ContractConfig("contract");
    public final @NotNull ButtonConfig newContractButton = new ButtonConfig("buttons.new-contract");
    
    public YourContractsGUIConfig() {
        super("your-contracts");
    }

    @Override
    public void reload() {
        title = config.getString("title");
        rows = config.getInteger("rows");
        newContractButton.reload(config);
        contractConfig.reload(config);
    }

    @Override
    public void save() {
        config.set("title", title);
        config.set("rows", rows);
        newContractButton.save(config);
        contractConfig.save(config);
    }

    @Override
    public void setDefault() {
        config.addDefault("title", title);
        config.addDefault("rows", rows);
        newContractButton.setDefault(config);
        contractConfig.setDefault(config);
    }

    @Override
    public void migrateV5(final @NotNull ConfigFile oldConfig) {
        title = oldConfig.getString("gui.your-orders.title");
        rows = 3;
        contractConfig.migrateV5(oldConfig, "gui.your-orders.order");
        newContractButton.migrateV5(oldConfig, "gui.your-orders.buttons.new-order", 0);
        newContractButton.slot = 26;

        save();
        try {
            config.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyDefaultValues() {
        title = "Your Contracts";
        rows = 3;
        contractConfig.lore.add("");
        contractConfig.lore.add("<!i><#786500>$<paid><gray>/<#017800>$<total> <gray>Paid");
        contractConfig.lore.add("<!i><#786500><delivered><gray>/<#017800><amount> <gray>Delivered");
        contractConfig.lore.add("<!i><green>$<money-per> <white>each");
        contractConfig.lore.add("");
        contractConfig.lore.add("<!i><contract-status>");
        contractConfig.slots.addAll(IntStream.range(0, 27).boxed().toList());
        contractConfig.itemRepresentation = ItemStack.of(Material.STONE);

        newContractButton.slot = 26;
        newContractButton.itemStack = ItemStack.of(Material.MAP);
        newContractButton.itemStack.editMeta(meta -> {
            meta.displayName(DecoratedText.buttonName("New Contract"));
            meta.lore(List.of(DecoratedText.buttonLore("Click to create a new contract")));
        });
    }
}
