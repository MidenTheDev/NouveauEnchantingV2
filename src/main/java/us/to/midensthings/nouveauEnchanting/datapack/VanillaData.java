package us.to.midensthings.nouveauEnchanting.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;

/***
 * Reads vanilla's data from the server jar: the enchantment definitions and the server's data pack format.
 * Reading it from the running server means it always matches the server's Minecraft version.
 */
public final class VanillaData {
    private static final String ENCHANTMENT_PATH = "data/minecraft/enchantment/";

    private final ComponentLogger logger;
    private final ClassLoader classLoader = VanillaData.class.getClassLoader();

    public VanillaData(ComponentLogger logger) {
        this.logger = logger;
    }

    /***
     * @param id the enchant's ID without the minecraft namespace, e.g. sharpness
     * @return vanilla's definition of the enchant, or null if there isn't one or it can't be read.
     */
    public JsonObject getEnchantment(String id) {
        JsonElement json = readJson(ENCHANTMENT_PATH + id + ".json");
        return json != null && json.isJsonObject() ? json.getAsJsonObject() : null;
    }

    /***
     * @param name the full name of one of Minecraft's own classes
     * @return true if this server version has that class, for features that only exist on some versions.
     */
    public boolean hasServerClass(String name) {
        return classLoader.getResource(name.replace('.', '/') + ".class") != null;
    }

    /***
     * @return the server's data pack format as {major, minor}, or null if it can't be read.
     */
    public int[] getDataPackFormat() {
        try {
            // Libraries can have their own version.json, so find the one that's Minecraft's
            Enumeration<URL> versionFiles = classLoader.getResources("version.json");
            for (URL url : Collections.list(versionFiles)) {
                JsonElement json = readJson(url);
                if (json == null || !json.isJsonObject() || !json.getAsJsonObject().has("pack_version")) {
                    continue;
                }
                JsonObject packVersion = json.getAsJsonObject().getAsJsonObject("pack_version");
                if (packVersion.has("data_major")) {
                    int minor = packVersion.has("data_minor") ? packVersion.get("data_minor").getAsInt() : 0;
                    return new int[]{packVersion.get("data_major").getAsInt(), minor};
                }
            }
        } catch (IOException | RuntimeException e) {
            logger.warn("Could not read the server's data pack format", e);
        }
        return null;
    }

    private JsonElement readJson(String path) {
        URL url = classLoader.getResource(path);
        return url == null ? null : readJson(url);
    }

    private JsonElement readJson(URL url) {
        try (InputStream in = url.openStream()) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            logger.warn("Could not read " + url, e);
            return null;
        }
    }
}
