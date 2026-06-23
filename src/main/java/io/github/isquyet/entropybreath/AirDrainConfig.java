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
        BreathingEffectConfig waterBreathing,
        BreathingEffectConfig conduitPower,
        boolean respirationReducesAirLoss,
        Set<GameMode> ignoredGameModes,
        AirDrainProfile inAir,
        AirDrainProfile inWater
) {
    private static final int DEFAULT_INTERVAL_TICKS = 20;

    static AirDrainConfig load(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("air-drain");
        if (section == null) {
            logger.warning("Missing air-drain config section; using safe defaults.");
            return new AirDrainConfig(true, DEFAULT_INTERVAL_TICKS, defaultBreathingEffect(), defaultBreathingEffect(), true, defaultIgnoredGameModes(), defaultInAirProfile(), defaultInWaterProfile());
        }

        boolean enabled = section.getBoolean("enabled", true);
        int intervalTicks = Math.max(1, section.getInt("interval-ticks", DEFAULT_INTERVAL_TICKS));
        ConfigurationSection protectionSection = section.getConfigurationSection("breathing-protection");
        BreathingEffectConfig waterBreathing = loadBreathingEffect(protectionSection, "water-breathing", defaultBreathingEffect());
        BreathingEffectConfig conduitPower = loadBreathingEffect(protectionSection, "conduit-power", defaultBreathingEffect());
        boolean respirationReducesAirLoss = protectionSection == null
                || protectionSection.getBoolean("respiration.reduces-air-loss", true);
        Set<GameMode> ignoredGameModes = loadIgnoredGameModes(section, logger);
        AirDrainProfile inAir = loadProfile(section.getConfigurationSection("in-air"), defaultInAirProfile(), logger, "in-air");
        AirDrainProfile inWater = loadProfile(section.getConfigurationSection("in-water"), defaultInWaterProfile(), logger, "in-water");

        return new AirDrainConfig(enabled, intervalTicks, waterBreathing, conduitPower, respirationReducesAirLoss, ignoredGameModes, inAir, inWater);
    }

    boolean ignores(GameMode gameMode) {
        return ignoredGameModes.contains(gameMode);
    }

    AirDrainProfile profileFor(boolean inWater) {
        return inWater ? this.inWater : inAir;
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

    private static BreathingEffectConfig loadBreathingEffect(ConfigurationSection section, String path, BreathingEffectConfig fallback) {
        if (section == null) {
            return fallback;
        }

        return new BreathingEffectConfig(
                section.getBoolean(path + ".stops-air-loss", fallback.stopsAirLoss()),
                section.getBoolean(path + ".stops-damage", fallback.stopsDamage()),
                section.getBoolean(path + ".allows-regeneration", fallback.allowsRegeneration())
        );
    }

    private static AirDrainProfile loadProfile(ConfigurationSection section, AirDrainProfile fallback, Logger logger, String path) {
        if (section == null) {
            logger.warning("Missing air-drain." + path + " config section; using defaults.");
            return fallback;
        }

        boolean enabled = section.getBoolean("enabled", fallback.enabled());
        boolean preventRegeneration = section.getBoolean("prevent-regeneration", fallback.preventRegeneration());
        boolean allowNegativeAir = section.getBoolean("allow-negative-air", fallback.allowNegativeAir());
        int minAir = section.getInt("min-air", fallback.minAir());
        if (!allowNegativeAir && minAir < 0) {
            minAir = 0;
        }
        List<AirDrainTier> tiers = loadTiers(section, fallback.tiers(), logger, path);
        AirDamageConfig damage = loadDamage(section.getConfigurationSection("damage"), fallback.damage());
        return new AirDrainProfile(enabled, preventRegeneration, allowNegativeAir, minAir, tiers, damage);
    }

    private static AirDamageConfig loadDamage(ConfigurationSection section, AirDamageConfig fallback) {
        if (section == null) {
            return fallback;
        }

        return new AirDamageConfig(
                section.getBoolean("enabled", fallback.enabled()),
                Math.max(1, section.getInt("interval-ticks", fallback.intervalTicks())),
                section.getInt("air-threshold", fallback.airThreshold()),
                Math.max(0.0D, section.getDouble("amount", fallback.amount()))
        );
    }

    private static List<AirDrainTier> loadTiers(ConfigurationSection section, List<AirDrainTier> fallback, Logger logger, String path) {
        List<AirDrainTier> tiers = new ArrayList<>();
        for (var tierMap : section.getMapList("tiers")) {
            Object minEntropyValue = tierMap.get("min-entropy");
            Object airLossValue = tierMap.get("air-loss");
            if (!(minEntropyValue instanceof Number minEntropy) || !(airLossValue instanceof Number airLoss)) {
                logger.warning("Skipping invalid air drain tier in air-drain." + path + ": " + tierMap);
                continue;
            }

            int safeMinEntropy = Math.max(1, minEntropy.intValue());
            int safeAirLoss = Math.max(0, airLoss.intValue());
            tiers.add(new AirDrainTier(safeMinEntropy, safeAirLoss));
        }

        if (tiers.isEmpty()) {
            logger.warning("No valid air drain tiers configured in air-drain." + path + "; using defaults.");
            return fallback;
        }

        return tiers.stream()
                .sorted(Comparator.comparingInt(AirDrainTier::minEntropy))
                .toList();
    }

    private static Set<GameMode> defaultIgnoredGameModes() {
        return Set.of(GameMode.CREATIVE, GameMode.SPECTATOR);
    }

    private static BreathingEffectConfig defaultBreathingEffect() {
        return new BreathingEffectConfig(true, true, true);
    }

    private static AirDrainProfile defaultInAirProfile() {
        return new AirDrainProfile(true, true, true, -20, defaultTiers(), new AirDamageConfig(true, 20, -20, 2.0D));
    }

    private static AirDrainProfile defaultInWaterProfile() {
        return new AirDrainProfile(false, false, true, -20, defaultTiers(), new AirDamageConfig(false, 20, -20, 2.0D));
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
