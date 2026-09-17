# MiraSpawners

MiraSpawners is the stacked-spawner and mob-farming layer for the Mira Paper server suite. It provides typed Silk Touch spawners, placed spawner stacks, mob stacking, spawn-policy controls and spawner efficiency/faction analytics.

## Download

**Latest compatibility release: v0.1.14**

[**Download MiraSpawners-0.1.14.jar**](https://github.com/FiveSOCE/Mira-Spawners/releases/download/v0.1.14/MiraSpawners-0.1.14.jar)

[View all releases](https://github.com/FiveSOCE/Mira-Spawners/releases)

## v0.1.14 MiraLoaders integration

MiraSpawners now understands spawns driven by **MiraLoaders v0.2.2+** while no real player is in range or even online.

MiraLoaders owns the physical spawner clock while its chunk loader is fueled and marks the originating spawner before the `SpawnReason.SPAWNER` event fires. MiraSpawners resolves that source block and applies the normal Mira behavior:

- placed spawner stack size
- effective `spawner_rate` multiplier
- mob-stack merging
- configured hard spawn-policy blocks
- adult-only managed-spawner safety rules
- chicken-jockey cleanup
- normal death/drop stack behavior

This keeps loader-driven Mira spawners behaviorally consistent with ordinary player-activated Mira spawners.

## Requirements / Dependencies

- Paper/Minecraft 1.21.11 through 26.2
- Java 21 production runtime
- MiraCore 0.2.0 or newer
- MiraLoaders v0.2.2+ optional for off-player loaded-chunk spawner operation
- MiraFactions optional for faction-linked analytics
- MiraBoosters optional for global `spawner_rate` multipliers
- MiraOutposts optional for faction-owned `spawner_rate` multipliers

## How MiraSpawners Works

Spawner items carry hidden mob-type identity and can be harvested with Silk Touch when the player has mining permission. Identical placed spawners can stack together up to the configured hard cap of 64. Spawned mobs can also be merged into stacked entities to reduce entity load, with optional whole-stack lava killing.

The spawn-policy layer can block natural/unmanaged hostile spawning while allowing explicit spawner, custom or command-based spawn reasons, and individual mob types can be fully disabled. MiraSpawners also tracks produced units and efficiency, supports faction-linked spawner statistics, and exposes `SpawnerStackChangeEvent` plus a public API for other Mira systems.

The optional split GUI lets an administrator split 1, 8, 16, 32, half, or all-but-one spawners from a placed stack while always leaving at least one spawner placed.

Global `spawner_rate` boosters and faction-owned outpost `spawner_rate` bonuses are combined at spawn time, while analytics cache faction ownership on stack/spawn updates and expose server-wide and per-faction units-per-hour estimates through commands and the public API.

## Spawn Policy

A configured `fully-blocked-types` entry is a genuine hard block. The check runs before the CUSTOM/COMMAND exemption, so a fully blocked mob cannot be reintroduced by a command or another plugin.

Current default hard blocks include:

- Bat
- Stray
- Phantom

Baby Zombies are also rejected as a forbidden variant regardless of spawn source. Mira-managed/player-placed spawners reject baby-capable variants and chicken-jockey combinations without changing unrelated natural, command or event spawning.

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

Normal gameplay actions are permission-controlled rather than command-driven: Silk Touch harvesting uses `miraspawners.mine`, spawner stacking uses `miraspawners.stack`, and left-click stack inspection uses `miraspawners.inspect`.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miraspawners.admin` | OP | Allows all MiraSpawners administration, analytics and diagnostics commands. |
| `miraspawners.mine` | Everyone | Allows Silk Touch harvesting of mob spawners. |
| `miraspawners.stack` | Everyone | Allows stacking identical spawners together. |
| `miraspawners.inspect` | Everyone | Allows left-click inspection of placed spawner stack sizes. |
