package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class InventoryClose implements Listener {


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnchantingGUI) {
            World world = event.getPlayer().getWorld();
            Inventory inv = event.getInventory();
            if (inv.getItem(EnchantingGUI.toolSlot) != null) {

                world.dropItem(event.getPlayer().getLocation(), inv.getItem(EnchantingGUI.toolSlot));
            }
            if (inv.getItem(EnchantingGUI.materialSlot) != null) {

                world.dropItem(event.getPlayer().getLocation(), inv.getItem(EnchantingGUI.materialSlot));
            }

        }
    }


}
