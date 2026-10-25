package dev.welles.worldreset;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Per-world state of the mod, stored in {@code <world>/data/worldreset.dat}: when each chunk was last visited. All times
 * are in game ticks.
 */
public final class ResetData {
    private static final int FORMAT = 1;

    public static final class Dimension {
        private final Long2LongOpenHashMap lastVisits = new Long2LongOpenHashMap();

        /** The game time of the last visit, or {@code fallback} if the chunk has never been seen. */
        public long lastVisit(int chunkX, int chunkZ, long fallback) {
            return lastVisits.getOrDefault(chunkKey(chunkX, chunkZ), fallback);
        }

        public void visit(int chunkX, int chunkZ, long time) {
            lastVisits.put(chunkKey(chunkX, chunkZ), time);
        }

        /** Records the first time a chunk is seen, so it does not count as unvisited since the mod was installed. */
        public void see(int chunkX, int chunkZ, long time) {
            lastVisits.putIfAbsent(chunkKey(chunkX, chunkZ), time);
        }
    }

    private final Path file;
    /** Chunks that have never been seen count as visited at this time (when the mod was added to the world). */
    public final long installTime;
    private final Map<String, Dimension> dimensions = new HashMap<>();

    private ResetData(Path file, long installTime) {
        this.file = file;
        this.installTime = installTime;
    }

    public Dimension dimension(String id) {
        return dimensions.computeIfAbsent(id, k -> new Dimension());
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
    }

    /** Loads the data of a world, or starts fresh with {@code now} as install time. */
    public static ResetData load(Path worldDir, long now) {
        Path file = worldDir.resolve("data").resolve(WorldReset.MOD_ID + ".dat");
        if (!Files.exists(file)) return new ResetData(file, now);

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file))))) {
            int format = in.readInt();
            if (format != FORMAT) throw new IOException("Unknown format " + format);

            ResetData data = new ResetData(file, in.readLong());
            for (int dimensionCount = in.readInt(); dimensionCount > 0; dimensionCount--) {
                Dimension dimension = data.dimension(in.readUTF());
                for (int visits = in.readInt(); visits > 0; visits--) {
                    dimension.lastVisits.put(in.readLong(), in.readLong());
                }
            }
            return data;
        } catch (IOException | RuntimeException e) {
            // Start over as if the mod was just installed, so lost visits do not make chunks reset
            Path broken = file.resolveSibling(file.getFileName() + ".broken");
            WorldReset.LOGGER.error("Could not read {}, moving it to {} and starting fresh", file, broken, e);
            try {
                Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveError) {
                WorldReset.LOGGER.error("Could not move {}", file, moveError);
            }
            return new ResetData(file, now);
        }
    }

    public void save() {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(temp))))) {
                out.writeInt(FORMAT);
                out.writeLong(installTime);
                out.writeInt(dimensions.size());
                for (var entry : dimensions.entrySet()) {
                    Dimension dimension = entry.getValue();
                    out.writeUTF(entry.getKey());
                    out.writeInt(dimension.lastVisits.size());
                    for (Long2LongMap.Entry visit : dimension.lastVisits.long2LongEntrySet()) {
                        out.writeLong(visit.getLongKey());
                        out.writeLong(visit.getLongValue());
                    }
                }
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            WorldReset.LOGGER.error("Could not save {}", file, e);
        }
    }
}
