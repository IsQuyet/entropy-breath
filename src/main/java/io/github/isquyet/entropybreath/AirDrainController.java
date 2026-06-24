package io.github.isquyet.entropybreath;

import io.github.isquyet.entropycore.api.EntropyService;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class AirDrainController implements Listener {
    private static final long DEBUG_LOG_INTERVAL_TICKS = 20L;

    private final JavaPlugin plugin;
    private final EntropyService entropyService;
    private final AirProfileResolver profileResolver = new AirProfileResolver();
    private final Map<UUID, Long> lastDamageAtTick = new HashMap<>();
    private final Map<UUID, Integer> lastObservedAir = new HashMap<>();
    private final Map<UUID, Long> lastDebugAirChangeAtTick = new HashMap<>();
    private final Map<UUID, Long> lastDebugObservedAtTick = new HashMap<>();

    private AirDrainConfig config;
    private BukkitTask drainTask;
    private long elapsedTicks;

    AirDrainController(JavaPlugin plugin, EntropyService entropyService, AirDrainConfig config) {
        this.plugin = plugin;
        this.entropyService = entropyService;
        this.config = config;
    }

    void start() {
        startDrainTask();
    }

    void stop() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
        clearState();
    }

    void reload(AirDrainConfig config) {
        this.config = config;
        clearState();
        elapsedTicks = 0L;
        startDrainTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityAirChange(EntityAirChangeEvent event) {
        if (config == null || !config.enabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || config.ignores(player.getGameMode())) {
            return;
        }

        int currentAir = player.getRemainingAir();
        int requestedAir = event.getAmount();
        if (requestedAir <= currentAir) {
            return;
        }

        if (profileResolver.usesWaterProfile(player)
                && !allowsAirRegeneration(player)
                && profileResolver.isVanillaAirRecovery(player, currentAir, requestedAir)) {
            profileResolver.markBreathableSurface(player, elapsedTicks, config.intervalTicks());
        }

        AirDrainProfile profile = profileResolver.profileFor(player, config, elapsedTicks);
        if (!profile.enabled() || !profile.preventRegeneration()) {
            debugAirChange(player, currentAir, requestedAir, "profile-allows-regeneration");
            return;
        }
        if (allowsAirRegeneration(player)) {
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastDamageAtTick.remove(playerId);
        lastObservedAir.remove(playerId);
        lastDebugAirChangeAtTick.remove(playerId);
        lastDebugObservedAtTick.remove(playerId);
        profileResolver.remove(playerId);
    }

    private void startDrainTask() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
        if (!config.enabled()) {
            return;
        }

        drainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::drainOnlinePlayers, config.intervalTicks(), config.intervalTicks());
    }

    private void drainOnlinePlayers() {
        if (config == null || !config.enabled()) {
            return;
        }

        elapsedTicks += config.intervalTicks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            debugObservedAirIncrease(player);
            drainPlayer(player);
            lastObservedAir.put(player.getUniqueId(), player.getRemainingAir());
        }
    }

    private void drainPlayer(Player player) {
        if (player.isDead() || config.ignores(player.getGameMode())) {
            return;
        }

        AirDrainProfile profile = profileResolver.profileFor(player, config, elapsedTicks);
        if (!profile.enabled()) {
            return;
        }

        boolean stopsAirLoss = stopsAirLoss(player);
        boolean stopsDamage = stopsDamage(player);
        if (stopsAirLoss && stopsDamage) {
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            return;
        }

        if (!stopsAirLoss) {
            int currentAir = player.getRemainingAir();
            int entropyAirLoss = profile.airLossFor(entropy);
            int rawAirLoss = currentAir <= 0 ? profile.depletedAir().airLoss(entropyAirLoss) : entropyAirLoss;
            int airLoss = adjustForRespiration(player, rawAirLoss);
            if (airLoss > 0) {
                int minimumAir = profile.allowNegativeAir() ? profile.minAir() : 0;
                player.setRemainingAir(Math.max(minimumAir, currentAir - airLoss));
            }
        }

        if (!stopsDamage) {
            damageIfAirDepleted(player, profile);
        }
    }

    private boolean stopsAirLoss(Player player) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && config.waterBreathing().stopsAirLoss()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && config.conduitPower().stopsAirLoss()) {
            return true;
        }
        return hasNautilusBreath(player) && config.nautilusBreath().stopsAirLoss();
    }

    private boolean stopsDamage(Player player) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && config.waterBreathing().stopsDamage()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && config.conduitPower().stopsDamage()) {
            return true;
        }
        return hasNautilusBreath(player) && config.nautilusBreath().stopsDamage();
    }

    private boolean allowsAirRegeneration(Player player) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && config.waterBreathing().allowsRegeneration()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && config.conduitPower().allowsRegeneration()) {
            return true;
        }
        return hasNautilusBreath(player) && config.nautilusBreath().allowsRegeneration();
    }

    private boolean hasNautilusBreath(Player player) {
        return player.hasPotionEffect(PotionEffectType.BREATH_OF_THE_NAUTILUS);
    }

    private int adjustForRespiration(Player player, int airLoss) {
        if (!config.respirationReducesAirLoss() || airLoss <= 0) {
            return airLoss;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        int respirationLevel = helmet == null ? 0 : helmet.getEnchantmentLevel(Enchantment.RESPIRATION);
        if (respirationLevel <= 0) {
            return airLoss;
        }

        int adjustedLoss = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int ignored = 0; ignored < airLoss; ignored++) {
            if (random.nextInt(respirationLevel + 1) == 0) {
                adjustedLoss++;
            }
        }
        return adjustedLoss;
    }

    private void damageIfAirDepleted(Player player, AirDrainProfile profile) {
        AirDamageConfig damage = profile.damage();
        if (!damage.enabled() || damage.amount() <= 0.0D || player.getRemainingAir() > damage.airThreshold()) {
            return;
        }

        Long lastDamageAt = lastDamageAtTick.get(player.getUniqueId());
        if (lastDamageAt != null && elapsedTicks - lastDamageAt < damage.intervalTicks()) {
            return;
        }

        player.damage(damage.amount(), damageSourceFor(damage.type()));
        lastDamageAtTick.put(player.getUniqueId(), elapsedTicks);
        if (damage.resetAirAfterDamage()) {
            player.setRemainingAir(damage.resetAirTo());
        }
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
                + " nautilusBreath=" + hasNautilusBreath(player)
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
                + " nautilusBreath=" + hasNautilusBreath(player));
    }

    private boolean isDebugThrottled(Map<UUID, Long> lastDebugAtTick, UUID playerId) {
        Long lastDebugAt = lastDebugAtTick.get(playerId);
        if (lastDebugAt != null && elapsedTicks - lastDebugAt < DEBUG_LOG_INTERVAL_TICKS) {
            return true;
        }

        lastDebugAtTick.put(playerId, elapsedTicks);
        return false;
    }

    private void clearState() {
        lastDamageAtTick.clear();
        lastObservedAir.clear();
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
