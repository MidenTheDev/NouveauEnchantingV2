package us.to.midensthings.nouveauEnchanting.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/***
 * Reads and changes the enchants on items, treating the enchants stored in enchanted books like normal enchants.
 */
public final class ItemEnchantments {

    private ItemEnchantments() {
    }

    /***
     * @return the item's enchants, or the enchants stored in it for enchanted books.
     */
    public static Map<Enchantment, Integer> get(ItemStack item) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        if (item == null || item.isEmpty()) {
            return enchants;
        }
        enchants.putAll(item.getEnchantments());
        if (item.getItemMeta() instanceof EnchantmentStorageMeta storage) {
            enchants.putAll(storage.getStoredEnchants());
        }
        return enchants;
    }

    /***
     * @return a copy of the item with the enchants added at the given levels, the way the enchanting table adds them:
     * books become enchanted books, and enchanted books store the enchants.
     */
    public static ItemStack with(ItemStack item, Map<Enchantment, Integer> enchants) {
        ItemStack result = item.getType() == Material.BOOK ? item.withType(Material.ENCHANTED_BOOK) : item.clone();
        if (result.getItemMeta() instanceof EnchantmentStorageMeta storage) {
            enchants.forEach((enchantment, level) -> storage.addStoredEnchant(enchantment, level, true));
            result.setItemMeta(storage);
        } else {
            result.addUnsafeEnchantments(enchants);
        }
        return result;
    }

    /***
     * @return a copy of the item without the enchants. Enchanted books with no stored enchants left become books.
     */
    public static ItemStack without(ItemStack item, Collection<Enchantment> enchants) {
        ItemStack result = item.clone();
        enchants.forEach(result::removeEnchantment);
        if (result.getItemMeta() instanceof EnchantmentStorageMeta storage) {
            enchants.forEach(storage::removeStoredEnchant);
            result.setItemMeta(storage);
            if (result.getType() == Material.ENCHANTED_BOOK && !storage.hasStoredEnchants()) {
                result = result.withType(Material.BOOK);
            }
        }
        return result;
    }
}
