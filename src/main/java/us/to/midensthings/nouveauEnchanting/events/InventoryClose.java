package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class InventoryClose implements Listener {


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof EnchantingGUI eGUI) {
            eGUI.returnInputs(event.getPlayer());
        }
    }


}
