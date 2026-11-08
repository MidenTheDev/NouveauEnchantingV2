package us.to.midensthings.nouveauEnchanting.events;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;
import us.to.midensthings.nouveauEnchanting.customevents.ItemDisenchantEvent;
import us.to.midensthings.nouveauEnchanting.customevents.ItemEnchantEvent;
import us.to.midensthings.nouveauEnchanting.util.Schedulers;

import java.util.HashMap;
import java.util.Map;

/***
 * Calls ItemEnchantEvent and ItemDisenchantEvent for the vanilla ways of enchanting and disenchanting, so other plugins
 * only need to listen to those two events. NouveauEnchanting's own enchanting gui calls them itself.
 * <p>
 * Runs at HIGH priority so it only calls the events for enchanting that other plugins haven't already cancelled.
 */
public class EnchantEventBridge implements Listener {

    // The result slot of anvils and grindstones
    private static final int RESULT_SLOT = 2;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEnchantingTable(EnchantItemEvent e) {
        ItemStack item = e.getItem().clone();
        // What vanilla will turn the item into, books become enchanted books with the enchants stored in them
        ItemStack expected = applyEnchants(item, e.getEnchantsToAdd());

        ItemEnchantEvent event = new ItemEnchantEvent(e.getEnchanter(), item, expected.clone(), new HashMap<>(e.getEnchantsToAdd()),
                ItemEnchantEvent.Source.ENCHANTING_TABLE);
        if (!event.callEvent()) {
            e.setCancelled(true);
            return;
        }

        // Vanilla enchants the item after this event, so swap in the changed result once it's done
        ItemStack result = event.getResult();
        if (!result.equals(expected)) {
            Player player = e.getEnchanter();
            Inventory inventory = e.getInventory();
            Schedulers.runNextTick(player, () -> {
                ItemStack enchanted = inventory.getItem(0);
                if (enchanted != null && enchanted.getType() == expected.getType() && enchanted.getAmount() == expected.getAmount()) {
                    inventory.setItem(0, result);
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onAnvilTake(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory() instanceof AnvilInventory anvil) || !takesResult(e)) {
            return;
        }
        // Only call the event if the anvil will actually let the player take the result
        if (e.getView() instanceof AnvilView view && !canPayAnvil(e.getWhoClicked(), view)) {
            return;
        }

        ItemStack item = anvil.getItem(0);
        ItemStack result = e.getCurrentItem();
        if (item == null || item.isEmpty()) {
            return;
        }

        // Enchants the result has at a higher level than the item, renaming and repairing alone don't count
        Map<Enchantment, Integer> before = getEnchants(item);
        Map<Enchantment, Integer> gained = new HashMap<>();
        getEnchants(result).forEach((enchantment, level) -> {
            if (level > before.getOrDefault(enchantment, 0)) {
                gained.put(enchantment, level);
            }
        });
        if (gained.isEmpty()) {
            return;
        }

        ItemEnchantEvent event = new ItemEnchantEvent(e.getWhoClicked(), item.clone(), result.clone(), gained, ItemEnchantEvent.Source.ANVIL);
        if (!event.callEvent()) {
            e.setCancelled(true);
            return;
        }
        if (!event.getResult().equals(result)) {
            e.setCurrentItem(event.getResult());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onGrindstoneTake(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory() instanceof GrindstoneInventory grindstone) || !takesResult(e)) {
            return;
        }

        ItemStack first = grindstone.getItem(0);
        ItemStack second = grindstone.getItem(1);
        ItemStack result = e.getCurrentItem();

        // Every enchant on the items going in, at the highest level either had
        Map<Enchantment, Integer> before = new HashMap<>();
        getEnchants(first).forEach((enchantment, level) -> before.merge(enchantment, level, Math::max));
        getEnchants(second).forEach((enchantment, level) -> before.merge(enchantment, level, Math::max));

        // Enchants the result doesn't keep (grindstones keep curses)
        Map<Enchantment, Integer> resultEnchants = getEnchants(result);
        Map<Enchantment, Integer> removed = new HashMap<>();
        before.forEach((enchantment, level) -> {
            if (resultEnchants.getOrDefault(enchantment, 0) < level) {
                removed.put(enchantment, level);
            }
        });
        if (removed.isEmpty()) {
            return;
        }

        ItemStack item = first != null && !first.isEmpty() ? first : second;
        ItemDisenchantEvent event = new ItemDisenchantEvent(e.getWhoClicked(), item.clone(), result.clone(), removed, ItemDisenchantEvent.Source.GRINDSTONE);
        if (!event.callEvent()) {
            e.setCancelled(true);
            return;
        }
        if (!event.getResult().equals(result)) {
            e.setCurrentItem(event.getResult());
        }
    }

    /***
     * @return true if the click takes the item out of an anvil's or grindstone's result slot.
     * Checks the obvious cases where vanilla won't let it happen, so the events aren't called for nothing.
     */
    private boolean takesResult(InventoryClickEvent e) {
        if (e.getRawSlot() != RESULT_SLOT || e.getCurrentItem() == null || e.getCurrentItem().isEmpty()) {
            return false;
        }
        return switch (e.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, DROP_ALL_SLOT, DROP_ONE_SLOT -> true;
            // Shift clicking needs room in the player's inventory
            case MOVE_TO_OTHER_INVENTORY -> e.getWhoClicked().getInventory().firstEmpty() != -1;
            // Number keys need an empty hotbar slot
            case HOTBAR_SWAP -> {
                ItemStack hotbarItem = e.getHotbarButton() >= 0 ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : null;
                yield hotbarItem == null || hotbarItem.isEmpty();
            }
            default -> false;
        };
    }

    /***
     * @return true if the player can pay the anvil's level cost, the same check vanilla does before letting them take the result.
     */
    private boolean canPayAnvil(HumanEntity player, AnvilView view) {
        int cost = view.getRepairCost();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return cost > 0;
        }
        return cost > 0 && cost < view.getMaximumRepairCost() && player instanceof Player p && p.getLevel() >= cost;
    }

    /***
     * @return the item's enchants, or the enchants stored in it for enchanted books.
     */
    private static Map<Enchantment, Integer> getEnchants(ItemStack item) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        if (item == null || item.isEmpty()) {
            return enchants;
        }
        enchants.putAll(item.getEnchantments());
        if (item.getItemMeta() instanceof EnchantmentStorageMeta storage) {
            enchants.putAll(storage.getStoredEnchants());
        }
        return enchants;
    }

    /***
     * @return a copy of the item with the enchants added the way the enchanting table adds them.
     */
    private static ItemStack applyEnchants(ItemStack item, Map<Enchantment, Integer> enchants) {
        if (item.getType() == Material.BOOK) {
            ItemStack book = item.withType(Material.ENCHANTED_BOOK);
            if (book.getItemMeta() instanceof EnchantmentStorageMeta storage) {
                enchants.forEach((enchantment, level) -> storage.addStoredEnchant(enchantment, level, true));
                book.setItemMeta(storage);
            }
            return book;
        }
        ItemStack enchanted = item.clone();
        enchanted.addUnsafeEnchantments(enchants);
        return enchanted;
    }
}
