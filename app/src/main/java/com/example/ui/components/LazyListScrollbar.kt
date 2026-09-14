package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.primary
) {
    val totalItems by remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
    val visibleItems by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }

    if (totalItems <= 1 || visibleItems >= totalItems) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var containerHeightPx by remember { mutableFloatStateOf(1f) }

    val thumbRatio by remember {
        derivedStateOf {
            (visibleItems.toFloat() / totalItems.toFloat().coerceAtLeast(1f)).coerceIn(0.12f, 0.8f)
        }
    }
    val thumbHeightPx = containerHeightPx * thumbRatio
    val maxThumbOffsetPx = (containerHeightPx - thumbHeightPx).coerceAtLeast(1f)

    val thumbOffsetPx by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val count = layoutInfo.totalItemsCount
            val vCount = layoutInfo.visibleItemsInfo.size
            val first = state.firstVisibleItemIndex
            val offset = state.firstVisibleItemScrollOffset
            val avgHeight = if (vCount > 0) layoutInfo.visibleItemsInfo.sumOf { it.size }.toFloat() / vCount else 1f
            val totalSize = count * avgHeight
            val curPos = first * avgHeight + offset
            val maxScroll = (totalSize - layoutInfo.viewportEndOffset).coerceAtLeast(1f)
            val progress = (curPos / maxScroll).coerceIn(0f, 1f)
            progress * (containerHeightPx - containerHeightPx * (vCount.toFloat() / count.toFloat().coerceAtLeast(1f)).coerceIn(0.12f, 0.8f)).coerceAtLeast(1f)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (state.isScrollInProgress || isDragging) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbar_alpha"
    )

    fun scrollToY(yPx: Float) {
        if (totalItems <= 0) return
        val clampedY = (yPx - thumbHeightPx / 2f).coerceIn(0f, maxThumbOffsetPx)
        val progress = clampedY / maxThumbOffsetPx
        val targetIndex = (progress * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
        coroutineScope.launch {
            state.scrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .padding(vertical = 4.dp)
            .onSizeChanged { containerHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(totalItems, containerHeightPx, thumbHeightPx) {
                detectTapGestures { offset ->
                    scrollToY(offset.y)
                }
            }
            .pointerInput(totalItems, containerHeightPx, thumbHeightPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        scrollToY(offset.y)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        scrollToY(change.position.y)
                    }
                )
            }
            .alpha(alpha)
    ) {
        // Track
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )
        // Thumb
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(with(LocalDensity.current) { thumbHeightPx.toDp() })
                .graphicsLayer { translationY = thumbOffsetPx }
                .clip(RoundedCornerShape(4.dp))
                .background(thumbColor)
        )
    }
}
