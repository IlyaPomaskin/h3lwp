package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.log.logger

class MapQueue {

    /**
     * Current batch with no rotation. Initializes/rebuilds the queue if the
     * fingerprint has changed (or if there's no saved state yet) — first
     * initialization stamps `lastRotationTime = now`, so [advanceIfDue] won't
     * fire immediately after install / first-run.
     */
    fun currentBatch(availableFiles: List<String>): List<String> {
        if (availableFiles.isEmpty()) return emptyList()

        val fingerprint = computeFingerprint(availableFiles)
        val state = loadState()

        if (state == null || state.fingerprint != fingerprint) {
            val shuffled = availableFiles.shuffled()
            val batch = selectBatch(shuffled, 0)
            saveState(QueueState(now(), 0, fingerprint, shuffled))
            log.info { "Map queue rebuilt. Queue order (${shuffled.size}): $shuffled" }
            log.info { "Loading ${batch.size} maps at position 0: $batch" }
            return batch
        }

        val batch = selectBatch(state.queue, state.position)
        log.info { "Queue (${state.queue.size}) at position ${state.position}: $batch" }
        return batch
    }

    /**
     * If at least [ROTATION_INTERVAL_MS] has elapsed since the last rotation
     * (or since first initialization), advances `position` by the current
     * batch size, persists the new state with `lastRotationTime = now`, and
     * returns the new batch. Otherwise returns `null`.
     *
     * When [force] is true, skips the elapsed-time check and rotates immediately.
     *
     * Like [currentBatch], rebuilds the queue if the fingerprint has changed.
     */
    fun advanceIfDue(availableFiles: List<String>, force: Boolean = false): List<String>? {
        if (availableFiles.isEmpty()) return null

        val fingerprint = computeFingerprint(availableFiles)
        val state = loadState()

        if (state == null || state.fingerprint != fingerprint) {
            // Treat first initialization as a rotation point so the next due-check
            // starts the timer from now.
            val shuffled = availableFiles.shuffled()
            val batch = selectBatch(shuffled, 0)
            saveState(QueueState(now(), 0, fingerprint, shuffled))
            log.info { "Map queue rebuilt during advanceIfDue. Queue (${shuffled.size}): $shuffled" }
            return batch
        }

        val elapsed = now() - state.lastRotationTime
        if (!force && elapsed < ROTATION_INTERVAL_MS) {
            log.info { "advanceIfDue: not yet due (${elapsed / 60_000}min / ${ROTATION_INTERVAL_MS / 60_000}min)" }
            return null
        }

        val currentBatchSize = selectBatch(state.queue, state.position).size
        val newPosition = (state.position + currentBatchSize) % state.queue.size
        val newBatch = selectBatch(state.queue, newPosition)
        saveState(QueueState(now(), newPosition, fingerprint, state.queue))
        log.info {
            "advanceIfDue: rotated after ${elapsed / 60_000}min (force=$force); " +
                "new position=$newPosition, batch=$newBatch"
        }
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

    private fun now(): Long = System.currentTimeMillis()

    private fun loadState(): QueueState? {
        return try {
            val prefs = Gdx.app.getPreferences(PREFS_NAME)
            val json = prefs.getString(PREFS_KEY, null) ?: return null
            val root = JsonReader().parse(json)

            val lastRotationTime = root.getLong("lastRotationTime", -1L)
            if (lastRotationTime < 0L) return null
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
            QueueState(lastRotationTime, position, fingerprint, queue)
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
                "lastRotationTime" to state.lastRotationTime,
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
        val lastRotationTime: Long,
        val position: Int,
        val fingerprint: String,
        val queue: List<String>
    )

    companion object {
        private val log = logger<MapQueue>()
        private const val BATCH_SIZE = 3
        private const val ROTATION_INTERVAL_MS = 60L * 60L * 1000L  // 1 hour
        private const val PREFS_NAME = "map-queue"
        private const val PREFS_KEY = "state"
    }
}
