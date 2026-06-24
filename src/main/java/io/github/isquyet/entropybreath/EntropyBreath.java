package io.github.isquyet.entropybreath;

import io.github.isquyet.entropybreath.air.AirDrainController;
import io.github.isquyet.entropybreath.command.EntropyBreathCommand;
import io.github.isquyet.entropybreath.config.AirDrainConfig;
import io.github.isquyet.entropybreath.entropy.EntropyLookup;
import io.github.isquyet.entropybreath.entropy.EntropyLookupFactory;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class EntropyBreath extends JavaPlugin {
    private EntropyLookup entropyLookup;
    private AirDrainConfig airDrainConfig;
    private AirDrainController airDrainController;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        airDrainConfig = AirDrainConfig.load(getConfig(), getLogger());
        entropyLookup = EntropyLookupFactory.create(this);
        airDrainController = new AirDrainController(this, entropyLookup, airDrainConfig);
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

    public void reloadEntropyBreath() {
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
