package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AudioTrack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sonic Radar & Oscilloscope (رادار الأمواج الصوتية والمذبذب)
 * Precision audiophile acoustic radar with sweeping sonar laser beam,
 * concentric decibel range circles, and live oscillating audio waveforms.
 */
@Composable
fun SonicRadarCanvas(
    track: AudioTrack?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonic_radar_anim")

    // Continuous 360 radar sweep rotation
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep_angle"
    )

    // Waveform oscillation phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_oscillation_phase"
    )

    // Dynamic frequency spike breathing
    val spikeScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "freq_spike_scale"
    )

    val currentSweep = if (isPlaying) sweepRotation else 0f
    val currentWavePhase = if (isPlaying) wavePhase else 0f
    val currentSpikeScale = if (isPlaying) spikeScale else 1.0f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("sonic_radar_canvas"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Radar & Oscilloscope Chassis
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .shadow(16.dp, CircleShape, spotColor = Color(0xFF00FF88))
                .clip(CircleShape)
                .background(Color(0xFF08140E)),
            contentAlignment = Alignment.Center
        ) {
            // 1. Static Acoustic Grids, Degree Ticks & Range Circles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Outer Bezel Ring (#00FF88)
                drawCircle(
                    color = Color(0xFF00FF88),
                    radius = maxRadius * 0.97f,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // 360 Degree Graduation Tick Marks (every 10 degrees)
                for (i in 0 until 36) {
                    val angleRad = Math.toRadians((i * 10).toDouble())
                    val isMajor = i % 9 == 0
                    val isMedium = i % 3 == 0
                    val tickLen = when {
                        isMajor -> maxRadius * 0.08f
                        isMedium -> maxRadius * 0.05f
                        else -> maxRadius * 0.028f
                    }
                    val startR = maxRadius * 0.97f - tickLen
                    val endR = maxRadius * 0.97f
                    val x1 = (center.x + startR * cos(angleRad)).toFloat()
                    val y1 = (center.y + startR * sin(angleRad)).toFloat()
                    val x2 = (center.x + endR * cos(angleRad)).toFloat()
                    val y2 = (center.y + endR * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (isMajor) Color(0xFF00FF88) else Color(0xFF00FF88).copy(alpha = 0.5f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = if (isMajor) 2.5f else 1.2f,
                        cap = StrokeCap.Round
                    )
                }

                // Concentric Acoustic Range Grid Rings
                val gridRatios = listOf(0.85f, 0.70f, 0.55f)
                gridRatios.forEach { ratio ->
                    drawCircle(
                        color = Color(0xFF00FF88).copy(alpha = 0.25f),
                        radius = maxRadius * ratio,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Orthogonal Crosshair Axis Lines
                val axisColor = Color(0xFF00FF88).copy(alpha = 0.35f)
                drawLine(
                    color = axisColor,
                    start = Offset(center.x - maxRadius * 0.95f, center.y),
                    end = Offset(center.x + maxRadius * 0.95f, center.y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = axisColor,
                    start = Offset(center.x, center.y - maxRadius * 0.95f),
                    end = Offset(center.x, center.y + maxRadius * 0.95f),
                    strokeWidth = 1.2f
                )
            }

            // 2. Oscillating Acoustic Sine Wave Ribbon & Frequency Bars
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Radial Equalizer Frequency Spikes (24 bars around mid-circle)
                val spikeR = maxRadius * 0.70f
                for (i in 0 until 24) {
                    val angleRad = Math.toRadians((i * 15).toDouble())
                    val baseHeight = (maxRadius * 0.055f) * ((i % 4) + 1) * 0.45f
                    val spikeHeight = baseHeight * currentSpikeScale
                    val x1 = (center.x + (spikeR - spikeHeight) * cos(angleRad)).toFloat()
                    val y1 = (center.y + (spikeR - spikeHeight) * sin(angleRad)).toFloat()
                    val x2 = (center.x + (spikeR + spikeHeight) * cos(angleRad)).toFloat()
                    val y2 = (center.y + (spikeR + spikeHeight) * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (i % 2 == 0) Color(0xFF00FF88) else Color(0xFF00E5FF),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Live Oscilloscope Sine Wave Ring (Radius ~ 0.85)
                val wavePath = Path()
                val waveBaseR = maxRadius * 0.85f
                val points = 60
                for (p in 0..points) {
                    val a = (p / points.toFloat()) * (2 * PI)
                    val waveOffset = sin(a * 6 + currentWavePhase) * (maxRadius * 0.035f)
                    val r = waveBaseR + waveOffset
                    val px = (center.x + r * cos(a)).toFloat()
                    val py = (center.y + r * sin(a)).toFloat()
                    if (p == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                }
                wavePath.close()

                drawPath(
                    path = wavePath,
                    color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    style = Stroke(width = 2.5f)
                )
            }

            // 3. Sweeping Sonar Laser Beam (Spinning Sector & Leading Edge)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = currentSweep }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Sweeping Radar Fan Sector
                val sweepRect = Rect(
                    center.x - maxRadius * 0.94f,
                    center.y - maxRadius * 0.94f,
                    center.x + maxRadius * 0.94f,
                    center.y + maxRadius * 0.94f
                )
                drawArc(
                    color = Color(0xFF00FF88).copy(alpha = 0.22f),
                    startAngle = 270f,
                    sweepAngle = 55f,
                    useCenter = true,
                    topLeft = sweepRect.topLeft,
                    size = sweepRect.size
                )

                // Leading Sharp Laser Line
                drawLine(
                    color = Color(0xFF00FF88),
                    start = center,
                    end = Offset(center.x, center.y - maxRadius * 0.94f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // 4. Central Oscilloscope Audio Target Lens (Album Art / Acoustic Icon)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.44f)
                    .shadow(12.dp, CircleShape, spotColor = Color(0xFF00FF88))
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFF00FF88), CircleShape)
                    .border(5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape)
                    .background(Color(0xFF051B11)),
                contentAlignment = Alignment.Center
            ) {
                if (track?.albumArtDrawableRes != null) {
                    Image(
                        painter = painterResource(id = track.albumArtDrawableRes),
                        contentDescription = "Radar Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (track?.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = "Radar Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Acoustic Waveform Icon
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Precision Reticle Ring & Crosshair Target
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val reticleR = size.width * 0.22f
                    val armLen = size.width * 0.12f

                    drawCircle(
                        color = Color(0xFF00FF88).copy(alpha = 0.75f),
                        radius = reticleR,
                        center = c,
                        style = Stroke(width = 1.5f)
                    )

                    val crossColor = Color(0xFF00FF88).copy(alpha = 0.85f)
                    drawLine(crossColor, Offset(c.x - armLen, c.y), Offset(c.x + armLen, c.y), strokeWidth = 2f)
                    drawLine(crossColor, Offset(c.x, c.y - armLen), Offset(c.x, c.y + armLen), strokeWidth = 2f)
                }
            }
        }
    }
}
