package io.github.jochyoua.offlinecommands.commands.subcommands;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.SoundStorage;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.applyChatColors;
import static io.github.jochyoua.offlinecommands.VariableConstants.*;

public class ModifyCommands {
    private final OfflineCommands offlineCommands;

    /**
     * Constructs a new ModifyCommands object with the specified OfflineCommands.
     *
     * @param offlineCommands the OfflineCommands instance
     */
    public ModifyCommands(OfflineCommands offlineCommands) {
        this.offlineCommands = offlineCommands;
    }

    /**
     * Removes a command from the configuration based on provided arguments.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     * @param args     the arguments provided with the command
     * @return true if the command was removed successfully, false otherwise
     */
    public boolean removeCommandfromDatabase(CommandSender sender, boolean feedback, String... args) {
        if (args.length != 3) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".incorrect-syntax")));
            return false;
        }

        sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-search"), args[2], args[1]));
        Map.Entry<UUID, OfflinePlayer> userData = getUserData(args[1]);

        UserStorage userStorage = getUserStorage(userData.getKey());
        if (userStorage == null) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".player-does-not-exist"), args[1], args[2]));
            return false;
        }

        List<CommandStorage> commandStorageList = new ArrayList<>(userStorage.getCommands());
        if (!args[2].equalsIgnoreCase("*")) {
            CommandStorage commandStorage = userStorage.getCommand(args[2]);
            if (commandStorage == null) {
                sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-not-found")));
                return false;
            }
            commandStorageList.removeIf(offlineCommand -> offlineCommand.getIdentifier().equalsIgnoreCase(args[2]));
        } else if (userStorage.getCommands().isEmpty()) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-not-found")));
            return false;
        } else {
            commandStorageList.clear();
        }

        if (commandStorageList.isEmpty()) {
            try {
                offlineCommands.getStorageManager().removeUser(userStorage.getUuid());
                offlineCommands.getDebugLogger().log(Level.INFO, String.format("Removed %1$s from database.", userStorage.getUsername()));
            } catch (SQLException e) {
                offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to remove user from database: " + e.getMessage());
            }
        } else {
            userStorage.setCommands(commandStorageList);

            if (!updateUserStorage(userStorage)) {
                sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".player-failed-to-update")));
                return false;
            }
        }
        sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-found")));
        offlineCommands.getDebugLogger().log(Level.INFO, String.format("Removed %1$s from database; Owned by %2$s.", args[2], userStorage.getUsername()));
        return true;
    }

    /**
     * Adds a command to the configuration based on provided arguments.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     * @param args     the arguments provided with the command
     * @return true if the command was added successfully, false otherwise
     */
    public boolean addCommandToDatabase(CommandSender sender, boolean feedback, String... args) {
        if (!(sender instanceof ConsoleCommandSender) && offlineCommands.getConfig().getBoolean(SETTINGS_PATH + ".only-allow-console-to-add-commands")) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".only-console")));
            return false;
        }
        if (args.length < 3) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".incorrect-syntax")));
            return false;
        }

        String user = OfflineCommandsUtils.getValue(USER_KEY, args);
        CommandStorage.Executor executor = CommandStorage.Executor.getEnum(OfflineCommandsUtils.getValue(EXECUTOR_KEY, args));
        String commandToAdd = OfflineCommandsUtils.getValue(COMMAND_KEY, args);
        String message = Optional.ofNullable(OfflineCommandsUtils.getValue("message", args)).orElse("");
        String requiredPermission = Optional.ofNullable(OfflineCommandsUtils.getValue("permission", args)).orElse("");
        Boolean recurring = Boolean.valueOf(OfflineCommandsUtils.getValue("recurring", args));

        SoundStorage soundStorage = getSoundStorageFromString(
                OfflineCommandsUtils.getValue("sound", args),
                OfflineCommandsUtils.getValue("pitch", args),
                OfflineCommandsUtils.getValue("volume", args));

        if (commandToAdd == null || user == null) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".incorrect-syntax")));
            return false;
        }

        Map.Entry<UUID, OfflinePlayer> userData = getUserData(user);
        CommandStorage commandStorage = CommandStorage.builder()
                .soundStorage(soundStorage)
                .commandValue(commandToAdd)
                .message(message)
                .requiredPermission(requiredPermission)
                .executor(executor)
                .recurring(recurring)
                .build();

        if (userData.getValue().isOnline() && offlineCommands.getConfig().getBoolean(SETTINGS_PATH + ".execute-if-online")) {
            OfflineCommandsUtils.runCommandAsPlayer(userData.getValue().getPlayer(), commandStorage);
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".currently-online")));
            return true;
        }

        UserStorage userStorage = getUserStorage(userData.getKey());
        if (userStorage == null) {
            userStorage = UserStorage.builder()
                    .username(userData.getValue().getName())
                    .uuid(userData.getKey())
                    .commands(Collections.singletonList(commandStorage))
                    .build();
        } else {
            List<CommandStorage> commandStorageList = new ArrayList<>(userStorage.getCommands());
            commandStorageList.add(commandStorage);
            userStorage.setUsername(userData.getValue().getName());
            userStorage.setCommands(commandStorageList);
        }

        if (!updateUserStorage(userStorage)) {
            sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".player-failed-to-update")));
            return false;
        }

        sendFeedbackMessage(sender, feedback, String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".new-command-added"),
                Optional.ofNullable(userStorage.getUsername()).orElse(userData.getValue().getName()),
                commandStorage.getIdentifier()));
        offlineCommands.getDebugLogger().log(Level.INFO, String.format("Added %1$s to database; Owned by %2$s.", commandStorage, userStorage.getUsername()));
        return true;
    }

    /**
     * Converts sound, pitch, and volume strings into a SoundStorage object.
     *
     * @param soundRaw  the raw sound string
     * @param pitchRaw  the raw pitch string
     * @param volumeRaw the raw volume string
     * @return the SoundStorage object
     */
    public SoundStorage getSoundStorageFromString(String soundRaw, String pitchRaw, String volumeRaw) {
        if (soundRaw != null) {
            try {
                return SoundStorage.builder()
                        .sound(Sound.valueOf(soundRaw))
                        .pitch(pitchRaw == null ? DEFAULT_SOUND.getPitch() : Float.parseFloat(pitchRaw))
                        .volume(volumeRaw == null ? DEFAULT_SOUND.getVolume() : Float.parseFloat(volumeRaw))
                        .build();
            } catch (IllegalArgumentException e) {
                offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to convert sound input into SoundStorage: " + e.getMessage());
            }
        }
        return null;
    }

    private void sendFeedbackMessage(CommandSender sender, boolean feedback, String message) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(message), feedback);
    }

    private Map.Entry<UUID, OfflinePlayer> getUserData(String identifier) {
        try {
            return OfflineCommandsUtils.getDataFromUUID(UUID.fromString(identifier));
        } catch (IllegalArgumentException ignored) {
            return OfflineCommandsUtils.getDataFromUsername(identifier);
        }
    }

    private UserStorage getUserStorage(UUID uuid) {
        try {
            return offlineCommands.getStorageManager().getUser(uuid);
        } catch (SQLException | JsonProcessingException e) {
            offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to get user from database: " + e.getMessage());
            return null;
        }
    }


    private boolean updateUserStorage(UserStorage userStorage) {
        try {
            offlineCommands.getStorageManager().addOrUpdateUser(userStorage);
            return true;
        } catch (SQLException | JsonProcessingException e) {
            offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to update user: " + e.getMessage());
            return false;
        }
    }
}