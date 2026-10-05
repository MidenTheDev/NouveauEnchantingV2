package us.to.midensthings.nouveauEnchanting.compat;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.setting.ItemSettingsModifierType;
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
        return CraftEngineItems.byId(namespacedItemID).buildBukkitItem();
    }
}
