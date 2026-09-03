# MiraSpawners

Spawner and mob-farming layer for the Mira Minecraft plugin ecosystem, targeting **Paper 1.21.11 / Java 21** and integrating with **MiraCore**.

## Download

Current release: **v0.1.8**

[**Download MiraSpawners v0.1.8**](https://github.com/FiveSOCE/Mira-Spawners/releases/download/v0.1.8/MiraSpawners-0.1.8.jar)

[View all releases](https://github.com/FiveSOCE/Mira-Spawners/releases)

## Requirements

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0 or newer
- MiraFactions optional for faction-linked analytics

## v0.1.8 spawner intelligence

- spawner efficiency statistics
- faction-linked spawner analytics
- optional placed-stack split GUI
- `/mspawners stats`
- `/mspawners efficiency`
- `/mspawners factionstats <faction>`
- `/mspawners split`

The split GUI supports taking 1, 8, 16, 32, half, or all-but-one spawners from a placed stack while always leaving at least one placed spawner.

## Stack-change API

MiraSpawners exposes:

```java
com.mira.spawners.api.event.SpawnerStackChangeEvent
```

with location, mob type, old stack, new stack, delta, cause and actor UUID. Causes are `PLACE`, `STACK`, `BREAK`, and `API`.

Event semantics:

```text
Place 32:       0 -> 32
Add 10:        32 -> 42
Break:         42 -> 0
```

## Core behaviour

- typed Silk Touch spawner harvesting
- hidden typed-spawner PDC identity
- placed spawner stacks up to 64
- mob stacking
- lava whole-stack kill support
- natural/unmanaged hostile spawn blocking
- explicit CUSTOM/COMMAND spawn exemptions
- configurable fully blocked mob types

## Building

```bash
gradle clean test build
```

Output:

```text
build/libs/MiraSpawners-0.1.8.jar
```
