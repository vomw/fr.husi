package fr.husi.compose

import androidx.compose.material3.SearchBarState
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Runs the Material search-bar state change without a visible transition. */
private object InstantMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float = 0f

    override val key: CoroutineContext.Key<*> = MotionDurationScale
}

suspend fun SearchBarState.expandImmediately() {
    withContext(InstantMotionDurationScale) {
        animateToExpanded()
    }
}

suspend fun SearchBarState.collapseImmediately() {
    withContext(InstantMotionDurationScale) {
        animateToCollapsed()
    }
}
