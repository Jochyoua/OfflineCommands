package io.github.jochyoua.offlinecommands.commands;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import io.github.jochyoua.offlinecommands.commands.subcommands.InfoCommands;
import io.github.jochyoua.offlinecommands.commands.subcommands.ModifyCommands;
import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.StorageUtils;
import io.github.jochyoua.offlinecommands.storage.UserStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.*;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.applyChatColors;
import static io.github.jochyoua.offlinecommands.VariableConstants.USERS_KEY;
import static io.github.jochyoua.offlinecommands.VariableConstants.VARIABLES_PATH;

public class OfflineCommandExecutor implements CommandExecutor, TabCompleter {
    private static final List<String> BASE_ARGS = Arrays.asList("help", "list", "add", "remove", "reload", "no-feedback");
    private static final List<String> ADD_ARGS = Arrays.asList("user=\"\"", "command=\"\"", "executor=\"\"", "permission=\"\"", "message=\"\"");

    private final OfflineCommands offlineCommands;

    public OfflineCommandExecutor(OfflineCommands plugin) {
        this.offlineCommands = plugin;
    }

    /**
     * Executes the offline commands plugin subcommands based on the command arguments.
     * The subcommands include list, add, remove, reload, and help.
     * The method will also handle invalid or missing arguments and provide feedback messages to the sender.
     *
     * @param sender  the command sender, who can be a console or a player
     * @param command the command that was executed
     * @param label   the alias of the command that was used
     * @param args    a variable-length array of strings containing the command arguments
     * @return true if the subcommand was executed successfully, false otherwise
     */
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
                int page = 1;
                if (args.length == 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                return infoCommands.showListOfCommands(sender, feedback, page);
            case "add":
                return modifyCommands.addCommandToConfig(sender, feedback, args);
            case "remove":
                return modifyCommands.removeCommandFromConfig(sender, feedback, args);
            case "reload":
                return reloadCommand(sender, feedback);
            case "help":
            default:
                return showHelpCommandToSender(sender, feedback);
        }
    }

    /**
     * Returns the formatted help message for the OfflineCommands plugin.
     *
     * @return the formatted help message as a string
     */
    private boolean showHelpCommandToSender(CommandSender sender, boolean feedback) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".help-command-format")), feedback);
        return true;
    }

    /**
     * Reloads the plugin and sends a success message to the sender.
     *
     * @param sender   the command sender who issued the reload command
     * @param feedback whether to send feedback messages to the sender or not
     * @return true if the reload was successful, false otherwise
     */
    private boolean reloadCommand(CommandSender sender, boolean feedback) {
        OfflineCommandsUtils.sendMessage(sender, applyChatColors(offlineCommands.getConfig().getString(VARIABLES_PATH + ".reload-successful")), feedback);
        offlineCommands.onReload();
        return true;
    }

    /**
     * Provides a list of possible completions for a command argument.
     * The method will copy partial matches from different sources to the completions list, depending on the number and value of the arguments.
     * The sources include predefined lists of base arguments and add arguments, and dynamic lists of usernames and identifiers from the configuration file.
     * The method will also handle exceptions and invalid inputs when parsing UUIDs or getting user storage objects.
     *
     * @param sender  the command sender, who can be a console or a player
     * @param command the command that was executed
     * @param alias   the alias of the command that was used
     * @param args    a variable-length array of strings containing the command arguments
     * @return a list of strings that match the partial argument, or an empty list if none
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        final int length = args.length;

        if (length == 1) {
            handleBaseArgs(args[0], completions);
        } else if (length == 2) {
            handleAddOrRemoveArgs(args[0], args[1], completions);
        } else if (length == 3) {
            if (args[0].equalsIgnoreCase("add")) {
                handleAddOrRemoveArgs(args[0], args[2], completions);
            } else {
                handleRemoveIdentifierArgs(args[0], args[1], args[2], completions);
            }
        } else {
            return ADD_ARGS;
        }

        Collections.sort(completions);
        return completions;
    }

    /**
     * Handles the case when there is only one argument.
     * Copies the partial matches from the predefined list of base arguments to the completions list.
     *
     * @param arg         the first and only argument, which should be a partial match of a base argument
     * @param completions the list of strings that will store the possible completions
     */
    private void handleBaseArgs(String arg, List<String> completions) {
        StringUtil.copyPartialMatches(arg, BASE_ARGS, completions);
    }

    /**
     * Handles the case when there are two arguments and the first one is add or remove.
     * Copies the partial matches from different sources to the completions list, depending on the value of the second argument.
     * The sources include predefined lists of add arguments and dynamic lists of usernames from the configuration file.
     *
     * @param arg1        the first argument, which should be either add or remove
     * @param arg2        the second argument, which should be a partial match of an add argument or a username
     * @param completions the list of strings that will store the possible completions
     */
    private void handleAddOrRemoveArgs(String arg1, String arg2, List<String> completions) {
        if (arg1.equalsIgnoreCase("add")) {
            StringUtil.copyPartialMatches(arg2, ADD_ARGS, completions);
        } else if (arg1.equalsIgnoreCase("remove")) {
            List<UserStorage> userStorageList = (List<UserStorage>) offlineCommands.getUserStorageData().getUserStorageConfig().getList(USERS_KEY);
            List<String> usernameList = userStorageList.stream().map(userStorage -> userStorage.getUsername() == null ? userStorage.getUuid().toString() : userStorage.getUsername()).toList();
            if (!usernameList.isEmpty()) {
                StringUtil.copyPartialMatches(arg2, usernameList, completions);
            }
        }
    }

    /**
     * Handles the case when there are three arguments and the first one is remove.
     * Copies the partial matches from the dynamic list of identifiers from the configuration file to the completions list, depending on the value of the third argument.
     * The method also parses the UUID from the second argument and handles exceptions and invalid inputs.
     *
     * @param arg1        the first argument, which should be remove
     * @param arg2        the second argument, which should be a valid UUID or a username
     * @param arg3        the third argument, which should be a partial match of an identifier
     * @param completions the list of strings that will store the possible completions
     */
    private void handleRemoveIdentifierArgs(String arg1, String arg2, String arg3, List<String> completions) {
        if (arg1.equalsIgnoreCase("remove")) {
            UUID uuid;
            try {
                uuid = UUID.fromString(arg2);
            } catch (IllegalArgumentException ignored) {
                uuid = OfflineCommandsUtils.getDataFromUsername(arg2).getKey();
            }
            UserStorage userStorage = StorageUtils.getUser(offlineCommands.getUserStorageData().getUserStorageConfig(), uuid);
            if (userStorage != null) {
                List<CommandStorage> commands = userStorage.getCommands();
                List<String> identifierList = new ArrayList<>(commands.stream().map(CommandStorage::getIdentifier).toList());
                if(!identifierList.isEmpty()){
                    identifierList.add("*");
                }
                StringUtil.copyPartialMatches(arg3, identifierList, completions);
            }
        }
    }
}
