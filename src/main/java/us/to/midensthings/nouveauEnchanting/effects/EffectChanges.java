package us.to.midensthings.nouveauEnchanting.effects;

import java.util.List;
import java.util.Set;

/***
 * Changes to an enchant's effects from enchants.yml.
 * @param add true if list effects are added to the enchant's existing ones instead of replacing them
 * @param effects the new effects
 * @param removed the kinds of effect to remove from the enchant
 */
public record EffectChanges(boolean add, List<EnchantmentEffect> effects, Set<EffectComponent> removed) {

    public boolean isEmpty() {
        return effects.isEmpty() && removed.isEmpty();
    }
}
