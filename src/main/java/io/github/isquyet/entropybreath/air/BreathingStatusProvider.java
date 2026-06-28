package io.github.isquyet.entropybreath.air;

import org.bukkit.entity.Player;

public interface BreathingStatusProvider {
    BreathingStatus statusFor(Player player);
}
