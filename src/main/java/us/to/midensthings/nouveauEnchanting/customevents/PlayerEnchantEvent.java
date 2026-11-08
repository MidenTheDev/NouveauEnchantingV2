package us.to.midensthings.nouveauEnchanting.customevents;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantMaterial;

import java.util.Map;

/***
 * Called when a player is about to enchant an item with NouveauEnchanting's enchanting gui, after the costs have been
 * checked and before anything is taken. The result, the costs and whether it happens at all can be changed.
 * <p>
 * This is also an {@link ItemEnchantEvent}, so listeners of that get these too. Listen to this one instead for the
 * enchanting material and costs.
 */
public class PlayerEnchantEvent extends ItemEnchantEvent {

    private final Player player;

    private final Enchantment enchantment;

    private final EnchantMaterial enchantMaterial;

    private final int enchantmentLevel;
    private int materialCost;
    private int expCost;

    public PlayerEnchantEvent(Player player, Enchantment enchantment, EnchantMaterial enchantMaterial, int materialCost, int expCost,
                              int enchantmentLevel, ItemStack item, ItemStack result) {
        super(player, item, result, Map.of(enchantment, enchantmentLevel), Source.NOUVEAU_ENCHANTING);
        this.player = player;
        this.enchantment = enchantment;
        this.enchantMaterial = enchantMaterial;
        this.materialCost = materialCost;
        this.expCost = expCost;
        this.enchantmentLevel = enchantmentLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
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

    /***
     * Changes how many materials are taken. The player must still have enough, or nothing happens.
     */
    public void setMaterialCost(int materialCost) {
        this.materialCost = Math.max(0, materialCost);
    }

    public int getExpCost() {
        return expCost;
    }

    /***
     * Changes how many experience levels are taken. The player must still have enough, or nothing happens.
     */
    public void setExpCost(int expCost) {
        this.expCost = Math.max(0, expCost);
    }
}
