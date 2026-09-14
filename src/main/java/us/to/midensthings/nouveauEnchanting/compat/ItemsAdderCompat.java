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

    public boolean isValidRecipeCustom() {



        return true;
    }


}
