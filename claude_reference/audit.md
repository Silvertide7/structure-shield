# Structure Shield — Audit Findings

Documented 2026-06-15. Tracks two audits, each run as a two-pass review (a self-audit plus an independent reviewer, reconciled).

**Legend:** `[x]` done · `[ ]` open · partials are unchecked with a `Partial:` note. Each finding shows severity, category, location, and (when resolved) what fixed it. API-behavior claims were verified against the decompiled NeoForge 21.0.167 / MC 1.21 sources.

---

## Feature work completed (closing non-block-item grief vectors)

This work resolved several audit findings (F1, F2, F4, F8, F9, F13, F16, F28, etc.) as a side effect.

- [x] **Placement authority moved to `BlockEvent.EntityPlaceEvent`** — replaced the old `UseItemOnBlockEvent` handler. Reports the actual placed block + true position natively; covers held blocks, flint & steel fire, and modded block-placers for free. (Surface-denial from the interim fix #2 was intentionally dropped in this trade.)
- [x] **Unified rule helpers** — `isPlacementBlocked` / `isRemovalBlocked` in `StructureShieldUtil`, keyed off the block created/removed, so every vector is governed by the existing `structure_shield_placeable` / `structure_shield_breakable` tags. No new tags.
- [x] **Bucket handler** (`PlayerInteractEvent.RightClickItem`) — fluid placement + scooping; per-fluid override via the breakable/placeable tags; `protectFromBucketScooping` config (default on).
- [x] **Explosion handler** (`ExplosionEvent.Detonate`) — strips protected non-breakable blocks from the blast; `protectFromExplosions` config (default on); covers all explosion sources.
- [x] **Fire spread** (`mixin/FireBlockMixin`) — cancels `checkBurnOut` + zeroes `getIgniteOdds` for protected positions; `protectFromFireSpread` config (default on). Runtime-verified the mixin applies (server boots clean).
- [x] **Pistons** (`PistonEvent.Pre`) — blocks push-in / pull-out / displace-within; `protectFromPistons` config (**default off**, opt-in). Includes perf fixes A + B from Audit 2.

**Documented gaps still open (intentional):** lily pads / frogspawn (`PlaceOnWaterBlockItem` bypasses both placement events), dispenser / non-player placement, fluids flowing in from outside, multi-block placements (beds/doors — only primary block validated).

---

## Audit 1 — Full project review

### High severity

- [x] **F1** (bug) `events/ModEvents.java` — place handler intercepted `UsePhase.ITEM_BEFORE_BLOCK`, blocking *all* block interaction (chests/doors) and cursing players for right-clicking with a held block; cursed players couldn't use any interactive block. _Fixed: moved to `ITEM_AFTER_BLOCK`, then superseded by `EntityPlaceEvent`._
- [x] **F2** (bug) `events/ModEvents.java` — checked the clicked position, not the real placement position, allowing one-block-deep placement into structures (permanent grief). _Fixed: interim `BlockPlaceContext` fix, then `EntityPlaceEvent` reports the true placed position natively._

### Medium severity

- [x] **F5** (bug) `effects/SanctumsCurseEffect.java` — milk/totem cured the curse (no `fillEffectCures` override), defeating the anti-spam rate-limiter. _Fixed: override `fillEffectCures` → `cures.clear()` so nothing lifts the curse._
- [x] **F6** (performance) `util/ProtectedStructureIndex.java` — `getStructureWithPieceAt` linearly scans every piece AABB per call. _Fixed: added a per-position result cache (`isInsideShieldedStructure`, keyed by `BlockPos.asLong`, per dimension, cleared on reload) so repeated checks at the same spot (piston clocking, sustained mining, fire ticks) skip the scan. Force-load risk handled by fix A. Residual: the first-ever check at a new position still does one scan (and may touch the structure's start chunk once)._
- [x] **F4** (bug) — only `BlockItem` placements intercepted; buckets, flint & steel, lily pads bypassed. _Fixed for buckets + flint&steel + modded placers; lily pads remain a documented gap._
- [x] **F18** (data) `tags/.../structure_shield_breakable.json` — breakable-tag gameplay holes. _Fixed: added `cobweb`, `gilded_blackstone`, `snow` (the layer), and the four infested stone-brick variants. Left rails and spawners protected, and igloo `snow_block` walls protected (only the thin `snow` layer is breakable), per the chosen scope._
- [x] **F23** (build) `gradle.properties` — version metadata was self-contradictory (`mod_version=1.21.1-1.0.0` with range `[1.21]`, double-prefixed publish name). _Resolved: `mod_version` was cleaned to plain semver `1.1.0`, so publish produces `1.21-1.1.0` and mods.toml carries `1.1.0`. No embedded MC version / no double-prefix remaining._
- [x] **F24** (build) `gradle.properties` / mods.toml — `mod_license="Creative Commons 4.0"` wasn't a real identifier. _Fixed: set `mod_license=All Rights Reserved` and added a top-level `LICENSE` file (strict ARR, no modpack carve-out — add one if desired). `TEMPLATE_LICENSE.txt` left as-is (it covers the MDK scaffolding)._

### Low severity

- [x] **F8** (bug) `events/ModEvents.java` — `BreakEvent` is server-only, so the "Client and Server check" comment and the nested `instanceof ServerPlayer` cursed-branch guard were dead. _Fixed: comment + redundant guard removed (typed-cast guards retained as they extract the typed locals)._
- [x] **F9** (smell) `events/ModEvents.java` — `shouldUpdateStaticData()` always true after the `SERVER_DATA_LOAD` check (dead condition). _Fixed: removed._
- [ ] **F10** (smell) `effects/SanctumsCurseEffect.java` — all three overrides are dead code; `applyEffectTick` returning false is the "remove effect" signal, a latent landmine.
- [ ] **F11** (smell) magic numbers. _Partial: `effectDuration*20` → `SharedConstants.TICKS_PER_SECOND` done; hotbar `36` removed with `syncItemToClient`. Still open: effect color `3124687` as decimal (use hex), and `define` instead of `defineInRange` for the duration (negative values silently treated as disabled)._
- [ ] **F12** (naming) `util/StructureShieldUtil.java` — `updateBlockFields` parameter named `structureRegistry` but it's the block registry (copy-paste).
- [x] **F13** (naming) `events/ModEvents.java` — `placePos` held the clicked position; `onBlockPlace` misnamed. _Fixed: handler replaced with `onEntityPlace`; variable gone._
- [x] **F14** (naming) — `isProtectedStructure` was a public static final constant in lowerCamelCase, declared after its use. _Fixed (side effect of F6): moved into `ProtectedStructureIndex` as `private static final IS_SHIELDED`, declared before use._
- [x] **F15** (naming) `events/ModEvents.java` — `tagsUpdatedEvent` broke the `onX` handler convention. _Fixed: renamed `onTagsUpdated`._
- [x] **F16** (smell) `events/ModEvents.java` — break `HIGHEST` vs place `HIGH` priority asymmetry; `@SubscribeEvent()` empty parens. _Fixed: both protection handlers HIGHEST; parens normalized._
- [x] **F17** (smell) `util/StructureShieldUtil.java` — util mixed registry flattening, structure queries, and client inventory networking (`syncItemToClient`). _Fixed: `syncItemToClient` deleted (replaced by `containerMenu.sendAllDataToRemote()` inline)._
- [ ] **F19** (data) tags enumerate dozens of blocks that vanilla tags already cover (`#minecraft:banners`, `#minecraft:crops`, `#minecraft:small_flowers`, per-ore tags, `#minecraft:village/mineshaft/shipwreck`); enumeration also misses modded blocks in those tags.
- [ ] **F20** (data) `structure_shield_protected.json` — omits ocean ruins, ruined portals, nether fossil, buried treasure; excluding ocean ruins while including shipwrecks is inconsistent.
- [ ] **F21** (data) JSON style — `breakable`/`placeable` omit `"replace": false` (inconsistent with `protected`); stray dangling-comma formatting in breakable; `wall_torch`/`soul_wall_torch` in the *placeable* tag are dead data (placement checks the standing block via `BlockItem.getBlock()`).
- [ ] **F22** (naming) tag ids repeat the namespace (`structure_shield:structure_shield_breakable` → `:breakable`); effect id `sanctums_curse_effect` has a redundant `_effect` suffix. Rename before the API ossifies.
- [ ] **F25** (build) README is the stock MDK readme; `neoforge.mods.toml` keeps all boilerplate comments + commented-out issue tracker / homepage / logo despite a CurseForge release.
- [ ] **F26** (build) NeoForge dependency range `[21.0.167,)` is unbounded above (claims compat with 21.1+/22+); `loader_version_range=[1,)` similarly wide.
- [ ] **F27** (build) CI nits — `on: [push, pull_request]` double-builds PRs; deep fetch + tags fetched but unused; no gradle wrapper-validation; no artifact uploaded.
- [x] **F28** (smell) `events/ModEvents.java` — place curse branch messaged client-side only while break messaged server-side; used an unchecked `(ServerPlayer)` cast. _Fixed: unified via `denyAndCurse`, server-side messages, instanceof pattern throughout._
- [ ] **F29** (data) `textures/mob_effect/sanctums_curse_effect.png` — 32×32, but vanilla mob-effect sprites are 18×18, so it renders downscaled/blurry. (Google Drive master is also 32×32.)
- [ ] **F30** (smell) `util/ProtectedStructureIndex.java` — `chunkHasNoShieldedStructures` is a negated, state-mutating query forcing double-negatives at call sites; `compute()`'s `refs.isEmpty()` pre-check is redundant.
- [ ] **F31** (smell) `config/ServerConfigs.java` — `BUILDER.push("Structure Shield Config")` makes a spaced/quoted TOML table name; `BUILDER.comment("")` emits bare `#` separator lines.
- [ ] **F32** (bug) `events/ModEvents.java` — `FakePlayer extends ServerPlayer`, so modded automation is blocked inside structures and accumulates pointless curse `MobEffectInstance`s (no crash; messages/connection are no-ops). Decide & document the policy.
- [ ] **F33** (bug) `config/ServerConfigs.java` — `PROTECT_ALL_STRUCTURES` only read in `setupModData` (server start / tag reload); editing the server config and reloading at runtime (`ModConfigEvent.Reloading`) doesn't re-flatten, so the change silently has no effect until `/reload` or restart.

### Reviewer-2 unique catches / informational

- [ ] **API naming risk** — `api/IBlock` & `api/IStructure` are dangerously generic duck-interface names for a public package; prefer `StructureShield`-prefixed names to avoid collisions with other mods' mixin ducks.
- [ ] **Design limitation** — `breakable`/`placeable` are global; you can't scope an exception to a specific structure (e.g. "torches placeable only in villages"). Worth a roadmap note since "data-driven" is the selling point.
- **(info, not a defect)** The dual `ServerStartedEvent` + `TagsUpdatedEvent` registration is **not** redundant — the first tag load fires before `getCurrentServer()` is set, so both are required. Don't "simplify" it away.

---

## Audit 2 — Piston handler performance review

Focused on `onPistonMove` (`events/ModEvents.java`) + the helpers it calls. Verdict: the handler was functionally correct; findings were performance + a force-load hazard.

- [x] **A-F1 / A-F2** (high, performance) — `resolve()` ran with no cheap fast-fail, so every piston on the server paid a `PistonStructureResolver` allocation + resolve (2–3× total, since vanilla's `checkIfExtend` and `moveBlocks` also resolve). _Fixed (fix B): chunk-cache proximity gate (`noShieldedStructureInChunk` for piston + face positions) before `resolve()`, so off-structure pistons exit after ~a couple of hashmap lookups._
- [x] **A-F4** (medium, correctness) — synchronous chunk force-load: `getStructureWithPieceAt` / `getAllStructuresAt` use 3-arg `getChunk(..., STRUCTURE_REFERENCES)` with `requireChunk=true` (blocks / can throw); a piston probing `moved.relative(pushDirection)` across an unloaded boundary could stall the server thread. _Fixed (fix A): `chunkHasNoShieldedStructures` returns "no structures" for unloaded chunks via `hasChunkAt` (verified non-loading). Hardens all handlers._
- [x] **A-F3** (high, performance) — piece-scan amplification near large structures. _Substantially mitigated by F6's per-position cache: repeated activations against a structure (the dominant cost — a piston clock, sustained mining) now hit the cache instead of re-scanning pieces. The remaining "resolve the start once per multi-block operation" batching only saves the first-ever scan of each new position and is no longer pressing (pistons default-off); left as an optional micro-optimization._
- [ ] **A-F5** (medium, gap) — **Accepted / won't-fix for now (decided 2026-06-15):** the per-dimension caches (chunk + F6 position cache) only clear on `setupModData`; they never evict on chunk/dimension unload. Growth is bounded by near-structure locality (realistically tens of KB) and cleared on reload/restart, so it's a hygiene gap, not a real leak. Deliberately left as-is to keep the hot-path caches simple. _Revisit only if a long-running server shows real growth; fix would be per-chunk eviction on `ChunkEvent.Unload`._
- [ ] **A-F6** (low, performance) — a powered piston jammed against a structure re-resolves + re-checks every activation. _Largely mitigated by the fix-B gate; further mitigated if fix C lands._
- [ ] **A-F7** (low, performance) — `BlockPos` allocations on the hot path (`getFaceOffsetPos()`, `moved.relative(...)`). _Recommended: reuse a `MutableBlockPos` for the probes (only matters once past the gate)._
- [ ] **A-F8** (low, performance) — `ProtectedStructureIndex` capturing lambda in `computeIfAbsent(long, …)` may allocate a closure per call unless JIT scalarizes. Suspected; profile before changing.
- **(info, by design)** **A-F9** — the extend-only `getFaceOffsetPos()` check overlaps `getToPush()[0]`; it's intentional defense-in-depth for the head replacing a non-pushed (air) block. Correct as-is.
- **(refuted)** **A-F10** — reviewer suggested using `resolver.getPushDirection()`; `javap` confirms no public getter exists. The manual `extending ? dir : dir.getOpposite()` matches the resolver's constructor and is the correct approach.

---

## Suggested next steps (open items, roughly prioritized)

1. Cheap cleanups — **F10, F11 (color + bounds), F12, F29 (18×18 icon), F31**.
2. Pre-ossification renames — **F22** (tag/effect ids), **API naming risk** (IBlock/IStructure).
3. **F19 / F20 / F21** — tag content: reference vanilla tags, reconcile structure list, JSON style + dead wall-torch entries.
4. **F33** — re-flatten on `ModConfigEvent.Reloading`; **F32** — decide FakePlayer policy.
5. **F26** — bound the NeoForge dependency range (publishing hygiene).
6. **F7 / F30** — cache eviction + negated-query-name cleanup (A-F5 accepted as-is for now).

_All medium-severity findings are now resolved or explicitly accepted; only low-severity items remain._
