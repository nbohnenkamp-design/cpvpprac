# Changelog

All notable changes to CPVPSingleBiome are documented here.

## [1.2.16] - 2026-06-20

### Changed

- Made the shipped reset timezone configuration neutral by default.
- Changed the default reset timezone to use the server/JVM timezone when left empty.
- Improved configuration comments to better explain behavior, safe ranges and PvP-friendly values.
- Replaced server-specific default maintenance command examples with neutral example commands.
- Changed the shipped fallback world example from `newspawn` to the standard `world`.

## [1.2.15] - 2026-06-20

### Fixed

- Disabled automatic scheduled resets by default in the shipped configuration.
- Disabled automatic Chunky pregeneration by default in the shipped configuration.
- Removed the hardcoded shipped `last-reset-date` runtime value from the default configuration.
- Added safe initialization for empty or invalid `last-reset-date` values when automatic resets are enabled for the first time.
- Prevented automatic resets from running immediately after a fresh installation or first enable.

## [1.2.14] - 2026-06-19

### Fixed

- Fixed reset handling for worlds that exist in Multiverse but are currently not loaded.
- Reset now removes stale Multiverse world registry entries before recreating worlds.
- Added Bukkit/Paper WorldCreator fallback for reset world creation when Multiverse is unavailable or does not load the world.
- Added recovery attempt with `mv load <world>` if a recreated reset world remains unloaded.

## [1.2.13] - 2026-06-12

### Added

- Added `/cpvpsb version`
- Added `/cpvpsb about`
- Added `/cpvpsb status`
- Added `/cpvpsb export`
- Added diagnostic export file generation under `plugins/CPVPSingleBiome/exports/`
- Added professional command permissions for public and admin information commands
- Added improved tab completion for new command structure

### Changed

- Updated command help output
- Updated `plugin.yml` command usage and permissions

### Notes

- `/cpvpsb export` writes a local diagnostic text file.
- Review exported files before sharing them publicly.
