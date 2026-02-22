package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.log.logger

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapQueue {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getMapsForToday(
        availableFiles: List<String>,
        mapSizeResolver: (String) -> Int
    ): List<String> {
        if (availableFiles.isEmpty()) return emptyList()

        val fingerprint = computeFingerprint(availableFiles)
        val today = dateFormat.format(Date())
        val state = loadState()

        if (state == null || state.fingerprint != fingerprint) {
            val shuffled = availableFiles.shuffled()
            val batch = selectBatch(shuffled, 0, mapSizeResolver)
            saveState(QueueState(today, 0, fingerprint, shuffled))
            log.info { "Map queue rebuilt. Loading ${batch.size} maps: $batch" }
            return batch
        }

        if (state.date == today) {
            val batch = selectBatch(state.queue, state.position, mapSizeResolver)
            log.info { "Same day. Loading ${batch.size} maps: $batch" }
            return batch
        }

        val previousBatch = selectBatch(state.queue, state.position, mapSizeResolver)
        val newPosition = (state.position + previousBatch.size) % state.queue.size
        val batch = selectBatch(state.queue, newPosition, mapSizeResolver)
        saveState(QueueState(today, newPosition, fingerprint, state.queue))
        log.info { "New day. Loading ${batch.size} maps: $batch" }
        return batch
    }

    private fun selectBatch(
        queue: List<String>,
        position: Int,
        mapSizeResolver: (String) -> Int
    ): List<String> {
        if (queue.isEmpty()) return emptyList()

        val pos = position % queue.size
        val firstFile = queue[pos]
        val firstSize = mapSizeResolver(firstFile)

        if (firstSize >= LARGE_MAP_THRESHOLD) {
            return listOf(firstFile)
        }

        val batch = mutableListOf(firstFile)
        for (i in 1 until MAX_SMALL_MAPS) {
            val nextPos = (pos + i) % queue.size
            if (nextPos == pos) break
            val nextFile = queue[nextPos]
            val nextSize = mapSizeResolver(nextFile)
            if (nextSize >= LARGE_MAP_THRESHOLD) break
            batch.add(nextFile)
        }
        return batch
    }

    private fun computeFingerprint(files: List<String>): String {
        return files.sorted().joinToString(",").hashCode().toString(16)
    }

    private fun loadState(): QueueState? {
        return try {
            val prefs = Gdx.app.getPreferences(PREFS_NAME)
            val json = prefs.getString(PREFS_KEY, null) ?: return null
            val root = JsonReader().parse(json)

            val date = root.getString("date", null) ?: return null
            val position = root.getInt("position", -1)
            if (position < 0) return null
            val fingerprint = root.getString("fingerprint", null) ?: return null

            val queueArray = root.get("queue") ?: return null
            val queue = mutableListOf<String>()
            var entry: JsonValue? = queueArray.child
            while (entry != null) {
                queue.add(entry.asString())
                entry = entry.next
            }

            if (queue.isEmpty()) return null
            QueueState(date, position, fingerprint, queue)
        } catch (e: Exception) {
            log.error { "Failed to load map queue state: ${e.message}" }
            null
        }
    }

    private fun saveState(state: QueueState) {
        try {
            val writer = Json()
            writer.setOutputType(JsonWriter.OutputType.json)
            val json = writer.toJson(mapOf(
                "date" to state.date,
                "position" to state.position,
                "fingerprint" to state.fingerprint,
                "queue" to state.queue
            ))

            val prefs = Gdx.app.getPreferences(PREFS_NAME)
            prefs.putString(PREFS_KEY, json)
            prefs.flush()
        } catch (e: Exception) {
            log.error { "Failed to save map queue state: ${e.message}" }
        }
    }

    private data class QueueState(
        val date: String,
        val position: Int,
        val fingerprint: String,
        val queue: List<String>
    )

    companion object {
        private val log = logger<MapQueue>()
        private const val LARGE_MAP_THRESHOLD = 144
        private const val MAX_SMALL_MAPS = 3
        private const val PREFS_NAME = "map-queue"
        private const val PREFS_KEY = "state"
    }
}
