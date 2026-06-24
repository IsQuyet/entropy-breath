package io.github.isquyet.entropybreath;

record AirDamageConfig(
        boolean enabled,
        int intervalTicks,
        int airThreshold,
        double amount,
        EntropyDamageType type,
        boolean resetAirAfterDamage,
        int resetAirTo
) {
    AirDamageConfig withAirThreshold(int airThreshold) {
        return new AirDamageConfig(enabled, intervalTicks, airThreshold, amount, type, resetAirAfterDamage, resetAirTo);
    }
}
