# Changelog

All notable changes to CPVPSingleBiome are documented here.

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
