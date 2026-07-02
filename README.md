# BorderEffects

A PaperMC / Purpur plugin that applies configurable potion effects to players when they are close to the world border.

Supports both **vanilla Minecraft borders** and **[ChunkyBorder](https://github.com/pop4959/ChunkyBorder)** (optional soft dependency).

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java | 21+ |
| Paper / Purpur | 1.21+ (API-compatible) |
| ChunkyBorder | Any (optional) |

---

## Building

```bash
mvn package
```

The compiled jar will be in `target/BorderEffects-0.1.0.jar`.

> **Note:** The build requires network access to [repo.papermc.io](https://repo.papermc.io/repository/maven-public/) and [repo.codemc.io](https://repo.codemc.io/repository/maven-public/).

---

## Installation

1. Copy `BorderEffects-0.1.0.jar` into your server's `plugins/` folder.
2. If you use ChunkyBorder, ensure its jar is also in `plugins/` — the plugin auto-detects it.
3. Start / restart the server.

---

## Configuration (`plugins/BorderEffects/config.yml`)

```yaml
# How often to check player positions (in ticks; 20 ticks = 1 second)
check-interval: 10

# Distance from the border edge (in blocks) at which effects begin to apply
warning-distance: 20.0

# Potion effects applied while a player is within the warning distance
effects:
  - type: BLINDNESS
    amplifier: 0
    duration: 60
  - type: SLOWNESS
    amplifier: 1
    duration: 60
```

### Effect types

Use any value from the [PotionEffectType](https://jd.papermc.io/paper/1.21/) list, e.g. `BLINDNESS`, `SLOWNESS`, `WEAKNESS`, `NAUSEA`, `DARKNESS`.

---

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/bordereffects reload` | `bordereffects.admin` | Reload config without restarting |

Aliases: `/be reload`

---

## How it works

Every `check-interval` ticks the plugin checks every online player:

1. **ChunkyBorder** (if installed): checks whether the player is inside a ChunkyBorder region for their world and within `warning-distance` blocks of its edge. Works with all ChunkyBorder shapes (square, rectangle, circle, ellipse, polygon).
2. **Vanilla border**: checks whether the player is within `warning-distance` blocks of the vanilla `WorldBorder`. The vanilla border is ignored if it is at its default size (≥ 59,000,000 blocks).

When a player enters the warning zone the configured effects are applied and refreshed continuously. When they leave, the effects are removed immediately.
