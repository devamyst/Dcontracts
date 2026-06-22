package me.devamy.contracts.obj.orderitem;

import com.google.common.collect.ImmutableList;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VanillaItem implements EnchantableItem {
    private ItemStack item;
    private final List<Enchantment> enchantable = new ArrayList<>();

    /**
     * Create a vanilla item
     * @param item the bukkit item stack
     * @param autoGenerateEnchantable if it should check all enchantments and see which ones are enchantable
     */
    public VanillaItem(@NotNull ItemStack item, boolean autoGenerateEnchantable) {
        this.item = item;

        if (!autoGenerateEnchantable) return;
        Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        for (Enchantment enchantment : enchantmentRegistry) {
            if (!enchantment.canEnchantItem(item) || enchantment.isCursed()) continue;
            enchantable.add(enchantment);
        }
    }

    @Override
    public void addEnchantable(@NotNull Enchantment enchantment) {
        enchantable.add(enchantment);
    }

    @Override
    public void removeEnchantable(@NotNull Enchantment enchantment) {
        enchantable.remove(enchantment);
    }

    @Override
    public void setEnchantable(@NotNull List<@NotNull Enchantment> enchantments) {
        enchantable.clear();
        enchantable.addAll(enchantments);
    }

    @Override
    public @NotNull ImmutableList<@NotNull Enchantment> getEnchantable() {
        return ImmutableList.copyOf(enchantable);
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return item.clone();
    }

    public @NotNull VanillaItem copy() {
        VanillaItem clone = new VanillaItem(item.clone(), false);
        clone.setEnchantable(enchantable);
        return clone;
    }

    @Override
    public void setItemStack(@NotNull ItemStack itemStack) {
        this.item = itemStack;
    }
}
