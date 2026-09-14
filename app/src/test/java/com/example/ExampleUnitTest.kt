package com.example

import android.net.Uri
import com.example.data.SongCache
import com.example.model.AudioTrack
import com.example.model.UserSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun userSettings_hideHeaderOnScroll_defaultAndCustom() {
    val defaultSettings = UserSettings()
    assertTrue(defaultSettings.hideHeaderOnScroll)

    val customSettings = defaultSettings.copy(hideHeaderOnScroll = false)
    assertFalse(customSettings.hideHeaderOnScroll)
  }

  @Test
  fun userSettings_autoScanDevice_defaultIsFalse() {
    val defaultSettings = UserSettings()
    assertFalse(defaultSettings.autoScanDevice)

    val enabledSettings = defaultSettings.copy(autoScanDevice = true)
    assertTrue(enabledSettings.autoScanDevice)
  }

  @Test
  fun songCache_preservesTracksInMemory() {
    val dummyTrack = AudioTrack(
      id = 101L,
      title = "Test Song",
      artist = "Test Artist",
      album = "Test Album",
      durationMs = 180000L,
      uri = Uri.parse("content://media/external/audio/media/101")
    )

    SongCache.allSongs = listOf(dummyTrack)
    assertTrue(SongCache.isLoaded)
    assertEquals(1, SongCache.allSongs.size)
    assertEquals("Test Song", SongCache.allSongs.first().title)
  }
}


