package io.github.isquyet.entropybreath.air;

import io.github.isquyet.entropycore.api.EntropyService;
import io.github.isquyet.entropybreath.config.AirDamageConfig;
import io.github.isquyet.entropybreath.config.AirDrainConfig;
import io.github.isquyet.entropybreath.config.AirDrainProfile;
import io.github.isquyet.entropybreath.config.EntropyDamageType;
import io.github.isquyet.entropybreath.config.WaterDamageConfig;
import io.github.isquyet.entropybreath.config.WaterDrainProfile;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AirDrainController implements Listener {
    private static final long DEBUG_LOG_INTERVAL_TICKS = 20L;

    private final JavaPlugin plugin;
    private final EntropyService entropyService;
    private final AirProfileResolver profileResolver = new AirProfileResolver();
    private final BreathingProtection breathingProtection = new BreathingProtection();
    private final Map<UUID, Long> lastDamageAtTick = new HashMap<>();
    private final Map<UUID, Integer> lastObservedAir = new HashMap<>();
    private final Map<UUID, Integer> waterAirDebt = new HashMap<>();
    private final EntropyAirLossClock entropyAirLossClock = new EntropyAirLossClock();
    private final Map<UUID, Long> lastDebugAirChangeAtTick = new HashMap<>();
    private final Map<UUID, Long> lastDebugObservedAtTick = new HashMap<>();

    private AirDrainConfig config;
    private BukkitTask drainTask;
    private long elapsedTicks;
    private long configGeneration;

    public AirDrainController(JavaPlugin plugin, EntropyService entropyService, AirDrainConfig config) {
        this.plugin = plugin;
        this.entropyService = entropyService;
        this.config = config;
    }

    public void start() {
        startDrainTask();
    }

    public void stop() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
        clearState();
    }

    public void reload(AirDrainConfig config) {
        this.config = config;
        clearState();
        elapsedTicks = 0L;
        configGeneration++;
        startDrainTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityAirChange(EntityAirChangeEvent event) {
        if (config == null || !config.enabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (config.ignores(player.getGameMode())) {
            clearPlayerState(player.getUniqueId());
            return;
        }

        int currentAir = player.getRemainingAir();
        int requestedAir = event.getAmount();
        // Water loss is only applied when vanilla has already decided air should decrease.
        if (requestedAir < currentAir) {
            handleWaterAirDecrease(event, player, currentAir, requestedAir);
            return;
        }
        if (requestedAir == currentAir) {
            return;
        }

        if (profileResolver.usesWaterProfile(player)
                && !breathingProtection.allowsAirRegeneration(player, config)
                && profileResolver.isVanillaAirRecovery(player, currentAir, requestedAir)) {
            profileResolver.markBreathableSurface(player, elapsedTicks, 1);
        }

        if (profileResolver.usesActiveWaterProfile(player, elapsedTicks)) {
            debugAirChange(player, currentAir, requestedAir, "active-water-profile");
            return;
        }

        AirDrainProfile profile = config.inAir();
        if (!profile.enabled() || !profile.preventRegeneration()) {
            debugAirChange(player, currentAir, requestedAir, "profile-allows-regeneration");
            return;
        }
        if (breathingProtection.allowsAirRegeneration(player, config)) {
            debugAirChange(player, currentAir, requestedAir, "breathing-effect-allows-regeneration");
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            debugAirChange(player, currentAir, requestedAir, "no-entropy");
            return;
        }

        debugAirChange(player, currentAir, requestedAir, "blocked-regeneration entropy=" + entropy);
        event.setAmount(currentAir);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (config == null || !config.enabled() || event.getCause() != EntityDamageEvent.DamageCause.DROWNING) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (config.ignores(player.getGameMode())) {
            clearPlayerState(player.getUniqueId());
            return;
        }
        if (!profileResolver.usesActiveWaterProfile(player, elapsedTicks)) {
            return;
        }

        WaterDrainProfile profile = config.inWater();
        WaterDamageConfig damage = profile.drowningDamage();
        if (!profile.enabled() || !damage.enabled() || breathingProtection.stopsDamage(player, config)) {
            waterAirDebt.remove(player.getUniqueId());
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            return;
        }

        event.setDamage(damage.amount());

        if (!damage.preserveOverflowReset()) {
            waterAirDebt.remove(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();
        int debt = waterAirDebt.getOrDefault(playerId, 0)
                + AirMath.overflowBelowThreshold(damage.airThreshold(), player.getRemainingAir());
        waterAirDebt.remove(playerId);

        // Let vanilla finish its drowning reset first, then re-apply preserved overflow debt.
        long scheduledGeneration = configGeneration;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (scheduledGeneration != configGeneration || !player.isOnline() || player.isDead()) {
                return;
            }
            WaterDrainProfile currentProfile = config.inWater();
            WaterDamageConfig currentDamage = currentProfile.drowningDamage();
            if (!currentProfile.enabled() || !currentDamage.enabled() || !currentDamage.preserveOverflowReset()) {
                return;
            }
            int resetAir = AirMath.clampedAir(currentProfile.minAir(), currentDamage.resetAirTo() - debt);
            player.setRemainingAir(resetAir);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayerState(event.getPlayer().getUniqueId());
    }

    private void startDrainTask() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
        if (!config.enabled()) {
            return;
        }

        drainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::drainOnlinePlayers, 1L, 1L);
    }

    private void drainOnlinePlayers() {
        if (config == null || !config.enabled()) {
            return;
        }

        elapsedTicks += 1L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            debugObservedAirIncrease(player);
            drainPlayer(player);
            lastObservedAir.put(player.getUniqueId(), player.getRemainingAir());
        }
    }

    private void drainPlayer(Player player) {
        if (player.isDead()) {
            return;
        }
        if (config.ignores(player.getGameMode())) {
            clearPlayerState(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();
        if (profileResolver.usesActiveWaterProfile(player, elapsedTicks)) {
            WaterDamageConfig waterDamage = config.inWater().drowningDamage();
            if (!config.inWater().enabled() || !waterDamage.enabled() || !waterDamage.preserveOverflowReset() || breathingProtection.stopsDamage(player, config)) {
                waterAirDebt.remove(playerId);
            }
            return;
        }
        waterAirDebt.remove(playerId);

        AirDrainProfile profile = config.inAir();
        if (!profile.enabled()) {
            return;
        }

        boolean stopsAirLoss = breathingProtection.stopsAirLoss(player, config);
        boolean stopsDamage = breathingProtection.stopsDamage(player, config);
        AirDamageConfig damage = profile.damage();
        boolean lossDue = !stopsAirLoss && entropyAirLossClock.isDue(playerId, elapsedTicks, profile.airLoss().intervalTicks());
        boolean damageDue = !stopsDamage
                && damage.enabled()
                && damage.amount() > 0.0D
                && player.getRemainingAir() <= damage.airThreshold()
                && isDamageDue(playerId, damage);
        if (!lossDue && !damageDue) {
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            return;
        }

        int theoreticalAir = player.getRemainingAir();
        if (lossDue) {
            int currentAir = player.getRemainingAir();
            int entropyAirLoss = profile.airLossFor(entropy);
            int rawAirLoss = currentAir <= 0 ? profile.depletedAir().airLoss(entropyAirLoss) : entropyAirLoss;
            if (rawAirLoss > 0) {
                int airLoss = breathingProtection.adjustForRespiration(player, rawAirLoss, config.respirationReducesInAirLoss());
                theoreticalAir = currentAir - airLoss;
                if (airLoss > 0) {
                    player.setRemainingAir(AirMath.clampedAir(profile.minAir(), theoreticalAir));
                }
                entropyAirLossClock.markApplied(playerId, elapsedTicks, profile.airLoss().intervalTicks());
            }
        }

        if (!stopsDamage) {
            damageIfAirDepleted(player, profile, theoreticalAir);
        }
    }

    private void handleWaterAirDecrease(EntityAirChangeEvent event, Player player, int currentAir, int requestedAir) {
        if (!profileResolver.usesActiveWaterProfile(player, elapsedTicks)) {
            return;
        }

        WaterDrainProfile profile = config.inWater();
        UUID playerId = player.getUniqueId();
        if (!profile.enabled() || breathingProtection.stopsAirLoss(player, config)) {
            return;
        }
        if (!entropyAirLossClock.isDue(playerId, elapsedTicks, profile.eventAirLoss().intervalTicks())) {
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            return;
        }

        int entropyAirLoss = profile.eventAirLossFor(entropy);
        int rawAirLoss = currentAir <= 0 ? profile.depletedAirLoss().airLoss(entropyAirLoss) : entropyAirLoss;
        if (rawAirLoss <= 0) {
            entropyAirLossClock.markApplied(playerId, elapsedTicks, profile.eventAirLoss().intervalTicks());
            return;
        }

        int airLoss = config.respirationReducesInWaterLoss()
                ? breathingProtection.adjustForRespiration(player, rawAirLoss, config.respirationReducesInWaterLoss())
                : rawAirLoss;
        int theoreticalAir = currentAir - airLoss;
        if (airLoss > 0) {
            int clampedAir = AirMath.clampedAir(profile.minAir(), theoreticalAir);
            event.setAmount(clampedAir);

            WaterDamageConfig damage = profile.drowningDamage();
            int debt = Math.max(0, clampedAir - theoreticalAir);
            if (debt > 0 && damage.enabled() && damage.preserveOverflowReset()) {
                waterAirDebt.merge(playerId, debt, Integer::sum);
            }
        }
        entropyAirLossClock.markApplied(playerId, elapsedTicks, profile.eventAirLoss().intervalTicks());
    }

    private boolean isDamageDue(UUID playerId, AirDamageConfig damage) {
        Long lastDamageAt = lastDamageAtTick.get(playerId);
        return lastDamageAt == null || elapsedTicks - lastDamageAt >= damage.intervalTicks();
    }

    private void damageIfAirDepleted(Player player, AirDrainProfile profile, int theoreticalAir) {
        AirDamageConfig damage = profile.damage();
        int effectiveAir = AirMath.effectiveAir(player.getRemainingAir(), theoreticalAir);
        if (!damage.enabled() || damage.amount() <= 0.0D || effectiveAir > damage.airThreshold()) {
            return;
        }

        Long lastDamageAt = lastDamageAtTick.get(player.getUniqueId());
        if (lastDamageAt != null && elapsedTicks - lastDamageAt < damage.intervalTicks()) {
            return;
        }

        player.damage(damage.amount(), damageSourceFor(damage.type()));
        lastDamageAtTick.put(player.getUniqueId(), elapsedTicks);
        player.setRemainingAir(AirMath.resetAirAfterDamage(damage, profile.minAir(), theoreticalAir));
    }

    private void debugAirChange(Player player, int currentAir, int requestedAir, String reason) {
        int airIncrease = requestedAir - currentAir;
        if (!config.debug() || airIncrease <= 0 || isDebugThrottled(lastDebugAirChangeAtTick, player.getUniqueId())) {
            return;
        }

        plugin.getLogger().info("[debug] EntityAirChange player=" + player.getName()
                + " currentAir=" + currentAir
                + " requestedAir=" + requestedAir
                + " airIncrease=" + airIncrease
                + profileResolver.debugBreathingContext(player, elapsedTicks)
                + " waterBreathing=" + player.hasPotionEffect(PotionEffectType.WATER_BREATHING)
                + " conduitPower=" + player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)
                + " nautilusBreath=" + breathingProtection.hasNautilusBreath(player)
                + " reason=" + reason);
    }

    private void debugObservedAirIncrease(Player player) {
        if (!config.debug()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Integer previousAir = lastObservedAir.get(playerId);
        int currentAir = player.getRemainingAir();
        if (previousAir == null || currentAir <= previousAir) {
            return;
        }
        if (isDebugThrottled(lastDebugObservedAtTick, playerId)) {
            return;
        }

        plugin.getLogger().info("[debug] Observed air increase player=" + player.getName()
                + " previousAir=" + previousAir
                + " currentAir=" + currentAir
                + " airIncrease=" + (currentAir - previousAir)
                + profileResolver.debugBreathingContext(player, elapsedTicks)
                + " waterBreathing=" + player.hasPotionEffect(PotionEffectType.WATER_BREATHING)
                + " conduitPower=" + player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)
                + " nautilusBreath=" + breathingProtection.hasNautilusBreath(player));
    }

    private boolean isDebugThrottled(Map<UUID, Long> lastDebugAtTick, UUID playerId) {
        Long lastDebugAt = lastDebugAtTick.get(playerId);
        if (lastDebugAt != null && elapsedTicks - lastDebugAt < DEBUG_LOG_INTERVAL_TICKS) {
            return true;
        }

        lastDebugAtTick.put(playerId, elapsedTicks);
        return false;
    }

    private void clearPlayerState(UUID playerId) {
        lastDamageAtTick.remove(playerId);
        lastObservedAir.remove(playerId);
        waterAirDebt.remove(playerId);
        entropyAirLossClock.remove(playerId);
        lastDebugAirChangeAtTick.remove(playerId);
        lastDebugObservedAtTick.remove(playerId);
        profileResolver.remove(playerId);
    }

    private void clearState() {
        lastDamageAtTick.clear();
        lastObservedAir.clear();
        waterAirDebt.clear();
        entropyAirLossClock.clear();
        lastDebugAirChangeAtTick.clear();
        lastDebugObservedAtTick.clear();
        profileResolver.clear();
    }

    private DamageSource damageSourceFor(EntropyDamageType damageType) {
        DamageType bukkitDamageType = switch (damageType) {
            case DROWNING -> DamageType.DROWN;
            case SUFFOCATION -> DamageType.IN_WALL;
            case GENERIC -> DamageType.GENERIC;
        };
        return DamageSource.builder(bukkitDamageType).build();
    }
}
