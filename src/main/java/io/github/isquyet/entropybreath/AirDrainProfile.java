package io.github.isquyet.entropybreath;

record AirDrainProfile(
        boolean enabled,
        boolean preventRegeneration,
        int minAir,
        AirLossConfig airLoss,
        DepletedAirConfig depletedAir,
        AirDamageConfig damage
) {
    int airLossFor(int entropy) {
        return airLoss.airLossFor(entropy);
    }
}
