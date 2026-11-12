package us.to.midensthings.nouveauEnchanting.addons;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry.EnchantmentCost;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import us.to.midensthings.nouveauEnchanting.effects.EnchantmentEffect;

import java.util.List;

/***
 * A custom enchant written in a .yml file in the addons folder. See YamlEnchantmentLoader.
 */
public record YamlEnchantment(NamespacedKey key, Component description, int maxLevel, List<String> allowedItems,
                              List<String> primaryItems, List<String> conflicts, List<String> tags, int anvilCost, int weight,
                              EnchantmentCost minimumCost, EnchantmentCost maximumCost, List<EquipmentSlotGroup> activeSlots,
                              List<EnchantmentEffect> effects) implements CustomEnchantment {

    @Override
    public String getNamespace() {
        return key.getNamespace();
    }

    @Override
    public String getId() {
        return key.getKey();
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public Component getDescription() {
        return description;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public List<String> getAllowedItems() {
        return allowedItems;
    }

    @Override
    public List<String> getPrimaryItems() {
        return primaryItems;
    }

    @Override
    public List<String> getConflicts() {
        return conflicts;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public int getAnvilCost() {
        return anvilCost;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public EnchantmentCost getMinimumCost() {
        return minimumCost;
    }

    @Override
    public EnchantmentCost getMaximumCost() {
        return maximumCost;
    }

    @Override
    public List<EquipmentSlotGroup> getActiveSlots() {
        return activeSlots;
    }

    @Override
    public List<EnchantmentEffect> getEffects() {
        return effects;
    }
}
