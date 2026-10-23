package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.log.logger

class MapQueue {

    /** Current batch with no rotation. Initializes/rebuilds the queue if the fingerprint has changed (or if there's no saved state yet). */
    fun currentBatch(availableFiles: List<String>): List<String> {
        if (availableFiles.isEmpty()) return emptyList()

        val fingerprint = computeFingerprint(availableFiles)
        val state = loadState()

        if (state == null || state.fingerprint != fingerprint) {
            val shuffled = availableFiles.shuffled()
            val batch = selectBatch(shuffled, 0)
            saveState(QueueState(0, fingerprint, shuffled))
            log.info { "Map queue rebuilt. Queue order (${shuffled.size}): $shuffled" }
            log.info { "Loading ${batch.size} maps at position 0: $batch" }
            return batch
        }

        val batch = selectBatch(state.queue, state.position)
        log.info { "Queue (${state.queue.size}) at position ${state.position}: $batch" }
        return batch
    }

    /**
     * Advances `position` by the current batch size, persists it and returns the new batch; empty when there are
     * no files. Like [currentBatch], rebuilds the queue (starting at its first batch) if the fingerprint has changed.
     */
    fun advance(availableFiles: List<String>): List<String> {
        if (availableFiles.isEmpty()) return emptyList()

        val fingerprint = computeFingerprint(availableFiles)
        val state = loadState()

        if (state == null || state.fingerprint != fingerprint) {
            val shuffled = availableFiles.shuffled()
            saveState(QueueState(0, fingerprint, shuffled))
            log.info { "Map queue rebuilt during advance. Queue (${shuffled.size}): $shuffled" }
            return selectBatch(shuffled, 0)
        }

        val currentBatchSize = selectBatch(state.queue, state.position).size
        val newPosition = (state.position + currentBatchSize) % state.queue.size
        val newBatch = selectBatch(state.queue, newPosition)
        saveState(QueueState(newPosition, fingerprint, state.queue))
        log.info { "Map queue advanced to position $newPosition: $newBatch" }
        return newBatch
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
            QueueState(position, fingerprint, queue)
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
