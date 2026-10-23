package com.homm3.livewallpaper.core.library

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SerialCommandQueueTest {
    private val timeoutMillis = 5_000L

    private fun <C : Any> CoroutineScope.recordingQueue(
        executed: MutableList<C>,
        lastCommand: C,
        done: CompletableDeferred<Unit>,
        beforeRecord: suspend (C) -> Unit = {},
    ) = SerialCommandQueue<C>(this) { command ->
        beforeRecord(command)
        executed.add(command)
        if (command == lastCommand) done.complete(Unit)
    }

    @Test
    fun `commands run one at a time in submission order`() = runBlocking {
        val executed = mutableListOf<String>()
        val done = CompletableDeferred<Unit>()
        var running = 0
        var maxRunning = 0
        val queue = recordingQueue(executed, lastCommand = "c", done) {
            running++
            maxRunning = maxOf(maxRunning, running)
            delay(10)
            yield()
            running--
        }

        listOf("a", "b", "c").forEach(queue::submit)
        withTimeout(timeoutMillis) { done.await() }
        queue.close()

        assertEquals(listOf("a", "b", "c"), executed)
        assertEquals(1, maxRunning)
    }

    @Test
    fun `a command equal to a waiting one is dropped`() = runBlocking {
        val executed = mutableListOf<MapCommand>()
        val done = CompletableDeferred<Unit>()
        val queue = recordingQueue(executed, lastCommand = MapCommand.Reinit, done)

        val forward = MapCommand.LoadNext(Direction.FORWARD)
        val backward = MapCommand.LoadNext(Direction.BACKWARD)
        assertTrue(queue.submit(forward))
        assertFalse(queue.submit(MapCommand.LoadNext(Direction.FORWARD)))
        assertTrue(queue.submit(backward))
        assertTrue(queue.submit(MapCommand.Reinit))
        withTimeout(timeoutMillis) { done.await() }
        queue.close()

        assertEquals(listOf(forward, backward, MapCommand.Reinit), executed)
    }

    @Test
    fun `a command can be queued again once it started`() = runBlocking {
        val executed = mutableListOf<MapCommand>()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val done = CompletableDeferred<Unit>()
        val queue = SerialCommandQueue<MapCommand>(this) { command ->
            if (executed.isEmpty()) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
            executed.add(command)
            if (executed.size == 2) done.complete(Unit)
        }

        queue.submit(MapCommand.Refresh)
        withTimeout(timeoutMillis) { firstStarted.await() }
        assertTrue(queue.submit(MapCommand.Refresh))
        releaseFirst.complete(Unit)
        withTimeout(timeoutMillis) { done.await() }
        queue.close()

        assertEquals(listOf<MapCommand>(MapCommand.Refresh, MapCommand.Refresh), executed)
    }

    @Test
    fun `a failing command doesn't stop the queue`() {
        val app = HeadlessApplication(object : ApplicationAdapter() {})
        try {
            runBlocking {
                val executed = mutableListOf<String>()
                val done = CompletableDeferred<Unit>()
                val queue = recordingQueue(executed, lastCommand = "after", done) { command ->
                    check(command != "failing") { "expected failure" }
                }

                queue.submit("failing")
                queue.submit("after")
                withTimeout(timeoutMillis) { done.await() }
                queue.close()

                assertEquals(listOf("after"), executed)
            }
        } finally {
            app.exit()
        }
    }
}
