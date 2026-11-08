package us.to.midensthings.nouveauEnchanting.addons;

import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

import java.util.List;

/***
 * The main class of a NouveauEnchanting addon. Addons are jars in plugins/NouveauEnchanting/addons with an addon.yml at the root:
 * <pre>
 * name: MyAddon
 * main: com.example.MyAddon          # the class implementing NouveauEnchantingAddon, needs a no-argument constructor
 * version: 1.0
 * depend: [ProjectKorra]             # (optional) plugins the addon needs, it isn't enabled if one is missing
 * softdepend: [WorldGuard]           # (optional) plugins the addon can use if they're installed
 * </pre>
 * Addons can use the classes of NouveauEnchanting, the Paper API, and the plugins listed in depend and softdepend,
 * without NouveauEnchanting depending on those plugins.
 * <p>
 * Addons are loaded in two steps:
 * <ol>
 *     <li>While the server is bootstrapping, {@link #getEnchantments()} is called so the custom enchants can be added to
 *     the enchantment registry. No plugins are loaded yet at this point, so the addon's main class and enchant classes
 *     can't use classes from the plugins in depend or softdepend (not even as fields or method parameters).</li>
 *     <li>When NouveauEnchanting enables, {@link #onEnable(NouveauEnchanting)} is called. Classes from depend and softdepend
 *     can be used from here on, so code that needs them should go in separate classes used from onEnable.</li>
 * </ol>
 * NouveauEnchanting supports Folia, where the Bukkit scheduler doesn't work. Addons should schedule tasks with
 * us.to.midensthings.nouveauEnchanting.util.Schedulers (or Paper's entity, global region and async schedulers) instead.
 */
public interface NouveauEnchantingAddon {

    /***
     * Called while the server is bootstrapping, before enchants.yml and materials.yml are loaded, so the enchants
     * returned here can be configured in enchants.yml and used as enchanting materials' enchants in materials.yml.
     * @return the custom enchants this addon adds. Enchants that implement Listener are registered automatically.
     */
    default List<CustomEnchantment> getEnchantments() {
        return List.of();
    }

    /***
     * Called when NouveauEnchanting enables, if all the plugins in depend are installed.
     * Register listeners, commands, etc. here.
     */
    default void onEnable(NouveauEnchanting plugin) {
    }

    /***
     * Called when NouveauEnchanting is disabled.
     */
    default void onDisable() {
    }

}
