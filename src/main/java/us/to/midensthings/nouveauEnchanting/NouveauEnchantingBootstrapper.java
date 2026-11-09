package us.to.midensthings.nouveauEnchanting;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PreFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.java.JavaPlugin;
import us.to.midensthings.nouveauEnchanting.addons.AddonLoader;
import us.to.midensthings.nouveauEnchanting.addons.CustomEnchantment;
import us.to.midensthings.nouveauEnchanting.addons.CustomEnchantmentRegistry;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;
import us.to.midensthings.nouveauEnchanting.enchanting.MaterialRegistry;
import us.to.midensthings.nouveauEnchanting.util.LegacyTagsMigrator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    // Custom enchants from addons
    CustomEnchantmentRegistry customEnchantments;
    AddonLoader addonLoader;
    // Settings for each custom enchant, with the defaults filled in for anything enchants.yml doesn't set
    private final Map<NamespacedKey, EnchantSettings> customEnchantSettings = new LinkedHashMap<>();

    // Equipment slot group names used in enchants.yml, the reverse of EquipmentSlotGroup.getByName
    private static final List<String> SLOT_NAMES = List.of("any", "mainhand", "offhand", "hand", "head", "chest", "legs", "feet", "armor", "body", "saddle");

    /***
     * The settings of an enchant from enchants.yml. Settings that aren't set are empty and keep the enchant's default.
     */
    private record EnchantSettings(Optional<Integer> maxLevel, Optional<Integer> anvilCost, Optional<Integer> weight,
                                   Optional<Integer> minCostBase, Optional<Integer> minCostPerLevel,
                                   Optional<Integer> maxCostBase, Optional<Integer> maxCostPerLevel,
                                   Optional<Component> description, Optional<List<EquipmentSlotGroup>> activeSlots,
                                   Optional<TagKey<ItemType>> supportedItems, Optional<TagKey<ItemType>> primaryItems,
                                   Optional<TagKey<Enchantment>> conflicts) {
    }

    /***
     * Gets a tag while the registry is loading, e.g. RegistryEntryAddEvent::getOrCreateTag or RegistryComposeEvent::getOrCreateTag.
     */
    private interface TagLookup {
        <V extends Keyed> Tag<V> getOrCreateTag(TagKey<V> tagKey);
    }

    // Tags to create, filled in from the configs and registered when the server loads tags
    private final Map<TagKey<ItemType>, List<TagEntry<ItemType>>> itemTags = new LinkedHashMap<>();
    private final Map<TagKey<Enchantment>, List<TagEntry<Enchantment>>> enchantmentTags = new LinkedHashMap<>();
    // Enchants to add to existing enchantment tags, e.g. in_enchanting_table
    private final Map<TagKey<Enchantment>, List<TagEntry<Enchantment>>> enchantmentTagAdditions = new LinkedHashMap<>();
    // The tags setting of custom enchants that set it in enchants.yml
    private final Map<NamespacedKey, List<String>> customEnchantTags = new LinkedHashMap<>();



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

        // Load the addons' custom enchants first so that materials.yml and enchants.yml can use them
        customEnchantments = new CustomEnchantmentRegistry(logger);
        addonLoader = new AddonLoader(logger);
        addonLoader.loadAddons(dataDirectory, customEnchantments);
        addMissingCustomEnchants();

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

            EnchantSettings settings = readSettings(enchant, enchantKey);

            // Custom enchants get their settings applied when they're registered instead
            if (customEnchantments.contains(enchantKey)) {
                customEnchantSettings.put(enchantKey, settings);
                if (enchantsConf.isSet(enchant + ".tags")) {
                    customEnchantTags.put(enchantKey, enchantsConf.getStringList(enchant + ".tags"));
                }
                return;
            }

            if (enchantsConf.isSet(enchant + ".tags")) {
                addToEnchantmentTags(enchantKey, enchantsConf.getStringList(enchant + ".tags"), enchant);
            }

            context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd()
                    .newHandler(event -> applySettings(event.builder(), settings, event::getOrCreateTag))

                    // Filter for this specific enchant
                    .filter(RegistryKey.ENCHANTMENT.typedKey(enchantKey))
            );

        });

        // Fill in the defaults of every custom enchant for anything enchants.yml doesn't set
        for (CustomEnchantment customEnchantment : customEnchantments.getAll()) {
            NamespacedKey key = customEnchantment.getKey();
            EnchantSettings settings = customEnchantSettings.getOrDefault(key, emptySettings());
            customEnchantSettings.put(key, withDefaults(settings, customEnchantment));
            // New enchants aren't in any tags, so without these they can't be found in enchanting tables, loot, trades, etc.
            addToEnchantmentTags(key, customEnchantTags.getOrDefault(key, customEnchantment.getTags()), key.asString());
        }

        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event ->
                 customEnchantSettings.forEach((key, settings) ->
                          event.registry().register(RegistryKey.ENCHANTMENT.typedKey(key),
                                 builder -> applySettings(builder, settings, event::getOrCreateTag)))));


        // Create the tags used by the enchants. This runs again whenever datapacks are reloaded.
        registerTags(context, RegistryKey.ITEM, itemTags);
        registerTags(context, RegistryKey.ENCHANTMENT, enchantmentTags);
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ENCHANTMENT), event ->
                enchantmentTagAdditions.forEach((tagKey, entries) -> event.registrar().addToTag(tagKey, entries)));


    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new NouveauEnchanting(materialRegistry,materialsConf,enchantsConf,customEnchantments,addonLoader);
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
        List<String> rawEntries = enchantsConf.getStringList(path);
        if (rawEntries.isEmpty()) {
            logger.warn(field + " for enchant " + enchant + " in enchants.yml is empty");
        }
        return Optional.of(createTag(tags, registryKey, rawEntries, field, tagName));
    }

    /***
     * Turns a list of entries into a tag made by this plugin, which gets registered when the server loads tags.
     */
    private <T> TagKey<T> createTag(Map<TagKey<T>, List<TagEntry<T>>> tags, RegistryKey<T> registryKey, List<String> rawEntries, String field, String tagName) {
        TagKey<T> tagKey = TagKey.create(registryKey, new NamespacedKey(TAG_NAMESPACE, tagName));
        tags.put(tagKey, parseEntries(registryKey, rawEntries, field));
        return tagKey;
    }

    /***
     * @return the name of a tag this plugin makes for an enchant, e.g. supported/sharpness or supported/myaddon/my_enchant.
     */
    private static String tagName(String prefix, NamespacedKey enchantKey) {
        if (enchantKey.getNamespace().equals(NamespacedKey.MINECRAFT)) {
            return prefix + "/" + enchantKey.getKey();
        }
        return prefix + "/" + enchantKey.getNamespace() + "/" + enchantKey.getKey();
    }

    /***
     * Reads every setting of an enchant from enchants.yml.
     */
    private EnchantSettings readSettings(String enchant, NamespacedKey enchantKey) {
        Optional<Component> description =
                enchantsConf.isSet(enchant + ".description") ? Optional.of(MiniMessage.miniMessage().deserialize(enchantsConf.getString(enchant + ".description", ""))) : Optional.empty();

        return new EnchantSettings(
                readInt(enchant, "max-level", 1, 255),
                readInt(enchant, "anvil-cost", 0, Integer.MAX_VALUE),
                readInt(enchant, "weight", 1, 1024),
                readInt(enchant, "min-cost.base", Integer.MIN_VALUE, Integer.MAX_VALUE),
                readInt(enchant, "min-cost.per-level", Integer.MIN_VALUE, Integer.MAX_VALUE),
                readInt(enchant, "max-cost.base", Integer.MIN_VALUE, Integer.MAX_VALUE),
                readInt(enchant, "max-cost.per-level", Integer.MIN_VALUE, Integer.MAX_VALUE),
                description,
                readActiveSlots(enchant),
                // Item and enchant lists become tags made by this plugin, the same way vanilla uses the enchantable and exclusive_set tags
                createTag(itemTags, RegistryKey.ITEM, enchant, "allowed-items", tagName("supported", enchantKey)),
                createTag(itemTags, RegistryKey.ITEM, enchant, "primary-items", tagName("primary", enchantKey)),
                createTag(enchantmentTags, RegistryKey.ENCHANTMENT, enchant, "conflicts-with", tagName("exclusive", enchantKey)));
    }

    /***
     * Adds an enchant to enchantment tags, e.g. in_enchanting_table so it can show up in enchanting tables.
     * Tags can be written with or without # and default to the minecraft namespace.
     */
    private void addToEnchantmentTags(NamespacedKey enchantKey, List<String> tags, String enchant) {
        for (String tag : tags) {
            String tagName = tag.toLowerCase();
            NamespacedKey tagKey = NamespacedKey.fromString(tagName.startsWith("#") ? tagName.substring(1) : tagName);
            if (tagKey == null) {
                logger.warn("Invalid tag " + tag + " in tags for enchant " + enchant + " in enchants.yml, skipping");
                continue;
            }
            enchantmentTagAdditions.computeIfAbsent(TagKey.create(RegistryKey.ENCHANTMENT, tagKey), k -> new ArrayList<>())
                    .add(TagEntry.valueEntry(TypedKey.create(RegistryKey.ENCHANTMENT, enchantKey), false));
        }
    }

    private static EnchantSettings emptySettings() {
        return new EnchantSettings(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /***
     * Fills in every setting enchants.yml doesn't set with the custom enchant's default, so a new enchant can be built from it.
     */
    private EnchantSettings withDefaults(EnchantSettings settings, CustomEnchantment enchant) {
        NamespacedKey key = enchant.getKey();
        String label = "the default " + key.asString() + " settings";
        return new EnchantSettings(
                Optional.of(settings.maxLevel().orElse(enchant.getMaxLevel())),
                Optional.of(settings.anvilCost().orElse(enchant.getAnvilCost())),
                Optional.of(settings.weight().orElse(enchant.getWeight())),
                Optional.of(settings.minCostBase().orElse(enchant.getMinimumCost().baseCost())),
                Optional.of(settings.minCostPerLevel().orElse(enchant.getMinimumCost().additionalPerLevelCost())),
                Optional.of(settings.maxCostBase().orElse(enchant.getMaximumCost().baseCost())),
                Optional.of(settings.maxCostPerLevel().orElse(enchant.getMaximumCost().additionalPerLevelCost())),
                Optional.of(settings.description().orElse(enchant.getDescription())),
                Optional.of(settings.activeSlots().orElse(enchant.getActiveSlots())),
                Optional.of(settings.supportedItems().orElseGet(() ->
                        createTag(itemTags, RegistryKey.ITEM, enchant.getAllowedItems(), label, tagName("supported", key)))),
                Optional.of(settings.primaryItems().orElseGet(() ->
                        createTag(itemTags, RegistryKey.ITEM, enchant.getPrimaryItems(), label, tagName("primary", key)))),
                Optional.of(settings.conflicts().orElseGet(() ->
                        createTag(enchantmentTags, RegistryKey.ENCHANTMENT, enchant.getConflicts(), label, tagName("exclusive", key)))));
    }

    /***
     * Applies the settings that are set to an enchant's builder, leaving everything else as it is.
     */
    private static void applySettings(EnchantmentRegistryEntry.Builder builder, EnchantSettings settings, TagLookup tags) {
        // set max level
        settings.maxLevel().ifPresent(builder::maxLevel);

        // set anvil cost
        settings.anvilCost().ifPresent(builder::anvilCost);

        // set description
        settings.description().ifPresent(builder::description);

        // set weight
        settings.weight().ifPresent(builder::weight);

        // set min and max cost, keeping the default for whichever half isn't set
        if (settings.minCostBase().isPresent() || settings.minCostPerLevel().isPresent()) {
            builder.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                    settings.minCostBase().orElseGet(() -> builder.minimumCost().baseCost()),
                    settings.minCostPerLevel().orElseGet(() -> builder.minimumCost().additionalPerLevelCost())));
        }
        if (settings.maxCostBase().isPresent() || settings.maxCostPerLevel().isPresent()) {
            builder.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                    settings.maxCostBase().orElseGet(() -> builder.maximumCost().baseCost()),
                    settings.maxCostPerLevel().orElseGet(() -> builder.maximumCost().additionalPerLevelCost())));
        }

        // set active slots
        settings.activeSlots().ifPresent(slots -> builder.activeSlots(slots));

        // set supported and primary items
        settings.supportedItems().ifPresent(tag -> builder.supportedItems(tags.getOrCreateTag(tag)));
        settings.primaryItems().ifPresent(tag -> builder.primaryItems(tags.getOrCreateTag(tag)));

        // set enchantment conflicts
        settings.conflicts().ifPresent(tag -> builder.exclusiveWith(tags.getOrCreateTag(tag)));
    }

    /***
     * Adds the default settings of custom enchants that aren't in enchants.yml yet to the end of it, so server owners can
     * see and edit them. Appends to the file instead of saving it through the config API, which would reformat it.
     */
    private void addMissingCustomEnchants() {
        YamlConfiguration current = YamlConfiguration.loadConfiguration(enchantsYml);
        Set<NamespacedKey> configured = new HashSet<>();
        for (String enchant : current.getKeys(false)) {
            NamespacedKey key = NamespacedKey.fromString(enchant);
            if (key != null) {
                configured.add(key);
            }
        }

        StringBuilder text = new StringBuilder();
        for (CustomEnchantment enchant : customEnchantments.getAll()) {
            NamespacedKey key = enchant.getKey();
            if (configured.contains(key)) {
                continue;
            }
            String source = customEnchantments.getSource(key);
            text.append("\n# Custom enchant added by ").append(source).append("\n");
            text.append(key.asString()).append(":\n");
            text.append("  max-level: ").append(enchant.getMaxLevel()).append("\n");
            text.append("  description: ").append(quote(MiniMessage.miniMessage().serialize(enchant.getDescription()))).append("\n");
            appendList(text, "allowed-items", enchant.getAllowedItems());
            appendList(text, "primary-items", enchant.getPrimaryItems());
            appendList(text, "conflicts-with", enchant.getConflicts());
            appendList(text, "tags", enchant.getTags());
            text.append("  anvil-cost: ").append(enchant.getAnvilCost()).append("\n");
            text.append("  weight: ").append(enchant.getWeight()).append("\n");
            text.append("  min-cost:\n");
            text.append("    base: ").append(enchant.getMinimumCost().baseCost()).append("\n");
            text.append("    per-level: ").append(enchant.getMinimumCost().additionalPerLevelCost()).append("\n");
            text.append("  max-cost:\n");
            text.append("    base: ").append(enchant.getMaximumCost().baseCost()).append("\n");
            text.append("    per-level: ").append(enchant.getMaximumCost().additionalPerLevelCost()).append("\n");
            List<String> slots = new ArrayList<>();
            for (EquipmentSlotGroup slot : enchant.getActiveSlots()) {
                SLOT_NAMES.stream().filter(name -> EquipmentSlotGroup.getByName(name) == slot).findFirst().ifPresent(slots::add);
            }
            appendList(text, "active-slots", slots);
            logger.info("Added the default settings of custom enchant " + key.asString() + " from " + source + " to enchants.yml");
        }

        if (text.isEmpty()) {
            return;
        }
        try {
            Files.writeString(enchantsYml.toPath(), text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Could not add the default custom enchant settings to enchants.yml", e);
        }
    }

    private static void appendList(StringBuilder text, String field, List<String> entries) {
        if (entries.isEmpty()) {
            text.append("  ").append(field).append(": []\n");
            return;
        }
        text.append("  ").append(field).append(":\n");
        for (String entry : entries) {
            text.append("    - ").append(quote(entry)).append("\n");
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
