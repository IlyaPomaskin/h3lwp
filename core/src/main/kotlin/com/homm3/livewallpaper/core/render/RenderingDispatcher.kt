package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.CoroutineDispatcher
import ktx.async.MainDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Runs coroutines on the rendering thread through [MainDispatcher] (KtxAsync), but always posts (never runs
 * inline like the immediate [MainDispatcher] does on the rendering thread, so submitting work from a frame
 * can't re-enter it) and requests a frame for every dispatch.
 *
 * Hack: with non-continuous rendering, the Android live-wallpaper backend runs posted runnables only inside
 * the next frame, and its `postRunnable` doesn't request one, so a coroutine resuming from another thread
 * (IO, a flow emission, a channel send) would otherwise wait for an unrelated frame.
 */
object RenderingDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        MainDispatcher.dispatch(context, block)
        Gdx.graphics.requestRendering()
    }
}
