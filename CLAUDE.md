# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PlayerTP (玩家互传) — a Minecraft Forge 1.20.1 mod that enables player-to-player teleportation, personal waypoint creation, shared/public waypoints, and an in-world beacon HUD overlay.

- **Mod ID**: `playertp`
- **Minecraft**: 1.20.1, **Forge**: 47.4.20, **Java**: 17
- **Mappings**: Parchment `2023.09.03-1.20.1`
- **Language**: Fully localized 中文/English — all UI text lives in `assets/playertp/lang/zh_cn.json` / `en_us.json` via translatable keys

## Build & Run Commands

```bash
# Build the mod JAR
./gradlew build                    # outputs to build/libs/

# Run the Minecraft client with the mod loaded
./gradlew runClient

# Run a dedicated server with the mod
./gradlew runServer

# Refresh dependencies / reset Gradle cache
./gradlew --refresh-dependencies
./gradlew clean

# Generate IDE run configurations (IntelliJ)
./gradlew genIntellijRuns
```

The Gradle working directory for all commands is `forge-1.20.1-47.4.20/`. The mod JAR is produced at `build/libs/playertp-1.0.0.jar`.

## Architecture

### Data flow

All persistent state lives **server-side** via Minecraft's `SavedData` (world-saved NBT). The client never holds authoritative data — it receives snapshots via sync packets.

```
Client (GUI / key press)
  → sends packet to server (ModNetwork.sendToServer)
    → server handler reads/writes SavedData
    → server sends sync packet back to client(s)
      → client GUI updates from sync packet
```

### Core data model (`core/` package)

Four `SavedData` classes, each stored in the overworld's data storage, keyed by mod ID prefix:

| Class | Shared key | What it stores |
|---|---|---|
| `PersonalTeleportPoints` | `playertp_personal_points` | Per-player teleport points (owner-locked) |
| `PublicTeleportPoints` | `playertp_public_points` | Globally shared teleport points |
| `PlayerData` | `playertp_player_data` | Hidden-players set (opt-out of appearing in player list) |
| `TeleportHistory` | `playertp_teleport_history` | Last 10 teleport destinations per player |

`TeleportPoint` is the shared value object — immutable fields (`id`, `ownerUUID`, `ownerName`, `x`, `y`, `z`, `dimension`, `timestamp`) plus a mutable `name`. Serializes to/from NBT.

`TeleportHelper` is the single teleport execution path — handles cross-dimension teleport, experience cost checking (from `Config`), chunk loading tickets, and recording history.

### Networking (`network/ModNetwork.java`)

All packets are inner static classes of `ModNetwork`. The `SimpleChannel` is registered in `FMLCommonSetupEvent`. Protocol version `"1.0"`.

14 packet types, following a C→S request / S→C sync pattern:
- **Personal points**: `AddPersonalPointPacket`, `DeletePersonalPointPacket`, `SyncPersonalPointsPacket`
- **Public points**: `SharePointPacket` (moves personal→public), `DeletePublicPointPacket`, `SyncPublicPointsPacket`
- **Renaming**: `RenamePointPacket` (used for both personal and public, with a boolean flag)
- **Teleport**: `TeleportToPointPacket`, `TeleportToPlayerPacket`, `QuickAddPointPacket`
- **Player list**: `RequestPlayerListPacket` → `SyncPlayerListPacket`, `ToggleVisibilityPacket`
- **Bootstrap**: `RequestSyncPointsPacket` (sent when opening any screen; server responds with both personal + public sync)

### GUI screens (`gui/` package)

Three main screens, all custom-drawn (no use of Minecraft's built-in list widgets):
- `PersonalTeleportScreen` — list personal points with teleport/delete/share/rename buttons; contains inner `RenameScreen`
- `PublicTeleportScreen` — list public points with teleport/delete/rename
- `PlayerListScreen` — online player list with teleport-to-player; self-entry always first with visibility toggle

Each screen requests a sync when opened and holds a `static List<...>` that sync packets update. Navigation buttons link between the three screens. All screens set `isPauseScreen() = false` (inventory-style overlay).

### HUD renderer (`renderer/CompassRenderer.java`)

Subscribed to `RenderGuiOverlayEvent.Post` (crosshair layer). Renders up to 6 directional beacon markers for the nearest in-dimension teleport points. Personal points are blue, public are orange. Controlled by `ClientConfig` toggles and `beaconMaxDistance`.

### Configuration

| File | Type | Contents |
|---|---|---|
| `Config.java` | Server/common | `requireExperience` (bool), `teleportExperienceCost` (int 1-1000) |
| `config/ClientConfig.java` | Client | `renderPersonalPoints`, `renderPublicPoints`, `beaconMaxDistance` |

Config values are read from static fields (`Config.requireExperience`, `Config.teleportExperienceCost`) that are populated in `onLoad`/`onReload` event handlers.

### API (`api/PlayerTPAPI.java`)

Public static API for other mods to programmatically trigger teleports, query history, and manage player visibility. Delegates to `TeleportHelper`, `TeleportHistory`, and `PlayerData`.

### Key bindings

| Key | Action |
|---|---|
| `P` | Open personal teleport points |
| `L` | Open public teleport points |
| `=` | Open player list |
| `-` | Quick-add current position as personal point |

Registered in `ExampleMod.onRegisterKeyMappings`, handled in `ExampleMod.onKeyInput`.

## Important conventions

- **Localization**: All UI text uses `Component.translatable()` with keys in `assets/playertp/lang/zh_cn.json` and `en_us.json`. When adding new user-facing text, add the key to BOTH lang files. Two strings are intentionally NOT localized (persisted NBT data, generated server-side): the death point name format in `ExampleMod` and the server-side default point name fallback in `ModNetwork`.
- **SavedData pattern**: To add new persistent data, extend `SavedData`, provide `read(CompoundTag)` and `save(CompoundTag)`, and fetch via `server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(...)`. The `setDirty()` call is what triggers NBT writes.
- **Networking pattern**: Each packet needs `encode`, `decode`, and `handle` static methods. Server-side handlers must run inside `ctx.get().enqueueWork()` and always check `ctx.get().getSender() != null`. Sync packets deliver to a specific player (`PLAYER.with(() -> player)`) for personal data or all players (`ALL.noArg()`) for public data.
- **Client-side static state in GUIs**: The three screens hold data in `static` lists updated by sync packets. This means data persists across screen open/close but could be stale — each screen calls `RequestSyncPointsPacket` (or `RequestPlayerListPacket`) in `init()`.
