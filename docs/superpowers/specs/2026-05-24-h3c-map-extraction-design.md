# H3C Campaign Map Extraction — Design

**Goal:** During asset setup, automatically extract every `.h3m` map from every `.h3c` campaign found in the user-provided archives, and drop the maps into the live wallpaper's `user-maps/` directory so they appear in the rotation alongside bundled maps.

**Trigger points in the existing setup flow:**

1. **H3sprite.lod picker** (`AssetSetupActivity.filePickerLauncher`) — after `LodValidator.validate(...)` passes, scan the LOD for `.h3c` entries and extract their maps. (Standard H3sprite.lod has none, but the picker accepts arbitrary classic LODs, so a campaign-bearing LOD like the original `H3ab.lod` would yield maps here.)
2. **HotA installer picker** (`AssetSetupActivity.hotaFilePickerLauncher`) — after `InnoSetupExtractor` finishes, also extract `HotA_lng.lod` from the same installer (it sits next to `data\HotA.lod` in the installer payload), then scan it for `.h3c` entries.
3. **HotA_lng.lod itself** — same scan as step 1/2: list `.h3c` entries, extract each, and run `H3cExtractor` on each.

The 11 h3c files we have in `data/h3c/` are all sourced from `HotA_lng.lod` (confirmed via `tools/find_h3_maps.py data/HotA_lng.lod`).

**Scope:** All campaign versions present in `data/h3c/` — RoE, AB, SoD, Chronicles (`Chr`), HotA. WoG is in the version enum for completeness but is not represented in the sample corpus.

---

## H3C Format

An `.h3c` file is **N + 1 concatenated gzip streams**:

| Stream | Content |
|---|---|
| `0` | Campaign metadata: campaign header followed by N scenario descriptors |
| `1..N` | One gzipped `.h3m` map per scenario, in scenario order |

Verified empirically against all 11 sample files in `data/h3c/`. The first u32 of each map stream (after ungzip) is always a valid `H3mVersion` value (`0x0e`/`0x15`/`0x1c`/`0x20`).

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

`extract` takes an in-memory `ByteArray` because the input often comes from a LOD entry that's already been decompressed into memory. The `File` overload is for direct file input (e.g., the `data/h3c/*.h3c` test corpus).

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

Generalize the existing single-target extraction into a multi-target call. The current public API stays as a thin wrapper for back-compat; a new variant extracts multiple destinations in one pass over the installer:

```kotlin
object InnoSetupExtractor {
    data class Target(val destinationSuffix: String, val output: File)

    fun extract(installer: File, targets: List<Target>, onProgress: ...): List<File>

    // Existing call delegated to extract(...) with a single Target.
    fun extractHotaLod(installer: File, output: File, onProgress: ...)
}
```

The HotA installer picker then requests two targets in one call: `data\hota.lod` and `data\HotA_lng.lod`.

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
  InnoSetupExtractor.extract(installer, [HotA.lod target, HotA_lng.lod target])
  LodValidator.validate(HotA.lod)  ──── if error: delete + bail ──┐
  CampaignMapInstaller.installFromLod(HotA_lng.lod, userMapsDir)   │
  delete HotA_lng.lod (not needed at runtime, only for h3c)        ▼
                                                                fail UI
```

`HotA_lng.lod` is a transient artifact: we extract it from the installer only to mine its `.h3c` entries, then delete it. It doesn't need to live in `filesDir` long-term.

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

`core/src/test/kotlin/.../h3c/`:

- **`GzipStreamSplitterTest`** — synthetic input of 3 concatenated gzip streams; assert offsets, lengths, and decompressed content match.
- **`CampaignHeaderReaderTest`** — feed stream 0 of `ab.h3c` (known: 8 scenarios, expected scenario names from the campaign). Assert returned name list matches.
- **`H3cExtractorTest`** — for each `data/h3c/*.h3c`:
  - Extract; assert the count of extracted maps matches a hard-coded table.
  - Each `ExtractedMap.bytes` decompresses to a stream whose first u32 is in `{0x0e, 0x15, 0x1c, 0x20}`.
  - Each `bytes` parses end-to-end via `H3mReader(bytes.inputStream()).read()` without throwing.

`core/src/test/kotlin/.../assets/`:

- **`CampaignMapInstallerTest`** — point at `data/HotA_lng.lod` (gated with `@Assume` if the file is missing for CI builds without the corpus), run `installFromLod` to a temp dir, assert:
  - `Result.campaignsFound == 11`
  - `Result.mapsWritten == <expected sum across the 11 campaigns>` (hard-coded table)
  - Every output file parses through `H3mReader`
  - Re-running the installer is a no-op (idempotent: no overwrite, same `mapsWritten` count is now 0)

`core/src/test/kotlin/.../inno/`:

- **`InnoSetupExtractorMultiTargetTest`** — extend the existing 1.8.0 golden test to also request `HotA_lng.lod` in the same call; assert both outputs match goldens (or that `HotA_lng.lod` is non-empty and contains the expected 11 h3c entry names via `LodReader`).

The sample corpus (`data/h3c/*.h3c` and `data/HotA_lng.lod` and `data/HotA_1.8.0_setup.exe`) is the golden test set. No new binary fixtures needed.

---

## Out of scope

- Parsing campaign progression / travel options semantically (only the bytes are skipped, not interpreted).
- Repacking maps back into an `.h3c`.
- WoG-specific edge cases beyond reusing the SoD layout (not in sample corpus).
- Showing the user a list of campaigns to selectively extract — all maps go into `user-maps/`.
- Cleanup of previously installed maps when the user re-runs setup with a different LOD.
