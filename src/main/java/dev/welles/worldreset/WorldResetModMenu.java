package dev.welles.worldreset;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/** "Config" button in Mod Menu's mod list. Only loaded by Mod Menu, so the mod works without it. */
public class WorldResetModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(
            parent,
            FabricLoader.getInstance().getConfigDir().resolve(WorldReset.MOD_ID + ".properties"),
            Component.literal(FabricLoader.getInstance().getModContainer(WorldReset.MOD_ID).map(mod -> mod.getMetadata().getName()).orElse(WorldReset.MOD_ID))
        );
    }
}
//?}
