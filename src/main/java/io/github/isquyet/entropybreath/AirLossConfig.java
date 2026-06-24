package io.github.isquyet.entropybreath;

import java.util.List;

record AirLossConfig(int intervalTicks, List<AirDrainTier> tiers) {
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
