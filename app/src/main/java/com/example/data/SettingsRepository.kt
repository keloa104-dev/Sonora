package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AccentColorOption
import com.example.model.DarkModeOption
import com.example.model.MusicVisualTheme
import com.example.model.SliderPosition
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedPlaybackState(
    val lastTrackUri: String = "",
    val trackIndex: Int = 0,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val queueUris: List<String> = emptyList(),
    val volume: Float = 1.0f
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("music_player_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val sliderPosStr = prefs.getString(KEY_SLIDER_POS, SliderPosition.RIGHT.name)
        val sliderPos = try {
            SliderPosition.valueOf(sliderPosStr ?: SliderPosition.RIGHT.name)
        } catch (e: Exception) {
            SliderPosition.RIGHT
        }

        val darkModeStr = prefs.getString(KEY_DARK_MODE, DarkModeOption.DARK.name)
        val darkMode = try {
            DarkModeOption.valueOf(darkModeStr ?: DarkModeOption.DARK.name)
        } catch (e: Exception) {
            DarkModeOption.DARK
        }

        val accentColorStr = prefs.getString(KEY_ACCENT_COLOR, AccentColorOption.PURPLE.name)
        val accentColor = try {
            AccentColorOption.valueOf(accentColorStr ?: AccentColorOption.PURPLE.name)
        } catch (e: Exception) {
            AccentColorOption.PURPLE
        }

        val folderUrisStr = prefs.getString(KEY_FOLDER_URIS_LIST, null)
        val folderUrisList = if (!folderUrisStr.isNullOrEmpty()) {
            folderUrisStr.split("|||").filter { it.isNotBlank() }
        } else {
            val legacySet = prefs.getStringSet(KEY_FOLDER_URIS, emptySet()) ?: emptySet()
            legacySet.toList()
        }

        val sliderLength = prefs.getFloat(KEY_SLIDER_LENGTH, 0.8f)
        val coloredSpeedSlider = prefs.getBoolean(KEY_COLORED_SPEED_SLIDER, false)
        val progressBarLength = prefs.getFloat(KEY_PROGRESS_BAR_LENGTH, 1.0f)
        val speedSliderLength = prefs.getFloat(KEY_SPEED_SLIDER_LENGTH, 1.0f)
        val volumeBarYOffsetDp = prefs.getFloat(KEY_VOLUME_BAR_Y_OFFSET_DP, 0f)
        val volumeBarXOffsetDp = prefs.getFloat(KEY_VOLUME_BAR_X_OFFSET_DP, 0f)
        val volumeBarThicknessDp = prefs.getFloat(KEY_VOLUME_BAR_THICKNESS_DP, 6f)
        val vinylYOffsetDp = prefs.getFloat(KEY_VINYL_Y_OFFSET_DP, 0f)
        val bottomControlsYOffsetDp = prefs.getFloat(KEY_BOTTOM_CONTROLS_Y_OFFSET_DP, 0f)
        val hideHeaderOnScroll = prefs.getBoolean(KEY_HIDE_HEADER_ON_SCROLL, true)
        val autoScanDevice = prefs.getBoolean(KEY_AUTO_SCAN_DEVICE, false)
        val visualThemeStr = prefs.getString(KEY_MUSIC_VISUAL_THEME, MusicVisualTheme.VINYL.name)
        val visualTheme = try {
            MusicVisualTheme.valueOf(visualThemeStr ?: MusicVisualTheme.VINYL.name)
        } catch (_: Exception) {
            MusicVisualTheme.VINYL
        }

        return UserSettings(
            sliderPosition = sliderPos,
            darkModeOption = darkMode,
            accentColorOption = accentColor,
            customFolderUris = folderUrisList,
            volumeSliderLength = sliderLength,
            coloredSpeedSlider = coloredSpeedSlider,
            progressBarLength = progressBarLength,
            speedSliderLength = speedSliderLength,
            volumeBarYOffsetDp = volumeBarYOffsetDp,
            volumeBarXOffsetDp = volumeBarXOffsetDp,
            volumeBarThicknessDp = volumeBarThicknessDp,
            vinylYOffsetDp = vinylYOffsetDp,
            bottomControlsYOffsetDp = bottomControlsYOffsetDp,
            hideHeaderOnScroll = hideHeaderOnScroll,
            autoScanDevice = autoScanDevice,
            musicVisualTheme = visualTheme
        )
    }

    fun setAutoScanDevice(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCAN_DEVICE, enabled).apply()
        _settings.value = _settings.value.copy(autoScanDevice = enabled)
    }

    fun setHideHeaderOnScroll(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_HEADER_ON_SCROLL, enabled).apply()
        _settings.value = _settings.value.copy(hideHeaderOnScroll = enabled)
    }

    fun setColoredSpeedSlider(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COLORED_SPEED_SLIDER, enabled).apply()
        _settings.value = _settings.value.copy(coloredSpeedSlider = enabled)
    }

    fun setVolumeSliderLength(length: Float) {
        val clamped = length.coerceIn(0.3f, 1.0f)
        prefs.edit().putFloat(KEY_SLIDER_LENGTH, clamped).apply()
        _settings.value = _settings.value.copy(volumeSliderLength = clamped)
    }

    fun setProgressBarLength(length: Float) {
        val clamped = length.coerceIn(0.3f, 1.0f)
        prefs.edit().putFloat(KEY_PROGRESS_BAR_LENGTH, clamped).apply()
        _settings.value = _settings.value.copy(progressBarLength = clamped)
    }

    fun setSpeedSliderLength(length: Float) {
        val clamped = length.coerceIn(0.3f, 1.0f)
        prefs.edit().putFloat(KEY_SPEED_SLIDER_LENGTH, clamped).apply()
        _settings.value = _settings.value.copy(speedSliderLength = clamped)
    }

    fun setVolumeBarYOffsetDp(offsetDp: Float) {
        val clamped = offsetDp.coerceIn(-250f, 250f)
        prefs.edit().putFloat(KEY_VOLUME_BAR_Y_OFFSET_DP, clamped).apply()
        _settings.value = _settings.value.copy(volumeBarYOffsetDp = clamped)
    }

    fun setVolumeBarXOffsetDp(offsetDp: Float) {
        val clamped = offsetDp.coerceIn(-150f, 150f)
        prefs.edit().putFloat(KEY_VOLUME_BAR_X_OFFSET_DP, clamped).apply()
        _settings.value = _settings.value.copy(volumeBarXOffsetDp = clamped)
    }

    fun setVolumeBarThicknessDp(thicknessDp: Float) {
        val clamped = thicknessDp.coerceIn(2f, 24f)
        prefs.edit().putFloat(KEY_VOLUME_BAR_THICKNESS_DP, clamped).apply()
        _settings.value = _settings.value.copy(volumeBarThicknessDp = clamped)
    }

    fun setVinylYOffsetDp(offsetDp: Float) {
        val clamped = offsetDp.coerceIn(-250f, 250f)
        prefs.edit().putFloat(KEY_VINYL_Y_OFFSET_DP, clamped).apply()
        _settings.value = _settings.value.copy(vinylYOffsetDp = clamped)
    }

    fun setBottomControlsYOffsetDp(offsetDp: Float) {
        val clamped = offsetDp.coerceIn(-250f, 250f)
        prefs.edit().putFloat(KEY_BOTTOM_CONTROLS_Y_OFFSET_DP, clamped).apply()
        _settings.value = _settings.value.copy(bottomControlsYOffsetDp = clamped)
    }

    private fun saveFolderList(list: List<String>) {
        val joined = list.joinToString("|||")
        prefs.edit().putString(KEY_FOLDER_URIS_LIST, joined).apply()
        _settings.value = _settings.value.copy(customFolderUris = list)
    }

    fun setSliderPosition(position: SliderPosition) {
        prefs.edit().putString(KEY_SLIDER_POS, position.name).apply()
        _settings.value = _settings.value.copy(sliderPosition = position)
    }

    fun setDarkModeOption(option: DarkModeOption) {
        prefs.edit().putString(KEY_DARK_MODE, option.name).apply()
        _settings.value = _settings.value.copy(darkModeOption = option)
    }

    fun setAccentColorOption(option: AccentColorOption) {
        prefs.edit().putString(KEY_ACCENT_COLOR, option.name).apply()
        _settings.value = _settings.value.copy(accentColorOption = option)
    }

    fun setMusicVisualTheme(theme: MusicVisualTheme) {
        prefs.edit().putString(KEY_MUSIC_VISUAL_THEME, theme.name).apply()
        _settings.value = _settings.value.copy(musicVisualTheme = theme)
    }

    fun addFolderUri(uriString: String) {
        val current = _settings.value.customFolderUris.toMutableList()
        if (!current.contains(uriString)) {
            current.add(uriString)
            saveFolderList(current)
        }
    }

    fun removeFolderUri(uriString: String) {
        val current = _settings.value.customFolderUris.toMutableList()
        if (current.remove(uriString)) {
            saveFolderList(current)
        }
    }

    fun moveFolderUriUp(uriString: String) {
        val current = _settings.value.customFolderUris.toMutableList()
        val index = current.indexOf(uriString)
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            saveFolderList(current)
        }
    }

    fun moveFolderUriDown(uriString: String) {
        val current = _settings.value.customFolderUris.toMutableList()
        val index = current.indexOf(uriString)
        if (index >= 0 && index < current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            saveFolderList(current)
        }
    }

    fun savePlaybackState(
        trackUri: String,
        trackIndex: Int,
        positionMs: Long,
        isPlaying: Boolean,
        queueUris: List<String>,
        volume: Float = 1.0f
    ) {
        prefs.edit()
            .putString(KEY_LAST_TRACK_URI, trackUri)
            .putInt(KEY_LAST_TRACK_INDEX, trackIndex)
            .putLong(KEY_LAST_POSITION_MS, positionMs)
            .putBoolean(KEY_LAST_IS_PLAYING, isPlaying)
            .putString(KEY_LAST_QUEUE_URIS, queueUris.joinToString("|||"))
            .putFloat(KEY_LAST_VOLUME, volume)
            .apply()
    }

    fun loadPlaybackState(): SavedPlaybackState {
        var trackUri = prefs.getString(KEY_LAST_TRACK_URI, "") ?: ""
        if (trackUri.contains("exoplayer-test-media")) {
            trackUri = ""
        }
        val trackIndex = prefs.getInt(KEY_LAST_TRACK_INDEX, 0)
        val pos = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
        val playing = prefs.getBoolean(KEY_LAST_IS_PLAYING, false)
        val queueStr = prefs.getString(KEY_LAST_QUEUE_URIS, "") ?: ""
        val queueUris = if (queueStr.isNotBlank()) {
            queueStr.split("|||").filter { it.isNotBlank() && !it.contains("exoplayer-test-media") }
        } else emptyList()
        val volume = prefs.getFloat(KEY_LAST_VOLUME, 1.0f)
        return SavedPlaybackState(trackUri, trackIndex, pos, playing, queueUris, volume)
    }

    companion object {
        private const val KEY_SLIDER_POS = "key_slider_position"
        private const val KEY_SLIDER_LENGTH = "key_slider_length"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_ACCENT_COLOR = "key_accent_color"
        private const val KEY_FOLDER_URIS = "key_folder_uris"
        private const val KEY_FOLDER_URIS_LIST = "key_folder_uris_list_ordered"
        private const val KEY_LAST_TRACK_URI = "key_last_track_uri"
        private const val KEY_LAST_TRACK_INDEX = "key_last_track_index"
        private const val KEY_LAST_POSITION_MS = "key_last_position_ms"
        private const val KEY_LAST_IS_PLAYING = "key_last_is_playing"
        private const val KEY_LAST_QUEUE_URIS = "key_last_queue_uris"
        private const val KEY_LAST_VOLUME = "key_last_volume"
        private const val KEY_COLORED_SPEED_SLIDER = "colored_speed_slider"
        private const val KEY_PROGRESS_BAR_LENGTH = "key_progress_bar_length"
        private const val KEY_SPEED_SLIDER_LENGTH = "key_speed_slider_length"
        private const val KEY_VOLUME_BAR_Y_OFFSET_DP = "key_volume_bar_y_offset_dp"
        private const val KEY_VOLUME_BAR_X_OFFSET_DP = "key_volume_bar_x_offset_dp"
        private const val KEY_VOLUME_BAR_THICKNESS_DP = "key_volume_bar_thickness_dp"
        private const val KEY_VINYL_Y_OFFSET_DP = "key_vinyl_y_offset_dp"
        private const val KEY_BOTTOM_CONTROLS_Y_OFFSET_DP = "key_bottom_controls_y_offset_dp"
        private const val KEY_HIDE_HEADER_ON_SCROLL = "pref_hide_header_on_scroll"
        private const val KEY_AUTO_SCAN_DEVICE = "pref_auto_scan_device"
        private const val KEY_MUSIC_VISUAL_THEME = "pref_music_visual_theme"
    }
}

