package io.github.jochyoua.offlinecommands.commands.subcommands;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import io.github.jochyoua.offlinecommands.api.Pagination;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.SoundStorage;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.applyChatColors;
import static io.github.jochyoua.offlinecommands.VariableConstants.VARIABLES_PATH;

public class InfoCommands {

    private static final String LIST_COMMAND_HEADER_PATH = VARIABLES_PATH + ".list-command-header";
    private static final String NO_USERS_FOUND_PATH = VARIABLES_PATH + ".no-users-found";
    private static final String LIST_COMMAND_FOOTER_PATH = VARIABLES_PATH + ".list-command-footer";
    private static final String LIST_COMMAND_FOOTER_END_PATH = VARIABLES_PATH + ".list-command-footer-end-of-list";
    private static final String LIST_INVALID_PAGE_NUMBER_PATH = VARIABLES_PATH + ".list-invalid-page-number";
    private static final String LIST_USER_UUID_PATH = VARIABLES_PATH + ".list-user-uuid";
    private static final String LIST_NO_COMMANDS_FOUND_PATH = VARIABLES_PATH + ".list-no-commands-found";
    private static final String LIST_COMMAND_FORMAT_PATH = VARIABLES_PATH + ".list-command-format";

    private final OfflineCommands offlineCommands;

    /**
     * Constructs a new InfoCommands object with the specified OfflineCommands.
     *
     * @param offlineCommands the OfflineCommands instance
     */
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
        List<UserStorage> userStorageList;
        try {
            userStorageList = offlineCommands.getStorageManager().getUserStorageList();
        } catch (SQLException | JsonProcessingException e) {
            offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to initialize database, fix error before continuing: " + e.getMessage());
            return false;
        }

        if (userStorageList.isEmpty()) {
            sendNoUsersFoundMessage(sender, feedback);
            return false;
        }

        sendHeaderMessage(sender, feedback);

        int pageSize = offlineCommands.getConfig().getInt("settings.page-size");
        Pagination pagination = new Pagination(userStorageList.size(), pageSize);

        if (!pagination.isValidPage(page)) {
            sendInvalidPageMessage(sender, feedback, page, pagination.getTotalPages());
            sendFooterMessage(sender, feedback, page, pagination.getTotalPages(), pagination.getNextPage(page));
            return false;
        }

        userStorageList.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .forEach(userStorage -> processUserStorage(sender, feedback, userStorage));

        sendFooterMessage(sender, feedback, page, pagination.getTotalPages(), pagination.getNextPage(page));
        return true;
    }

    /**
     * Sends the header message to the sender.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     */
    private void sendHeaderMessage(CommandSender sender, boolean feedback) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(LIST_COMMAND_HEADER_PATH)), feedback);
    }

    /**
     * Sends the no users found message to the sender.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     */
    private void sendNoUsersFoundMessage(CommandSender sender, boolean feedback) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(NO_USERS_FOUND_PATH)), feedback);
    }

    /**
     * Sends the invalid page message to the sender.
     *
     * @param sender     the sender of the command
     * @param feedback   whether to send feedback messages to the sender
     * @param page       the current page number
     * @param totalPages the total number of pages
     */
    private void sendInvalidPageMessage(CommandSender sender, boolean feedback, int page, int totalPages) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(LIST_INVALID_PAGE_NUMBER_PATH), page, totalPages)), feedback);
    }

    /**
     * Sends the footer message to the sender.
     *
     * @param sender     the sender of the command
     * @param feedback   whether to send feedback messages to the sender
     * @param page       the current page number
     * @param totalPages the total number of pages
     * @param nextPage   the next page number
     */
    private void sendFooterMessage(CommandSender sender, boolean feedback, int page, int totalPages, int nextPage) {
        String footerPath = nextPage <= totalPages ? LIST_COMMAND_FOOTER_PATH : LIST_COMMAND_FOOTER_END_PATH;
        String listCommandFooter = applyChatColors(String.format(offlineCommands.getConfig().getString(footerPath), page, totalPages, nextPage));
        OfflineCommandsUtils.sendMessage(sender, listCommandFooter, feedback);
    }

    /**
     * Processes user storage and sends relevant messages.
     *
     * @param sender      the sender of the command
     * @param feedback    whether to send feedback messages to the sender
     * @param userStorage the user storage object
     */
    private void processUserStorage(CommandSender sender, boolean feedback, UserStorage userStorage) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(String.format(offlineCommands.getConfig().getString(LIST_USER_UUID_PATH), userStorage.getUuid())), feedback);
        List<CommandStorage> commandStorageList = getCommandStorageList(userStorage);

        if (commandStorageList.isEmpty()) {
            OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(LIST_NO_COMMANDS_FOUND_PATH)), feedback);
        } else {
            commandStorageList.forEach(commandStorage -> sendCommandStorageMessage(sender, feedback, commandStorage));
        }
    }

    /**
     * Retrieves the command storage list for the given user storage.
     *
     * @param userStorage the user storage object
     * @return the list of command storage objects
     */
    private List<CommandStorage> getCommandStorageList(UserStorage userStorage) {
        return userStorage.getCommands() != null ? userStorage.getCommands() : Collections.emptyList();
    }

    /**
     * Shows full command information based on the given identifier.
     *
     * @param sender     the sender of the command
     * @param feedback   whether to send feedback messages to the sender
     * @param identifier the command identifier
     * @return true if the command information was successfully retrieved, false otherwise
     */
    public boolean showFullCommandInfo(CommandSender sender, boolean feedback, String identifier) {
        StringBuilder stringBuilder = new StringBuilder();

        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".list-commandInfo-header")), feedback);

        CommandStorage commandStorage;
        try {
            commandStorage = offlineCommands.getStorageManager().getCommandFromDatabase(identifier);
        } catch (SQLException | JsonProcessingException e) {
            offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to get information from command: " + e.getMessage());
            return false;
        }

        if (commandStorage == null) {
            OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".identifier-not-found")), feedback);
            return false;
        }

        Map<String, Object> serializedFields = commandStorage.serialize();
        String formatPath = offlineCommands.getConfig().getString(LIST_COMMAND_FORMAT_PATH);

        serializedFields.forEach((fieldName, value) -> {
            if (value instanceof SoundStorage) {
                value = ((SoundStorage) value).getSound();
            }
            stringBuilder.append(String.format(formatPath, fieldName, value != null && !value.toString().isEmpty() ? value : "UNSET")).append("\n");
        });

        OfflineCommandsUtils.sendMessage(sender, applyChatColors(stringBuilder.toString().trim()), feedback);
        return true;
    }

    /**
     * Sends the command storage message to the sender.
     *
     * @param sender         the sender of the command
     * @param feedback       whether to send feedback messages to the sender
     * @param commandStorage the command storage object
     */
    private void sendCommandStorageMessage(CommandSender sender, boolean feedback, CommandStorage commandStorage) {
        String message = String.format(offlineCommands.getConfig().getString(LIST_COMMAND_FORMAT_PATH),
                commandStorage.getIdentifier(),
                commandStorage.getCommandValue());
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(message), feedback);
    }
}