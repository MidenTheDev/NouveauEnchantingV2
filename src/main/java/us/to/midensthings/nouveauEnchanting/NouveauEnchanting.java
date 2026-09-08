package us.to.midensthings.nouveauEnchanting;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.to.midensthings.nouveauEnchanting.commands.Debug;
import us.to.midensthings.nouveauEnchanting.enchanting.MaterialRegistry;
import us.to.midensthings.nouveauEnchanting.events.InventoryClick;
import us.to.midensthings.nouveauEnchanting.events.InventoryClose;
import us.to.midensthings.nouveauEnchanting.events.OpenEnchantingTable;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public final class NouveauEnchanting extends JavaPlugin {

    FileConfiguration config;
    PluginManager pm;

    File materialsYml = new File(this.getDataFolder()+"/materials.yml");
    public YamlConfiguration materialsConf;

    Logger logger = this.getLogger();
    public MaterialRegistry materialRegistry;



    public NouveauEnchanting(MaterialRegistry bootstrapRegistry, YamlConfiguration bootMaterialsConf) {
        materialRegistry = bootstrapRegistry;
        materialsConf = bootMaterialsConf;
    }


    @Override
    public void onEnable() {
        logger.info("Starting Neuveau Enchanting");
        pm = Bukkit.getServer().getPluginManager();

        loadConfigs();
        loadCompats();
        registerEvents();
        registerCommands();



    }
    @Override
    public void onDisable() {
        logger.info("Disabling Neuveau Enchanting");
        // Plugin shutdown logic
    }


    public void registerEvents() {
        if (config.getBoolean("override-enchanting-table")){
            pm.registerEvents(new OpenEnchantingTable(),this);
        }
        pm.registerEvents(new InventoryClick(), this);
        pm.registerEvents(new InventoryClose(), this);
    }

    public void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            Debug debugCmd = new Debug();
            commands.registrar().register(debugCmd.debugRootBuilder);
        });

    }


    public void loadCompats() {

    }

    public void loadConfigs() {
        // make configs
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        config = this.getConfig();


    }
}
