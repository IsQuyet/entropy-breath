package io.github.isquyet.entropybreath.config;

public record DepletedAirConfig(DepletedAirMode mode, int fixedLoss) {
    public int airLoss(int entropyLoss) {
        return switch (mode) {
            case FIXED -> fixedLoss;
            case ENTROPY -> entropyLoss;
        };
    }
}
