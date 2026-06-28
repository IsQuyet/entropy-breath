# EntropyBreath

[English](README.md) | [简体中文](README.zh-CN.md)

EntropyBreath is a Paper plugin that makes dangerous environments harder to breathe in. It can read local entropy from EntropyCore when available, adds optional height-based oxygen pressure, drains a player's air supply, blocks air regeneration when configured, and can damage players who stay in unsafe areas too long.

Use it when you want EntropyCore danger zones, extreme heights, or both to affect moment-to-moment survival without adding new commands or custom items.

## Features

EntropyBreath focuses on one gameplay loop: environmental pressure makes air scarce.

- Drains air while a player stands in an area where entropy or height pressure is active
- Scales air loss through configurable entropy tiers
- Adds configurable air loss at extreme absolute Y levels
- Blocks natural air regeneration in dangerous areas by default
- Applies configurable damage after air is depleted
- Respects Water Breathing, Conduit Power, Breath of the Nautilus, and Respiration
- Ignores creative and spectator players by default
- Keeps one air drain timer per player, so moving into or out of water cannot reset the drain interval

## Requirements

Install EntropyBreath on a Paper server. EntropyCore is optional.

- Paper `1.21.11`
- Java `21`
- EntropyCore `1.0-SNAPSHOT` optional, only needed for entropy-based air loss
- EntropyBreath `1.0-SNAPSHOT`

When EntropyCore is not installed or does not expose `EntropyService`, EntropyBreath treats local entropy as `0`. Height-based air loss can still work normally.

## Installation

Install EntropyBreath before starting the server. Install EntropyCore too if you want entropy-based air loss.

1. Build or download `entropy-breath-1.0-SNAPSHOT.jar`
2. Optional: build or download `entropy-core-1.0-SNAPSHOT.jar`
3. Copy the jars into the server `plugins` directory:

```text
plugins/entropy-core-1.0-SNAPSHOT.jar
plugins/entropy-breath-1.0-SNAPSHOT.jar
```

4. Start the server once to generate `plugins/EntropyBreath/config.yml`
5. Edit the config if needed
6. Run `/entropybreath reload` after config changes

## Default gameplay

By default, entropy and height pressure affect breathing only when players are not in water. If EntropyCore is not present, only height pressure can contribute air loss.

When a player enters a location with entropy above `0`, EntropyBreath decreases the player's air every `20` ticks. Higher entropy values remove more air per interval. At the default settings, entropy values `1`, `2`, `3`, `5`, and `8` map to stronger drain tiers.

Height pressure uses the player's absolute Y level. The default config adds extra air loss at Y `0` and below, then again at Y `128` and above. Y levels near sea level add no height pressure.

In water, vanilla Minecraft already drains air and applies drowning damage. EntropyBreath leaves that behavior unchanged by default. Enable `entropy-breath.in-water` only if entropy should make underwater areas more dangerous too. Enable `entropy-breath.height-air-loss.applies-to.in-water` if height pressure should also affect underwater air loss.

## Configuration

Edit `plugins/EntropyBreath/config.yml` after the first server start. The default source file lives at `src/main/resources/config.yml`.

See [Configuration reference](docs/configuration.md) for the full option list.

Key options:

| Path | Default | Effect |
| --- | --- | --- |
| `entropy-breath.enabled` | `true` | Enables the plugin's air loss logic |
| `entropy-breath.debug` | `false` | Logs drain and regeneration decisions |
| `entropy-breath.plugin-breathing-protection.water-breathing.stops-air-loss` | `true` | Lets Water Breathing stop EntropyBreath air loss |
| `entropy-breath.plugin-breathing-protection.respiration.reduces-in-air-loss` | `true` | Lets Respiration reduce EntropyBreath air loss when players are not in water |
| `entropy-breath.ignored-game-modes` | `CREATIVE`, `SPECTATOR` | Skips players in these game modes |
| `entropy-breath.height-air-loss.enabled` | `true` | Adds air loss from absolute Y level |
| `entropy-breath.height-air-loss.applies-to.in-air` | `true` | Applies height air loss when players are not in water |
| `entropy-breath.height-air-loss.applies-to.in-water` | `false` | Applies height air loss to underwater air loss |
| `entropy-breath.height-air-loss.regeneration.prevent-when-active` | `true` | Blocks natural air regeneration when height pressure is active |
| `entropy-breath.in-air.regeneration.prevent` | `true` | Allows EntropyBreath to block air regeneration in dangerous areas |
| `entropy-breath.in-air.air-loss.interval-ticks` | `20` | Sets the drain interval when players are not in water |
| `entropy-breath.in-air.damage.enabled` | `true` | Damages players after depleted air reaches the threshold |
| `entropy-breath.in-water.enabled` | `false` | Adds entropy scaling to underwater air loss |

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

The default height tiers add oxygen pressure at extreme Y levels:

```yaml
height-air-loss:
  neutral-y: 64
  tiers:
    - y: 256
      amount: 3
    - y: 192
      amount: 2
    - y: 128
      amount: 1
    - y: 0
      amount: 1
    - y: -32
      amount: 2
    - y: -64
      amount: 3
```

Tiers below `neutral-y` apply at or below their `y` value. Tiers above `neutral-y` apply at or above their `y` value. If multiple height tiers match, EntropyBreath uses the highest `amount`.

## Plugin breathing protection

This section controls how vanilla breathing effects protect players from EntropyBreath. It does not change vanilla underwater breathing.

- Water Breathing stops EntropyBreath air loss and damage, then allows air to regenerate
- Conduit Power stops EntropyBreath air loss and damage, then allows air to regenerate
- Breath of the Nautilus stops EntropyBreath air loss and damage, but does not refill air
- Respiration reduces EntropyBreath air loss when players are not in water

Change these rules under `entropy-breath.plugin-breathing-protection`.

## Vanilla drowning damage gamerule

EntropyBreath explicitly respects Minecraft's `drowningDamage` gamerule. When `drowningDamage` is `false`, the plugin still applies air loss and regeneration blocking, but it does not deal plugin air-depletion damage.

## Commands and permissions

EntropyBreath provides two admin commands.

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/entropybreath reload` | `/ebreath reload` | `entropybreath.command.reload` | Reloads `config.yml` |
| `/entropybreath status` | `/ebreath status` | `entropybreath.command.status` | Shows your current plugin air change |

Both command permissions default to `op`.

## Plugin metadata

The Paper plugin metadata identifies EntropyCore as an optional server dependency.

```yaml
name: EntropyBreath
main: io.github.isquyet.entropybreath.EntropyBreath
api-version: '1.21.11'
load: POSTWORLD
dependencies:
  server:
    EntropyCore:
      required: false
```

The description used by Paper comes from `gradle.properties`:

```properties
description=A Paper plugin that makes entropy and extreme height drain player air.
```

## Build from source

Build EntropyBreath:

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

- **Entropy-based air loss does not work**: Install EntropyCore and confirm it exposes `EntropyService`
- **Players do not lose air**: Check that local entropy or height pressure is active and `entropy-breath.enabled` is `true`
- **Creative players are ignored**: Remove `CREATIVE` from `entropy-breath.ignored-game-modes`
- **Water behavior does not change**: Set `entropy-breath.in-water.enabled` to `true` for entropy, or `entropy-breath.height-air-loss.applies-to.in-water` to `true` for height pressure
- **Reload command is denied**: Grant `entropybreath.command.reload` or run the command as an operator
