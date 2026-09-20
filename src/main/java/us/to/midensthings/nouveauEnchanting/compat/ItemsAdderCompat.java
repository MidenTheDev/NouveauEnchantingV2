package us.to.midensthings.nouveauEnchanting.compat;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;

public class ItemsAdderCompat {


    public boolean isCustomItem(ItemStack item) {
        CustomStack customItem = CustomStack.byItemStack(item);

        if (customItem == null) {
            return false;
        }
        return true;
    }


    public String getItemID(ItemStack material) {
        CustomStack customStack = CustomStack.byItemStack(material);
        return customStack.getId();
    }

    public ItemStack getItemStack(String namespacedItemID) {
        CustomStack customStack = CustomStack.getInstance(namespacedItemID);
        if (customStack != null) {
            return customStack.getItemStack();
        }
        // default to enchanted book if item not found
        return ItemStack.of(Material.ENCHANTED_BOOK);
    }


}
