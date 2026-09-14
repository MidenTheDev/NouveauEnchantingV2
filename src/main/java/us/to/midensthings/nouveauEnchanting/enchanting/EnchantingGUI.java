package us.to.midensthings.nouveauEnchanting.enchanting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

import java.util.ArrayList;
import java.util.List;

public class EnchantingGUI implements InventoryHolder {
    public static final int toolSlot = 10;
    public static final int materialSlot = 12;
    public static final int resultSlot = 16;
    public static final int costPreviewSlot = 14;

    private final EnchHandler enchHandler;
    private final Inventory inv;
    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    public EnchantingGUI() {
        // Create 3X9 chest gui with custom name for fontimage gui
        inv = plugin.getServer().createInventory(this, 27, ":offset_-16::enchanting_window:");
        enchHandler = new EnchHandler();
        // Add starting Items
        initializeItems();
    }

    public void initializeItems() {
        inv.setItem(costPreviewSlot,createGuiItem(Material.PAPER,
                Component.text("No material entered").color(TextColor.color(Color.FUCHSIA.asRGB())),
                10001,
                Component.text("No material entered").color(TextColor.color(Color.AQUA.asRGB()))));

    }
    protected ItemStack createGuiItem(final Material material, final Component name, int customModelData, Component lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        // Set the name of the item
        meta.customName(name);

        // Set the custom model data
        meta.setCustomModelData(customModelData);

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

    public void updateCostText(int matCost, int levelCost) {
        ItemMeta costPreviewMeta = inv.getItem(costPreviewSlot).getItemMeta();

        final Component levelCostComponent = Component.text("Cost: " + levelCost + " levels").color(TextColor.color(Color.FUCHSIA.asRGB()));
        final Component matCostComponent = Component.text("Material Cost: " + matCost).color(TextColor.color(Color.AQUA.asRGB()));

        costPreviewMeta.customName(levelCostComponent);
        List<Component> newLore = new ArrayList<>();

        newLore.add(matCostComponent);
        costPreviewMeta.lore(newLore);

        inv.getItem(costPreviewSlot).setItemMeta(costPreviewMeta);


    }
}
