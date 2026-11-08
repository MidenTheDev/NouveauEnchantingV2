package us.to.midensthings.nouveauEnchanting.addons;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/***
 * Loads the addon jars in plugins/NouveauEnchanting/addons. See NouveauEnchantingAddon for how to make an addon.
 * <p>
 * Addons are loaded while the server is bootstrapping so their enchants can be registered, then enabled when
 * NouveauEnchanting enables, once the plugins they depend on exist.
 */
public class AddonLoader {
    private final ComponentLogger logger;

    private record LoadedAddon(String name, List<String> depend, List<String> softdepend,
                               NouveauEnchantingAddon addon, AddonClassLoader classLoader, List<CustomEnchantment> enchantments) {}

    // Addon name -> addon, in load order
    private final Map<String, LoadedAddon> addons = new LinkedHashMap<>();
    // Addons whose onEnable ran, and so need onDisable
    private final List<LoadedAddon> enabledAddons = new ArrayList<>();

    public AddonLoader(ComponentLogger logger) {
        this.logger = logger;
    }

    /***
     * Loads every addon in the addons folder and adds their custom enchants to the registry.
     * An addon that fails to load is skipped without affecting the others.
     */
    public void loadAddons(Path dataDirectory, CustomEnchantmentRegistry enchantmentRegistry) {
        Path folder = dataDirectory.resolve("addons");
        List<Path> jars;
        try {
            Files.createDirectories(folder);
            try (Stream<Path> files = Files.list(folder)) {
                jars = files.filter(file -> file.getFileName().toString().endsWith(".jar")).sorted().toList();
            }
        } catch (IOException e) {
            logger.error("Could not read the addons folder", e);
            return;
        }

        for (Path jar : jars) {
            loadAddon(jar, enchantmentRegistry);
        }
        if (!addons.isEmpty()) {
            logger.info("Loaded " + addons.size() + " addon(s): " + String.join(", ", addons.keySet()));
        }
    }

    private void loadAddon(Path jar, CustomEnchantmentRegistry enchantmentRegistry) {
        String fileName = jar.getFileName().toString();
        YamlConfiguration description = readDescription(jar);
        if (description == null) {
            logger.warn("Skipping addon " + fileName + ", it has no addon.yml");
            return;
        }
        String name = description.getString("name");
        String mainClass = description.getString("main");
        if (name == null || mainClass == null) {
            logger.warn("Skipping addon " + fileName + ", its addon.yml needs a name and main");
            return;
        }
        if (addons.containsKey(name)) {
            logger.warn("Skipping addon " + fileName + ", an addon named " + name + " is already loaded");
            return;
        }

        AddonClassLoader classLoader = null;
        try {
            classLoader = new AddonClassLoader(jar, getClass().getClassLoader());
            Class<?> main = Class.forName(mainClass, true, classLoader);
            if (!NouveauEnchantingAddon.class.isAssignableFrom(main)) {
                logger.warn("Skipping addon " + name + ", " + mainClass + " doesn't implement NouveauEnchantingAddon");
                classLoader.close();
                return;
            }
            NouveauEnchantingAddon addon = (NouveauEnchantingAddon) main.getDeclaredConstructor().newInstance();

            List<CustomEnchantment> registered = new ArrayList<>();
            for (CustomEnchantment enchantment : addon.getEnchantments()) {
                if (enchantmentRegistry.register(enchantment, name)) {
                    registered.add(enchantment);
                }
            }

            addons.put(name, new LoadedAddon(name, description.getStringList("depend"), description.getStringList("softdepend"), addon, classLoader, registered));
            logger.info("Loaded addon " + name + " " + description.getString("version", "") + " from " + fileName
                    + " with " + registered.size() + " custom enchant(s)");
        } catch (Throwable e) {
            // Throwable so a missing class (NoClassDefFoundError) in one addon doesn't stop the server from starting
            logger.error("Could not load addon " + name + " from " + fileName, e);
            closeQuietly(classLoader);
        }
    }

    private YamlConfiguration readDescription(Path jar) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry("addon.yml");
            if (entry == null) {
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.warn("Could not read " + jar.getFileName(), e);
            return null;
        }
    }

    /***
     * Gives each addon access to the plugins it depends on, runs its onEnable and registers its enchants that are listeners.
     * Addons missing a plugin from depend aren't enabled.
     */
    public void enableAddons(NouveauEnchanting plugin) {
        for (LoadedAddon loaded : addons.values()) {
            List<String> missing = new ArrayList<>();
            for (String dependency : loaded.depend()) {
                Plugin dependencyPlugin = Bukkit.getPluginManager().getPlugin(dependency);
                if (dependencyPlugin == null) {
                    missing.add(dependency);
                } else {
                    loaded.classLoader().addDependencyLoader(dependencyPlugin.getClass().getClassLoader());
                }
            }
            if (!missing.isEmpty()) {
                logger.warn("Not enabling addon " + loaded.name() + ", it needs these plugins which aren't installed: "
                        + String.join(", ", missing) + ". Its custom enchants are still registered but won't do anything.");
                continue;
            }
            for (String dependency : loaded.softdepend()) {
                Plugin dependencyPlugin = Bukkit.getPluginManager().getPlugin(dependency);
                if (dependencyPlugin != null) {
                    loaded.classLoader().addDependencyLoader(dependencyPlugin.getClass().getClassLoader());
                }
            }

            try {
                loaded.addon().onEnable(plugin);
                for (CustomEnchantment enchantment : loaded.enchantments()) {
                    if (enchantment instanceof Listener listener) {
                        Bukkit.getPluginManager().registerEvents(listener, plugin);
                    }
                }
                enabledAddons.add(loaded);
            } catch (Throwable e) {
                logger.error("Error while enabling addon " + loaded.name(), e);
            }
        }
    }

    /***
     * Runs every enabled addon's onDisable and closes all the addon jars.
     */
    public void disableAddons() {
        for (LoadedAddon loaded : enabledAddons) {
            try {
                loaded.addon().onDisable();
            } catch (Throwable e) {
                logger.error("Error while disabling addon " + loaded.name(), e);
            }
        }
        for (LoadedAddon loaded : addons.values()) {
            closeQuietly(loaded.classLoader());
        }
        enabledAddons.clear();
        addons.clear();
    }

    private void closeQuietly(AddonClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }

    public List<String> getAddonNames() {
        return List.copyOf(addons.keySet());
    }
}
