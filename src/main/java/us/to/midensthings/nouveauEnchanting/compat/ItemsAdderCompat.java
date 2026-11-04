package us.to.midensthings.nouveauEnchanting.compat;

import dev.lone.itemsadder.api.CustomStack;
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

    /***
     * @return the custom item, or null if ItemsAdder has no item with that ID.
     */
    public ItemStack getItemStack(String namespacedItemID) {
        CustomStack customStack = CustomStack.getInstance(namespacedItemID);
        if (customStack != null) {
            return customStack.getItemStack();
        }
        return null;
    }


}
