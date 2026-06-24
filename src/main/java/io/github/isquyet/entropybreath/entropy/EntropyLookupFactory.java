package io.github.isquyet.entropybreath.entropy;

import org.bukkit.plugin.java.JavaPlugin;

public final class EntropyLookupFactory {
    static final String ENTROPY_CORE_PLUGIN_NAME = "EntropyCore";
    static final String ENTROPY_SERVICE_CLASS_NAME = "io.github.isquyet.entropycore.api.EntropyService";

    private EntropyLookupFactory() {
    }

    public static EntropyLookup create(JavaPlugin plugin) {
        return new EventDrivenEntropyLookup(plugin);
    }
}
