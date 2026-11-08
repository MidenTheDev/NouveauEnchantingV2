package us.to.midensthings.nouveauEnchanting.customevents;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Map;

/***
 * Called when a player is about to enchant an item, from any source: NouveauEnchanting's enchanting gui, the vanilla
 * enchanting table and anvils. Listen to this instead of each source's own event.
 * <p>
 * The result can be changed (e.g. to add data to the item) or the enchanting cancelled. Enchants that are only upgraded
 * count too, e.g. Sharpness II to III.
 * <p>
 * Items that are created already enchanted (loot, villager trades, commands like /enchant and /give) don't call this,
 * since nothing is enchanted by a player.
 */
public class ItemEnchantEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /***
     * Where the enchanting happened.
     */
    public enum Source {
        /** NouveauEnchanting's enchanting gui. These events are always a {@link PlayerEnchantEvent}. */
        NOUVEAU_ENCHANTING,
        /** The vanilla enchanting table. */
        ENCHANTING_TABLE,
        /** Combining items or enchanted books in an anvil. */
        ANVIL
    }

    private final HumanEntity enchanter;
    private final ItemStack item;
    private ItemStack result;
    private final Map<Enchantment, Integer> enchantments;
    private final Source source;
    private boolean cancelled;

    public ItemEnchantEvent(HumanEntity enchanter, ItemStack item, ItemStack result, Map<Enchantment, Integer> enchantments, Source source) {
        this.enchanter = enchanter;
        this.item = item;
        this.result = result;
        this.enchantments = Collections.unmodifiableMap(enchantments);
        this.source = source;
    }

    /***
     * @return the player enchanting the item.
     */
    public HumanEntity getEnchanter() {
        return enchanter;
    }

    /***
     * @return a copy of the item before it's enchanted.
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /***
     * @return the enchanted item the player will get. It can be edited directly, or replaced with {@link #setResult(ItemStack)}.
     */
    public ItemStack getResult() {
        return result;
    }

    /***
     * Changes the item the player will get.
     */
    public void setResult(ItemStack result) {
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("The result can't be empty, cancel the event instead");
        }
        this.result = result;
    }

    /***
     * @return the enchants the item is gaining or being upgraded to, and their new levels. For books these are the
     * stored enchants. Changing the result doesn't change this map.
     */
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public Source getSource() {
        return source;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
