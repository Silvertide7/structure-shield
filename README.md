# Structure Shield

Structure Shield stops players from breaking or placing blocks inside protected structures. It is fully data-driven through tags and built to stay cheap on the server tick (most checks fail fast on a per-chunk cache before any expensive structure lookup).

## How it works

A structure is protected if it carries the `structure_shield:structure_shield_protected` structure tag — most vanilla structures are protected by default. Inside a protected structure, survival players cannot break or place blocks, place or scoop fluids, and (optionally) the structure is shielded from explosions, fire spread, and pistons. Creative and spectator players bypass all protection.

Two block tags carve out exceptions, always keyed off the block being created or removed:

- `structure_shield:structure_shield_breakable` — blocks players may still **break** inside protected structures (ores, crops, decorations, etc.).
- `structure_shield:structure_shield_placeable` — blocks players may still **place** inside protected structures (torches by default).

Because the decision is based on the resulting block, allowing a specific block — including a fluid such as `minecraft:water` — is just a matter of adding it to the matching tag.

## Configuration

Per-world server config (`serverconfig/structure_shield-server.toml`):

| Option | Default | Effect |
| --- | --- | --- |
| `protectAllStructures` | `false` | Protect every structure, ignoring the protected tag. |
| `sanctumsCurseEffectDuration` | `4` | Seconds of Sanctum's Curse applied when a protected action is denied (rate-limits spam). `0` disables it. |
| `protectFromExplosions` | `true` | Shield protected blocks from all explosions. |
| `protectFromBucketScooping` | `true` | Prevent scooping fluids out of protected structures. |
| `protectFromFireSpread` | `true` | Stop fire spreading onto or into protected structures. |
| `protectFromPistons` | `false` | Prevent pistons pushing/pulling blocks across a protected boundary. |

## Customizing with a datapack

Override any of the three tags in a datapack to change which structures are protected or which blocks are exempt. The bundled tags use `"replace": false`, so your datapack entries are added to the defaults rather than replacing them.

## Building

Requires JDK 21.

```bash
./gradlew build        # outputs the jar to build/libs
./gradlew runClient    # launch a dev client
./gradlew runServer    # launch a dev server
```

## License

All Rights Reserved — see [LICENSE](LICENSE).
