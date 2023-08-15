package io.github.jochyoua.offlinecommands.storage;


import io.github.jochyoua.offlinecommands.OfflineCommandsUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.*;

@Data
@Builder
@Jacksonized
public class UserStorage implements ConfigurationSerializable {

    private UUID uuid;
    @Builder.Default
    private String username = "UNSET";
    private List<CommandStorage> commands;

    /**
     * A static method that deserializes a map of strings and objects into a UserStorage object.
     * It uses the builder pattern to create a new instance of UserStorage with the values from the map.
     * It also converts the list of maps that represents the commands of the user into a list of CommandStorage objects by using the CommandStorage.deserialize method.
     * It also parses the string that represents the UUID of the user into a UUID object by using the UUID.fromString method.
     *
     * @param map a map of strings and objects that represents a serialized UserStorage object
     * @return a new UserStorage object with the values from the map
     */
    public static UserStorage deserialize(Map<String, Object> map) {
        List<CommandStorage> commands = new ArrayList<>(((List<Map<String, Object>>) map.get("commands")).stream().map(CommandStorage::deserialize).toList());
        return UserStorage.builder().uuid(UUID.fromString((String) map.get("uuid"))).username((String) map.get("username")).commands(commands).build();
    }

    /**
     * An overridden method that serializes a UserStorage object into a map of strings and objects.
     * It uses the fields of the UserStorage object as the keys and their values as the values of the map.
     * It also converts the UUID field into a string by using its toString() method.
     * It also converts the list of CommandStorage objects that represents the commands of the user into a list of maps by using the CommandStorage.serialize method.
     *
     * @return a map of strings and objects that represents a serialized UserStorage object
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid.toString());
        map.put("username", username);
        map.put("commands", commands.stream().map(CommandStorage::serialize).toList());
        return map;
    }

    /**
     * A method that returns a command storage that matches a given identifier from the commands list of the user storage.
     * It uses a lambda stream to process the commands list and find the matching command storage.
     * It also uses the equalsIgnoreCase method of the string class to compare the identifiers.
     * If it finds a matching command storage, it returns it. Otherwise, it returns null.
     *
     * @param identifier a string identifier that identifies the command to be searched
     * @return a command storage that matches the identifier, or null if none matches
     */
    public CommandStorage getCommand(String identifier) {
        Optional<CommandStorage> offlineCommandOptional = commands.stream().filter(offlineCommand -> offlineCommand.getIdentifier().equalsIgnoreCase(identifier)).findFirst();
        return offlineCommandOptional.orElse(null);
    }

    /**
     * A method that runs all the commands stored in the user storage for a given player.
     * It takes a player as a parameter, and iterates over the commands list of the user storage to execute each command.
     * It also uses the OfflineCommandsUtils class to run the command as the player by using the runCommandAsPlayer method.
     *
     * @param player a player that represents the target of the commands
     */
    public void runAllCommands(Player player) {
        for (CommandStorage command : this.getCommands()) {
            OfflineCommandsUtils.runCommandAsPlayer(player, command);
        }
    }
}
