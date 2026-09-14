package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.CountDownTimer
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.example.data.MediaScanner
import com.example.data.SettingsRepository
import com.example.model.AudioTrack
import com.example.model.SliderPosition
import com.example.model.UserSettings
import com.example.model.MusicVisualTheme
import com.example.service.PlayerService
import com.example.util.VinylArtworkGenerator
import com.example.util.MusicVisualArtworkGenerator
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.net.Uri
import android.os.Bundle
import androidx.media3.session.SessionCommand

import com.example.data.FolderTreeScanner
import com.example.model.FolderNode

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mediaScanner = MediaScanner(context)
    val settingsRepository = SettingsRepository(context)
    val musicRepository = com.example.data.MusicRepository(context)

    // State Flows
    private val _tracks = MutableStateFlow<List<AudioTrack>>(com.example.data.SongCache.allSongs)
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    private val _isTracksLoading = MutableStateFlow(!com.example.data.SongCache.isLoaded)
    val isTracksLoading: StateFlow<Boolean> = _isTracksLoading.asStateFlow()

    private val _folderTrees = MutableStateFlow<List<FolderNode>>(emptyList())
    val folderTrees: StateFlow<List<FolderNode>> = _folderTrees.asStateFlow()

    private val _isFoldersLoading = MutableStateFlow(true)
    val isFoldersLoading: StateFlow<Boolean> = _isFoldersLoading.asStateFlow()

    private val _deletionProgress = MutableStateFlow<DeletionProgress?>(null)
    val deletionProgress: StateFlow<DeletionProgress?> = _deletionProgress.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<AudioTrack>>(emptyList())
    val currentQueue: StateFlow<List<AudioTrack>> = _currentQueue.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volume = MutableStateFlow(getCurrentSystemVolumeRatio())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow(0)
    val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val userSettings: StateFlow<UserSettings> = settingsRepository.settings

    private val _isScrollHeaderDockVisible = MutableStateFlow(true)
    val isScrollHeaderDockVisible: StateFlow<Boolean> = _isScrollHeaderDockVisible.asStateFlow()

    fun setScrollHeaderDockVisible(visible: Boolean) {
        if (_isScrollHeaderDockVisible.value != visible) {
            _isScrollHeaderDockVisible.value = visible
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressUpdateJob: Job? = null
    private var countDownTimer: CountDownTimer? = null

    private var volumeAtMuteRatio: Float = 0.5f

    private val volumeObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            syncVolumeFromSystem()
        }
    }

    private val broadcastReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    syncVolumeFromSystem()
                }
                "com.example.ACTION_TOGGLE_FAVORITE_EVENT" -> {
                    val trackId = intent.getLongExtra("TRACK_ID", -1L)
                    val isFav = intent.getBooleanExtra("IS_FAVORITE", false)
                    if (trackId != -1L) {
                        val currentFavs = _favoriteIds.value.toMutableSet()
                        if (isFav) currentFavs.add(trackId) else currentFavs.remove(trackId)
                        _favoriteIds.value = currentFavs

                        _currentTrack.value?.let { active ->
                            if (active.id == trackId) {
                                _currentTrack.value = active.copy(isFavorite = isFav)
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        // 3-Layer Caching:
        // Layer 1: In-memory SongCache (immediate 0ms emit)
        if (com.example.data.SongCache.isLoaded) {
            _tracks.value = com.example.data.SongCache.allSongs
            _isTracksLoading.value = false
        } else {
            // Layer 2: Fast Room DB load on IO before network/mediastore scan completes
            viewModelScope.launch(Dispatchers.IO) {
                val dbTracks = musicRepository.getCachedOrDbTracks()
                if (dbTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _tracks.value = dbTracks
                        _isTracksLoading.value = false
                    }
                }
            }
        }
        initMediaController()
        observeRoomDatabase()
        loadAudioTracks()
        try {
            val filter = android.content.IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction("com.example.ACTION_TOGGLE_FAVORITE_EVENT")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                volumeObserver
            )
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.getUriFor("volume_music"),
                true,
                volumeObserver
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncVolumeFromSystem(isHardwareKeyPress: Boolean = false, keyCode: Int? = null) {
        val current = try { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } catch (e: Exception) { 0 }
        val max = try { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) } catch (e: Exception) { 15 }
        val newVol = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0.5f
        val isSystemStreamMuted = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) || current == 0

        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_MUTE || keyCode == android.view.KeyEvent.KEYCODE_MUTE) {
            val nextMuted = !_isMuted.value
            _isMuted.value = nextMuted
            if (nextMuted) {
                if (_volume.value > 0.01f) {
                    volumeAtMuteRatio = _volume.value
                }
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _volume.value = 0f
                mediaController?.volume = 0f
            } else {
                val restored = if (volumeAtMuteRatio > 0.01f) volumeAtMuteRatio else 0.5f
                val targetVol = Math.round(restored * max).toInt().coerceIn(1, max)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val actualRatio = if (max > 0) targetVol.toFloat() / max.toFloat() else restored
                _volume.value = actualRatio
                mediaController?.volume = actualRatio
            }
            return
        }

        if (isSystemStreamMuted) {
            _isMuted.value = true
            _volume.value = 0f
            mediaController?.volume = 0f
        } else {
            _isMuted.value = false
            _volume.value = newVol
            mediaController?.volume = newVol
            if (newVol > 0.01f) {
                volumeAtMuteRatio = newVol
            }
        }
    }

    fun setVolumeRatio(ratio: Float) {
        val clampedRatio = ratio.coerceIn(0.0f, 1.0f)
        val maxVolume = try { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) } catch (e: Exception) { 15 }
        val targetVolume = Math.round(clampedRatio * maxVolume).toInt().coerceIn(0, maxVolume)
        
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val exactRatio = if (maxVolume > 0) targetVolume.toFloat() / maxVolume.toFloat() else clampedRatio
        _volume.value = exactRatio
        _isMuted.value = targetVolume == 0
        mediaController?.volume = exactRatio
        if (exactRatio > 0.01f) {
            volumeAtMuteRatio = exactRatio
        }
    }

    fun toggleMute() {
        val max = try { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) } catch (e: Exception) { 15 }
        val nextMuted = !_isMuted.value
        _isMuted.value = nextMuted
        if (nextMuted) {
            // Save pre-mute volume ratio and mute media playback
            if (_volume.value > 0.01f) {
                volumeAtMuteRatio = _volume.value
            }
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _volume.value = 0f
            mediaController?.volume = 0f
        } else {
            // Unmute media playback back to current volume ratio
            val restored = if (volumeAtMuteRatio > 0.01f) volumeAtMuteRatio else 0.5f
            val targetVol = Math.round(restored * max).toInt().coerceIn(1, max)
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val actualRatio = if (max > 0) targetVol.toFloat() / max.toFloat() else restored
            _volume.value = actualRatio
            mediaController?.volume = actualRatio
        }
    }

    private fun observeRoomDatabase() {
        viewModelScope.launch {
            musicRepository.allTracksFlow.collect { list ->
                _tracks.value = list
                _isTracksLoading.value = false
                val roomFavs = list.filter { it.isFavorite }.map { it.id }.toSet()
                _favoriteIds.value = roomFavs
                _currentTrack.value?.let { active ->
                    val isFav = roomFavs.contains(active.id)
                    if (active.isFavorite != isFav) {
                        _currentTrack.value = active.copy(isFavorite = isFav)
                    }
                }
                if (!isStateRestored && list.isNotEmpty()) {
                    restoreSavedStateIfReady()
                }
            }
        }
        viewModelScope.launch {
            musicRepository.allFoldersFlow.collect { folderNodes ->
                _folderTrees.value = folderNodes
                _isFoldersLoading.value = false
            }
        }
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val controller = controllerFuture?.get()
                    mediaController = controller
                    setupPlayerListener(controller)
                    if (pendingExternalTrack != null) {
                        val trackToPlay = pendingExternalTrack
                        pendingExternalTrack = null
                        isStateRestored = true
                        playTrackList(listOf(trackToPlay!!), 0)
                    } else if (!isStateRestored) {
                        restoreSavedStateIfReady()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(controller: MediaController?) {
        controller ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Determine whether player should be considered playing.
                // When seeking or buffering, ExoPlayer temporarily sets isPlaying to false even though
                // playWhenReady is true. We keep the play state stable so the play/pause button does not flicker.
                val shouldBePlaying = if (controller.playbackState == Player.STATE_ENDED) {
                    false
                } else {
                    controller.playWhenReady
                }
                _isPlaying.value = shouldBePlaying
                saveCurrentPlaybackState()
                if (shouldBePlaying) {
                    startProgressUpdateLoop()
                } else {
                    stopProgressUpdateLoop()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                val shouldBePlaying = if (controller.playbackState == Player.STATE_ENDED) {
                    false
                } else {
                    playWhenReady
                }
                _isPlaying.value = shouldBePlaying
                saveCurrentPlaybackState()
                if (shouldBePlaying) {
                    startProgressUpdateLoop()
                } else {
                    stopProgressUpdateLoop()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    stopProgressUpdateLoop()
                    saveCurrentPlaybackState()
                } else if (playbackState == Player.STATE_READY) {
                    _duration.value = controller.duration.coerceAtLeast(0L)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentMediaId = mediaItem?.mediaId?.toLongOrNull()
                if (currentMediaId != null) {
                    val track = _tracks.value.find { it.id == currentMediaId }
                        ?: _currentQueue.value.find { it.id == currentMediaId }
                    if (track != null) {
                        _currentTrack.value = track
                    }
                }
                _duration.value = controller.duration.coerceAtLeast(0L)
                saveCurrentPlaybackState()
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackSpeed.value = playbackParameters.speed
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffle.value = shuffleModeEnabled
            }
        })

        _isPlaying.value = controller.isPlaying
        _playbackSpeed.value = controller.playbackParameters.speed
        _duration.value = controller.duration.coerceAtLeast(0L)
        _repeatMode.value = controller.repeatMode
        _isShuffle.value = controller.shuffleModeEnabled
    }

    private var isStateRestored = false
    private var isRestoringState = false
    private var pendingExternalTrack: AudioTrack? = null

    private fun restoreSavedStateIfReady() {
        if (isStateRestored || isRestoringState) return
        val controller = mediaController ?: return
        val list = _tracks.value
        val folderNodes = _folderTrees.value
        if (list.isEmpty() && folderNodes.isEmpty()) return

        isRestoringState = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedState = settingsRepository.loadPlaybackState()
                val allScanned = (list + folderNodes.flatMap { it.getAllTracksRecursively() })
                    .filter { it.id !in 10001L..10005L && !it.uri.toString().contains("exoplayer-test-media") }
                    .distinctBy { it.id }

                val restoredQueue = if (savedState.queueUris.isNotEmpty()) {
                    savedState.queueUris.mapNotNull { uriStr ->
                        allScanned.find { it.uri.toString() == uriStr }
                    }
                } else emptyList()

                val queueToUse = restoredQueue
                if (queueToUse.isNotEmpty()) {
                    val restoredTrack = if (savedState.lastTrackUri.isNotBlank()) {
                        allScanned.find { it.uri.toString() == savedState.lastTrackUri } ?: queueToUse.getOrNull(savedState.trackIndex) ?: queueToUse.firstOrNull()
                    } else {
                        queueToUse.getOrNull(savedState.trackIndex) ?: queueToUse.firstOrNull()
                    }

                    val mediaItems = queueToUse.map { track ->
                        val themeBytes = if (track.albumArtUri == null && track.albumArtDrawableRes == null) {
                            MusicVisualArtworkGenerator.generateThemeByteArray(context, track, userSettings.value.musicVisualTheme, 512)
                        } else null

                        val metadataBuilder = MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)

                        if (track.albumArtUri != null) {
                            metadataBuilder.setArtworkUri(track.albumArtUri)
                        } else if (track.albumArtDrawableRes != null) {
                            metadataBuilder.setArtworkUri(android.net.Uri.parse("android.resource://${context.packageName}/${track.albumArtDrawableRes}"))
                        } else {
                            if (themeBytes != null) {
                                metadataBuilder.setArtworkData(themeBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            }
                        }

                        val metadata = metadataBuilder.build()

                        MediaItem.Builder()
                            .setMediaId(track.id.toString())
                            .setUri(track.uri)
                            .setMediaMetadata(metadata)
                            .build()
                    }

                    withContext(Dispatchers.Main) {
                        _currentQueue.value = queueToUse
                        _currentTrack.value = restoredTrack

                        controller.setMediaItems(mediaItems)
                        controller.prepare()

                        val targetIndex = if (restoredTrack != null) queueToUse.indexOfFirst { it.id == restoredTrack.id }.coerceAtLeast(0) else savedState.trackIndex.coerceIn(0, queueToUse.size - 1)
                        controller.seekTo(targetIndex, savedState.positionMs.coerceAtLeast(0L))
                        _currentPosition.value = savedState.positionMs.coerceAtLeast(0L)

                        if (savedState.isPlaying) {
                            controller.play()
                        }
                        isStateRestored = true
                    }
                } else {
                    isStateRestored = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRestoringState = false
            }
        }
    }

    fun saveCurrentPlaybackState() {
        val track = _currentTrack.value
        val trackUri = track?.uri?.toString() ?: ""
        val queue = _currentQueue.value
        val index = if (track != null) queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0) else 0
        val pos = _currentPosition.value
        val playing = _isPlaying.value
        val vol = _volume.value
        settingsRepository.savePlaybackState(
            trackUri = trackUri,
            trackIndex = index,
            positionMs = pos,
            isPlaying = playing,
            queueUris = queue.map { it.uri.toString() },
            volume = vol
        )
    }

    fun loadAudioTracks() {
        if (_tracks.value.isEmpty() && !com.example.data.SongCache.isLoaded) {
            _isTracksLoading.value = true
            _isFoldersLoading.value = true
        }
        viewModelScope.launch(Dispatchers.IO) {
            val customUris = userSettings.value.customFolderUris
            val autoScan = userSettings.value.autoScanDevice
            musicRepository.syncMedia(customUris, autoScan)
        }
    }

    fun setAutoScanDevice(enabled: Boolean) {
        settingsRepository.setAutoScanDevice(enabled)
        loadAudioTracks()
    }

    @OptIn(UnstableApi::class)
    private fun prepareQueueOnMain(trackList: List<AudioTrack>, startIndex: Int = 0, autoPlay: Boolean = false) {
        val controller = mediaController ?: return
        _currentQueue.value = trackList
        val validIndex = startIndex.coerceIn(0, (trackList.size - 1).coerceAtLeast(0))
        if (trackList.isNotEmpty()) {
            _currentTrack.value = trackList[validIndex]
        }

        val activeTrackId = trackList.getOrNull(validIndex)?.id
        val mediaItems = trackList.map { track ->
            val themeBytes = if (track.id == activeTrackId && track.albumArtUri == null && track.albumArtDrawableRes == null) {
                MusicVisualArtworkGenerator.generateThemeByteArray(context, track, userSettings.value.musicVisualTheme, 512)
            } else null

            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)

            if (track.albumArtUri != null) {
                metadataBuilder.setArtworkUri(track.albumArtUri)
            } else if (track.albumArtDrawableRes != null) {
                metadataBuilder.setArtworkUri(android.net.Uri.parse("android.resource://${context.packageName}/${track.albumArtDrawableRes}"))
            } else {
                if (themeBytes != null) {
                    metadataBuilder.setArtworkData(themeBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }

            val metadata = metadataBuilder.build()

            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(metadata)
                .build()
        }

        try {
            controller.setMediaItems(mediaItems)
            controller.prepare()
            if (validIndex < mediaItems.size) {
                controller.seekTo(validIndex, 0L)
            }
            if (autoPlay) {
                controller.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentList = _currentQueue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _currentQueue.value = currentList
            saveCurrentPlaybackState()
            try {
                mediaController?.moveMediaItem(fromIndex, toIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun commitReorderedQueue(newList: List<AudioTrack>, fromIndex: Int, toIndex: Int) {
        _currentQueue.value = newList
        saveCurrentPlaybackState()
        try {
            if (fromIndex != toIndex && fromIndex in newList.indices && toIndex in newList.indices) {
                mediaController?.moveMediaItem(fromIndex, toIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addTracksToQueue(tracks: List<AudioTrack>) {
        if (tracks.isEmpty()) return
        val currentList = _currentQueue.value.toMutableList()
        currentList.addAll(tracks)
        _currentQueue.value = currentList
        saveCurrentPlaybackState()
        val currentIndex = if (_currentTrack.value != null) {
            currentList.indexOfFirst { it.id == _currentTrack.value?.id }.coerceAtLeast(0)
        } else 0
        prepareQueueOnMain(currentList, startIndex = currentIndex, autoPlay = false)
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        val currentList = _currentQueue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _currentQueue.value = currentList
            try {
                controller.removeMediaItem(index)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearQueue() {
        val controller = mediaController
        _currentQueue.value = emptyList()
        _currentTrack.value = null
        _isPlaying.value = false
        try {
            controller?.clearMediaItems()
            controller?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearRecentlyLoadedTracks() {
        val controller = mediaController
        _tracks.value = emptyList()
        _currentQueue.value = emptyList()
        _currentTrack.value = null
        _isPlaying.value = false
        try {
            controller?.clearMediaItems()
            controller?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OptIn(UnstableApi::class)
    fun addToQueue(track: AudioTrack) {
        val controller = mediaController ?: return
        val currentList = _currentQueue.value.toMutableList()
        currentList.add(track)
        _currentQueue.value = currentList

        val themeBytes = if (track.albumArtUri == null && track.albumArtDrawableRes == null) {
            MusicVisualArtworkGenerator.generateThemeByteArray(context, track, userSettings.value.musicVisualTheme, 512)
        } else null

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)

        if (track.albumArtUri != null) {
            metadataBuilder.setArtworkUri(track.albumArtUri)
        } else if (track.albumArtDrawableRes != null) {
            metadataBuilder.setArtworkUri(android.net.Uri.parse("android.resource://${context.packageName}/${track.albumArtDrawableRes}"))
        } else {
            if (themeBytes != null) {
                metadataBuilder.setArtworkData(themeBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }

        val metadata = metadataBuilder.build()

        val item = MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()

        try {
            controller.addMediaItem(item)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCustomFolderUri(uri: android.net.Uri?) {
        if (uri == null) return
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val uriStr = uri.toString()
            if (uriStr.isNotBlank()) {
                settingsRepository.addFolderUri(uriStr)
                loadAudioTracks()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeCustomFolderUri(uriStr: String) {
        val removedNode = _folderTrees.value.find { it.uri?.toString() == uriStr }
        settingsRepository.removeFolderUri(uriStr)
        if (removedNode != null) {
            val removedTrackIds = removedNode.getAllTracksRecursively().map { it.id }.toSet()
            val currentTrack = _currentTrack.value
            if (currentTrack != null && removedTrackIds.contains(currentTrack.id)) {
                mediaController?.stop()
                mediaController?.clearMediaItems()
                _currentTrack.value = null
                _currentQueue.value = emptyList()
            } else {
                val updatedQueue = _currentQueue.value.filter { !removedTrackIds.contains(it.id) }
                if (updatedQueue.size != _currentQueue.value.size) {
                    prepareQueueOnMain(updatedQueue)
                }
            }
        }
        loadAudioTracks()
    }

    fun moveCustomFolderUriUp(uriStr: String) {
        settingsRepository.moveFolderUriUp(uriStr)
        loadAudioTracks()
    }

    fun moveCustomFolderUriDown(uriStr: String) {
        settingsRepository.moveFolderUriDown(uriStr)
        loadAudioTracks()
    }

    fun playTrackList(tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        prepareQueueOnMain(tracks, startIndex = startIndex, autoPlay = true)
    }

    fun playExternalUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            var title = "Audio Track"
            var artist = "Unknown Artist"
            var album = "External Audio"
            var durationMs = 0L

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) {
                                title = displayName.substringBeforeLast(".")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val metaTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaAlbum = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val metaDuration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaAlbum.isNullOrBlank()) album = metaAlbum
                if (!metaDuration.isNullOrBlank()) {
                    durationMs = metaDuration.toLongOrNull() ?: 0L
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val mimeType = try { context.contentResolver.getType(uri) ?: "audio/*" } catch (_: Exception) { "audio/*" }

            val track = AudioTrack(
                id = System.currentTimeMillis(),
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uri = uri,
                mimeType = mimeType
            )

            withContext(Dispatchers.Main) {
                isStateRestored = true
                if (mediaController != null) {
                    playTrackList(listOf(track), 0)
                } else {
                    pendingExternalTrack = track
                }
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        val currentList = _currentQueue.value
        val existingIndex = currentList.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            val controller = mediaController
            if (controller != null && controller.mediaItemCount > existingIndex) {
                _currentTrack.value = track
                controller.seekTo(existingIndex, 0L)
                controller.play()
                _isPlaying.value = true
            } else {
                prepareQueueOnMain(currentList, startIndex = existingIndex, autoPlay = true)
            }
        } else {
            // Strictly add ONLY this specific track to the playback queue
            val newQueue = currentList + track
            prepareQueueOnMain(newQueue, startIndex = newQueue.lastIndex, autoPlay = true)
        }
    }

    fun togglePlayPause() {
        val player = mediaController
        val currentlyPlaying = _isPlaying.value
        _isPlaying.value = !currentlyPlaying
        if (player != null) {
            if (currentlyPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0, 0L)
                }
                player.play()
            }
        }
    }

    fun playNext() {
        val player = mediaController
        if (player != null && player.mediaItemCount > 0) {
            val nextIndex = (player.currentMediaItemIndex + 1) % player.mediaItemCount
            val nextItem = player.getMediaItemAt(nextIndex)
            val trackId = nextItem.mediaId.toLongOrNull()
            if (trackId != null) {
                (_tracks.value.find { it.id == trackId } ?: _currentQueue.value.find { it.id == trackId })?.let {
                    _currentTrack.value = it
                }
            }
            player.seekToNextMediaItem()
        } else {
            val queue = _currentQueue.value
            if (queue.isNotEmpty()) {
                val currentTrackId = _currentTrack.value?.id
                val currentIndex = queue.indexOfFirst { it.id == currentTrackId }
                val nextIndex = if (currentIndex != -1) (currentIndex + 1) % queue.size else 0
                _currentTrack.value = queue[nextIndex]
                playTrackList(queue, nextIndex)
            }
        }
    }

    fun playPrevious() {
        val player = mediaController
        if (player != null && player.mediaItemCount > 0) {
            val prevIndex = (player.currentMediaItemIndex - 1 + player.mediaItemCount) % player.mediaItemCount
            val prevItem = player.getMediaItemAt(prevIndex)
            val trackId = prevItem.mediaId.toLongOrNull()
            if (trackId != null) {
                (_tracks.value.find { it.id == trackId } ?: _currentQueue.value.find { it.id == trackId })?.let {
                    _currentTrack.value = it
                }
            }
            player.seekToPreviousMediaItem()
        } else {
            val queue = _currentQueue.value
            if (queue.isNotEmpty()) {
                val currentTrackId = _currentTrack.value?.id
                val currentIndex = queue.indexOfFirst { it.id == currentTrackId }
                val prevIndex = if (currentIndex != -1) (currentIndex - 1 + queue.size) % queue.size else queue.size - 1
                _currentTrack.value = queue[prevIndex]
                playTrackList(queue, prevIndex)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController
        val wasPlaying = controller?.playWhenReady == true && controller.playbackState != Player.STATE_ENDED
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
        if (wasPlaying) {
            _isPlaying.value = true
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        mediaController?.playbackParameters = PlaybackParameters(clampedSpeed)
        _playbackSpeed.value = clampedSpeed
    }

    private fun getCurrentSystemVolumeRatio(): Float {
        return try {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) current.toFloat() / max.toFloat() else 0.5f
        } catch (e: Exception) {
            0.5f
        }
    }

    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        mediaController?.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        mediaController?.shuffleModeEnabled = newShuffle
        _isShuffle.value = newShuffle
    }

    fun toggleFavorite(trackId: Long) {
        val current = _favoriteIds.value.toMutableSet()
        val isFav = !current.contains(trackId)
        if (isFav) {
            current.add(trackId)
        } else {
            current.remove(trackId)
        }
        _favoriteIds.value = current

        _currentTrack.value?.let { active ->
            if (active.id == trackId) {
                _currentTrack.value = active.copy(isFavorite = isFav)
            }
        }

        viewModelScope.launch {
            musicRepository.updateFavorite(trackId, isFav)
            val intent = android.content.Intent("com.example.FAVORITE_STATE_CHANGED_EVENT").apply {
                putExtra("TRACK_ID", trackId)
                putExtra("IS_FAVORITE", isFav)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }

    fun setSleepTimer(minutes: Int) {
        countDownTimer?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = 0
            return
        }

        val totalMs = minutes * 60 * 1000L
        _sleepTimerRemainingSec.value = minutes * 60

        countDownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerRemainingSec.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _sleepTimerMinutes.value = 0
                _sleepTimerRemainingSec.value = 0
                mediaController?.pause()
            }
        }.start()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSliderPosition(position: SliderPosition) {
        settingsRepository.setSliderPosition(position)
    }

    fun setVolumeSliderLength(length: Float) {
        settingsRepository.setVolumeSliderLength(length)
    }

    fun setVolumeBarYOffsetDp(offsetDp: Float) {
        settingsRepository.setVolumeBarYOffsetDp(offsetDp)
    }

    fun setVolumeBarXOffsetDp(offsetDp: Float) {
        settingsRepository.setVolumeBarXOffsetDp(offsetDp)
    }

    fun setVolumeBarThicknessDp(thicknessDp: Float) {
        settingsRepository.setVolumeBarThicknessDp(thicknessDp)
    }

    fun setVinylYOffsetDp(offsetDp: Float) {
        settingsRepository.setVinylYOffsetDp(offsetDp)
    }

    fun setBottomControlsYOffsetDp(offsetDp: Float) {
        settingsRepository.setBottomControlsYOffsetDp(offsetDp)
    }

    fun setProgressBarLength(length: Float) {
        settingsRepository.setProgressBarLength(length)
    }

    fun setSpeedSliderLength(length: Float) {
        settingsRepository.setSpeedSliderLength(length)
    }

    fun setAccentColorOption(option: com.example.model.AccentColorOption) {
        settingsRepository.setAccentColorOption(option)
    }

    fun setColoredSpeedSlider(enabled: Boolean) {
        settingsRepository.setColoredSpeedSlider(enabled)
    }

    fun setHideHeaderOnScroll(enabled: Boolean) {
        settingsRepository.setHideHeaderOnScroll(enabled)
    }

    fun setMusicVisualTheme(theme: MusicVisualTheme) {
        settingsRepository.setMusicVisualTheme(theme)
    }

    fun sortQueueTracks(sortOption: com.example.model.SortOption) {
        val current = _currentQueue.value
        val sorted = when (sortOption) {
            com.example.model.SortOption.TITLE_ASC -> current.sortedBy { it.title.lowercase() }
            com.example.model.SortOption.TITLE_DESC -> current.sortedByDescending { it.title.lowercase() }
            com.example.model.SortOption.REVERSE -> current.reversed()
        }
        _currentQueue.value = sorted
        prepareQueueOnMain(sorted)
    }

    fun sortFolderTracks(folderId: String, sortOption: com.example.model.SortOption) {
        fun sortNodeList(nodes: List<FolderNode>): List<FolderNode> {
            return nodes.map { node ->
                if (node.id == folderId) {
                    val sorted = when (sortOption) {
                        com.example.model.SortOption.TITLE_ASC -> node.tracks.sortedBy { it.title.lowercase() }
                        com.example.model.SortOption.TITLE_DESC -> node.tracks.sortedByDescending { it.title.lowercase() }
                        com.example.model.SortOption.REVERSE -> node.tracks.reversed()
                    }
                    node.copy(tracks = sorted)
                } else if (node.subfolders.isNotEmpty()) {
                    node.copy(subfolders = sortNodeList(node.subfolders))
                } else {
                    node
                }
            }
        }
        _folderTrees.value = sortNodeList(_folderTrees.value)
    }

    fun moveFolderTrack(folderId: String, fromIndex: Int, toIndex: Int) {
        fun moveInNodeList(nodes: List<FolderNode>): List<FolderNode> {
            return nodes.map { node ->
                if (node.id == folderId) {
                    val list = node.tracks.toMutableList()
                    if (fromIndex in list.indices && toIndex in list.indices) {
                        val item = list.removeAt(fromIndex)
                        list.add(toIndex, item)
                    }
                    node.copy(tracks = list)
                } else if (node.subfolders.isNotEmpty()) {
                    node.copy(subfolders = moveInNodeList(node.subfolders))
                } else {
                    node
                }
            }
        }
        _folderTrees.value = moveInNodeList(_folderTrees.value)
    }

    fun deleteFolder(context: Context, folderNode: FolderNode, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            folderNode.uri?.toString()?.let { uriStr ->
                settingsRepository.removeFolderUri(uriStr)
            }
            musicRepository.deleteFolderById(folderNode.id)
            withContext(Dispatchers.Main) {
                _folderTrees.value = _folderTrees.value.filter { it.id != folderNode.id }
                onComplete()
            }
        }
    }

    fun deleteTracks(context: Context, tracksToDelete: List<AudioTrack>, onComplete: () -> Unit = {}) {
        if (tracksToDelete.isEmpty()) return
        viewModelScope.launch(Dispatchers.Main) {
            val targetIds = tracksToDelete.map { it.id }.toSet()
            val currentTrack = _currentTrack.value
            val updatedQueue = _currentQueue.value.filter { !targetIds.contains(it.id) }

            _currentQueue.value = updatedQueue

            if (currentTrack != null && targetIds.contains(currentTrack.id)) {
                if (updatedQueue.isNotEmpty()) {
                    playTrack(updatedQueue.first())
                } else {
                    mediaController?.stop()
                    mediaController?.clearMediaItems()
                    _currentTrack.value = null
                    _isPlaying.value = false
                }
            } else if (updatedQueue.isNotEmpty()) {
                prepareQueueOnMain(updatedQueue)
            }

            saveCurrentPlaybackState()
            android.widget.Toast.makeText(context, "Deleted ${tracksToDelete.size} song(s) from queue", android.widget.Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    fun moveTracks(context: Context, tracksToMove: List<AudioTrack>, destTreeUri: Uri, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val destDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, destTreeUri)
            if (destDoc != null) {
                for (track in tracksToMove) {
                    try {
                        val fileName = "${track.title}.mp3"
                        val mime = track.mimeType.ifEmpty { "audio/*" }
                        val newFile = destDoc.createFile(mime, fileName)
                        if (newFile != null) {
                            context.contentResolver.openInputStream(track.uri)?.use { input ->
                                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            loadAudioTracks()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun startProgressUpdateLoop() {
        stopProgressUpdateLoop()
        var tickCount = 0
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
                    _duration.value = controller.duration.coerceAtLeast(0L)
                }
                tickCount++
                if (tickCount % 6 == 0) {
                    saveCurrentPlaybackState()
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdateLoop() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    override fun onCleared() {
        try {
            saveCurrentPlaybackState()
            context.unregisterReceiver(broadcastReceiver)
            context.contentResolver.unregisterContentObserver(volumeObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        countDownTimer?.cancel()
        stopProgressUpdateLoop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }
}

@Immutable
data class DeletionProgress(
    val current: Int = 0,
    val total: Int = 0,
    val isDeleting: Boolean = false
)
