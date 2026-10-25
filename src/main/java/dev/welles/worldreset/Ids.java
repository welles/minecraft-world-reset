package dev.welles.worldreset;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Conversions between ids in the config and Minecraft's keys (ResourceLocation was renamed to Identifier in 1.21.11). */
public final class Ids {
    private Ids() {}

    public static String of(ResourceKey<?> key) {
        //? if >=1.21.11 {
        /*return key.identifier().toString();
        *///?} else {
        return key.location().toString();
        //?}
    }

    public static String of(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** The key of the dimension with the given id, or null if the id is invalid. */
    public static ResourceKey<Level> dimension(String id) {
        //? if >=1.21.11 {
        /*Identifier location = Identifier.tryParse(id);
        *///?} else {
        ResourceLocation location = ResourceLocation.tryParse(id);
        //?}
        return location == null ? null : ResourceKey.create(Registries.DIMENSION, location);
    }
}
