package io.github.isquyet.entropybreath;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

record AirDrainConfig(
        boolean enabled,
        int intervalTicks,
        boolean preventRegeneration,
        Set<GameMode> ignoredGameModes,
        boolean allowNegativeAir,
        int minAir,
        List<AirDrainTier> tiers
) {
    private static final int DEFAULT_INTERVAL_TICKS = 20;

    static AirDrainConfig load(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("air-drain");
        if (section == null) {
            logger.warning("Missing air-drain config section; using safe defaults.");
            return new AirDrainConfig(true, DEFAULT_INTERVAL_TICKS, true, defaultIgnoredGameModes(), true, -20, defaultTiers());
        }

        boolean enabled = section.getBoolean("enabled", true);
        int intervalTicks = Math.max(1, section.getInt("interval-ticks", DEFAULT_INTERVAL_TICKS));
        boolean preventRegeneration = section.getBoolean("prevent-regeneration", true);
        Set<GameMode> ignoredGameModes = loadIgnoredGameModes(section, logger);
        boolean allowNegativeAir = section.getBoolean("allow-negative-air", true);
        int minAir = section.getInt("min-air", -20);
        if (!allowNegativeAir && minAir < 0) {
            minAir = 0;
        }

        List<AirDrainTier> tiers = loadTiers(section, logger);
        return new AirDrainConfig(enabled, intervalTicks, preventRegeneration, ignoredGameModes, allowNegativeAir, minAir, tiers);
    }

    int airLossFor(int entropy) {
        int loss = 0;
        for (AirDrainTier tier : tiers) {
            if (entropy < tier.minEntropy()) {
                break;
            }
            loss = tier.airLoss();
        }
        return Math.max(0, loss);
    }

    boolean ignores(GameMode gameMode) {
        return ignoredGameModes.contains(gameMode);
    }

    private static Set<GameMode> loadIgnoredGameModes(ConfigurationSection section, Logger logger) {
        Set<GameMode> gameModes = EnumSet.noneOf(GameMode.class);
        for (String rawGameMode : section.getStringList("ignored-game-modes")) {
            try {
                gameModes.add(GameMode.valueOf(rawGameMode.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                logger.warning("Ignoring unknown game mode in air-drain.ignored-game-modes: " + rawGameMode);
            }
        }
        return Set.copyOf(gameModes.isEmpty() ? defaultIgnoredGameModes() : gameModes);
    }

    private static List<AirDrainTier> loadTiers(ConfigurationSection section, Logger logger) {
        List<AirDrainTier> tiers = new ArrayList<>();
        for (var tierMap : section.getMapList("tiers")) {
            Object minEntropyValue = tierMap.get("min-entropy");
            Object airLossValue = tierMap.get("air-loss");
            if (!(minEntropyValue instanceof Number minEntropy) || !(airLossValue instanceof Number airLoss)) {
                logger.warning("Skipping invalid air drain tier: " + tierMap);
                continue;
            }

            int safeMinEntropy = Math.max(1, minEntropy.intValue());
            int safeAirLoss = Math.max(0, airLoss.intValue());
            tiers.add(new AirDrainTier(safeMinEntropy, safeAirLoss));
        }

        if (tiers.isEmpty()) {
            logger.warning("No valid air drain tiers configured; using default tiers.");
            return defaultTiers();
        }

        return tiers.stream()
                .sorted(Comparator.comparingInt(AirDrainTier::minEntropy))
                .toList();
    }

    private static Set<GameMode> defaultIgnoredGameModes() {
        return Set.of(GameMode.CREATIVE, GameMode.SPECTATOR);
    }

    private static List<AirDrainTier> defaultTiers() {
        return List.of(
                new AirDrainTier(1, 1),
                new AirDrainTier(2, 2),
                new AirDrainTier(3, 4),
                new AirDrainTier(5, 7),
                new AirDrainTier(8, 12)
        );
    }
}
