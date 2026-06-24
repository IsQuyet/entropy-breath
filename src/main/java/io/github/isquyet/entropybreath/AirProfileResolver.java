package io.github.isquyet.entropybreath;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class AirProfileResolver {
    private final Map<UUID, Long> breathableSurfaceUntilTick = new HashMap<>();

    boolean usesActiveWaterProfile(Player player, long elapsedTicks) {
        return usesWaterProfile(player) && !usesBreathableSurfaceAirProfile(player, elapsedTicks);
    }

    boolean usesWaterProfile(Player player) {
        return isWaterBreathBlockingBlock(player.getEyeLocation().getBlock());
    }

    void markBreathableSurface(Player player, long elapsedTicks, int durationTicks) {
        long safeDurationTicks = Math.max(1L, durationTicks);
        breathableSurfaceUntilTick.put(player.getUniqueId(), elapsedTicks + safeDurationTicks);
    }

    boolean usesBreathableSurfaceAirProfile(Player player, long elapsedTicks) {
        Long untilTick = breathableSurfaceUntilTick.get(player.getUniqueId());
        return untilTick != null && untilTick >= elapsedTicks;
    }

    boolean isVanillaAirRecovery(Player player, int currentAir, int requestedAir) {
        int airIncrease = requestedAir - currentAir;
        return airIncrease > 0 && airIncrease <= 4 && requestedAir <= player.getMaximumAir();
    }

    String debugBreathingContext(Player player, long elapsedTicks) {
        Block feetBlock = player.getLocation().getBlock();
        Block eyeBlock = player.getEyeLocation().getBlock();
        return " inWater=" + player.isInWater()
                + " waterProfile=" + usesWaterProfile(player)
                + " breathableSurfaceAirProfile=" + usesBreathableSurfaceAirProfile(player, elapsedTicks)
                + " location=" + formatLocation(player.getLocation())
                + " feetBlock=" + feetBlock.getType()
                + " feetWaterlogged=" + isWaterlogged(feetBlock)
                + " eyeLocation=" + formatLocation(player.getEyeLocation())
                + " eyeBlock=" + eyeBlock.getType()
                + " eyeWaterlogged=" + isWaterlogged(eyeBlock)
                + " maxAir=" + player.getMaximumAir();
    }

    void remove(UUID playerId) {
        breathableSurfaceUntilTick.remove(playerId);
    }

    void clear() {
        breathableSurfaceUntilTick.clear();
    }

    private boolean isWaterBreathBlockingBlock(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }

        return switch (type) {
            case WATER, BUBBLE_COLUMN, KELP, KELP_PLANT, SEAGRASS, TALL_SEAGRASS -> true;
            default -> isWaterlogged(block);
        };
    }

    private boolean isWaterlogged(Block block) {
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName()
                + ":" + location.getBlockX()
                + "," + location.getBlockY()
                + "," + location.getBlockZ();
    }
}
