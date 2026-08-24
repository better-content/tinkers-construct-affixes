# Tinkers Construct Affixes

Forge 1.20.1 mod that turns Tinkers' Construct parts into a global, long-term loot chase.

Affixed parts can drop from player-killed hostile mobs and appear in chest loot caches. Each reward has a compatible Tinkers material, rolled from configurable weighted tiers; tier 4 remains a deliberately rare jackpot. Tier weights are renormalized across the tiers that actually contain materials compatible with the selected part and physical origin, so a valid cache cannot randomly fail because an incompatible tier was selected. Percentage rolls persist through Tinkers stat rebuilds, and modifier grants are tracked separately from ordinary player-applied modifier levels.

The Nether, Aether, Undergarden, and Otherside act as distinct "Fonts" with themed materials, part pools, guaranteed regional affixes, and better 2–6-affix rolls. Physical part/material provenance wins over stored metadata, so moving or editing NBT cannot turn a global item into a Font reward.

Six currencies support an inventory-native crafting loop:

- Right-click a flux or seal in any player-inventory slot to arm it, then left-click a compatible Tinkers part. Shift-click repeats while the currency remains available.
- Recasting rerolls, Grafting adds a line, and Shearing removes one.
- Forerune and Afterrune preserve one populated affix side while rerolling the other.
- Ruinous Flux offers a confirmed, one-way mutation with powerful outcomes, permanent mutation lock, and a 20% destruction risk.
- Shift-right-click an affixed part with no currency armed to salvage it. Natural rewards keep an immutable salvage seed and quality band, preventing reroll or duplication exploits.

Master toolsmiths sell the two preservation seals. Fluxes, seals, caches, and affixed parts also enter the global combat and chest-loot economy at deliberately low rates.

## Material salience

This repository owns behavioural profiles for its fifteen Aether, Undergarden, and Otherside materials. Profiles use the shared signed `-3..3` vocabulary of Impact, Tempo, Work, Mobility, Endurance, Robustness, Renewal, and Control. Tooltips compress each profile to its two strongest affinities and strongest weakness, using a shared colour and portable text glyph for each concept. The complete machine-readable contract is in `data/tinkers_construct_affixes/systemic_salience/material_profiles.json`.

That contract is deliberately scoped to pack-authored materials. Third-party TConstruct materials retain their native exact statistics and tooltips; this mod does not guess affinities for behavior it does not own. The shared palette is sRGB-gamut constrained and regression-tested for a minimum OKLab distance of 0.10 under normal vision and full-strength protan, deutan, and tritan simulations. Glyphs and names accompany every color.

The profile is descriptive, not a global stat pool: Tinkers remains the causal authority. Each owned material has a native Tinkers trait and its normal part statistics. In particular, Froststeel is a cold defensive material, Gravitite is forceful displacement with an accuracy cost, Skyroot is quick but fragile, and Ambrosium, Regalium, and Reinforced Echo no longer share blank behaviour.

Affix rewards deliberately exclude inert material definitions and materials or modifier grants that teleport the player, hover, reduce gravity, add jumps, or otherwise bypass traversal. Grounded guard, target displacement, projectile, and status behaviours remain valid.

Server worlds may tune hostile and Font drop rates, currency and cache rates, tier weights, and material allowlists in `tinkers_construct_affixes-server.toml`. Global defaults are 1% hostile part/currency drops, 3% chest cache/currency rolls, and material-tier weights of 80% / 17% / 2.9% / 0.1%. Font hostile drops use 3% part and 4% currency rates.

## Build

Use Java 17 and run:

```sh
./gradlew test reobfJar stageRuntimeJar
```

The deployable reobfuscated JAR is written to `build/libs/tinkers-construct-affixes-<version>.jar`.

## Canonical identity

- Repository and release artifact: `tinkers-construct-affixes`
- Mod ID and resource namespace: `tinkers_construct_affixes`
- Java package: `com.bettercontent.tinkersconstructaffixes`
- Validation: `./gradlew build`

This normalization is a clean break. Worlds, configuration files, and integrations created for earlier identities are not migrated or aliased.
