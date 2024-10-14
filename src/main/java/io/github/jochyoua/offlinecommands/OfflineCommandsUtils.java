package io.github.jochyoua.offlinecommands;

import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

public final class OfflineCommandsUtils {

    private OfflineCommandsUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }

    /**
     * Replaces placeholders in a given string with values from a player object.
     * Uses PlaceholderAPI plugin if available and enabled, otherwise only replaces {playername} with player's name.
     *
     * @param string the string to process
     * @param player the player object to get values from
     * @return the string with placeholders replaced
     */
    public static String preparePlaceholders(String string, Player player) {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        string = string.replaceAll("(?i)\\{playername}", player.getName());

        if (placeholderAPI != null && placeholderAPI.isEnabled()) {
            return PlaceholderAPI.setPlaceholders(player, string);
        }
        return string;
    }

    /**
     * Sends a message to a command sender, optionally replacing placeholders with player values.
     * If feedback is false, does nothing. If sender is a player, uses preparePlaceholders method to process the message.
     *
     * @param sender            the command sender to send the message to
     * @param message           the message to send
     * @param isFeedbackEnabled whether to send the message or not
     */
    public static void sendMessage(CommandSender sender, String message, boolean isFeedbackEnabled) {
        if (!isFeedbackEnabled) {
            return;
        }
        if (sender instanceof Player) {
            message = preparePlaceholders(message, (Player) sender);
        }
        sender.sendMessage(message);
    }

    /**
     * Returns a map entry containing the UUID and the offline player object for a given UUID.
     * If the UUID corresponds to an online player, returns the player object instead of the offline player object.
     *
     * @param uuid the UUID to look up
     * @return a map entry with the UUID as the key and the offline player or player as the value, or null if the UUID is null
     */
    public static Map.Entry<UUID, OfflinePlayer> getDataFromUUID(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(uuid);
        OfflinePlayer offlinePlayer = (player != null) ? player : Bukkit.getOfflinePlayer(uuid);
        return new AbstractMap.SimpleEntry<>(offlinePlayer.getUniqueId(), offlinePlayer);
    }

    /**
     * Executes a command as a player or as the console, depending on the command storage object.
     * Checks the required permission and the executor of the command before dispatching it.
     * Optionally sends a message to the player before executing the command.
     *
     * @param player  the player to execute the command as or to send the message to
     * @param command the command storage object that contains the command value, message, executor, and required permission
     */
    public static void runCommandAsPlayer(Player player, CommandStorage command) {
        if (command.getRequiredPermission().isEmpty() || player.hasPermission(command.getRequiredPermission())) {
            if (!command.getMessage().isEmpty()) {
                sendMessage(player, applyChatColors(command.getMessage()), true);
            }

            if (command.getSoundStorage() != null) {
                command.getSoundStorage().playSoundForPlayer(player);
            }

            String preparedCommand = prepareCommand(command.getCommandValue(), player);
            if (command.getExecutor() == CommandStorage.Executor.CONSOLE) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), preparedCommand);
            } else if (command.getExecutor() == CommandStorage.Executor.PLAYER && player.isOnline()) {
                Bukkit.dispatchCommand(player, preparedCommand);
            }
        }
    }

    /**
     * Returns a map entry containing the UUID and the offline player object for a given username.
     * If the username corresponds to an online player, returns the player object instead of the offline player object.
     *
     * @param username the username to look up
     * @return a map entry with the UUID as the key and the offline player or player as the value, or null if the username is null
     */
    public static Map.Entry<UUID, OfflinePlayer> getDataFromUsername(String username) {
        if (username == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(username);
        OfflinePlayer offlinePlayer = (player != null) ? player : Bukkit.getOfflinePlayer(username);
        return new AbstractMap.SimpleEntry<>(offlinePlayer.getUniqueId(), offlinePlayer);
    }

    /**
     * Translates color codes in a given string using the ChatColor class.
     * Replaces '&amp;' characters with the color code character '\u00A7' before calling the translateAlternateColorCodes method.
     *
     * @param string the string to process
     * @return the string with color codes translated
     */
    public static String applyChatColors(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Prepares a command string by removing the leading slash and replacing placeholders with player values.
     * Uses the preparePlaceholders method to process the command string.
     *
     * @param command the command string to prepare
     * @param player  the player object to get values from
     * @return the prepared command string
     */
    public static String prepareCommand(String command, Player player) {
        command = command.startsWith("/") ? command.substring(1) : command;
        return preparePlaceholders(command, player);
    }

    /**
     * Returns the value of a given type from an array of arguments.
     * Searches for the type prefix followed by an equal sign and a double quote in the arguments.
     * Appends the argument values until it finds a closing double quote that is not escaped.
     * Replaces any escaped double quotes with unescaped ones in the returned value.
     *
     * @param valueType the type of the value to look for
     * @param args      the array of arguments to search in
     * @return the value of the given type, or null if not found
     * @author Phoenix616
     */
    public static String getValue(String valueType, String[] args) {
        StringBuilder value = new StringBuilder();

        boolean foundType = false;
        for (String arg : args) {
            int start = 0;
            if (!foundType) {
                String typePrefix = valueType + "=\"";
                if (arg.startsWith(typePrefix)) {
                    foundType = true;
                    start = typePrefix.length();
                }
            } else {
                value.append(' ');
            }
            if (foundType) {
                if (arg.endsWith("\"") && !arg.endsWith("\\\"")) {
                    value.append(arg, start, arg.length() - 1);
                    return value.toString().replace("\\\"", "\"");
                } else {
                    value.append(arg, start, arg.length());
                }
            }
        }

        return null;
    }
}