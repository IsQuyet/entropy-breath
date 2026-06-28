package io.github.isquyet.entropybreath.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.List;

final class ReloadEntropyBreathCommand implements EntropyBreathSubcommand {
    private static final String PERMISSION = "entropybreath.command.reload";

    private final EntropyBreathCommandContext context;

    ReloadEntropyBreathCommand(EntropyBreathCommandContext context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public List<String> usages() {
        return List.of("command.help-reload");
    }

    @Override
    public String permission() {
        return PERMISSION;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!canUse(sender)) {
            context.messages().send(sender, "command.no-permission");
            return;
        }

        context.reloadCallback().run();
        context.messages().send(sender, "command.reload-success");
    }
}
