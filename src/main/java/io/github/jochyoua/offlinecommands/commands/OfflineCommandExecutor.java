package io.github.jochyoua.offlinecommands.commands;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
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
        String helpCommand = "&7OfflineCommands's Help Page&7:" +
                "\n" +
                "&7CHEATSHEET:" +
                "\n" +
                " - &7<&6arg&7> &f= &7required arg" +
                "\n" +
                " - &7(&6arg&7) &f= &7optional arg " +
                "\n" +
                "&7  │" +
                "\n" +
                "&7  ├─ &8[&eofflinecommands list&8]" +
                "\n" +
                "&7  │  &fUsed to list all current users and their commands" +
                "\n" +
                "&7  │" +
                "\n" +
                "&7  ├─ &8[&eofflinecommands add &8<&6user=\"username/UUID\"&8> &8<&6command=\"command\"&8> &8(&6executor=\"CONSOLE/PLAYER\"&8)]" +
                "\n" +
                "&7  │  &fUsed add a command for a user." +
                "\n" +
                "&7  │" +
                "\n" +
                "&7  ├─ &8[&eofflinecommands remove &8<&6UUID&8> &8<&6identifier&8>]" +
                "\n" +
                "&7  │  &fUsed to remove a command from the config.";
        return color(helpCommand);
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
                sender.sendMessage(ChatColor.RED + "You must follow the correct syntax.");
                sender.sendMessage(ChatColor.RED + "Please make sure to provide a user and command.");
                sender.sendMessage(ChatColor.GRAY + "/offlinecommands help");
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
                        sender.sendMessage(ChatColor.RED + "The player you provided does not exist.");
                        sender.sendMessage(ChatColor.RED + "Please make sure to provide a valid user.");
                        sender.sendMessage(ChatColor.RED + "Attempt to use UUID if possible.");
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
                    sender.sendMessage(color("&7That user is currently online, executing now."));
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
            sender.sendMessage(color("&7New command has successfully been added."));
            sender.sendMessage(color("&7  ├─ &8[&e" + uuid + "&8]"));
            sender.sendMessage(color("&7  │  &8[Executor&8]&7: " + executor));
            sender.sendMessage(color("&7  │  &3&o" + commandIdentifier + " &r&f" + commandToAdd));
            sender.sendMessage(color("&7  │"));
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
            sender.sendMessage(ChatColor.RED + "You have provided incorrect syntax.");
            sender.sendMessage(ChatColor.GRAY + "/offlinecommands help");
            return true;
        }

        sender.sendMessage(color("&7Identifier &3" + args[2] + "&7 for uuid &e" + args[1]));

        if (!plugin.getConfig().isSet("users." + args[1] + ".commands-to-execute." + args[2])) {
            sender.sendMessage(color("&cIdentifier does not exist."));
            return true;
        }

        plugin.getConfig().set("users." + args[1] + ".commands-to-execute." + args[2], null);
        sender.sendMessage(color("&3Identifier has been removed."));
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
            sender.sendMessage(ChatColor.RED + "There are no users in the configuration to list. ");
            sender.sendMessage(ChatColor.RED + "Please ensure sure your config is set-up correctly.");
            return true;
        }
        sender.sendMessage(color("&7OfflineCommands's List Page&7:"));
        for (String uuid :
                userConfigurationSection.getKeys(false)) {
            sender.sendMessage(color("&7  ├─ &8[&e" + uuid + "&8]"));

            ConfigurationSection commandConfigurationSection = plugin.getConfig().getConfigurationSection("users." + uuid + ".commands-to-execute");

            if (commandConfigurationSection == null) {
                sender.sendMessage(ChatColor.RED + "This user has no commands to execute.");
                sender.sendMessage(ChatColor.RED + "Please ensure sure your config is set-up correctly.");
                return true;
            }

            int iterated = 1;
            for (String commandToExecute :
                    commandConfigurationSection.getKeys(false)) {
                String path = "users." + uuid + ".commands-to-execute." + commandToExecute;
                sender.sendMessage(color("&7  │  &8[Executor&8]&7: " + plugin.getConfig().getString(path + ".execute_as")));
                sender.sendMessage(color("&7  │  &3&o" + commandToExecute + " &r&f" + plugin.getConfig().getString(path + ".command")));
                if (iterated != commandConfigurationSection.getKeys(false).size()) {
                    sender.sendMessage(color("&7  │"));
                }
                iterated++;
            }
        }
        return true;
    }
}
