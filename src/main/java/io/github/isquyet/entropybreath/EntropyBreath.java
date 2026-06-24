package io.github.isquyet.entropybreath;

import io.github.isquyet.entropycore.api.EntropyService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EntropyBreath extends JavaPlugin {
    private EntropyService entropyService;
    private AirDrainConfig airDrainConfig;
    private AirDrainController airDrainController;

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
        airDrainController = new AirDrainController(this, entropyService, airDrainConfig);
        getServer().getPluginManager().registerEvents(airDrainController, this);
        registerCommands();
        airDrainController.start();
    }

    @Override
    public void onDisable() {
        if (airDrainController != null) {
            airDrainController.stop();
            airDrainController = null;
        }
    }

    void reloadEntropyBreath() {
        reloadConfig();
        airDrainConfig = AirDrainConfig.load(getConfig(), getLogger());
        if (airDrainController != null) {
            airDrainController.reload(airDrainConfig);
        }
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                "entropybreath",
                "Reload EntropyBreath configuration.",
                java.util.List.of("ebreath"),
                new EntropyBreathCommand(this)
        ));
    }
}
