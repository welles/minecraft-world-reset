package dev.welles.worldreset;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deletes expired chunks from the region files before the world is loaded, so Minecraft generates them again.
 *
 * <p>A region file ({@code r.<x>.<z>.mca}) holds 32x32 chunks and starts with two 4 KiB tables: the location of each
 * chunk in the file and the time it was saved. A chunk whose location is 0 does not exist, which is exactly what
 * vanilla's {@code RegionFile.clear} writes. Chunks too big for the region file live next to it in
 * {@code c.<x>.<z>.mcc}.
 */
public final class ChunkReset {
    private static final Pattern REGION_FILE = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    /** Chunk blocks, entities and points of interest (beds, workstations, ...) are stored in separate region files. */
    private static final String[] STORAGES = {"region", "entities", "poi"};
    private static final long TICKS_PER_DAY = 24000;

    private ChunkReset() {}

    public static void resetExpiredChunks(LevelStorageSource.LevelStorageAccess storage, ResetData data, long now) {
        ResetConfig config = ResetConfig.get();
        if (!config.enabled) return;

        long maxAge = config.resetAfterDays * TICKS_PER_DAY;
        for (String id : config.dimensions) {
            ResourceKey<Level> key = Ids.dimension(id);
            if (key == null) {
                WorldReset.LOGGER.warn("Invalid dimension in config: {}", id);
                continue;
            }

            Path dir = storage.getDimensionPath(key);
            try {
                int count = resetDimension(dir, data, data.dimension(id), now, maxAge, config.dryRun);
                if (count > 0) {
                    WorldReset.LOGGER.info(config.dryRun ? "Would reset {} chunks in {} (dry run)" : "Reset {} chunks in {}", count, id);
                }
            } catch (IOException e) {
                WorldReset.LOGGER.error("Could not reset chunks in {}", id, e);
            }
        }
    }

    private static int resetDimension(Path dir, ResetData data, ResetData.Dimension dimension, long now, long maxAge, boolean dryRun) throws IOException {
        Path regionDir = dir.resolve("region");
        if (!Files.isDirectory(regionDir)) return 0;

        int count = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(regionDir, "r.*.mca")) {
            for (Path file : files) {
                Matcher matcher = REGION_FILE.matcher(file.getFileName().toString());
                if (!matcher.matches()) continue;
                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));

                IntList expired = new IntArrayList();
                int[] locations = readLocations(file);
                for (int index = 0; index < locations.length; index++) {
                    if (locations[index] == 0) continue;
                    int chunkX = regionX * 32 + (index & 31);
                    int chunkZ = regionZ * 32 + (index >> 5);
                    if (now - dimension.lastVisit(chunkX, chunkZ, data.installTime) < maxAge) continue;
                    expired.add(index);
                }
                if (expired.isEmpty()) continue;

                count += expired.size();
                if (dryRun) continue;
                for (String storage : STORAGES) {
                    clear(dir.resolve(storage).resolve(file.getFileName()), regionX, regionZ, expired);
                }
                // The regenerated chunk starts over: it resets again once it has not been visited for long enough
                for (int index : expired) {
                    dimension.visit(regionX * 32 + (index & 31), regionZ * 32 + (index >> 5), now);
                }
            }
        }
        return count;
    }

    private static int[] readLocations(Path file) throws IOException {
        int[] locations = new int[1024];
        try (RandomAccessFile region = new RandomAccessFile(file.toFile(), "r")) {
            if (region.length() < 8192) return locations;
            for (int i = 0; i < locations.length; i++) locations[i] = region.readInt();
        }
        return locations;
    }

    private static void clear(Path file, int regionX, int regionZ, IntList indices) throws IOException {
        if (!Files.exists(file)) return;
        try (RandomAccessFile region = new RandomAccessFile(file.toFile(), "rw")) {
            if (region.length() < 8192) return;
            for (int index : indices) {
                region.seek(index * 4L);
                region.writeInt(0);
                region.seek(4096 + index * 4L);
                region.writeInt(0);
            }
        }
        for (int index : indices) {
            Files.deleteIfExists(file.resolveSibling("c." + (regionX * 32 + (index & 31)) + "." + (regionZ * 32 + (index >> 5)) + ".mcc"));
        }
    }
}
