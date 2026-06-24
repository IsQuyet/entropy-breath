package io.github.isquyet.entropybreath.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HeightAirLossConfigTest {
    @Test
    void tiersCanCoverHighAndLowYLevelsInOneList() {
        HeightAirLossConfig config = new HeightAirLossConfig(true, true, false, true, 64, List.of(
                new HeightAirLossTier(0, 1),
                new HeightAirLossTier(-32, 2),
                new HeightAirLossTier(128, 1),
                new HeightAirLossTier(192, 2)
        ));

        assertEquals(2, config.inAirLossFor(-40));
        assertEquals(1, config.inAirLossFor(0));
        assertEquals(0, config.inAirLossFor(64));
        assertEquals(1, config.inAirLossFor(128));
        assertEquals(2, config.inAirLossFor(220));
    }

    @Test
    void overlappingTiersUseHighestAmount() {
        HeightAirLossConfig config = new HeightAirLossConfig(true, true, false, true, 64, List.of(
                new HeightAirLossTier(128, 1),
                new HeightAirLossTier(160, 3),
                new HeightAirLossTier(192, 2)
        ));

        assertEquals(1, config.inAirLossFor(140));
        assertEquals(3, config.inAirLossFor(180));
        assertEquals(3, config.inAirLossFor(200));
        assertEquals(3, config.inAirLossFor(240));
    }

    @Test
    void profileSwitchesControlWhereHeightLossApplies() {
        HeightAirLossConfig config = new HeightAirLossConfig(true, true, false, true, 64, List.of(
                new HeightAirLossTier(128, 1)
        ));

        assertEquals(1, config.inAirLossFor(128));
        assertEquals(0, config.inWaterLossFor(128));
    }

    @Test
    void disabledConfigReturnsNoLoss() {
        HeightAirLossConfig config = new HeightAirLossConfig(false, true, true, true, 64, List.of(
                new HeightAirLossTier(128, 1)
        ));

        assertEquals(0, config.inAirLossFor(256));
        assertEquals(0, config.inWaterLossFor(256));
    }
}
