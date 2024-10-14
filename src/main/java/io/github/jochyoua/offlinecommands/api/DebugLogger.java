package io.github.jochyoua.offlinecommands.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class DebugLogger {

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;
    private final Plugin plugin;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DebugLogger(Plugin plugin) {
        this.plugin = plugin;
    }

    public void log(Level level, String message) {
        if (level.equals(Level.WARNING)) {
            Bukkit.getLogger().warning(message);
        }
        String timeStamp = LocalDateTime.now().format(formatter);
        StackTraceElement element = getCallerDetails();

        String logMessage = String.format("%s [%s] %s.%s(%s:%d) -%n %s", timeStamp, level.getName(),
                element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber(), message);

        writeLog(level.getName(), logMessage);
        purgeOldLogs();
    }

    private StackTraceElement getCallerDetails() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return stackTrace[3];
    }

    private void writeLog(String level, String message) {
        File logDir = new File(plugin.getDataFolder(), "debug");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        File logFile = new File(logDir, level + ".log");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void purgeOldLogs() {
        File logDir = new File(plugin.getDataFolder(), "debug");
        if (!logDir.exists()) return;

        File[] files = logDir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    FileTime fileTime = Files.getLastModifiedTime(Paths.get(file.getPath()));
                    if (System.currentTimeMillis() - fileTime.toMillis() > ONE_DAY_MILLIS) {
                        Files.delete(file.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
