package io.github.jochyoua.offlinecommands.commands;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.color;
import static io.github.jochyoua.offlinecommands.OfflineCommandsUtils.prepareCommand;

public class OfflineCommandExecutor implements CommandExecutor {
    private static final Pattern COMMAND_PATTERN = Pattern.compile("command=\"([^\"]*)\"");
    private static final Pattern EXECUTOR_PATTERN = Pattern.compile("executor=\"([^\"]*)\"");
    private static final Pattern USER_PATTERN = Pattern.compile("user=\"([^\"]*)\"");

    private final OfflineCommands plugin;

    public OfflineCommandExecutor(OfflineCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(helpCommand());
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list":
                return listCommand(sender);
            case "add":
                return addCommand(sender, args);
            case "remove":
                return removeCommand(sender, args);
            default:
            case "help":
                sender.sendMessage(helpCommand());
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
    private boolean addCommand(CommandSender sender, String... args) {
        StringBuilder commandStringBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            commandStringBuilder.append(" ").append(args[i]);
        }
        boolean noFeedback = commandStringBuilder.toString().contains("no-feedback");

        String user = null;
        String executor = null;
        String commandToAdd = null;

        Matcher executorMatcher = EXECUTOR_PATTERN.matcher(commandStringBuilder);
        while (executorMatcher.find()) {
            executor = executorMatcher.group(1);
        }

        Matcher commandMatcher = COMMAND_PATTERN.matcher(commandStringBuilder);
        while (commandMatcher.find()) {
            commandToAdd = commandMatcher.group(1);
        }

        Matcher userMatcher = USER_PATTERN.matcher(commandStringBuilder);
        while (userMatcher.find()) {
            user = userMatcher.group(1);
        }

        if (executor == null) {
            executor = "CONSOLE";
        }

        if (commandToAdd == null || user == null) {
            if (!noFeedback) {
                sender.sendMessage(color(plugin.getConfig().getString("variables.incorrect-syntax")));
            }
            return true;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(user);
        } catch (IllegalArgumentException ignored) {
            Player player = Bukkit.getPlayer(user);
            if (player == null) {
                if (!plugin.getConfig().getBoolean("settings.use-offline-player-fallback")) {
                    if (!noFeedback) {
                        sender.sendMessage(color(plugin.getConfig().getString("variables.player-does-not-exist")));
                    }
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
                if (!noFeedback) {
                    sender.sendMessage(color(plugin.getConfig().getString("variables.currently-online")));
                }
                return true;
            }
        }

        String commandIdentifier = UUID.randomUUID().toString().split("-")[0];
        String path = "users." + uuid + ".commands-to-execute." + commandIdentifier;

        plugin.getConfig().set(path + ".execute_as", executor);
        plugin.getConfig().set(path + ".command", commandToAdd);
        plugin.saveConfig();

        if (!noFeedback) {
            sender.sendMessage(color(plugin.getConfig().getString("variables.new-command-added")
                    .replaceAll("(?i)\\{uuid}", uuid.toString())
                    .replaceAll("(?i)\\{executor}", executor)
                    .replaceAll("(?i)\\{command}", commandToAdd)
                    .replaceAll("(?i)\\{identifier}", commandIdentifier)));
        }
        return true;
    }

    /**
     * Handles the remove command for the command sender.
     *
     * @param sender the user executing the command
     * @param args   the arguments from the command
     * @return if the command has failed
     */
    private boolean removeCommand(CommandSender sender, String... args) {
        if (args.length != 3) {
            sender.sendMessage(color(plugin.getConfig().getString("variables.incorrect-syntax")));
            return true;
        }

        sender.sendMessage(color(plugin.getConfig().getString("variables.identifier-search")
                .replaceAll("(?i)\\{uuid}", args[1])
                .replaceAll("(?i)\\{identifier}", args[1])));

        if (!plugin.getConfig().isSet("users." + args[1] + ".commands-to-execute." + args[2])) {
            sender.sendMessage(color(plugin.getConfig().getString("variables.identifier-not-found")));
            return true;
        }

        plugin.getConfig().set("users." + args[1] + ".commands-to-execute." + args[2], null);
        plugin.saveConfig();
        sender.sendMessage(color(plugin.getConfig().getString("variables.identifier-found")));
        return true;
    }

    /**
     * Shows a list of users who have commands saved to their UUID
     *
     * @param sender the user executing the command
     * @return if the command has failed
     */
    private boolean listCommand(CommandSender sender) {
        ConfigurationSection userConfigurationSection = plugin.getConfig().getConfigurationSection("users");
        if (userConfigurationSection == null) {
            sender.sendMessage(color(plugin.getConfig().getString("variables.no-users-found")));
            return true;
        }
        sender.sendMessage(color(plugin.getConfig().getString("variables.list-command-header")));
        Set<String> matchedUsers = userConfigurationSection.getKeys(false);
        if (matchedUsers.isEmpty()) {
            sender.sendMessage(color(plugin.getConfig().getString("variables.no-users-found")));
            return true;
        }
        for (String uuid :
                matchedUsers) {
            sender.sendMessage(color(plugin.getConfig().getString("variables.list-user-uuid").
                    replaceAll("(?i)\\{uuid}", uuid)));

            ConfigurationSection commandConfigurationSection = plugin.getConfig().getConfigurationSection("users." + uuid + ".commands-to-execute");

            if (commandConfigurationSection == null) {
                sender.sendMessage(color(plugin.getConfig().getString("variables.list-no-commands-found")));
                return true;
            }

            for (String commandToExecute :
                    commandConfigurationSection.getKeys(false)) {
                String path = "users." + uuid + ".commands-to-execute." + commandToExecute;
                sender.sendMessage(color(plugin.getConfig().getString("variables.list-command-format")
                        .replaceAll("(?i)\\{executor}", plugin.getConfig().getString(path + ".execute_as"))
                        .replaceAll("(?i)\\{identifier}", commandToExecute)
                        .replaceAll("(?i)\\{command}", plugin.getConfig().getString(path + ".command"))));
            }
        }
        return true;
    }
}
