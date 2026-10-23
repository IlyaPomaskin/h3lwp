# Indexed-palette sprites prototype

Goal: drop RGBA conversion, ETC1 encoding and the atlas cache. Keep sprites as 8-bit palette
indices in GL_ALPHA textures; a fragment shader looks the colour up in a palette texture.

Shared types: `core/assets/indexed/IndexedModel.kt` (do not change signatures without the integrator).

## Files

| File | Owner | Status |
|---|---|---|
| `core/assets/indexed/IndexedModel.kt` | integrator | shared contract |
| `core/assets/indexed/IndexedSpriteLoader.kt` | agent A | new |
| `core/assets/indexed/PaletteAtlas.kt` | agent B | new |
| `core/assets/indexed/IndexedSpriteRegistry.kt` | agent B | new |
| `core/render/PaletteShader.kt` | agent C | new |
| `core/render/PaletteSpriteBatch.kt` | agent C | new |
| `core/render/PaletteTiledMapRenderer.kt` | agent C | new |
| `core/map/MapSprite.kt` | agent C | edit: encode palette row before draw |
| `core/assets/GameAssets.kt` | integrator | edit: use loader + registry, drop ETC1 cache |
| `core/screen/GameScreen.kt` | integrator | edit: use new batch/renderer, tick palette rotation |

The old ETC1 path (LodSpriteLoader, Hota18SpriteLoader, Etc1*, SpriteRegistry, D32Reader) was removed after integration; its palette rules live in `core/assets/indexed/DefPalette.kt`.

## Public API

```kotlin
// agent A — CPU only, no GL calls, thread-safe to call from Dispatchers.IO
class IndexedSpriteLoader {
    fun load(sources: List<LodSource>, neededNames: Set<String>): IndexedBundle
}

// agent B — GL thread
class PaletteAtlas : Disposable {
    val texture: Texture                                  // PALETTE_SIZE x MAX_PALETTE_ROWS, RGBA8888, Nearest
    fun add(palette: PaletteData): Int                    // returns row; identical static palettes share a row
    fun update(delta: Float)                              // advances rotating palettes every FRAME_TIME, re-uploads their rows
    override fun dispose()
}

class IndexedSpriteRegistry : Disposable {
    val paletteTexture: Texture
    fun add(bundle: IndexedBundle)                        // uploads pages + palettes, creates PaletteRegions, disposes bundle
    fun getTerrainFrames(defName: String, index: Int): Array<AtlasRegion>
    fun getObjectFrames(defName: String): Array<AtlasRegion>
    fun findRegions(name: String): Array<AtlasRegion>
    fun isDefLoaded(defName: String): Boolean
    fun update(delta: Float)                              // delegates to PaletteAtlas
    override fun dispose()
}

// agent C — GL thread
object PaletteShader { fun compile(): ShaderProgram }    // u_texture = unit 0 (index page), u_palette = unit 1
class PaletteSpriteBatch(size: Int, paletteTexture: () -> Texture?) : SpriteBatch // keeps palette bound on unit 1
class PaletteTiledMapRenderer(map: TiledMap, batch: PaletteSpriteBatch) : OrthogonalTiledMapRenderer
// MapSprite.render sets batch.packedColor = PaletteColor.encode(region.paletteRow) before drawing
```

## Results (desktop, map "Aeten Noemere 1.87", 1466 sprites)

| | ETC1 (main) | Indexed palette |
|---|---|---|
| Cold start, no cache | 12.9–16.6 s | 1.33–1.37 s |
| Warm start (ETC1 cache hit) | 1.3–2.0 s | n/a, no cache |
| Atlas pages | 11 (ETC1 colour+alpha, 4 MB each) | 11 (GL_ALPHA, 4 MB each) + 2 MB palette |
| Draw calls/frame, pinned view, 0–5 s → 15–20 s | 50 → 201 | 52 → 260 |
| Map render CPU | 0.16–0.59 ms | 0.21–0.77 ms |
| Palette cycling | 1932 baked copies | runtime palette rows |

Open issues:
- 1353 palettes for one map vs MAX_PALETTE_ROWS = 2048: a second map can overflow. Fix before adopting
  (several palettes per texture row, or 4096 rows where supported).
- Draw calls grow over time with a fixed view in both builds (animated frames spread over pages).
- Textures are unmanaged: GL context loss on Android needs a reload (same as the ETC1 path).
