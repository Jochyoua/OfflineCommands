package io.github.jochyoua.offlinecommands.listeners;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.prepareCommand;

public class PlayerConnectionListener implements Listener {
    private final OfflineCommands plugin;

    public PlayerConnectionListener(OfflineCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = playerJoinEvent.getPlayer();

            if(!player.isOnline()){
                return;
            }

            StringBuilder debugStringBuilder = new StringBuilder();

            for (Map.Entry<String, Boolean> entrySet :
                    getCommands(player).entrySet()) {
                debugStringBuilder.append("\n");
                debugStringBuilder.append(entrySet.getKey()).append(":").append(" ").append(entrySet.getValue());
            }

            if (debugStringBuilder.length() != 0) {
                debugStringBuilder.append("\n");
                OfflineCommandsUtils.logMessage("Executing commands for " + player.getName() + "\n" + debugStringBuilder, "debug");
            }

            plugin.getConfig().set("users." + player.getUniqueId(), null);
            plugin.saveConfig();
        }, plugin.getConfig().getInt("settings.delay-execute-after-join-ticks", 20));
    }


    private Map<String, Boolean> getCommands(Player player) {
        Map<String, Boolean> commands = new HashMap<>();
        String path = "users." + player.getUniqueId() + ".commands-to-execute";

        ConfigurationSection commandConfigurationSection = plugin.getConfig().getConfigurationSection(path);
        if (commandConfigurationSection == null) {
            return commands;
        }

        for (String pathEntry :
                commandConfigurationSection.getKeys(false)) {
            String command = plugin.getConfig().getString(path + "." + pathEntry + ".command");
            String executor = plugin.getConfig().getString(path + "." + pathEntry + ".execute_as");
            if (command != null && executor != null) {
                command = prepareCommand(command, player);
                commands.put(command,
                        Bukkit.dispatchCommand(executor.equalsIgnoreCase("CONSOLE") ?
                                Bukkit.getConsoleSender() : player, command));
            }
        }
        return commands;
    }
}
