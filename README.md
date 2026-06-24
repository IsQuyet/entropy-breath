# EntropyBreath

[English](README.md) | [简体中文](README.zh-CN.md)

EntropyBreath is a Paper plugin that makes high-entropy areas harder to breathe in. It reads local entropy from EntropyCore, drains a player's air supply, blocks air regeneration when configured, and can damage players who stay in unsafe areas too long.

Use it when you want EntropyCore danger zones to affect moment-to-moment survival without adding new commands or custom items.

## Features

EntropyBreath focuses on one gameplay loop: entropy makes air scarce.

- Drains air while a player stands in an area where entropy is above `0`
- Scales air loss through configurable entropy tiers
- Blocks natural air regeneration in dangerous areas by default
- Applies configurable damage after air is depleted
- Respects Water Breathing, Conduit Power, Breath of the Nautilus, and Respiration
- Ignores creative and spectator players by default
- Keeps one air drain timer per player, so moving into or out of water cannot reset the drain interval

## Requirements

Install EntropyBreath on a Paper server with EntropyCore.

- Paper `1.21.11`
- Java `21`
- EntropyCore `1.0-SNAPSHOT`
- EntropyBreath `1.0-SNAPSHOT`

EntropyBreath requires EntropyCore at server startup. If EntropyCore does not expose `EntropyService`, EntropyBreath disables itself.

## Installation

Install both plugins before starting the server.

1. Build or download `entropy-core-1.0-SNAPSHOT.jar`
2. Build or download `entropy-breath-1.0-SNAPSHOT.jar`
3. Copy both jars into the server `plugins` directory:

```text
plugins/entropy-core-1.0-SNAPSHOT.jar
plugins/entropy-breath-1.0-SNAPSHOT.jar
```

4. Start the server once to generate `plugins/EntropyBreath/config.yml`
5. Edit the config if needed
6. Run `/entropybreath reload` after config changes

## Default gameplay

By default, entropy affects breathing only when players are not in water.

When a player enters a location with entropy above `0`, EntropyBreath decreases the player's air every `20` ticks. Higher entropy values remove more air per interval. At the default settings, entropy values `1`, `2`, `3`, `5`, and `8` map to stronger drain tiers.

In water, vanilla Minecraft already drains air and applies drowning damage. EntropyBreath leaves that behavior unchanged by default. Enable `air-drain.in-water` only if entropy should make underwater areas more dangerous too.

## Configuration

Edit `plugins/EntropyBreath/config.yml` after the first server start. The default source file lives at `src/main/resources/config.yml`.

Key options:

| Path | Default | Effect |
| --- | --- | --- |
| `air-drain.enabled` | `true` | Enables the plugin's air-drain logic |
| `air-drain.debug` | `false` | Logs drain and regeneration decisions |
| `air-drain.ignored-game-modes` | `CREATIVE`, `SPECTATOR` | Skips players in these game modes |
| `air-drain.in-air.regeneration.prevent` | `true` | Blocks air regeneration in areas where entropy is above `0` |
| `air-drain.in-air.air-loss.interval-ticks` | `20` | Sets the drain interval when players are not in water |
| `air-drain.in-air.damage.enabled` | `true` | Damages players after depleted air reaches the threshold |
| `air-drain.in-water.enabled` | `false` | Adds entropy scaling to underwater air loss |

The default drain tiers for players not in water are:

```yaml
air-loss:
  interval-ticks: 20
  tiers:
    - min-entropy: 1
      amount: 1
    - min-entropy: 2
      amount: 2
    - min-entropy: 3
      amount: 4
    - min-entropy: 5
      amount: 7
    - min-entropy: 8
      amount: 12
```

Use lower amounts when players should have more time to leave an entropy area. Use higher amounts when entropy zones should force players to retreat.

## Breathing protection

Potion effects and enchantments follow vanilla expectations by default.

- Water Breathing stops entropy air loss and damage, then allows air to regenerate
- Conduit Power stops entropy air loss and damage, then allows air to regenerate
- Breath of the Nautilus stops entropy air loss and damage, but does not refill air
- Respiration reduces EntropyBreath air loss when players are not in water

Change these rules under `air-drain.breathing-protection`.

## Commands and permissions

EntropyBreath has one admin command.

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/entropybreath reload` | `/ebreath reload` | `entropybreath.command.reload` | Reloads `config.yml` |

The reload permission defaults to `op`.

## Plugin metadata

The Paper plugin metadata identifies EntropyBreath as an EntropyCore-dependent plugin.

```yaml
name: EntropyBreath
main: io.github.isquyet.entropybreath.EntropyBreath
api-version: '1.21.11'
load: POSTWORLD
```

The description used by Paper comes from `gradle.properties`:

```properties
description=A Paper plugin that makes EntropyCore danger zones drain player air.
```

## Build from source

Build EntropyCore's API before building EntropyBreath.

```powershell
cd ..\entropy-core
.\gradlew.bat :api:publishToMavenLocal
```

Then build EntropyBreath:

```powershell
cd ..\entropy-breath
.\gradlew.bat build
```

Run tests with:

```powershell
.\gradlew.bat test
```

The plugin jar is written to:

```text
build/libs/entropy-breath-1.0-SNAPSHOT.jar
```

## Troubleshooting

Start with the server log when the plugin does not behave as expected.

- **EntropyBreath disables itself**: Install EntropyCore and confirm it loads before EntropyBreath
- **Players do not lose air**: Check that the player's local entropy is above `0` and `air-drain.enabled` is `true`
- **Creative players are ignored**: Remove `CREATIVE` from `air-drain.ignored-game-modes`
- **Water behavior does not change**: Set `air-drain.in-water.enabled` to `true`
- **Reload command is denied**: Grant `entropybreath.command.reload` or run the command as an operator
