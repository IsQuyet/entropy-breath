package io.github.isquyet.entropybreath.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

interface EntropyBreathSubcommand {
    String name();

    List<String> usages();

    String permission();

    void execute(CommandSourceStack source, String[] args);

    default Collection<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    default boolean canUse(CommandSender sender) {
        return sender.hasPermission(permission());
    }
}
