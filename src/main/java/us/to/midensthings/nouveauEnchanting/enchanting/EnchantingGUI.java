package us.to.midensthings.nouveauEnchanting.enchanting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
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


    public EnchantingGUI() {

        // get slot numbers from config in case people want to set their own guis
        toolSlot = config.getInt("gui-info.tool-slot");
        materialSlot = config.getInt("gui-info.material-slot");
        resultSlot = config.getInt("gui-info.result-slot");
        costPreviewSlot = config.getInt("gui-info.cost-preview-slot");
        // Create 3X9 chest gui with custom name for fontimage gui
        inv = plugin.getServer().createInventory(this, 27, config.getString("gui-info.inventory-name", ":offset_-16::enchanting_window:"));
        enchHandler = new EnchHandler();
        // Add starting Items
        initializeItems();
    }

    public void initializeItems() {

        inv.setItem(costPreviewSlot,createGuiItem(
                Component.text("No material entered").color(TextColor.color(Color.FUCHSIA.asRGB())),
                Component.text("No material entered").color(TextColor.color(Color.AQUA.asRGB()))));
        if (config.getBoolean("gui-info.fill-inventory")) {
            ItemStack pane = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE);
            for (int i = 0;i<27;i++) {
                if (i == toolSlot || i == materialSlot || i == costPreviewSlot || i == resultSlot) {
                    continue;
                }
                inv.setItem(i,pane);
            }
        }

    }
    protected ItemStack createGuiItem(Component name, Component lore) {
        ItemStack item;
        if (config.getConfigurationSection("gui-info.cost-preview-item.custom-material") != null) {
            if (config.getString("gui-info.cost-preview-item.custom-material.source").equalsIgnoreCase("ItemsAdder")) {
                if (plugin.enabledCompats.contains("ItemsAdder")) {
                    ItemsAdderCompat iacomp = new ItemsAdderCompat();
                    item = iacomp.getItemStack(config.getString("gui-info.cost-preview-item.custom-material.namespace")+":"+config.getString("gui-info.cost-preview-item.material"));
                } else {
                    // error in config somewhere, default to book
                    item = ItemStack.of(Material.ENCHANTED_BOOK);
                }
            } else {
                // error in config somewhere, default to book
                item = ItemStack.of(Material.ENCHANTED_BOOK);
            }
        } else {
            // Not custom item, load vanilla material from config.
            item = ItemStack.of(Material.valueOf(config.getString("gui-info.cost-preview-item.material")));
        }

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

    public void updateCostText() {
        ItemMeta costPreviewMeta = inv.getItem(costPreviewSlot).getItemMeta();

        final Component levelCostComponent = Component.text("Cost: " + enchHandler.getCurrentEnchantLevelCost() + " levels").color(TextColor.color(Color.FUCHSIA.asRGB()));
        final Component matCostComponent = Component.text("Material Cost: " + enchHandler.getCurrentEnchantMaterialCost()).color(TextColor.color(Color.AQUA.asRGB()));

        costPreviewMeta.customName(levelCostComponent);
        List<Component> newLore = new ArrayList<>();

        newLore.add(matCostComponent);

        costPreviewMeta.lore(newLore);

        inv.getItem(costPreviewSlot).setItemMeta(costPreviewMeta);

    }

    public void updateCostText(List<Component> extraLore) {
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
