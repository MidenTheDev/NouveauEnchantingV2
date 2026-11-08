package us.to.midensthings.nouveauEnchanting.addons.builtin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.to.midensthings.nouveauEnchanting.addons.CustomEnchantment;

import java.util.List;

/***
 * Slows down whatever the wielder hits, like Fire Aspect but with ice.
 */
public class IceAspectEnchant implements CustomEnchantment, Listener {
    @Override
    public Component getDescription() {
        return MiniMessage.miniMessage().deserialize("<aqua>Ice Aspect");
    }

    @Override
    public String getId() {
        return "ice_aspect";
    }

    @Override
    public String getNamespace() {
        return "nouveauenchanting";
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public List<String> getAllowedItems() {
        return List.of("#swords", "#spears", "mace");
    }

    @Override
    public List<String> getPrimaryItems() {
        return List.of("#swords", "#spears");
    }

    @Override
    public List<String> getConflicts() {
        return List.of("fire_aspect");
    }

    @Override
    public List<String> getTags() {
        // Found in the same places as normal vanilla enchants like Fire Aspect
        return List.of("non_treasure", "in_enchanting_table", "on_random_loot", "tradeable", "on_mob_spawn_equipment", "on_traded_equipment");
    }

    @Override
    public int getAnvilCost() {
        return 4;
    }

    @Override
    public int getWeight() {
        return 2;
    }

    @Override
    public List<EquipmentSlotGroup> getActiveSlots() {
        return List.of(EquipmentSlotGroup.MAINHAND);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHit(EntityDamageByEntityEvent e) {
        // Works for any mob holding the weapon, not just players
        if (!(e.getDamager() instanceof LivingEntity attacker) || !(e.getEntity() instanceof LivingEntity target)) return;
        // Only melee hits, like Fire Aspect
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

        EntityEquipment equipment = attacker.getEquipment();
        if (equipment == null) return;

        // check if the item has THIS enchantment
        int level = getLevel(equipment.getItemInMainHand());
        if (level <= 0) return;

        // Apply slowness, longer and stronger with higher levels
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40 * level, level - 1));
    }
}
