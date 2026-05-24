package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import java.io.File

/** Variant of [Lwjgl3Application] whose `Gdx.files.local(...)` resolves under
 *  `<JVM_CWD>/assets-desktop/` instead of the JVM cwd itself. */
class DesktopLwjgl3Application(
    listener: ApplicationListener,
    config: Lwjgl3ApplicationConfiguration,
) : Lwjgl3Application(listener, config) {

    /** Called by the super constructor — keep it pure (no instance state),
     *  since subclass fields are not initialised yet at this point. */
    override fun createFiles(): Files = DesktopFiles(File("assets-desktop"))
}
