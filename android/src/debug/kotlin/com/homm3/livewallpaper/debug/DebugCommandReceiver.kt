package com.homm3.livewallpaper.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.badlogic.gdx.Gdx
import com.homm3.livewallpaper.android.appContainer
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.wallpaper.AndroidEngine
import com.homm3.livewallpaper.android.wallpaper.RunningEngine
import com.homm3.livewallpaper.core.EngineState
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.input.ZoomAction
import com.homm3.livewallpaper.core.library.Direction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/** Debug-build-only remote control for the running wallpaper; see docs/debug-commands.md. */
class DebugCommandReceiver : BroadcastReceiver() {

    private enum class Command { PING, MAP_NEXT, MAP_RANDOM_VIEW, MAP_TOGGLE_UNDERGROUND, CAMERA_PAN, CAMERA_PAN_SCREEN, CAMERA_ZOOM, PREFS_SET, STATE_GET, ENGINE_REFRESH }

    private class Reply(val code: Int, val data: JSONObject)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val repository = context.appContainer.preferencesRepository
        receiverScope.launch {
            val reply = replyTo(intent, repository)
            pendingResult.resultCode = reply.code
            pendingResult.resultData = reply.data.toString()
            pendingResult.finish()
        }
    }

    private suspend fun replyTo(intent: Intent, repository: WallpaperPreferencesRepository): Reply = try {
        Reply(RESULT_SUCCESS, withTimeout(TIMEOUT_MS) { execute(intent, repository) })
    } catch (exception: TimeoutCancellationException) {
        failure(TIMEOUT_ERROR)
    } catch (exception: Exception) {
        failure(exception.message ?: exception.javaClass.simpleName)
    }

    private fun failure(message: String) = Reply(RESULT_FAILURE, JSONObject().put(KEY_ERROR, message))

    private suspend fun execute(intent: Intent, repository: WallpaperPreferencesRepository): JSONObject {
        val command = Command.entries.firstOrNull { ACTION_PREFIX + it.name == intent.action }
        requireNotNull(command) { "unknown action: ${intent.action}" }
        return when (command) {
            Command.PING -> JSONObject().put(KEY_PONG, true)
            Command.MAP_NEXT -> {
                val direction = parseDirection(intent)
                onRenderThread { loadNextMap(direction) }
            }
            Command.MAP_RANDOM_VIEW -> onRenderThread { randomizeView() }
            Command.MAP_TOGGLE_UNDERGROUND -> onRenderThread { toggleUnderground() }
            Command.CAMERA_PAN -> {
                val dx = intent.typedExtra<Int>(EXTRA_DX, INT_FLAG) ?: 0
                val dy = intent.typedExtra<Int>(EXTRA_DY, INT_FLAG) ?: 0
                onRenderThread { panCameraByTiles(dx, dy) }
            }
            Command.CAMERA_PAN_SCREEN -> {
                val fractionX = intent.typedExtra<Float>(EXTRA_FX, FLOAT_FLAG) ?: 0f
                val fractionY = intent.typedExtra<Float>(EXTRA_FY, FLOAT_FLAG) ?: 0f
                require(fractionX.isFinite() && fractionY.isFinite()) { "fx and fy must be finite" }
                onRenderThread { panCameraByScreenFraction(fractionX, fractionY) }
            }
            Command.CAMERA_ZOOM -> {
                val action = parseZoomAction(intent)
                onRenderThread { zoomCamera(action) }
            }
            Command.PREFS_SET -> setPrefs(intent, repository)
            Command.STATE_GET -> {
                val state = runOnRenderThread { snapshotState() }
                stateJson(state, repository.preferences.first())
            }
            Command.ENGINE_REFRESH -> onRenderThread { requestRefresh() }
        }
    }

    private suspend fun onRenderThread(action: AndroidEngine.() -> Unit): JSONObject {
        runOnRenderThread(action)
        return JSONObject()
    }

    private suspend fun <T> runOnRenderThread(block: AndroidEngine.() -> T): T {
        val engine = checkNotNull(RunningEngine.current) { ENGINE_NOT_RUNNING_ERROR }
        val result = CompletableDeferred<T>()
        Gdx.app.postRunnable {
            result.completeWith(runCatching { engine.block() })
            Gdx.graphics.requestRendering()
        }
        return result.await()
    }

    private fun parseDirection(intent: Intent): Direction {
        val name = intent.typedExtra<String>(EXTRA_DIRECTION, STRING_FLAG) ?: return Direction.FORWARD
        val direction = Direction.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        return requireNotNull(direction) { "$EXTRA_DIRECTION must be forward|backward, got '$name'" }
    }

    private fun parseZoomAction(intent: Intent): ZoomAction {
        val name = requireNotNull(intent.typedExtra<String>(EXTRA_ACTION, STRING_FLAG)) {
            "$EXTRA_ACTION is required: in|out|reset"
        }
        val action = ZoomAction.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        return requireNotNull(action) { "$EXTRA_ACTION must be in|out|reset, got '$name'" }
    }

    private suspend fun setPrefs(intent: Intent, repository: WallpaperPreferencesRepository): JSONObject {
        val scale = intent.typedExtra<Int>(EXTRA_SCALE, INT_FLAG)?.let(::parseScale)
        val mapUpdateInterval = intent.typedExtra<String>(EXTRA_MAP_UPDATE_INTERVAL, STRING_FLAG)?.let(::parseMapUpdateInterval)
        val useScroll = intent.typedExtra<Boolean>(EXTRA_USE_SCROLL, BOOLEAN_FLAG)
        val brightness = intent.typedExtra<Float>(EXTRA_BRIGHTNESS, FLOAT_FLAG)
        brightness?.let { require(it.isFinite()) { "$EXTRA_BRIGHTNESS must be finite" } }
        val hasAnyPref = listOf(scale, mapUpdateInterval, useScroll, brightness).any { it != null }
        require(hasAnyPref) { "PREFS_SET needs at least one of: ${PREF_EXTRAS.joinToString()}" }

        scale?.let { repository.setScale(it) }
        mapUpdateInterval?.let { repository.setMapUpdateInterval(it) }
        useScroll?.let { repository.setUseScroll(it) }
        brightness?.let { repository.setBrightness(it) }
        return prefsJson(repository.preferences.first())
    }

    private fun parseScale(value: Int): Scale {
        val scale = Scale.entries.firstOrNull { it.value == value }
        return requireNotNull(scale) { "$EXTRA_SCALE must be ${Scale.entries.first().value}..${Scale.entries.last().value}, got $value" }
    }

    private fun parseMapUpdateInterval(name: String): MapUpdateInterval {
        val interval = MapUpdateInterval.entries.firstOrNull { it.name == name }
        return requireNotNull(interval) {
            "$EXTRA_MAP_UPDATE_INTERVAL must be one of ${MapUpdateInterval.entries.joinToString("|")}, got '$name'"
        }
    }

    private fun stateJson(state: EngineState, prefs: WallpaperPreferences) = JSONObject()
        .put(KEY_SCREEN, state.screen ?: JSONObject.NULL)
        .put(KEY_MAP, state.mapName ?: JSONObject.NULL)
        .put(KEY_UNDERGROUND, state.isUnderground)
        .put(KEY_ZOOM, state.zoom as Any)
        .put(KEY_CAMERA, JSONObject().put(KEY_X, state.cameraX as Any).put(KEY_Y, state.cameraY as Any))
        .put(KEY_PREFS, prefsJson(prefs))

    private fun prefsJson(prefs: WallpaperPreferences) = JSONObject()
        .put(EXTRA_SCALE, prefs.scale.value)
        .put(EXTRA_MAP_UPDATE_INTERVAL, prefs.mapUpdateInterval.name)
        .put(EXTRA_USE_SCROLL, prefs.useScroll)
        .put(EXTRA_BRIGHTNESS, prefs.brightness as Any)

    @Suppress("DEPRECATION")
    private inline fun <reified T> Intent.typedExtra(key: String, flag: String): T? {
        if (!hasExtra(key)) return null
        val value = extras?.get(key)
        require(value is T) { "$key must be passed with $flag" }
        return value
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val ACTION_PREFIX = "com.homm3.livewallpaper.debug."
        private const val TIMEOUT_MS = 3000L
        private const val RESULT_SUCCESS = 0
        private const val RESULT_FAILURE = 1
        private const val TIMEOUT_ERROR = "timeout"
        private const val ENGINE_NOT_RUNNING_ERROR = "engine not running"

        private const val INT_FLAG = "--ei"
        private const val FLOAT_FLAG = "--ef"
        private const val BOOLEAN_FLAG = "--ez"
        private const val STRING_FLAG = "--es"

        private const val EXTRA_DIRECTION = "direction"
        private const val EXTRA_DX = "dx"
        private const val EXTRA_DY = "dy"
        private const val EXTRA_FX = "fx"
        private const val EXTRA_FY = "fy"
        private const val EXTRA_ACTION = "action"
        private const val EXTRA_SCALE = "scale"
        private const val EXTRA_MAP_UPDATE_INTERVAL = "mapUpdateInterval"
        private const val EXTRA_USE_SCROLL = "useScroll"
        private const val EXTRA_BRIGHTNESS = "brightness"
        private val PREF_EXTRAS = listOf(EXTRA_SCALE, EXTRA_MAP_UPDATE_INTERVAL, EXTRA_USE_SCROLL, EXTRA_BRIGHTNESS)

        private const val KEY_ERROR = "error"
        private const val KEY_PONG = "pong"
        private const val KEY_SCREEN = "screen"
        private const val KEY_MAP = "map"
        private const val KEY_UNDERGROUND = "underground"
        private const val KEY_ZOOM = "zoom"
        private const val KEY_CAMERA = "camera"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_PREFS = "prefs"
    }
}
