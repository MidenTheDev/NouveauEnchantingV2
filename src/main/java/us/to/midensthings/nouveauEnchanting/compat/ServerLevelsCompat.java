package us.to.midensthings.nouveauEnchanting.compat;

import org.bukkit.entity.Player;
import us.to.midensthings.serverLevels.data.Registries;
import us.to.midensthings.serverLevels.systems.LevelSystem;
import us.to.midensthings.serverLevels.systems.LeveledPlayer;

public class ServerLevelsCompat {


    public int getPlayerLevel(Player player, String systemName) {
        // get the specified level system from the specified name
        LevelSystem system = Registries.getLevelSystem(systemName);
        // generate the leveled player to retrieve the player's level in the given system
        LeveledPlayer lp = system.generateLeveledPlayer(player);
        return lp.getLevel();
    }
}
