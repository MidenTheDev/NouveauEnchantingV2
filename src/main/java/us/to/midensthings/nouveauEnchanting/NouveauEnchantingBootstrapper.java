package us.to.midensthings.nouveauEnchanting;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PreFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.java.JavaPlugin;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;
import us.to.midensthings.nouveauEnchanting.enchanting.MaterialRegistry;
import us.to.midensthings.nouveauEnchanting.util.LegacyTagsMigrator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NouveauEnchantingBootstrapper implements PluginBootstrap {
    // Namespace for the tags this plugin creates
    private static final String TAG_NAMESPACE = "nouveauenchanting";

    MaterialRegistry materialRegistry;
    YamlConfiguration materialsConf;
    YamlConfiguration enchantsConf;
    Path dataDirectory;
    ComponentLogger logger;
    File materialsYml;
    File enchantsYml;

    // Tags to create, filled in from the configs and registered when the server loads tags
    private final Map<TagKey<ItemType>, List<TagEntry<ItemType>>> itemTags = new LinkedHashMap<>();
    private final Map<TagKey<Enchantment>, List<TagEntry<Enchantment>>> enchantmentTags = new LinkedHashMap<>();



    @Override
    public void bootstrap(BootstrapContext context) {
        logger = context.getLogger();
        dataDirectory = context.getDataDirectory();
        materialsYml = dataDirectory.resolve("materials.yml").toFile();
        enchantsYml = dataDirectory.resolve("enchants.yml").toFile();

        generateFile(materialsYml, "materials.yml");
        generateFile(enchantsYml, "enchants.yml");

        // Convert enchants.yml from the old tags.yml system if it's still there
        LegacyTagsMigrator.migrate(dataDirectory, enchantsYml, logger);

        materialsConf = YamlConfiguration.loadConfiguration(materialsYml);
        enchantsConf = YamlConfiguration.loadConfiguration(enchantsYml);


        // Fill in material registry
        materialRegistry = new MaterialRegistry();

        // Check material config for each material, check each enchantment available per material, and load them into the material registry
        materialsConf.getConfigurationSection("").getKeys(false).forEach(material -> {
            if (materialsConf.getConfigurationSection(material) == null) {
                logger.warn("Material " + material + " in materials.yml has no enchants listed, skipping");
                return;
            }
            List<String> enchantKeys = new ArrayList<>();
            materialsConf.getConfigurationSection(material).getKeys(false).forEach(enchantKey -> {

                // Check if it's the custom item info section
                // If it ISN'T, it's an enchant, add it to the list.
                if (!enchantKey.equals("custom-material")) {
                    enchantKeys.add(enchantKey);
                }

            });
            EnchantMaterial enchantMaterial;
            // Check if the material exists and if it doesn't, check the compat instead
            try {
                Material.valueOf(material.toUpperCase());
                // If it gets past this point, material exists. Register as vanilla EnchantMaterial
                // toUpperCase allows for lowercase item IDs
                enchantMaterial = new EnchantMaterial(Material.valueOf(material.toUpperCase()), enchantKeys);
                materialRegistry.addMaterial(material.toUpperCase(), enchantMaterial);
            } catch (IllegalArgumentException e) {
                // If it doesn't, there's no vanilla material. Check compats.
                // First, check if there's a custom-material section.
                if (materialsConf.getConfigurationSection(material+".custom-material") == null) {
                    // No custom material section, warn in console
                    logger.warn("No valid material or custom material found for material " + material);
                } else {
                    // Check the specified custom material source (currently only itemsadder support, maybe more in the future.
                    String customMaterialSource = materialsConf.getString(material+".custom-material.source");
                    if (customMaterialSource == null) {
                        logger.warn("No custom-material source set for material " + material);
                    } else if (customMaterialSource.equalsIgnoreCase("itemsadder")) {
                        // Get the namespace and material name to make retrieving the item via the API easier later
                        String namespace = materialsConf.getString(material+".custom-material.namespace");
                        enchantMaterial = new EnchantMaterial(namespace+":"+material,enchantKeys,true,customMaterialSource);
                        materialRegistry.addMaterial(material, enchantMaterial);
                    } else if (customMaterialSource.equalsIgnoreCase("craftengine")) {
                        // Get the namespace and material name to make retrieving the item via the API easier later
                        String namespace = materialsConf.getString(material+".custom-material.namespace");
                        enchantMaterial = new EnchantMaterial(namespace+":"+material,enchantKeys,true,customMaterialSource);
                        materialRegistry.addMaterial(material, enchantMaterial);
                    }

                    else {
                        // no support for whatever plugin is specified.
                        logger.warn("No compatibility module found for custom material source plugin " + customMaterialSource);
                    }
                }
            }


        });


        // Modify vanilla enchants according to enchants.yml. Every setting is optional, anything left out keeps its default value.
        enchantsConf.getConfigurationSection("").getKeys(false).forEach(enchant -> {

            NamespacedKey enchantKey = NamespacedKey.fromString(enchant);
            if (enchantKey == null) {
                logger.warn("Invalid enchant name " + enchant + " in enchants.yml, skipping");
                return;
            }

            // Read out configurable enchants.yml options
            final Optional<Integer> maxLevel = readInt(enchant, "max-level", 1, 255);
            final Optional<Integer> anvilCost = readInt(enchant, "anvil-cost", 0, Integer.MAX_VALUE);
            final Optional<Integer> weight = readInt(enchant, "weight", 1, 1024);
            final Optional<Integer> minCostBase = readInt(enchant, "min-cost.base", Integer.MIN_VALUE, Integer.MAX_VALUE);
            final Optional<Integer> minCostPerLevel = readInt(enchant, "min-cost.per-level", Integer.MIN_VALUE, Integer.MAX_VALUE);
            final Optional<Integer> maxCostBase = readInt(enchant, "max-cost.base", Integer.MIN_VALUE, Integer.MAX_VALUE);
            final Optional<Integer> maxCostPerLevel = readInt(enchant, "max-cost.per-level", Integer.MIN_VALUE, Integer.MAX_VALUE);

            final Optional<Component> description =
                    enchantsConf.isSet(enchant + ".description") ? Optional.of(MiniMessage.miniMessage().deserialize(enchantsConf.getString(enchant + ".description", ""))) : Optional.empty();

            final Optional<List<EquipmentSlotGroup>> activeSlots = readActiveSlots(enchant);

            // Item and enchant lists become tags made by this plugin, the same way vanilla uses the enchantable and exclusive_set tags
            final Optional<TagKey<ItemType>> supportedItemsTag =
                    createTag(itemTags, RegistryKey.ITEM, enchant, "allowed-items", "supported/" + enchantKey.getKey());
            final Optional<TagKey<ItemType>> primaryItemsTag =
                    createTag(itemTags, RegistryKey.ITEM, enchant, "primary-items", "primary/" + enchantKey.getKey());
            final Optional<TagKey<Enchantment>> conflictsTag =
                    createTag(enchantmentTags, RegistryKey.ENCHANTMENT, enchant, "conflicts-with", "exclusive/" + enchantKey.getKey());

            context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd()
                    .newHandler(event -> {
                        EnchantmentRegistryEntry.Builder builder = event.builder();

                        // set max level
                        maxLevel.ifPresent(builder::maxLevel);

                        // set anvil cost
                        anvilCost.ifPresent(builder::anvilCost);

                        // set description
                        description.ifPresent(builder::description);

                        // set weight
                        weight.ifPresent(builder::weight);

                        // set min and max cost, keeping the default for whichever half isn't set
                        if (minCostBase.isPresent() || minCostPerLevel.isPresent()) {
                            builder.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                    minCostBase.orElse(builder.minimumCost().baseCost()),
                                    minCostPerLevel.orElse(builder.minimumCost().additionalPerLevelCost())));
                        }
                        if (maxCostBase.isPresent() || maxCostPerLevel.isPresent()) {
                            builder.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                    maxCostBase.orElse(builder.maximumCost().baseCost()),
                                    maxCostPerLevel.orElse(builder.maximumCost().additionalPerLevelCost())));
                        }

                        // set active slots
                        activeSlots.ifPresent(slots -> builder.activeSlots(slots));

                        // set supported and primary items
                        supportedItemsTag.ifPresent(tag -> builder.supportedItems(event.getOrCreateTag(tag)));
                        primaryItemsTag.ifPresent(tag -> builder.primaryItems(event.getOrCreateTag(tag)));

                        // set enchantment conflicts
                        conflictsTag.ifPresent(tag -> builder.exclusiveWith(event.getOrCreateTag(tag)));
                    })

                    // Filter for this specific enchant
                    .filter(RegistryKey.ENCHANTMENT.typedKey(enchantKey))
            );

        });


        // Create the tags used by the enchants. This runs again whenever datapacks are reloaded.
        registerTags(context, RegistryKey.ITEM, itemTags);
        registerTags(context, RegistryKey.ENCHANTMENT, enchantmentTags);

        // TODO: Add Custom Enchantments to Registry

    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new NouveauEnchanting(materialRegistry,materialsConf,enchantsConf);
        //return new NouveauEnchanting();
    }

    private void generateFile(File file, String filename) {
        URL fileURL = getClass().getResource("/"+filename);
        if (fileURL == null) {
            logger.error("No default " + filename + " found in the plugin jar! Plugin will not function!");
            return;
        }

        try {
            // Check if data directory exists and if not, make directories
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            // Check if the yml exists and if not, extract the YML from the plugin and copy it to the proper path.
            if (!file.exists()) {
                try (InputStream in = fileURL.openStream()) {
                    Files.copy(in, file.toPath());
                }
            }
        } catch (IOException e) {
            logger.error("Could not create or extract file: " + file.getName()+"!", e);
        }

    }

    /***
     * Reads an optional whole number setting for an enchant.
     * @return the value, or empty if it isn't set or isn't valid (with a warning), so the default is used.
     */
    private Optional<Integer> readInt(String enchant, String field, int min, int max) {
        String path = enchant + "." + field;
        if (!enchantsConf.isSet(path)) {
            return Optional.empty();
        }
        int value = enchantsConf.getInt(path);
        if (!enchantsConf.isInt(path) || value < min || value > max) {
            String range = "";
            if (max == Integer.MAX_VALUE && min != Integer.MIN_VALUE) {
                range = " of at least " + min;
            } else if (max != Integer.MAX_VALUE) {
                range = " from " + min + " to " + max;
            }
            logger.warn(field + " for enchant " + enchant + " in enchants.yml must be a whole number" + range + ", using the default instead");
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /***
     * Reads the optional list of equipment slots the enchant works in.
     * @return the slots, or empty if the setting isn't set so the default is used.
     */
    private Optional<List<EquipmentSlotGroup>> readActiveSlots(String enchant) {
        String path = enchant + ".active-slots";
        if (!enchantsConf.isSet(path)) {
            return Optional.empty();
        }
        List<EquipmentSlotGroup> slots = new ArrayList<>();
        for (String name : enchantsConf.getStringList(path)) {
            EquipmentSlotGroup slot = EquipmentSlotGroup.getByName(name.toLowerCase());
            if (slot == null) {
                logger.warn("Unknown active slot " + name + " for enchant " + enchant + " in enchants.yml, skipping");
                continue;
            }
            slots.add(slot);
        }
        return Optional.of(slots);
    }

    /***
     * Turns a list setting for an enchant into a tag made by this plugin, which gets registered when the server loads tags.
     * @return the tag, or empty if the setting isn't set so the default is used.
     */
    private <T> Optional<TagKey<T>> createTag(Map<TagKey<T>, List<TagEntry<T>>> tags, RegistryKey<T> registryKey, String enchant, String field, String tagName) {
        String path = enchant + "." + field;
        if (!enchantsConf.isSet(path)) {
            return Optional.empty();
        }
        List<TagEntry<T>> entries = parseEntries(registryKey, enchantsConf.getStringList(path), field);
        if (entries.isEmpty()) {
            logger.warn(field + " for enchant " + enchant + " in enchants.yml is empty");
        }
        TagKey<T> tagKey = TagKey.create(registryKey, new NamespacedKey(TAG_NAMESPACE, tagName));
        tags.put(tagKey, entries);
        return Optional.of(tagKey);
    }

    /***
     * Turns a list of entries and tags from the configs into tag entries.
     * Tags start with # (e.g. "#swords" or "#minecraft:enchantable/armor"), anything else is a single entry (e.g. "mace").
     * Both default to the minecraft namespace.
     */
    private <T> List<TagEntry<T>> parseEntries(RegistryKey<T> registryKey, List<String> rawInputs, String field) {
        List<TagEntry<T>> entries = new ArrayList<>();

        for (String input : rawInputs) {
            if (input == null || input.isBlank()) continue;

            String inputString = input.toLowerCase();
            boolean isTag = inputString.startsWith("#");
            NamespacedKey key = NamespacedKey.fromString(isTag ? inputString.substring(1) : inputString);
            if (key == null) {
                logger.warn("Invalid entry " + input + " in " + field + " in enchants.yml, skipping");
                continue;
            }

            // Entries aren't required, so a missing entry or tag can't stop the server from loading tags
            if (isTag) {
                entries.add(TagEntry.tagEntry(TagKey.create(registryKey, key), false));
            } else if (registryKey == RegistryKey.ITEM && key.getNamespace().equals(NamespacedKey.MINECRAFT) && Material.getMaterial(key.getKey().toUpperCase()) == null) {
                // Only vanilla items can be checked here
                logger.warn("Unknown item " + input + " in " + field + " in enchants.yml, skipping");
            } else {
                entries.add(TagEntry.valueEntry(TypedKey.create(registryKey, key), false));
            }
        }
        return entries;
    }

    /***
     * Registers the tags made from the configs. Runs whenever the server loads tags, including datapack reloads.
     */
    private <T> void registerTags(BootstrapContext context, RegistryKey<T> registryKey, Map<TagKey<T>, List<TagEntry<T>>> tags) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.preFlatten(registryKey), event -> {
            PreFlattenTagRegistrar<T> registrar = event.registrar();
            tags.forEach((tagKey, entries) -> {
                for (TagEntry<T> entry : entries) {
                    if (entry.isTag() && !registrar.hasTag(TagKey.create(registryKey, entry.key()))) {
                        logger.warn("Unknown tag #" + entry.key().asString() + " used by " + tagKey.key().asString());
                    }
                }
                registrar.setTag(tagKey, entries);
            });
        });
    }
}
