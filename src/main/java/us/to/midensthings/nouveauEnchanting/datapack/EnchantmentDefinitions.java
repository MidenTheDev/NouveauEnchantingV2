package us.to.midensthings.nouveauEnchanting.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import us.to.midensthings.nouveauEnchanting.addons.CustomEnchantment;
import us.to.midensthings.nouveauEnchanting.effects.EffectChanges;
import us.to.midensthings.nouveauEnchanting.effects.EffectComponent;
import us.to.midensthings.nouveauEnchanting.effects.EnchantmentEffect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/***
 * Builds the enchantment definitions that go in the generated datapack.
 */
public final class EnchantmentDefinitions {
    // The IDs of the server's attributes, without the minecraft namespace
    private static final Set<String> ATTRIBUTES = findAttributes();

    private final VanillaData vanillaData;
    private final ComponentLogger logger;

    public EnchantmentDefinitions(VanillaData vanillaData, ComponentLogger logger) {
        this.vanillaData = vanillaData;
        this.logger = logger;
    }

    /***
     * Creates the definition of a custom enchant with its default effects.
     * Everything except the effects is only a placeholder: the real settings (the addon's defaults and enchants.yml)
     * are applied when the server loads the enchant, the same way as for vanilla enchants.
     */
    public JsonObject createCustom(CustomEnchantment enchant) {
        JsonObject description = new JsonObject();
        description.addProperty("text", PlainTextComponentSerializer.plainText().serialize(enchant.getDescription()));

        JsonArray slots = new JsonArray();
        slots.add("any");

        JsonObject definition = new JsonObject();
        definition.add("description", description);
        definition.addProperty("supported_items", "minecraft:stick");
        definition.addProperty("weight", 1);
        definition.addProperty("max_level", 1);
        definition.add("min_cost", cost(1, 0));
        definition.add("max_cost", cost(1, 0));
        definition.addProperty("anvil_cost", 1);
        definition.add("slots", slots);

        applyChanges(definition, enchant.getKey(), new EffectChanges(false, enchant.getEffects(), Set.of()));
        return definition;
    }

    /***
     * Changes the effects in an enchant's definition.
     * @return true if the definition can be used. If something in the effects isn't supported by this server,
     * the definition is left unchanged and false is returned.
     */
    public boolean applyChanges(JsonObject definition, NamespacedKey enchant, EffectChanges changes) {
        JsonObject effects = definition.has("effects") && definition.get("effects").isJsonObject()
                ? definition.getAsJsonObject("effects").deepCopy() : new JsonObject();

        for (EffectComponent component : changes.removed()) {
            effects.remove(component.getId());
        }

        // Group the new effects by kind, keeping their order
        Map<EffectComponent, List<EnchantmentEffect>> byComponent = new LinkedHashMap<>();
        for (EnchantmentEffect effect : changes.effects()) {
            if (isUsable(effect, enchant)) {
                byComponent.computeIfAbsent(effect.getComponent(), component -> new ArrayList<>()).add(effect);
            }
        }

        for (Map.Entry<EffectComponent, List<EnchantmentEffect>> entry : byComponent.entrySet()) {
            EffectComponent component = entry.getKey();
            List<EnchantmentEffect> newEffects = entry.getValue();
            switch (component.getShape()) {
                case FLAG, SINGLE_VALUE ->
                        // There can only be one, so the last one wins
                        effects.add(component.getId(), newEffects.getLast().toJson());
                default -> {
                    JsonArray list = new JsonArray();
                    if (changes.add() && effects.has(component.getId()) && effects.get(component.getId()).isJsonArray()) {
                        list = effects.getAsJsonArray(component.getId());
                    }
                    for (EnchantmentEffect effect : newEffects) {
                        list.add(effect.toJson());
                    }
                    effects.add(component.getId(), list);
                }
            }
        }

        if (effects.has(EffectComponent.ATTRIBUTES.getId())) {
            addAttributeIds(effects.getAsJsonArray(EffectComponent.ATTRIBUTES.getId()), enchant);
        }

        // Exponent values only exist on newer versions, and the server won't start with a definition it can't read
        if (effects.toString().contains("\"minecraft:exponent\"")
                && !vanillaData.hasServerClass("net.minecraft.world.item.enchantment.LevelBasedValue$Exponent")) {
            logger.warn("The effects of enchant " + enchant.asString() + " use an exponent value, which this Minecraft version doesn't have. Its effects weren't changed.");
            return false;
        }

        if (effects.isEmpty()) {
            definition.remove("effects");
        } else {
            definition.add("effects", effects);
        }
        return true;
    }

    /***
     * Attribute modifiers need an ID that's unique on the entity, so make one from the enchant's key for the ones without.
     */
    private void addAttributeIds(JsonArray modifiers, NamespacedKey enchant) {
        Set<String> used = new HashSet<>();
        for (JsonElement element : modifiers) {
            JsonObject modifier = element.getAsJsonObject();
            if (modifier.has("id")) {
                used.add(modifier.get("id").getAsString());
            }
        }
        for (JsonElement element : modifiers) {
            JsonObject modifier = element.getAsJsonObject();
            if (modifier.has("id")) {
                continue;
            }
            String attribute = NamespacedKey.fromString(modifier.get("attribute").getAsString()).getKey();
            String base = "nouveauenchanting:enchantment." + enchant.getNamespace() + "." + enchant.getKey() + "." + attribute;
            String id = base;
            for (int i = 2; used.contains(id); i++) {
                id = base + "." + i;
            }
            used.add(id);
            modifier.addProperty("id", id);
        }
    }

    /***
     * Checks what can be checked about an effect before the server reads it, since a definition the server can't read stops it from starting.
     */
    private boolean isUsable(EnchantmentEffect effect, NamespacedKey enchant) {
        if (effect.getComponent() != EffectComponent.ATTRIBUTES) {
            return true;
        }
        NamespacedKey attribute = NamespacedKey.fromString(effect.toJson().getAsJsonObject().get("attribute").getAsString());
        if (!attribute.getNamespace().equals(NamespacedKey.MINECRAFT) || !ATTRIBUTES.contains(attribute.getKey())) {
            logger.warn("Unknown attribute " + attribute.asString() + " in the effects of enchant " + enchant.asString() + ", skipping it"
                    + (ATTRIBUTES.isEmpty() ? " (the attributes of this server version couldn't be found)" : ""));
            return false;
        }
        return true;
    }

    /***
     * The attribute registry can't be used yet while the server is bootstrapping, so the names are taken from the
     * constants of the server's Attribute class (e.g. MOVEMENT_SPEED), which are named after the attributes' IDs.
     * Reading the names doesn't load the class, which would need the registry.
     */
    private static Set<String> findAttributes() {
        Set<String> attributes = new HashSet<>();
        try {
            for (Field field : Attribute.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Attribute.class) {
                    attributes.add(field.getName().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Throwable ignored) {
        }
        return attributes;
    }

    private static JsonObject cost(int base, int perLevel) {
        JsonObject cost = new JsonObject();
        cost.addProperty("base", base);
        cost.addProperty("per_level_above_first", perLevel);
        return cost;
    }
}
