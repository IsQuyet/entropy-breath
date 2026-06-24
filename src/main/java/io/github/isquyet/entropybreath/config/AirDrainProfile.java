package io.github.isquyet.entropybreath.config;

public record AirDrainProfile(
        boolean enabled,
        boolean preventRegeneration,
        int minAir,
        AirLossConfig airLoss,
        DepletedAirConfig depletedAir,
        AirDamageConfig damage
) {
    public int airLossFor(int entropy) {
        return airLoss.airLossFor(entropy);
    }
}
