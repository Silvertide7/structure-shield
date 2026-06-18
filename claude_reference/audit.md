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
- [x] **F10** (smell) `effects/SanctumsCurseEffect.java` — three dead overrides (incl. the `applyEffectTick`-returns-false landmine). _Fixed: deleted all three; only the constructor + `fillEffectCures` (F5) remain._
- [x] **F11** (smell) magic numbers. _Fixed: effect color → `0x2FADCF`; duration → `defineInRange(..., 0, Integer.MAX_VALUE)` (rejects negatives). (`*20` → `SharedConstants.TICKS_PER_SECOND` and hotbar `36` were already removed earlier.)_
- [x] **F12** (naming) `util/StructureShieldUtil.java` — `updateBlockFields` parameter renamed `structureRegistry` → `blockRegistry`.
- [x] **F13** (naming) `events/ModEvents.java` — `placePos` held the clicked position; `onBlockPlace` misnamed. _Fixed: handler replaced with `onEntityPlace`; variable gone._
- [x] **F14** (naming) — `isProtectedStructure` was a public static final constant in lowerCamelCase, declared after its use. _Fixed (side effect of F6): moved into `ProtectedStructureIndex` as `private static final IS_SHIELDED`, declared before use._
- [x] **F15** (naming) `events/ModEvents.java` — `tagsUpdatedEvent` broke the `onX` handler convention. _Fixed: renamed `onTagsUpdated`._
- [x] **F16** (smell) `events/ModEvents.java` — break `HIGHEST` vs place `HIGH` priority asymmetry; `@SubscribeEvent()` empty parens. _Fixed: both protection handlers HIGHEST; parens normalized._
- [x] **F17** (smell) `util/StructureShieldUtil.java` — util mixed registry flattening, structure queries, and client inventory networking (`syncItemToClient`). _Fixed: `syncItemToClient` deleted (replaced by `containerMenu.sendAllDataToRemote()` inline)._
- [x] **F19** (data) — enumerated block lists replaced with vanilla tag refs where they exist: `#minecraft:ice`, the 8 per-ore tags, `#minecraft:small_flowers` + `#minecraft:tall_flowers`, `#minecraft:crops`, `#minecraft:banners`. _Verified against the 1.21 vanilla tag contents to preserve coverage; now also covers modded blocks in those tags. Groups with no clean tag (lights, storage blocks, skulls, foliage, azalea) kept enumerated._
- [x] **F20** (data) `structure_shield_protected.json` — added `ocean_ruin_cold` + `ocean_ruin_warm` (consistency with shipwrecks). Buried treasure intentionally still excluded; ruined portals / nether fossils left out (no preference given — easy to add).
- [x] **F21** (data) — added `"replace": false` to `breakable` + `placeable`, removed the dead `wall_torch`/`soul_wall_torch` from `placeable`, and the F19 rewrite eliminated the dangling-comma formatting.
- [ ] **F22** (naming) — **Declined (decided 2026-06-15):** keep the tag IDs and effect ID as-is to avoid breaking existing worlds/datapacks on a released mod. Revisit only with a deliberate breaking release.
- [x] **F25** (build) — README replaced with real mod docs (features, tags, config, datapack, build); `neoforge.mods.toml` trimmed to essentials with real `issueTrackerURL` + `displayURL` (GitHub). `logoFile` left unset (no logo bundled in the jar).
- [x] **F26** (build) — NeoForge dependency range bounded to `[${neo_version},21.1)` (the 1.21.0 line). `loader_version_range` left `[1,)` (conventional FML-major range).
- [x] **F27** (build) CI — push builds restricted to `main` (no more PR double-builds), added Gradle wrapper validation, dropped the unused deep fetch, and upload `build/libs` as an artifact.
- [x] **F28** (smell) `events/ModEvents.java` — place curse branch messaged client-side only while break messaged server-side; used an unchecked `(ServerPlayer)` cast. _Fixed: unified via `denyAndCurse`, server-side messages, instanceof pattern throughout._
- [ ] **F29** (data) `textures/mob_effect/sanctums_curse_effect.png` — 32×32 vs vanilla's 18×18, renders downscaled. **Cannot auto-fix** (needs an 18×18 pixel-art redraw, not a lossy downscale). Left for a manual art pass.
- [x] **F30** (smell) `util/ProtectedStructureIndex.java` — renamed `chunkHasNoShieldedStructures` → positive `chunkHasShieldedStructure` (no more inner `!` / double-negative call sites) and dropped the redundant `refs.isEmpty()` pre-check.
- [ ] **F31** (smell) `config/ServerConfigs.java` — **Accepted as-is:** the spaced category `[Structure Shield Config]` is kept (renaming resets existing users' configs — declined with F22); the `comment("")` separators are conventional/readable and left in place.
- [x] **F32** (bug) `events/ModEvents.java` — fake players (modded automation) are still blocked, but `denyAndCurse`/`denyPlacement`/`denyBucketUse` now early-return for `FakePlayer`, skipping the pointless curse/message/resync (also avoids a latent `sendAllDataToRemote` NPE on fake connections).
- [x] **F33** (bug) `StructureShield.java` — added a mod-bus `ModConfigEvent.Reloading` listener that re-runs `setupModData` (on the server thread) when the server config reloads at runtime, so `protectAllStructures` etc. take effect without `/reload`.

### Reviewer-2 unique catches / informational

- [ ] **API naming risk** — `api/IBlock` & `api/IStructure` are generic duck-interface names. **Deferred:** internal-only and cosmetic; the methods are already namespaced (`structureShield$…`) so real collision risk is low, and renaming is pure churn in a released mod. Revisit if the `api` package ever becomes a real public surface.
- [ ] **Design limitation** — `breakable`/`placeable` are global; you can't scope an exception to a specific structure (e.g. "torches placeable only in villages"). Worth a roadmap note since "data-driven" is the selling point.
- **(info, not a defect)** The dual `ServerStartedEvent` + `TagsUpdatedEvent` registration is **not** redundant — the first tag load fires before `getCurrentServer()` is set, so both are required. Don't "simplify" it away.

---

## Audit 2 — Piston handler performance review

Focused on `onPistonMove` (`events/ModEvents.java`) + the helpers it calls. Verdict: the handler was functionally correct; findings were performance + a force-load hazard.

- [x] **A-F1 / A-F2** (high, performance) — `resolve()` ran with no cheap fast-fail, so every piston on the server paid a `PistonStructureResolver` allocation + resolve (2–3× total, since vanilla's `checkIfExtend` and `moveBlocks` also resolve). _Fixed (fix B): chunk-cache proximity gate (`chunkHasShieldedStructure` for piston + face positions) before `resolve()`, so off-structure pistons exit after ~a couple of hashmap lookups._
- [x] **A-F4** (medium, correctness) — synchronous chunk force-load: `getStructureWithPieceAt` / `getAllStructuresAt` use 3-arg `getChunk(..., STRUCTURE_REFERENCES)` with `requireChunk=true` (blocks / can throw); a piston probing `moved.relative(pushDirection)` across an unloaded boundary could stall the server thread. _Fixed (fix A): `chunkHasShieldedStructure` returns false for unloaded chunks via `hasChunkAt` (verified non-loading). Hardens all handlers._
- [x] **A-F3** (high, performance) — piece-scan amplification near large structures. _Substantially mitigated by F6's per-position cache: repeated activations against a structure (the dominant cost — a piston clock, sustained mining) now hit the cache instead of re-scanning pieces. The remaining "resolve the start once per multi-block operation" batching only saves the first-ever scan of each new position and is no longer pressing (pistons default-off); left as an optional micro-optimization._
- [ ] **A-F5** (medium, gap) — **Accepted / won't-fix for now (decided 2026-06-15):** the per-dimension caches (chunk + F6 position cache) only clear on `setupModData`; they never evict on chunk/dimension unload. Growth is bounded by near-structure locality (realistically tens of KB) and cleared on reload/restart, so it's a hygiene gap, not a real leak. Deliberately left as-is to keep the hot-path caches simple. _Revisit only if a long-running server shows real growth; fix would be per-chunk eviction on `ChunkEvent.Unload`._
- [ ] **A-F6** (low, performance) — **Accepted:** a jammed powered piston re-resolves each activation, but the fix-B gate + F6 position cache make it cheap; no further action.
- [ ] **A-F7** (low, performance) — **Accepted (won't-fix):** `BlockPos` allocations on the piston probe path. A `MutableBlockPos` micro-opt isn't worth the aliasing risk on a default-off, near-structure-only path. Revisit if pistons become widely enabled.
- [ ] **A-F8** (low, performance) — **Accepted:** the `computeIfAbsent` capturing lambda is speculative (likely scalarized by JIT); no profiling evidence, left as-is.
- **(info, by design)** **A-F9** — the extend-only `getFaceOffsetPos()` check overlaps `getToPush()[0]`; it's intentional defense-in-depth for the head replacing a non-pushed (air) block. Correct as-is.
- **(refuted)** **A-F10** — reviewer suggested using `resolver.getPushDirection()`; `javap` confirms no public getter exists. The manual `extending ? dir : dir.getOpposite()` matches the resolver's constructor and is the correct approach.

---

## Remaining open items

Every numbered finding (F1–F33) and the audit-2 findings are now **resolved, accepted, or declined**. What's left:

1. **F29 (needs art)** — re-export the Sanctum's Curse icon at 18×18. The only item that can't be done in code; needs a manual pixel-art pass.
2. **Accepted / declined (no action planned):** F22 (keep IDs — no breaking renames), F31 (keep config category), A-F5 (cache eviction — bounded growth accepted), A-F6/A-F7/A-F8 (piston micro-opts), API-naming risk (cosmetic/internal), and the design-limitation roadmap note (per-structure tag scoping).

_All medium- and low-severity findings are resolved or explicitly accepted; only the F29 art redraw remains actionable._

---

## IntelliJ inspection review (2026-06-17)

Full project Inspect Code export reviewed (`claude_reference/problems/`). Totals: **16 errors, 210 warnings, 16 weak, 392 typos** — but ~95% is noise, not code defects:

- **Noise (no fix):** all 14 "lossy encoding" errors + 14 "reassigned to plain text" warnings + ~700 spelling hits are `run/logs/*.log.gz` (gzipped dev logs); 184 grammar warnings are prose nits.
- **False positives (no fix):** the 2 Annotator errors + 5 TOML "unresolved" are `${...}` placeholders in the mods.toml **template**; the 15 Groovy warnings are IntelliJ not modelling the Gradle/ModDevGradle DSL in build.gradle; 1 is `.idea` module metadata.

**Done:**
- [x] **T0** — excluded `run/` from IDE inspection via `idea.module.excludeDirs += file('run')` in build.gradle (committed). Clears all 14 errors + 14 warnings + the bulk of typos. *(Re-sync Gradle in IntelliJ to apply.)*
- [x] **T1.1** — `ProtectedStructureIndex`: deprecated `hasChunkAt(BlockPos)` → `getChunkSource().hasChunk(...)` (also clears the Gradle build's deprecation note).
- [x] **T1.2** — `FireBlockMixin`: `@Unique` helper renamed to `structureShield$isFireSpreadProtected` (mixin uniqueness pattern).
- [x] **T1.3** — `SanctumsCurseEffect`: added `@ParametersAreNonnullByDefault` so the `fillEffectCures` override matches the supertype's nonnull contract.

**Not done (Tier 2, cosmetic):** boolean-always-inverted on `chunkHasShieldedStructure` (accepted — F30 deliberately chose the positive name), README markdown-table formatting, Gradle minor-version bump. The template/DSL false positives are suppressible by excluding `src/main/templates` + `build/` from inspection but need no code change.
