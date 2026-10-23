package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.I18NBundle
import com.homm3.livewallpaper.core.AssetPaths

/** Skin and strings for the setup and loading screens; create it on the GL thread. */
class UiAssets : Disposable {
    val skin = Skin(Gdx.files.internal(AssetPaths.SKIN_PATH))
    val i18n: I18NBundle = run {
        I18NBundle.setExceptionOnMissingKey(false)
        I18NBundle.createBundle(Gdx.files.internal(AssetPaths.I18N_PATH))
    }

    override fun dispose() {
        skin.dispose()
    }
}
