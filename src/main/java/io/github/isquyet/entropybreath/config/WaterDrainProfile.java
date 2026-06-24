package io.github.isquyet.entropybreath.config;

public record WaterDrainProfile(
        boolean enabled,
        int minAir,
        AirLossConfig eventAirLoss,
        DepletedAirConfig depletedAirLoss,
        WaterDamageConfig drowningDamage
) {
    public int eventAirLossFor(int entropy) {
        return eventAirLoss.airLossFor(entropy);
    }
}
