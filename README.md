# MiraSpawners

MiraSpawners is the stacked-spawner and mob-farming layer for the Mira Paper server suite. It provides typed Silk Touch spawners, placed spawner stacks, mob stacking, spawn-policy controls and spawner efficiency/faction analytics.

## Download

**Latest compatibility release: v0.1.16**

[**Download MiraSpawners-0.1.16.jar**](https://github.com/FiveSOCE/Mira-Spawners/releases/download/v0.1.16/MiraSpawners-0.1.16.jar)

[View all releases](https://github.com/FiveSOCE/Mira-Spawners/releases)

## v0.1.16 Spawner Activation Repair

v0.1.16 restores one clear activation model for both Mira-managed and ordinary physical spawners.

| Chunk state | Spawner behavior |
| --- | --- |
| No MiraLoader present | Normal Minecraft/Paper behavior. A real player must be within the configured activation range. |
| MiraLoader present but unfueled | Same as no loader. The loader is completely transparent. |
| MiraLoader present and fueled | Native/player-nearby behavior remains valid, and MiraLoaders supplements the spawner clock when no qualifying real player is nearby. |
| Loader fuel expires | Immediately returns to normal nearby-player activation. |

Older MiraSpawners/MiraLoaders builds could persist `requiredPlayerRange = 0` or `maxNearbyEntities = 0`. v0.1.16 repairs those values as chunks load and when spawners are placed. Defaults are:

- required player range: `16`
- maximum nearby entities: `6`

Both values are configurable.

Ordinary physical spawners are valid spawn sources again. Mira's hostile-mob policy may still block natural hostile spawning, but it no longer cancels a hostile mob merely because it came from a non-Mira physical spawner.

## MiraLoaders Integration

MiraSpawners supports **MiraLoaders v0.2.3+** for off-player spawner operation in a fueled loader chunk.

MiraLoaders owns only the supplemental off-player clock. MiraSpawners remains authoritative for managed-spawner behavior such as:

- placed spawner stack size
- effective `spawner_rate` multiplier
- mob-stack merging
- configured hard spawn-policy blocks
- adult-only managed-spawner safety rules
- chicken-jockey cleanup
- normal death/drop stack behavior

Loader-driven mobs are spawned through safe block-centred air cells so checkerboard/one-block-gap farm layouts do not place zombies inside neighbouring spawner blocks.

## Requirements / Dependencies

- Paper/Minecraft 1.21.11 through 26.2
- Java 21 production runtime
- MiraCore 0.2.0 or newer
- MiraLoaders v0.2.3+ optional for fueled off-player chunk spawner operation
- MiraFactions optional for faction-linked analytics
- MiraBoosters optional for global `spawner_rate` multipliers
- MiraOutposts optional for faction-owned `spawner_rate` multipliers

## How MiraSpawners Works

Spawner items carry hidden mob-type identity and can be harvested with Silk Touch when the player has mining permission. Identical placed spawners can stack together up to the configured hard cap of 64. Spawned mobs can also be merged into stacked entities to reduce entity load, with optional whole-stack lava killing.

The spawn-policy layer can block natural/unmanaged hostile spawning while allowing physical spawner, Mira-managed spawner, custom and command-based spawn sources. Individual mob types can also be fully disabled. MiraSpawners tracks produced units and efficiency, supports faction-linked spawner statistics, and exposes `SpawnerStackChangeEvent` plus a public API for other Mira systems.

The optional split GUI lets an administrator split 1, 8, 16, 32, half, or all-but-one spawners from a placed stack while always leaving at least one spawner placed.

Global `spawner_rate` boosters and faction-owned outpost `spawner_rate` bonuses are combined at spawn time, while analytics cache faction ownership on stack/spawn updates and expose server-wide and per-faction units-per-hour estimates through commands and the public API.

## Spawn Policy

A configured `fully-blocked-types` entry is a genuine hard block. The check runs before command/plugin exemptions, so a fully blocked mob cannot be reintroduced by a spawner, command or another plugin.

Current default hard blocks include:

- Bat
- Stray
- Phantom

Baby Zombies are also rejected as a forbidden variant. Mira-managed spawners additionally reject baby-capable variants and chicken-jockey combinations.

## Configuration

```yaml
spawners:
  max-stack-size: 64
  silk-touch-required: true
  natural-spawners-harvestable: true
  protect-from-explosions: true
  required-player-range: 16
  max-nearby-entities: 6
```

`required-player-range` is the normal activation rule when there is no active fueled MiraLoader.

## Commands

All `/mspawners` administration subcommands require `miraspawners.admin`.

| Command | Permission | What it does |
| --- | --- | --- |
| `/mspawners help` | `miraspawners.admin` | Shows MiraSpawners command help. |
| `/mspawners give <spawner> [amount]` | `miraspawners.admin` | Gives the executing player typed spawner items. |
| `/mspawners change <spawner>` | `miraspawners.admin` | Changes the mob type of the spawner held in the player's main hand. |
| `/mspawners stack` | `miraspawners.admin` | Sets the looked-at placed spawner stack to the configured maximum. |
| `/mspawners info` | `miraspawners.admin` | Shows runtime stack limits and spawn-policy settings. |
| `/mspawners test` | `miraspawners.admin` | Runs MiraSpawners self-tests/diagnostics. |
| `/mspawners reload` | `miraspawners.admin` | Reloads MiraSpawners configuration. |
| `/mspawners stats` | `miraspawners.admin` | Shows tracked spawner production statistics. |
| `/mspawners efficiency` | `miraspawners.admin` | Shows the same efficiency/statistics view. |
| `/mspawners factionstats <faction>` | `miraspawners.admin` | Shows faction-linked spawner analytics. |
| `/mspawners split` | `miraspawners.admin` | Opens the split GUI for the looked-at stacked spawner. |

Aliases: `/miraspawners`, `/mspawn`.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miraspawners.admin` | OP | Allows all MiraSpawners administration, analytics and diagnostics commands. |
| `miraspawners.mine` | Everyone | Allows Silk Touch harvesting of mob spawners. |
| `miraspawners.stack` | Everyone | Allows stacking identical spawners together. |
| `miraspawners.inspect` | Everyone | Allows left-click inspection of placed spawner stack sizes. |
