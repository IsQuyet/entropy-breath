package io.github.isquyet.entropybreath.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EntropyBreathCommand implements BasicCommand {
    static final String LABEL = "entropybreath";

    private final EntropyBreathCommandContext context;
    private final List<EntropyBreathSubcommand> subcommands;
    private final Map<String, EntropyBreathSubcommand> subcommandsByName;

    public EntropyBreathCommand(EntropyBreathCommandContext context) {
        this.context = context;
        this.subcommands = List.of(
                new ReloadEntropyBreathCommand(context),
                new StatusEntropyBreathCommand(context)
        );
        this.subcommandsByName = indexSubcommands(subcommands);
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String @NonNull [] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        EntropyBreathSubcommand subcommand = subcommandsByName.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            sendHelp(sender);
            return;
        }

        subcommand.execute(source, args);
    }

    @Override
    public @NonNull Collection<@NonNull String> suggest(@NonNull CommandSourceStack source, @NonNull String @NonNull [] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            return availableRootCommands(sender);
        }
        if (args.length == 1) {
            return CommandUtils.filter(availableRootCommands(sender), args[0]);
        }

        EntropyBreathSubcommand subcommand = subcommandsByName.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null || !subcommand.canUse(sender)) {
            return List.of();
        }
        return subcommand.suggest(source, args);
    }

    @Override
    public boolean canUse(@NonNull CommandSender sender) {
        return subcommands.stream().anyMatch(subcommand -> subcommand.canUse(sender));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(context.messages().component(sender, "command.help-title"));
        for (EntropyBreathSubcommand subcommand : subcommands) {
            if (!subcommand.canUse(sender)) {
                continue;
            }
            for (String usage : subcommand.usages()) {
                sender.sendMessage(Component.text("/" + LABEL + " ", CommandUtils.ACCENT)
                        .append(context.messages().component(sender, usage)));
            }
        }
    }

    private List<String> availableRootCommands(CommandSender sender) {
        return subcommands.stream()
                .filter(subcommand -> subcommand.canUse(sender))
                .map(EntropyBreathSubcommand::name)
                .toList();
    }

    private Map<String, EntropyBreathSubcommand> indexSubcommands(List<EntropyBreathSubcommand> subcommands) {
        Map<String, EntropyBreathSubcommand> indexed = new LinkedHashMap<>();
        for (EntropyBreathSubcommand subcommand : subcommands) {
            indexed.put(subcommand.name(), subcommand);
        }
        return Map.copyOf(indexed);
    }
}
