package us.to.midensthings.nouveauEnchanting.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.nouveauEnchanting.customevents.ItemDisenchantEvent;
import us.to.midensthings.nouveauEnchanting.customevents.ItemEnchantEvent;
import us.to.midensthings.nouveauEnchanting.util.ItemEnchantments;
import us.to.midensthings.nouveauEnchanting.util.Schedulers;

import java.util.List;
import java.util.Map;

/***
 * /ne enchant and /ne disenchant, which change the enchants on the item the player is holding.
 * The enchants come from the enchantment registry, so custom enchants and the settings from enchants.yml are included.
 * Both call ItemEnchantEvent and ItemDisenchantEvent, so other plugins can change or cancel them.
 */
public final class EnchantCommands {
    private static final TextColor RED = TextColor.color(Color.RED.asRGB());
    private static final TextColor GREEN = TextColor.color(Color.LIME.asRGB());

    private EnchantCommands() {
    }

    /***
     * /ne enchant &lt;enchantment&gt; [level]
     */
    public static LiteralArgumentBuilder<CommandSourceStack> enchant() {
        return Commands.literal("enchant")
                .requires(source -> source.getSender().hasPermission("nouveauenchanting.commands.enchant"))
                .then(Commands.argument("enchantment", ArgumentTypes.resource(RegistryKey.ENCHANTMENT))
                        .executes(ctx -> enchantHeldItem(ctx, 1))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                .executes(ctx -> enchantHeldItem(ctx, IntegerArgumentType.getInteger(ctx, "level")))));
    }

    /***
     * A replacement for vanilla's /enchant with the same syntax, /enchant &lt;targets&gt; &lt;enchantment&gt; [level], so command
     * blocks and datapack functions that use /enchant keep working. Enchants the item each target player is holding.
     * @param label the command's name, e.g. "enchant"
     */
    public static LiteralArgumentBuilder<CommandSourceStack> vanillaStyleEnchant(String label) {
        return Commands.literal(label)
                .requires(source -> source.getSender().hasPermission("nouveauenchanting.commands.enchant"))
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .then(Commands.argument("enchantment", ArgumentTypes.resource(RegistryKey.ENCHANTMENT))
                                .executes(ctx -> enchantTargets(ctx, 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                        .executes(ctx -> enchantTargets(ctx, IntegerArgumentType.getInteger(ctx, "level"))))));
    }

    /***
     * /ne disenchant [enchantment], removes every enchant if no enchantment is given.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> disenchant() {
        return Commands.literal("disenchant")
                .requires(source -> source.getSender().hasPermission("nouveauenchanting.commands.disenchant"))
                .executes(ctx -> disenchantHeldItem(ctx, null))
                .then(Commands.argument("enchantment", ArgumentTypes.resource(RegistryKey.ENCHANTMENT))
                        .executes(ctx -> disenchantHeldItem(ctx, ctx.getArgument("enchantment", Enchantment.class))));
    }

    private static int enchantHeldItem(CommandContext<CommandSourceStack> ctx, int level) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Only players can use this command!", RED));
            return Command.SINGLE_SUCCESS;
        }
        enchantHeldItem(player, player, ctx.getArgument("enchantment", Enchantment.class), level);
        return Command.SINGLE_SUCCESS;
    }

    private static int enchantTargets(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        Enchantment enchantment = ctx.getArgument("enchantment", Enchantment.class);
        List<Player> targets = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
        if (targets.isEmpty()) {
            sender.sendMessage(Component.text("No players were found!", RED));
            return 0;
        }
        for (Player target : targets) {
            // Targets can be in other regions on Folia, so change their items on their thread
            Schedulers.runFor(target, () -> enchantHeldItem(sender, target, enchantment, level));
        }
        return targets.size();
    }

    /***
     * Enchants the item the target is holding, telling the sender what happened.
     */
    private static void enchantHeldItem(CommandSender sender, Player target, Enchantment enchantment, int level) {
        boolean self = sender == target;
        String whose = self ? "your" : target.getName() + "'s";
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            sender.sendMessage(Component.text(self ? "You must be holding an item!" : target.getName() + " isn't holding an item!", RED));
            return;
        }

        // The same checks as vanilla's /enchant, books can hold any enchant
        boolean isBook = item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK;
        if (!isBook && !enchantment.canEnchantItem(item)) {
            sender.sendMessage(enchantment.description().append(Component.text(" can't be put on " + whose + " item!", RED)).colorIfAbsent(RED));
            return;
        }
        if (level > enchantment.getMaxLevel()) {
            sender.sendMessage(Component.text("The highest level of ", RED).append(enchantment.description())
                    .append(Component.text(" is " + enchantment.getMaxLevel() + "!", RED)));
            return;
        }
        Map<Enchantment, Integer> current = ItemEnchantments.get(item);
        for (Enchantment existing : current.keySet()) {
            if (!existing.equals(enchantment) && enchantment.conflictsWith(existing)) {
                sender.sendMessage(enchantment.description().append(Component.text(" can't be combined with ", RED))
                        .append(existing.description()).append(Component.text("!", RED)).colorIfAbsent(RED));
                return;
            }
        }
        if (current.getOrDefault(enchantment, 0) == level) {
            sender.sendMessage(Component.text(self ? "Your item already has " : target.getName() + "'s item already has ", RED).append(enchantment.displayName(level)).append(Component.text("!", RED)));
            return;
        }

        ItemStack result = ItemEnchantments.with(item, Map.of(enchantment, level));
        ItemEnchantEvent event = new ItemEnchantEvent(target, item.clone(), result, Map.of(enchantment, level), ItemEnchantEvent.Source.COMMAND);
        if (!event.callEvent()) {
            sender.sendMessage(Component.text("Enchanting the item was cancelled by another plugin.", RED));
            return;
        }

        target.getInventory().setItemInMainHand(event.getResult());
        sender.sendMessage(Component.text("Enchanted " + whose + " item with ", GREEN).append(enchantment.displayName(level)));
    }

    private static int disenchantHeldItem(CommandContext<CommandSourceStack> ctx, Enchantment enchantment) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Only players can use this command!", RED));
            return Command.SINGLE_SUCCESS;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            player.sendMessage(Component.text("You must be holding an item!", RED));
            return Command.SINGLE_SUCCESS;
        }

        // Every enchant, or just the one given
        Map<Enchantment, Integer> current = ItemEnchantments.get(item);
        Map<Enchantment, Integer> removed;
        if (enchantment == null) {
            removed = current;
        } else if (current.containsKey(enchantment)) {
            removed = Map.of(enchantment, current.get(enchantment));
        } else {
            removed = Map.of();
        }
        if (removed.isEmpty()) {
            player.sendMessage(Component.text(enchantment == null ? "This item has no enchants!" : "This item doesn't have that enchant!", RED));
            return Command.SINGLE_SUCCESS;
        }

        ItemStack result = ItemEnchantments.without(item, removed.keySet());
        ItemDisenchantEvent event = new ItemDisenchantEvent(player, item.clone(), result, removed, ItemDisenchantEvent.Source.COMMAND);
        if (!event.callEvent()) {
            player.sendMessage(Component.text("Disenchanting the item was cancelled by another plugin.", RED));
            return Command.SINGLE_SUCCESS;
        }

        player.getInventory().setItemInMainHand(event.getResult());
        player.sendMessage(Component.text("Removed " + removed.size() + " enchant(s) from your item.", GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
