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
            EnchantingGUI eGUI = (EnchantingGUI) inv.getHolder();
            if (inv.getItem(eGUI.getToolSlot()) != null) {

                world.dropItem(event.getPlayer().getLocation(), inv.getItem(eGUI.getToolSlot()));
            }
            if (inv.getItem(eGUI.getMaterialSlot()) != null) {

                world.dropItem(event.getPlayer().getLocation(), inv.getItem(eGUI.getMaterialSlot()));
            }

        }
    }


}
