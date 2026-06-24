package io.github.isquyet.entropybreath.air;

import io.github.isquyet.entropybreath.config.AirDamageConfig;
import io.github.isquyet.entropybreath.config.EntropyDamageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AirMathTest {
    private static final AirDamageConfig DAMAGE = new AirDamageConfig(
            true,
            20,
            -20,
            2.0D,
            EntropyDamageType.SUFFOCATION,
            true,
            0
    );
    private static final AirDamageConfig DAMAGE_WITHOUT_OVERFLOW_RESET = new AirDamageConfig(
            true,
            20,
            -20,
            2.0D,
            EntropyDamageType.SUFFOCATION,
            false,
            0
    );

    @Test
    void clampsAirToConfiguredMinimum() {
        assertEquals(-20, AirMath.clampedAir(-20, -21));
        assertEquals(-1, AirMath.clampedAir(-20, -1));
    }

    @Test
    void effectiveAirUsesLowerTheoreticalValue() {
        assertEquals(-21, AirMath.effectiveAir(-20, -21));
        assertEquals(-20, AirMath.effectiveAir(-20, -18));
    }

    @Test
    void resetAirAfterDamagePreservesOverflowDebt() {
        assertEquals(0, AirMath.resetAirAfterDamage(DAMAGE, -20, -20));
        assertEquals(-1, AirMath.resetAirAfterDamage(DAMAGE, -20, -21));
        assertEquals(-20, AirMath.resetAirAfterDamage(DAMAGE, -20, -45));
    }

    @Test
    void resetAirAfterDamageCanDiscardOverflowDebt() {
        assertEquals(0, AirMath.resetAirAfterDamage(DAMAGE_WITHOUT_OVERFLOW_RESET, -20, -21));
    }

    @Test
    void overflowBelowThresholdOnlyCountsAirBelowThreshold() {
        assertEquals(0, AirMath.overflowBelowThreshold(DAMAGE, -20));
        assertEquals(1, AirMath.overflowBelowThreshold(DAMAGE, -21));
        assertEquals(0, AirMath.overflowBelowThreshold(DAMAGE, 0));
    }
}
