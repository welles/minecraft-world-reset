package dev.welles.worldreset;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

/** Runs on the server thread while the world is loaded and records which chunks players visit. */
public final class HomeTracker {
    private static final int VISIT_INTERVAL = 100;

    public void tick(MinecraftServer server, ResetData data) {
        int tick = server.getTickCount();
        ResetConfig config = ResetConfig.get();
        // Visits are recorded even while the mod is disabled, so enabling it again does not reset visited areas
        if (tick % VISIT_INTERVAL == 0) trackVisits(server, data, config);
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
                    } else {
                        dimension.see(x, z, now);
                    }
                }
            }
        }
    }
}
