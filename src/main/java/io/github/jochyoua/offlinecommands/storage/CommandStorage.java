package io.github.jochyoua.offlinecommands.storage;

import com.google.common.eventbus.EventBus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.github.jochyoua.offlinecommands.VariableConstants.DEFAULT_COMMAND;

@Data
@Builder
@Jacksonized
public class CommandStorage implements ConfigurationSerializable {

    @Builder.Default
    private String identifier = UUID.randomUUID().toString().split("-")[0];

    @Builder.Default
    private Executor executor = Executor.CONSOLE;
    @Builder.Default
    private String commandValue = "me test";

    @Builder.Default
    private String message = "";

    @Builder.Default
    private String requiredPermission = "";

    @Builder.Default
    private SoundStorage soundStorage = null;


    /**
     * A static method that deserializes a map of strings and objects into a CommandStorage object.
     * It uses the builder pattern to create a new instance of CommandStorage with the values from the map.
     * If the map does not contain a value for a field, it uses the default value from the DEFAULT_COMMAND constant.
     * It also handles the case where the executor value is not a valid enum constant by using Executor.CONSOLE as the default.
     *
     * @param map a map of strings and objects that represents a serialized CommandStorage object
     * @return a new CommandStorage object with the values from the map
     */
    public static CommandStorage deserialize(Map<String, Object> map) {
        return CommandStorage.builder()
                .identifier(Optional.ofNullable((String) map.get("identifier"))
                        .orElse(DEFAULT_COMMAND.getIdentifier()))
                .executor(Optional.ofNullable(Executor.getEnum((String) map.get("executor")))
                        .orElse(DEFAULT_COMMAND.getExecutor()))
                .commandValue(Optional.ofNullable((String) map.get("commandValue"))
                        .orElse(DEFAULT_COMMAND.getCommandValue()))
                .message(Optional.ofNullable((String) map.get("message"))
                        .orElse(DEFAULT_COMMAND.getMessage()))
                .requiredPermission(Optional.ofNullable((String) map.get("requiredPermission"))
                        .orElse(DEFAULT_COMMAND.getRequiredPermission()))
                .soundStorage(Optional.ofNullable((SoundStorage) map.get("soundStorage"))
                        .orElse(DEFAULT_COMMAND.getSoundStorage()))
                .build();
    }

    /**
     * An overridden method that serializes a CommandStorage object into a map of strings and objects.
     * It uses the fields of the CommandStorage object as the keys and their values as the values of the map.
     * It also converts the executor field into a string by using its name() method.
     *
     * @return a map of strings and objects that represents a serialized CommandStorage object
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("identifier", identifier);
        map.put("executor", executor.name());
        map.put("commandValue", commandValue);
        map.put("message", message);
        map.put("requiredPermission", requiredPermission);
        map.put("soundStorage", soundStorage);
        return map;
    }

    public enum Executor {
        CONSOLE, PLAYER;

        /**
         * A static method that returns an Executor enum constant based on a string value.
         * It converts the string value to upper case and tries to match it with the enum constants.
         * If the string value is null or does not match any enum constant, it returns Executor.CONSOLE as the default.
         *
         * @param value a string value that represents an Executor enum constant
         * @return an Executor enum constant that matches the string value, or Executor.CONSOLE if none matches
         */
        public static Executor getEnum(String value) {
            if (value == null) {
                return Executor.CONSOLE;
            }
            try {
                return Executor.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return Executor.CONSOLE;
            }
        }
    }
}