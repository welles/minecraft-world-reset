package dev.welles.worldreset;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Settings stored in {@code config/worldreset.properties}.
 * The file is re-read automatically whenever it changes on disk.
 */
public final class ResetConfig {
    public boolean enabled = true;
    public boolean dryRun = false;
    public int resetAfterDays = 7;
    public int visitRadius = 4;
    public List<String> dimensions = List.of("minecraft:overworld", "minecraft:the_nether");

    public Map<String, Integer> altarBlocks = radii(
        "minecraft:copper_block=1,minecraft:iron_block=2,minecraft:gold_block=3,minecraft:diamond_block=4,minecraft:netherite_block=5");
    public boolean soulCampfires = true;

    private static final String HEADER = """
        World Reset configuration
        Changes are picked up automatically, no restart required.

        Chunks are reset when the world is loaded (server start / opening the singleplayer world),
        if no player has been near them for resetAfterDays in-game days and no fire altar protects them.
        A reset chunk is deleted from the world files (blocks, entities, points of interest) and
        generated again from the world seed the next time it is loaded.

        enabled             Master switch. false = the mod does nothing, no chunks are reset.
        dryRun              true = only log which chunks would be reset, without deleting them.
        resetAfterDays      In-game days (20 minutes of play each) since the last visit before a chunk is reset.
        visitRadius         Chunks within this radius around a player count as visited.
                            Chunks the player only sees from further away reset resetAfterDays after they were first seen.
        dimensions          Comma separated dimensions whose chunks are reset (e.g. minecraft:overworld,minecraft:the_nether).
        altarBlocks         Comma separated block=radius pairs. A lit campfire on one of these blocks is a fire altar
                            that protects the chunks within the radius around its chunk (radius 1 = 3x3 chunks).
                            All oxidation stages and waxed variants of copper count as minecraft:copper_block.
        soulCampfires       Soul campfires can be fire altars too.""";

    /** Radius of the area protected by a fire altar on {@code blockId}, or -1 if it is not an altar block. */
    public int altarRadius(String blockId) {
        return altarBlocks.getOrDefault(blockId, -1);
    }

    private static Path file;
    private static FileTime loadedAt;
    private static ResetConfig current = new ResetConfig();

    public static synchronized void init(Path configDir) {
        file = configDir.resolve(WorldReset.MOD_ID + ".properties");
        reload();
    }

    /** Returns the current settings, re-reading the file if it was modified. */
    public static synchronized ResetConfig get() {
        if (file != null) {
            try {
                FileTime modified = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
                if (modified == null || !modified.equals(loadedAt)) reload();
            } catch (IOException e) {
                WorldReset.LOGGER.warn("Could not check config file {}", file, e);
            }
        }
        return current;
    }

    private static void reload() {
        ResetConfig config = new ResetConfig();
        Properties props = new Properties();
        boolean complete = false;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
                complete = config.read(props);
            } catch (IOException | RuntimeException e) {
                WorldReset.LOGGER.error("Could not read config file {}, using defaults", file, e);
                current = config;
                return;
            }
        }

        if (!complete) {
            try {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    config.write(writer);
                }
            } catch (IOException e) {
                WorldReset.LOGGER.error("Could not write config file {}", file, e);
            }
        }

        try {
            loadedAt = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
        } catch (IOException e) {
            loadedAt = null;
        }
        current = config;
    }

    /** Applies all values present in {@code props}; returns whether every key was present. */
    private boolean read(Properties props) {
        enabled = bool(props, "enabled", enabled);
        dryRun = bool(props, "dryRun", dryRun);
        resetAfterDays = integer(props, "resetAfterDays", resetAfterDays, 1, 100000);
        visitRadius = integer(props, "visitRadius", visitRadius, 0, 32);
        dimensions = list(props, "dimensions", dimensions);
        altarBlocks = radii(props, "altarBlocks", altarBlocks);
        soulCampfires = bool(props, "soulCampfires", soulCampfires);
        return props.keySet().containsAll(toProperties().keySet());
    }

    private void write(Writer writer) throws IOException {
        for (String line : HEADER.split("\n")) writer.write(("# " + line).stripTrailing() + "\n");
        writer.write("\n");
        for (var entry : toProperties().entrySet()) {
            String value = entry.getValue().replace("\\", "\\\\").replace("\n", "\\n");
            writer.write(entry.getKey() + "=" + value + "\n");
        }
    }

    private Map<String, String> toProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("enabled", String.valueOf(enabled));
        props.put("dryRun", String.valueOf(dryRun));
        props.put("resetAfterDays", String.valueOf(resetAfterDays));
        props.put("visitRadius", String.valueOf(visitRadius));
        props.put("dimensions", String.join(",", dimensions));
        props.put("altarBlocks", altarBlocks.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(",")));
        props.put("soulCampfires", String.valueOf(soulCampfires));
        return props;
    }

    private static boolean bool(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int integer(Properties props, String key, int fallback, int min, int max) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            return Math.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException e) {
            WorldReset.LOGGER.warn("Invalid number for {}: {}", key, value);
            return fallback;
        }
    }

    private static List<String> list(Properties props, String key, List<String> fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static Map<String, Integer> radii(Properties props, String key, Map<String, Integer> fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : radii(value);
    }

    /** Parses comma separated {@code block=radius} pairs. */
    private static Map<String, Integer> radii(String value) {
        Map<String, Integer> radii = new LinkedHashMap<>();
        for (String pair : value.split(",")) {
            if (pair.isBlank()) continue;
            int separator = pair.lastIndexOf('=');
            try {
                if (separator < 0) throw new NumberFormatException();
                radii.put(pair.substring(0, separator).trim(), Math.clamp(Integer.parseInt(pair.substring(separator + 1).trim()), 0, 32));
            } catch (NumberFormatException e) {
                WorldReset.LOGGER.warn("Invalid entry for altarBlocks: {} (expected block=radius)", pair);
            }
        }
        return radii;
    }
}
