package io.github.isquyet.entropybreath.air;

import io.github.isquyet.entropybreath.config.AirDrainConfig;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class BreathingProtection {
    boolean stopsAirLoss(Player player, AirDrainConfig config) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && config.waterBreathing().stopsAirLoss()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && config.conduitPower().stopsAirLoss()) {
            return true;
        }
        return hasNautilusBreath(player) && config.nautilusBreath().stopsAirLoss();
    }

    boolean stopsDamage(Player player, AirDrainConfig config) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && config.waterBreathing().stopsDamage()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && config.conduitPower().stopsDamage()) {
            return true;
        }
        return hasNautilusBreath(player) && config.nautilusBreath().stopsDamage();
    }

    boolean allowsAirRegeneration(Player player, AirDrainConfig config) {
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && config.waterBreathing().allowsRegeneration()) {
            return true;
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER) && config.conduitPower().allowsRegeneration()) {
            return true;
        }
        return hasNautilusBreath(player) && config.nautilusBreath().allowsRegeneration();
    }

    boolean hasNautilusBreath(Player player) {
        return player.hasPotionEffect(PotionEffectType.BREATH_OF_THE_NAUTILUS);
    }

    List<BreathingProtectionStatus> activeProtections(Player player) {
        List<BreathingProtectionStatus> protections = new ArrayList<>();
        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
            protections.add(new BreathingProtectionStatus(ProtectionType.WATER_BREATHING, 0));
        }
        if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)) {
            protections.add(new BreathingProtectionStatus(ProtectionType.CONDUIT_POWER, 0));
        }
        if (hasNautilusBreath(player)) {
            protections.add(new BreathingProtectionStatus(ProtectionType.NAUTILUS_BREATH, 0));
        }

        int respirationLevel = respirationLevel(player);
        if (respirationLevel > 0) {
            protections.add(new BreathingProtectionStatus(ProtectionType.RESPIRATION, respirationLevel));
        }
        return List.copyOf(protections);
    }

    int adjustForRespiration(Player player, int airLoss, boolean enabled) {
        if (!enabled || airLoss <= 0) {
            return airLoss;
        }

        int respirationLevel = respirationLevel(player);
        if (respirationLevel <= 0) {
            return airLoss;
        }

        int adjustedLoss = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int ignored = 0; ignored < airLoss; ignored++) {
            if (random.nextInt(respirationLevel + 1) == 0) {
                adjustedLoss++;
            }
        }
        return adjustedLoss;
    }

    private int respirationLevel(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet == null ? 0 : helmet.getEnchantmentLevel(Enchantment.RESPIRATION);
    }
}
