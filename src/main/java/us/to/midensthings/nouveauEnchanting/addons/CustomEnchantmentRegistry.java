package us.to.midensthings.nouveauEnchanting.addons;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.NamespacedKey;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/***
 * The custom enchants added by NouveauEnchanting and its addons, filled in while the server is bootstrapping.
 */
public class CustomEnchantmentRegistry {
    private record Entry(CustomEnchantment enchantment, String source) {}

    private final ComponentLogger logger;
    // Enchant key -> enchant, in registration order
    private final Map<NamespacedKey, Entry> enchantments = new LinkedHashMap<>();

    public CustomEnchantmentRegistry(ComponentLogger logger) {
        this.logger = logger;
    }

    /***
     * Adds a custom enchant. Enchants with an invalid key or the same key as another custom enchant are skipped.
     * @param source what added the enchant, e.g. the addon's name, used in messages
     * @return true if the enchant was added.
     */
    public boolean register(CustomEnchantment enchantment, String source) {
        NamespacedKey key = enchantment.getKey();
        if (key == null) {
            logger.warn("Skipping custom enchant " + enchantment.getNamespace() + ":" + enchantment.getId() + " from " + source
                    + ", its namespace and ID can only use lowercase letters, numbers, ., _ and -");
            return false;
        }
        if (key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
            logger.warn("Skipping custom enchant " + key.asString() + " from " + source + ", custom enchants can't use the minecraft namespace");
            return false;
        }
        if (enchantments.containsKey(key)) {
            logger.warn("Skipping custom enchant " + key.asString() + " from " + source + ", "
                    + enchantments.get(key).source() + " already added an enchant with that key");
            return false;
        }
        enchantments.put(key, new Entry(enchantment, source));
        return true;
    }

    public CustomEnchantment get(NamespacedKey key) {
        Entry entry = enchantments.get(key);
        return entry == null ? null : entry.enchantment();
    }

    public boolean contains(NamespacedKey key) {
        return enchantments.containsKey(key);
    }

    /***
     * @return what added the enchant, or null if there's no custom enchant with that key.
     */
    public String getSource(NamespacedKey key) {
        Entry entry = enchantments.get(key);
        return entry == null ? null : entry.source();
    }

    public Collection<CustomEnchantment> getAll() {
        return Collections.unmodifiableCollection(enchantments.values().stream().map(Entry::enchantment).toList());
    }
}
