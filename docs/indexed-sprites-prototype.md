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

Old ETC1 classes stay untouched (unused) in the prototype.

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
