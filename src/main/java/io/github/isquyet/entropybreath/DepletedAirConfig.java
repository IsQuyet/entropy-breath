package io.github.isquyet.entropybreath;

record DepletedAirConfig(DepletedAirMode mode, int fixedLoss) {
    int airLoss(int entropyLoss) {
        return switch (mode) {
            case FIXED -> fixedLoss;
            case ENTROPY -> entropyLoss;
        };
    }
}
