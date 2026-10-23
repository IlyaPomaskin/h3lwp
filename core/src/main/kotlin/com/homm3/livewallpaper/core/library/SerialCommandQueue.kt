package com.homm3.livewallpaper.core.library

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ktx.log.logger

/**
 * Runs submitted commands one at a time, in order, on a single coroutine of [scope], so commands never
 * interleave at suspension points. A command equal to one still waiting is dropped. A failing command is
 * logged and doesn't stop the queue.
 */
class SerialCommandQueue<C : Any>(
    scope: CoroutineScope,
    private val execute: suspend (C) -> Unit,
) {
    private val commands = Channel<C>(Channel.UNLIMITED)
    private val waiting = mutableListOf<C>()

    init {
        scope.launch {
            for (command in commands) run(command)
        }
    }

    /** Queues [command] unless an equal one is waiting; safe to call from any thread. Returns whether it was queued. */
    fun submit(command: C): Boolean {
        synchronized(waiting) {
            if (command in waiting) return false
            waiting.add(command)
        }
        val isSent = commands.trySend(command).isSuccess
        if (!isSent) synchronized(waiting) { waiting.remove(command) }
        return isSent
    }

    /** Stops accepting commands; waiting ones are still run unless the scope is cancelled. */
    fun close() {
        commands.close()
    }

    private suspend fun run(command: C) {
        synchronized(waiting) { waiting.remove(command) }
        try {
            execute(command)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Command $command failed" }
        }
    }

    companion object {
        private val log = logger<SerialCommandQueue<*>>()
    }
}
