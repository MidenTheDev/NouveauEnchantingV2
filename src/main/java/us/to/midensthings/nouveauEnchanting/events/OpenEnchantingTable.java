package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;
import us.to.midensthings.nouveauEnchanting.util.Schedulers;

public class OpenEnchantingTable implements Listener {

    @EventHandler
    public void onEnchantingOpen(InventoryOpenEvent event) {

        if (event.getInventory().getType() == InventoryType.ENCHANTING && event.getPlayer() instanceof Player player) {
            // close inv
            event.setCancelled(true);
            // open custom one for player on the next tick, opening an inventory inside the open event isn't reliable
            Schedulers.runNextTick(player, () -> player.openInventory(new EnchantingGUI().getInventory()));

        }

    }

}
