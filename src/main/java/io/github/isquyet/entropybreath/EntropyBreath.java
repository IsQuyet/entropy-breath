package io.github.isquyet.entropybreath;

import io.github.isquyet.entropybreath.air.AirDrainController;
import io.github.isquyet.entropybreath.command.EntropyBreathCommand;
import io.github.isquyet.entropybreath.config.AirDrainConfig;
import io.github.isquyet.entropybreath.entropy.EntropyLookup;
import io.github.isquyet.entropybreath.entropy.EntropyLookupFactory;
import io.github.isquyet.entropybreath.command.EntropyBreathCommandContext;
import io.github.isquyet.entropybreath.message.MessageService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class EntropyBreath extends JavaPlugin {
    private EntropyLookup entropyLookup;
    private AirDrainConfig airDrainConfig;
    private AirDrainController airDrainController;
    private MessageService messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new MessageService(this);
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
        messages.reload();
        airDrainConfig = AirDrainConfig.load(getConfig(), getLogger());
        if (airDrainController != null) {
            airDrainController.reload(airDrainConfig);
        }
    }

    private void registerCommands() {
        EntropyBreathCommandContext commandContext = new EntropyBreathCommandContext(messages, airDrainController, this::reloadEntropyBreath);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                "entropybreath",
                "Reload EntropyBreath configuration.",
                java.util.List.of("ebreath"),
                new EntropyBreathCommand(commandContext)
        ));
    }
}
