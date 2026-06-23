package io.github.isquyet.entropybreath;

import io.github.isquyet.entropycore.api.EntropyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EntropyBreath extends JavaPlugin implements Listener {
    private EntropyService entropyService;
    private AirDrainConfig airDrainConfig;
    private BukkitTask drainTask;

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
        startDrainTask();
    }

    @Override
    public void onDisable() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityAirChange(EntityAirChangeEvent event) {
        if (entropyService == null || airDrainConfig == null || !airDrainConfig.enabled() || !airDrainConfig.preventRegeneration()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || airDrainConfig.ignores(player.getGameMode())) {
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

    private void startDrainTask() {
        if (drainTask != null) {
            drainTask.cancel();
        }
        if (!airDrainConfig.enabled()) {
            return;
        }

        drainTask = Bukkit.getScheduler().runTaskTimer(this, this::drainOnlinePlayers, airDrainConfig.intervalTicks(), airDrainConfig.intervalTicks());
    }

    private void drainOnlinePlayers() {
        if (entropyService == null || airDrainConfig == null || !airDrainConfig.enabled()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            drainPlayer(player);
        }
    }

    private void drainPlayer(Player player) {
        if (airDrainConfig.ignores(player.getGameMode())) {
            return;
        }

        int entropy = entropyService.getEntropy(player.getLocation());
        if (entropy <= 0) {
            return;
        }

        int airLoss = airDrainConfig.airLossFor(entropy);
        if (airLoss <= 0) {
            return;
        }

        int currentAir = player.getRemainingAir();
        int minimumAir = airDrainConfig.allowNegativeAir() ? airDrainConfig.minAir() : 0;
        player.setRemainingAir(Math.max(minimumAir, currentAir - airLoss));
    }
}
