package us.to.midensthings.nouveauEnchanting.effects;

import java.util.Locale;

/***
 * The kinds of effect an enchant can have, i.e. what the game uses the effect for.
 */
public enum EffectComponent {
    /** Reduces damage taken (Protection). */
    DAMAGE_PROTECTION(Shape.VALUE_LIST),
    /** Changes the damage of attacks (Sharpness, Power). */
    DAMAGE(Shape.VALUE_LIST),
    /** Changes mace smash damage for every block fallen (Density). */
    SMASH_DAMAGE_PER_FALLEN_BLOCK(Shape.VALUE_LIST),
    /** Changes the knockback of attacks (Knockback, Punch). */
    KNOCKBACK(Shape.VALUE_LIST),
    /** Changes how effective the target's armor is, from 0 to 1 (Breach). */
    ARMOR_EFFECTIVENESS(Shape.VALUE_LIST),
    /** Changes how much durability the item loses (Unbreaking). */
    ITEM_DAMAGE(Shape.VALUE_LIST),
    /** Changes how much ammo is used (Infinity). */
    AMMO_USE(Shape.VALUE_LIST),
    /** Changes how many entities a projectile goes through (Piercing). */
    PROJECTILE_PIERCING(Shape.VALUE_LIST),
    /** Changes the spread between projectiles, in degrees (Multishot). */
    PROJECTILE_SPREAD(Shape.VALUE_LIST),
    /** Changes how many projectiles are fired (Multishot). */
    PROJECTILE_COUNT(Shape.VALUE_LIST),
    /** Changes how fast a thrown trident returns (Loyalty). */
    TRIDENT_RETURN_ACCELERATION(Shape.VALUE_LIST),
    /** Changes how long fishing takes, in seconds (Lure). */
    FISHING_TIME_REDUCTION(Shape.VALUE_LIST),
    /** Changes the luck of fishing loot (Luck of the Sea). */
    FISHING_LUCK_BONUS(Shape.VALUE_LIST),
    /** Changes the experience dropped by blocks (Silk Touch). */
    BLOCK_EXPERIENCE(Shape.VALUE_LIST),
    /** Changes the experience dropped by mobs. */
    MOB_EXPERIENCE(Shape.VALUE_LIST),
    /** Changes how much durability is repaired for each experience point (Mending). */
    REPAIR_WITH_XP(Shape.VALUE_LIST),
    /** Changes the chance of mobs dropping their equipment (Looting). */
    EQUIPMENT_DROPS(Shape.TARGETED_VALUE_LIST),
    /** Changes how long a crossbow takes to charge, in seconds (Quick Charge). */
    CROSSBOW_CHARGE_TIME(Shape.SINGLE_VALUE),
    /** Changes the strength of a trident's spin attack (Riptide). */
    TRIDENT_SPIN_ATTACK_STRENGTH(Shape.SINGLE_VALUE),
    /** Changes the wearer's attributes while the item is in an active slot (Efficiency, Swift Sneak). */
    ATTRIBUTES(Shape.ATTRIBUTE_LIST),
    /** The item isn't dropped on death (Curse of Vanishing). */
    PREVENT_EQUIPMENT_DROP(Shape.FLAG),
    /** The item can't be taken off (Curse of Binding). */
    PREVENT_ARMOR_CHANGE(Shape.FLAG);

    /***
     * How a kind of effect is written in an enchantment definition.
     */
    public enum Shape {
        /** A list of value effects. */
        VALUE_LIST,
        /** A list of value effects that also say whose item is enchanted. */
        TARGETED_VALUE_LIST,
        /** A single value effect. */
        SINGLE_VALUE,
        /** A list of attribute modifiers. */
        ATTRIBUTE_LIST,
        /** Just on or off. */
        FLAG
    }

    private final Shape shape;

    EffectComponent(Shape shape) {
        this.shape = shape;
    }

    public Shape getShape() {
        return shape;
    }

    /***
     * @return the effect's ID in enchantment definitions, e.g. minecraft:damage_protection.
     */
    public String getId() {
        return "minecraft:" + name().toLowerCase(Locale.ROOT);
    }

    /***
     * @return the effect's name in enchants.yml, e.g. damage-protection.
     */
    public String getConfigName() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /***
     * @return the effect with that enchants.yml name (dashes or underscores), or null if there isn't one.
     */
    public static EffectComponent fromConfigName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replace('-', '_');
        for (EffectComponent component : values()) {
            if (component.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return component;
            }
        }
        return null;
    }
}
