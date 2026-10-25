package dev.welles.worldreset;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Per-world state of the mod, stored in {@code <world>/data/worldreset.dat}: when each chunk was last visited and
 * where the fire altars are. All times are in game ticks.
 */
public final class ResetData {
    private static final int FORMAT = 1;

    /** A lit campfire on an altar block, protecting the chunks within {@code radius} around its chunk. */
    public record Altar(int x, int y, int z, int radius) {
        public int chunkX() {
            return x >> 4;
        }

        public int chunkZ() {
            return z >> 4;
        }

        public boolean protects(int chunkX, int chunkZ) {
            return Math.abs(chunkX - chunkX()) <= radius && Math.abs(chunkZ - chunkZ()) <= radius;
        }
    }

    public static final class Dimension {
        private final Long2LongOpenHashMap lastVisits = new Long2LongOpenHashMap();
        private final Long2ObjectOpenHashMap<List<Altar>> altarsByChunk = new Long2ObjectOpenHashMap<>();

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

        public List<Altar> altars(int chunkX, int chunkZ) {
            return altarsByChunk.getOrDefault(chunkKey(chunkX, chunkZ), List.of());
        }

        public void setAltars(int chunkX, int chunkZ, List<Altar> altars) {
            if (altars.isEmpty()) altarsByChunk.remove(chunkKey(chunkX, chunkZ));
            else altarsByChunk.put(chunkKey(chunkX, chunkZ), List.copyOf(altars));
        }

        public boolean isProtected(int chunkX, int chunkZ) {
            for (List<Altar> altars : altarsByChunk.values()) {
                for (Altar altar : altars) {
                    if (altar.protects(chunkX, chunkZ)) return true;
                }
            }
            return false;
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
                for (int altarCount = in.readInt(); altarCount > 0; altarCount--) {
                    Altar altar = new Altar(in.readInt(), in.readInt(), in.readInt(), in.readInt());
                    dimension.altarsByChunk.computeIfAbsent(chunkKey(altar.chunkX(), altar.chunkZ()), k -> new ArrayList<>()).add(altar);
                }
            }
            return data;
        } catch (IOException | RuntimeException e) {
            // Without the altars every home could be reset, so start over as if the mod was just installed
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
                    List<Altar> altars = dimension.altarsByChunk.values().stream().flatMap(List::stream).toList();
                    out.writeInt(altars.size());
                    for (Altar altar : altars) {
                        out.writeInt(altar.x());
                        out.writeInt(altar.y());
                        out.writeInt(altar.z());
                        out.writeInt(altar.radius());
                    }
                }
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            WorldReset.LOGGER.error("Could not save {}", file, e);
        }
    }
}
