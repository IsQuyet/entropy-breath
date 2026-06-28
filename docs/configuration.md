# Configuration reference

[English](configuration.md) | [简体中文](configuration.zh-CN.md)

EntropyBreath reads `plugins/EntropyBreath/config.yml` on startup and reload. This file controls EntropyBreath air loss, damage, and air-regeneration blocking only. Vanilla underwater oxygen remains Minecraft behavior.

## Configuration file

```yaml
entropy-breath:
  # Master switch for EntropyBreath air loss, damage, and recovery blocking.
  enabled: true

  # Logs drain and regeneration decisions. Keep disabled outside testing.
  debug: false

  # How vanilla breathing effects protect players from EntropyBreath.
  # This section does not change vanilla underwater oxygen.
  plugin-breathing-protection:
    water-breathing:
      stops-air-loss: true       # Cancels plugin air loss.
      stops-damage: true         # Cancels plugin damage.
      allows-regeneration: true  # Allows air recovery even under plugin pressure.
    conduit-power:
      stops-air-loss: true
      stops-damage: true
      allows-regeneration: true
    nautilus-breath:
      stops-air-loss: true
      stops-damage: true
      allows-regeneration: false # Pauses pressure without refilling air.
    respiration:
      reduces-in-air-loss: true  # Applies Respiration to plugin air loss outside water.
      reduces-in-water-loss: false # Usually false because vanilla already applies Respiration underwater.

  # Players in these game modes are ignored by EntropyBreath.
  ignored-game-modes:
    - CREATIVE
    - SPECTATOR

  # Extra air loss from absolute Y level. This can work without EntropyCore.
  height-air-loss:
    enabled: true
    applies-to:
      in-air: true   # Applies outside active underwater breathing.
      in-water: false # Adds height pressure to the plugin's underwater adjustment.
    regeneration:
      prevent-when-active: true # Blocks natural air recovery while height pressure is active.
    neutral-y: 64 # Tiers above this apply upward; tiers below this apply downward.
    tiers:
      # y >= neutral-y: applies when player Y is at least this value.
      - y: 256
        amount: 3
      - y: 192
        amount: 2
      - y: 128
        amount: 1
      # y < neutral-y: applies when player Y is at most this value.
      - y: 0
        amount: 1
      - y: -32
        amount: 2
      - y: -64
        amount: 3
      # If multiple tiers match, the highest amount is used.

  # EntropyBreath behavior outside active underwater breathing.
  in-air:
    enabled: true
    regeneration:
      prevent: true # Blocks air recovery while entropy or height pressure is active.
    min-air: -20 # Lowest air value EntropyBreath can set.
    air-loss:
      interval-ticks: 20 # Air loss interval. Shared with underwater adjustments.
      tiers:
        # Each tier applies when local entropy is at least min-entropy.
        # If multiple tiers match, the highest amount is used.
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
    depleted-air:
      # fixed: use fixed-loss after air reaches 0 or lower.
      # environment: keep using entropy air loss plus height air loss.
      mode: fixed
      fixed-loss: 20
    damage:
      enabled: true
      interval-ticks: 20
      air-threshold: -20 # Damage can apply at or below this air value.
      amount: 2.0
      # Damage type: generic, suffocation, or drowning.
      type: suffocation
      preserve-overflow-reset: true # Keeps overflow air debt when damage resets air.
      reset-air-to: 0

  # Extra pressure added only after vanilla triggers underwater air loss.
  in-water:
    enabled: false
    min-air: -20
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
    depleted-air-loss:
      # environment: keep using entropy air loss plus height air loss.
      # fixed: use fixed-loss after air reaches 0 or lower.
      mode: environment
      fixed-loss: 20
    drowning-damage:
      enabled: false # Adjusts vanilla drowning damage while plugin underwater pressure is active.
      air-threshold: -20
      amount: 2.0
      preserve-overflow-reset: true
      reset-air-to: 0
```

## Vanilla interaction

### Plugin breathing protection

`plugin-breathing-protection` does not control vanilla oxygen. It only decides how vanilla effects protect players from EntropyBreath.

### Underwater air loss

EntropyBreath does not replace vanilla underwater air loss. When vanilla has already triggered an underwater air decrease, `in-water` or height pressure in water can add plugin air loss.

### Depleted air

Depleted-air modes use the final environment air loss value. This value includes entropy air loss plus height air loss.

### `drowningDamage` gamerule

EntropyBreath explicitly respects Minecraft's `drowningDamage` gamerule. When `drowningDamage` is `false`, the plugin still applies air loss and regeneration blocking, but it does not deal plugin air-depletion damage.
