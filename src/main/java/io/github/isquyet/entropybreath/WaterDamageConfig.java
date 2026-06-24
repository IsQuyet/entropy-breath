package io.github.isquyet.entropybreath;

record WaterDamageConfig(
        boolean enabled,
        int airThreshold,
        double amount,
        boolean preserveOverflowReset,
        int resetAirTo
) {
    WaterDamageConfig withAirThreshold(int airThreshold) {
        return new WaterDamageConfig(enabled, airThreshold, amount, preserveOverflowReset, resetAirTo);
    }
}
