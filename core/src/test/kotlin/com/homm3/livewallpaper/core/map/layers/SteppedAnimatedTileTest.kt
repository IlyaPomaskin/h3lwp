package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.homm3.livewallpaper.core.render.AnimationClock
import org.junit.Test
import kotlin.test.assertSame

class SteppedAnimatedTileTest {
    private val frames = List(3) { TextureRegion() }
    private val tile = SteppedAnimatedTile(frames)

    @Test
    fun `shows the next frame on every advanced step and wraps around`() {
        AnimationClock.beginFrame()
        val startIndex = frames.indexOf(tile.textureRegion)

        repeat(frames.size + 1) { stepsAhead ->
            assertSame(frames[(startIndex + stepsAhead) % frames.size], tile.textureRegion)
            AnimationClock.advance()
            AnimationClock.beginFrame()
        }
    }

    @Test
    fun `keeps the frame until the clock advances`() {
        AnimationClock.beginFrame()
        val shown = tile.textureRegion

        AnimationClock.beginFrame()

        assertSame(shown, tile.textureRegion)
    }
}
