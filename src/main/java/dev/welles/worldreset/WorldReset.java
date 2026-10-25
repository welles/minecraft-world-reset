package dev.welles.worldreset;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class WorldReset {
    public static final String MOD_ID = "worldreset";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** State of the currently loaded world, null while no world is loaded. */
    private static ResetData data;
    private static HomeTracker tracker;

    private WorldReset() {}

    public static void init(Path configDir) {
        ResetConfig.init(configDir);
        LOGGER.info("World Reset loaded");
    }

    /** Called before the dimensions of a world are loaded, while none of its chunks are in use. */
    public static void onLoadWorld(MinecraftServer server, LevelStorageSource.LevelStorageAccess storage) {
        long now = server.getWorldData().overworldData().getGameTime();
        data = ResetData.load(server.getWorldPath(LevelResource.ROOT), now);
        tracker = new HomeTracker();
        ChunkReset.resetExpiredChunks(storage, data, now);
        data.save();
    }

    public static void onTick(MinecraftServer server) {
        if (data != null) tracker.tick(server, data);
    }

    public static void onSave() {
        if (data != null) data.save();
    }

    public static void onStop() {
        data = null;
        tracker = null;
    }
}
