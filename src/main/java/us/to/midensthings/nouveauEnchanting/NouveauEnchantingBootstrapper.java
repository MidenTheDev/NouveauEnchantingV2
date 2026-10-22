package us.to.midensthings.nouveauEnchanting;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import java.util.List;

public class NouveauEnchantingBootstrapper implements PluginBootstrap {
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


        // Modify vanilla enchants according to enchants.yml
        enchantsConf.getConfigurationSection("").getKeys(false).forEach(enchant -> {

            NamespacedKey enchantKey = NamespacedKey.fromString(enchant);
            if (enchantKey == null) {
                logger.warn("Invalid enchant name " + enchant + " in enchants.yml, skipping");
                return;
            }

            int maxLevel = enchantsConf.getInt(enchant+".max-level");
            if (maxLevel < 1) {
                logger.warn("Enchant " + enchant + " in enchants.yml needs a max-level of at least 1, skipping");
                return;
            }

            RegistryKeySet<ItemType> allowedItems = createItemKeySet(tagsConf,enchantsConf.getStringList(enchant+".allowed-items"));

            if (allowedItems.isEmpty()) {
                logger.warn("Could not generate valid item list for enchant " + enchant);
            }

            context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd()
                    .newHandler(event -> event.builder()
                            // Set Max Level
                            .maxLevel(maxLevel)
                            // Set supported items
                            .supportedItems(allowedItems)

                            // TODO Set enchantment conflicts
                            // TODO Set Primary/Secondary enchants
                            // TODO Set Anvil Cost
                            // TODO Set Description
                            // TODO Set Min Cost
                            // TODO Set Max Cost
                            // TODO Set Weight
                            // TODO Set Active Slots


                    )


                    // Filter for this specific enchant
                    .filter(RegistryKey.ENCHANTMENT.typedKey(enchantKey))
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

    public  RegistryKeySet<ItemType> createItemKeySet(YamlConfiguration tagsConf, List<String> rawInputs) {
        List<TypedKey<ItemType>> resolvedKeys = new ArrayList<>();

        for (String input : rawInputs) {
            if (input == null || input.isBlank()) continue;

            String inputString = input.toLowerCase();

            // Handle Tags (e.g., "#swords" or "#minecraft:swords")
            if (inputString.startsWith("#")) {
                inputString = inputString.substring(1);

                // Check if the tag exists

                if (tagsConf.getStringList(inputString).isEmpty()) {
                    logger.warn("Unknown or empty tag #" + inputString + " in enchants.yml");
                    continue;
                }

                // Get all the itemtypes associated with the tag and add it to our key list.
                tagsConf.getStringList(inputString).forEach(item -> addItemKey(resolvedKeys, item.toLowerCase()));

            }
            // Handle Direct Item Names (e.g., "diamond_sword")
            else {
                addItemKey(resolvedKeys, inputString);
            }
        }
        // Return registry key set from the resolved keys item list
        return RegistrySet.keySet(RegistryKey.ITEM, resolvedKeys);
    }

    private void addItemKey(List<TypedKey<ItemType>> resolvedKeys, String item) {
        // Defaults to "minecraft" namespace if omitted
        NamespacedKey itemKey = NamespacedKey.fromString(item);
        // Unknown items would break the registry, so skip them. Only vanilla items can be checked here.
        if (itemKey == null || (itemKey.getNamespace().equals(NamespacedKey.MINECRAFT) && Material.getMaterial(itemKey.getKey().toUpperCase()) == null)) {
            logger.warn("Unknown item " + item + " in enchants.yml or tags.yml, skipping");
            return;
        }
        resolvedKeys.add(TypedKey.create(RegistryKey.ITEM, itemKey));
    }
}
