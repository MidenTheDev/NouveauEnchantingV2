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
    private List<String> enchantmentKeys;


    public EnchantMaterial(Material material, List<String> keys) {
        vanillaMaterial = material;
        enchantmentKeys = keys;


    }


    public Material getVanillaMaterial() {
        return vanillaMaterial;
    }

    public List<String> getEnchantmentKeys() {
        return enchantmentKeys;
    }

}
