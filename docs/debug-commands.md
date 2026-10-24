# Debug commands (debug build only)

Control the running Android wallpaper engine from a host via `adb`.

## Transport

- Receiver: `com.homm3.livewallpaper.debug.DebugCommandReceiver`, declared only in `android/src/debug/AndroidManifest.xml` (exported). Absent in release.
- Always send with an explicit component (`-n`); implicit broadcasts don't reach manifest receivers on API 26+.
- Reply: `setResultCode` + `setResultData(json)`. `am broadcast` prints it as
  `Broadcast completed: result=<code>, data="<json>"`.
  - `result=0` → success, data is a JSON object (`{}` when nothing to return).
  - `result=1` → failure, data is `{"error":"<message>"}`.
- Receiver requires `android.permission.DUMP` (held by `adb shell`, not grantable to third-party apps).
- If no wallpaper engine is running: `result=1`, `{"error":"engine not running"}`.
- While the wallpaper is hidden (screen off, app on top) the GL thread is paused, so engine commands return `{"error":"timeout"}`. Wake the device first: `adb shell input keyevent KEYCODE_WAKEUP`.
- Unknown action or invalid extra: `result=1` with a descriptive error.

```
adb shell am broadcast \
  -n com.homm3.livewallpaper/.debug.DebugCommandReceiver \
  -a com.homm3.livewallpaper.debug.<CMD> [extras]
```

## Commands

Action = `com.homm3.livewallpaper.debug.<CMD>`.

| CMD                      | Extras                                                    | Desktop key   | Effect                                   |
|--------------------------|-----------------------------------------------------------|---------------|------------------------------------------|
| `PING`                   | —                                                         | —             | Returns `{"pong":true}`                  |
| `MAP_NEXT`               | `--es direction forward\|backward` (default `forward`)    | `]` / `[`     | `MapCommand.LoadNext(direction)`         |
| `MAP_RANDOM_VIEW`        | —                                                         | `Enter`       | `randomizeVisibleMapPart(force = true)`  |
| `MAP_TOGGLE_UNDERGROUND` | —                                                         | `U`           | `toggleUnderground()`                    |
| `CAMERA_PAN`             | `--ei dx N --ei dy N` (tiles, default 0)                  | arrows        | Translate camera by N × `TILE_SIZE`; arrow Up = `dy -1`, Down = `dy 1`, Left = `dx -1`, Right = `dx 1` (same signs as `CameraInputProcessor`) |
| `CAMERA_PAN_SCREEN`      | `--ef fx F --ef fy F` (fraction of visible width/height, default 0) | — | Translate camera by `fx × worldWidth`, `fy × worldHeight` of the viewport; same signs as `CAMERA_PAN`. TUI arrows send ±0.75 |
| `CAMERA_ZOOM`            | `--es action in\|out\|reset`                              | `=` / `-` / `0` | Same step (0.1) and clamps as `CameraInputProcessor` |
| `PREFS_SET`              | any of: `--ei scale 1..5`, `--es mapUpdateInterval EVERY_SWITCH\|MINUTES_10\|MINUTES_30\|HOURS_2\|HOURS_24`, `--ez useScroll true\|false`, `--ef brightness <float>` | — | Writes to `WallpaperPreferencesRepository` (DataStore); engine picks it up via prefs flow. At least one extra required. Returns the resulting prefs. |
| `STATE_GET`              | —                                                         | —             | Returns state JSON (below)               |
| `ENGINE_REFRESH`         | —                                                         | —             | `MapCommand.Refresh` (as on resume)      |

`PREFS_SET` works even when the engine isn't running.

## STATE_GET response

```json
{
  "screen": "GameScreen | LoadingScreen | AssetSetupScreen | null",
  "map": "<current map name or null>",
  "underground": false,
  "zoom": 1.0,
  "camera": {"x": 0.0, "y": 0.0},
  "prefs": {"scale": 2, "mapUpdateInterval": "MINUTES_10", "useScroll": true, "brightness": 1.0}
}
```

All engine-side work runs on the GL thread (`Gdx.app.postRunnable`); the receiver waits for the result (`goAsync()`, timeout 3 s → `result=1`, `{"error":"timeout"}`) and calls `requestRendering()`.
