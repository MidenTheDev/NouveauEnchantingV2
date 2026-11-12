package us.to.midensthings.nouveauEnchanting.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/***
 * How an enchantment effect changes a number, e.g. the damage dealt or the durability lost.
 */
public final class ValueEffect {
    private final JsonObject json;

    private ValueEffect(JsonObject json) {
        this.json = json;
    }

    /***
     * Adds to the number. Negative values subtract.
     */
    public static ValueEffect add(LevelValue value) {
        return single("add", "value", value);
    }

    /***
     * Multiplies the number.
     */
    public static ValueEffect multiply(LevelValue factor) {
        return single("multiply", "factor", factor);
    }

    /***
     * Replaces the number.
     */
    public static ValueEffect set(LevelValue value) {
        return single("set", "value", value);
    }

    /***
     * For whole numbers: each point has this chance (0 to 1) of being removed.
     * Unbreaking uses this to sometimes not use up durability.
     */
    public static ValueEffect removeBinomial(LevelValue chance) {
        return single("remove_binomial", "chance", chance);
    }

    /***
     * Applies several changes, one after the other.
     */
    public static ValueEffect allOf(List<ValueEffect> effects) {
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("all-of needs at least one effect");
        }
        JsonArray array = new JsonArray();
        effects.forEach(effect -> array.add(effect.toJson()));
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:all_of");
        json.add("effects", array);
        return new ValueEffect(json);
    }

    /***
     * @return the effect in the format used by enchantment definitions in datapacks.
     */
    public JsonElement toJson() {
        return json.deepCopy();
    }

    private static ValueEffect single(String type, String field, LevelValue value) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:" + type);
        json.add(field, value.toJson());
        return new ValueEffect(json);
    }
}
