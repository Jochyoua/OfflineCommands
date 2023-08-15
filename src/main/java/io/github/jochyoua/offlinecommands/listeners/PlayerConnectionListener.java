package io.github.jochyoua.offlinecommands.listeners;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.storage.StorageUtils;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static io.github.jochyoua.offlinecommands.VariableConstants.SETTINGS_PATH;

public class PlayerConnectionListener implements Listener {
    private final OfflineCommands offlineCommands;

    public PlayerConnectionListener(OfflineCommands plugin) {
        this.offlineCommands = plugin;
    }

    /**
     * Handles the PlayerJoinEvent with the lowest priority.
     * Schedules a task to run later that checks the user storage data for the player who joined and executes any commands stored for them.
     * If the user storage data is null, creates a new user storage object and adds it to the user storage data.
     * If the user storage data is not null, removes it from the user storage data and runs all the commands stored for the player.
     *
     * @param playerJoinEvent the event that occurred when a player joined the server
     * @see org.bukkit.event.player.PlayerJoinEvent
     * @see io.github.jochyoua.offlinecommands.storage.UserStorage
     * @see io.github.jochyoua.offlinecommands.storage.StorageUtils
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(offlineCommands, () -> {
            Player player = playerJoinEvent.getPlayer();
            UserStorage userStorage = StorageUtils.getUser(offlineCommands.getUserStorageData().getUserStorageConfig(), player.getUniqueId());

            if (!player.isOnline()) {
                return;
            }

            if (userStorage == null) {
                offlineCommands.getUserStorageData().modifyUsersData((StorageUtils.addOrUpdateUserList(offlineCommands.getUserStorageData().getUserStorageConfig(), UserStorage.builder().uuid(player.getUniqueId()).username(player.getName()).build())));
                return;
            }

            offlineCommands.getUserStorageData().modifyUsersData((StorageUtils.removeUserFromUserList(offlineCommands.getUserStorageData().getUserStorageConfig(), player.getUniqueId())));

            userStorage.runAllCommands(player);
        }, offlineCommands.getConfig().getInt(SETTINGS_PATH + ".delay-execute-after-join-ticks", 20));
    }
}
