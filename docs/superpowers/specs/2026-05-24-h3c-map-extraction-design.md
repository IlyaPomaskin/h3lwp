# H3C Campaign Map Extraction — Design

**Goal:** During asset setup, automatically extract every `.h3m` map from every `.h3c` campaign found in the user-provided archives, and drop the maps into the live wallpaper's `user-maps/` directory so they appear in the rotation alongside bundled maps.

**Trigger points in the existing setup flow:**

1. **H3sprite.lod picker** (`AssetSetupActivity.filePickerLauncher`) — after `LodValidator.validate(...)` passes, scan the LOD for `.h3c` entries and extract their maps. (Standard H3sprite.lod has none, but the picker accepts arbitrary classic LODs, so a campaign-bearing LOD like the original `H3ab.lod` would yield maps here.)
2. **HotA installer picker** (`AssetSetupActivity.hotaFilePickerLauncher`) — after `InnoSetupExtractor` finishes its `HotA.lod` extraction, in the same installer pass also:
   - extract `HotA_lng.lod` (transient — scanned for `.h3c` then deleted),
   - extract **every `app/Maps/*.h3m`** entry from the installer payload directly into `user-maps/` (the HotA 1.8.0 installer contains 128 such files — single-scenario maps that ship with HotA).
3. **HotA_lng.lod scan** — list `.h3c` entries, extract each, and run `H3cExtractor` on each. `HotA_lng.lod` carries 11 `.h3c` campaign files (confirmed by listing its contents with `tools/find_h3_maps.py`).

**Scope:** All campaign versions observed across those 11 campaigns — RoE, AB, SoD, Chronicles (`Chr`), HotA. WoG is in the version enum for completeness but is not represented in the corpus.

---

## H3C Format

An `.h3c` file is **N + 1 concatenated gzip streams**:

| Stream | Content |
|---|---|
| `0` | Campaign metadata: campaign header followed by N scenario descriptors |
| `1..N` | One gzipped `.h3m` map per scenario, in scenario order |

Verified empirically against all 11 sample campaigns. The first u32 of each map stream (after ungzip) is always a valid `H3mVersion` value (`0x0e`/`0x15`/`0x1c`/`0x20`).

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

Two new packages, both inside `core`:

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
    fun extract(input: File): List<ExtractedMap>
}
```

`extract` takes an in-memory `ByteArray` because the input typically comes from a LOD entry that's already been decompressed into memory. The `File` overload is for direct file input (e.g., a saved `.h3c` on disk).

### `core/src/main/kotlin/com/homm3/livewallpaper/core/assets/CampaignMapInstaller.kt`

Glue between `LodReader`, `H3cExtractor`, and the filesystem. This is where the wallpaper-specific policy lives (output directory, overwrite behavior, progress reporting).

```kotlin
object CampaignMapInstaller {
    data class Result(
        val campaignsFound: Int,
        val mapsWritten: Int,
        val skipped: List<String>,   // h3c names that failed to parse
    )

    /** Scans the given LOD for .h3c entries, extracts each, and writes every map
     *  to [outputDir]. Existing files with the same name are left untouched. */
    fun installFromLod(
        lod: File,
        outputDir: File,
        onProgress: ((current: Int, total: Int, message: String) -> Unit)? = null,
    ): Result
}
```

### `InnoSetupExtractor` change

Generalize the existing single-suffix extraction so callers can both **pick specific files** (e.g. `data\hota.lod`, `data\HotA_lng.lod`) and **glob a directory** (e.g. every `app\Maps\*.h3m`) in one pass over the installer. Picking by predicate rather than exact suffix lets the 128-h3m extraction reuse the same scan as the two LODs.

```kotlin
object InnoSetupExtractor {
    /** Caller-supplied target. The matcher runs against the entry's `destination`
     *  (forward/backslashes preserved as authored). `outputFor` resolves to the
     *  destination file given the matched destination string (so a glob target
     *  can map each match to a unique output path). */
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

The HotA installer picker constructs three targets in one call:
- `data\hota.lod` → `filesDir/HotA.lod`
- `data\HotA_lng.lod` → `cacheDir/HotA_lng.lod` (transient)
- Glob `app\Maps\*.h3m` (case-insensitive) → `userMapsDir/<basename>` (skip when target file exists; sanitize per filename rules below)

Internally `FileEntries.findByDestinationSuffix` becomes `FileEntries.collectMatching` returning `List<FileEntry>` (preserving the "must finish scanning to leave the stream positioned at data entries" behavior). The new function takes a `(String) -> Boolean` matcher plus a cap (`Int.MAX_VALUE` by default) so a future caller can short-circuit when collecting a single named file.

---

## Pipeline wiring (AssetSetupActivity)

```
H3sprite.lod picker:
  copy LOD to filesDir
  LodValidator.validate          ──── if error: delete + bail ────┐
  CampaignMapInstaller.installFromLod(lod, userMapsDir)            │
  copyBundledMaps()                                                ▼
  startActivity(SettingsActivity)                              fail UI

HotA installer picker:
  copy installer to cacheDir
  InnoSetupExtractor.extract(installer, [
      HotA.lod        → filesDir/HotA.lod,
      HotA_lng.lod    → cacheDir/HotA_lng.lod  (transient),
      app/Maps/*.h3m  → userMapsDir/<basename> (skip on collision)
  ])
  LodValidator.validate(HotA.lod)  ──── if error: delete + bail ──┐
  CampaignMapInstaller.installFromLod(HotA_lng.lod, userMapsDir)   │
  delete HotA_lng.lod                                              ▼
                                                                fail UI
```

`HotA_lng.lod` is a transient artifact: we extract it from the installer only to mine its `.h3c` entries, then delete it. It doesn't need to live in `filesDir` long-term.

`app/Maps/*.h3m` entries are written **straight to `user-maps/`** during the installer pass — they're already standalone playable maps and don't need to go through `H3cExtractor`. They share the same skip-on-collision + filename sanitization rules as campaign-derived maps.

Progress callbacks bubble up via the existing `hotaStatusMessage` / `statusMessage` state.

### Data flow inside `CampaignMapInstaller`

```
.lod file
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

- **LOD with no `.h3c` entries** → `Result(campaignsFound = 0, ...)`. Not an error; setup proceeds.
- **`H3cExtractor` throws on one entry** → catch, append entry name to `skipped`, continue with next entry. One bad campaign doesn't fail the whole install.
- **`HotA_lng.lod` not found in installer** → log + skip the lng pass. We still extract `HotA.lod` (the critical one), so setup succeeds.
- **Filename collision in `user-maps/`** → existing file wins (no overwrite). Rationale: user may have edited bundled maps; we don't want to clobber.
- **Not a gzip stream at offset 0 of an h3c** → `H3cExtractor` throws `IllegalArgumentException("Not an h3c file")`; caller catches and records in `skipped`.
- **Stream 0 parse fails partway** → fall back to numeric names (`<campaignFileStem>_0.h3m`...). All map streams still extracted.

### Filename sanitization

`mapName` can contain characters illegal on some filesystems (`:`, `/`, `\`, `?`, etc.). Replace each with `_`. Append `.h3m` if not present. Collisions inside one campaign resolved by appending `_<index>`.

---

## Testing

**Fixture policy:** No new binary fixtures land in the repo. Tests use only the fixtures the project already accesses via `TestFixtures` (`installer180`, `goldenLod180`) plus a new `lng180` accessor that points at the HotA 1.8.0 language LOD. All such accessors gate with `@Assume`, so tests skip cleanly when the corpus is absent. Everything else is built in-process (synthetic gzip concatenation).

`core/src/test/kotlin/.../h3c/`:

- **`GzipStreamSplitterTest`** — pure synthetic: build 3 concatenated gzip streams from in-memory bytes and assert offsets, lengths, and decompressed content match. No file fixtures.
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

This pipeline runs on Android (the live wallpaper's home), so the design honors mobile constraints:

- **No Android-specific types in `core`.** Public APIs in the `parser.h3c`, `parser.inno`, and `core.assets` packages take only `java.io.File`, `InputStream`/`ByteArray`, and Kotlin/JDK types. Android-side code (`AssetSetupActivity`) supplies paths rooted under `filesDir` and `cacheDir`; the parsers don't know about app context.
- **Memory ceiling.** Largest single in-memory buffer is one LOD entry being decompressed (`LodReader.readFileContent` returns `ByteArrayInputStream`) — the biggest h3c in `HotA_lng.lod` is ~770 KB compressed / ~2 MB decompressed, well under a single Android allocation budget. The installer's `app/Maps/*.h3m` glob streams each file straight to disk via the existing `ChunkDecompressor.extract(...)`, never fully buffered. `H3cExtractor` holds one campaign at a time (one decompressed h3c byte array + the list of stream slices into it).
- **Disk paths from the host.** All output `File`s in the public API are caller-supplied. The `CampaignMapInstaller` writes to whatever directory `AssetSetupActivity` passes (typically `filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)`); it never resolves paths itself.
- **Filename sanitization is filesystem-conservative.** Strip the same character set on every platform (`:`, `/`, `\`, `?`, `*`, `<`, `>`, `|`, `"`, control chars) so the result is valid on Android internal storage (ext4-backed), tmpfs, and JVM-host filesystems alike.
- **Background thread compatibility.** The pipeline already runs on a worker thread inside `AssetSetupActivity.thread { ... }`. Progress callbacks must be safe to invoke from that thread; UI marshaling is the caller's responsibility (existing `runOnUiThread { ... }` pattern).
- **No Android resources or assets pulled by `core`.** The bundled-maps copy in `copyBundledMaps()` stays where it is (Android `assets`); the campaign-derived maps land in the same target directory.

---

## Out of scope

- Parsing campaign progression / travel options semantically (only the bytes are skipped, not interpreted).
- Repacking maps back into an `.h3c`.
- WoG-specific edge cases beyond reusing the SoD layout (not in sample corpus).
- Showing the user a list of campaigns to selectively extract — all maps go into `user-maps/`.
- Cleanup of previously installed maps when the user re-runs setup with a different LOD.
