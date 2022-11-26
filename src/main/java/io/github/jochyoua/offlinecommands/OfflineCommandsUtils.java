package io.github.jochyoua.offlinecommands;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class OfflineCommandsUtils {

    private OfflineCommandsUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }

    public static String preparePlaceholders(String string, Player player) {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        string = string.replaceAll("(?i)\\{playername}", player.getName());

        if (!(placeholderAPI != null && placeholderAPI.isEnabled())) {
            return string;
        }
        return PlaceholderAPI.setPlaceholders(player, string);
    }

    public static void sendMessage(CommandSender sender, String message, boolean feedback) {
        if (!feedback) {
            return;
        }
        if (sender instanceof Player) {
            message = preparePlaceholders(message, (Player) sender);
        }

        sender.sendMessage(message);
    }

    public static void logMessage(String message, String fileName) {
        StackTraceElement st = Thread.currentThread().getStackTrace()[2];
        Logger logger = Logger.getLogger(st.getClassName() + ":" + st.getLineNumber() + " " + Thread.currentThread().getStackTrace()[2].getMethodName());
        try {
            File file = new File(OfflineCommands.getPlugin(OfflineCommands.class).getDataFolder(), "logs/" + fileName + ".log");
            File pluginDirectory = OfflineCommands.getPlugin(OfflineCommands.class).getDataFolder();
            File directory = new File(OfflineCommands.getPlugin(OfflineCommands.class).getDataFolder(), "logs/");
            if (!pluginDirectory.exists() && !pluginDirectory.mkdirs()) {
                return;
            }
            if (!directory.exists() && !directory.mkdirs()) {
                return;
            }
            if (!file.exists() && !file.createNewFile()) {
                return;
            }
            FileHandler fileHandler = new FileHandler(file.getPath(), true);
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord logRecord) {
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(logRecord.getMillis()), ZoneId.systemDefault());
                    String source;
                    source = logRecord.getLoggerName();
                    String message = formatMessage(logRecord);
                    return String.format("%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%n",
                            zdt,
                            source,
                            logRecord.getLoggerName(),
                            logRecord.getLevel().getLocalizedName(),
                            message);
                }
            });

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.INFO);

            logger.log(Level.INFO, message + "\n");
            fileHandler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static String prepareCommand(String command, Player player) {
        command = command.startsWith("/") ? command.substring(1) : command;
        command = preparePlaceholders(command, player);
        return command;
    }

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
