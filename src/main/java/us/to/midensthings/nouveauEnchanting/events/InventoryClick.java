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
import us.to.midensthings.nouveauEnchanting.util.Schedulers;

public class InventoryClick implements Listener {

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        // If gui is NOT the custom enchanting GUI, exit.
        if (!(top.getHolder(false) instanceof EnchantingGUI eGUI) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Double clicking gathers matching items from every slot, including the decorations and the result
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        if (clicked != top) {
            // Shift clicking would put the item in the first open slot, so send it to the tool or material slot instead
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                ItemStack item = event.getCurrentItem();
                if (item != null && !item.isEmpty()) {
                    event.setCurrentItem(eGUI.insertFromPlayer(item));
                    eGUI.update();
                }
            }
            return;
        }

        if (eGUI.isInputSlot(event.getSlot())) {
            // Let the click happen like a normal inventory, then update the result once the slot has changed
            Schedulers.runNextTick(player, eGUI::update);
            return;
        }

        // Everything else in the gui is controlled by the gui
        event.setCancelled(true);
        if (event.getSlot() == eGUI.getResultSlot()) {
            takeResult(event, player, eGUI);
        }
    }

    private void takeResult(InventoryClickEvent event, Player player, EnchantingGUI eGUI) {
        Inventory inv = eGUI.getInventory();
        ItemStack toolItem = inv.getItem(eGUI.getToolSlot());
        ItemStack materialItem = inv.getItem(eGUI.getMaterialSlot());
        EnchHandler eHandler = eGUI.getEnchHandler();

        // Check the recipe against what's actually in the tool and material slots, not what the preview showed.
        if (!eHandler.isValidRecipe(toolItem, materialItem)) {
            eGUI.update();
            return;
        }

        int matRequirement = eHandler.getCurrentEnchantMaterialCost();
        int levelRequirement = eHandler.getCurrentEnchantLevelCost();

        // You have not enough minerals
        if (materialItem.getAmount() < matRequirement) {
            player.sendMessage(Component.text("You do not have enough materials!").color(TextColor.color(Color.RED.asRGB())));
            return;
        }

        // Not enough levels
        if (player.getLevel() < levelRequirement) {
            player.sendMessage(Component.text("You do not have enough levels!").color(TextColor.color(Color.RED.asRGB())));
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
                    return;
                }
            }
        }

        // Get the material before it's used up
        EnchantMaterial enchantMaterial = eHandler.getCompatibleEnchantMaterial(materialItem);

        if (!giveResult(event, player, eHandler.getAppliedItem(toolItem))) {
            return;
        }

        // Take required materials and levels
        eGUI.consumeInputs(matRequirement);
        player.setLevel(player.getLevel() - levelRequirement);

        // Call the player enchant event
        PlayerEnchantEvent playerEnchantEvent = new PlayerEnchantEvent(player,eHandler.getLastEnchant(),enchantMaterial,matRequirement,levelRequirement,eHandler.getCurrentEnchantLevel());
        playerEnchantEvent.callEvent();

        if (plugin.getConfig().getBoolean("play-sound-on-enchant")) {
            player.playSound(Sound.sound(Key.key("block.enchantment_table.use"), Sound.Source.UI, 0.5F, 1F), Sound.Emitter.self());
        }

        eGUI.update();
    }

    /***
     * Puts the enchanted item on the player's cursor, or in their inventory when shift clicking.
     * @return true if the player received the item.
     */
    private boolean giveResult(InventoryClickEvent event, Player player, ItemStack result) {
        switch (event.getClick()) {
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                player.getInventory().addItem(result).values().forEach(leftover ->
                        player.getWorld().dropItem(player.getLocation(), leftover));
                return true;
            }
            case LEFT, RIGHT -> {
                ItemStack cursor = event.getView().getCursor();
                if (cursor.isEmpty()) {
                    event.getView().setCursor(result);
                    return true;
                }
                if (cursor.isSimilar(result) && cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize()) {
                    cursor.setAmount(cursor.getAmount() + result.getAmount());
                    event.getView().setCursor(cursor);
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof EnchantingGUI eGUI)) {
            return;
        }

        // Only allow dragging across the tool and material slots (and the player's own inventory)
        boolean touchesGui = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= top.getSize()) {
                continue;
            }
            if (!eGUI.isInputSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
            touchesGui = true;
        }
        if (touchesGui && event.getWhoClicked() instanceof Player player) {
            Schedulers.runNextTick(player, eGUI::update);
        }
    }
}
