package us.to.midensthings.nouveauEnchanting.enchanting;

import java.util.HashMap;
import java.util.Map;

public class MaterialRegistry {

    private Map<String, EnchantMaterial> materialRegistry;

    public MaterialRegistry() {
        materialRegistry = new HashMap<>();
    }

    public Map<String, EnchantMaterial> getMaterialRegistry() {
        return materialRegistry;
    }

    public void addMaterial(String name, EnchantMaterial material) {
        materialRegistry.put(name, material);
    }

    public EnchantMaterial getMaterial(String name) {
        return materialRegistry.get(name);
    }
}
