package fr.husi.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CoreHostReadinessTest {
    @Test
    fun `retries until the host serves requests`() = runTest {
        var attempts = 0
        awaitCoreHostReady("core", 1.seconds, { true }) {
            if (++attempts < 3) throw IOException("not listening yet")
        }
        assertEquals(3, attempts)
    }

    @Test
    fun `timeout retains the connection failure`() = runTest {
        val failure = IOException("transport 1 is not supported on this platform")
        val error = assertFailsWith<IOException> {
            awaitCoreHostReady("core", 1.seconds, { true }) { throw failure }
        }
        assertEquals(failure.message, generateSequence<Throwable>(error) { it.cause }.last().message)
        assertTrue(error.message.orEmpty().contains(failure.message!!))
    }

    @Test
    fun `timeout also bounds a stalled probe`() = runTest {
        val error = assertFailsWith<IOException> {
            awaitCoreHostReady("core", 1.seconds, { true }) { awaitCancellation() }
        }
        assertTrue(error.message.orEmpty().contains("timed out"))
    }

    @Test
    fun `cancellation is propagated without retry`() = runTest {
        val cancellation = CancellationException("cancelled")
        assertEquals(cancellation.message, assertFailsWith<CancellationException> {
            awaitCoreHostReady("core", 1.seconds, { true }) { throw cancellation }
        }.message)
    }

    @Test
    fun `process exit retains the preceding connection failure`() = runTest {
        var alive = true
        val failure = IOException("connection refused")
        val error = assertFailsWith<IOException> {
            awaitCoreHostReady("core", 1.seconds, { alive }) {
                alive = false
                throw failure
            }
        }
        assertEquals(failure.message, generateSequence<Throwable>(error) { it.cause }.last().message)
        assertTrue(error.message.orEmpty().contains("exited"))
    }
}
