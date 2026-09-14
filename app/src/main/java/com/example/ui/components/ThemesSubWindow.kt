package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AccentColorOption
import com.example.model.DarkModeOption
import com.example.model.MusicVisualTheme
import com.example.model.AudioTrack
import com.example.ui.theme.getAccentColor
import com.example.viewmodel.PlayerViewModel

@Composable
fun ThemesSubWindow(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept back button press to dismiss sub-window and return to main Settings
    BackHandler {
        onDismiss()
    }

    val userSettings by viewModel.userSettings.collectAsState()
    val scrollState = rememberScrollState()

    // Smooth capsule scroll connection: hides the dock and capsules when scrolling down
    val nestedScrollConnection = rememberCapsuleScrollConnection(
        enabled = true,
        onVisibilityChanged = { visible ->
            viewModel.setScrollHeaderDockVisible(visible)
        }
    )

    // Automatically hide floating capsules when scrolling through themes or reaching the speed slider
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }.collect { scrollOffset ->
            if (scrollOffset > 30) {
                viewModel.setScrollHeaderDockVisible(false)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
            .testTag("themes_sub_window"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("themes_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))

            Column {
                Text(
                    text = "Themes & Colors",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Theme mode, botanical color palettes & accent options.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. App Theme Mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dark_mode_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "App Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DarkModeOption.values().forEach { option ->
                    val isSelected = userSettings.darkModeOption == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.settingsRepository.setDarkModeOption(option) }
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.settingsRepository.setDarkModeOption(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                DarkModeOption.SYSTEM -> "System Default"
                                DarkModeOption.DARK -> "Dark Mode"
                                DarkModeOption.LIGHT -> "Light Mode"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Accent Color Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Botanical & Accent Palette",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspired by nature's herbs, spices, roots, earth, and deep night shades.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val isDark = when (userSettings.darkModeOption) {
                    DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                    DarkModeOption.DARK -> true
                    DarkModeOption.LIGHT -> false
                }

                val categories = listOf(
                    "Herbs" to "🌿 Herbs (Muted Greens)",
                    "Spices" to "🌶️ Warm Spices (Earthy Reds & Yellows)",
                    "Roots & Earth" to "🏜️ Roots & Earth (Browns & Sands)",
                    "Deep Shades" to "🌑 Deep Shades (Night Accents)",
                    "Vibrant" to "🎨 Vibrant Classics"
                )

                categories.forEach { (catKey, catTitle) ->
                    val optionsInCategory = AccentColorOption.values().filter { it.category == catKey }
                    if (optionsInCategory.isNotEmpty()) {
                        Text(
                            text = catTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(optionsInCategory) { option ->
                                val isSelected = userSettings.accentColorOption == option
                                val previewColor = getAccentColor(option, isDark)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.setAccentColorOption(option) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("color_option_${option.name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(previewColor)
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = CircleShape
                                                ) else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Music Visualizer & Art Style (3 Distinct Styles)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("music_visual_theme_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Music Visualizer & Art Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select the digital art style applied to tracks without custom album art, in Now Playing, notifications, floating capsules, and player icons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val dummyTrack = remember {
                    AudioTrack(
                        id = -1L,
                        title = "Visual Theme Preview",
                        artist = "Audio Engine",
                        album = "Soundscapes",
                        durationMs = 240000L,
                        uri = android.net.Uri.EMPTY,
                        albumArtUri = null,
                        albumArtDrawableRes = null,
                        filePath = ""
                    )
                }

                val themes = listOf(
                    Triple(
                        MusicVisualTheme.VINYL,
                        "Classic Vinyl Record",
                        "Spinning retro vinyl groove disc with concentric textures, tonearm, and vintage center label."
                    ),
                    Triple(
                        MusicVisualTheme.CYBER_PULSE,
                        "Cyber Pulse Orb",
                        "Futuristic glowing neon quantum core with rotating cybernetic arcs and radiating audio pulse waves."
                    ),
                    Triple(
                        MusicVisualTheme.SONIC_RADAR,
                        "Sonic Radar & Oscilloscope",
                        "Tactical phosphor green acoustic radar sweep with real-time audio oscilloscope waveform."
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    themes.forEach { (themeOption, title, description) ->
                        val isSelected = userSettings.musicVisualTheme == themeOption

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.setMusicVisualTheme(themeOption) }
                                .testTag("theme_option_${themeOption.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live Visual Canvas Preview
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0D1117)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MusicVisualizerArtCanvas(
                                        track = dummyTrack,
                                        isPlaying = true,
                                        theme = themeOption,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setMusicVisualTheme(themeOption) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("theme_radio_${themeOption.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Colored Speed Slider Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("colored_speed_slider_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Colored Speed Slider",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Use primary accent color for playback speed slider instead of neutral theme color.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = userSettings.coloredSpeedSlider,
                    onCheckedChange = { viewModel.setColoredSpeedSlider(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("colored_speed_slider_switch")
                )
            }
        }

        // Generous bottom spacer so the user can easily scroll all options well above any bottom bars or capsules
        Spacer(modifier = Modifier.height(180.dp))
    }
}
