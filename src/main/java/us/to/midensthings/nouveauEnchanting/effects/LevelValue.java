package us.to.midensthings.nouveauEnchanting.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;

/***
 * A number that can depend on the enchant's level, used by enchantment effects.
 * For example, Sharpness adds linear(1, 0.5) damage: 1 at level I and 0.5 more for every level after that.
 */
public final class LevelValue {
    private final JsonElement json;

    private LevelValue(JsonElement json) {
        this.json = json;
    }

    /***
     * The same value at every level.
     */
    public static LevelValue constant(double value) {
        return new LevelValue(new JsonPrimitive(finite(value, "value")));
    }

    /***
     * base at level I, plus perLevelAboveFirst for every level after that.
     */
    public static LevelValue linear(double base, double perLevelAboveFirst) {
        JsonObject json = typed("linear");
        json.addProperty("base", finite(base, "base"));
        json.addProperty("per_level_above_first", finite(perLevelAboveFirst, "per-level"));
        return new LevelValue(json);
    }

    /***
     * Another value, kept between min and max.
     */
    public static LevelValue clamped(LevelValue value, double min, double max) {
        if (!(max > min)) {
            throw new IllegalArgumentException("max must be larger than min");
        }
        JsonObject json = typed("clamped");
        json.add("value", value.toJson());
        json.addProperty("min", finite(min, "min"));
        json.addProperty("max", finite(max, "max"));
        return new LevelValue(json);
    }

    /***
     * numerator divided by denominator.
     */
    public static LevelValue fraction(LevelValue numerator, LevelValue denominator) {
        JsonObject json = typed("fraction");
        json.add("numerator", numerator.toJson());
        json.add("denominator", denominator.toJson());
        return new LevelValue(json);
    }

    /***
     * The level squared, plus added. Efficiency uses levelsSquared(1).
     */
    public static LevelValue levelsSquared(double added) {
        JsonObject json = typed("levels_squared");
        json.addProperty("added", finite(added, "added"));
        return new LevelValue(json);
    }

    /***
     * A value picked from a list: the first for level I, the second for level II, and so on.
     * Levels past the end of the list use the fallback.
     */
    public static LevelValue lookup(List<Double> values, LevelValue fallback) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("lookup needs at least one value");
        }
        JsonArray array = new JsonArray();
        for (Double value : values) {
            array.add(finite(value, "values"));
        }
        JsonObject json = typed("lookup");
        json.add("values", array);
        json.add("fallback", fallback.toJson());
        return new LevelValue(json);
    }

    /***
     * base to the power of power. Only exists on newer Minecraft versions, enchants using it on a server
     * without it keep their normal effects.
     */
    public static LevelValue exponent(LevelValue base, LevelValue power) {
        JsonObject json = typed("exponent");
        json.add("base", base.toJson());
        json.add("power", power.toJson());
        return new LevelValue(json);
    }

    /***
     * @return the value in the format used by enchantment definitions in datapacks.
     */
    public JsonElement toJson() {
        return json.deepCopy();
    }

    private static JsonObject typed(String type) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:" + type);
        return json;
    }

    private static double finite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a normal number");
        }
        return value;
    }
}
