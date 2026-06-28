package io.github.isquyet.entropybreath.air;

import java.util.List;

public record BreathingStatus(
        BreathingMode mode,
        int entropy,
        int y,
        int entropyAirLoss,
        int heightAirLoss,
        int environmentAirLoss,
        int intervalTicks,
        int currentAir,
        int maxAir,
        boolean drowningDamageGamerule,
        List<BreathingProtectionStatus> protections
) {
}
