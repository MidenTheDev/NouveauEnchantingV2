package us.to.midensthings.nouveauEnchanting.customevents;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;

/***
 * Called when a player successfully enchants an item
 */
public class PlayerEnchantEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private Player player;

    private Enchantment enchantment;

    private EnchantMaterial enchantMaterial;

    private int materialCost;
    private int expcCost;

    public PlayerEnchantEvent(Player player, Enchantment enchantment, EnchantMaterial enchantMaterial, int materialCost, int expcCost) {
        this.player = player;
        this.enchantment = enchantment;
        this.enchantMaterial = enchantMaterial;
        this.materialCost = materialCost;
        this.expcCost = expcCost;
    }

    public Player getPlayer() {
        return player;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public EnchantMaterial getEnchantMaterial() {
        return enchantMaterial;
    }

    public int getMaterialCost() {
        return materialCost;
    }

    public int getExpcCost() {
        return expcCost;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
