package io.github.isquyet.entropybreath.config;

import java.util.List;

public record AirLossConfig(int intervalTicks, List<AirDrainTier> tiers) {
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
