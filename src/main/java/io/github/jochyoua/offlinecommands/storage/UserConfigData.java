package io.github.jochyoua.offlinecommands.storage;

import io.github.jochyoua.offlinecommands.OfflineCommands;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.jochyoua.offlinecommands.VariableConstants.USERS_KEY;

public class UserConfigData {

    private final OfflineCommands offlineCommands;
    @Getter
    private FileConfiguration userStorageConfig;

    public UserConfigData(OfflineCommands offlineCommands) {
        this.offlineCommands = offlineCommands;
        loadConfig();
    }

    public void reloadConfig() {
        this.userStorageConfig = null;
        loadConfig();
    }

    /**
     * A method that loads the user data from a file configuration.
     * It checks if the userStorageData field is null, and if so, it creates a new file configuration from the users_data.yml file in the offlineCommands data folder.
     * If the users_data.yml file does not exist, it tries to create it and set an empty list as the value of the USERS_KEY constant in the file configuration.
     * It also saves the file configuration to the users_data.yml file after setting the value.
     * If an IOException occurs while creating or saving the file configuration, it prints the stack trace of the exception.
     */
    public void loadConfig() {
        if (userStorageConfig == null) {
            File usersDataFile = new File(offlineCommands.getDataFolder(), "users_data.yml");

            if (!usersDataFile.exists()) {
                try {
                    if (!usersDataFile.createNewFile()) {
                        offlineCommands.getLogger().warning("Failed to create user_data.yml file, do you have the correct permissions?");
                    }
                    userStorageConfig = YamlConfiguration.loadConfiguration(usersDataFile);
                    userStorageConfig.set(USERS_KEY, new ArrayList<>());
                    userStorageConfig.save(usersDataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                userStorageConfig = YamlConfiguration.loadConfiguration(usersDataFile);
            }
        }
    }

    /**
     * A synchronized method that modifies the user data stored in a file configuration.
     * It takes a list of user storages as a parameter, and sets it as the value of the USERS_KEY constant in the file configuration.
     * It also saves the file configuration to the users_data.yml file in the offlineCommands data folder.
     * If the file configuration is null, it logs a warning message and returns without modifying the data.
     * If an IOException occurs while saving the file configuration, it prints the stack trace of the exception.
     *
     * @param userStorageList a list of user storages that represents the new user data
     */
    public synchronized void modifyUsersData(List<UserStorage> userStorageList) {
        if (userStorageConfig != null) {
            userStorageConfig.set(USERS_KEY, userStorageList);
        } else {
            offlineCommands.getLogger().warning("users_data.yml is null, unable to modify data.");
            return;
        }

        try {
            userStorageConfig.save(new File(offlineCommands.getDataFolder(), "users_data.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
