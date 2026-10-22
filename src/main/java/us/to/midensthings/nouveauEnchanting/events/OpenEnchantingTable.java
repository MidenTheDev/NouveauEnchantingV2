package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class OpenEnchantingTable implements Listener {

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    @EventHandler
    public void onEnchantingOpen(InventoryOpenEvent event) {

        if (event.getInventory().getType() == InventoryType.ENCHANTING && event.getPlayer() instanceof Player player) {
            // close inv
            event.setCancelled(true);
            // open custom one for player on the next tick, opening an inventory inside the open event isn't reliable
            player.getScheduler().run(plugin, task -> player.openInventory(new EnchantingGUI().getInventory()), null);

        }

    }

}
