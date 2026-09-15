package us.to.midensthings.nouveauEnchanting.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

import java.util.List;

public class Debug {

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);

    public LiteralCommandNode<CommandSourceStack> debugRootBuilder = Commands.literal("nedebug")

            // Material Registry Check debug command displays all registered enchant materials and their associated enchantments
            .then(Commands.literal("matreg").executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                plugin.materialRegistry.getMaterialRegistry().forEach((name, enchantMaterial) -> {
                    if (enchantMaterial.isCustomMaterial()) {
                        doubleSenderConsoleMessage(sender,"name: "+ name + "\n material: " + enchantMaterial.getMaterialName());

                    } else {
                        doubleSenderConsoleMessage(sender,"name: "+ name + "\n material: " + enchantMaterial.getVanillaMaterial().toString());

                    }
                    List<String> enchantKeys = enchantMaterial.getEnchantmentKeys();
                    doubleSenderConsoleMessage(sender, "Enchant keys:");
                    enchantKeys.forEach(key -> {
                        doubleSenderConsoleMessage(sender,key);
                    });
                });


                return Command.SINGLE_SUCCESS;
            }))


            .build();




    private void doubleSenderConsoleMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
        plugin.getLogger().info(message);
    }

}
