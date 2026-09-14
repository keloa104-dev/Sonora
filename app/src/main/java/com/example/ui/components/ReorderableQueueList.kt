package com.example.ui.components

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.model.AudioTrack
import java.util.Collections

/**
 * High-performance 120Hz drag-and-drop reorderable queue list powered by Android's native
 * [RecyclerView] and [ItemTouchHelper] with stable IDs and hardware-accelerated translation.
 *
 * Features:
 * - Real-time continuous fluid drag following finger coordinates with 0ms lag
 * - Unrestricted multi-position reordering in a single stroke (drag freely across 1 to 200+ positions)
 * - Dynamic list rearrangement: surrounding items slide smoothly out of the way in real time
 * - Elimination of hanging behavior: visual item detaches cleanly and elevates immediately
 * - Automated edge scrolling with responsive velocity scaling
 * - Hardware drag elevation (shadow & 1.03x scale) cleanly applied and removed
 * - Smooth drop and snap animation into the final destination slot
 */
@Composable
fun ReorderableQueueList(
    tracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    selectedTrackIds: Set<Long>,
    isDarkTheme: Boolean,
    canReorder: Boolean,
    searchQuery: String,
    onTrackClick: (AudioTrack) -> Unit,
    onToggleSelect: (AudioTrack) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onReorderCommitted: (List<AudioTrack>, Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) "No songs match \"$searchQuery\"" else "No songs in queue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val currentTrackId = currentTrack?.id
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 6.dp.roundToPx() }
    val bottomPaddingPx = with(density) { 80.dp.roundToPx() }

    // Keep updated lambda references to prevent stale closures inside ViewHolder bindings
    val currentOnTrackClick by rememberUpdatedState(onTrackClick)
    val currentOnToggleSelect by rememberUpdatedState(onToggleSelect)
    val currentOnMoveItem by rememberUpdatedState(onMoveItem)
    val currentOnReorderCommitted by rememberUpdatedState(onReorderCommitted)
    val currentOnRemoveItem by rememberUpdatedState(onRemoveItem)
    val currentCanReorder by rememberUpdatedState(canReorder)

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("playlist_sheet_tracks_list"),
        factory = { ctx ->
            val recyclerView = RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
                clipToPadding = true
                clipChildren = true
                setPadding(0, topPaddingPx, 0, bottomPaddingPx)
                (itemAnimator as? DefaultItemAnimator)?.apply {
                    supportsChangeAnimations = false
                    moveDuration = 180
                }
            }

            val adapter = QueueTrackAdapter(
                initialTracks = tracks,
                initialCurrentTrackId = currentTrackId,
                initialSelectedTrackIds = selectedTrackIds,
                isDarkTheme = isDarkTheme,
                canReorderProvider = { currentCanReorder },
                onTrackClick = { currentOnTrackClick(it) },
                onToggleSelect = { currentOnToggleSelect(it) },
                onMoveItemStep = { from, to -> currentOnMoveItem(from, to) },
                onRemoveItem = { currentOnRemoveItem(it) },
                onReorderCommitted = { list, from, to -> currentOnReorderCommitted(list, from, to) }
            )

            val callback = QueueItemTouchHelperCallback(
                adapter = adapter,
                canDragProvider = { currentCanReorder }
            )
            val itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper.attachToRecyclerView(recyclerView)
            adapter.itemTouchHelper = itemTouchHelper

            recyclerView.adapter = adapter
            recyclerView
        },
        update = { recyclerView ->
            val adapter = recyclerView.adapter as? QueueTrackAdapter
            adapter?.updateData(
                newTracks = tracks,
                newCurrentTrackId = currentTrackId,
                newSelectedIds = selectedTrackIds,
                newIsDarkTheme = isDarkTheme
            )
        }
    )
}

/**
 * Adapter with stable IDs for 120Hz smooth item animations and continuous reordering.
 */
class QueueTrackAdapter(
    initialTracks: List<AudioTrack>,
    private var initialCurrentTrackId: Long?,
    private var initialSelectedTrackIds: Set<Long>,
    private var isDarkTheme: Boolean,
    private val canReorderProvider: () -> Boolean,
    private val onTrackClick: (AudioTrack) -> Unit,
    private val onToggleSelect: (AudioTrack) -> Unit,
    private val onMoveItemStep: (Int, Int) -> Unit,
    private val onRemoveItem: (Int) -> Unit,
    private val onReorderCommitted: (List<AudioTrack>, Int, Int) -> Unit
) : RecyclerView.Adapter<QueueTrackAdapter.TrackViewHolder>() {

    private val tracks = initialTracks.toMutableList()
    private var currentTrackId = initialCurrentTrackId
    private var selectedTrackIds = initialSelectedTrackIds
    private var initialDragPosition: Int = RecyclerView.NO_POSITION
    private var isDraggingActive: Boolean = false
    var itemTouchHelper: ItemTouchHelper? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return tracks[position].id
    }

    override fun getItemCount(): Int = tracks.size

    fun updateData(
        newTracks: List<AudioTrack>,
        newCurrentTrackId: Long?,
        newSelectedIds: Set<Long>,
        newIsDarkTheme: Boolean
    ) {
        if (isDraggingActive) {
            // Never interrupt active drag operation with external data mutations
            return
        }
        val dataChanged = tracks != newTracks ||
                currentTrackId != newCurrentTrackId ||
                selectedTrackIds != newSelectedIds ||
                isDarkTheme != newIsDarkTheme

        if (dataChanged) {
            tracks.clear()
            tracks.addAll(newTracks)
            currentTrackId = newCurrentTrackId
            selectedTrackIds = newSelectedIds
            isDarkTheme = newIsDarkTheme
            notifyDataSetChanged()
        }
    }

    fun onItemDragStarted(fromPosition: Int) {
        isDraggingActive = true
        initialDragPosition = fromPosition
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= tracks.size || toPosition >= tracks.size) {
            return false
        }
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(tracks, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(tracks, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun onItemDragEnded(viewHolder: RecyclerView.ViewHolder) {
        val finalPosition = viewHolder.bindingAdapterPosition
        val startPos = initialDragPosition
        isDraggingActive = false
        initialDragPosition = RecyclerView.NO_POSITION
        if (startPos != RecyclerView.NO_POSITION && finalPosition != RecyclerView.NO_POSITION && startPos != finalPosition) {
            onReorderCommitted(tracks.toList(), startPos, finalPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return TrackViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        val isCurrent = track.id == currentTrackId
        val isSelected = selectedTrackIds.contains(track.id)
        val selectedCount = selectedTrackIds.size
        val canReorder = canReorderProvider() && selectedCount == 0

        holder.composeView.setContent {
            QueueTrackRow(
                track = track,
                index = position,
                totalCount = tracks.size,
                isCurrent = isCurrent,
                isSelected = isSelected,
                isSelectionActive = selectedCount > 0,
                canReorder = canReorder,
                isDarkTheme = isDarkTheme,
                onTrackClick = {
                    if (selectedCount > 0) {
                        onToggleSelect(track)
                    } else {
                        onTrackClick(track)
                    }
                },
                onToggleSelect = { onToggleSelect(track) },
                onStartDrag = {
                    itemTouchHelper?.startDrag(holder)
                },
                onMoveUp = {
                    val pos = holder.bindingAdapterPosition
                    if (pos > 0) {
                        onMoveItemStep(pos, pos - 1)
                    }
                },
                onMoveDown = {
                    val pos = holder.bindingAdapterPosition
                    if (pos in 0 until tracks.size - 1) {
                        onMoveItemStep(pos, pos + 1)
                    }
                },
                onRemove = {
                    val pos = holder.bindingAdapterPosition
                    if (pos in tracks.indices) {
                        onRemoveItem(pos)
                    }
                }
            )
        }
    }

    class TrackViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)
}

/**
 * Custom [ItemTouchHelper.Callback] enabling free vertical dragging, multi-position movement,
 * elevation, real-time swapping, and edge auto-scrolling.
 */
class QueueItemTouchHelperCallback(
    private val adapter: QueueTrackAdapter,
    private val canDragProvider: () -> Boolean
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = canDragProvider()

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if (!canDragProvider()) return makeMovementFlags(0, 0)
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            val pos = viewHolder.bindingAdapterPosition
            adapter.onItemDragStarted(pos)
            viewHolder.itemView.apply {
                elevation = 28f
                translationZ = 28f
                animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .alpha(0.96f)
                    .setDuration(120)
                    .start()
            }
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPos = viewHolder.bindingAdapterPosition
        val toPos = target.bindingAdapterPosition
        if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION && fromPos != toPos) {
            return adapter.onItemMove(fromPos, toPos)
        }
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.apply {
            animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(150)
                .withEndAction {
                    elevation = 0f
                    translationZ = 0f
                }
                .start()
        }
        adapter.onItemDragEnded(viewHolder)
    }

    override fun interpolateOutOfBoundsScroll(
        recyclerView: RecyclerView,
        viewSize: Int,
        viewSizeOutOfBounds: Int,
        totalSize: Int,
        msSinceStartScroll: Long
    ): Int {
        // Butter-smooth, accelerated edge auto-scroll when dragged near list boundaries
        val base = super.interpolateOutOfBoundsScroll(
            recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll
        )
        return (base * 1.5f).toInt().coerceIn(-40, 40)
    }
}

/**
 * Visual row representation for tracks in the playback queue with high-contrast styling,
 * selection checkbox, drag handle, step arrows, and delete action.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QueueTrackRow(
    track: AudioTrack,
    index: Int,
    totalCount: Int,
    isCurrent: Boolean,
    isSelected: Boolean,
    isSelectionActive: Boolean,
    canReorder: Boolean,
    isDarkTheme: Boolean,
    onTrackClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onStartDrag: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTrackBgColor = if (isDarkTheme) Color(0xFF223555) else Color(0xFFE2ECFA)
    val activeTrackBorderColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val activeTitleColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val activeSubtitleColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1E40AF)
    val activeBadgeBgColor = if (isDarkTheme) Color(0xFF2563EB) else Color(0xFF1D4ED8)
    val activeActionIconColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val selectedTrackBgColor = if (isDarkTheme) Color(0xFF2A2D3A) else Color(0xFFE8EDF5)
    val selectedTrackBorderColor = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
    val normalTrackBgColor = if (isDarkTheme) Color(0xFF1C2028) else Color(0xFFF1F5F9)
    val normalTrackBorderColor = if (isDarkTheme) Color(0xFF282D39) else Color(0xFFE2E8F0)
    val normalTitleColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val normalSubtitleColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
    val normalActionIconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            colors = CheckboxDefaults.colors(
                checkedColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB),
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .size(32.dp)
                .testTag("queue_checkbox_${track.id}")
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Highlight Container
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(track.id, canReorder, isSelectionActive) {
                    if (canReorder) {
                        detectTapGestures(
                            onTap = { onTrackClick() },
                            onLongPress = { onStartDrag() }
                        )
                    } else {
                        detectTapGestures(
                            onTap = { onTrackClick() }
                        )
                    }
                }
                .testTag("queue_item_${track.id}"),
            shape = RoundedCornerShape(8.dp),
            color = when {
                isSelected -> selectedTrackBgColor
                isCurrent -> activeTrackBgColor
                else -> normalTrackBgColor
            },
            border = when {
                isCurrent -> BorderStroke(1.5.dp, activeTrackBorderColor)
                isSelected -> BorderStroke(1.dp, selectedTrackBorderColor)
                else -> BorderStroke(1.dp, normalTrackBorderColor)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Thumbnail, Title and Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    StaticTrackThumbnail(
                        track = track,
                        cornerRadius = 6.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag(if (isCurrent) "now_playing_artwork" else "track_artwork_${track.id}")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) activeTitleColor else normalTitleColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = activeBadgeBgColor,
                                    modifier = Modifier.testTag("now_playing_badge")
                                ) {
                                    Text(
                                        text = "NOW PLAYING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            letterSpacing = 0.3.sp,
                                            lineHeight = 10.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            val subtitle = if (track.relativePath.isNotBlank()) {
                                track.relativePath
                            } else if (track.artist.isNotBlank() || track.album.isNotBlank()) {
                                "${track.artist} • ${track.album}"
                            } else {
                                "Audio Track"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) activeSubtitleColor else normalSubtitleColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                // Actions: Drag Handle, Up/Down Arrows, and Delete Button
                if (!isSelectionActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        if (canReorder) {
                            // Dedicated Drag Handle: Direct instant touch triggers ItemTouchHelper.startDrag
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("queue_drag_handle_${track.id}")
                                    .pointerInteropFilter { motionEvent ->
                                        if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                                            onStartDrag()
                                        }
                                        false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = if (isCurrent) activeActionIconColor else normalActionIconColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Move Up Arrow
                            if (index > 0) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (isCurrent) activeActionIconColor else normalActionIconColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Move Down Arrow
                            if (index < totalCount - 1) {
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (isCurrent) activeActionIconColor else normalActionIconColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Remove from Queue
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
