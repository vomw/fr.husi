package fr.husi.repository

import fr.husi.ktx.readableMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal suspend fun awaitCoreHostReady(
    socketBasePath: String,
    timeout: Duration,
    isAlive: () -> Boolean,
    probe: suspend () -> Unit,
) {
    var lastFailure: Exception? = null
    val ready = withTimeoutOrNull(timeout) {
        var retryDelay = 50.milliseconds
        var serving = false
        while (!serving) {
            if (!isAlive()) {
                throw IOException("husi-core session exited before becoming ready", lastFailure)
            }
            try {
                probe()
                serving = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
            }
            if (!serving) {
                delay(retryDelay)
                retryDelay = (retryDelay * 2).coerceAtMost(500.milliseconds)
            }
        }
        true
    }
    if (ready == true) return
    val detail = lastFailure?.let { ": ${it.readableMessage}" }.orEmpty()
    throw IOException("timed out waiting for husi-core session on $socketBasePath$detail", lastFailure)
}
