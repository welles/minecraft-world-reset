# Changelog

All notable changes to this mod are documented in this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the mod follows [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-11-03

### Added

- A pixel art icon, shown in Mod Menu, in NeoForge's mod list and at the top of the README.

## [1.1.0] - 2026-11-02

### Added

- The config screen checks every value while typing: numbers outside their range are marked red, and Done stays
  disabled until every value is valid, listing the invalid ones in its tooltip.
- Every value in the config screen can be reset to its default with the ↺ button next to it, or all at once with
  Reset all. Nothing is saved until Done.

## [1.0.0] - 2026-10-26

First release, for Fabric and NeoForge on Minecraft 1.21.1 – 26.3.

### Added

- Chunks that no player has been near for a while (7 in-game days by default) are generated again from the seed when
  the world is loaded, so the wilderness grows back.
- A lit campfire on a copper, iron, gold, diamond or netherite block is a fire altar and protects 3×3 up to 11×11
  chunks around it from resets.
- Using a campfire with an empty hand shows the border of the protected area or explains how to build an altar, and
  action bar messages tell players when they enter or leave their home. No commands needed.
- A dry run mode that only logs which chunks would be reset.
- `config/worldreset.properties`, reloaded automatically and editable in game through NeoForge's mod list or
  Mod Menu.

[1.2.0]: https://github.com/welles/minecraft-world-reset/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/welles/minecraft-world-reset/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/welles/minecraft-world-reset/releases/tag/v1.0.0
