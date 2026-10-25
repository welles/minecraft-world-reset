package dev.welles.worldreset;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Fire altars: lit campfires on an altar block (see {@link ResetConfig#altarBlocks}) that protect a home area. */
public final class FireAltars {
    private FireAltars() {}

    /** The radius protected by a campfire with {@code below} under it, or -1 if it is not a fire altar. */
    public static int radius(BlockState state, BlockState below, ResetConfig config) {
        if (!(state.getBlock() instanceof CampfireBlock) || !state.getValue(CampfireBlock.LIT)) return -1;
        if (!config.soulCampfires && state.is(Blocks.SOUL_CAMPFIRE)) return -1;
        return config.altarRadius(normalize(Ids.of(below.getBlock())));
    }

    /** Copper oxidizes over time and can be waxed; every variant counts as the plain copper block. */
    private static String normalize(String blockId) {
        String id = blockId.replace("minecraft:waxed_", "minecraft:");
        return switch (id) {
            case "minecraft:exposed_copper", "minecraft:weathered_copper", "minecraft:oxidized_copper" -> "minecraft:copper_block";
            default -> id;
        };
    }

    /** Looks for altars in a loaded chunk and records them. */
    public static void scan(LevelChunk chunk, ResetData.Dimension dimension, ResetConfig config) {
        // ChunkPos became a record in 26.1, its block coordinates are available in every version
        int chunkX = chunk.getPos().getMinBlockX() >> 4;
        int chunkZ = chunk.getPos().getMinBlockZ() >> 4;

        List<ResetData.Altar> found = new ArrayList<>();
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            int radius = radius(chunk.getBlockState(pos), chunk.getBlockState(pos.below()), config);
            if (radius >= 0) found.add(new ResetData.Altar(pos.getX(), pos.getY(), pos.getZ(), radius));
        }

        List<ResetData.Altar> previous = dimension.altars(chunkX, chunkZ);
        if (Set.copyOf(previous).equals(Set.copyOf(found))) return;
        dimension.setAltars(chunkX, chunkZ, found);
    }
}
