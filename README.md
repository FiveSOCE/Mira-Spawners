# MiraSpawners

MiraSpawners is the stacked-spawner and mob-farming layer for the Mira Paper server suite. It provides typed Silk Touch spawners, placed spawner stacks, mob stacking, spawn-policy controls and spawner efficiency/faction analytics.

## Download

[**Download MiraSpawners v0.1.8**](https://github.com/FiveSOCE/Mira-Spawners/releases/download/v0.1.8/MiraSpawners-0.1.8.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0 or newer
- MiraFactions optional for faction-linked analytics

## How MiraSpawners Works

Spawner items carry hidden mob-type identity and can be harvested with Silk Touch when the player has mining permission. Identical placed spawners can stack together up to the configured hard cap of 64. Spawned mobs can also be merged into stacked entities to reduce entity load, with optional whole-stack lava killing.

The spawn-policy layer can block natural/unmanaged hostile spawning while allowing explicit spawner, custom or command-based spawn reasons, and individual mob types can be fully disabled. MiraSpawners also tracks produced units and efficiency, supports faction-linked spawner statistics, and exposes `SpawnerStackChangeEvent` plus a public API for other Mira systems.

The optional split GUI lets an administrator split 1, 8, 16, 32, half, or all-but-one spawners from a placed stack while always leaving at least one spawner placed.

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
