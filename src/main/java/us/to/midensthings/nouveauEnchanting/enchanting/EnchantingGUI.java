package us.to.midensthings.nouveauEnchanting.enchanting;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.compat.CraftEngineCompat;
import us.to.midensthings.nouveauEnchanting.compat.ItemsAdderCompat;

import java.util.ArrayList;
import java.util.List;

public class EnchantingGUI implements InventoryHolder {
    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);
    FileConfiguration config = plugin.getConfig();
    private int toolSlot;
    private int materialSlot;
    private int resultSlot;
    private int costPreviewSlot;

    private final EnchHandler enchHandler;
    private final Inventory inv;
    private final ItemStack costPreviewItem;


    public EnchantingGUI() {

        // get slot numbers from config in case people want to set their own guis
        toolSlot = config.getInt("gui-info.tool-slot");
        materialSlot = config.getInt("gui-info.material-slot");
        resultSlot = config.getInt("gui-info.result-slot");
        costPreviewSlot = config.getInt("gui-info.cost-preview-slot");
        // Create 3X9 chest gui with custom name for fontimage gui
        inv = plugin.getServer().createInventory(this, 27, config.getString("gui-info.inventory-name", ":offset_-16::enchanting_window:"));
        enchHandler = new EnchHandler();
        // Load the cost preview item once instead of on every update
        costPreviewItem = loadConfigItem("gui-info.cost-preview-item", Material.ENCHANTED_BOOK);
        // Add starting Items
        if (config.getBoolean("gui-info.fill-inventory")) {
            ItemStack filler = loadConfigItem("gui-info.fill-inventory-material", Material.BLACK_STAINED_GLASS_PANE);
            // Hide the filler's tooltip so hovering over it shows nothing
            filler.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
            for (int i = 0;i<inv.getSize();i++) {
                if (isInputSlot(i) || i == costPreviewSlot || i == resultSlot) {
                    continue;
                }
                inv.setItem(i,filler);
            }
        }
        update();
    }

    /***
     * @return true if players can freely place and take items in this slot.
     */
    public boolean isInputSlot(int slot) {
        return slot == toolSlot || slot == materialSlot;
    }

    /***
     * Recalculates the result and cost preview from what's currently in the tool and material slots.
     * The result slot only ever shows a preview, the real item is made when the player clicks it.
     */
    public void update() {
        ItemStack tool = inv.getItem(toolSlot);
        ItemStack material = inv.getItem(materialSlot);

        if (!enchHandler.isValidRecipe(tool, material)) {
            inv.setItem(resultSlot, null);
            inv.setItem(costPreviewSlot,createGuiItem(
                    Component.text("No material entered").color(TextColor.color(Color.FUCHSIA.asRGB())),
                    Component.text("No material entered").color(TextColor.color(Color.AQUA.asRGB()))));
            return;
        }

        inv.setItem(resultSlot, enchHandler.getAppliedItem(tool));

        List<Component> extraLore = new ArrayList<>();
        // Check if enchant has a ServerLevels Requirement
        if (plugin.enabledCompats.contains("ServerLevels")) {
            String slReqPath = enchHandler.getSlRequirementsPath();
            // If there are ServerLevels requirements for this level
            if (slReqPath != null) {
                // Requires (system name) level (required level)
                extraLore.add(Component.text("Requires "+
                        plugin.materialsConf.getString(slReqPath+".system-name")+
                        " level "+
                        plugin.materialsConf.getInt(slReqPath+".level"),TextColor.color(Color.LIME.asRGB())));
            }
        }
        updateCostText(extraLore);
    }

    /***
     * Moves an item shift-clicked from the player's inventory into the material slot if it's an enchanting material,
     * or the tool slot otherwise.
     * @return what is left of the item after moving it, or null if all of it was moved.
     */
    public ItemStack insertFromPlayer(ItemStack item) {
        int slot = enchHandler.getCompatibleEnchantMaterial(item) != null ? materialSlot : toolSlot;

        ItemStack current = inv.getItem(slot);
        if (current == null || current.isEmpty()) {
            inv.setItem(slot, item.clone());
            return null;
        }
        if (!current.isSimilar(item)) {
            return item;
        }

        int moved = Math.min(current.getMaxStackSize() - current.getAmount(), item.getAmount());
        if (moved <= 0) {
            return item;
        }
        current.setAmount(current.getAmount() + moved);
        inv.setItem(slot, current);

        ItemStack leftover = item.clone();
        leftover.setAmount(item.getAmount() - moved);
        return leftover.getAmount() > 0 ? leftover : null;
    }

    /***
     * Uses up one tool and the material cost after the result has been taken.
     */
    public void consumeInputs(int materialCost) {
        subtract(toolSlot, 1);
        subtract(materialSlot, materialCost);
    }

    private void subtract(int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.isEmpty()) {
            return;
        }
        item.setAmount(item.getAmount() - amount);
        inv.setItem(slot, item.getAmount() > 0 ? item : null);
    }

    /***
     * Gives the items in the tool and material slots back to the player, dropping whatever doesn't fit, and clears the slots.
     */
    public void returnInputs(HumanEntity player) {
        for (int slot : new int[]{toolSlot, materialSlot}) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.isEmpty()) {
                inv.setItem(slot, null);
                player.getInventory().addItem(item).values().forEach(leftover ->
                        player.getWorld().dropItem(player.getLocation(), leftover));
            }
        }
        inv.setItem(resultSlot, null);
    }
    /***
     * Loads an item from a config section with a material and an optional custom-material section, see config.yml.
     * @param fallback the vanilla material to use if the configured item can't be found
     */
    private ItemStack loadConfigItem(String path, Material fallback) {
        String materialName = config.getString(path + ".material");

        if (config.getConfigurationSection(path + ".custom-material") != null) {
            String source = config.getString(path + ".custom-material.source");
            String itemId = config.getString(path + ".custom-material.namespace") + ":" + materialName;
            ItemStack item = null;
            if ("ItemsAdder".equalsIgnoreCase(source) && plugin.enabledCompats.contains("ItemsAdder")) {
                item = new ItemsAdderCompat().getItemStack(itemId);
            } else if ("CraftEngine".equalsIgnoreCase(source) && plugin.enabledCompats.contains("CraftEngine")) {
                item = new CraftEngineCompat().getItemStack(itemId);
            } else {
                plugin.getLogger().warning("Custom material source " + source + " for " + path + " in config.yml isn't installed or supported");
                return ItemStack.of(fallback);
            }
            if (item == null) {
                plugin.getLogger().warning("Could not find custom item " + itemId + " for " + path + " in config.yml");
                return ItemStack.of(fallback);
            }
            return item;
        }

        // Not custom item, load vanilla material from config.
        Material material = materialName == null ? null : Material.matchMaterial(materialName);
        if (material == null || !material.isItem()) {
            plugin.getLogger().warning("Invalid material " + materialName + " for " + path + " in config.yml");
            material = fallback;
        }
        return ItemStack.of(material);
    }

    protected ItemStack createGuiItem(Component name, Component lore) {
        ItemStack item = costPreviewItem.clone();

        final ItemMeta meta = item.getItemMeta();

        // Set the name of the item
        meta.customName(name);

        // Add the lore line
        List<Component> newLore = new ArrayList<>();
        newLore.add(lore);
        meta.lore(newLore);

        item.setItemMeta(meta);

        return item;
    }

    public EnchHandler getEnchHandler() {return enchHandler;}

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void updateCostText(List<Component> extraLore) {
        ItemMeta costPreviewMeta = inv.getItem(costPreviewSlot).getItemMeta();

        final Component levelCostComponent = Component.text("Cost: " + enchHandler.getCurrentEnchantLevelCost() + " levels").color(TextColor.color(Color.FUCHSIA.asRGB()));
        final Component matCostComponent = Component.text("Material Cost: " + enchHandler.getCurrentEnchantMaterialCost()).color(TextColor.color(Color.AQUA.asRGB()));

        costPreviewMeta.customName(levelCostComponent);
        List<Component> newLore = new ArrayList<>();

        newLore.add(matCostComponent);
        newLore.addAll(extraLore);

        costPreviewMeta.lore(newLore);

        inv.getItem(costPreviewSlot).setItemMeta(costPreviewMeta);

    }

    public int getToolSlot() {
        return toolSlot;
    }

    public int getMaterialSlot() {
        return materialSlot;
    }

    public int getResultSlot() {
        return resultSlot;
    }

    public int getCostPreviewSlot() {
        return costPreviewSlot;
    }
}
