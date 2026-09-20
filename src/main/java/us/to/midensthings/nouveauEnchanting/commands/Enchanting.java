package us.to.midensthings.nouveauEnchanting.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.nouveauEnchanting.enchanting.EnchantingGUI;


public class Enchanting {
    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);
    public LiteralCommandNode<CommandSourceStack> coreCommandBuilder = Commands.literal("ne")

            // Material Registry Check debug command displays all registered enchant materials and their associated enchantments
            .then(Commands.literal("opengui").executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                        if (!sender.hasPermission("nouveauenchanting.commands.gui")) {
                            sender.sendMessage(Component.text("You do not have permission to use this command!").color(TextColor.color(Color.RED.asRGB())));
                            return Command.SINGLE_SUCCESS;
                        }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("You must specify a player!").color(TextColor.color(Color.RED.asRGB())));
                    return Command.SINGLE_SUCCESS;
                }

                Player player = (Player) sender;
                player.openInventory(new EnchantingGUI().getInventory());

                return Command.SINGLE_SUCCESS;
            }))

            .then(Commands.literal("openguiOther").then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> {
                Player player = Bukkit.getPlayer(StringArgumentType.getString(ctx,"player"));
                CommandSender sender = ctx.getSource().getSender();
                if (!sender.hasPermission("nouveauenchanting.commands.gui.other")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command!").color(TextColor.color(Color.RED.asRGB())));
                    return Command.SINGLE_SUCCESS;
                }
                if (player == null || !player.isOnline()) {
                    sender.sendMessage(Component.text("That player is not online!").color(TextColor.color(Color.RED.asRGB())));
                    return Command.SINGLE_SUCCESS;
                }

                player.openInventory(new EnchantingGUI().getInventory());
                sender.sendMessage(Component.text("Opened enchanting window for "+player.getName()).color(TextColor.color(Color.RED.asRGB())));
                return Command.SINGLE_SUCCESS;
            })))

            .then(Commands.literal("reload").executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!sender.hasPermission("nouveauenchanting.commands.reload")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command!").color(TextColor.color(Color.RED.asRGB())));
                    return Command.SINGLE_SUCCESS;
                }
                plugin.reloadConfig();

                sender.sendMessage(Component.text("Warning: this command only reloads the config.yml!").color(TextColor.color(Color.RED.asRGB())));
                sender.sendMessage(Component.text("A restart is required to apply edits in enchants.yml and materials.yml").color(TextColor.color(Color.RED.asRGB())));



                return Command.SINGLE_SUCCESS;
            }))


            .build();

}
