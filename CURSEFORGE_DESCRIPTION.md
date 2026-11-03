---
summary: The wilderness grows back! Unvisited chunks regenerate with fresh ores, trees and loot, while a fire altar guards your home. Inspired by Enshrouded, no commands needed, server-side only.
---

> ⚠️ **This project is AI-generated.** The code, this description and the icon of this mod were created with generative AI (Claude by Anthropic), directed by the author. Please keep that in mind, and report any problems on GitHub.

# World Reset

Let the wilderness grow back, inspired by the flame altars of *Enshrouded*: everything players have not visited for a while is generated anew, only your home is protected by a fire altar.

## Features

- **The world resets** – chunks that no player has been near for a while (7 in-game days by default) are generated again from the seed when the world is loaded: terrain, ores, trees, structures, loot chests and mobs come back, everything players changed there is gone.
- **Fire altars protect your home** – a lit campfire on an altar block keeps the chunks around it from resetting. The block decides the size: copper 3×3, iron 5×5, gold 7×7, diamond 9×9 and netherite 11×11 chunks. Soul campfires work too.
- **No commands needed** – using a campfire with an empty hand shows the border of the protected area with flame particles, or explains how to turn it into a fire altar. Action bar messages tell you when you enter or leave your home.

Overworld and Nether reset by default, the End does not.

**Keep in mind:** items, pets and builds left in the wilderness are lost when it resets, and trees at the edge of a protected area can be cut off.

## Installation

All logic runs on the server, so the mod works in **singleplayer** and on **dedicated servers** without being installed on the clients: players with an unmodded game can join.

## Configuration

`config/worldreset.properties` is created on first start and reloaded automatically when it changes. It can also be edited in game: on NeoForge in *Mods → World Reset → Config*, on Fabric through [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu). Invalid values are marked red and cannot be saved, and every value can be reset to its default.

The reset time, the visit radius, the dimensions that reset, the altar blocks and their sizes, soul campfires and all messages can be configured. A dry run mode only logs what would be reset.

## Versions

Available for **Fabric** and **NeoForge** on Minecraft **1.21 – 26.3**. Download the file that matches your loader and Minecraft version.

## Links

- [Source code](https://github.com/welles/minecraft-world-reset)
- [Report a problem](https://github.com/welles/minecraft-world-reset/issues)
- [Changelog](https://github.com/welles/minecraft-world-reset/blob/main/CHANGELOG.md)
