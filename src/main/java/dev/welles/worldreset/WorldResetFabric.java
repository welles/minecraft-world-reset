package dev.welles.worldreset;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class WorldResetFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        WorldReset.init(FabricLoader.getInstance().getConfigDir());
    }
}
//?}
