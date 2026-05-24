# HotA Installer → Map Extraction — Design

**Goal:** When the user picks the HotA installer, the existing `HotA.lod` extraction continues to run unchanged. In addition, two new behaviors fire in the same installer pass and land their output in `user-maps/`:

1. **`.h3m` extraction from the installer payload.** Every `app/Maps/*.h3m` entry inside the installer (128 files in HotA 1.8.0) is written straight to `user-maps/`.
2. **`HotA_lng.lod` → `.h3c` → `.h3m` extraction.** `HotA_lng.lod` is extracted from the installer (transient), every `.h3c` entry inside it is found, and each `.h3c` is split into its component `.h3m` maps. All maps are written to `user-maps/`.

**Out of scope of this change:** the H3sprite.lod picker is untouched. Nothing changes for the classic-game setup path.

---

## H3C Format

An `.h3c` file is **N + 1 concatenated gzip streams**:

| Stream | Content |
|---|---|
| `0` | Campaign metadata: campaign header followed by N scenario descriptors |
| `1..N` | One gzipped `.h3m` map per scenario, in scenario order |

Verified empirically against all 11 `.h3c` campaigns inside `HotA_lng.lod`. The first u32 of each map stream (after ungzip) is always a valid `H3mVersion` value (`0x0e`/`0x15`/`0x1c`/`0x20`).

**Implication:** Streams `1..N` can be written to disk verbatim as standalone `.h3m` files — no recompression needed. The only reason to touch stream 0 is to recover scenario names for filenames.

### Stream 0 layout (campaign metadata)

Field order, transcribed from VCMI's `lib/campaign/CampaignHandler.cpp`:

**Campaign header**
1. `version: u32` — campaign version (`RoE=4`, `AB=5`, `SoD=6`, `Chr=7`, `WoG=8`, `HotA=10`)
2. **HotA-only block**:
   - `formatVersion: i32`. If `== 2`: read `hotaMajor: u32`, `hotaMinor: u32`, `hotaPatch: u32`, `forceMatchingVersion: u8`
   - `unknownB: i8`
   - `unknownC: i32`
   - `numberOfScenarios: i32`
3. `campaignMapId: u8`
4. `name: base_string` (u32 length + UTF-8 bytes)
5. `description: base_string`
6. If `version > RoE`: `difficultyChosenByPlayer: u8`
7. `music: u8`

**Scenario count**
- HotA: from explicit `numberOfScenarios` field above
- Non-HotA: looked up by `campaignMapId` from a fixed table (region count per campaign — same table VCMI uses; constant per official campaign)

**Per scenario** (`numberOfScenarios` times):
1. `mapName: base_string` ← **the only field we need**
2. `packedMapSize: u32` (ignored — we use gzip-stream boundaries)
3. preconditionRegions: `u16` if `numberOfScenarios > 8` else `u8`
4. `regionColor: u8`
5. `difficulty: u8`
6. `regionText: base_string`
7. `prolog`: video id `u8` + music id `u8` + voice id `u8` + text `base_string`
8. `epilog`: same shape as prolog
9. `travelOptions`: starting bonus, crossover heroes/creatures/artifacts bitfields, starting hero options, chosen bonus options list. Fully skipped — exact byte counts transcribed from VCMI in the implementation plan.

We only consume `mapName`; everything else is skipped just enough to reach the next scenario.

---

## Architecture

Two new units inside `core`, plus a small extension to `InnoSetupExtractor`.

### `core/src/main/kotlin/com/homm3/livewallpaper/parser/h3c/`

The h3c parser. One public symbol; rest `internal`. Same conventions as `parser.inno` and `parser.lod`.

| File | Responsibility |
|---|---|
| `H3cExtractor.kt` | Public orchestrator. Single entry point. |
| `GzipStreamSplitter.kt` | Walks a byte source, yields each concatenated gzip stream as `(compressedRange, decompressedBytes)`. Manual gzip header walk + `Inflater(nowrap=true)` + 8-byte trailer skip. `internal`. |
| `CampaignHeaderReader.kt` | Parses stream 0; returns the list of `mapName` strings. Everything else discarded. `internal`. |
| `CampaignVersion.kt` | Enum of the 6 known version values. `internal`. |
| `CampaignRegionTable.kt` | Lookup `campaignMapId → scenarioCount` for non-HotA campaigns (constant table). `internal`. |

Public surface:

```kotlin
object H3cExtractor {
    data class ExtractedMap(val name: String, val bytes: ByteArray)  // bytes = gzipped h3m

    /** Returns extracted maps in scenario order. `name` is the scenario's mapName
     *  (suffixed with .h3m if absent), or "map_<index>.h3m" if header parsing fails. */
    fun extract(input: ByteArray): List<ExtractedMap>
}
```

Only the `ByteArray` overload is needed — h3c input always comes from a LOD entry decompressed into memory (`LodReader.readFileContent`). No `File` overload until a caller needs one.

### `core/src/main/kotlin/com/homm3/livewallpaper/core/assets/CampaignMapInstaller.kt`

Glue between `LodReader`, `H3cExtractor`, and the filesystem. Wallpaper-specific policy (output directory, overwrite behavior, progress reporting) lives here.

```kotlin
object CampaignMapInstaller {
    data class Result(
        val campaignsFound: Int,
        val mapsWritten: Int,
        val skipped: List<String>,   // h3c entry names whose extraction threw
    )

    /** Scans [lod] for .h3c entries, extracts each via H3cExtractor, and writes
     *  every produced map to [outputDir]. Existing files left untouched. */
    fun installFromLod(
        lod: File,
        outputDir: File,
        onProgress: ((current: Int, total: Int, message: String) -> Unit)? = null,
    ): Result
}
```

### `InnoSetupExtractor` change

The current extractor finds a single file by destination suffix. Generalize it so one installer pass can produce all three outputs we need (`HotA.lod` + `HotA_lng.lod` + 128 `.h3m`s). Targets are matcher-based so the directory glob fits the same shape as the named-file fetches.

```kotlin
object InnoSetupExtractor {
    /** Caller-supplied target. The matcher runs against the entry's `destination`
     *  (backslashes preserved as authored in Inno Setup). `outputFor` chooses an
     *  output file given the matched destination — used by glob targets to map
     *  each match to a unique path. */
    data class Target(
        val matcher: (destination: String) -> Boolean,
        val outputFor: (destination: String) -> File,
    )

    data class ExtractedFile(val destination: String, val output: File)

    fun extract(
        installer: File,
        targets: List<Target>,
        onProgress: ((written: Long, total: Long, currentDestination: String) -> Unit)? = null,
    ): List<ExtractedFile>

    // Existing call kept for back-compat; delegates to extract(...) with one Target.
    fun extractHotaLod(installer: File, output: File, onProgress: ...)
}
```

The installer picker constructs three targets in one call:
- `data\HotA.lod` (exact-suffix) → `filesDir/HotA.lod`
- `data\HotA_lng.lod` (exact-suffix) → `cacheDir/HotA_lng.lod` (transient)
- Glob `app\Maps\*.h3m` (case-insensitive) → `userMapsDir/<sanitized basename>` (skip when target file exists)

Internally `FileEntries.findByDestinationSuffix` becomes `FileEntries.collectMatching`, returning `List<FileEntry>` while still scanning all entries to leave the cursor at the data-entries block.

---

## Pipeline wiring (AssetSetupActivity.hotaFilePickerLauncher)

```
HotA installer picker (CHANGED — H3sprite picker untouched):

  copy installer to cacheDir
  InnoSetupExtractor.extract(installer, [
      HotA.lod        → filesDir/HotA.lod,
      HotA_lng.lod    → cacheDir/HotA_lng.lod  (transient),
      app/Maps/*.h3m  → userMapsDir/<basename> (skip on collision)
  ])
  LodValidator.validate(HotA.lod)  ──── if error: delete + bail ────┐
  CampaignMapInstaller.installFromLod(HotA_lng.lod, userMapsDir)     │
  delete cacheDir/HotA_lng.lod                                       ▼
  delete cacheDir/hota_installer.exe                              fail UI
```

Three changes inside the existing `hotaFilePickerLauncher` `thread { ... }`:
1. Replace `InnoSetupExtractor.extractHotaLod(...)` with the multi-target `InnoSetupExtractor.extract(...)`.
2. After `LodValidator` succeeds, call `CampaignMapInstaller.installFromLod(lngFile, userMapsDir)`.
3. Delete `cacheDir/HotA_lng.lod` afterwards (it's only needed long enough to mine the h3c entries).

The `.h3m` files extracted from `app/Maps/*.h3m` arrive in `user-maps/` as a side effect of step 1 — no extra plumbing.

`hotaStatusMessage` updates remain the only UI surface; each new step gets a progress message ("Extracting maps...", "Extracting campaigns...").

### Data flow inside `CampaignMapInstaller`

```
HotA_lng.lod
   │
   ▼  LodReader.read()
LodArchive ── filter entries by .h3c suffix ──► List<LodEntry>
   │
   ▼  for each entry:
   │     LodReader.readFileContent(entry) ──► ByteArrayInputStream
   │     ByteArray ─► H3cExtractor.extract ──► List<ExtractedMap>
   │     for each ExtractedMap:
   │       write outputDir/<sanitized name>.h3m  (skip if exists)
   ▼
Result(campaignsFound, mapsWritten, skipped)
```

### Error handling

- **`HotA_lng.lod` not present in installer** → log + skip the campaign pass. We still extracted `HotA.lod` (the critical one) and the 128 standalone maps, so setup succeeds. (HotA 1.7.3 / 1.8.0 both ship `HotA_lng.lod`, but defending against future builds is cheap.)
- **`HotA_lng.lod` has no `.h3c` entries** → `Result(campaignsFound = 0, ...)`. Not an error.
- **`H3cExtractor` throws on one `.h3c` entry** → catch, append entry name to `skipped`, continue with next entry. One bad campaign doesn't abort.
- **Not a gzip stream at offset 0 of an h3c** → `H3cExtractor` throws `IllegalArgumentException("Not an h3c file")`; caller records in `skipped`.
- **Stream 0 parse fails partway** → fall back to numeric names (`<entryStem>_0.h3m`, `<entryStem>_1.h3m`, ...). All map streams still extracted.
- **Filename collision in `user-maps/`** → existing file wins (no overwrite). Same policy for installer-glob `.h3m`s and for campaign-derived `.h3m`s.

### Filename sanitization

`mapName` can contain characters illegal on some filesystems (`:`, `/`, `\`, `?`, `*`, `<`, `>`, `|`, `"`, control chars). Replace each with `_`. Append `.h3m` if absent. Collisions inside one campaign resolved by appending `_<index>`. The same sanitizer is reused for the installer-glob path so a hostile `app/Maps/...` destination can't escape `userMapsDir`.

---

## Testing

**Fixture policy:** No new binary fixtures land in the repo. Tests use only the fixtures the project already accesses via `TestFixtures` (`installer180`, `goldenLod180`) plus a new `lng180` accessor pointing at the HotA 1.8.0 language LOD. All accessors gate with `@Assume`, so tests skip when the corpus is absent. Anything else is built in-process (synthetic gzip concatenation).

`core/src/test/kotlin/.../h3c/`:

- **`GzipStreamSplitterTest`** — pure synthetic: build 3 concatenated gzip streams from in-memory bytes; assert offsets, lengths, and decompressed content match. No file fixtures.
- **`CampaignHeaderReaderTest`** — at test time, pull one `.h3c` blob via `LodReader.readFileContent("ab.h3c")` from `TestFixtures.lng180`; feed it through the header reader; assert the returned scenario-name list has the expected count and specific entries (hard-coded against the known AB campaign).
- **`H3cExtractorTest`** — for each `.h3c` entry inside `TestFixtures.lng180`:
  - Read the entry from the LOD into memory; pass to `H3cExtractor.extract`.
  - Assert per-campaign scenario count matches a hard-coded table keyed by entry name.
  - Each `ExtractedMap.bytes` ungzips to a stream whose first u32 is in `{0x0e, 0x15, 0x1c, 0x20}`.
  - Each `bytes` parses end-to-end via `H3mReader(bytes.inputStream()).read()` without throwing.

`core/src/test/kotlin/.../assets/`:

- **`CampaignMapInstallerTest`** — point at `TestFixtures.lng180`, run `installFromLod` to a JUnit `@TempDir`, assert:
  - `Result.campaignsFound == 11`
  - `Result.mapsWritten == <expected sum across the 11 campaigns>` (hard-coded table)
  - Every output file parses through `H3mReader`
  - Re-running the installer is a no-op (idempotent: no overwrite, `mapsWritten == 0` on the second call)

`core/src/test/kotlin/.../inno/`:

- **`InnoSetupExtractorMultiTargetTest`** — extend the existing 1.8.0 golden test to request three targets in one pass:
  - `data\HotA.lod` → byte-for-byte equality with `TestFixtures.goldenLod180` (unchanged from current test).
  - `data\HotA_lng.lod` → non-empty; `LodReader` lists 11 `.h3c` entries with the expected names.
  - Glob `app\Maps\*.h3m` (case-insensitive) → exactly 128 output files; each starts with a valid `H3mVersion` magic after gunzip; spot-check 2–3 by parsing with `H3mReader`.

Tests degrade to `assumeTrue` skips when fixtures are absent (matches the existing inno-test pattern).

---

## Android constraints

The pipeline runs on Android (the live wallpaper's host), so the design honors mobile constraints:

- **No Android-specific types in `core`.** Public APIs in `parser.h3c`, `parser.inno`, and `core.assets` take only `java.io.File`, `InputStream`/`ByteArray`, and Kotlin/JDK types. `AssetSetupActivity` supplies paths rooted under `filesDir` and `cacheDir`; the parsers don't know about app context.
- **Memory ceiling.** Largest single in-memory buffer is one LOD entry being decompressed (`LodReader.readFileContent` returns `ByteArrayInputStream`) — the biggest `.h3c` in `HotA_lng.lod` is ~770 KB compressed / ~2 MB decompressed, well under a single Android allocation budget. The installer's `app/Maps/*.h3m` glob streams each file straight to disk via `ChunkDecompressor.extract(...)`, never fully buffered. `H3cExtractor` holds one campaign at a time.
- **Disk paths from the host.** All output `File`s in the public API are caller-supplied. `CampaignMapInstaller` writes to whatever directory `AssetSetupActivity` passes (typically `filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)`); it never resolves paths itself.
- **Filename sanitization is filesystem-conservative.** Same character set stripped on every platform so output is valid on Android internal storage (ext4-backed), tmpfs, and dev hosts alike.
- **Background thread compatibility.** The pipeline already runs on a worker thread inside `AssetSetupActivity.thread { ... }`. Progress callbacks are invoked from that thread; UI marshaling is the caller's responsibility (existing `runOnUiThread { ... }` pattern).
- **No Android resources or assets pulled by `core`.** The bundled-maps copy in `copyBundledMaps()` stays as-is; the new maps land in the same target directory.

---

## Out of scope

- Changes to the H3sprite.lod picker (`filePickerLauncher`) — untouched by this work.
- Parsing campaign progression / travel options semantically (only the bytes are skipped, not interpreted).
- Repacking maps back into an `.h3c`.
- WoG-specific edge cases beyond reusing the SoD layout (not in sample corpus).
- Showing the user a list of campaigns to selectively extract — all maps go into `user-maps/`.
- Cleanup of previously installed maps when the user re-runs the installer.
