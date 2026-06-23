package io.github.isquyet.entropybreath;

import java.util.List;

record AirDrainProfile(
        boolean enabled,
        boolean preventRegeneration,
        boolean allowNegativeAir,
        int minAir,
        List<AirDrainTier> tiers,
        AirDamageConfig damage
) {
    int airLossFor(int entropy) {
        int loss = 0;
        for (AirDrainTier tier : tiers) {
            if (entropy < tier.minEntropy()) {
                break;
            }
            loss = tier.airLoss();
        }
        return Math.max(0, loss);
    }
}
