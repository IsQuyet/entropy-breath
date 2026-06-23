# EntropyBreath

EntropyBreath is a Paper addon plugin for EntropyCore. It drains player air based on the final entropy value at the player's current location.

## Behavior

- Queries `EntropyService` from EntropyCore through Bukkit services.
- Drains online players' air on a configurable interval when local entropy is greater than 0.
- Uses tiered low-integer entropy thresholds because EntropyCore's base entropy grows slowly.
- Optionally blocks vanilla air regeneration in entropy-positive areas.
- Ignores creative and spectator players by default.

## Build

Publish EntropyCore's API to Maven Local first:

```powershell
cd ..\entropy-core
.\gradlew.bat :api:publishToMavenLocal
```

Then build this addon:

```powershell
cd ..\entropy-breath
.\gradlew.bat build
```

## Runtime

Install both plugin jars on the server:

```text
plugins/entropy-core-1.0-SNAPSHOT.jar
plugins/entropy-breath-1.0-SNAPSHOT.jar
```

`EntropyBreath` declares a required Paper server dependency on `EntropyCore` and joins the EntropyCore classpath so it can access the shared API classes at runtime.
