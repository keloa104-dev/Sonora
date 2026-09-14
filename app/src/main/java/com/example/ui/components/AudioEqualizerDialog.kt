package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.audio.AudioEffectsManager

@Composable
fun AudioEqualizerDialog(
    onDismiss: () -> Unit
) {
    val presets = listOf("Flat", "Bass Boost", "Rock", "Pop", "Vocal")
    var selectedPreset by remember { mutableStateOf("Bass Boost") }

    val bandLevels by AudioEffectsManager.bandLevels.collectAsState()
    val bassBoostStrength by AudioEffectsManager.bassBoostStrength.collectAsState()

    val minLevel = AudioEffectsManager.getMinLevel().toFloat()
    val maxLevel = AudioEffectsManager.getMaxLevel().toFloat()
    val levelRange = if (maxLevel > minLevel) (maxLevel - minLevel) else 3000f

    var band60Hz by remember(bandLevels) {
        val level = bandLevels[0]?.toFloat() ?: 0f
        mutableFloatStateOf(((level - minLevel) / levelRange).coerceIn(0f, 1f))
    }
    var band230Hz by remember(bandLevels) {
        val level = bandLevels[1]?.toFloat() ?: 0f
        mutableFloatStateOf(((level - minLevel) / levelRange).coerceIn(0f, 1f))
    }
    var band910Hz by remember(bandLevels) {
        val level = bandLevels[2]?.toFloat() ?: 0f
        mutableFloatStateOf(((level - minLevel) / levelRange).coerceIn(0f, 1f))
    }
    var band3k6Hz by remember(bandLevels) {
        val level = bandLevels[3]?.toFloat() ?: 0f
        mutableFloatStateOf(((level - minLevel) / levelRange).coerceIn(0f, 1f))
    }
    var band14kHz by remember(bandLevels) {
        val level = bandLevels[4]?.toFloat() ?: 0f
        mutableFloatStateOf(((level - minLevel) / levelRange).coerceIn(0f, 1f))
    }
    var bassBoost by remember(bassBoostStrength) {
        mutableFloatStateOf((bassBoostStrength.toFloat() / 1000f).coerceIn(0f, 1f))
    }

    fun onBandChanged(band: Short, fraction: Float) {
        val level = (minLevel + fraction * levelRange).toInt().toShort()
        AudioEffectsManager.setBandLevel(band, level)
    }

    fun onBassBoostChanged(fraction: Float) {
        bassBoost = fraction
        val strength = (fraction * 1000f).toInt().toShort()
        AudioEffectsManager.setBassBoostStrength(strength)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Equalizer",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(28.dp)
                    )
                    Text(
                        text = "Audio Equalizer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "EQUALIZER PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF00E5FF)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedPreset = preset
                                    AudioEffectsManager.applyPreset(preset)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BASS BOOST SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${(bassBoost * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Slider(
                        value = bassBoost,
                        onValueChange = { onBassBoostChanged(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "5-BAND FREQUENCY SPECTRUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                EqBandRow(label = "60 Hz (Sub Bass)", value = band60Hz, onValueChange = { band60Hz = it; onBandChanged(0, it) })
                EqBandRow(label = "230 Hz (Bass)", value = band230Hz, onValueChange = { band230Hz = it; onBandChanged(1, it) })
                EqBandRow(label = "910 Hz (Midrange)", value = band910Hz, onValueChange = { band910Hz = it; onBandChanged(2, it) })
                EqBandRow(label = "3.6 kHz (Presence)", value = band3k6Hz, onValueChange = { band3k6Hz = it; onBandChanged(3, it) })
                EqBandRow(label = "14 kHz (Brilliance)", value = band14kHz, onValueChange = { band14kHz = it; onBandChanged(4, it) })

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun EqBandRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1.3f)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(2f)
        )
    }
}
