package io.github.jochyoua.offlinecommands.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

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
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        int delay = offlineCommands.getConfig().getInt(SETTINGS_PATH + ".delay-execute-after-join-ticks", 20);


        offlineCommands.getScheduler().entity(player).runDelayed(() -> handlePlayerJoin(player), delay);
    }

    /**
     * Handles the logic for a player joining the server.
     *
     * @param player the player who joined the server
     */
    private void handlePlayerJoin(Player player) {
        UserStorage userStorage = getUserStorage(player.getUniqueId());
        if (userStorage == null || !player.isOnline()) {
            return;
        }


        offlineCommands.getScheduler().global().run(() -> {

            userStorage.runAllCommands(player);

            try {
                if (userStorage.getCommands().isEmpty()) {
                    offlineCommands.getStorageManager().removeUser(player.getUniqueId());
                } else {
                    offlineCommands.getStorageManager().addOrUpdateUser(userStorage);
                }
            } catch (SQLException | JsonProcessingException e) {
                offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to update user in database, fix error before continuing: " + e.getMessage());
            }
        });
    }

    private UserStorage getUserStorage(UUID uuid) {
        try {
            return offlineCommands.getStorageManager().getUser(uuid);
        } catch (SQLException | JsonProcessingException e) {
            offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to get user from database, fix error before continuing: " + e.getMessage());
            return null;
        }
    }
}
