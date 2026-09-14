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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cyber Pulse Orb (هولوجرام نبض النيون)
 * Futuristic holographic sound sphere with neon energy rings, orbital particle nodes,
 * and high-tech soundwave matrix.
 */
@Composable
fun CyberPulseCanvas(
    track: AudioTrack?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_pulse_anim")

    // Continuous smooth rotation for orbital rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cyber_rotation"
    )

    // Counter rotation for reverse orbital layer
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cyber_counter_rotation"
    )

    // Pulsing energy wave breathing effect
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyber_pulse_scale"
    )

    val currentRotation = if (isPlaying) rotation else 0f
    val currentCounterRotation = if (isPlaying) counterRotation else 0f
    val currentPulse = if (isPlaying) pulseScale else 1.0f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("cyber_pulse_canvas"),
        contentAlignment = Alignment.Center
    ) {
        // Deep Holographic Cyber Canvas
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .shadow(16.dp, CircleShape, spotColor = Color(0xFF00F0FF))
                .clip(CircleShape)
                .background(Color(0xFF080B14)),
            contentAlignment = Alignment.Center
        ) {
            // 1. Static Matrix Grid & Crosshairs
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Outer Cyber Rim
                drawCircle(
                    color = Color(0xFF00F0FF),
                    radius = maxRadius * 0.97f,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // Outer Neon Hex / Matrix Laser Grid Lines (12 radial beams)
                for (i in 0 until 12) {
                    val angleRad = Math.toRadians((i * 30).toDouble())
                    val startR = maxRadius * 0.46f
                    val endR = maxRadius * 0.95f
                    val x1 = (center.x + startR * cos(angleRad)).toFloat()
                    val y1 = (center.y + startR * sin(angleRad)).toFloat()
                    val x2 = (center.x + endR * cos(angleRad)).toFloat()
                    val y2 = (center.y + endR * sin(angleRad)).toFloat()

                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = 0.25f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.8f
                    )
                }

                // Precision Degree Tick Marks
                for (i in 0 until 24) {
                    val angleRad = Math.toRadians((i * 15).toDouble())
                    val isMajor = i % 6 == 0
                    val tickLen = if (isMajor) maxRadius * 0.07f else maxRadius * 0.035f
                    val startR = maxRadius * 0.97f - tickLen
                    val endR = maxRadius * 0.97f
                    val x1 = (center.x + startR * cos(angleRad)).toFloat()
                    val y1 = (center.y + startR * sin(angleRad)).toFloat()
                    val x2 = (center.x + endR * cos(angleRad)).toFloat()
                    val y2 = (center.y + endR * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (isMajor) Color(0xFF00F0FF) else Color(0xFFFF007F),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = if (isMajor) 2.5f else 1.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Middle Translucent Frequency Ring
                drawCircle(
                    color = Color(0xFF00F0FF).copy(alpha = 0.35f),
                    radius = maxRadius * 0.68f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // 2. Rotating Forward Orbital Energy Layer (Magenta arcs + Cyan nodes)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = currentRotation }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Outer Neon Magenta Energy Arcs (#FF007F)
                val arcR1 = maxRadius * 0.83f
                val rectArc1 = Rect(
                    center.x - arcR1,
                    center.y - arcR1,
                    center.x + arcR1,
                    center.y + arcR1
                )
                for (i in 0 until 4) {
                    drawArc(
                        color = Color(0xFFFF007F),
                        startAngle = i * 90f + 15f,
                        sweepAngle = 55f,
                        useCenter = false,
                        topLeft = rectArc1.topLeft,
                        size = rectArc1.size,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                }

                // 6 Glowing Orbital Sound Particle Nodes
                val orbitR = maxRadius * 0.68f
                val nodeColors = listOf(
                    Color(0xFF00F0FF),
                    Color(0xFFFF007F),
                    Color(0xFFA855F7),
                    Color(0xFF00F0FF),
                    Color(0xFFFF007F),
                    Color(0xFFA855F7)
                )
                for (i in 0 until 6) {
                    val angleRad = Math.toRadians((i * 60 + 20).toDouble())
                    val nx = (center.x + orbitR * cos(angleRad)).toFloat()
                    val ny = (center.y + orbitR * sin(angleRad)).toFloat()

                    // Glow halo
                    drawCircle(
                        color = nodeColors[i].copy(alpha = 0.4f),
                        radius = 12f,
                        center = Offset(nx, ny)
                    )
                    // Core dot
                    drawCircle(
                        color = nodeColors[i],
                        radius = 6.5f,
                        center = Offset(nx, ny)
                    )
                    // Specular highlight
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = Offset(nx, ny)
                    )
                }
            }

            // 3. Counter-Rotating Reverse Pulse Layer (Violet & Cyan segmented rings)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = currentCounterRotation
                        scaleX = currentPulse
                        scaleY = currentPulse
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Inner Violet Pulse Segments (#8B5CF6)
                val arcR2 = maxRadius * 0.54f
                val rectArc2 = Rect(
                    center.x - arcR2,
                    center.y - arcR2,
                    center.x + arcR2,
                    center.y + arcR2
                )
                for (i in 0 until 6) {
                    drawArc(
                        color = Color(0xFF8B5CF6),
                        startAngle = i * 60f + 10f,
                        sweepAngle = 35f,
                        useCenter = false,
                        topLeft = rectArc2.topLeft,
                        size = rectArc2.size,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Central Holographic Plasma Core (Album Art / Cyber Icon)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.44f)
                    .graphicsLayer {
                        scaleX = if (isPlaying) currentPulse else 1f
                        scaleY = if (isPlaying) currentPulse else 1f
                    }
                    .shadow(12.dp, CircleShape, spotColor = Color(0xFFFF007F))
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFF00F0FF), CircleShape)
                    .border(5.dp, Color(0xFFFF007F).copy(alpha = 0.4f), CircleShape)
                    .background(Color(0xFF0D1B2A)),
                contentAlignment = Alignment.Center
            ) {
                if (track?.albumArtDrawableRes != null) {
                    Image(
                        painter = painterResource(id = track.albumArtDrawableRes),
                        contentDescription = "Cyber Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (track?.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = "Cyber Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Futuristic Cyber Equalizer Glyph
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Center Holographic Reticle Crosshair
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val crossLen = size.width * 0.16f
                    val crossGap = size.width * 0.05f

                    val crossColor = Color(0xFF00F0FF).copy(alpha = 0.85f)
                    drawLine(crossColor, Offset(c.x - crossLen, c.y), Offset(c.x - crossGap, c.y), strokeWidth = 2.5f)
                    drawLine(crossColor, Offset(c.x + crossGap, c.y), Offset(c.x + crossLen, c.y), strokeWidth = 2.5f)
                    drawLine(crossColor, Offset(c.x, c.y - crossLen), Offset(c.x, c.y - crossGap), strokeWidth = 2.5f)
                    drawLine(crossColor, Offset(c.x, c.y + crossGap), Offset(c.x, c.y + crossLen), strokeWidth = 2.5f)
                }
            }
        }
    }
}
