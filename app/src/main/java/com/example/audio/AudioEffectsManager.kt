package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _isEqualizerEnabled = MutableStateFlow(true)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _bandLevels = MutableStateFlow<Map<Int, Short>>(emptyMap())
    val bandLevels: StateFlow<Map<Int, Short>> = _bandLevels.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow<Short>(0)
    val bassBoostStrength: StateFlow<Short> = _bassBoostStrength.asStateFlow()

    private var minLevel: Short = -1500
    private var maxLevel: Short = 1500

    fun init(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                val range = bandLevelRange
                if (range != null && range.size >= 2) {
                    minLevel = range[0]
                    maxLevel = range[1]
                }
            }
            _isEqualizerEnabled.value = equalizer?.enabled == true
            updateCurrentLevels()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            _bassBoostStrength.value = bassBoost?.roundedStrength ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMinLevel(): Short = minLevel
    fun getMaxLevel(): Short = maxLevel

    fun setBandLevel(band: Short, level: Short) {
        try {
            val clamped = level.coerceIn(minLevel, maxLevel)
            equalizer?.setBandLevel(band, clamped)
            val updated = _bandLevels.value.toMutableMap()
            updated[band.toInt()] = clamped
            _bandLevels.value = updated
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoostStrength(strength: Short) {
        try {
            val clamped = strength.coerceIn(0, 1000)
            bassBoost?.setStrength(clamped)
            _bassBoostStrength.value = clamped
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreset(presetName: String) {
        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands.toInt()
            when (presetName) {
                "Flat" -> {
                    for (i in 0 until bandCount) setBandLevel(i.toShort(), 0)
                    setBassBoostStrength(0)
                }
                "Bass Boost" -> {
                    if (bandCount >= 5) {
                        setBandLevel(0, (maxLevel * 0.7f).toInt().toShort())
                        setBandLevel(1, (maxLevel * 0.4f).toInt().toShort())
                        setBandLevel(2, (minLevel * 0.2f).toInt().toShort())
                        setBandLevel(3, (maxLevel * 0.2f).toInt().toShort())
                        setBandLevel(4, (maxLevel * 0.4f).toInt().toShort())
                    }
                    setBassBoostStrength(800)
                }
                "Rock" -> {
                    if (bandCount >= 5) {
                        setBandLevel(0, (maxLevel * 0.6f).toInt().toShort())
                        setBandLevel(1, (maxLevel * 0.2f).toInt().toShort())
                        setBandLevel(2, (minLevel * 0.4f).toInt().toShort())
                        setBandLevel(3, (maxLevel * 0.4f).toInt().toShort())
                        setBandLevel(4, (maxLevel * 0.8f).toInt().toShort())
                    }
                    setBassBoostStrength(600)
                }
                "Pop" -> {
                    if (bandCount >= 5) {
                        setBandLevel(0, 0)
                        setBandLevel(1, (maxLevel * 0.4f).toInt().toShort())
                        setBandLevel(2, (maxLevel * 0.6f).toInt().toShort())
                        setBandLevel(3, (maxLevel * 0.2f).toInt().toShort())
                        setBandLevel(4, 0)
                    }
                    setBassBoostStrength(400)
                }
                "Vocal" -> {
                    if (bandCount >= 5) {
                        setBandLevel(0, (minLevel * 0.4f).toInt().toShort())
                        setBandLevel(1, 0)
                        setBandLevel(2, (maxLevel * 0.8f).toInt().toShort())
                        setBandLevel(3, (maxLevel * 0.6f).toInt().toShort())
                        setBandLevel(4, (maxLevel * 0.2f).toInt().toShort())
                    }
                    setBassBoostStrength(200)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCurrentLevels() {
        val eq = equalizer ?: return
        try {
            val map = mutableMapOf<Int, Short>()
            for (i in 0 until eq.numberOfBands) {
                map[i.toInt()] = eq.getBandLevel(i.toShort())
            }
            _bandLevels.value = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
