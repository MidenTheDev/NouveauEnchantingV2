package us.to.midensthings.nouveauEnchanting.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
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
import us.to.midensthings.nouveauEnchanting.enchanting.EnchHandler;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

public class InventoryClick implements Listener {

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

        final int materialSlot = EnchantingGUI.materialSlot;
        final int toolSlot = EnchantingGUI.toolSlot;
        switch (event.getRawSlot()) {
            case toolSlot:

                // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
                if (event.getCurrentItem() != null) {clearResult(inv); eGUI.initializeItems(); return;}

                if (event.getCurrentItem() == null && inv.getItem(materialSlot) != null) {

                    tryEnchant(event.getCursor(), inv.getItem(materialSlot), inv, eGUI);

                }


                break;



            case materialSlot:

                // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
                if (event.getCurrentItem() != null) {clearResult(inv); eGUI.initializeItems(); return;}

                if (event.getCurrentItem() == null && inv.getItem(toolSlot) != null) {

                    tryEnchant(inv.getItem(toolSlot), event.getCursor(), inv, eGUI);

                }

                break;



            case resultSlot:

                // Prevent dupes/free enchants by verifying there is a tool and material present when trying to remove enchanted item.
                if (inv.getItem(toolSlot) == null || inv.getItem(materialSlot) == null) {
                    event.setCancelled(true);
                    clearResult(inv);
                    eGUI.initializeItems();
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

                    // You have not enough minerals
                    if (matCount < matRequirement) {
                        player.sendMessage(Component.text("You do not have enough materials!").color(TextColor.color(Color.RED.asRGB())));
                        event.setCancelled(true);
                        return;
                    }

                    // Not enough levels
                    if (player.getLevel() < levelRequirement) {
                        player.sendMessage(Component.text("You do not have enough levels!").color(TextColor.color(Color.RED.asRGB())));
                        event.setCancelled(true);
                        return;
                    }

                    // Take required materials and levels, do not cancel event (let player take item)
                    inv.setItem(toolSlot, ItemStack.empty());
                    inv.getItem(materialSlot).subtract(matRequirement);
                    player.setLevel(player.getLevel()-levelRequirement);
                    eGUI.initializeItems();
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
            eGUI.initializeItems();
            return;
        }
        // else, run enchant logic
        EnchHandler eHandler = eGUI.getEnchHandler();
        if (eHandler.isValidRecipe(toolItem, materialItem)) {

            inv.setItem(resultSlot,eHandler.getAppliedItem(toolItem, materialItem));

            // Update cost preview
            int matCost = eHandler.getCurrentEnchantMaterialCost();
            int levelCost = eHandler.getCurrentEnchantLevelCost();
            eGUI.updateCostText(matCost,levelCost);
        }
    }
}
