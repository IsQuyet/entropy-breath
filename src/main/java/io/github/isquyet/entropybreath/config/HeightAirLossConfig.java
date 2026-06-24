package io.github.isquyet.entropybreath.config;

import java.util.List;

public record HeightAirLossConfig(
        boolean enabled,
        boolean appliesInAir,
        boolean appliesInWater,
        boolean preventRegenerationWhenActive,
        int neutralY,
        List<HeightAirLossTier> tiers
) {
    public HeightAirLossConfig {
        tiers = List.copyOf(tiers);
    }

    public int inAirLossFor(int y) {
        return enabled && appliesInAir ? airLossFor(y) : 0;
    }

    public int inWaterLossFor(int y) {
        return enabled && appliesInWater ? airLossFor(y) : 0;
    }

    int airLossFor(int y) {
        int loss = 0;
        for (HeightAirLossTier tier : tiers) {
            if (tier.matches(y, neutralY)) {
                loss = Math.max(loss, tier.airLoss());
            }
        }
        return loss;
    }
}
