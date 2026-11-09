package us.to.midensthings.nouveauEnchanting.api;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import us.to.midensthings.nouveauEnchanting.addons.CustomEnchantment;
import us.to.midensthings.nouveauEnchanting.addons.CustomEnchantmentRegistry;

import java.util.ArrayList;
import java.util.List;

/***
 * Lets other plugins add custom enchants without making a NouveauEnchanting addon. Their enchants work exactly like
 * addon enchants: they can be configured in enchants.yml and used in materials.yml.
 * <p>
 * Enchants have to be registered while the server is bootstrapping, before NouveauEnchanting is bootstrapped, so the
 * plugin must be a Paper plugin with a bootstrapper that calls {@link #registerEnchantment} in its bootstrap method.
 * Its paper-plugin.yml needs NouveauEnchanting as a bootstrap dependency that loads after it:
 * <pre>
 * bootstrapper: com.example.MyBootstrapper
 * dependencies:
 *   bootstrap:
 *     NouveauEnchanting:
 *       load: AFTER            # NouveauEnchanting is bootstrapped after this plugin
 *       join-classpath: true   # lets the bootstrapper use NouveauEnchanting's classes
 *   server:
 *     NouveauEnchanting:
 *       load: BEFORE           # NouveauEnchanting is enabled before this plugin
 *       required: true
 * </pre>
 * Unlike addon enchants, enchants registered here aren't registered as listeners automatically, the plugin should
 * register them itself when it enables.
 */
public final class NouveauEnchantingAPI {
    private static final ComponentLogger logger = ComponentLogger.logger("NouveauEnchanting");

    private record PendingEnchantment(CustomEnchantment enchantment, String source) {}

    // Enchants registered before NouveauEnchanting was bootstrapped
    private static final List<PendingEnchantment> pending = new ArrayList<>();
    // Set once NouveauEnchanting has loaded the custom enchants, after which it's too late to register more
    private static CustomEnchantmentRegistry registry;

    private NouveauEnchantingAPI() {
    }

    /***
     * Adds a custom enchant. Must be called from a bootstrapper that runs before NouveauEnchanting's, see the class
     * documentation for the setup.
     * @param source the name of the plugin adding the enchant, used in messages and enchants.yml
     * @return true if the enchant was accepted. It can still be skipped later if its key is invalid or already used,
     * which is logged when NouveauEnchanting bootstraps.
     */
    public static synchronized boolean registerEnchantment(CustomEnchantment enchantment, String source) {
        if (registry != null) {
            logger.warn("{} tried to add the custom enchant {}:{} after NouveauEnchanting was bootstrapped, so it was skipped. "
                            + "Custom enchants must be added from a bootstrapper, and NouveauEnchanting must be a bootstrap dependency "
                            + "with load: AFTER in its paper-plugin.yml.",
                    source, enchantment.getNamespace(), enchantment.getId());
            return false;
        }
        pending.add(new PendingEnchantment(enchantment, source));
        return true;
    }

    /***
     * @return true if custom enchants can still be registered, i.e. NouveauEnchanting hasn't been bootstrapped yet.
     */
    public static synchronized boolean isAcceptingEnchantments() {
        return registry == null;
    }

    /***
     * Moves the enchants registered so far into NouveauEnchanting's registry. Called by NouveauEnchanting while
     * bootstrapping, plugins shouldn't call this.
     */
    public static synchronized void loadPendingEnchantments(CustomEnchantmentRegistry customEnchantments) {
        registry = customEnchantments;
        for (PendingEnchantment enchantment : pending) {
            customEnchantments.register(enchantment.enchantment(), enchantment.source());
        }
        if (!pending.isEmpty()) {
            logger.info("Loaded {} custom enchant(s) from other plugins", pending.size());
        }
        pending.clear();
    }
}
