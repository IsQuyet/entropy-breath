package io.github.isquyet.entropybreath.config;

public record DepletedAirConfig(DepletedAirMode mode, int fixedLoss) {
    public int airLoss(int environmentLoss) {
        return switch (mode) {
            case FIXED -> fixedLoss;
            case ENVIRONMENT -> environmentLoss;
        };
    }
}
