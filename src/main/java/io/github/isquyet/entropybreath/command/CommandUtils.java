package io.github.isquyet.entropybreath.command;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class CommandUtils {
    static final TextColor ACCENT = NamedTextColor.AQUA;

    private CommandUtils() {
    }

    static List<String> filter(Collection<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
