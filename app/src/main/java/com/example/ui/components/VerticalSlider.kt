package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalVolumeSlider(
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    lengthFraction: Float = 0.8f,
    thicknessDp: Float = 6f,
    onSpeakerClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(maxOf(52f, thicknessDp * 3f).dp)
            .fillMaxHeight(lengthFraction.coerceIn(0.5f, 1.0f))
            .testTag("vertical_volume_slider_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { onSpeakerClick?.invoke() },
            modifier = Modifier
                .padding(bottom = 6.dp)
                .testTag("volume_speaker_toggle_button")
        ) {
            SpeakerIconIndicator(value = value, isMuted = isMuted)
        }

        val activeTrackColor = MaterialTheme.colorScheme.primary
        val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant

        // Vertical Seekbar Canvas: Reuses exact progress seekbar track & thumb styling
        Box(
            modifier = Modifier
                .weight(1f)
                .width(maxOf(36f, thicknessDp * 2.8f).dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(thicknessDp) {
                        detectTapGestures { offset ->
                            val thumbLengthPx = maxOf(18f, thicknessDp * 2.5f).dp.toPx()
                            val padding = thumbLengthPx / 2f
                            val usableHeight = (size.height - 2 * padding).coerceAtLeast(1f)
                            val clampedY = offset.y.coerceIn(padding, padding + usableHeight)
                            val newValue = 1.0f - ((clampedY - padding) / usableHeight)
                            onValueChange(newValue.coerceIn(0f, 1f))
                        }
                    }
                    .pointerInput(thicknessDp) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            val thumbLengthPx = maxOf(18f, thicknessDp * 2.5f).dp.toPx()
                            val padding = thumbLengthPx / 2f
                            val usableHeight = (size.height - 2 * padding).coerceAtLeast(1f)
                            val clampedY = change.position.y.coerceIn(padding, padding + usableHeight)
                            val newValue = 1.0f - ((clampedY - padding) / usableHeight)
                            onValueChange(newValue.coerceIn(0f, 1f))
                        }
                    }
            ) {
                val trackWidthPx = thicknessDp.dp.toPx()
                val thumbLengthPx = maxOf(18f, thicknessDp * 2.5f).dp.toPx()
                val thumbThicknessPx = (thicknessDp * (4f / 6f)).coerceAtLeast(2f).dp.toPx()

                val centerX = size.width / 2f
                val padding = thumbLengthPx / 2f
                val usableHeight = (size.height - 2 * padding).coerceAtLeast(1f)
                val thumbY = padding + usableHeight * (1f - value.coerceIn(0f, 1f))

                // Inactive Track (Top to thumbY)
                if (thumbY > padding) {
                    drawLine(
                        color = inactiveTrackColor,
                        start = Offset(centerX, padding),
                        end = Offset(centerX, thumbY),
                        strokeWidth = trackWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // Active Track (thumbY to Bottom)
                if (thumbY < padding + usableHeight) {
                    drawLine(
                        color = activeTrackColor,
                        start = Offset(centerX, thumbY),
                        end = Offset(centerX, padding + usableHeight),
                        strokeWidth = trackWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // Thumb Indicator Line (Horizontal bar perpendicular to track)
                drawLine(
                    color = activeTrackColor,
                    start = Offset(centerX - thumbLengthPx / 2f, thumbY),
                    end = Offset(centerX + thumbLengthPx / 2f, thumbY),
                    strokeWidth = thumbThicknessPx,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Percentage label at bottom
        Text(
            text = "${kotlin.math.round(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpeakerIconIndicator(
    value: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        val iconColor = MaterialTheme.colorScheme.primary
        if (isMuted) {
            Icon(
                imageVector = Icons.Default.VolumeMute,
                contentDescription = "Muted Speaker",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val slashColor = Color(
                    red = (iconColor.red * 0.75f).coerceIn(0f, 1f),
                    green = (iconColor.green * 0.75f).coerceIn(0f, 1f),
                    blue = (iconColor.blue * 0.75f).coerceIn(0f, 1f),
                    alpha = iconColor.alpha
                )
                val strokePx = 3.dp.toPx()

                // Slash (/) line across speaker icon
                drawLine(
                    color = slashColor,
                    start = Offset(size.width * 0.75f, size.height * 0.22f),
                    end = Offset(size.width * 0.25f, size.height * 0.78f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        } else {
            val volumeIcon = when {
                value <= 0.01f -> Icons.Default.VolumeMute
                value < 0.5f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            }
            Icon(
                imageVector = volumeIcon,
                contentDescription = "Active Speaker",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

