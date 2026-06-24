package io.github.isquyet.entropybreath.entropy;

import org.bukkit.Location;

enum NoEntropyLookup implements EntropyLookup {
    INSTANCE;

    @Override
    public int getEntropy(Location location) {
        return 0;
    }
}
