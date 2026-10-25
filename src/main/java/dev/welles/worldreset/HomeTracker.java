package dev.welles.worldreset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Runs on the server thread while the world is loaded: records visits, finds fire altars and informs players. */
public final class HomeTracker {
    private static final int VISIT_INTERVAL = 100;
    private static final int HOME_INTERVAL = 20;
    private static final int BORDER_INTERVAL = 10;
    /** Border particles are only sent within this horizontal distance of the player. */
    private static final int BORDER_DISTANCE = 48;

    /** The area of an altar whose border is shown to a player until {@code untilTick}. */
    private record Border(ServerLevel level, int minX, int minZ, int maxX, int maxZ, ParticleOptions particle, int untilTick) {}

    /** Whether a player was in a protected area in the given dimension at the last check. */
    private record Location(String dimension, boolean home) {}

    private final Map<UUID, Location> atHome = new HashMap<>();
    private final Map<UUID, Border> borders = new HashMap<>();

    public void tick(MinecraftServer server, ResetData data) {
        int tick = server.getTickCount();
        ResetConfig config = ResetConfig.get();
        // Visits are recorded even while the mod is disabled, so enabling it again does not reset visited areas
        if (tick % VISIT_INTERVAL == 0) trackVisits(server, data, config);
        if (!config.enabled) return;
        if (tick % HOME_INTERVAL == 0) updateHomeMessages(server, data, config);
        if (tick % BORDER_INTERVAL == 0) showBorders(server);
    }

    private static void trackVisits(MinecraftServer server, ResetData data, ResetConfig config) {
        long now = server.overworld().getGameTime();
        int viewDistance = server.getPlayerList().getViewDistance();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = (ServerLevel) player.level();
            ResetData.Dimension dimension = data.dimension(Ids.of(level.dimension()));
            int centerX = player.getBlockX() >> 4;
            int centerZ = player.getBlockZ() >> 4;

            int radius = Math.max(viewDistance, config.visitRadius);
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null) continue;
                    if (Math.abs(x - centerX) <= config.visitRadius && Math.abs(z - centerZ) <= config.visitRadius) {
                        dimension.visit(x, z, now);
                        FireAltars.scan(level, chunk, dimension, config);
                    } else {
                        dimension.see(x, z, now);
                    }
                }
            }
        }
    }

    private void updateHomeMessages(MinecraftServer server, ResetData data, ResetConfig config) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            String dimensionId = Ids.of(player.level().dimension());
            // Areas that never reset are always home
            boolean home = !config.dimensions.contains(dimensionId)
                || data.dimension(dimensionId).isProtected(player.getBlockX() >> 4, player.getBlockZ() >> 4);

            // Changing the dimension is not entering or leaving a home
            Location previous = atHome.put(player.getUUID(), new Location(dimensionId, home));
            if (previous == null || !previous.dimension().equals(dimensionId) || previous.home() == home || !config.showMessages) continue;
            String message = home ? config.enterHomeMessage : config.leaveHomeMessage.replace("{days}", String.valueOf(config.resetAfterDays));
            player.sendSystemMessage(Component.literal(message), true);
        }
        atHome.keySet().retainAll(online);
    }

    private void showBorders(MinecraftServer server) {
        int tick = server.getTickCount();
        borders.entrySet().removeIf(entry -> {
            Border border = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level() != border.level() || tick > border.untilTick()) return true;

            double y = player.getY() + 1;
            for (int x = border.minX(); x <= border.maxX(); x++) {
                spawn(player, border, x, y, border.minZ());
                spawn(player, border, x, y, border.maxZ());
            }
            for (int z = border.minZ() + 1; z < border.maxZ(); z++) {
                spawn(player, border, border.minX(), y, z);
                spawn(player, border, border.maxX(), y, z);
            }
            return false;
        });
    }

    private static void spawn(ServerPlayer player, Border border, double x, double y, double z) {
        if (Math.abs(player.getX() - x) > BORDER_DISTANCE || Math.abs(player.getZ() - z) > BORDER_DISTANCE) return;
        //? if >=1.21.4 {
        /*border.level().sendParticles(player, border.particle(), true, false, x, y, z, 1, 0, 0, 0, 0);
        *///?} else {
        border.level().sendParticles(player, border.particle(), true, x, y, z, 1, 0, 0, 0, 0);
        //?}
    }

    /**
     * Called when a player uses a campfire without an item. Shows the border of a fire altar, or explains how to
     * build one. Returns whether the interaction was handled.
     */
    public boolean useCampfire(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, ResetData data) {
        ResetConfig config = ResetConfig.get();
        if (!config.enabled) return false;

        int radius = FireAltars.radius(state, level.getBlockState(pos.below()), config);
        if (radius < 0) {
            if (config.showMessages) player.sendSystemMessage(Component.literal(config.altarHintMessage), true);
            return true;
        }

        // Record the altar right away instead of waiting for the next scan
        LevelChunk chunk = level.getChunkAt(pos);
        FireAltars.scan(level, chunk, data.dimension(Ids.of(level.dimension())), config);
        if (config.showMessages) {
            player.sendSystemMessage(Component.literal(config.altarInfoMessage.replace("{size}", String.valueOf(FireAltars.size(radius)))), true);
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ParticleOptions particle = state.is(Blocks.SOUL_CAMPFIRE) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        borders.put(player.getUUID(), new Border(level,
            (chunkX - radius) * 16, (chunkZ - radius) * 16, (chunkX + radius + 1) * 16, (chunkZ + radius + 1) * 16,
            particle, level.getServer().getTickCount() + config.borderSeconds * 20));
        return true;
    }
}
