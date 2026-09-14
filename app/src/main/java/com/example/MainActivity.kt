package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ui.components.CheckAndRequestPermissions
import com.example.ui.components.DockTab
import com.example.ui.components.DualTierFloatingDock
import com.example.ui.screens.DedicatedSongsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SongsScreen
import com.example.ui.theme.MusicPlayerTheme
import com.example.viewmodel.PlayerViewModel

import android.view.KeyEvent

class MainActivity : ComponentActivity() {
    private var playerViewModel: PlayerViewModel? = null
    private var pendingIntentUri: android.net.Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        enableEdgeToEdge()

        // Configure high-refresh rate (120Hz) for flagship performance (e.g. Dimensity 9300+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val params = window.attributes
                params.preferredRefreshRate = 120.0f
                val modes = display?.supportedModes
                val highRateMode = modes?.filter { it.refreshRate >= 119.0f }
                    ?.maxByOrNull { it.refreshRate }
                if (highRateMode != null) {
                    params.preferredDisplayModeId = highRateMode.modeId
                }
                window.attributes = params
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Failed to configure 120Hz mode", e)
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val params = window.attributes
                params.preferredRefreshRate = 120.0f
                window.attributes = params
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Failed to set preferredRefreshRate", e)
            }
        }

        // Layer 1/2 Pre-cache at startup: High-priority async load from Room DB
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.db.AppDatabase.getInstance(applicationContext)
                db.trackDao().deletePreinstalledTracks()
                val tracks = db.trackDao().getAllTracks().map { it.toAudioTrack() }
                if (tracks.isNotEmpty()) {
                    com.example.data.SongCache.allSongs = tracks
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        handleIntent(intent)
        setContent {
            val viewModel: PlayerViewModel = viewModel()
            playerViewModel = viewModel
            pendingIntentUri?.let { uri ->
                viewModel.playExternalUri(uri)
                pendingIntentUri = null
            }
            val userSettings by viewModel.userSettings.collectAsState()

            MusicPlayerTheme(
                darkModeOption = userSettings.darkModeOption,
                accentColorOption = userSettings.accentColorOption
            ) {
                CheckAndRequestPermissions {
                    viewModel.loadAudioTracks()
                }

                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                if (playerViewModel != null) {
                    playerViewModel?.playExternalUri(uri)
                } else {
                    pendingIntentUri = uri
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
            keyCode == KeyEvent.KEYCODE_MUTE) {
            val handled = super.onKeyDown(keyCode, event)
            playerViewModel?.syncVolumeFromSystem(isHardwareKeyPress = true, keyCode = keyCode)
            return handled
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
            keyCode == KeyEvent.KEYCODE_MUTE) {
            val handled = super.onKeyUp(keyCode, event)
            playerViewModel?.syncVolumeFromSystem(isHardwareKeyPress = true, keyCode = keyCode)
            return handled
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun MainAppContent(viewModel: PlayerViewModel) {
    var selectedTab by remember { mutableStateOf(DockTab.HOME) }
    val tabBackStack = remember { mutableStateListOf<DockTab>() }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var dedicatedSubScreen by remember { mutableStateOf<String?>(null) }

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isScrollHeaderDockVisible by viewModel.isScrollHeaderDockVisible.collectAsState()

    val isDockVisible = !isNowPlayingExpanded && (!userSettings.hideHeaderOnScroll || isScrollHeaderDockVisible)

    val navigateToTab: (DockTab) -> Unit = { tab ->
        if (selectedTab != tab) {
            tabBackStack.add(selectedTab)
            selectedTab = tab
        }
        isSettingsOpen = false
        dedicatedSubScreen = null
        viewModel.setScrollHeaderDockVisible(true)
    }

    // Modern, Robust Back Navigation Stack Hierarchy
    if (isNowPlayingExpanded) {
        BackHandler { isNowPlayingExpanded = false }
    } else if (isSettingsOpen) {
        BackHandler { isSettingsOpen = false }
    } else if (dedicatedSubScreen != null) {
        BackHandler { dedicatedSubScreen = null }
    } else if (tabBackStack.isNotEmpty()) {
        BackHandler {
            val previousTab = tabBackStack.removeAt(tabBackStack.lastIndex)
            selectedTab = previousTab
        }
    } else if (selectedTab != DockTab.HOME) {
        BackHandler {
            selectedTab = DockTab.HOME
        }
    }

    // Android 17 Predictive Spatial Inward Depression Animations - Weightless Spring Synergy
    val spatialDepressionSpring = spring<Float>(
        stiffness = Spring.StiffnessHigh,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val bgScale by animateFloatAsState(
        targetValue = if (isNowPlayingExpanded) 0.93f else 1.0f,
        animationSpec = spatialDepressionSpring,
        label = "bgScale"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (isNowPlayingExpanded) 0.75f else 1.0f,
        animationSpec = spatialDepressionSpring,
        label = "bgAlpha"
    )

    val bgCornerRadius by animateDpAsState(
        targetValue = if (isNowPlayingExpanded) 28.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "bgCorner"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // BACKGROUND SCREEN CONTAINER WITH GPU-OFFLOADED SPATIAL DEPTH DEPRESSION
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                    alpha = bgAlpha
                    shadowElevation = if (isNowPlayingExpanded) 16f else 0f
                    clip = bgCornerRadius > 0.dp
                    shape = RoundedCornerShape(bgCornerRadius)
                }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val navigationState by remember(isSettingsOpen, dedicatedSubScreen, selectedTab) {
                        derivedStateOf {
                            when {
                                isSettingsOpen -> "SETTINGS"
                                dedicatedSubScreen != null -> dedicatedSubScreen!!
                                else -> selectedTab.name
                            }
                        }
                    }

                    fun getNavIndex(state: String): Int {
                        return when (state) {
                            DockTab.HOME.name -> 0
                            DockTab.SONGS.name -> 1
                            "FAVORITES" -> 2
                            "ALL_SONGS" -> 3
                            DockTab.LIBRARY.name -> 4
                            "SETTINGS" -> 5
                            else -> 0
                        }
                    }

                    val spatialNavSpring = spring<Float>(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )

                    val spatialOffsetSpring = spring<IntOffset>(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )

                    AnimatedContent(
                        targetState = navigationState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            },
                        transitionSpec = {
                            val initialIndex = getNavIndex(initialState)
                            val targetIndex = getNavIndex(targetState)
                            val isForward = targetIndex > initialIndex

                            if (isForward) {
                                (slideInHorizontally(
                                    initialOffsetX = { width -> (width * 0.16f).toInt() },
                                    animationSpec = spatialOffsetSpring
                                ) + scaleIn(
                                    initialScale = 0.95f,
                                    animationSpec = spatialNavSpring
                                ) + fadeIn(animationSpec = spatialNavSpring))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { width -> (-width * 0.16f).toInt() },
                                            animationSpec = spatialOffsetSpring
                                        ) + scaleOut(
                                            targetScale = 0.95f,
                                            animationSpec = spatialNavSpring
                                        ) + fadeOut(animationSpec = spatialNavSpring)
                                    )
                            } else {
                                (slideInHorizontally(
                                    initialOffsetX = { width -> (-width * 0.16f).toInt() },
                                    animationSpec = spatialOffsetSpring
                                ) + scaleIn(
                                    initialScale = 0.95f,
                                    animationSpec = spatialNavSpring
                                ) + fadeIn(animationSpec = spatialNavSpring))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { width -> (width * 0.16f).toInt() },
                                            animationSpec = spatialOffsetSpring
                                        ) + scaleOut(
                                            targetScale = 0.95f,
                                            animationSpec = spatialNavSpring
                                        ) + fadeOut(animationSpec = spatialNavSpring)
                                    )
                            }
                        },
                        label = "screen_android17_spatial_transition"
                    ) { target ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Auto
                                }
                        ) {
                            when (target) {
                                "SETTINGS" -> SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { isSettingsOpen = false }
                                )
                                "FAVORITES" -> {
                                    val tracks by viewModel.tracks.collectAsState()
                                    val favoriteIds by viewModel.favoriteIds.collectAsState()
                                    val favoriteTracks = remember(tracks, favoriteIds) {
                                        tracks.filter { favoriteIds.contains(it.id) }
                                    }
                                    DedicatedSongsScreen(
                                        title = "Favorite Songs",
                                        subtitle = "Exclusively Favorited Tracks",
                                        tracks = favoriteTracks,
                                        viewModel = viewModel,
                                        onBack = { dedicatedSubScreen = null },
                                        onNavigateToSettings = { isSettingsOpen = true },
                                        testTagPrefix = "favorite_songs"
                                    )
                                }
                                "ALL_SONGS" -> {
                                    val tracks by viewModel.tracks.collectAsState()
                                    DedicatedSongsScreen(
                                        title = "All Songs",
                                        subtitle = "All Library Music Tracks",
                                        tracks = tracks,
                                        viewModel = viewModel,
                                        onBack = { dedicatedSubScreen = null },
                                        onNavigateToSettings = { isSettingsOpen = true },
                                        testTagPrefix = "all_songs"
                                    )
                                }
                                DockTab.HOME.name -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { isSettingsOpen = true },
                                    onNavigateToSongs = { navigateToTab(DockTab.SONGS) },
                                    onNavigateToFavoritesScreen = { dedicatedSubScreen = "FAVORITES" },
                                    onNavigateToAllSongsScreen = { dedicatedSubScreen = "ALL_SONGS" }
                                )
                                DockTab.SONGS.name -> SongsScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { isSettingsOpen = true }
                                )
                                DockTab.LIBRARY.name -> LibraryScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { isSettingsOpen = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // DUAL-TIER FLOATING DOCK (GPU offloaded layer)
        AnimatedVisibility(
            visible = isDockVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> (fullHeight * 1.15f).toInt() },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = 0.8f
                )
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = 0.8f
                )
            ) + fadeIn(
                animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> (fullHeight * 1.2f).toInt() },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 0.90f,
                animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 220)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer()
        ) {
            DualTierFloatingDock(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                selectedTab = selectedTab,
                visualTheme = userSettings.musicVisualTheme,
                onTabSelected = { tab ->
                    navigateToTab(tab)
                },
                onOpenNowPlaying = { isNowPlayingExpanded = true },
                onPlayPauseClick = { viewModel.togglePlayPause() }
            )
        }

        // NOW PLAYING CANVAS OVERLAY WITH ANDROID 17 SPATIAL EXPANSION & GPU RASTERIZATION
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring<IntOffset>(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
            modifier = Modifier.graphicsLayer()
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onCollapse = { isNowPlayingExpanded = false }
            )
        }
    }
}
