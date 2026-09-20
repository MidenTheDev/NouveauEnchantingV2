package us.to.midensthings.nouveauEnchanting.enchanting;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.compat.ItemsAdderCompat;

public class EnchHandler {
    private Enchantment currentEnchant;
    private int currentEnchantLevelCost;
    private int currentEnchantMaterialCost;

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    public EnchHandler () {
        currentEnchant = null;
    }

    /***
    Checks if the given tool and material have a valid enchanting result.
     */
    public boolean isValidRecipe(ItemStack tool, ItemStack material) {
        EnchantMaterial enchMaterial = getCompatibleEnchantMaterial(material);


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


        for (String enchantKey : enchMaterial.getEnchantmentKeys()) {
            final Enchantment enchantment = enchantmentRegistry.get(
                    RegistryKey.ENCHANTMENT.typedKey(Key.key("minecraft:" + enchantKey)));

            if (enchantment.canEnchantItem(tool)) {

                if (enchantment.getMaxLevel() == tool.getEnchantmentLevel(enchantment)) {

                    // Enchant max level, recipe not valid
                    currentEnchant = null;
                    return false;
                }

                for (Enchantment toolEnchant : tool.getEnchantments().keySet()) {
                    if (enchantment.conflictsWith(toolEnchant) && enchantment != toolEnchant) {
                        // Enchantment conflicts with already existing enchant on item.
                        currentEnchant = null;
                        return false;
                    }
                }

                // Valid enchantment for material applied to given tool found.
                int enchantLevel = tool.getEnchantmentLevel(enchantment)+1;
                currentEnchantMaterialCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName()+"."+enchantKey+"."+enchantLevel+".material-cost");
                if (currentEnchantMaterialCost == 0) {
                    if (enchMaterial.isCustomMaterial) {
                        if (enchMaterial.getCustomMaterialSource().equalsIgnoreCase("ItemsAdder")) {
                            String unNamespacedID = enchMaterial.getMaterialName();
                            unNamespacedID = unNamespacedID.substring(unNamespacedID.indexOf(":")+1);
                            currentEnchantMaterialCost = plugin.materialsConf.getInt(unNamespacedID + "." + enchantKey + "." + enchantLevel + ".material-cost");
                        }
                    } else {
                        currentEnchantMaterialCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName().toLowerCase() + "." + enchantKey + "." + enchantLevel + ".material-cost");
                    }
                }

                currentEnchantLevelCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName()+"."+enchantKey+"."+enchantLevel+".level-cost");
                if (currentEnchantLevelCost == 0) {
                    if (enchMaterial.isCustomMaterial) {
                        if (enchMaterial.getCustomMaterialSource().equalsIgnoreCase("ItemsAdder")) {
                            String unNamespacedID = enchMaterial.getMaterialName();
                            unNamespacedID = unNamespacedID.substring(unNamespacedID.indexOf(":")+1);
                            currentEnchantLevelCost = plugin.materialsConf.getInt(unNamespacedID + "." + enchantKey + "." + enchantLevel + ".level-cost");

                        }
                    } else {
                        currentEnchantLevelCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName().toLowerCase() + "." + enchantKey + "." + enchantLevel + ".level-cost");
                    }
                }

                currentEnchant = enchantment;
                return true;
            }

        }

        // NO valid enchantment for material applied to given tool found.
        currentEnchant = null;
        return false;
    }

    /***
     Gets the last enchantment handled by this EnchHandler instance.
     Returns null if it hasn't handled any enchants yet.
     */
    public Enchantment getLastEnchant() {
        return currentEnchant;
    }


    /***
     Takes an item and a material and applies the material's enchant to the tool.
     CHECK IF THE RECIPE IS VALID BEFORE USING THIS METHOD!
     This method assumes the recipe is already valid.
     */
    public ItemStack getAppliedItem(ItemStack tool, ItemStack material) {
        ItemStack result = tool.clone();


        EnchantMaterial enchMaterial = getCompatibleEnchantMaterial(material);


        /*
        Get Enchantment Registry via paper API.
        Important to do this instead of using enchantment enum b/c registry values
        are changed in NouveauEnchantingBootstrapper and may not update in the enum
        */
        final Registry<Enchantment> enchantmentRegistry = RegistryAccess
                .registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

        Enchantment enchantment;
        for (String enchantKey : enchMaterial.getEnchantmentKeys()) {
            enchantment = enchantmentRegistry.get(
                    RegistryKey.ENCHANTMENT.typedKey(Key.key("minecraft:" + enchantKey)));
            if (enchantment.canEnchantItem(tool)) {
                currentEnchant = enchantment;
                int enchantLevel = tool.getEnchantmentLevel(enchantment)+1;
                currentEnchantMaterialCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName()+"."+enchantKey+"."+enchantLevel+".material-cost");
                if (currentEnchantMaterialCost == 0) {
                    if (enchMaterial.isCustomMaterial) {
                        if (enchMaterial.getCustomMaterialSource().equalsIgnoreCase("ItemsAdder")) {
                            String unNamespacedID = enchMaterial.getMaterialName();
                            unNamespacedID = unNamespacedID.substring(unNamespacedID.indexOf(":")+1);
                            currentEnchantMaterialCost = plugin.materialsConf.getInt(unNamespacedID + "." + enchantKey + "." + enchantLevel + ".material-cost");
                        }
                    } else {
                        currentEnchantMaterialCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName().toLowerCase() + "." + enchantKey + "." + enchantLevel + ".material-cost");
                    }
                }

                currentEnchantLevelCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName()+"."+enchantKey+"."+enchantLevel+".level-cost");
                if (currentEnchantLevelCost == 0) {
                    if (enchMaterial.isCustomMaterial) {
                        if (enchMaterial.getCustomMaterialSource().equalsIgnoreCase("ItemsAdder")) {
                            String unNamespacedID = enchMaterial.getMaterialName();
                            unNamespacedID = unNamespacedID.substring(unNamespacedID.indexOf(":")+1);
                            currentEnchantLevelCost = plugin.materialsConf.getInt(unNamespacedID + "." + enchantKey + "." + enchantLevel + ".level-cost");

                        }
                    } else {
                        currentEnchantLevelCost = plugin.materialsConf.getInt(enchMaterial.getMaterialName().toLowerCase() + "." + enchantKey + "." + enchantLevel + ".level-cost");
                    }
                }
                currentEnchant = enchantment;
                result.addEnchantment(enchantment, enchantLevel);
            }

        }

        return result;
    }

    private EnchantMaterial getCompatibleEnchantMaterial(ItemStack material) {
        EnchantMaterial enchMaterial;

        // Check for compats and get the material according to compat or vanilla
        if (plugin.enabledCompats.contains("ItemsAdder")) {
            ItemsAdderCompat iaComp = new ItemsAdderCompat();
            if (iaComp.isCustomItem(material)) {
                enchMaterial = plugin.materialRegistry.getMaterial(iaComp.getItemID(material));
            } else {
                enchMaterial = plugin.materialRegistry.getMaterial(material.getType().name());
            }
        } else {
            enchMaterial = plugin.materialRegistry.getMaterial(material.getType().name());
        }
        return enchMaterial;
    }

    public int getCurrentEnchantLevelCost() {
        return currentEnchantLevelCost;
    }

    public int getCurrentEnchantMaterialCost() {
        return currentEnchantMaterialCost;
    }
}
