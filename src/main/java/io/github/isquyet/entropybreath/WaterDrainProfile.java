package io.github.isquyet.entropybreath;

record WaterDrainProfile(
        boolean enabled,
        int minAir,
        AirLossConfig eventAirLoss,
        DepletedAirConfig depletedAirLoss,
        WaterDamageConfig drowningDamage
) {
    int eventAirLossFor(int entropy) {
        return eventAirLoss.airLossFor(entropy);
    }
}
