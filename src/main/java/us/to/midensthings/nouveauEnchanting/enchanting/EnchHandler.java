package us.to.midensthings.nouveauEnchanting.enchanting;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.compat.CraftEngineCompat;
import us.to.midensthings.nouveauEnchanting.compat.ItemsAdderCompat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EnchHandler {
    // Enchant keys in materials.yml that don't exist in the registry, so each one is only warned about once
    private static final Set<String> warnedEnchantKeys = ConcurrentHashMap.newKeySet();

    private Enchantment currentEnchant;
    private String currentEnchantKey;
    private String currentMaterialKey;
    private int currentEnchantLevel;
    private int currentEnchantLevelCost;
    private int currentEnchantMaterialCost;

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    public EnchHandler () {
        currentEnchant = null;
    }

    /***
    Checks if the given tool and material have a valid enchanting result.
    If they do, the enchant, its level and its costs are stored on this EnchHandler.
     */
    public boolean isValidRecipe(ItemStack tool, ItemStack material) {
        currentEnchant = null;

        if (tool == null || tool.isEmpty() || material == null || material.isEmpty()) {
            return false;
        }

        // Only enchant one item at a time, otherwise a whole stack gets enchanted for the price of one
        if (tool.getAmount() != 1) {
            return false;
        }

        String materialKey = getMaterialKey(material);
        EnchantMaterial enchMaterial = plugin.materialRegistry.getMaterial(materialKey);

        if (enchMaterial == null) {
            // Not a valid material for enchanting at all
            return false;
        }
        /*
        Get Enchantment Registry via paper API.
        Important to do this instead of using enchantment enum b/c registry values
        are changed in NouveauEnchantingBootstrapper and may not update in the enum
        */
        final Registry<Enchantment> enchantmentRegistry = RegistryAccess
                .registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

        // Use the first enchant from this material that can actually be applied to the tool
        for (String enchantKey : enchMaterial.getEnchantmentKeys()) {
            final Enchantment enchantment = getEnchantment(enchantmentRegistry, enchantKey);

            if (enchantment == null || !enchantment.canEnchantItem(tool)) {
                continue;
            }

            int existingLevel = tool.getEnchantmentLevel(enchantment);
            if (existingLevel >= enchantment.getMaxLevel()) {
                // Enchant already at max level
                continue;
            }

            if (conflictsWithTool(enchantment, tool)) {
                // Enchantment conflicts with already existing enchant on item.
                continue;
            }

            // Valid enchantment for material applied to given tool found.
            currentEnchant = enchantment;
            currentEnchantKey = enchantKey;
            currentMaterialKey = materialKey;
            currentEnchantLevel = existingLevel + 1;
            currentEnchantMaterialCost = plugin.materialsConf.getInt(getMaterialsPath(materialKey, enchantKey + "." + currentEnchantLevel + ".material-cost"));
            currentEnchantLevelCost = plugin.materialsConf.getInt(getMaterialsPath(materialKey, enchantKey + "." + currentEnchantLevel + ".level-cost"));
            return true;
        }

        // NO valid enchantment for material applied to given tool found.
        return false;
    }

    /***
     Gets the last enchantment handled by this EnchHandler instance.
     Returns null if the last recipe checked wasn't valid.
     */
    public Enchantment getLastEnchant() {
        return currentEnchant;
    }


    /***
     Returns a copy of the tool with the enchant found by the last isValidRecipe call applied.
     CHECK IF THE RECIPE IS VALID BEFORE USING THIS METHOD!
     This method assumes the recipe is already valid.
     */
    public ItemStack getAppliedItem(ItemStack tool) {
        ItemStack result = tool.clone();
        result.addEnchantment(currentEnchant, currentEnchantLevel);
        return result;
    }

    /***
     Gets the path to the ServerLevels requirements for the enchant found by the last isValidRecipe call.
     Returns null if there are no requirements.
     */
    public String getSlRequirementsPath() {
        String path = getMaterialsPath(currentMaterialKey, currentEnchantKey + "." + currentEnchantLevel + ".sl-requirements");
        if (plugin.materialsConf.getConfigurationSection(path) == null) {
            return null;
        }
        return path;
    }

    public EnchantMaterial getCompatibleEnchantMaterial(ItemStack material) {
        return plugin.materialRegistry.getMaterial(getMaterialKey(material));
    }

    /***
     Gets the key the material is registered under in the material registry.
     Custom items use their un-namespaced ID, vanilla items use their material name.
     */
    private String getMaterialKey(ItemStack material) {
        if (plugin.enabledCompats.contains("ItemsAdder")) {
            ItemsAdderCompat iaComp = new ItemsAdderCompat();
            if (iaComp.isCustomItem(material)) {
                return iaComp.getItemID(material);
            }
        }
        if (plugin.enabledCompats.contains("CraftEngine")) {
            CraftEngineCompat ceComp = new CraftEngineCompat();
            if (ceComp.isCustomItem(material)) {
                return ceComp.getItemID(material);
            }
        }
        return material.getType().name();
    }

    // Material keys in materials.yml can be uppercase or lowercase, so fall back to lowercase if the exact key isn't there
    private String getMaterialsPath(String materialKey, String subPath) {
        String path = materialKey + "." + subPath;
        if (plugin.materialsConf.contains(path)) {
            return path;
        }
        return materialKey.toLowerCase() + "." + subPath;
    }

    private Enchantment getEnchantment(Registry<Enchantment> enchantmentRegistry, String enchantKey) {
        NamespacedKey key = NamespacedKey.fromString(enchantKey);
        Enchantment enchantment = key == null ? null : enchantmentRegistry.get(key);
        if (enchantment == null && warnedEnchantKeys.add(enchantKey)) {
            plugin.getLogger().warning("Unknown enchantment " + enchantKey + " in materials.yml");
        }
        return enchantment;
    }

    private boolean conflictsWithTool(Enchantment enchantment, ItemStack tool) {
        for (Enchantment toolEnchant : tool.getEnchantments().keySet()) {
            if (toolEnchant != enchantment && enchantment.conflictsWith(toolEnchant)) {
                return true;
            }
        }
        return false;
    }

    public int getCurrentEnchantLevel() {
        return currentEnchantLevel;
    }

    public int getCurrentEnchantLevelCost() {
        return currentEnchantLevelCost;
    }

    public int getCurrentEnchantMaterialCost() {
        return currentEnchantMaterialCost;
    }
}
