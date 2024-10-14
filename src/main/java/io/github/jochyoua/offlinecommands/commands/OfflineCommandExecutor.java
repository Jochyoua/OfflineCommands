package io.github.jochyoua.offlinecommands.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import io.github.jochyoua.offlinecommands.commands.subcommands.InfoCommands;
import io.github.jochyoua.offlinecommands.commands.subcommands.ModifyCommands;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.applyChatColors;
import static io.github.jochyoua.offlinecommands.VariableConstants.VARIABLES_PATH;

public class OfflineCommandExecutor implements CommandExecutor, TabCompleter {
    private static final List<String> BASE_ARGS = Arrays.asList("help", "list", "add", "remove", "info", "reload", "no-feedback");
    private static final List<String> ADD_ARGS = Arrays.asList("user=\"\"", "command=\"\"", "executor=\"\"", "permission=\"\"", "message=\"\"", "recurring=\"\"");

    private final OfflineCommands offlineCommands;

    public OfflineCommandExecutor(OfflineCommands plugin) {
        this.offlineCommands = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showHelpCommandToSender(sender, true);
        }

        InfoCommands infoCommands = new InfoCommands(offlineCommands);
        ModifyCommands modifyCommands = new ModifyCommands(offlineCommands);

        boolean feedback = !(String.join(" ", args).contains("no-feedback"));
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list":
                int page = parsePageNumber(args);
                return infoCommands.showListOfCommands(sender, feedback, page);
            case "info":
                return args.length == 2 && infoCommands.showFullCommandInfo(sender, feedback, args[1]);
            case "add":
                return modifyCommands.addCommandToDatabase(sender, feedback, args);
            case "remove":
                return modifyCommands.removeCommandfromDatabase(sender, feedback, args);
            case "reload":
                return reloadCommand(sender, feedback);
            case "help":
            default:
                return showHelpCommandToSender(sender, feedback);
        }
    }

    /**
     * Parses the page number from the command arguments.
     *
     * @param args the command arguments
     * @return the parsed page number or 1 if parsing fails
     */
    private int parsePageNumber(String[] args) {
        if (args.length == 2) {
            try {
                return Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    /**
     * Sends the help command message to the sender.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     * @return always returns true
     */
    private boolean showHelpCommandToSender(CommandSender sender, boolean feedback) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".help-command-format")), feedback);
        return true;
    }

    /**
     * Reloads the plugin configuration and sends a success message to the sender.
     *
     * @param sender   the sender of the command
     * @param feedback whether to send feedback messages to the sender
     * @return always returns true
     */
    private boolean reloadCommand(CommandSender sender, boolean feedback) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".reload-successful")), feedback);
        offlineCommands.onReload();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        final int length = args.length;

        if (length == 1) {
            handleBaseArgs(args[0], completions);
        } else if (length == 2) {
            handleSecondArg(args, completions);
        } else if (length == 3) {
            handleThirdArg(args, completions);
        } else {
            return ADD_ARGS;
        }

        Collections.sort(completions);
        return completions;
    }

    /**
     * Handles tab completion for the base arguments.
     *
     * @param arg         the current argument
     * @param completions the list to store completions
     */
    private void handleBaseArgs(String arg, List<String> completions) {
        StringUtil.copyPartialMatches(arg, BASE_ARGS, completions);
    }

    /**
     * Handles tab completion for the second argument based on the first argument.
     *
     * @param args        the command arguments
     * @param completions the list to store completions
     */
    private void handleSecondArg(String[] args, List<String> completions) {
        if (args[0].equalsIgnoreCase("info")) {
            handleInfoArgs(args[1], completions);
        }
        handleAddOrRemoveArgs(args[0], args[1], completions);
    }

    /**
     * Handles tab completion for the third argument based on the first two arguments.
     *
     * @param args        the command arguments
     * @param completions the list to store completions
     */
    private void handleThirdArg(String[] args, List<String> completions) {
        if (args[0].equalsIgnoreCase("add")) {
            handleAddOrRemoveArgs(args[0], args[2], completions);
        } else {
            handleRemoveIdentifierArgs(args[0], args[1], args[2], completions);
        }
    }

    /**
     * Handles tab completion for the add or remove arguments.
     *
     * @param arg1        the first argument
     * @param arg2        the second argument
     * @param completions the list to store completions
     */
    private void handleAddOrRemoveArgs(String arg1, String arg2, List<String> completions) {
        if (arg1.equalsIgnoreCase("add")) {
            StringUtil.copyPartialMatches(arg2, ADD_ARGS, completions);
        } else if (arg1.equalsIgnoreCase("remove")) {
            handleRemoveArgs(arg2, completions);
        }
    }

    /**
     * Handles tab completion for the remove arguments.
     *
     * @param arg2        the second argument
     * @param completions the list to store completions
     */
    private void handleRemoveArgs(String arg2, List<String> completions) {
        try {
            List<UserStorage> userStorageList = offlineCommands.getStorageManager().getUserStorageList();
            List<String> usernameList = userStorageList.stream()
                    .map(userStorage -> Optional.ofNullable(userStorage.getUsername()).orElse(userStorage.getUuid().toString()))
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(arg2, usernameList, completions);
        } catch (SQLException | JsonProcessingException e) {
            offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to get data from database, fix error before continuing: " + e.getMessage());
        }
    }

    /**
     * Handles tab completion for the info arguments.
     *
     * @param arg2        the second argument
     * @param completions the list to store completions
     */
    private void handleInfoArgs(String arg2, List<String> completions) {
        try {
            List<UserStorage> userStorageList = offlineCommands.getStorageManager().getUserStorageList();
            List<String> allCommands = userStorageList.stream()
                    .flatMap(userStorage -> userStorage.getCommands().stream())
                    .map(CommandStorage::getIdentifier)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(arg2, allCommands, completions);
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles tab completion for the remove identifier arguments.
     *
     * @param arg1        the first argument
     * @param arg2        the second argument
     * @param arg3        the third argument
     * @param completions the list to store completions
     */
    private void handleRemoveIdentifierArgs(String arg1, String arg2, String arg3, List<String> completions) {
        if (arg1.equalsIgnoreCase("remove")) {
            try {
                UUID uuid = getUUIDFromString(arg2);
                UserStorage userStorage = offlineCommands.getStorageManager().getUser(uuid);
                if (userStorage != null) {
                    List<String> identifierList = userStorage.getCommands().stream()
                            .map(CommandStorage::getIdentifier)
                            .collect(Collectors.toList());
                    identifierList.add("*");
                    StringUtil.copyPartialMatches(arg3, identifierList, completions);
                }
            } catch (SQLException | JsonProcessingException e) {
                offlineCommands.getDebugLogger().log(Level.WARNING, "Failed to get user from database, fix error before continuing: " + e.getMessage());
            }
        }
    }

    /**
     * Retrieves a UUID from a string.
     *
     * @param arg the string argument
     * @return the UUID
     */
    private UUID getUUIDFromString(String arg) {
        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException ignored) {
            return OfflineCommandsUtils.getDataFromUsername(arg).getKey();
        }
    }
}
