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
 * Called when a player is about to remove enchants from an item, from any source. Listen to this instead of each
 * source's own event: grindstones (the only way to remove enchants in vanilla) and the /ne disenchant command.
 * <p>
 * The result can be changed (e.g. to remove data an enchant added to the item) or the disenchanting cancelled.
 */
public class ItemDisenchantEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /***
     * Where the disenchanting happened.
     */
    public enum Source {
        /** Removing enchants with a grindstone. */
        GRINDSTONE,
        /** NouveauEnchanting's /ne disenchant command. */
        COMMAND
    }

    private final HumanEntity disenchanter;
    private final ItemStack item;
    private ItemStack result;
    private final Map<Enchantment, Integer> removedEnchantments;
    private final Source source;
    private boolean cancelled;

    public ItemDisenchantEvent(HumanEntity disenchanter, ItemStack item, ItemStack result, Map<Enchantment, Integer> removedEnchantments, Source source) {
        this.disenchanter = disenchanter;
        this.item = item;
        this.result = result;
        this.removedEnchantments = Collections.unmodifiableMap(removedEnchantments);
        this.source = source;
    }

    /***
     * @return the player removing the enchants.
     */
    public HumanEntity getDisenchanter() {
        return disenchanter;
    }

    /***
     * @return a copy of the item before its enchants are removed. For a grindstone with two items in it, this is the first one.
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /***
     * @return the item the player will get. It can be edited directly, or replaced with {@link #setResult(ItemStack)}.
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
     * @return the enchants being removed, and the levels they had. For books these are the stored enchants.
     */
    public Map<Enchantment, Integer> getRemovedEnchantments() {
        return removedEnchantments;
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
