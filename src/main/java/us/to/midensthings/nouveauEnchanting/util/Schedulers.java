package us.to.midensthings.nouveauEnchanting.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

/***
 * Runs tasks on the right thread on both Paper and Folia. On Folia players are owned by the thread of the region
 * they're in, so player tasks go through the player's scheduler. On Paper this runs on the main thread.
 */
public final class Schedulers {
    private static final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    private Schedulers() {
    }

    /***
     * Runs the task on the thread that owns the player, right away if this is already that thread.
     * Does nothing if the player logs out before it runs.
     */
    public static void runFor(Player player, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            task.run();
        } else {
            runNextTick(player, task);
        }
    }

    /***
     * Runs the task on the player's thread next tick, e.g. to open an inventory or update a GUI after a click has finished.
     * Does nothing if the player logs out before it runs.
     */
    public static void runNextTick(Player player, Runnable task) {
        player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }
}
