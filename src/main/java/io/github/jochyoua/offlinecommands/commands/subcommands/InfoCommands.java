package io.github.jochyoua.offlinecommands.commands.subcommands;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.applyChatColors;
import static io.github.jochyoua.offlinecommands.VariableConstants.USERS_KEY;
import static io.github.jochyoua.offlinecommands.VariableConstants.VARIABLES_PATH;

public class InfoCommands {
    OfflineCommands offlineCommands;

    public InfoCommands(OfflineCommands offlineCommands) {
        this.offlineCommands = offlineCommands;
    }

    /**
     * Executes the list command to show the stored commands for offline players.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     * @param page     the page to show the player
     * @return true if the command was executed successfully, false otherwise
     */
    public boolean showListOfCommands(CommandSender sender, boolean feedback, int page) {
        List<UserStorage> userStorageList = Optional.ofNullable(offlineCommands.getUserStorageData().getUserStorageConfig().get(USERS_KEY))
                .map(list -> (List<UserStorage>) list)
                .orElse(Collections.emptyList());

        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-command-header")), feedback);

        if (userStorageList.isEmpty()) {
            OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".no-users-found")), feedback);
            return false;
        }

        int pageSize = offlineCommands.getConfig().getInt("settings.page-size");
        int listSize = userStorageList.size();
        int totalPages = (int) Math.ceil((double) listSize / pageSize);
        int nextPage = page + 1;

        String listCommandFooter = applyChatColors(nextPage <= totalPages ?
                String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-command-footer"), page, totalPages, nextPage) :
                String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-command-footer-end-of-list"), page, totalPages));

        if (page < 1 || page > totalPages) {
            OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-invalid-page-number"), page, totalPages)), feedback);
            OfflineCommandsUtils.sendMessage(sender, listCommandFooter, feedback);
            return false;
        }

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, userStorageList.size());
        List<UserStorage> subList = userStorageList.subList(fromIndex, toIndex);

        for (UserStorage userStorage : subList) {
            OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-user-uuid"), userStorage.getUuid())), feedback);

            List<CommandStorage> commandStorageList = Optional.ofNullable(userStorage.getCommands())
                    .orElse(Collections.emptyList());

            if (commandStorageList.isEmpty()) {
                OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-no-commands-found")), feedback);
                continue;
            }

            for (CommandStorage commandStorage : commandStorageList) {
                OfflineCommandsUtils.sendMessage(sender, applyChatColors(
                        String.format(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-command-format"),
                                commandStorage.getIdentifier(),
                                commandStorage.getExecutor(),
                                commandStorage.getCommandValue(),
                                commandStorage.getSoundStorage() == null ? "UNSET" : commandStorage.getSoundStorage().getSound().toString(),
                                commandStorage.getRequiredPermission().isBlank() ? "UNSET" : commandStorage.getRequiredPermission())), feedback);
            }
        }

        OfflineCommandsUtils.sendMessage(sender, listCommandFooter, feedback);
        return true;
    }

}