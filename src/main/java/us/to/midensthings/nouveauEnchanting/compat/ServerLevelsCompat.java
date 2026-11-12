package us.to.midensthings.nouveauEnchanting.compat;

import org.bukkit.entity.Player;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;
import us.to.midensthings.serverLevels.data.Registries;
import us.to.midensthings.serverLevels.systems.LevelSystem;
import us.to.midensthings.serverLevels.systems.LeveledPlayer;

public class ServerLevelsCompat {


    public int getPlayerLevel(Player player, String systemName) {
        // get the specified level system from the specified name
        LevelSystem system = Registries.getLevelSystem(systemName);
        if (system == null) {
            // Misconfigured system name, treat the player as not meeting the requirement
            NouveauEnchanting.getPlugin(NouveauEnchanting.class).getLogger().warning("Unknown ServerLevels system " + systemName + " in materials.yml");
            return -1;
        }
        // get the player's data in the given system to retrieve their level
        LeveledPlayer lp = system.getLeveledPlayer(player);
        if (lp == null) {
            // The player isn't in this level system, so they haven't reached any level in it
            return 0;
        }
        return lp.getLevel();
    }
}
