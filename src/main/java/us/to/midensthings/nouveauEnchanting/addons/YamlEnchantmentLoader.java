package us.to.midensthings.nouveauEnchanting.addons;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry.EnchantmentCost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import us.to.midensthings.nouveauEnchanting.effects.EffectConfigParser;
import us.to.midensthings.nouveauEnchanting.effects.EnchantmentEffect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/***
 * Loads the custom enchants written in the .yml files in plugins/NouveauEnchanting/addons, so custom enchants can be
 * made without writing an addon. Every top-level section of a file is an enchant, with the same settings as
 * enchants.yml. See example-enchants.yml for the format.
 * <p>
 * These enchants only do what their effects say. Enchants that need code have to be made as an addon.
 */
public class YamlEnchantmentLoader {
    private static final Set<String> SETTINGS = Set.of("max-level", "description", "allowed-items", "primary-items",
            "conflicts-with", "tags", "anvil-cost", "weight", "min-cost", "max-cost", "active-slots", "effects");

    private final ComponentLogger logger;

    public YamlEnchantmentLoader(ComponentLogger logger) {
        this.logger = logger;
    }

    /***
     * Loads every .yml file in the folder and adds their enchants to the registry.
     * A file or enchant that isn't valid is skipped without affecting the others.
     */
    public void loadEnchantments(Path folder, CustomEnchantmentRegistry enchantmentRegistry) {
        List<Path> files;
        try (Stream<Path> paths = Files.list(folder)) {
            files = paths.filter(file -> {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                return Files.isRegularFile(file) && (name.endsWith(".yml") || name.endsWith(".yaml"));
            }).sorted().toList();
        } catch (IOException e) {
            logger.error("Could not read the addons folder", e);
            return;
        }

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(file.toFile());
            } catch (IOException | InvalidConfigurationException e) {
                logger.error("Could not read " + fileName + " in the addons folder, its enchants weren't added: " + e.getMessage());
                continue;
            }

            int loaded = 0;
            for (String name : config.getKeys(false)) {
                YamlEnchantment enchantment = readEnchantment(config, name, fileName);
                if (enchantment != null && enchantmentRegistry.register(enchantment, fileName)) {
                    loaded++;
                }
            }
            if (loaded > 0) {
                logger.info("Loaded " + loaded + " custom enchant(s) from " + fileName);
            }
        }
    }

    private YamlEnchantment readEnchantment(YamlConfiguration config, String name, String fileName) {
        String where = "enchant " + name + " in " + fileName;
        ConfigurationSection section = config.getConfigurationSection(name);
        if (section == null) {
            logger.warn("Skipping " + where + ", it has no settings");
            return null;
        }

        // Enchants without a namespace use the file's name, e.g. frost_blade in my_enchants.yml is my_enchants:frost_blade
        String fullName = name.contains(":") ? name : defaultNamespace(fileName) + ":" + name;
        NamespacedKey key = NamespacedKey.fromString(fullName.toLowerCase(Locale.ROOT));
        if (key == null) {
            logger.warn("Skipping " + where + ", enchant names can only use lowercase letters, numbers, _ and -");
            return null;
        }

        for (String setting : section.getKeys(false)) {
            if (!SETTINGS.contains(setting)) {
                logger.warn("Unknown setting " + setting + " for " + where + ", ignoring it");
            }
        }

        List<String> allowedItems = section.getStringList("allowed-items");
        if (allowedItems.isEmpty()) {
            logger.warn("Skipping " + where + ", it needs allowed-items, the items it can be put on");
            return null;
        }

        Component description = section.isString("description")
                ? MiniMessage.miniMessage().deserialize(section.getString("description", ""))
                : Component.text(defaultName(key.getKey()));

        List<EquipmentSlotGroup> activeSlots = new ArrayList<>();
        for (String slotName : section.getStringList("active-slots")) {
            EquipmentSlotGroup slot = EquipmentSlotGroup.getByName(slotName.toLowerCase(Locale.ROOT));
            if (slot == null) {
                logger.warn("Unknown active slot " + slotName + " for " + where + ", skipping it");
            } else {
                activeSlots.add(slot);
            }
        }
        if (activeSlots.isEmpty()) {
            activeSlots.add(EquipmentSlotGroup.ANY);
        }

        List<EnchantmentEffect> effects = List.of();
        if (section.isConfigurationSection("effects")) {
            // There's nothing to replace or add to, so the mode doesn't matter here
            effects = new EffectConfigParser(logger, fileName).parse(name, section.getConfigurationSection("effects")).effects();
        } else if (section.isSet("effects")) {
            logger.warn("effects for " + where + " isn't written correctly, see example-enchants.yml for an example");
        }

        return new YamlEnchantment(key, description,
                readInt(section, "max-level", 1, 1, 255, where),
                allowedItems,
                section.isSet("primary-items") ? section.getStringList("primary-items") : allowedItems,
                section.getStringList("conflicts-with"),
                section.getStringList("tags"),
                readInt(section, "anvil-cost", 1, 0, Integer.MAX_VALUE, where),
                readInt(section, "weight", 1, 1, 1024, where),
                EnchantmentCost.of(readInt(section, "min-cost.base", 1, Integer.MIN_VALUE, Integer.MAX_VALUE, where),
                        readInt(section, "min-cost.per-level", 10, Integer.MIN_VALUE, Integer.MAX_VALUE, where)),
                EnchantmentCost.of(readInt(section, "max-cost.base", 51, Integer.MIN_VALUE, Integer.MAX_VALUE, where),
                        readInt(section, "max-cost.per-level", 10, Integer.MIN_VALUE, Integer.MAX_VALUE, where)),
                activeSlots, effects);
    }

    private int readInt(ConfigurationSection section, String setting, int defaultValue, int min, int max, String where) {
        if (!section.isSet(setting)) {
            return defaultValue;
        }
        int value = section.getInt(setting);
        if (!section.isInt(setting) || value < min || value > max) {
            logger.warn(setting + " for " + where + " isn't a valid whole number, using " + defaultValue + " instead");
            return defaultValue;
        }
        return value;
    }

    // my-enchants.yml -> my-enchants, with anything a namespace can't contain replaced.
    // Dots are replaced too (my.enchants.yml -> my_enchants), the configs use them to separate settings
    private static String defaultNamespace(String fileName) {
        String name = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return name.replaceAll("[^a-z0-9_-]", "_");
    }

    // frost_blade -> Frost Blade
    private static String defaultName(String id) {
        StringBuilder name = new StringBuilder();
        for (String word : id.split("[_/.-]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
