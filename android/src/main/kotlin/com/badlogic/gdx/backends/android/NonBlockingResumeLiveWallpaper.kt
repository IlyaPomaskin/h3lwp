// Lives in libGDX's package to reach AndroidGraphics' package-private resume state.
package com.badlogic.gdx.backends.android

import com.badlogic.gdx.Gdx

/**
 * [AndroidGraphicsLiveWallpaper.resume] blocks the main thread, without a timeout, until the GL thread starts a frame.
 * When the surface isn't drawable on screen-on, that frame needs surface callbacks the blocked main thread can't
 * deliver, so the wallpaper deadlocks into an ANR. This flags the resume and returns: the GL thread runs it at the
 * start of its next frame, before the runnables the wallpaper engine posts after resuming.
 */
class NonBlockingResumeLiveWallpaper(service: AndroidLiveWallpaperService) : AndroidLiveWallpaper(service) {
    override fun onResume() {
        Gdx.app = this
        Gdx.input = input
        Gdx.audio = audio
        Gdx.files = files
        Gdx.graphics = graphics
        Gdx.net = net

        input.onResume()
        graphics?.onResumeGLSurfaceView()

        if (firstResume) {
            firstResume = false
            return
        }
        audio.resume()
        resumeGraphics()
    }

    private fun resumeGraphics() {
        synchronized(graphics.synch) {
            graphics.running = true
            graphics.resume = true
        }
        graphics.requestRendering()
    }
}
