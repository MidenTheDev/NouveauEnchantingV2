package us.to.midensthings.nouveauEnchanting;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;
import us.to.midensthings.nouveauEnchanting.enchanting.MaterialRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class NouveauEnchantingBootstrapper implements PluginBootstrap {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(NouveauEnchantingBootstrapper.class);
    MaterialRegistry materialRegistry;
    YamlConfiguration materialsConf;
    YamlConfiguration enchantsConf;
    YamlConfiguration tagsConf;
    Path dataDirectory;
    ComponentLogger logger;
    File materialsYml;
    File enchantsYml;
    File tagsYml;



    @Override
    public void bootstrap(BootstrapContext context) {
        logger = context.getLogger();
        dataDirectory = context.getDataDirectory();
        materialsYml = dataDirectory.resolve("materials.yml").toFile();
        enchantsYml = dataDirectory.resolve("enchants.yml").toFile();
        tagsYml = dataDirectory.resolve("tags.yml").toFile();

        generateFile(materialsYml, "materials.yml");
        generateFile(enchantsYml, "enchants.yml");
        generateFile(tagsYml, "tags.yml");

        materialsConf = YamlConfiguration.loadConfiguration(materialsYml);
        enchantsConf = YamlConfiguration.loadConfiguration(enchantsYml);
        tagsConf = YamlConfiguration.loadConfiguration(tagsYml);


        // Fill in material registry
        materialRegistry = new MaterialRegistry();

        // Check material config for each material, check each enchantment available per material, and load them into the material registry
        materialsConf.getConfigurationSection("").getKeys(false).forEach(material -> {
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
                Material.valueOf(material);
                // If it gets past this point, material exists. Register as vanilla EnchantMaterial
                enchantMaterial = new EnchantMaterial(Material.valueOf(material), enchantKeys);
                materialRegistry.addMaterial(material, enchantMaterial);
            } catch (IllegalArgumentException e) {
                // If it doesn't, there's no vanilla amterial. Check compats.
                // First, check if there's a custom-material section.
                if (materialsConf.getConfigurationSection(material+".custom-material") == null) {
                    // No custom material section, warn in console
                    logger.warn("No valid material or custom material found for material " + material);
                } else {
                    // Check the specified custom material source (currently only itemsadder support, maybe more in the future.
                    String customMaterialSource = materialsConf.getString(material+".custom-material.source");
                    if (customMaterialSource.equalsIgnoreCase("itemsadder")) {
                        // Get the namespace and material name to make retrieving the item via the API easier later
                        String namespace = materialsConf.getString(material+".custom-material.namespace");
                        enchantMaterial = new EnchantMaterial(namespace+":"+material,enchantKeys,true,customMaterialSource);
                        materialRegistry.addMaterial(material, enchantMaterial);
                    } else {
                        // no support for whatever plugin is specified.
                        logger.warn("No compatibility module found for custom material source plugin " + customMaterialSource);
                    }
                }
            }


        });


        // Modify vanilla enchants according to enchants.yml
        enchantsConf.getConfigurationSection("").getKeys(false).forEach(enchant -> {

            RegistryKeySet<ItemType> allowedItems = createItemKeySet(tagsConf,enchantsConf.getStringList(enchant+".allowed-items"));

            if (allowedItems.isEmpty()) {
                logger.warn("Could not generate valid item list for enchant " + enchant);
            }

            context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd()
                    // Set Max Level
                    .newHandler(event -> event.builder().maxLevel(enchantsConf.getInt(enchant+".max-level"))
                            .supportedItems(allowedItems)

                    )


                    // Filter for this specific enchant
                    .filter(RegistryKey.ENCHANTMENT.typedKey(Key.key("minecraft:"+enchant)))
            );

        });


        // TODO: Add Custom Enchantments to Registry

    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new NouveauEnchanting(materialRegistry,materialsConf,enchantsConf,tagsConf);
        //return new NouveauEnchanting();
    }

    private void generateFile(File file, String filename) {


        //Materials YML first
        URL fileURL = getClass().getResource("/"+filename);
        if (fileURL == null) {
            logger.error("No materials file found! Plugin will not function!");
        }

        try {
            // Check if data directory exists and if not, make directories
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            // Check if the yml exists and if not, extract the YML from the plugin and copy it to the proper path.
            if (!file.exists()) {
                try (InputStream in = fileURL.openStream()) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                    } else {

                    }
                }
            }
        } catch (IOException e) {
            logger.error("Could not create or extract file: " + file.getName()+"!", e);
        }

    }

    public  RegistryKeySet<ItemType> createItemKeySet(YamlConfiguration tagsConf, List<String> rawInputs) {
        List<TypedKey<ItemType>> resolvedKeys = new ArrayList<>();

        for (String input : rawInputs) {
            if (input == null || input.isBlank()) continue;

            String inputString = input.toLowerCase();

            // Handle Tags (e.g., "#swords" or "#minecraft:swords")
            if (inputString.startsWith("#")) {
                inputString = inputString.substring(1);

                // Check if the tag exists

                if (tagsConf.getStringList(inputString) == null) {
                    continue;
                }

                // Get all the itemtypes associated with the tag and add it to our key list.
                tagsConf.getStringList(inputString).forEach(item -> {
                    NamespacedKey itemKey = parseNamespacedKey(item);
                    resolvedKeys.add(TypedKey.create(RegistryKey.ITEM, itemKey));
                });

            }
            // Handle Direct Item Names (e.g., "diamond_sword")
            else {
                NamespacedKey itemKey = parseNamespacedKey(inputString);
                resolvedKeys.add(TypedKey.create(RegistryKey.ITEM, itemKey));
            }
        }
        // Return registry key set from the resolved keys item list
        return RegistrySet.keySet(RegistryKey.ITEM, resolvedKeys);
    }

    private  NamespacedKey parseNamespacedKey(String input) {
        if (input.contains(":")) {
            return NamespacedKey.fromString(input);
        }
        // Default to "minecraft" namespace if omitted
        return NamespacedKey.minecraft(input);
    }
}
