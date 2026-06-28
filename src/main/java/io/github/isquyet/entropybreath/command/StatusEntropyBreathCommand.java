package io.github.isquyet.entropybreath.command;

import io.github.isquyet.entropybreath.air.BreathingProtectionStatus;
import io.github.isquyet.entropybreath.air.BreathingStatus;
import io.github.isquyet.entropybreath.air.ProtectionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class StatusEntropyBreathCommand implements EntropyBreathSubcommand {
    private static final String PERMISSION = "entropybreath.command.status";

    private final EntropyBreathCommandContext context;

    StatusEntropyBreathCommand(EntropyBreathCommandContext context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public List<String> usages() {
        return List.of("command.help-status");
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
        if (!(sender instanceof Player player)) {
            context.messages().send(sender, "command.player-only");
            return;
        }

        BreathingStatus status = context.statusProvider().statusFor(player);
        context.messages().send(sender, "status.header");
        context.messages().send(sender, "status.line-mode", messageComponent(sender, "status.mode." + key(status.mode())));
        context.messages().send(sender, "status.line-air", status.currentAir(), status.maxAir());
        context.messages().send(sender, "status.line-interval", status.intervalTicks());
        context.messages().send(sender, "status.line-air-change", signedAirLoss(status.environmentAirLoss()));
        context.messages().send(sender, "status.line-sources", signedAirLoss(status.entropyAirLoss()), signedAirLoss(status.heightAirLoss()));
        context.messages().send(sender, "status.line-values", status.entropy(), status.y());
        context.messages().send(sender, "status.line-gamerule", status.drowningDamageGamerule());
        context.messages().send(sender, "status.line-protection", protections(sender, status.protections()));
    }

    private Component protections(CommandSender sender, List<BreathingProtectionStatus> protections) {
        if (protections.isEmpty()) {
            return messageComponent(sender, "status.protection.none");
        }

        Component result = Component.empty();
        for (int index = 0; index < protections.size(); index++) {
            if (index > 0) {
                result = result.append(Component.text(", "));
            }
            result = result.append(protection(sender, protections.get(index)));
        }
        return result;
    }

    private Component protection(CommandSender sender, BreathingProtectionStatus protection) {
        if (protection.type() == ProtectionType.RESPIRATION) {
            return messageComponent(sender, "status.protection.respiration", protection.level());
        }
        return messageComponent(sender, "status.protection." + key(protection.type()));
    }

    private Component messageComponent(CommandSender sender, String key, Object... args) {
        return context.messages().component(sender, key, args);
    }

    private String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String signedAirLoss(int airLoss) {
        return signed(-airLoss);
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

}
