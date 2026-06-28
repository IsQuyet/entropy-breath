# EntropyBreath

[English](README.md) | [简体中文](README.zh-CN.md)

EntropyBreath 是一个 Paper 插件，用于让危险环境变得更难呼吸。它可以在 EntropyCore 可用时读取本地 entropy，加入可选的高度氧气压力，扣减玩家的空气值，按配置阻止空气恢复，并能对长时间停留在危险区域的玩家造成伤害。

如果你希望 EntropyCore 的危险区域、极端高度，或两者一起影响玩家的即时生存压力，又不想加入新命令或自定义物品，可以使用这个插件。

## 功能

EntropyBreath 专注于一个玩法循环：环境压力会让空气变稀缺。

- 玩家处于 entropy 或高度压力生效的区域时扣减空气值
- 通过可配置的 entropy 分层调整空气扣减强度
- 在极端绝对 Y 高度加入可配置的额外空气扣减
- 在危险区域默认阻止空气自然恢复
- 空气耗尽后造成可配置的伤害
- 兼容 Water Breathing、Conduit Power、Breath of the Nautilus 与 Respiration 的保护效果
- 默认忽略创造模式和旁观模式玩家
- 每个玩家只使用一个空气扣减计时器，因此进出水时不会重置扣减间隔

## 运行要求

在 Paper 服务器上安装 EntropyBreath。EntropyCore 是可选依赖。

- Paper `1.21.11`
- Java `21`
- EntropyCore `1.0-SNAPSHOT` 可选，仅在需要基于 entropy 扣氧时安装
- EntropyBreath `1.0-SNAPSHOT`

如果没有安装 EntropyCore，或 EntropyCore 没有暴露 `EntropyService`，EntropyBreath 会把本地 entropy 当作 `0`。基于高度的空气扣减仍可正常工作。

## 安装

启动服务器前先安装 EntropyBreath。如果需要基于 entropy 的扣氧，再同时安装 EntropyCore。

1. 构建或下载 `entropy-breath-1.0-SNAPSHOT.jar`
2. 可选：构建或下载 `entropy-core-1.0-SNAPSHOT.jar`
3. 将 jar 复制到服务器 `plugins` 目录：

```text
plugins/entropy-core-1.0-SNAPSHOT.jar
plugins/entropy-breath-1.0-SNAPSHOT.jar
```

4. 启动服务器一次，生成 `plugins/EntropyBreath/config.yml`
5. 按需要编辑配置
6. 修改配置后运行 `/entropybreath reload`

## 默认玩法

默认配置下，entropy 和高度压力只会在玩家不在水里时影响呼吸。如果没有安装 EntropyCore，则只有高度压力会参与扣氧。

当玩家进入 entropy 大于 `0` 的区域时，EntropyBreath 每 `20` tick 扣减一次空气值。entropy 越高，每次扣减的空气越多。默认配置下，entropy 值 `1`、`2`、`3`、`5`、`8` 会对应更强的扣减分层。

高度压力使用玩家的绝对 Y 值。默认配置会在 Y `0` 及以下增加额外空气扣减，也会在 Y `128` 及以上再次增加额外空气扣减。接近海平面的高度不会产生高度压力。

在水中，原版 Minecraft 已经会扣减空气并造成溺水伤害。EntropyBreath 默认不改变水中行为。只有当你希望 entropy 也让水下区域更危险时，才需要启用 `entropy-breath.in-water`；如果希望高度压力也影响水下空气扣减，则启用 `entropy-breath.height-air-loss.applies-to.in-water`。

## 配置

第一次启动服务器后，编辑 `plugins/EntropyBreath/config.yml`。默认配置源文件位于 `src/main/resources/config.yml`。

完整选项见[配置参考](docs/configuration.zh-CN.md)。

关键选项：

| 路径 | 默认值 | 效果 |
| --- | --- | --- |
| `entropy-breath.enabled` | `true` | 启用插件的空气扣减逻辑 |
| `entropy-breath.debug` | `false` | 输出扣减和恢复判定日志 |
| `entropy-breath.plugin-breathing-protection.water-breathing.stops-air-loss` | `true` | 让 Water Breathing 阻止 EntropyBreath 空气扣减 |
| `entropy-breath.plugin-breathing-protection.respiration.reduces-in-air-loss` | `true` | 让 Respiration 降低玩家不在水里时的 EntropyBreath 空气扣减 |
| `entropy-breath.ignored-game-modes` | `CREATIVE`, `SPECTATOR` | 跳过这些游戏模式的玩家 |
| `entropy-breath.height-air-loss.enabled` | `true` | 根据绝对 Y 高度加入额外空气扣减 |
| `entropy-breath.height-air-loss.applies-to.in-air` | `true` | 玩家不在水里时启用高度空气扣减 |
| `entropy-breath.height-air-loss.applies-to.in-water` | `false` | 水下空气扣减也启用高度空气扣减 |
| `entropy-breath.height-air-loss.regeneration.prevent-when-active` | `true` | 高度压力生效时阻止空气自然恢复 |
| `entropy-breath.in-air.regeneration.prevent` | `true` | 允许 EntropyBreath 在危险区域阻止空气恢复 |
| `entropy-breath.in-air.air-loss.interval-ticks` | `20` | 设置不在水里时的空气扣减间隔 |
| `entropy-breath.in-air.damage.enabled` | `true` | 空气耗尽并达到阈值后对玩家造成伤害 |
| `entropy-breath.in-water.enabled` | `false` | 让水下空气扣减也受 entropy 影响 |

玩家不在水里时的默认扣减分层：

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

如果希望玩家有更多撤离时间，可以降低 `amount`。如果 entropy 区域应该迫使玩家撤离，可以提高 `amount`。

默认高度分层会在极端 Y 高度加入氧气压力：

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

低于 `neutral-y` 的 tier 会在玩家 Y 小于等于对应 `y` 时生效。高于 `neutral-y` 的 tier 会在玩家 Y 大于等于对应 `y` 时生效。如果多个高度 tier 同时命中，EntropyBreath 会使用最高的 `amount`。

## 插件呼吸保护

这一段控制原版呼吸相关效果如何保护玩家免受 EntropyBreath 影响，不会修改原版水下呼吸。

- Water Breathing 会阻止 EntropyBreath 空气扣减和伤害，并允许空气恢复
- Conduit Power 会阻止 EntropyBreath 空气扣减和伤害，并允许空气恢复
- Breath of the Nautilus 会阻止 EntropyBreath 空气扣减和伤害，但不会恢复空气
- Respiration 会降低 EntropyBreath 对不在水里玩家造成的空气扣减

在 `entropy-breath.plugin-breathing-protection` 下修改这些规则。

## 原版溺水伤害规则

EntropyBreath 专门适配了 Minecraft 的 `drowningDamage` 游戏规则。当 `drowningDamage` 为 `false` 时，插件仍会扣减空气并阻止空气恢复，但不会造成插件空气耗尽伤害。

## 命令与权限

EntropyBreath 提供两个管理命令。

| 命令 | 别名 | 权限 | 描述 |
| --- | --- | --- | --- |
| `/entropybreath reload` | `/ebreath reload` | `entropybreath.command.reload` | 重载 `config.yml` |
| `/entropybreath status` | `/ebreath status` | `entropybreath.command.status` | 查看当前插件空气变化 |

两个命令权限默认授予 `op`。

## 插件元数据

Paper 插件元数据会将 EntropyCore 标记为可选服务器依赖。

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

Paper 使用的插件描述来自 `gradle.properties`：

```properties
description=A Paper plugin that makes entropy and extreme height drain player air.
```

## 从源码构建

构建 EntropyBreath：

```powershell
cd ..\entropy-breath
.\gradlew.bat build
```

运行测试：

```powershell
.\gradlew.bat test
```

插件 jar 输出到：

```text
build/libs/entropy-breath-1.0-SNAPSHOT.jar
```

## 故障排查

如果插件行为不符合预期，先检查服务器日志。

- **基于 entropy 的扣氧没有生效**：安装 EntropyCore，并确认它暴露了 `EntropyService`
- **玩家没有被扣减空气值**：确认玩家所在位置的 entropy 或高度压力已生效，并且 `entropy-breath.enabled` 为 `true`
- **创造模式玩家被忽略**：从 `entropy-breath.ignored-game-modes` 移除 `CREATIVE`
- **水中行为没有变化**：如果要让 entropy 影响水中行为，将 `entropy-breath.in-water.enabled` 设为 `true`；如果要让高度压力影响水中行为，将 `entropy-breath.height-air-loss.applies-to.in-water` 设为 `true`
- **重载命令被拒绝**：授予 `entropybreath.command.reload`，或以 OP 身份执行命令
