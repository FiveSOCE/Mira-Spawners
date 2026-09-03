# MiraSpawners

Spawner and mob-farming layer for the Mira Minecraft plugin ecosystem, targeting **Paper 1.21.11 / Java 21** and integrating with **MiraCore**.

## Download

Current release: **v0.1.7**

[**Download MiraSpawners v0.1.7**](https://github.com/FiveSOCE/Mira-Spawners/releases/download/v0.1.7/MiraSpawners-0.1.7.jar)

[View all releases](https://github.com/FiveSOCE/Mira-Spawners/releases)

## Requirements

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0 or newer

## v0.1.7 stack-change integration hooks

MiraSpawners now exposes a public Bukkit event:

```java
com.mira.spawners.api.event.SpawnerStackChangeEvent
```

Other plugins can listen for every meaningful spawner-stack value change. The event exposes:

- spawner location
- mob `EntityType`
- old stack amount
- new stack amount
- signed `delta()`
- change cause
- optional actor UUID

Causes:

```text
PLACE
STACK
BREAK
API
```

Event semantics are consistent:

```text
Place a stack of 32:  0 -> 32
Add 10 to that stack: 32 -> 42
Break the stack:       42 -> 0
```

This gives MiraFactions and future Mira plugins a proper event-driven integration point instead of needing to guess when the block PDC changed.

## Spawner harvesting and stacking

- Silk Touch harvesting preserves the exact spawned mob type.
- Spawner items store hidden MiraSpawners PDC mob-type data.
- Identical spawners stack into one placed block up to 64.
- The physical inventory ItemStack amount represents real spawner units.
- Breaking a stacked spawner returns the full stack as real typed spawner items.
- Left-click inspection reports the placed stack size.
- Right-clicking with matching typed spawners adds them to the placed stack.
- Sneak-right-click bypasses stacking and places a separate block.
- Different mob types never merge.

## Hostile mob spawn policy

Natural and unmanaged hostile spawning is blocked while intentional special/event spawns remain allowed.

- Mira-managed/player-placed hostile spawners are allowed.
- Bukkit/Paper `CUSTOM` and `COMMAND` spawn reasons are allowed and persistently exempted from later cleanup.
- Natural hostile spawners, patrols, darkness spawns and other unmanaged hostile paths remain blocked.
- Fully blocked types remain configurable.

## Mob stacking

MiraSpawners performance-stacks managed mobs, with a default maximum of 1000 entities represented by one visible entity.

Normal deaths remove one mob from the stack at a time. A death caused directly by `LAVA` intentionally kills the entire stack and produces corresponding loot to support automatic farms.

## Commands

```text
/mspawners
/mspawners help
/mspawners give <spawner> [amount]
/mspawners change <spawner>
/mspawners stack
/mspawners info
/mspawners test
/mspawners reload
```

Aliases:

```text
/miraspawners
/mspawners
/mspawn
```

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `miraspawners.admin` | OP | Administration and diagnostics |
| `miraspawners.mine` | Everyone | Silk Touch harvesting |
| `miraspawners.stack` | Everyone | Right-click spawner stacking |
| `miraspawners.inspect` | Everyone | Left-click stack inspection |

## Configuration

```yaml
spawners:
  max-stack-size: 64
  silk-touch-required: true
  natural-spawners-harvestable: true
  protect-from-explosions: true

mobs:
  max-stack-size: 1000
  merge-radius: 6.0
  show-stack-name: true
  lava-stack-kill: true
  block-non-spawner-hostiles: true
  fully-blocked-types:
    - BAT
    - STRAY
```

## MiraCore integration

MiraSpawners registers with MiraCore and exposes `MiraSpawnersApi` through the shared service registry for correct typed-spawner item creation.

## Persistence and safety

Spawner stack size is stored in block PDC, typed item identity is stored in item PDC, mob stack state persists on entities, stack limits are enforced and partial stacking safely leaves overflow with the player.

## Building from source

```bash
gradle clean test build
```

Output:

```text
build/libs/MiraSpawners-0.1.7.jar
```
