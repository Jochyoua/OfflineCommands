package io.github.jochyoua.offlinecommands.storage;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static io.github.jochyoua.offlinecommands.VariableConstants.USERS_KEY;


public class StorageUtils {

    private StorageUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }

    /**
     * A static method that returns a user storage that matches a given UUID from a user list stored in a file configuration.
     * It uses a lambda stream to process the user list and find the matching user storage.
     * It uses the USERS_KEY constant to access the user list from the file configuration, and the UserStorage class to represent each user.
     * It also uses the equalsIgnoreCase method of the string class to compare the UUIDs.
     * If it finds a matching user storage, it returns it. Otherwise, it returns null.
     *
     * @param config a file configuration that contains a user list
     * @param uuid   a UUID that identifies the user to be searched
     * @return a user storage that matches the UUID, or null if none matches
     */
    public static UserStorage getUser(FileConfiguration config, UUID uuid) {
        return ((List<UserStorage>) config.getList(USERS_KEY)).stream().filter(userStorage -> userStorage.getUuid().toString().equalsIgnoreCase(uuid.toString())).findFirst().orElse(null);
    }

    /**
     * A static method that adds or updates a user in a user list stored in a file configuration.
     * It takes a file configuration and a user storage as parameters, and returns a new list of user storages with the added or updated user.
     * It uses the USERS_KEY constant to access the user list from the file configuration, and the UserStorage class to represent each user.
     * It also uses the StorageUtils class to get the old user that matches the UUID of the user storage, and removes it from the list if it exists.
     * It then checks if the user storage has a commands field, and sets it to an empty list if it is null.
     * Finally, it adds the user storage to the list and returns it.
     *
     * @param config      a file configuration that contains a user list
     * @param userStorage a user storage that represents the user to be added or updated
     * @return a new list of user storages with the added or updated user
     */
    public static List<UserStorage> addOrUpdateUserList(FileConfiguration config, UserStorage userStorage) {
        List<UserStorage> userStorageList = new ArrayList<>((Collection<? extends UserStorage>) config.getList(USERS_KEY));
        UserStorage oldUser = StorageUtils.getUser(config, userStorage.getUuid());
        if (oldUser != null) {
            userStorageList.removeIf(removedUser -> removedUser.getUuid().toString().equalsIgnoreCase(userStorage.getUuid().toString()));
        }
        if (userStorage.getCommands() == null) {
            userStorage.setCommands(new ArrayList<>());
        }
        userStorageList.add(userStorage);
        return userStorageList;
    }

    /**
     * A static method that removes a user from a user list stored in a file configuration.
     * It takes a file configuration and a UUID as parameters, and returns a new list of user storages without the user that matches the UUID.
     * It uses the USERS_KEY constant to access the user list from the file configuration, and the UserStorage class to represent each user.
     * It also uses the removeIf method of the list to filter out the user that matches the UUID.
     *
     * @param config a file configuration that contains a user list
     * @param uuid   a UUID that identifies the user to be removed
     * @return a new list of user storages without the user that matches the UUID
     */
    public static List<UserStorage> removeUserFromUserList(FileConfiguration config, UUID uuid) {
        List<UserStorage> userStorageList = new ArrayList<>((Collection<? extends UserStorage>) config.getList(USERS_KEY));
        userStorageList.removeIf(removedUser -> removedUser.getUuid().equals(uuid));
        return userStorageList;
    }
}
