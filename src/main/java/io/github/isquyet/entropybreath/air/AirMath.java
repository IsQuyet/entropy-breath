package io.github.isquyet.entropybreath.air;

import io.github.isquyet.entropybreath.config.AirDamageConfig;

final class AirMath {
    private AirMath() {
    }

    static int clampedAir(int minAir, int theoreticalAir) {
        return Math.max(minAir, theoreticalAir);
    }

    static int effectiveAir(int observedAir, int theoreticalAir) {
        return Math.min(observedAir, theoreticalAir);
    }

    static int resetAirAfterDamage(AirDamageConfig damage, int minAir, int theoreticalAir) {
        int overflow = damage.preserveOverflowReset()
                ? Math.max(0, damage.airThreshold() - theoreticalAir)
                : 0;
        return clampedAir(minAir, damage.resetAirTo() - overflow);
    }

    static int overflowBelowThreshold(AirDamageConfig damage, int theoreticalAir) {
        return overflowBelowThreshold(damage.airThreshold(), theoreticalAir);
    }

    static int overflowBelowThreshold(int airThreshold, int theoreticalAir) {
        return Math.max(0, airThreshold - theoreticalAir);
    }
}
