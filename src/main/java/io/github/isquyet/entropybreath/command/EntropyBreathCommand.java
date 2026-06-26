package io.github.isquyet.entropybreath.command;

import io.github.isquyet.entropybreath.EntropyBreath;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class EntropyBreathCommand implements BasicCommand {
    private static final String RELOAD_PERMISSION = "entropybreath.command.reload";

    private final EntropyBreath plugin;

    public EntropyBreathCommand(EntropyBreath plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String @NonNull [] args) {
        CommandSender sender = source.getSender();
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            plugin.reloadEntropyBreath();
            sender.sendMessage("EntropyBreath configuration reloaded.");
            return;
        }

        sender.sendMessage("EntropyBreath commands:");
        sender.sendMessage("/entropybreath reload");
    }

    @Override
    public @NonNull Collection<@NonNull String> suggest(@NonNull CommandSourceStack source, @NonNull String @NonNull [] args) {
        if (args.length == 0) {
            return List.of("reload");
        }
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return "reload".startsWith(prefix) ? List.of("reload") : List.of();
    }

    @Override
    public boolean canUse(@NonNull CommandSender sender) {
        return sender.hasPermission(RELOAD_PERMISSION);
    }
}
