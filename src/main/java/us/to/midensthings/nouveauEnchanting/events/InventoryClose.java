package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class InventoryClose implements Listener {


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnchantingGUI eGUI) {
            Inventory inv = event.getInventory();
            returnItem(event.getPlayer(), inv, eGUI.getToolSlot());
            returnItem(event.getPlayer(), inv, eGUI.getMaterialSlot());
        }
    }

    // Give the item back to the player, dropping whatever doesn't fit in their inventory
    private void returnItem(HumanEntity player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null) {
            return;
        }
        inv.clear(slot);
        player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItem(player.getLocation(), leftover));
    }


}
