package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a smooth NestedScrollConnection with directional hysteresis for music capsules & floating docks:
 * - Scrolling Down (user swiping up to see newer items):
 *   Requires a deliberate threshold [scrollDownThresholdDp] before smoothly tucking away, avoiding accidental triggers on micro-taps.
 * - Scrolling Up (user swiping down to see earlier items):
 *   Requires a comfortable, generous scroll buffer distance [scrollUpThresholdDp] before revealing the capsules again,
 *   giving the user uninterrupted viewing space without capsules jumping in prematurely.
 */
@Composable
fun rememberCapsuleScrollConnection(
    enabled: Boolean,
    onVisibilityChanged: (Boolean) -> Unit,
    scrollUpThresholdDp: Dp = 110.dp,
    scrollDownThresholdDp: Dp = 32.dp
): NestedScrollConnection {
    val density = LocalDensity.current
    val upThresholdPx = with(density) { scrollUpThresholdDp.toPx() }
    val downThresholdPx = with(density) { scrollDownThresholdDp.toPx() }

    return remember(enabled, upThresholdPx, downThresholdPx) {
        var accumulatedUp = 0f
        var accumulatedDown = 0f

        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero

                val deltaY = available.y

                if (deltaY < -1.5f) {
                    // Scrolling down (finger moves up, content scrolls down)
                    accumulatedUp = 0f
                    accumulatedDown += -deltaY
                    if (accumulatedDown >= downThresholdPx) {
                        onVisibilityChanged(false)
                        accumulatedDown = 0f
                    }
                } else if (deltaY > 1.5f) {
                    // Scrolling up (finger moves down, content scrolls up)
                    // Accumulate upward scroll distance until reaching the comfort threshold
                    accumulatedDown = 0f
                    accumulatedUp += deltaY
                    if (accumulatedUp >= upThresholdPx) {
                        onVisibilityChanged(true)
                        accumulatedUp = 0f
                    }
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If overscrolling or pull-to-top (available.y > 0 and not consumed),
                // user is pulling at the top of the list -> immediately show capsules
                if (enabled && available.y > 6f) {
                    onVisibilityChanged(true)
                    accumulatedUp = 0f
                    accumulatedDown = 0f
                }
                return Offset.Zero
            }
        }
    }
}
