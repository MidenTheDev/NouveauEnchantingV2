package us.to.midensthings.nouveauEnchanting;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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
    MaterialRegistry materialRegistry;
    YamlConfiguration materialsConf;
    YamlConfiguration enchantsConf;
    Path dataDirectory;
    ComponentLogger logger;
    File materialsYml;
    File enchantsYml;


    @Override
    public void bootstrap(BootstrapContext context) {
        logger = context.getLogger();
        dataDirectory = context.getDataDirectory();
        materialsYml = dataDirectory.resolve("materials.yml").toFile();
        enchantsYml = dataDirectory.resolve("enchants.yml").toFile();

        generateFile(materialsYml, "materials.yml");
        generateFile(enchantsYml, "enchants.yml");

        materialsConf = YamlConfiguration.loadConfiguration(materialsYml);
        enchantsConf = YamlConfiguration.loadConfiguration(enchantsYml);


        // Fill in material registry
        materialRegistry = new MaterialRegistry();

        // Check material config for each material, check each enchantment available per material, and load them into the material registry
        materialsConf.getConfigurationSection("").getKeys(false).forEach(material -> {
            List<String> enchantKeys = new ArrayList<>();
            materialsConf.getConfigurationSection(material).getKeys(false).forEach(enchantKey -> {
                enchantKeys.add(enchantKey);
            });
            EnchantMaterial enchantMaterial = new EnchantMaterial(Material.valueOf(material), enchantKeys);
            materialRegistry.addMaterial(material, enchantMaterial);
        });

        // Modify vanilla enchants according to enchants.yml
        enchantsConf.getConfigurationSection("").getKeys(false).forEach(enchant -> {
            List<String> allowedItems = enchantsConf.getStringList(enchant+"allowed-items");

            allowedItems.forEach(itemOrTag -> {
                if (itemOrTag.substring(0,0).equals("#")) {
                    // is a tag, add all items from tag to list
                } else {
                    // is an item, add all to list directly
                }

            });

            context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd()
                    // Set Max Level
                    .newHandler(event -> event.builder().maxLevel(enchantsConf.getInt(enchant+".max-level"))
                            .suppo
                    )


                    // Filter for this specific enchant
                    .filter(RegistryKey.ENCHANTMENT.typedKey(Key.key("minecraft:"+enchant)))
            );

        });


        // TODO: Add Custom Enchantments to Registry

    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new NouveauEnchanting(materialRegistry,materialsConf);
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

    public  RegistryKeySet<ItemType> createItemKeySet(Registry<ItemType> itemRegistry, List<String> rawInputs) {
        List<TypedKey<ItemType>> resolvedKeys = new ArrayList<>();

        for (String input : rawInputs) {
            if (input == null || input.isBlank()) continue;

            String trimmed = input.trim().toLowerCase();

            // Handle Tags (e.g., "#swords" or "#minecraft:swords")
            if (trimmed.startsWith("#")) {
                String tagString = trimmed.substring(1);
                NamespacedKey tagKey = parseNamespacedKey(tagString);
                TagKey<ItemType> itemTagKey = TagKey.create(RegistryKey.ITEM, tagKey);

                // Fetch tag contents and extract each ItemType's TypedKey
                var tag = itemRegistry.getTag(itemTagKey);
                if (tag != null) {
                    for (TypedKey<ItemType> itemType : tag) {
                        itemType.typedKey().ifPresent(resolvedKeys::add);
                    }
                }
            }
            // Handle Direct Item Names (e.g., "diamond_sword")
            else {
                NamespacedKey itemKey = parseNamespacedKey(trimmed);
                resolvedKeys.add(TypedKey.create(RegistryKey.ITEM, itemKey));
            }
        }

        // Pass 2 parameters: RegistryKey and Iterable<TypedKey<ItemType>>
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
