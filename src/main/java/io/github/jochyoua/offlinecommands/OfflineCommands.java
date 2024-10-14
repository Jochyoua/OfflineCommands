package io.github.jochyoua.offlinecommands;

import io.github.jochyoua.offlinecommands.api.DebugLogger;
import io.github.jochyoua.offlinecommands.commands.OfflineCommandExecutor;
import io.github.jochyoua.offlinecommands.libs.Metrics;
import io.github.jochyoua.offlinecommands.listeners.PlayerConnectionListener;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.SoundStorage;
import io.github.jochyoua.offlinecommands.storage.StorageManager;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * Main class for the OfflineCommands plugin.
 */
@Getter
public final class OfflineCommands extends JavaPlugin {

    static {
        ConfigurationSerialization.registerClass(UserStorage.class);
        ConfigurationSerialization.registerClass(CommandStorage.class);
        ConfigurationSerialization.registerClass(SoundStorage.class);

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private StorageManager storageManager;
    private DebugLogger debugLogger;

    public void onReload() {
        this.reloadConfig();
        storageManager.closeConnection();
        this.initializeStorageManager();

        this.saveConfig();
    }

    @Override
    public void onEnable() {
        setupConfig();
        initializeStorageManager();
        registerEvents();
        setupCommand();
        setupMetrics();
        debugLogger = new DebugLogger(getConfig().getBoolean("settings.log-to-file", true) ? this : null);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        unregisterClasses();
    }

    private void setupConfig() {
        getConfig().options().copyDefaults(true);
        getConfig().options().header("OfflineCommands by Jochyoua \nGithub: https://github.com/Jochyoua/OfflineCommands");
        saveDefaultConfig();
    }

    private void initializeStorageManager() {
        try {
            this.storageManager = new StorageManager(this);
            storageManager.initializeDatabase();
        } catch (SQLException e) {
            getDebugLogger().log(Level.WARNING, "Failed to initialize database, fix error before continuing: " + e.getMessage());
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    private void setupCommand() {
        OfflineCommandExecutor offlineCommandExecutor = new OfflineCommandExecutor(this);
        PluginCommand offlineCommand = getCommand("offlinecommands");

        if (offlineCommand != null) {
            offlineCommand.setExecutor(offlineCommandExecutor);
            offlineCommand.setTabCompleter(offlineCommandExecutor);
        } else {
            getDebugLogger().log(Level.WARNING, "Command 'offlinecommands' was unsuccessfully registered/null, perhaps a corrupt plugin.yml?");
        }
    }

    private void setupMetrics() {
        Metrics offlineMetrics = new Metrics(this, 13922);

        offlineMetrics.addCustomChart(new Metrics.SingleLineChart("commands", () -> {
            List<UserStorage> userStorageList = getStorageManager().getUserStorageList();
            return (int) userStorageList.stream()
                    .mapToLong(userStorage -> userStorage.getCommands().size()).sum();
        }));
    }

    private void unregisterClasses() {
        ConfigurationSerialization.unregisterClass(UserStorage.class);
        ConfigurationSerialization.unregisterClass(CommandStorage.class);
        ConfigurationSerialization.unregisterClass(SoundStorage.class);
    }
}
