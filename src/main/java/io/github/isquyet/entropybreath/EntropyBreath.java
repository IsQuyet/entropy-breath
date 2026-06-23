package io.github.isquyet.entropybreath;

import io.github.isquyet.entropycore.api.EntropyService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EntropyBreath extends JavaPlugin implements Listener {
    private EntropyService entropyService;
    private AirDrainConfig airDrainConfig;
    private BukkitTask drainTask;
    private final Map<UUID, Long> lastDamageAtTick = new HashMap<>();
    private long elapsedTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        airDrainConfig = AirDrainConfig.load(getConfig(), getLogger());

        RegisteredServiceProvider<EntropyService> provider = Bukkit.getServicesManager().getRegistration(EntropyService.class);
        if (provider == null) {
            getLogger().severe("EntropyCore is required but EntropyService is unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        entropyService = provider.getProvider();
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
        startDrainTask();
    }

    @Override
    public void onDisable() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
        lastDamageAtTick.clear();
    }

    void reloadEntropyBreath() {
        reloadConfig();
        airDrainConfig = AirDrainConfig.load(getConfig(), getLogger());
        lastDamageAtTick.clear();
        elapsedTicks = 0L;
        startDrainTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityAirChange(EntityAirChangeEvent event) {
        if (entropyService == null || airDrainConfig == null || !airDrainConfig.enabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || airDrainConfig.ignores(player.getGameMode())) {
            return;
        }

        AirDrainProfile profile = airDrainConfig.profileFor(player.isInWater());
        if (!profile.enabled() || !profile.preventRegeneration() || allowsAirRegeneration(player)) {
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            return;
        }

        int currentAir = player.getRemainingAir();
        if (event.getAmount() > currentAir) {
            event.setAmount(currentAir);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastDamageAtTick.remove(event.getPlayer().getUniqueId());
    }

    private void startDrainTask() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
        if (!airDrainConfig.enabled()) {
            return;
        }

        drainTask = Bukkit.getScheduler().runTaskTimer(this, this::drainOnlinePlayers, airDrainConfig.intervalTicks(), airDrainConfig.intervalTicks());
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                "entropybreath",
                "Reload EntropyBreath configuration.",
                java.util.List.of("ebreath"),
                new EntropyBreathCommand(this)
        ));
    }

    private void drainOnlinePlayers() {
        if (entropyService == null || airDrainConfig == null || !airDrainConfig.enabled()) {
            return;
        }

        elapsedTicks += airDrainConfig.intervalTicks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            drainPlayer(player);
        }
    }

    private void drainPlayer(Player player) {
        if (player.isDead() || airDrainConfig.ignores(player.getGameMode())) {
            return;
        }

        AirDrainProfile profile = airDrainConfig.profileFor(player.isInWater());
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
            int airLoss = adjustForRespiration(player, profile.airLossFor(entropy));
            if (airLoss > 0) {
                int currentAir = player.getRemainingAir();
                int minimumAir = profile.allowNegativeAir() ? profile.minAir() : 0;
                player.setRemainingAir(Math.max(minimumAir, currentAir - airLoss));
            }
        }

        if (!stopsDamage) {
            damageIfAirDepleted(player, profile);
        }
    }

    private boolean stopsAirLoss(Player player) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && airDrainConfig.waterBreathing().stopsAirLoss()) {
            return true;
        }
        return player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && airDrainConfig.conduitPower().stopsAirLoss();
    }

    private boolean stopsDamage(Player player) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && airDrainConfig.waterBreathing().stopsDamage()) {
            return true;
        }
        return player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && airDrainConfig.conduitPower().stopsDamage();
    }

    private boolean allowsAirRegeneration(Player player) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && airDrainConfig.waterBreathing().allowsRegeneration()) {
            return true;
        }
        return player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && airDrainConfig.conduitPower().allowsRegeneration();
    }

    private int adjustForRespiration(Player player, int airLoss) {
        if (!airDrainConfig.respirationReducesAirLoss() || airLoss <= 0) {
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
