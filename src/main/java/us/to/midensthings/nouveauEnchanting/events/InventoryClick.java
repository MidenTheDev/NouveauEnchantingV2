package us.to.midensthings.nouveauEnchanting.events;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.compat.ItemsAdderCompat;
import us.to.midensthings.nouveauEnchanting.compat.ServerLevelsCompat;
import us.to.midensthings.nouveauEnchanting.customevents.PlayerEnchantEvent;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchHandler;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;
import us.to.midensthings.serverLevels.ServerLevels;

import java.util.ArrayList;
import java.util.List;

public class InventoryClick implements Listener {

    private int resultSlot;



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

        final int materialSlot = eGUI.getMaterialSlot();
        final int toolSlot = eGUI.getToolSlot();
        resultSlot  = eGUI.getResultSlot();
        if (event.getRawSlot() == toolSlot) {


            // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
            if (event.getCurrentItem() != null) {
                clearResult(inv);
                eGUI.initializeItems();
                return;
            }

            if (event.getCurrentItem() == null && inv.getItem(materialSlot) != null) {

                tryEnchant(event.getCursor(), inv.getItem(materialSlot), inv, eGUI);

            }


        } else if (event.getRawSlot() == materialSlot) {

                // If player swaps items in tool slot or removes item in slot, clear result (Dupe prevention)
                if (event.getCurrentItem() != null) {
                    clearResult(inv);
                    eGUI.initializeItems();
                    return;
                }

                if (event.getCurrentItem() == null && inv.getItem(toolSlot) != null) {

                    tryEnchant(inv.getItem(toolSlot), event.getCursor(), inv, eGUI);

                }

        } else if (event.getRawSlot() == resultSlot) {

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

                    int matRequirement = plugin.materialsConf.getInt(materialName + "." + enchName + "." + resultEnchantLevel + ".material-cost");
                    if (matRequirement == 0) {
                        matRequirement = plugin.materialsConf.getInt(materialName.toLowerCase() + "." + enchName + "." + resultEnchantLevel + ".material-cost");
                    }
                    int levelRequirement = plugin.materialsConf.getInt(materialName + "." + enchName + "." + resultEnchantLevel + ".level-cost");
                    if (levelRequirement == 0) {
                        levelRequirement = plugin.materialsConf.getInt(materialName.toLowerCase() + "." + enchName + "." + resultEnchantLevel + ".level-cost");
                    }
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

                    // Check if enchant has an SL requirement
                    if (plugin.enabledCompats.contains("ServerLevels")) {
                        String slReqPath = materialName+"."+
                                currentEnchant.getKey().asString().substring(10)+"."+
                                resultEnchantLevel+"."+
                                "sl-requirements";
                        ConfigurationSection configurationSection = plugin.materialsConf.getConfigurationSection(slReqPath);
                        if (configurationSection == null) {
                            slReqPath = materialName.toLowerCase()+"."+
                                    currentEnchant.getKey().asString().substring(10)+"."+
                                    resultEnchantLevel+"."+
                                    "sl-requirements";
                            configurationSection = plugin.materialsConf.getConfigurationSection(slReqPath);
                        }
                        // If there are ServerLevels requirements for this level
                        if (configurationSection != null) {
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

                    // Take required materials and levels, do not cancel event (let player take item)
                    inv.setItem(toolSlot, ItemStack.empty());
                    inv.getItem(materialSlot).subtract(matRequirement);
                    player.setLevel(player.getLevel() - levelRequirement);

                    String customMaterialSource = plugin.materialsConf.getString(materialName+".custom-material.source");
                    EnchantMaterial enchantMaterial;
                    if (customMaterialSource != null) {
                        if (customMaterialSource.equalsIgnoreCase("itemsadder")) {
                            // Get the namespace and material name to make retrieving the item via the API easier later
                            String namespace = plugin.materialsConf.getString(materialName + ".custom-material.namespace");
                            enchantMaterial = plugin.materialRegistry.getMaterial(namespace + ":" + materialName);
                        } else {
                            enchantMaterial = plugin.materialRegistry.getMaterial(materialName);
                        }
                    } else {
                        enchantMaterial = plugin.materialRegistry.getMaterial(materialName);
                    }


                    PlayerEnchantEvent playerEnchantEvent = new PlayerEnchantEvent(player,currentEnchant,enchantMaterial,matRequirement,levelRequirement);
                    playerEnchantEvent.callEvent();

                    if (plugin.getConfig().getBoolean("play-sound-on-enchant")) {
                        player.playSound(Sound.sound(Key.key("block.enchantment_table.use"), Sound.Source.UI, 0.5F, 1F), Sound.Emitter.self());
                    }

                    eGUI.initializeItems();
                }


        } else {

                if (event.getRawSlot()<27) {
                    event.setCancelled(true);
                }
                if (event.getClick() == ClickType.SHIFT_LEFT) {
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
            ItemStack resultItem = eHandler.getAppliedItem(toolItem, materialItem);
            inv.setItem(resultSlot,resultItem);

            // Check if enchant has a ServerLevels Requirement
            if (plugin.enabledCompats.contains("ServerLevels")) {
                String slReqPath = eHandler.getCompatibleEnchantMaterial(materialItem).getMaterialName()+"."+
                        eHandler.getLastEnchant().getKey().asString().substring(10)+"."+
                        resultItem.getEnchantmentLevel(eHandler.getLastEnchant())+"."+
                        "sl-requirements";
                // If there are ServerLevels requirements for this level
                if (plugin.materialsConf.getConfigurationSection(slReqPath) != null) {
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
}
