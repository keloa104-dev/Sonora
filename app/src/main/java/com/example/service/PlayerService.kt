package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

import com.example.audio.AudioEffectsManager
import com.example.data.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private val serviceReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.FAVORITE_STATE_CHANGED_EVENT") {
                val trackId = intent.getLongExtra("TRACK_ID", -1L)
                val isFav = intent.getBooleanExtra("IS_FAVORITE", false)
                val currentTrackId = exoPlayer?.currentMediaItem?.mediaId?.toLongOrNull()
                if (currentTrackId == trackId) {
                    updateFavoriteNotificationButton(isFav)
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        try {
            val filter = android.content.IntentFilter("com.example.FAVORITE_STATE_CHANGED_EVENT")
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                serviceReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()

        exoPlayer = player

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEffectsManager.init(audioSessionId)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                error.printStackTrace()
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val trackId = mediaItem?.mediaId?.toLongOrNull()
                if (trackId != null) {
                    serviceScope.launch(Dispatchers.IO) {
                        val repository = MusicRepository(applicationContext)
                        val isFav = repository.isFavorite(trackId)
                        withContext(Dispatchers.Main) {
                            updateFavoriteNotificationButton(isFav)
                        }
                    }
                }
            }
        })

        if (player.audioSessionId > 0) {
            AudioEffectsManager.init(player.audioSessionId)
        }

        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                if (command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_NEXT ||
                    command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_PLAY_PAUSE
                ) {
                    return true
                }
                return super.isCommandAvailable(command)
            }

            override fun hasNextMediaItem(): Boolean {
                return player.mediaItemCount > 0
            }

            override fun hasPreviousMediaItem(): Boolean {
                return player.mediaItemCount > 0
            }

            override fun seekToNext() {
                seekToNextMediaItem()
            }

            override fun seekToNextMediaItem() {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else if (player.mediaItemCount > 0) {
                    val nextIndex = (player.currentMediaItemIndex + 1) % player.mediaItemCount
                    player.seekTo(nextIndex, 0L)
                }
                player.play()
            }

            override fun seekToPrevious() {
                seekToPreviousMediaItem()
            }

            override fun seekToPreviousMediaItem() {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else if (player.mediaItemCount > 0) {
                    val prevIndex = (player.currentMediaItemIndex - 1 + player.mediaItemCount) % player.mediaItemCount
                    player.seekTo(prevIndex, 0L)
                }
                player.play()
            }
        }

        val intent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val favoriteCommand = SessionCommand("ACTION_TOGGLE_FAVORITE", Bundle.EMPTY)
        val favoriteButton = CommandButton.Builder()
            .setDisplayName("Favorite")
            .setIconResId(com.example.R.drawable.ic_notification_fav)
            .setSessionCommand(favoriteCommand)
            .setEnabled(true)
            .build()

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setId("MusicPlayerMediaSession")
            .setSessionActivity(pendingIntent)
            .setCustomLayout(listOf(favoriteButton))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(favoriteCommand)
                        .build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                        .buildUpon()
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .setCustomLayout(listOf(favoriteButton))
                        .build()
                }

                @Suppress("WrongConstant")
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == "ACTION_TOGGLE_FAVORITE") {
                        val currentItem = exoPlayer?.currentMediaItem
                        val trackId = currentItem?.mediaId?.toLongOrNull()
                        if (trackId != null) {
                            serviceScope.launch(Dispatchers.IO) {
                                val repository = MusicRepository(applicationContext)
                                repository.toggleFavorite(trackId)
                                val isFav = repository.isFavorite(trackId)
                                withContext(Dispatchers.Main) {
                                    updateFavoriteNotificationButton(isFav)
                                    val intent = Intent("com.example.ACTION_TOGGLE_FAVORITE_EVENT").apply {
                                        putExtra("TRACK_ID", trackId)
                                        putExtra("IS_FAVORITE", isFav)
                                        setPackage(packageName)
                                    }
                                    sendBroadcast(intent)
                                }
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            })
            .build()

        val notificationProvider = CustomMediaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
    }

    private fun updateFavoriteNotificationButton(isFav: Boolean) {
        val favoriteCommand = SessionCommand("ACTION_TOGGLE_FAVORITE", Bundle.EMPTY)
        val iconRes = if (isFav) com.example.R.drawable.ic_notification_fav_filled else com.example.R.drawable.ic_notification_fav_outline
        val favoriteButton = CommandButton.Builder()
            .setDisplayName(if (isFav) "Unstar" else "Star")
            .setIconResId(iconRes)
            .setSessionCommand(favoriteCommand)
            .setEnabled(true)
            .build()
        mediaSession?.setCustomLayout(listOf(favoriteButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    private class CustomMediaNotificationProvider(
        private val context: Context,
        private val defaultProvider: DefaultMediaNotificationProvider = DefaultMediaNotificationProvider(context).apply {
            setSmallIcon(com.example.R.drawable.ic_notification_music)
        }
    ) : MediaNotification.Provider {

        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            callback: MediaNotification.Provider.Callback
        ): MediaNotification {
            val mediaNotification = defaultProvider.createNotification(
                mediaSession,
                customLayout,
                actionFactory,
                callback
            )
            val origNotif = mediaNotification.notification
            val settingsRepo = com.example.data.SettingsRepository(context)
            val activeTheme = settingsRepo.settings.value.musicVisualTheme

            val smallIconRes = when (activeTheme) {
                com.example.model.MusicVisualTheme.VINYL -> com.example.R.drawable.ic_notification_vinyl
                com.example.model.MusicVisualTheme.CYBER_PULSE -> com.example.R.drawable.ic_notification_cyber
                com.example.model.MusicVisualTheme.SONIC_RADAR -> com.example.R.drawable.ic_notification_radar
            }

            val builder = NotificationCompat.Builder(context, origNotif)
                .setSmallIcon(smallIconRes)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)

            if (origNotif.getLargeIcon() == null) {
                try {
                    val currentTrackItem = mediaSession.player.currentMediaItem
                    val themeBitmap = com.example.util.MusicVisualArtworkGenerator.generateThemeBitmap(
                        context,
                        track = null,
                        theme = activeTheme,
                        size = 512
                    )
                    if (themeBitmap != null) {
                        builder.setLargeIcon(themeBitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val updatedNotif = builder.build()

            return MediaNotification(mediaNotification.notificationId, updatedNotif)
        }

        override fun handleCustomCommand(
            session: MediaSession,
            action: String,
            extras: Bundle
        ): Boolean {
            return defaultProvider.handleCustomCommand(session, action, extras)
        }
    }
}


