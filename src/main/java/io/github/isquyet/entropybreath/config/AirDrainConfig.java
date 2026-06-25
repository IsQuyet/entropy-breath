package io.github.isquyet.entropybreath.config;

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

public record AirDrainConfig(
        boolean enabled,
        boolean debug,
        BreathingEffectConfig waterBreathing,
        BreathingEffectConfig conduitPower,
        BreathingEffectConfig nautilusBreath,
        boolean respirationReducesInAirLoss,
        boolean respirationReducesInWaterLoss,
        Set<GameMode> ignoredGameModes,
        HeightAirLossConfig heightAirLoss,
        AirDrainProfile inAir,
        WaterDrainProfile inWater
) {
    private static final int DEFAULT_AIR_LOSS_INTERVAL_TICKS = 20;

    public static AirDrainConfig load(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("entropy-breath");
        if (section == null) {
            logger.warning("Missing entropy-breath config section; using safe defaults.");
            return new AirDrainConfig(true, false, defaultBreathingEffect(), defaultBreathingEffect(), defaultNautilusBreathEffect(), true, false, defaultIgnoredGameModes(), defaultHeightAirLoss(), defaultInAirProfile(), defaultInWaterProfile());
        }

        boolean enabled = section.getBoolean("enabled", true);
        boolean debug = section.getBoolean("debug", false);
        ConfigurationSection protectionSection = section.getConfigurationSection("plugin-breathing-protection");
        BreathingEffectConfig waterBreathing = loadBreathingEffect(protectionSection, "water-breathing", defaultBreathingEffect());
        BreathingEffectConfig conduitPower = loadBreathingEffect(protectionSection, "conduit-power", defaultBreathingEffect());
        BreathingEffectConfig nautilusBreath = loadBreathingEffect(protectionSection, "nautilus-breath", defaultNautilusBreathEffect());
        boolean respirationReducesInAirLoss = true;
        boolean respirationReducesInWaterLoss = false;
        if (protectionSection != null) {
            respirationReducesInAirLoss = protectionSection.getBoolean("respiration.reduces-in-air-loss", true);
            respirationReducesInWaterLoss = protectionSection.getBoolean("respiration.reduces-in-water-loss", false);
        }
        Set<GameMode> ignoredGameModes = loadIgnoredGameModes(section, logger);
        HeightAirLossConfig heightAirLoss = loadHeightAirLoss(section.getConfigurationSection("height-air-loss"), defaultHeightAirLoss(), logger);
        AirDrainProfile inAir = loadAirProfile(section.getConfigurationSection("in-air"), defaultInAirProfile(), logger, "in-air");
        WaterDrainProfile inWater = loadWaterProfile(section.getConfigurationSection("in-water"), defaultInWaterProfile(), logger, "in-water");

        return new AirDrainConfig(enabled, debug, waterBreathing, conduitPower, nautilusBreath, respirationReducesInAirLoss, respirationReducesInWaterLoss, ignoredGameModes, heightAirLoss, inAir, inWater);
    }

    public boolean ignores(GameMode gameMode) {
        return ignoredGameModes.contains(gameMode);
    }

    private static Set<GameMode> loadIgnoredGameModes(ConfigurationSection section, Logger logger) {
        if (!section.contains("ignored-game-modes")) {
            return defaultIgnoredGameModes();
        }

        Set<GameMode> gameModes = EnumSet.noneOf(GameMode.class);
        for (String rawGameMode : section.getStringList("ignored-game-modes")) {
            try {
                gameModes.add(GameMode.valueOf(rawGameMode.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                logger.warning("Ignoring unknown game mode in entropy-breath.ignored-game-modes: " + rawGameMode);
            }
        }
        return Set.copyOf(gameModes);
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

    private static AirDrainProfile loadAirProfile(ConfigurationSection section, AirDrainProfile fallback, Logger logger, String path) {
        if (section == null) {
            logger.warning("Missing entropy-breath." + path + " config section; using defaults.");
            return fallback;
        }

        boolean enabled = section.getBoolean("enabled", fallback.enabled());
        boolean preventRegeneration = section.getBoolean("regeneration.prevent", fallback.preventRegeneration());
        int minAir = section.getInt("min-air", fallback.minAir());
        AirLossConfig airLoss = loadAirLoss(section, "air-loss", fallback.airLoss(), logger, path + ".air-loss");
        DepletedAirConfig depletedAir = loadDepletedAir(section.getConfigurationSection("depleted-air"), fallback.depletedAir(), logger, path);
        AirDamageConfig damage = loadDamage(section.getConfigurationSection("damage"), fallback.damage(), logger, path);
        if (damage.enabled() && damage.airThreshold() < minAir) {
            logger.warning("entropy-breath." + path + ".damage.air-threshold is below the minimum reachable air ("
                    + minAir + "); using " + minAir + " instead.");
            damage = damage.withAirThreshold(minAir);
        }
        return new AirDrainProfile(enabled, preventRegeneration, minAir, airLoss, depletedAir, damage);
    }

    private static WaterDrainProfile loadWaterProfile(ConfigurationSection section, WaterDrainProfile fallback, Logger logger, String path) {
        if (section == null) {
            logger.warning("Missing entropy-breath." + path + " config section; using defaults.");
            return fallback;
        }

        boolean enabled = section.getBoolean("enabled", fallback.enabled());
        int minAir = section.getInt("min-air", fallback.minAir());
        AirLossConfig eventAirLoss = loadAirLoss(section, "air-loss", fallback.eventAirLoss(), logger, path + ".air-loss");
        DepletedAirConfig depletedAirLoss = loadDepletedAir(
                section.getConfigurationSection("depleted-air-loss"),
                fallback.depletedAirLoss(),
                logger,
                path
        );
        WaterDamageConfig drowningDamage = loadWaterDamage(
                section.getConfigurationSection("drowning-damage"),
                fallback.drowningDamage()
        );
        if (drowningDamage.enabled() && drowningDamage.airThreshold() < minAir) {
            logger.warning("entropy-breath." + path + ".drowning-damage.air-threshold is below the minimum reachable air ("
                    + minAir + "); using " + minAir + " instead.");
            drowningDamage = drowningDamage.withAirThreshold(minAir);
        }
        return new WaterDrainProfile(enabled, minAir, eventAirLoss, depletedAirLoss, drowningDamage);
    }

    private static AirLossConfig loadAirLoss(ConfigurationSection parent, String childPath, AirLossConfig fallback, Logger logger, String path) {
        ConfigurationSection section = parent.getConfigurationSection(childPath);
        if (section == null) {
            logger.warning("Missing entropy-breath." + path + " config section; using defaults.");
            return fallback;
        }

        return new AirLossConfig(
                Math.max(1, section.getInt("interval-ticks", fallback.intervalTicks())),
                loadTiers(section, fallback.tiers(), logger, path)
        );
    }

    private static HeightAirLossConfig loadHeightAirLoss(ConfigurationSection section, HeightAirLossConfig fallback, Logger logger) {
        if (section == null) {
            return fallback;
        }

        return new HeightAirLossConfig(
                section.getBoolean("enabled", fallback.enabled()),
                section.getBoolean("applies-to.in-air", fallback.appliesInAir()),
                section.getBoolean("applies-to.in-water", fallback.appliesInWater()),
                section.getBoolean("regeneration.prevent-when-active", fallback.preventRegenerationWhenActive()),
                section.getInt("neutral-y", fallback.neutralY()),
                loadHeightAirLossTiers(section, fallback.tiers(), logger)
        );
    }

    private static List<HeightAirLossTier> loadHeightAirLossTiers(ConfigurationSection section, List<HeightAirLossTier> fallback, Logger logger) {
        List<HeightAirLossTier> tiers = new ArrayList<>();
        for (var tierMap : section.getMapList("tiers")) {
            Object yValue = tierMap.get("y");
            Object airLossValue = tierMap.get("amount");
            if (!(yValue instanceof Number y) || !(airLossValue instanceof Number airLoss)) {
                logger.warning("Skipping invalid height air loss tier in entropy-breath.height-air-loss: " + tierMap);
                continue;
            }

            tiers.add(new HeightAirLossTier(y.intValue(), Math.max(0, airLoss.intValue())));
        }

        if (tiers.isEmpty()) {
            logger.warning("No valid height air loss tiers configured in entropy-breath.height-air-loss; using defaults.");
            return fallback;
        }
        return List.copyOf(tiers);
    }

    private static DepletedAirConfig loadDepletedAir(ConfigurationSection section, DepletedAirConfig fallback, Logger logger, String path) {
        if (section == null) {
            return fallback;
        }

        return new DepletedAirConfig(
                loadDepletedAirMode(section.getString("mode"), fallback.mode(), logger, path),
                Math.max(0, section.getInt("fixed-loss", fallback.fixedLoss()))
        );
    }

    private static DepletedAirMode loadDepletedAirMode(String rawMode, DepletedAirMode fallback, Logger logger, String path) {
        if (rawMode == null || rawMode.isBlank()) {
            return fallback;
        }

        try {
            return DepletedAirMode.valueOf(rawMode.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            logger.warning("Unknown depleted air mode in entropy-breath." + path + ".depleted-air.mode: " + rawMode + "; using " + fallback.name().toLowerCase(Locale.ROOT) + ".");
            return fallback;
        }
    }

    private static AirDamageConfig loadDamage(ConfigurationSection section, AirDamageConfig fallback, Logger logger, String path) {
        if (section == null) {
            return fallback;
        }

        return new AirDamageConfig(
                section.getBoolean("enabled", fallback.enabled()),
                Math.max(1, section.getInt("interval-ticks", fallback.intervalTicks())),
                section.getInt("air-threshold", fallback.airThreshold()),
                Math.max(0.0D, section.getDouble("amount", fallback.amount())),
                loadDamageType(section.getString("type"), fallback.type(), logger, path),
                section.getBoolean("preserve-overflow-reset", fallback.preserveOverflowReset()),
                section.getInt("reset-air-to", fallback.resetAirTo())
        );
    }

    private static WaterDamageConfig loadWaterDamage(ConfigurationSection section, WaterDamageConfig fallback) {
        if (section == null) {
            return fallback;
        }

        return new WaterDamageConfig(
                section.getBoolean("enabled", fallback.enabled()),
                section.getInt("air-threshold", fallback.airThreshold()),
                Math.max(0.0D, section.getDouble("amount", fallback.amount())),
                section.getBoolean("preserve-overflow-reset", fallback.preserveOverflowReset()),
                section.getInt("reset-air-to", fallback.resetAirTo())
        );
    }

    private static EntropyDamageType loadDamageType(String rawType, EntropyDamageType fallback, Logger logger, String path) {
        if (rawType == null || rawType.isBlank()) {
            return fallback;
        }

        try {
            return EntropyDamageType.valueOf(rawType.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            logger.warning("Unknown damage type in entropy-breath." + path + ".damage.type: " + rawType + "; using " + fallback.name().toLowerCase(Locale.ROOT) + ".");
            return fallback;
        }
    }

    private static List<AirDrainTier> loadTiers(ConfigurationSection section, List<AirDrainTier> fallback, Logger logger, String path) {
        List<AirDrainTier> tiers = new ArrayList<>();
        for (var tierMap : section.getMapList("tiers")) {
            Object minEntropyValue = tierMap.get("min-entropy");
            Object airLossValue = tierMap.get("amount");
            if (!(minEntropyValue instanceof Number minEntropy) || !(airLossValue instanceof Number airLoss)) {
                logger.warning("Skipping invalid air drain tier in entropy-breath." + path + ": " + tierMap);
                continue;
            }

            int safeMinEntropy = Math.max(1, minEntropy.intValue());
            int safeAirLoss = Math.max(0, airLoss.intValue());
            tiers.add(new AirDrainTier(safeMinEntropy, safeAirLoss));
        }

        if (tiers.isEmpty()) {
            logger.warning("No valid air drain tiers configured in entropy-breath." + path + "; using defaults.");
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

    private static BreathingEffectConfig defaultNautilusBreathEffect() {
        return new BreathingEffectConfig(true, true, false);
    }

    private static AirDrainProfile defaultInAirProfile() {
        return new AirDrainProfile(true, true, -20, defaultAirLoss(), defaultFixedDepletedAir(), new AirDamageConfig(true, 20, -20, 2.0D, EntropyDamageType.SUFFOCATION, true, 0));
    }

    private static WaterDrainProfile defaultInWaterProfile() {
        return new WaterDrainProfile(false, -20, defaultAirLoss(), defaultEntropyDepletedAir(), new WaterDamageConfig(false, -20, 2.0D, true, 0));
    }

    private static HeightAirLossConfig defaultHeightAirLoss() {
        return new HeightAirLossConfig(true, true, false, true, 64, defaultHeightAirLossTiers());
    }

    private static AirLossConfig defaultAirLoss() {
        return new AirLossConfig(DEFAULT_AIR_LOSS_INTERVAL_TICKS, defaultTiers());
    }

    private static DepletedAirConfig defaultFixedDepletedAir() {
        return new DepletedAirConfig(DepletedAirMode.FIXED, 20);
    }

    private static DepletedAirConfig defaultEntropyDepletedAir() {
        return new DepletedAirConfig(DepletedAirMode.ENVIRONMENT, 20);
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

    private static List<HeightAirLossTier> defaultHeightAirLossTiers() {
        return List.of(
                new HeightAirLossTier(-64, 3),
                new HeightAirLossTier(-32, 2),
                new HeightAirLossTier(0, 1),
                new HeightAirLossTier(128, 1),
                new HeightAirLossTier(192, 2),
                new HeightAirLossTier(256, 3)
        );
    }
}
