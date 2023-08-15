package io.github.jochyoua.offlinecommands;

import io.github.jochyoua.offlinecommands.commands.OfflineCommandExecutor;
import io.github.jochyoua.offlinecommands.listeners.PlayerConnectionListener;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.SoundStorage;
import io.github.jochyoua.offlinecommands.storage.UserConfigData;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;


public final class OfflineCommands extends JavaPlugin {

    static {
        ConfigurationSerialization.registerClass(UserStorage.class);
        ConfigurationSerialization.registerClass(CommandStorage.class);
        ConfigurationSerialization.registerClass(SoundStorage.class);
    }

    UserConfigData userConfigData;


    public UserConfigData getUserStorageData() {
        return this.userConfigData;
    }

    public void onReload() {
        this.reloadConfig();
        this.userConfigData.reloadConfig();
    }

    @Override
    public void onEnable() {

        getConfig().options().copyDefaults(true);
        getConfig().options().header("OfflineCommands by Jochyoua \n Github: https://github.com/Jochyoua/OfflineCommands");
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        OfflineCommandExecutor offlineCommandExecutor = new OfflineCommandExecutor(this);
        PluginCommand offlineCommand = getCommand("offlinecommands");

        if (offlineCommand != null) {
            offlineCommand.setExecutor(offlineCommandExecutor);
            offlineCommand.setTabCompleter(offlineCommandExecutor);
        } else {
            getLogger().warning("Command 'offlinecommands' was unsuccessfully registered/null, perhaps a corrupt plugin.yml?");
        }

        userConfigData = new UserConfigData(this);
    }

    @Override
    public void onDisable() {
        this.userConfigData = null;
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        ConfigurationSerialization.unregisterClass(UserStorage.class);
        ConfigurationSerialization.unregisterClass(CommandStorage.class);
        ConfigurationSerialization.unregisterClass(SoundStorage.class);

    }
}
