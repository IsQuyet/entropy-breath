package io.github.isquyet.entropybreath;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntropyAirLossClockTest {
    @Test
    void firstLossIsDueAfterInterval() {
        EntropyAirLossClock clock = new EntropyAirLossClock();
        UUID playerId = UUID.randomUUID();

        assertFalse(clock.isDue(playerId, 0L, 20));
        assertFalse(clock.isDue(playerId, 19L, 20));
        assertTrue(clock.isDue(playerId, 20L, 20));
    }

    @Test
    void cadenceIsSharedUntilLossIsApplied() {
        EntropyAirLossClock clock = new EntropyAirLossClock();
        UUID playerId = UUID.randomUUID();

        assertFalse(clock.isDue(playerId, 0L, 20));

        // Simulates changing environment without applying entropy loss.
        assertTrue(clock.isDue(playerId, 20L, 20));
        assertTrue(clock.isDue(playerId, 21L, 20));

        clock.markApplied(playerId, 21L, 20);
        assertFalse(clock.isDue(playerId, 40L, 20));
        assertTrue(clock.isDue(playerId, 41L, 20));
    }

    @Test
    void removeResetsPlayerCadence() {
        EntropyAirLossClock clock = new EntropyAirLossClock();
        UUID playerId = UUID.randomUUID();

        assertFalse(clock.isDue(playerId, 0L, 20));
        assertTrue(clock.isDue(playerId, 20L, 20));

        clock.remove(playerId);
        assertFalse(clock.isDue(playerId, 20L, 20));
    }
}
