package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class OpenEnchantingTable implements Listener {


    @EventHandler
    public void onEnchantingOpen(InventoryOpenEvent event) {

        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            Player player = (Player) event.getPlayer();
            // close inv
            event.setCancelled(true);
            // open custom one for player
            EnchantingGUI enchantingGUI = new EnchantingGUI();
            player.openInventory(enchantingGUI.getInventory());

        }

    }

}
