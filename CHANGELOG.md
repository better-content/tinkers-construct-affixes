# Changelog

## Unreleased

### Added

- Added native defining traits for every owned material and executable traversal, inert-pool, and damage-ceiling contracts.

### Changed

- Focused Froststeel on cold defense, Gravitite on forceful displacement with an accuracy weakness, and Skyroot on fragile speed.
- Differentiated the formerly blank Ambrosium, Regalium, and Reinforced Echo material definitions.
- Removed inert Blood, Clay, and Honey parts and player hover, teleport, low-gravity, bounce, and extra-jump routes from affix reward pools.
- Replaced traversal affixes with Straight Shot, Windward Guard, Grounded Guard, and grounded knockback resistance.

### Fixed

- Made Affixed Part Caches select only part profiles and weighted material tiers that can produce a valid Tinkers part in the current physical origin.
- Kept failed cache openings non-destructive and added player-facing feedback for genuinely invalid material configurations.
- Expanded startup diagnostics to identify malformed, missing, hidden, incorrectly tiered, incompatible, and zero-weight reward pools.

### Testing

- Restored canonical-package unit coverage for reward selection, affix invariants, merging, stat multipliers, modifier ownership, origins, salvage, and currencies.
- Added Forge GameTests for cache consumption, reward metadata, offhand use, every physical origin, and selectable profile/material viability.

### Changed

- Standardized the project as **Tinkers Construct Affixes** with mod ID `tinkers_construct_affixes`, artifact `tinkers-construct-affixes`, and package `com.bettercontent.tinkersconstructaffixes`.
- Adopted Java 17 and Forge 1.20.1-47.4.13 as the build baseline without changing the project version.
- This is a clean break; legacy worlds, configurations, and integrations are not migrated.
