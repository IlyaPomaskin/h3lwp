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

    fun getMapsForToday(availableFiles: List<String>): List<String> {
        if (availableFiles.isEmpty()) return emptyList()

        val fingerprint = computeFingerprint(availableFiles)
        val today = dateFormat.format(Date())
        val state = loadState()

        if (state == null || state.fingerprint != fingerprint) {
            val shuffled = availableFiles.shuffled()
            val batch = selectBatch(shuffled, 0)
            saveState(QueueState(today, 0, fingerprint, shuffled))
            log.info { "Map queue rebuilt. Queue order (${shuffled.size}): $shuffled" }
            log.info { "Loading ${batch.size} maps at position 0: $batch" }
            return batch
        }

        if (state.date == today) {
            val batch = selectBatch(state.queue, state.position)
            log.info { "Same day. Queue (${state.queue.size}) at position ${state.position}: ${state.queue}" }
            log.info { "Loading ${batch.size} maps: $batch" }
            return batch
        }

        val previousBatch = selectBatch(state.queue, state.position)
        val newPosition = (state.position + previousBatch.size) % state.queue.size
        val batch = selectBatch(state.queue, newPosition)
        saveState(QueueState(today, newPosition, fingerprint, state.queue))
        log.info { "New day. Queue (${state.queue.size}) at position $newPosition: ${state.queue}" }
        log.info { "Loading ${batch.size} maps: $batch" }
        return batch
    }

    private fun selectBatch(queue: List<String>, position: Int): List<String> {
        if (queue.isEmpty()) return emptyList()
        val pos = position % queue.size
        val take = minOf(BATCH_SIZE, queue.size)
        return List(take) { i -> queue[(pos + i) % queue.size] }
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
        private const val BATCH_SIZE = 3
        private const val PREFS_NAME = "map-queue"
        private const val PREFS_KEY = "state"
    }
}
