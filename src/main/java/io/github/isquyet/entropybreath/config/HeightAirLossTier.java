package io.github.isquyet.entropybreath.config;

public record HeightAirLossTier(int y, int airLoss) {
    public HeightAirLossTier {
        airLoss = Math.max(0, airLoss);
    }

    boolean matches(int playerY, int neutralY) {
        return y >= neutralY ? playerY >= y : playerY <= y;
    }
}
