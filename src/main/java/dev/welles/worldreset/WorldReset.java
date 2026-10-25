package dev.welles.worldreset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class WorldReset {
    public static final String MOD_ID = "worldreset";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private WorldReset() {}

    public static void init(Path configDir) {
        ResetConfig.init(configDir);
        LOGGER.info("World Reset loaded");
    }
}
