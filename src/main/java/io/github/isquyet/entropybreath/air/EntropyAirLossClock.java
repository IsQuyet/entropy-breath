package io.github.isquyet.entropybreath.air;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class EntropyAirLossClock {
    private final Map<UUID, Long> nextLossAtTick = new HashMap<>();

    boolean isDue(UUID playerId, long currentTick, int intervalTicks) {
        long nextTick = nextLossAtTick.computeIfAbsent(playerId, ignored -> currentTick + safeInterval(intervalTicks));
        return currentTick >= nextTick;
    }

    void markApplied(UUID playerId, long currentTick, int intervalTicks) {
        nextLossAtTick.put(playerId, currentTick + safeInterval(intervalTicks));
    }

    void remove(UUID playerId) {
        nextLossAtTick.remove(playerId);
    }

    void clear() {
        nextLossAtTick.clear();
    }

    private long safeInterval(int intervalTicks) {
        return Math.max(1L, intervalTicks);
    }
}
