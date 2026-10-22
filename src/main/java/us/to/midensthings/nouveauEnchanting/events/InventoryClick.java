package us.to.midensthings.nouveauEnchanting.events;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.compat.ServerLevelsCompat;
import us.to.midensthings.nouveauEnchanting.customevents.PlayerEnchantEvent;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchHandler;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;

import java.util.ArrayList;
import java.util.List;

public class InventoryClick implements Listener {

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        //do enchanting stuff

        // If gui is NOT the custom enchanting GUI, exit.
        if (!(event.getInventory().getHolder() instanceof EnchantingGUI eGUI)) {
            return;
        }

        // Shift clicks and double clicks move items in and out of the gui without going through the slot checks below
        if (event.isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        Inventory inv = event.getInventory();

        final int materialSlot = eGUI.getMaterialSlot();
        final int toolSlot = eGUI.getToolSlot();
        final int resultSlot = eGUI.getResultSlot();

        // Check if the Tool Slot was clicked
        if (event.getRawSlot() == toolSlot) {


            // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
            if (event.getCurrentItem() != null) {
                clearResult(inv, resultSlot);
                eGUI.initializeItems();
                return;
            }

            if (inv.getItem(materialSlot) != null) {

                // Right clicking only places one item from the cursor
                ItemStack placedTool = event.getAction() == InventoryAction.PLACE_ONE ? event.getCursor().asOne() : event.getCursor();
                tryEnchant(placedTool, inv.getItem(materialSlot), inv, eGUI);

            }


        } else if (event.getRawSlot() == materialSlot) {

                // If player swaps items in material slot or removes item in slot, clear result (Dupe prevention)
                if (event.getCurrentItem() != null) {
                    clearResult(inv, resultSlot);
                    eGUI.initializeItems();
                    return;
                }

                if (inv.getItem(toolSlot) != null) {

                    tryEnchant(inv.getItem(toolSlot), event.getCursor(), inv, eGUI);

                }

        } else if (event.getRawSlot() == resultSlot) {

                handleResultClick(event, inv, eGUI);

        } else {

                if (event.getRawSlot() < inv.getSize()) {
                    event.setCancelled(true);
                }

        }


    }

    private void handleResultClick(InventoryClickEvent event, Inventory inv, EnchantingGUI eGUI) {
        final int materialSlot = eGUI.getMaterialSlot();
        final int toolSlot = eGUI.getToolSlot();
        final int resultSlot = eGUI.getResultSlot();

        // Only allow taking the result out with an empty cursor.
        // Swapping or placing would put the player's own item into the result slot.
        if (event.getCurrentItem() == null || !isTakeAction(event)) {
            event.setCancelled(true);
            return;
        }

        ItemStack toolItem = inv.getItem(toolSlot);
        ItemStack materialItem = inv.getItem(materialSlot);
        EnchHandler eHandler = eGUI.getEnchHandler();

        // Prevent dupes/free enchants by re-checking the recipe against what's actually in the tool and material slots.
        if (!eHandler.isValidRecipe(toolItem, materialItem)) {
            event.setCancelled(true);
            clearResult(inv, resultSlot);
            eGUI.initializeItems();
            return;
        }

        int matRequirement = eHandler.getCurrentEnchantMaterialCost();
        int levelRequirement = eHandler.getCurrentEnchantLevelCost();
        Player player = (Player) event.getWhoClicked();

        // You have not enough minerals
        if (materialItem.getAmount() < matRequirement) {
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

        // Check if enchant has an SL requirement
        if (plugin.enabledCompats.contains("ServerLevels")) {
            String slReqPath = eHandler.getSlRequirementsPath();
            // If there are ServerLevels requirements for this level
            if (slReqPath != null) {
                // Check if player's current level is at or above the minimum
                ServerLevelsCompat slCompat = new ServerLevelsCompat();
                int playerLevel = slCompat.getPlayerLevel(player,plugin.materialsConf.getString(slReqPath+".system-name"));
                int requiredLevel = plugin.materialsConf.getInt(slReqPath+".level");
                if (playerLevel < requiredLevel) {
                    Component msg = Component.text("Your enchanting level is too low! Current level: "+playerLevel+" Required level: "+requiredLevel, TextColor.color(Color.RED.asRGB()));
                    if (plugin.materialsConf.getString(slReqPath+".low-level-msg") != null) {
                        msg = MiniMessage.miniMessage().deserialize(plugin.materialsConf.getString(slReqPath+".low-level-msg").replace("%level%",String.valueOf(playerLevel)));
                    }
                    player.sendMessage(msg);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Get the material before it's used up
        EnchantMaterial enchantMaterial = eHandler.getCompatibleEnchantMaterial(materialItem);

        // Take required materials and levels, do not cancel event (let player take item)
        inv.setItem(toolSlot, ItemStack.empty());
        materialItem.subtract(matRequirement);
        player.setLevel(player.getLevel() - levelRequirement);

        // Call the player enchant event
        PlayerEnchantEvent playerEnchantEvent = new PlayerEnchantEvent(player,eHandler.getLastEnchant(),enchantMaterial,matRequirement,levelRequirement,eHandler.getCurrentEnchantLevel());
        playerEnchantEvent.callEvent();

        if (plugin.getConfig().getBoolean("play-sound-on-enchant")) {
            player.playSound(Sound.sound(Key.key("block.enchantment_table.use"), Sound.Source.UI, 0.5F, 1F), Sound.Emitter.self());
        }

        eGUI.initializeItems();
    }

    private boolean isTakeAction(InventoryClickEvent event) {
        if (!event.getCursor().isEmpty()) {
            return false;
        }
        return switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, DROP_ALL_SLOT, DROP_ONE_SLOT -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof EnchantingGUI) {
            e.setCancelled(true);
        }
    }

    private void clearResult(Inventory inv, int resultSlot) {

        inv.clear(resultSlot);

    }

    private void tryEnchant(ItemStack toolItem, ItemStack materialItem, Inventory inv, EnchantingGUI eGUI) {
        EnchHandler eHandler = eGUI.getEnchHandler();
        // If there's no valid enchant (including an empty tool or material), clear result
        if (!eHandler.isValidRecipe(toolItem, materialItem)) {

            clearResult(inv, eGUI.getResultSlot());
            eGUI.initializeItems();
            return;
        }
        // else, show the result
        inv.setItem(eGUI.getResultSlot(), eHandler.getAppliedItem(toolItem));

        // Check if enchant has a ServerLevels Requirement
        if (plugin.enabledCompats.contains("ServerLevels")) {
            String slReqPath = eHandler.getSlRequirementsPath();
            // If there are ServerLevels requirements for this level
            if (slReqPath != null) {
                // Requires (system name) level (required level)
                Component lvlReq = Component.text("Requires "+
                        plugin.materialsConf.getString(slReqPath+".system-name")+
                        " level "+
                        plugin.materialsConf.getInt(slReqPath+".level"),TextColor.color(Color.LIME.asRGB()));
                List<Component> extraLore = new ArrayList<>();
                extraLore.add(lvlReq);
                eGUI.updateCostText(extraLore);
                return;
            }
        }

        // Update cost preview
        eGUI.updateCostText();
    }
}
