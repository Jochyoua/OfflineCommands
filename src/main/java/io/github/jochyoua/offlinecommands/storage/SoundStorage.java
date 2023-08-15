package io.github.jochyoua.offlinecommands.storage;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class SoundStorage implements ConfigurationSerializable {

    @Builder.Default
    private Sound sound = Sound.BLOCK_NOTE_BLOCK_CHIME;

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
        String soundName = (String) map.get("sound");
        float volume = Float.parseFloat(map.get("volume").toString());
        float pitch = Float.parseFloat(map.get("pitch").toString());
        Sound sound;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {
            sound = Sound.BLOCK_NOTE_BLOCK_CHIME;
        }

        return SoundStorage.builder().sound(sound).volume(volume).pitch(pitch).build();
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
