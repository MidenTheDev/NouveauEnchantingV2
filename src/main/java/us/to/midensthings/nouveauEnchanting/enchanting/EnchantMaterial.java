package us.to.midensthings.nouveauEnchanting.enchanting;

import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

import java.util.List;

public class EnchantMaterial {
    private Material vanillaMaterial;
    private String materialName;
    private List<String> enchantmentKeys;
    boolean isCustomMaterial;
    String customMaterialSource;


    public EnchantMaterial(Material material, List<String> keys) {
        vanillaMaterial = material;
        enchantmentKeys = keys;
        materialName = material.name();


    }

    public EnchantMaterial(String name, List<String> keys, boolean isCustomMaterial, String customMaterialSource) {
        vanillaMaterial = null;
        enchantmentKeys = keys;
        materialName = name;
        this.isCustomMaterial = isCustomMaterial;
        this.customMaterialSource = customMaterialSource;
    }


    public Material getVanillaMaterial() {
        return vanillaMaterial;
    }

    public List<String> getEnchantmentKeys() {
        return enchantmentKeys;
    }

    public boolean isCustomMaterial() {
        return isCustomMaterial;
    }

    public String getCustomMaterialSource() {
        return customMaterialSource;
    }

    public String getMaterialName() {
        return materialName;
    }
}
