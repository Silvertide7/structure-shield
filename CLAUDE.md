# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Structure Shield is a NeoForge mod for Minecraft 1.21 (Java 21, mod id `structure_shield`, package `net.silvertide.structure_shield`) that prevents players from breaking or placing blocks inside protected structures. It is fully data-driven via tags and built around making the per-event check as cheap as possible.

Art assets (textures, Blockbench models, logos) live outside the repo in Google Drive: `~/Library/CloudStorage/GoogleDrive-natephillips801@gmail.com/My Drive/Games/Minecraft/Mod Assets/Structure Shield/`.

## Commands

```bash
./gradlew build              # build the mod jar into build/libs
./gradlew runClient          # launch a dev client with the mod
./gradlew runServer          # launch a dev server (--nogui)
./gradlew runData            # datagen; outputs to src/generated/resources (registered as a resources srcDir)
./gradlew runGameTestServer  # run registered gametests and exit (none exist yet; crashes if no tests are registered)
./gradlew publishMods        # publish to CurseForge (project 1269461); requires CF_TOKEN env var, reads CHANGELOG.md as the changelog
```

There is no unit test suite; verification is done by running the client/server. CI (GitHub Actions) runs `./gradlew build` on push/PR.

## Architecture

The mod's core design goal is performance: block break/place events fire constantly, so the hot path must fail fast. Everything below exists in service of that.

### Data-driven protection via three tags

Defaults ship in `src/main/resources/data/structure_shield/tags/`:

- `worldgen/structure/structure_shield_protected.json` — structure tag: which structures are protected (defaults to most vanilla structures)
- `block/structure_shield_breakable.json` — block tag: blocks players may still break inside protected structures (ores, crops, torches, decorations, etc.)
- `block/structure_shield_placeable.json` — block tag: blocks players may still place inside protected structures (torches)

Tag keys are declared in `tags/ModTags.java`. Pack makers override behavior entirely through datapacks.

### Tag flattening into mixin fields (no tag lookups in the hot path)

Rather than checking tag membership per event, `StructureShieldUtil.setupModData` iterates the structure and block registries and writes tag membership into boolean fields injected directly into the `Structure` and `Block` singletons:

- `mixin/StructureMixin.java` / `mixin/BlockMixin.java` add the fields and implement the duck interfaces `api/IStructure` / `api/IBlock` (methods prefixed `structureShield$`).
- Hot-path checks are plain field reads on objects already in hand (e.g. `((IBlock) state.getBlock()).structureShield$isBreakable()`).
- `setupModData` runs on `ServerStartedEvent` and again on `TagsUpdatedEvent` (datapack `/reload`), so flattened state always tracks current tag data. New mixins must be registered in `src/main/resources/structure_shield.mixins.json`.

### Chunk-level structure cache

`util/ProtectedStructureIndex` (singleton) lazily caches "does this chunk contain any shielded structure?" per dimension in a `Long2BooleanOpenHashMap` keyed by `ChunkPos.asLong`. Most events exit here without touching the expensive piece-accurate lookup. The cache is cleared in `setupModData` on every reload since shielded flags may have changed. It returns "no shielded structures" for **unloaded** chunks (guarded by `hasChunkAt`) so a position check never forces a synchronous chunk load/generation on the server thread — important for piston destinations that can probe across a chunk boundary.

### The unified rule (`StructureShieldUtil`)

Every handler routes through one of two helpers that encode the whole data model — always keyed off the **block being created or removed**, never the item:

- `isPlacementBlocked(level, pos, block)` — `false` if the block is in `structure_shield_placeable`, else whether `pos` is in a protected structure.
- `isRemovalBlocked(level, pos, block)` — `false` if the block is in `structure_shield_breakable`, else whether `pos` is in a protected structure.

Both call `isProtectedPosition`, which fast-fails through the `ProtectedStructureIndex` chunk cache before the piece-accurate `getStructureWithPieceAt`. So "allow water to be placed" = add `minecraft:water` to the placeable tag; "allow scooping water" = add it to the breakable tag. No item tags, no new concepts.

### Event flow (`events/ModEvents.java`)

Four protection handlers, all server-side, all gated on creative/spectator bypass and the Sanctum's Curse check first (curse = a temporary total break/place/bucket lockout that rate-limits spam so the structure lookup never repeats):

- `BlockEvent.BreakEvent` (HIGHEST) → `isRemovalBlocked` on the broken block.
- `BlockEvent.EntityPlaceEvent` (HIGHEST) → `isRemovalBlocked`'s sibling `isPlacementBlocked` on `getPlacedBlock()`. This is the placement authority and replaced the old `UseItemOnBlockEvent` handler: it reports the actual placed block + true position natively, so it also covers flint & steel fire and any modded block-placer for free. Denial is keyed to the placement position only (clicking a protected block's exterior face is allowed if the block lands outside the structure box — the prior clicked-surface denial was dropped in this trade).
- `PlayerInteractEvent.RightClickItem` (HIGH) → buckets only. Buckets bypass both `EntityPlaceEvent` (excluded from snapshot capture) and `UseItemOnBlockEvent` (air-click `use()`), so this handler re-runs the bucket's own raycast (`Item.getPlayerPOVHitResult`) and checks the resulting fluid block (placement) or scooped fluid/powder-snow block (removal). Powder snow buckets are `SolidBucketItem extends BlockItem`, so their *placement* is handled by `EntityPlaceEvent` instead.
- `ExplosionEvent.Detonate` → `getAffectedBlocks().removeIf(isRemovalBlocked)`, stripping protected non-breakable blocks from the blast. Covers all explosion sources.
- `PistonEvent.Pre` (gated on `protectFromPistons`, **default off**) → first fast-fails on the chunk cache (`noShieldedStructureInChunk` for the piston and face positions) so off-structure pistons skip all work; only then resolves the move via `getStructureHelper()` and cancels it if any pushed/destroyed block's source **or** destination position (or, on extend, the piston head's `getFaceOffsetPos()`) is inside a protected structure. The fast-fail matters because `PistonEvent.Pre` fires per activation and `resolve()` duplicates work vanilla already does — without the gate this would tax every piston on the server. Like fire, this is position-based rather than tag-respecting, and it is all-or-nothing (a contraption straddling the boundary won't actuate). Default off because piston grief is niche and this is the only check that inspects redstone activity.

On denial, `denyAndCurse` applies the curse and sends the action-bar message; placement/bucket denials also call `containerMenu.sendAllDataToRemote()` to repair the client's predicted item count (the predicted *block* reverts via vanilla's sequence-ack system).

Fire *spread* has no NeoForge event, so it is handled by `mixin/FireBlockMixin` instead (gated on `protectFromFireSpread`): it cancels `FireBlock.checkBurnOut` (the 6 direct neighbors — igniting/burning them) and zeroes the private `getIgniteOdds(LevelReader, BlockPos)` (the wider 3×3×6 spread loop) whenever the target position is inside a protected structure. Unlike the player-facing handlers, fire spread is blocked at *any* protected position regardless of the breakable/placeable tags — it is an environmental hazard, not a deliberate action. It does not extinguish existing fire or stop players lighting fires (that is the `EntityPlaceEvent` path).

**Known protection gaps (intentional, document if changed):** lily pads / frogspawn (`PlaceOnWaterBlockItem` calls `BlockItem.useOn` directly, firing neither placement event), dispenser/non-player placement, fluids flowing in from outside, and multi-block placements (beds, doors — only the primary block's position is validated).

### Config

`config/ServerConfigs.java` defines a SERVER-type config (`structure_shield-server.toml` in the world's serverconfig): `protectAllStructures` (ignore the structure tag, shield everything), `sanctumsCurseEffectDuration` (seconds; 0 disables the curse), `protectFromExplosions` (default on), `protectFromBucketScooping` (default on; per-fluid overrides still work via the breakable tag), `protectFromFireSpread` (default on), and `protectFromPistons` (default **off** — opt-in; piston grief is niche and it's the only check that inspects redstone activity).

## Build system notes

- `neoforge.mods.toml` is a template at `src/main/templates/META-INF/neoforge.mods.toml`; properties (`mod_version`, `mod_description`, etc.) are expanded from `gradle.properties` by the `generateModMetadata` task. Edit `gradle.properties` for metadata changes — there is no mods.toml under `src/main/resources`.
- Uses ModDevGradle with Parchment mappings; versions are pinned in `gradle.properties` (`neo_version`, `parchment_*`).
- Release flow: bump `mod_version` in `gradle.properties`, update `CHANGELOG.md`, then `./gradlew publishMods`.

---

# Reusable Engineering Standards

The sections below are project-agnostic. Copy this block (everything below the `---` separator) into any other project's `CLAUDE.md` unchanged to apply the same standards there.

## Code Style

**Never write comments.** No inline `//` comments, no `/* */` blocks, no javadoc, no leading explanatory headers on methods or fields. Code must be self-documenting through naming alone.

- Variable names describe what the value *is* (e.g. `armorCoveragePercent`, not `acp` with a comment).
- Method names describe what they *do* and under what conditions (e.g. `applyMultiplierIfAttackerIsPlayer`, not `applyBonus` with a comment explaining the player check).
- Extract a well-named helper method instead of writing a comment to explain a block.
- Constants get descriptive names that encode their meaning and unit (e.g. `KNIGHTMETAL_BONUS_DAMAGE_AT_FULL_ARMOR`, not `MAX` with a `// 2.0 vs fully-armored target` comment).
- If a name would need a comment to explain it, rename it until it doesn't.

Existing files may still contain comments and javadoc — leave them in place when editing unrelated code, but do not add new ones and prefer to delete obsolete ones when touching the surrounding code.

**Never leave dead code.** No unused methods, fields, classes, parameters, or imports. No "escape hatch" or "just in case" code. No commented-out blocks. If it's not called, delete it — the git history is the archive.

## Code Review

When asked to review code, do a "pass", check for issues, or otherwise audit a recent change, do **two** passes in order:

1. **Self-audit first.** Read the diff yourself. Fix the obvious — dead code, comments, naming, anything that violates the Code Style rules above. Report findings.
2. **Then spawn an independent reviewer** via the `/code-review` skill or a fresh agent. Give it only the diff and the goal, no context about why you made the choices you did. That catches the bugs you would otherwise rationalize away.
3. **Write to audit.md** Write the findings from the audit to audit.md in the claude_reference folder so we can checkmark them as we complete them.
4. **Project Problems check** by running a whole project search in the problems / project tab. Have the user download and put this file into the claude_reference/problems to check. Ask before deploys if we should do this.
   Don't skip step 2 because step 1 looked clean — the value of the independent reviewer is exactly that it doesn't share your blind spots.

## Version Control

**The user handles commits in git.** Never run `git add`, `git commit`, or `git push` — and don't suggest doing so — unless the user explicitly asks. Wrap up work by reporting what changed; staging and pushing are the user's job.
