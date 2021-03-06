package io.github.jochyoua.offlinecommands.commands;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.color;
import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.prepareCommand;

public class OfflineCommandExecutor implements CommandExecutor, TabCompleter {
    private final static List<String> BASE_ARGS = Arrays.asList("help", "list", "add", "remove");

    private final OfflineCommands plugin;

    public OfflineCommandExecutor(OfflineCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            OfflineCommandsUtils.sendMessage(sender, helpCommand(), true);
            return true;
        }

        boolean feedback = !(String.join(" ", args).contains("no-feedback"));
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list":
                return listCommand(sender, feedback);
            case "add":
                return addCommand(sender, feedback, args);
            case "remove":
                return removeCommand(sender, feedback, args);
            default:
            case "help":
                OfflineCommandsUtils.sendMessage(sender, helpCommand(), feedback);
                return true;
        }
    }

    /**
     * Generates the help command for the sender
     *
     * @return returns the help command with color coding
     */
    private String helpCommand() {
        return color(plugin.getConfig().getString("variables.help-command-format"));
    }

    /**
     * Handles the add command for the command sender.
     *
     * @param sender the user executing the command
     * @param args   the arguments from the command
     * @return if the command has failed
     */
    private boolean addCommand(CommandSender sender, boolean feedback, String... args) {
        if (!(sender instanceof ConsoleCommandSender) && plugin.getConfig().getBoolean("settings.only-allow-console-to-add-commands")) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.only-console"))), feedback);
            return true;
        }
        if (args.length < 3) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.incorrect-syntax"))), feedback);
            return true;
        }

        String user = OfflineCommandsUtils.getValue("user", args);
        String executor = OfflineCommandsUtils.getValue("executor", args);
        String commandToAdd = OfflineCommandsUtils.getValue("command", args);

        if (executor == null) {
            executor = "CONSOLE";
        }

        if (commandToAdd == null || user == null) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.incorrect-syntax"))), feedback);
            return true;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(user);
        } catch (IllegalArgumentException ignored) {
            Player player = Bukkit.getPlayer(user);
            if (player == null) {
                if (!plugin.getConfig().getBoolean("settings.use-offline-player-fallback")) {
                    OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.player-does-not-exist"))), feedback);
                    return true;
                }
                @SuppressWarnings("deprecation") OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(user);
                uuid = offlinePlayer.getUniqueId();
            } else {
                uuid = player.getUniqueId();
            }
            if (player != null
                    && player.isOnline()
                    && plugin.getConfig().getBoolean("settings.execute-if-online")) {
                Bukkit.dispatchCommand(executor.equalsIgnoreCase("CONSOLE") ?
                        Bukkit.getConsoleSender() : player, prepareCommand(commandToAdd, player));
                OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.currently-online"))), feedback);
                return true;
            }
        }

        String commandIdentifier = UUID.randomUUID().toString().split("-")[0];
        String path = "users." + uuid + ".commands-to-execute." + commandIdentifier;

        plugin.getConfig().set(path + ".execute_as", executor);
        plugin.getConfig().set(path + ".command", commandToAdd);
        plugin.saveConfig();

        OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.new-command-added")
                .replaceAll("(?i)\\{uuid}", uuid.toString())
                .replaceAll("(?i)\\{executor}", executor)
                .replaceAll("(?i)\\{command}", commandToAdd)
                .replaceAll("(?i)\\{identifier}", commandIdentifier))), feedback);
        return true;
    }

    /**
     * Handles the remove command for the command sender.
     *
     * @param sender the user executing the command
     * @param args   the arguments from the command
     * @return if the command has failed
     */
    private boolean removeCommand(CommandSender sender, boolean feedback, String... args) {
        if (args.length != 3) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.incorrect-syntax"))), feedback);
            return true;
        }

        OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.identifier-search")
                .replaceAll("(?i)\\{uuid}", args[1])
                .replaceAll("(?i)\\{identifier}", args[2]))), feedback);

        if (!plugin.getConfig().isSet("users." + args[1] + ".commands-to-execute." + args[2])) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.identifier-not-found"))), feedback);
            return true;
        }

        plugin.getConfig().set("users." + args[1] + ".commands-to-execute." + args[2], null);
        ConfigurationSection configurationSection = plugin.getConfig().getConfigurationSection("users." + args[1] + ".commands-to-execute");

        if (configurationSection == null || configurationSection.getKeys(false).isEmpty()) {
            plugin.getConfig().set("users." + args[1], null);
        }
        plugin.saveConfig();
        OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.identifier-found"))), feedback);
        return true;
    }

    /**
     * Shows a list of users who have commands saved to their UUID
     *
     * @param sender   the user executing the command
     * @param feedback if false, sender will not receive mesages
     * @return if the command has failed
     */
    private boolean listCommand(CommandSender sender, boolean feedback) {
        ConfigurationSection userConfigurationSection = plugin.getConfig().getConfigurationSection("users");
        if (userConfigurationSection == null) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.no-users-found"))), feedback);
            return true;
        }
        OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.list-command-header"))), feedback);
        Set<String> matchedUsers = userConfigurationSection.getKeys(false);
        if (matchedUsers.isEmpty()) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.no-users-found"))), feedback);
            return true;
        }
        for (String uuid :
                matchedUsers) {
            OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.list-user-uuid").
                    replaceAll("(?i)\\{uuid}", uuid))), feedback);

            ConfigurationSection commandConfigurationSection = plugin.getConfig().getConfigurationSection("users." + uuid + ".commands-to-execute");

            if (commandConfigurationSection == null || commandConfigurationSection.getKeys(false).isEmpty()) {
                OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.list-no-commands-found"))), feedback);
                return true;
            }

            for (String commandToExecute :
                    commandConfigurationSection.getKeys(false)) {
                String path = "users." + uuid + ".commands-to-execute." + commandToExecute;
                OfflineCommandsUtils.sendMessage(sender, (color(plugin.getConfig().getString("variables.list-command-format")
                        .replaceAll("(?i)\\{executor}", plugin.getConfig().getString(path + ".execute_as"))
                        .replaceAll("(?i)\\{identifier}", commandToExecute)
                        .replaceAll("(?i)\\{command}", plugin.getConfig().getString(path + ".command")))), feedback);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        final int length = args.length;
        switch (length) {
            case 1:
                StringUtil.copyPartialMatches(args[0], BASE_ARGS, completions);
                break;
            case 2:
                if (args[0].equalsIgnoreCase("add")) {
                    StringUtil.copyPartialMatches(args[1], Arrays.asList("user=\"\"", "command=\"\"", "executor=\"\"", "no-feedback"), completions);
                } else if (args[0].equalsIgnoreCase("remove")) {
                    ConfigurationSection allUserMatch = plugin.getConfig().getConfigurationSection("users");
                    if (allUserMatch != null) {
                        StringUtil.copyPartialMatches(args[1], allUserMatch.getKeys(false), completions);
                    }
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("add")) {
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("user=\"\"", "command=\"\"", "executor=\"\"", "no-feedback"), completions);
                } else if (args[0].equalsIgnoreCase("remove")) {
                    ConfigurationSection userMatch = plugin.getConfig().getConfigurationSection("users." + args[1] + ".commands-to-execute");
                    if (userMatch != null) {
                        StringUtil.copyPartialMatches(args[2], userMatch.getKeys(false), completions);
                    }
                }
                break;
        }
        return completions;
    }
}
