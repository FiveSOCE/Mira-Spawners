# MiraSpawners

MiraSpawners is the spawner and mob-farming layer for the Mira Minecraft plugin ecosystem. It targets **Paper 1.21.11** and **Java 21** and integrates directly with **MiraCore**.

## Download

[**Download MiraSpawners v0.1.0**](https://github.com/FiveSOCE/Mira-Spawners/releases/download/v0.1.0/MiraSpawners-0.1.0.jar)

## Requirements

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0 or newer

## Spawner harvesting

Mob spawners can be harvested with a Silk Touch tool.

- The exact spawned mob type is preserved.
- A stacked spawner drops as one compact spawner item containing the whole stack.
- Natural spawners are harvestable by default.
- Creative-mode breaking does not create a custom drop.
- Breaking without the required Silk Touch destroys the spawner normally.

## Spawner stacking

Identical spawners can be stacked into a single block up to a hard maximum of **64 spawners per block**.

Right-click a placed spawner with another spawner of the same mob type to add it to the stack.

If only part of the held stack fits, MiraSpawners adds only the available capacity and safely returns the remainder to the player.

Sneak while right-clicking to bypass stacking and place a separate spawner normally.

Different mob types never combine.

### Stack inspection

There are no spawner holograms or floating text.

Left-click a spawner to receive a chat message such as:

```text
[Mira] Zombie Spawner Stack: 37/64
```

## Compact spawner items

A stacked spawner is represented by one physical spawner item with PDC-backed data describing:

- mob type
- internal stack size
- a unique item token to prevent compact stacks from merging incorrectly

Example:

```text
Zombie Spawner
Stack Size: 64/64
Place to deploy this spawner stack.
```

Placing that item creates one Zombie Spawner block with a stack size of 64.

## Mob stacking

Mobs spawned by MiraSpawners are performance-stacked.

A stacked spawner does not need to create dozens or hundreds of separate entity objects. Spawner output is folded into nearby managed mobs of the same type within the configured merge radius.

Example:

```text
Zombie x247
```

Default mob stack maximum: **1000**.

Only MiraSpawners-managed spawner mobs are merged. Naturally spawned mobs are not automatically absorbed into farm stacks.

## Normal deaths

For normal deaths, one mob is removed from the stack at a time.

```text
Zombie x247
player kills it
normal loot/XP for one mob
Zombie x246 remains
```

This applies to player attacks, projectiles, fall damage, fire, magma, drowning, suffocation, explosions and other normal death sources.

## Lava farm rule

Lava has one intentional special rule to preserve traditional automatic mob farms.

If the visible entity representing a mob stack **dies directly from `LAVA` damage**, the entire stack dies with it and the stack is removed.

MiraSpawners then produces loot for the entire stack. The first mob uses Bukkit's already-computed death drops, while the remaining mobs receive independent rolls from the mob's Bukkit loot table where available. Resulting items are combined into normal Minecraft item stacks before being dropped.

This triggers only when the fatal damage cause is exactly `LAVA`.

It does **not** trigger for:

- fire
- fire tick
- Fire Aspect
- magma blocks
- campfires
- fall damage
- drowning
- suffocation
- cactus
- explosions
- player damage

## Explosion protection

Spawner blocks are protected from TNT, creeper and other block/entity explosions by default. This can be disabled in config.

## Commands

All commands require `miraspawners.admin`, which defaults to OP.

```text
/mspawners info
/mspawners test
/mspawners give <player> <mob> [count]
/mspawners reload
/mspawners help
```

Aliases:

```text
/miraspawners
/mspawners
/mspawn
```

### Testing in game

After installing MiraCore and MiraSpawners, run:

```text
/mspawners test
```

A healthy installation should show every diagnostic as passed.

For a practical test:

```text
/mspawners give YourName zombie 64
```

Place the item, left-click the placed spawner and verify the chat reports `64/64`.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `miraspawners.admin` | OP | Admin commands and diagnostics |
| `miraspawners.mine` | Everyone | Silk Touch spawner harvesting |
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
```

`spawners.max-stack-size` can be lowered, but MiraSpawners enforces an absolute maximum of **64**.

## MiraCore integration

MiraSpawners registers itself with MiraCore as `MiraSpawners`, so `/miracore status` exposes its health.

It also registers a `MiraSpawnersApi` through MiraCore's shared service registry. Future plugins such as MiraShop, MiraCrates and MiraKits can therefore create correct spawner items without duplicating MiraSpawners' PDC format.

Example:

```java
MiraSpawnersApi spawners = core.services()
        .get(MiraSpawnersApi.class)
        .orElseThrow();

ItemStack item = spawners.createSpawner(EntityType.ZOMBIE, 64);
```

## Persistence and safety

- Spawner block stack size is stored in the spawner TileState PersistentDataContainer.
- Compact item data is stored in ItemStack PDC.
- Mob stack size is stored on the entity PDC and survives normal chunk unload/load cycles.
- Spawner stacks are capped at 64.
- Mob stacks are capped independently, defaulting to 1000.
- Only matching mob types can merge.
- Partial stacking safely returns overflow instead of deleting it.
- Compact spawner items carry unique tokens so internal stack counts cannot merge ambiguously.

## Building from source

```bash
gradle clean test build
```

The Gradle build automatically downloads the pinned MiraCore 0.1.0 release JAR and verifies its SHA-256 before compilation.

The built plugin is produced at:

```text
build/libs/MiraSpawners-0.1.0.jar
```

GitHub Actions runs unit tests, compiles against Paper 1.21.11 and uploads the built JAR as the `MiraSpawners` artifact.
