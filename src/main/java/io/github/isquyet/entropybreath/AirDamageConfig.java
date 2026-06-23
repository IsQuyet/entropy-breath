package io.github.isquyet.entropybreath;

record AirDamageConfig(boolean enabled, int intervalTicks, int airThreshold, double amount, EntropyDamageType type) {
    AirDamageConfig withAirThreshold(int airThreshold) {
        return new AirDamageConfig(enabled, intervalTicks, airThreshold, amount, type);
    }
}
