# World Reset

A Minecraft mod for **Fabric** and **NeoForge** (Minecraft 1.21.1 – 26.3) that lets the wilderness grow back, inspired
by the flame altars of *Enshrouded*:

- **The world resets** – chunks that no player has been near for a while (7 in-game days by default) are deleted
  when the world is loaded and generated again from the seed: terrain, ores, trees, structures, loot chests and mobs
  come back, everything players changed there is gone.
- **Fire altars protect your home** – a lit campfire standing on an altar block never lets the chunks around it reset.
  The block below decides the size of the protected area:

  | Block below the campfire | Protected area |
  |--------------------------|----------------|
  | Copper block (any oxidation stage, waxed or not) | 3×3 chunks |
  | Iron block               | 5×5 chunks     |
  | Gold block               | 7×7 chunks     |
  | Diamond block            | 9×9 chunks     |
  | Netherite block          | 11×11 chunks   |

  Soul campfires work too. Putting out the campfire or removing it or the block below ends the protection.
- **No commands needed** – using a campfire with an empty hand shows the border of the protected area with flame
  particles, or explains how to turn it into a fire altar. An action bar message tells players when they enter or
  leave their home and when an altar is lit or put out.

Overworld and Nether reset by default, the End does not (its main island and the dragon fight would regenerate).

## How it works

- Every 5 seconds the mod records the current game time for every chunk within `visitRadius` (4) chunks of a player.
  Chunks a player only sees from further away count as visited when they are seen for the first time.
- Time is measured in game ticks, so it only passes while the world is running. Chunks that existed before the mod
  was installed count as visited at installation time.
- Resets happen **only when the world is loaded** (server start, opening the singleplayer world), before any chunk is
  in use. The chunk is removed from the region files (`region`, `entities`, `poi`), exactly like vanilla clears a chunk,
  so Minecraft generates it again the next time it is needed.
- The data lives in `<world>/data/worldreset.dat`.

Things to keep in mind:

- Items, pets and builds left in the wilderness are lost when it resets.
- At the edge of a protected area features that cross chunk borders (mostly trees) can be cut off.
- A player who logs out in the wilderness and does not come back for longer than the reset time may log in inside
  regenerated terrain.

## Server-side only

All logic runs on the server, so the mod works

- **in singleplayer** when installed on the client (the integrated server runs it), and
- **on dedicated servers** without being installed on the clients – vanilla clients can join.

## Configuration

`config/worldreset.properties` is created on first start. Changes are applied automatically while the game is
running; resets use the values at the time the world is loaded.
It can also be edited in game: on NeoForge in *Mods → World Reset → Config*, on Fabric through
[Mod Menu](https://modrinth.com/mod/modmenu). It changes the file of the game it is opened in (e.g. for singleplayer
worlds), not the config of a server you are connected to.

| Key                | Default                     | Description                                                                                  |
|--------------------|-----------------------------|----------------------------------------------------------------------------------------------|
| `enabled`          | `true`                      | Master switch. `false` = no chunks are reset, no messages. Visits are still recorded.        |
| `dryRun`           | `false`                     | Only log how many chunks would be reset, without deleting them.                              |
| `resetAfterDays`   | `7`                         | In-game days (20 minutes of play each) since the last visit before a chunk resets.           |
| `visitRadius`      | `4`                         | Chunks within this radius around a player count as visited.                                 |
| `dimensions`       | overworld, the_nether       | Comma separated dimensions whose chunks reset.                                               |
| `altarBlocks`      | see above                   | Comma separated `block=radius` pairs; radius 1 = 3×3 chunks.                                 |
| `soulCampfires`    | `true`                      | Soul campfires can be fire altars too.                                                       |
| `borderSeconds`    | `10`                        | How long the border is shown after using an altar with an empty hand.                        |
| `showMessages`     | `true`                      | Show the action bar messages.                                                                |
| `…Message`         | …                           | Texts of the messages (`{days}` and `{size}` are replaced).                                  |

## Building

The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build every Minecraft version and loader from one
source tree. Each node in `settings.gradle.kts` targets one Minecraft version and declares the compatible range in
`stonecutter.properties.toml`.

```sh
./gradlew build                          # build all versions and loaders
./gradlew buildAndCollect                # copy all jars to build/libs/<mod version>/
./gradlew :1.21.1-fabric:runClient       # run a specific version (Fabric)
./gradlew :26.3-neoforge:runClient       # run a specific version (NeoForge)
```

Every push to `main` builds all versions on GitHub Actions; the jars can be downloaded from the run's
artifacts.

On **NixOS**, run the game inside the provided Nix shell so Minecraft finds its native libraries
(otherwise it crashes with `GLFW error during init: Failed to detect any supported platform`):

```sh
nix-shell --run './gradlew :1.21.1-fabric:runClient'
```

Version-specific code uses Stonecutter comments (`//? if >=1.21.11 {`). The source tree is checked in for the
`1.21.1-fabric` node; switch the active version with the `Set active project to …` Gradle tasks before editing code
for another version, and switch back to `1.21.1-fabric` (`Reset active project`) before committing.

## License

[MIT](LICENSE)
