package us.to.midensthings.nouveauEnchanting.addons;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry.EnchantmentCost;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/***
 * A custom enchantment added by NouveauEnchanting or an addon.
 * <p>
 * Everything returned here is only the enchant's default. Server owners can change every setting in enchants.yml under
 * the enchant's key (e.g. nouveauenchanting:ice_aspect), the same way as vanilla enchants. When the enchant is first
 * loaded, its defaults are added to enchants.yml so they're easy to find and edit.
 * <p>
 * Because of that, the enchant's behaviour should read its settings from {@link #getEnchantment()} (e.g. its max level)
 * instead of the methods here. If the enchant implements {@link org.bukkit.event.Listener}, NouveauEnchanting registers
 * it automatically when it enables.
 */
public interface CustomEnchantment {

    /***
     * @return the namespace of the enchant's key, e.g. the addon's name. Lowercase letters, numbers, ., _ and - only.
     */
    String getNamespace();

    /***
     * @return the enchant's ID within its namespace. Lowercase letters, numbers, ., _, - and / only.
     */
    String getId();

    /***
     * @return the enchant's full key, e.g. nouveauenchanting:ice_aspect, or null if the namespace or ID isn't valid.
     */
    default NamespacedKey getKey() {
        return NamespacedKey.fromString(getNamespace() + ":" + getId());
    }

    /***
     * @return the name shown on items. The level is added after it automatically.
     */
    Component getDescription();

    /***
     * @return the highest level the enchant can reach, from 1 to 255.
     */
    int getMaxLevel();

    /***
     * @return the items the enchant can be put on, in the same format as allowed-items in enchants.yml
     * (items like "mace", and item tags starting with # like "#swords").
     */
    List<String> getAllowedItems();

    /***
     * @return the items the enchant can show up on from enchanting tables and random enchanting, in the same format as
     * {@link #getAllowedItems()}. Defaults to the allowed items.
     */
    default List<String> getPrimaryItems() {
        return getAllowedItems();
    }

    /***
     * @return the enchants this one can't be on the same item with, e.g. "fire_aspect" or "#exclusive_set/damage".
     */
    default List<String> getConflicts() {
        return List.of();
    }

    /***
     * @return the enchantment tags the enchant is added to. These control where the enchant can be found, e.g.
     * in_enchanting_table (enchanting tables), on_random_loot (loot chests and fishing), tradeable (librarian villagers)
     * and treasure. Defaults to no tags, so the enchant can only be added by this plugin's enchanting gui, anvils and commands.
     * A normal vanilla enchant like Sharpness is in non_treasure, in_enchanting_table, on_random_loot, tradeable,
     * on_mob_spawn_equipment and on_traded_equipment.
     */
    default List<String> getTags() {
        return List.of();
    }

    /***
     * @return how expensive the enchant is to combine in an anvil.
     */
    default int getAnvilCost() {
        return 1;
    }

    /***
     * @return how likely the enchant is to be picked in random enchanting, from 1 (very rare) to 1024.
     */
    default int getWeight() {
        return 1;
    }

    /***
     * @return the lowest enchanting level each level of the enchant can be picked at in random enchanting.
     */
    default EnchantmentCost getMinimumCost() {
        return EnchantmentCost.of(1, 10);
    }

    /***
     * @return the highest enchanting level each level of the enchant can be picked at in random enchanting.
     */
    default EnchantmentCost getMaximumCost() {
        return EnchantmentCost.of(51, 10);
    }

    /***
     * @return the equipment slots the item has to be in for the enchant to have an effect.
     */
    default List<EquipmentSlotGroup> getActiveSlots() {
        return List.of(EquipmentSlotGroup.ANY);
    }

    /***
     * @return the registered enchantment, with the settings from enchants.yml applied, or null if it isn't registered.
     */
    default Enchantment getEnchantment() {
        NamespacedKey key = getKey();
        return key == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
    }

    /***
     * @return the level of this enchant on the item, or 0 if the item doesn't have it.
     */
    default int getLevel(ItemStack item) {
        Enchantment enchantment = getEnchantment();
        if (item == null || item.isEmpty() || enchantment == null) {
            return 0;
        }
        return item.getEnchantmentLevel(enchantment);
    }
}
