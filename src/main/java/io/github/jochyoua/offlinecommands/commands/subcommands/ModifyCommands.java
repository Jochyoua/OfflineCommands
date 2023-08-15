package io.github.jochyoua.offlinecommands.commands.subcommands;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.SoundStorage;
import io.github.jochyoua.offlinecommands.storage.StorageUtils;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.*;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.applyChatColors;
import static io.github.jochyoua.offlinecommands.VariableConstants.*;

public class ModifyCommands {
    OfflineCommands offlineCommands;

    public ModifyCommands(OfflineCommands offlineCommands) {
        this.offlineCommands = offlineCommands;
    }

    /**
     * Removes a command from the configuration file for a specified user and identifier.
     * The method will send feedback messages to the sender according to the configuration variables and the outcome of the operation.
     *
     * @param sender   the command sender, who can be a console or a player
     * @param feedback a boolean flag indicating whether to send feedback messages to the sender or not
     * @param args     a variable-length array of strings containing the user UUID and the command identifier arguments
     * @return true if the command was successfully removed from the configuration file, false otherwise
     **/
    public boolean removeCommandFromConfig(CommandSender sender, boolean feedback, String... args) {
        if (args.length != 3) {
            OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".incorrect-syntax"))), feedback);
            return false;
        }

        OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-search"), args[2], args[1])), feedback);
        Map.Entry<UUID, OfflinePlayer> userData;
        try {
            userData = OfflineCommandsUtils.getDataFromUUID(UUID.fromString(args[1]));
        } catch (IllegalArgumentException ignored) {
            userData = OfflineCommandsUtils.getDataFromUsername(args[1]);
        }

        UserStorage userStorage = StorageUtils.getUser(offlineCommands.getUserStorageData().getUserStorageConfig(), userData.getKey());

        if (userStorage == null) {
            OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".player-does-not-exist"), args[1], args[2])), feedback);
            return false;
        }

        List<CommandStorage> commandStorageList = new ArrayList<>(userStorage.getCommands());

        if (!args[2].equalsIgnoreCase("*")) {
            CommandStorage commandStorage = userStorage.getCommand(args[2]);
            if (commandStorage == null) {
                OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-not-found"))), feedback);
                return false;
            }
            commandStorageList.removeIf(offlineCommand -> offlineCommand.getIdentifier().equalsIgnoreCase(args[2]));
        } else {
            if (!userStorage.getCommands().isEmpty()) {
                commandStorageList.clear();
            } else {
                OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-not-found"))), feedback);
                return false;
            }
        }

        userStorage.setCommands(commandStorageList);
        offlineCommands.getUserStorageData().modifyUsersData(StorageUtils.addOrUpdateUserList(offlineCommands.getUserStorageData().getUserStorageConfig(), userStorage));

        OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-found"))), feedback);
        return true;
    }

    /**
     * Adds a command to the database for a specified user and executor.
     * The command will be executed when the user logs in, unless the user is online and the execute-if-online setting is true.
     * The method will send feedback messages to the sender according to the configuration variables and the outcome of the operation.
     *
     * @param sender   the command sender, who can be a console or a player
     * @param feedback a boolean flag indicating whether to send feedback messages to the sender or not
     * @param args     a variable-length array of strings containing the user, executor, and command arguments
     * @return true if the command was successfully added to the config or executed online, false otherwise
     **/
    public boolean addCommandToConfig(CommandSender sender, boolean feedback, String... args) {
        if (!(sender instanceof ConsoleCommandSender) && offlineCommands.getConfig().getBoolean(SETTINGS_PATH + ".only-allow-console-to-add-commands")) {
            OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".only-console"))), feedback);
            return false;
        }
        if (args.length < 3) {
            OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".incorrect-syntax"))), feedback);
            return false;
        }

        String user = OfflineCommandsUtils.getValue(USER_KEY, args);

        CommandStorage.Executor executor = CommandStorage.Executor.getEnum(OfflineCommandsUtils.getValue(EXECUTOR_KEY, args));

        String commandToAdd = OfflineCommandsUtils.getValue(COMMAND_KEY, args);

        String message = OfflineCommandsUtils.getValue("message", args);

        String requiredPermission = OfflineCommandsUtils.getValue("permission", args);

        String soundRaw = OfflineCommandsUtils.getValue("sound", args);
        String pitchRaw = OfflineCommandsUtils.getValue("pitch", args);
        String volumeRaw = OfflineCommandsUtils.getValue("volume", args);

        SoundStorage soundStorage = getSoundStorageFromString(soundRaw, pitchRaw, volumeRaw);

        if (commandToAdd == null || user == null) {
            OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".incorrect-syntax"))), feedback);
            return false;
        }

        Map.Entry<UUID, OfflinePlayer> userData;
        try {
            userData = OfflineCommandsUtils.getDataFromUUID(UUID.fromString(user));
        } catch (IllegalArgumentException ignored) {
            userData = OfflineCommandsUtils.getDataFromUsername(user);
        }

        CommandStorage commandStorage = CommandStorage.builder().soundStorage(soundStorage).commandValue(commandToAdd).message(message == null ? "" : message).requiredPermission(requiredPermission == null ? "" : requiredPermission).executor(executor).build();

        if (userData.getValue().isOnline() && (offlineCommands.getConfig().getBoolean(SETTINGS_PATH + ".execute-if-online"))) {
            OfflineCommandsUtils.runCommandAsPlayer(userData.getValue().getPlayer(), commandStorage);
            OfflineCommandsUtils.sendMessage(sender, (applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".currently-online"))), feedback);
            return true;
        }

        UserStorage userStorage = StorageUtils.getUser(offlineCommands.getUserStorageData().getUserStorageConfig(), userData.getKey());
        if (userStorage == null) {
            userStorage = UserStorage.builder().username(userData.getValue().getName()).uuid(userData.getKey()).commands(Collections.singletonList(commandStorage)).build();
            offlineCommands.getUserStorageData().modifyUsersData((StorageUtils.addOrUpdateUserList(offlineCommands.getUserStorageData().getUserStorageConfig(), userStorage)));
        } else {
            List<CommandStorage> commandStorageList = new ArrayList<>(userStorage.getCommands());
            commandStorageList.add(commandStorage);
            userStorage.setUsername(userData.getValue().getName());
            userStorage.setCommands(commandStorageList);
            offlineCommands.getUserStorageData().modifyUsersData((StorageUtils.addOrUpdateUserList(offlineCommands.getUserStorageData().getUserStorageConfig(), userStorage)));
        }


        OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".new-command-added"), Optional.ofNullable(userStorage.getUsername()).orElse(userData.getValue().getName()), commandStorage.getIdentifier(), commandStorage.getExecutor(), commandStorage.getCommandValue(), commandStorage.getSoundStorage() == null ? "UNSET" : commandStorage.getSoundStorage().getSound().toString(), commandStorage.getRequiredPermission().isBlank() ? "UNSET" : commandStorage.getRequiredPermission())), feedback);
        return true;
    }

    public SoundStorage getSoundStorageFromString(String soundRaw, String pitchRaw, String volumeRaw) {
        SoundStorage soundStorage = null;
        if (soundRaw != null) {
            try {
                soundStorage = SoundStorage.builder().sound(Sound.valueOf(soundRaw)).pitch(pitchRaw == null ? DEFAULT_SOUND.getPitch() : Float.parseFloat(pitchRaw)).pitch(volumeRaw == null ? DEFAULT_SOUND.getVolume() : Float.parseFloat(volumeRaw)).build();
            } catch (IllegalArgumentException e) {
                offlineCommands.getLogger().warning("Failed to convert sound input into SoundStorage: " + e.getMessage());
            }

        }
        return soundStorage;
    }
}
