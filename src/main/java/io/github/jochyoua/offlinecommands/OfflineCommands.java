package io.github.jochyoua.offlinecommands;

import io.github.jochyoua.offlinecommands.commands.OfflineCommandExecutor;
import io.github.jochyoua.offlinecommands.listeners.PlayerConnectionListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class OfflineCommands extends JavaPlugin {

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
        }
    }
}
