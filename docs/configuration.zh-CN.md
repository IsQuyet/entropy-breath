# 配置参考

[English](configuration.md) | [简体中文](configuration.zh-CN.md)

EntropyBreath 在启动和重载时读取 `plugins/EntropyBreath/config.yml`。这个文件只控制插件自己的扣氧、伤害和空气恢复阻止；原版水下氧气仍由 Minecraft 处理。

## 配置文件

```yaml
entropy-breath:
  # EntropyBreath 扣氧、伤害和恢复阻止的总开关。
  enabled: true

  # 输出扣氧和恢复判定日志。正式服务器建议保持关闭。
  debug: false

  # 原版呼吸相关效果如何保护玩家免受 EntropyBreath 影响。
  # 这一段不修改原版水下氧气机制。
  plugin-breathing-protection:
    water-breathing:
      stops-air-loss: true       # 阻止插件扣氧。
      stops-damage: true         # 阻止插件伤害。
      allows-regeneration: true  # 插件压力生效时仍允许空气恢复。
    conduit-power:
      stops-air-loss: true
      stops-damage: true
      allows-regeneration: true
    nautilus-breath:
      stops-air-loss: true
      stops-damage: true
      allows-regeneration: false # 暂停压力，但不主动恢复空气。
    respiration:
      reduces-in-air-loss: true  # Respiration 降低不在水里时的插件扣氧。
      reduces-in-water-loss: false # 通常保持 false，避免和原版水下 Respiration 重复减免。

  # 这些游戏模式的玩家会被 EntropyBreath 忽略。
  ignored-game-modes:
    - CREATIVE
    - SPECTATOR

  # 基于绝对 Y 高度的额外扣氧。没有 EntropyCore 时也可以独立工作。
  height-air-loss:
    enabled: true
    applies-to:
      in-air: true   # 玩家不在水里时应用高度压力。
      in-water: false # 水下插件扣氧也加入高度压力。
    regeneration:
      prevent-when-active: true # 高度压力生效时阻止自然空气恢复。
    neutral-y: 64 # 高于此值的 tier 向上生效，低于此值的 tier 向下生效。
    tiers:
      # y >= neutral-y：玩家 Y 大于等于该值时生效。
      - y: 256
        amount: 3
      - y: 192
        amount: 2
      - y: 128
        amount: 1
      # y < neutral-y：玩家 Y 小于等于该值时生效。
      - y: 0
        amount: 1
      - y: -32
        amount: 2
      - y: -64
        amount: 3
      # 多个 tier 同时命中时，使用最高 amount。

  # 玩家不在水里时的 EntropyBreath 行为。
  in-air:
    enabled: true
    regeneration:
      prevent: true # entropy 或高度压力生效时阻止空气恢复。
    min-air: -20 # EntropyBreath 能设置的最低空气值。
    air-loss:
      interval-ticks: 20 # 扣氧间隔；和水下调整共用同一个计时器。
      tiers:
        # 本地 entropy 大于等于 min-entropy 时命中该 tier。
        # 多个 tier 同时命中时，使用最高 amount。
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
      # fixed：空气值已经小于等于 0 后，使用 fixed-loss。
      # environment：继续使用最终环境扣氧值，也就是 entropy 扣氧 + 高度扣氧。
      mode: fixed
      fixed-loss: 20
    damage:
      enabled: true
      interval-ticks: 20
      air-threshold: -20 # 空气值小于等于此值时可以造成伤害。
      amount: 2.0
      # 伤害类型：generic、suffocation 或 drowning。
      type: suffocation
      preserve-overflow-reset: true # 伤害重置空气时保留溢出的空气债务。
      reset-air-to: 0

  # 只在原版已经触发水下扣氧后追加插件压力。
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
      # environment：继续使用最终环境扣氧值，也就是 entropy 扣氧 + 高度扣氧。
      # fixed：空气值已经小于等于 0 后，使用 fixed-loss。
      mode: environment
      fixed-loss: 20
    drowning-damage:
      enabled: false # 插件水下压力生效时，调整原版溺水伤害。
      air-threshold: -20
      amount: 2.0
      preserve-overflow-reset: true
      reset-air-to: 0
```

## 与原版机制的关系

### 插件呼吸保护

`plugin-breathing-protection` 不控制原版氧气。它只决定原版效果如何保护玩家免受 EntropyBreath 影响。

### 水下扣氧

EntropyBreath 不替换原版水下扣氧。只有在原版已经触发水下空气减少时，`in-water` 或水下高度压力才会追加插件扣减。

### 耗尽空气

耗尽空气模式使用最终环境扣氧值。这个值包含 entropy 扣减和高度扣减。

### `drowningDamage` 游戏规则

EntropyBreath 专门适配了 Minecraft 的 `drowningDamage` 游戏规则。当 `drowningDamage` 为 `false` 时，插件仍会扣减空气并阻止空气恢复，但不会造成插件空气耗尽伤害。
