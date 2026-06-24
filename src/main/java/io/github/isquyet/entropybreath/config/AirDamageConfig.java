package io.github.isquyet.entropybreath.config;

public record AirDamageConfig(
        boolean enabled,
        int intervalTicks,
        int airThreshold,
        double amount,
        EntropyDamageType type,
        boolean preserveOverflowReset,
        int resetAirTo
) {
    AirDamageConfig withAirThreshold(int airThreshold) {
        return new AirDamageConfig(enabled, intervalTicks, airThreshold, amount, type, preserveOverflowReset, resetAirTo);
    }
}
