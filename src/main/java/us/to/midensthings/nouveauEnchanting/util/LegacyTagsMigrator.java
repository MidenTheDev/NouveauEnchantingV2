package us.to.midensthings.nouveauEnchanting.util;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Converts enchants.yml from the old tags.yml system to vanilla item tags.
 * Tags from the old default tags.yml that weren't changed become the matching vanilla tags.
 * Any other tag is replaced by the items it listed, so the enchants keep working on exactly the same items.
 */
public final class LegacyTagsMigrator {
    private static final List<String> TOOL_TIERS = List.of("wooden", "stone", "copper", "iron", "golden", "diamond", "netherite");
    private static final List<String> ARMOR_TIERS = List.of("leather", "chainmail", "copper", "iron", "golden", "diamond", "netherite");

    // The items in each tag of the old default tags.yml
    private static final Map<String, Set<String>> OLD_DEFAULTS = new HashMap<>();
    // The vanilla tags each old default tag becomes
    private static final Map<String, List<String>> VANILLA_EQUIVALENTS = new HashMap<>();

    static {
        for (String tool : List.of("sword", "pickaxe", "axe", "shovel", "hoe", "spear")) {
            addDefault(tool + "s", withTiers(TOOL_TIERS, tool), List.of("#" + tool + "s"));
        }

        Set<String> helmets = withTiers(ARMOR_TIERS, "helmet");
        helmets.add("turtle_helmet");
        Set<String> chestplates = withTiers(ARMOR_TIERS, "chestplate");
        Set<String> leggings = withTiers(ARMOR_TIERS, "leggings");
        Set<String> boots = withTiers(ARMOR_TIERS, "boots");
        Set<String> armor = new HashSet<>();
        armor.addAll(helmets);
        armor.addAll(chestplates);
        armor.addAll(leggings);
        armor.addAll(boots);

        addDefault("helmets", helmets, List.of("#head_armor"));
        addDefault("chestplates", chestplates, List.of("#chest_armor"));
        addDefault("leggings", leggings, List.of("#leg_armor"));
        addDefault("boots", boots, List.of("#foot_armor"));
        addDefault("armor", armor, List.of("#head_armor", "#chest_armor", "#leg_armor", "#foot_armor"));
        addDefault("heads", Set.of("player_head", "zombie_head", "creeper_head", "skeleton_skull",
                "wither_skeleton_skull", "piglin_head", "dragon_head"), List.of("#skulls"));
    }

    // An enchant's name, e.g. "sharpness:"
    private static final Pattern ENCHANT_LINE = Pattern.compile("^([^\\s#][^:]*):.*$");
    // A setting inside an enchant that holds a list, e.g. "  allowed-items:"
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+([\\w-]+):\\s*(#.*)?$");
    // A list entry that's a tag, e.g. '    - "#swords" # comment'
    private static final Pattern TAG_ENTRY_LINE = Pattern.compile("^(\\s*-\\s*)([\"']?)#([^\"'\\s#]+)\\2(\\s+#.*)?\\s*$");

    private LegacyTagsMigrator() {
    }

    /***
     * Converts enchants.yml if there's a tags.yml in the data directory, then renames tags.yml to tags.yml.old.
     * Must run before enchants.yml is loaded.
     */
    public static void migrate(Path dataDirectory, File enchantsYml, ComponentLogger logger) {
        File tagsYml = dataDirectory.resolve("tags.yml").toFile();
        File oldTagsYml = dataDirectory.resolve("tags.yml.old").toFile();

        if (!tagsYml.exists()) {
            if (oldTagsYml.exists()) {
                logger.info("tags.yml.old is no longer used by Nouveau Enchanting and can be deleted.");
            }
            return;
        }

        logger.info("Found tags.yml from an older version of Nouveau Enchanting, converting enchants.yml to use vanilla item tags...");
        YamlConfiguration tagsConf = YamlConfiguration.loadConfiguration(tagsYml);

        // Work out what each tag in tags.yml gets replaced with
        Map<String, List<String>> replacements = new HashMap<>();
        for (String tag : tagsConf.getKeys(false)) {
            String tagName = tag.toLowerCase();
            List<String> items = tagsConf.getStringList(tag).stream().map(String::toLowerCase).toList();
            if (VANILLA_EQUIVALENTS.containsKey(tagName) && normalize(items).equals(OLD_DEFAULTS.get(tagName))) {
                replacements.put(tagName, VANILLA_EQUIVALENTS.get(tagName));
            } else {
                replacements.put(tagName, items);
            }
        }

        try {
            // Edit the file line by line instead of saving it through the config API, which would reformat it and drop some comments
            List<String> lines = Files.readAllLines(enchantsYml.toPath());
            List<String> newLines = new ArrayList<>();
            String enchant = null;
            String field = null;
            int changes = 0;

            for (String line : lines) {
                Matcher enchantMatcher = ENCHANT_LINE.matcher(line);
                Matcher fieldMatcher = FIELD_LINE.matcher(line);
                Matcher entryMatcher = TAG_ENTRY_LINE.matcher(line);

                if (enchantMatcher.matches()) {
                    enchant = enchantMatcher.group(1);
                    field = null;
                } else if (fieldMatcher.matches()) {
                    field = fieldMatcher.group(1);
                } else if (entryMatcher.matches() && ("allowed-items".equals(field) || "primary-items".equals(field))) {
                    String tagName = stripMinecraft(entryMatcher.group(3).toLowerCase());
                    List<String> replacement = replacements.get(tagName);

                    // Only replace tags from tags.yml that aren't already the same vanilla tag
                    if (replacement != null && !replacement.equals(List.of("#" + tagName))) {
                        String prefix = entryMatcher.group(1);
                        String quote = entryMatcher.group(2).isEmpty() ? "\"" : entryMatcher.group(2);
                        String comment = entryMatcher.group(4) == null ? "" : entryMatcher.group(4);
                        for (int i = 0; i < replacement.size(); i++) {
                            newLines.add(prefix + quote + replacement.get(i) + quote + (i == 0 ? comment : ""));
                        }
                        changes++;
                        logger.info("  " + enchant + "." + field + ": replaced #" + entryMatcher.group(3) + " with " + String.join(", ", replacement));
                        continue;
                    }
                }
                newLines.add(line);
            }

            if (changes > 0) {
                // Keep a copy of the old enchants.yml in case the conversion isn't what the server owner wanted
                Path backup = dataDirectory.resolve("enchants.yml.pre-tags-conversion");
                Files.copy(enchantsYml.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
                Files.write(enchantsYml.toPath(), newLines);
                logger.info("Made " + changes + " changes to enchants.yml. The old version was saved as " + backup.getFileName() + ".");
            } else {
                logger.info("enchants.yml didn't need any changes.");
            }

            Files.move(tagsYml.toPath(), oldTagsYml.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("tags.yml is no longer used by Nouveau Enchanting, it was renamed to tags.yml.old and can be deleted.");
        } catch (IOException e) {
            logger.error("Could not finish converting tags.yml! enchants.yml may still use tags from tags.yml.", e);
        }
    }

    private static void addDefault(String tag, Set<String> items, List<String> vanillaTags) {
        OLD_DEFAULTS.put(tag, items);
        VANILLA_EQUIVALENTS.put(tag, vanillaTags);
    }

    private static Set<String> withTiers(List<String> tiers, String item) {
        Set<String> items = new HashSet<>();
        for (String tier : tiers) {
            items.add(tier + "_" + item);
        }
        return items;
    }

    private static Set<String> normalize(List<String> items) {
        Set<String> normalized = new HashSet<>();
        for (String item : items) {
            normalized.add(stripMinecraft(item));
        }
        return normalized;
    }

    private static String stripMinecraft(String key) {
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }
}
