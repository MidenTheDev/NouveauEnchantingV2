package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.compat.ItemsAdderCompat;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class InventoryClick implements Listener {

    private final int toolSlot = EnchantingGUI.toolSlot;
    private final int materialSlot = EnchantingGUI.materialSlot;
    private final int resultSlot = EnchantingGUI.resultSlot;



    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        //do enchanting stuff

        // If gui is NOT the custom enchanting GUI, exit.
        if (!(event.getInventory().getHolder() instanceof EnchantingGUI)) {
            return;
        }

        Inventory inv = event.getInventory();
        EnchantingGUI eGUI = (EnchantingGUI) inv.getHolder();
        // Check if the Tool Slot was clicked

        switch (event.getRawSlot()) {
            case toolSlot:

                // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
                if (event.getCurrentItem() != null) {clearResult(inv); return;}

                if (event.getCurrentItem() == null && inv.getItem(materialSlot) != null) {

                    tryEnchant(event.getCursor(), inv.getItem(materialSlot), inv, eGUI);

                }


                break;



            case materialSlot:

                // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
                if (event.getCurrentItem() != null) {clearResult(inv); return;}

                if (event.getCurrentItem() == null && inv.getItem(toolSlot) != null) {

                    tryEnchant(inv.getItem(toolSlot), event.getCursor(), inv, eGUI);

                }

                break;



            case resultSlot:

                // Prevent dupes/free enchants by verifying there is a tool and material present when trying to remove enchanted item.
                if (inv.getItem(toolSlot) == null || inv.getItem(materialSlot) == null) {
                    event.setCancelled(true);
                    clearResult(inv);
                }

                if (event.getCurrentItem() != null) {
                    ItemStack materialItem = inv.getItem(materialSlot);
                    int matCount = materialItem.getAmount();
                    Enchantment currentEnchant = eGUI.getEnchHandler().getLastEnchant();
                    // remove "minecraft:" namespace from ench name
                    String enchName = currentEnchant.getKey().asString().substring(10);
                    int resultEnchantLevel = event.getCurrentItem().getEnchantmentLevel(currentEnchant);
                    String materialName;
                    if (plugin.enabledCompats.contains("ItemsAdder")) {
                        ItemsAdderCompat iaComp = new ItemsAdderCompat();
                        if (iaComp.isCustomItem(materialItem)) {
                            materialName = iaComp.getItemID(materialItem);
                        } else {
                            materialName = materialItem.getType().name();
                        }
                    } else {
                        materialName = materialItem.getType().name();
                    }

                    int matRequirement = plugin.materialsConf.getInt(materialName+"."+enchName+"."+resultEnchantLevel+".material-cost");
                    int levelRequirement = plugin.materialsConf.getInt(materialName+"."+enchName+"."+resultEnchantLevel+".level-cost");
                    Player player = (Player) event.getWhoClicked();

                    if (matCount < matRequirement) {
                        // TODO: Send player message, not enough materials
                        event.setCancelled(true);
                        return;
                    }

                    if (player.getLevel() < levelRequirement) {
                        // TODO: Send player message, not enough levels
                        event.setCancelled(true);
                        return;
                    }

                    // Take required materials and levels, do not cancel event (let player take item)
                    inv.setItem(toolSlot, ItemStack.empty());
                    inv.getItem(materialSlot).subtract(matRequirement);
                    player.setLevel(player.getLevel()-levelRequirement);
                }


                break;

            default:
                if (event.getRawSlot()<27) {
                    event.setCancelled(true);
                }



        }


    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof EnchantingGUI) {
            e.setCancelled(true);
        }
    }

    private void clearResult(Inventory inv) {

        inv.clear(resultSlot);

    }

    private void tryEnchant(ItemStack toolItem, ItemStack materialItem, Inventory inv, EnchantingGUI eGUI) {
        // check if air and if it is, clear result
        if (toolItem.getType() == Material.AIR) {

            clearResult(inv);
            return;
        }
        // else, run enchant logic

        if (eGUI.getEnchHandler().isValidRecipe(toolItem, materialItem)) {

            inv.setItem(resultSlot,eGUI.getEnchHandler().getAppliedItem(toolItem, materialItem));
        }
    }
}
