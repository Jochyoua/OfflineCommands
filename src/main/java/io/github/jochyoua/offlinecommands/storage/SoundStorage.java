package io.github.jochyoua.offlinecommands.storage;

import io.github.jochyoua.offlinecommands.VariableConstants;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Data
@Builder
@Jacksonized
public class SoundStorage implements ConfigurationSerializable {

    @Builder.Default
    private Sound sound = Bukkit.getVersion().contains("1.8") ? Sound.valueOf("NOTE_PIANO") : Sound.BLOCK_NOTE_BLOCK_CHIME;

    @Builder.Default
    private float volume = 1.0F;
    @Builder.Default
    private float pitch = 1.0F;

    /**
     * Deserializes a map of string and object pairs into a SoundStorage object.
     *
     * @param map The map containing the sound name, volume, and pitch as keys and their corresponding values as objects.
     * @return A SoundStorage object with the sound, volume, and pitch attributes set according to the map values.
     * @throws IllegalArgumentException If the sound name is not a valid Sound enum value.
     */
    public static SoundStorage deserialize(Map<String, Object> map) {
        if (map == null) {
            Bukkit.getLogger().log(Level.WARNING, "SoundStorage: map was null, using default sound.");
            return SoundStorage.builder()
                    .sound(VariableConstants.DEFAULT_SOUND.getSound())
                    .volume(1.0f)
                    .pitch(1.0f)
                    .build();
        }

        Object rawSoundObject = map.get("sound");
        float volume = parseFloatOrDefault(map.get("volume"), 1.0f);
        float pitch = parseFloatOrDefault(map.get("pitch"), 1.0f);

        Sound resolvedSound = VariableConstants.DEFAULT_SOUND.getSound();

        try {
            if (rawSoundObject instanceof Sound) {
                resolvedSound = (Sound) rawSoundObject;

            } else if (rawSoundObject instanceof String) {
                String soundName = (String) rawSoundObject;
                resolvedSound = Sound.valueOf(soundName.toUpperCase());

            } else if (rawSoundObject instanceof Map) {
                Map<?, ?> nestedMap = (Map<?, ?>) rawSoundObject;
                Object nestedSoundName = nestedMap.get("sound");

                if (nestedSoundName instanceof String) {
                    resolvedSound = Sound.valueOf(((String) nestedSoundName).toUpperCase());
                }

            }
        } catch (Exception exception) {
            Bukkit.getLogger().log(Level.WARNING, String.format("SoundStorage: invalid sound '%s', using default. Error: %s", rawSoundObject, exception.getMessage()));
            resolvedSound = VariableConstants.DEFAULT_SOUND.getSound();
        }

        return SoundStorage.builder()
                .sound(resolvedSound)
                .volume(volume)
                .pitch(pitch)
                .build();
    }


    private static float parseFloatOrDefault(Object o, float def) {
        if (o == null) return def;
        try {
            return Float.parseFloat(o.toString());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    /**
     * Serializes the SoundStorage object into a map of string and object pairs.
     *
     * @return A map containing the sound name, volume, and pitch as keys and their corresponding values as objects.
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("sound", sound.name());
        map.put("volume", volume);
        map.put("pitch", pitch);
        return map;
    }

    /**
     * Plays the sound for the given player if the player is online.
     *
     * @param player The player to play the sound for.
     */
    public void playSoundForPlayer(Player player) {
        if (player != null && player.isOnline()) {
            player.getWorld().playSound(player.getLocation(), this.sound, this.volume, this.pitch);
        }
    }

}
