package us.to.midensthings.nouveauEnchanting.compat;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CraftEngineCompat {

    public boolean isCustomItem(ItemStack item) {
        return CraftEngineItems.isCustomItem(item);
    }

    public String getItemID(ItemStack item) {
        return CraftEngineItems.getCustomItemId(item).value;
    }
    public ItemStack getItemStack(String namespacedItemID) {
        BukkitItemDefinition item = CraftEngineItems.byId(namespacedItemID);
        if (item != null) {
            return item.buildBukkitItem();
        }
        // default to enchanted book if item not found
        return ItemStack.of(Material.ENCHANTED_BOOK);
    }
}
