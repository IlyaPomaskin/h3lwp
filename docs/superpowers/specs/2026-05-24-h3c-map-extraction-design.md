# H3C Campaign Map Extraction — Design

**Goal:** Add a parser that extracts individual `.h3m` maps from a Heroes 3 `.h3c` campaign file. Output is gzipped `.h3m` files compatible with the existing `H3mReader` and with HoMM3 itself.

**Scope:** All campaign versions present in `data/h3c/` — RoE, AB, SoD, Chronicles (`Chr`), HotA. WoG is in the version enum for completeness but is not represented in the sample corpus.

---

## Format

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

New package `core/src/main/kotlin/com/homm3/livewallpaper/parser/h3c/`. Same conventions as `parser.inno` and `parser.lod`: one public symbol, the rest `internal`.

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
    fun extract(input: File): List<ExtractedMap>

    /** Writes each extracted map to `outputDir`, returning the resulting files in order.
     *  If `outputDir` is null, defaults to `assets/user-maps/` relative to the project root. */
    fun extractToDirectory(input: File, outputDir: File? = null): List<File>
}
```

### Data flow

```
.h3c file
   │
   ▼
GzipStreamSplitter ── yields ──► [Stream 0, Stream 1, ..., Stream N]
   │                                  │            └──────┬──────┘
   │                                  ▼                   ▼
   │                       CampaignHeaderReader    (raw bytes — already gzipped h3m)
   │                                  │                   │
   │                                  ▼                   │
   │                          List<String> names ─────────┤
   │                                                       ▼
   └─────────────────────────────────────────────► List<ExtractedMap>
```

### Error handling

- **Not a gzip stream at offset 0** → throw `IllegalArgumentException("Not an h3c file")`.
- **Stream 0 parse fails partway** → log a warning, fall back to numeric names (`map_0.h3m`...`map_N.h3m`). All map streams still extracted.
- **Scenario count from region table doesn't match stream count** → trust stream count (we have ground truth from gzip boundaries), log mismatch.
- **A stream 1..N decompresses to bytes whose first u32 isn't a known `H3mVersion`** → still write it, log a warning. (Keeps extraction lossless even if the embedded map uses a version we don't model yet.)

### Filename sanitization

`mapName` can contain characters illegal on some filesystems (`:`, `/`, `\`, `?`, etc.). Replace each with `_`. Append `.h3m` if not present. Collisions resolved by appending `_<index>`.

---

## Testing

`core/src/test/kotlin/.../h3c/`:

- **`GzipStreamSplitterTest`** — synthetic input of 3 concatenated gzip streams; assert offsets, lengths, and decompressed content match.
- **`CampaignHeaderReaderTest`** — feed stream 0 of `ab.h3c` (known: 8 scenarios, expected scenario names from the campaign). Assert returned name list matches.
- **`H3cExtractorTest`** — for each `data/h3c/*.h3c`:
  - Extract; assert the count of extracted maps matches a hard-coded table.
  - Each `ExtractedMap.bytes` decompresses to a stream whose first u32 is in `{0x0e, 0x15, 0x1c, 0x20}`.
  - Each `bytes` parses end-to-end via `H3mReader(bytes.inputStream()).read()` without throwing.

The sample corpus (`data/h3c/*.h3c`) is the golden test set. No new fixtures needed.

---

## Out of scope

- Parsing campaign progression / travel options semantically (only the bytes are skipped, not interpreted).
- UI integration — this is a pure parser, mirroring `InnoSetupExtractor`. Wiring into the wallpaper picker is a separate task.
- Repacking maps back into an `.h3c`.
- WoG-specific edge cases beyond reusing the SoD layout (not in sample corpus).
