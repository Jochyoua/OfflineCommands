package io.github.jochyoua.offlinecommands;


import io.github.jochyoua.offlinecommands.storage.CommandStorage;
import io.github.jochyoua.offlinecommands.storage.SoundStorage;

/**
 * A class that contains constants related to the offline commands plugin.
 * It defines the paths and keys used to access and store the user and command data in the file configurations.
 * It also defines a default command storage object that is used as a fallback value when deserializing commands.
 * <p>
 * This class cannot be instantiated, as it only serves as a container for constants.
 * </p>
 *
 * @author Jochyoua
 * @version 2.0
 * @since 2.0
 */
public class VariableConstants {

    public static final String SETTINGS_PATH = "settings";
    public static final String VARIABLES_PATH = "variables";
    public static final String USER_KEY = "user";
    public static final String USERS_KEY = "users";
    public static final String EXECUTOR_KEY = "executor";
    public static final String COMMAND_KEY = "command";

    public static final CommandStorage DEFAULT_COMMAND = CommandStorage.builder().build();
    public static final SoundStorage DEFAULT_SOUND = SoundStorage.builder().build();

    /**
     * Private constructor for VariableConstants class.
     * Throws an exception if an attempt is made to instantiate it.
     *
     * @throws UnsupportedOperationException if this constructor is invoked
     * @see java.lang.UnsupportedOperationException
     */
    private VariableConstants() {
        throw new UnsupportedOperationException("Cannot instantiate variable class.");
    }

}
