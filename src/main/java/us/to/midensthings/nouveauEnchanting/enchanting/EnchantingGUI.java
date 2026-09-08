package us.to.midensthings.nouveauEnchanting.enchanting;

import org.bukkit.ChatColor;
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

    private void initializeItems() {
        inv.setItem(14,createGuiItem(Material.PAPER, ChatColor.LIGHT_PURPLE+"Cost: 0 levels",10001,ChatColor.BLUE+"No material entered."));

    }
    protected ItemStack createGuiItem(final Material material, final String name, int customModelData, String lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        // Set the name of the item
        meta.setDisplayName(name);

        // Set the custom model data
        meta.setCustomModelData(customModelData);

        // Add the lore line
        List<String> newLore = new ArrayList<>();
        newLore.add(lore);
        meta.setLore(newLore);

        item.setItemMeta(meta);

        return item;
    }

    public EnchHandler getEnchHandler() {return enchHandler;}

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
