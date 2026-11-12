package us.to.midensthings.nouveauEnchanting.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;

import java.util.Locale;

/***
 * One effect of an enchant, e.g. "adds 1 damage per level" or "adds 5% movement speed per level".
 * These are turned into the enchantment definitions Minecraft itself uses, so the game applies them everywhere
 * (and removes them again) without any listeners.
 * <p>
 * Custom enchants return their effects from CustomEnchantment#getEffects, for example:
 * <pre>
 * return List.of(
 *     EnchantmentEffect.damage(ValueEffect.add(LevelValue.linear(1, 0.5))),
 *     EnchantmentEffect.attribute("movement_speed", LevelValue.linear(0.05, 0.05), AttributeOperation.ADD_MULTIPLIED_BASE));
 * </pre>
 * Server owners can change them in enchants.yml.
 */
public final class EnchantmentEffect {

    /***
     * Whose item has the enchant, for effects that involve two entities.
     */
    public enum Target {
        ATTACKER, VICTIM
    }

    /***
     * How an attribute modifier changes the attribute.
     */
    public enum AttributeOperation {
        /** Adds the amount to the attribute's base value. */
        ADD_VALUE,
        /** Adds the amount times the base value, e.g. 0.1 adds 10% of the base. Several of these add up. */
        ADD_MULTIPLIED_BASE,
        /** Multiplies the final value by 1 + the amount. Several of these multiply each other. */
        ADD_MULTIPLIED_TOTAL
    }

    private final EffectComponent component;
    private final JsonElement json;

    private EnchantmentEffect(EffectComponent component, JsonElement json) {
        this.component = component;
        this.json = json;
    }

    /***
     * An effect that changes a number.
     * @param component what the number is, it can't be EQUIPMENT_DROPS, ATTRIBUTES or one of the on/off effects
     */
    public static EnchantmentEffect value(EffectComponent component, ValueEffect effect) {
        switch (component.getShape()) {
            case VALUE_LIST -> {
                JsonObject json = new JsonObject();
                json.add("effect", effect.toJson());
                return new EnchantmentEffect(component, json);
            }
            case SINGLE_VALUE -> {
                return new EnchantmentEffect(component, effect.toJson());
            }
            default -> throw new IllegalArgumentException(component.getConfigName() + " isn't a simple value effect");
        }
    }

    /** Changes the damage of attacks. */
    public static EnchantmentEffect damage(ValueEffect effect) {
        return value(EffectComponent.DAMAGE, effect);
    }

    /** Reduces damage taken. */
    public static EnchantmentEffect damageProtection(ValueEffect effect) {
        return value(EffectComponent.DAMAGE_PROTECTION, effect);
    }

    /** Changes the knockback of attacks. */
    public static EnchantmentEffect knockback(ValueEffect effect) {
        return value(EffectComponent.KNOCKBACK, effect);
    }

    /** Changes how effective the target's armor is, from 0 to 1. */
    public static EnchantmentEffect armorEffectiveness(ValueEffect effect) {
        return value(EffectComponent.ARMOR_EFFECTIVENESS, effect);
    }

    /** Changes how much durability the item loses. */
    public static EnchantmentEffect itemDamage(ValueEffect effect) {
        return value(EffectComponent.ITEM_DAMAGE, effect);
    }

    /***
     * Changes the chance of mobs dropping their equipment.
     * @param enchanted whether the attacker's or the victim's item has the enchant
     */
    public static EnchantmentEffect equipmentDrops(Target enchanted, ValueEffect effect) {
        JsonObject json = new JsonObject();
        json.add("effect", effect.toJson());
        json.addProperty("enchanted", enchanted.name().toLowerCase(Locale.ROOT));
        return new EnchantmentEffect(EffectComponent.EQUIPMENT_DROPS, json);
    }

    /***
     * Changes one of the wearer's attributes while the item is in one of the enchant's active slots.
     * @param attribute the attribute's ID, e.g. movement_speed or minecraft:attack_speed
     */
    public static EnchantmentEffect attribute(String attribute, LevelValue amount, AttributeOperation operation) {
        return attribute(attribute, amount, operation, null);
    }

    /***
     * Changes one of the wearer's attributes while the item is in one of the enchant's active slots.
     * @param attribute the attribute's ID, e.g. movement_speed or minecraft:attack_speed
     * @param id a unique ID for the modifier, e.g. myaddon:swiftness. If it's null one is made from the enchant's key.
     */
    public static EnchantmentEffect attribute(String attribute, LevelValue amount, AttributeOperation operation, String id) {
        JsonObject json = new JsonObject();
        json.addProperty("attribute", key(attribute, "attribute").asString());
        json.add("amount", amount.toJson());
        json.addProperty("operation", operation.name().toLowerCase(Locale.ROOT));
        if (id != null) {
            json.addProperty("id", key(id, "id").asString());
        }
        return new EnchantmentEffect(EffectComponent.ATTRIBUTES, json);
    }

    /** The item isn't dropped on death, like Curse of Vanishing. */
    public static EnchantmentEffect preventEquipmentDrop() {
        return new EnchantmentEffect(EffectComponent.PREVENT_EQUIPMENT_DROP, new JsonObject());
    }

    /** The item can't be taken off, like Curse of Binding. */
    public static EnchantmentEffect preventArmorChange() {
        return new EnchantmentEffect(EffectComponent.PREVENT_ARMOR_CHANGE, new JsonObject());
    }

    public EffectComponent getComponent() {
        return component;
    }

    /***
     * @return the effect in the format used by enchantment definitions in datapacks.
     */
    public JsonElement toJson() {
        return json.deepCopy();
    }

    private static NamespacedKey key(String value, String name) {
        NamespacedKey key = value == null ? null : NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        if (key == null) {
            throw new IllegalArgumentException("\"" + value + "\" isn't a valid " + name);
        }
        return key;
    }
}
