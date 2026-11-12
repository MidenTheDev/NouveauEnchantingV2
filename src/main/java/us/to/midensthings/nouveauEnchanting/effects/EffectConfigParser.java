package us.to.midensthings.nouveauEnchanting.effects;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/***
 * Reads the effects section of an enchant in enchants.yml or in an enchant file in the addons folder.
 * See bane_of_arthropods in the default enchants.yml for the format.
 * Anything that isn't valid is skipped with a warning, so a mistake can't end up in the generated datapack.
 */
public final class EffectConfigParser {
    // Kinds of effect Minecraft has that can't be set in the configs yet
    private static final Set<String> UNSUPPORTED = Set.of("post_attack", "post_piercing_attack", "hit_block", "tick",
            "location_changed", "projectile_spawned", "damage_immunity", "crossbow_charging_sounds", "trident_sound");

    private final ComponentLogger logger;
    private final String fileName;

    /***
     * @param fileName the name of the file being read, used in warnings
     */
    public EffectConfigParser(ComponentLogger logger, String fileName) {
        this.logger = logger;
        this.fileName = fileName;
    }

    /***
     * @param enchant the enchant's name in the file, used in warnings
     * @param section the enchant's effects section
     */
    public EffectChanges parse(String enchant, ConfigurationSection section) {
        boolean add = false;
        List<EnchantmentEffect> effects = new ArrayList<>();
        Set<EffectComponent> removed = EnumSet.noneOf(EffectComponent.class);

        for (Map.Entry<String, Object> entry : toMap(section).entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            String where = "effects." + name + " for enchant " + enchant + " in " + fileName;

            if (normalize(name).equals("mode")) {
                String mode = String.valueOf(value).toLowerCase(Locale.ROOT);
                if (mode.equals("add")) {
                    add = true;
                } else if (!mode.equals("replace")) {
                    logger.warn(where + " must be replace or add, using replace");
                }
                continue;
            }

            EffectComponent component = EffectComponent.fromConfigName(name);
            if (component == null) {
                if (UNSUPPORTED.contains(normalize(name).replace('-', '_'))) {
                    logger.warn(where + " can't be set in the configs yet, skipping");
                } else {
                    logger.warn(where + " isn't a known effect, skipping");
                }
                continue;
            }

            // false or an empty list removes that kind of effect from the enchant
            if (Boolean.FALSE.equals(value) || (value instanceof List<?> list && list.isEmpty())) {
                removed.add(component);
                continue;
            }

            if (component.getShape() == EffectComponent.Shape.FLAG) {
                if (Boolean.TRUE.equals(value)) {
                    effects.add(component == EffectComponent.PREVENT_ARMOR_CHANGE
                            ? EnchantmentEffect.preventArmorChange() : EnchantmentEffect.preventEquipmentDrop());
                } else {
                    logger.warn(where + " must be true or false, skipping");
                }
                continue;
            }

            // A single effect can be written without the list
            List<?> entries = value instanceof List<?> list ? list : List.of(value);
            if (component.getShape() == EffectComponent.Shape.SINGLE_VALUE && entries.size() > 1) {
                logger.warn(where + " can only have one effect, use all-of to combine several. Skipping");
                continue;
            }
            for (int i = 0; i < entries.size(); i++) {
                try {
                    effects.add(parseEffect(component, entries.get(i)));
                } catch (IllegalArgumentException e) {
                    logger.warn(where + ", entry " + (i + 1) + ": " + e.getMessage() + ". Skipping it");
                }
            }
        }
        return new EffectChanges(add, effects, removed);
    }

    private EnchantmentEffect parseEffect(EffectComponent component, Object value) {
        Map<String, Object> map = requireMap(value, "the effect");
        if (map.containsKey("conditions") || map.containsKey("requirements")) {
            throw new IllegalArgumentException("conditions can't be set in the configs yet");
        }

        switch (component.getShape()) {
            case ATTRIBUTE_LIST -> {
                requireOnly(map, Set.of("attribute", "amount", "operation", "id"));
                if (!(map.get("attribute") instanceof String attribute)) {
                    throw new IllegalArgumentException("it needs an attribute, e.g. attribute: movement_speed");
                }
                if (!map.containsKey("amount")) {
                    throw new IllegalArgumentException("it needs an amount");
                }
                EnchantmentEffect.AttributeOperation operation = EnchantmentEffect.AttributeOperation.ADD_VALUE;
                if (map.containsKey("operation")) {
                    operation = parseEnum(EnchantmentEffect.AttributeOperation.class, map.get("operation"), "operation");
                }
                String id = map.get("id") instanceof String string ? string : null;
                return EnchantmentEffect.attribute(attribute, parseLevelValue(map.get("amount")), operation, id);
            }
            case TARGETED_VALUE_LIST -> {
                if (!map.containsKey("enchanted")) {
                    throw new IllegalArgumentException("it needs enchanted: attacker or enchanted: victim");
                }
                EnchantmentEffect.Target target = parseEnum(EnchantmentEffect.Target.class, map.get("enchanted"), "enchanted");
                Map<String, Object> effect = new LinkedHashMap<>(map);
                effect.remove("enchanted");
                return EnchantmentEffect.equipmentDrops(target, parseValueEffect(effect));
            }
            default -> {
                return EnchantmentEffect.value(component, parseValueEffect(map));
            }
        }
    }

    /***
     * A value effect is written as one key saying what to do, e.g. add: 1 or multiply: {linear: {base: 1, per-level: 0.5}}.
     */
    private ValueEffect parseValueEffect(Object value) {
        Map<String, Object> map = requireMap(value, "the effect");
        if (map.size() != 1) {
            throw new IllegalArgumentException("it must have exactly one of add, multiply, set, remove-binomial or all-of");
        }
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        switch (entry.getKey()) {
            case "add" -> {
                return ValueEffect.add(parseLevelValue(entry.getValue()));
            }
            case "multiply" -> {
                return ValueEffect.multiply(parseLevelValue(entry.getValue()));
            }
            case "set" -> {
                return ValueEffect.set(parseLevelValue(entry.getValue()));
            }
            case "remove-binomial" -> {
                return ValueEffect.removeBinomial(parseLevelValue(entry.getValue()));
            }
            case "all-of" -> {
                if (!(entry.getValue() instanceof List<?> list)) {
                    throw new IllegalArgumentException("all-of must be a list of effects");
                }
                List<ValueEffect> effects = new ArrayList<>();
                for (Object effect : list) {
                    effects.add(parseValueEffect(effect));
                }
                return ValueEffect.allOf(effects);
            }
            default -> throw new IllegalArgumentException("\"" + entry.getKey() + "\" isn't one of add, multiply, set, remove-binomial or all-of");
        }
    }

    /***
     * A level value is either a plain number, or one key saying how it changes with the level,
     * e.g. linear: {base: 1, per-level: 0.5}.
     */
    private LevelValue parseLevelValue(Object value) {
        if (value instanceof Number number) {
            return LevelValue.constant(number.doubleValue());
        }
        Map<String, Object> map = requireMap(value, "a value");
        if (map.size() != 1) {
            throw new IllegalArgumentException("a value must be a number or exactly one of linear, clamped, fraction, levels-squared, lookup or exponent");
        }
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        Object inner = entry.getValue();
        switch (entry.getKey()) {
            case "constant" -> {
                return LevelValue.constant(number(inner, "constant"));
            }
            case "linear" -> {
                Map<String, Object> fields = fields(inner, "linear", Set.of("base", "per-level"));
                return LevelValue.linear(number(fields.get("base"), "linear's base"), number(fields.get("per-level"), "linear's per-level"));
            }
            case "clamped" -> {
                Map<String, Object> fields = fields(inner, "clamped", Set.of("value", "min", "max"));
                return LevelValue.clamped(parseLevelValue(required(fields, "value", "clamped")),
                        number(fields.get("min"), "clamped's min"), number(fields.get("max"), "clamped's max"));
            }
            case "fraction" -> {
                Map<String, Object> fields = fields(inner, "fraction", Set.of("numerator", "denominator"));
                return LevelValue.fraction(parseLevelValue(required(fields, "numerator", "fraction")),
                        parseLevelValue(required(fields, "denominator", "fraction")));
            }
            case "levels-squared" -> {
                if (inner instanceof Number number) {
                    return LevelValue.levelsSquared(number.doubleValue());
                }
                Map<String, Object> fields = fields(inner, "levels-squared", Set.of("added"));
                return LevelValue.levelsSquared(number(fields.get("added"), "levels-squared's added"));
            }
            case "lookup" -> {
                Map<String, Object> fields = fields(inner, "lookup", Set.of("values", "fallback"));
                if (!(fields.get("values") instanceof List<?> list)) {
                    throw new IllegalArgumentException("lookup needs a list of values");
                }
                List<Double> values = new ArrayList<>();
                for (Object element : list) {
                    values.add(number(element, "lookup's values"));
                }
                return LevelValue.lookup(values, parseLevelValue(required(fields, "fallback", "lookup")));
            }
            case "exponent" -> {
                Map<String, Object> fields = fields(inner, "exponent", Set.of("base", "power"));
                return LevelValue.exponent(parseLevelValue(required(fields, "base", "exponent")),
                        parseLevelValue(required(fields, "power", "exponent")));
            }
            default -> throw new IllegalArgumentException("\"" + entry.getKey() + "\" isn't one of linear, clamped, fraction, levels-squared, lookup or exponent");
        }
    }

    private Map<String, Object> fields(Object value, String name, Set<String> allowed) {
        Map<String, Object> map = requireMap(value, name);
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException(name + " doesn't have a \"" + key + "\" setting, only " + String.join(", ", allowed.stream().sorted().toList()));
            }
        }
        return map;
    }

    private void requireOnly(Map<String, Object> map, Set<String> allowed) {
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("\"" + key + "\" isn't a setting here, only " + String.join(", ", allowed.stream().sorted().toList()));
            }
        }
    }

    private Object required(Map<String, Object> map, String key, String name) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(name + " needs a " + key);
        }
        return map.get(key);
    }

    private double number(Object value, String name) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException(name + " must be a number");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, Object value, String name) {
        String wanted = String.valueOf(value).toUpperCase(Locale.ROOT).replace('-', '_');
        List<String> options = new ArrayList<>();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(wanted)) {
                return constant;
            }
            options.add(constant.name().toLowerCase(Locale.ROOT));
        }
        throw new IllegalArgumentException(name + " must be one of " + String.join(", ", options));
    }

    private Map<String, Object> requireMap(Object value, String name) {
        if (value instanceof ConfigurationSection section) {
            return toMap(section);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, element) -> result.put(normalize(String.valueOf(key)), element));
            return result;
        }
        throw new IllegalArgumentException(name + " isn't written correctly, check its indentation");
    }

    private Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> result = new LinkedHashMap<>();
        section.getValues(false).forEach((key, value) -> result.put(normalize(key), value));
        return result;
    }

    // Setting names can be written with dashes or underscores, in any case
    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
