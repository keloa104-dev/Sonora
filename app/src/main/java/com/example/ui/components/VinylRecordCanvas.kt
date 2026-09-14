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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AudioTrack

@Composable
fun VinylRecordCanvas(
    track: AudioTrack?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("vinyl_record_canvas"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Vinyl Record Disc (Spinning)
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer { rotationZ = currentRotation }
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Groove Lines & Stroboscopic Patterns Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Outer Rim Ring
                drawCircle(
                    color = Color(0xFFE63946),
                    radius = maxRadius * 0.97f,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // Concentric Red Grooved Rings (#E63946)
                val grooveRatios = listOf(0.92f, 0.86f, 0.80f, 0.74f, 0.68f, 0.62f, 0.56f, 0.50f)
                grooveRatios.forEach { ratio ->
                    drawCircle(
                        color = Color(0xFFE63946).copy(alpha = 0.85f),
                        radius = maxRadius * ratio,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }

                // Stroboscopic Dots Outer Ring
                val outerRadius = maxRadius * 0.88f
                for (i in 0 until 36) {
                    val angleRad = Math.toRadians((i * 10).toDouble())
                    val dotX = (center.x + outerRadius * kotlin.math.cos(angleRad)).toFloat()
                    val dotY = (center.y + outerRadius * kotlin.math.sin(angleRad)).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = Offset(dotX, dotY)
                    )
                }

                // Stroboscopic Dashes Middle Ring
                val midRadius = maxRadius * 0.68f
                val dashRect = androidx.compose.ui.geometry.Rect(
                    center.x - midRadius,
                    center.y - midRadius,
                    center.x + midRadius,
                    center.y + midRadius
                )
                for (i in 0 until 18) {
                    drawArc(
                        color = Color(0xFFFFD166),
                        startAngle = i * 20f,
                        sweepAngle = 10f,
                        useCenter = false,
                        topLeft = dashRect.topLeft,
                        size = dashRect.size,
                        style = Stroke(width = 3.5f)
                    )
                }

                // Inner Stroboscopic Dots Ring
                val innerRadius = maxRadius * 0.52f
                for (i in 0 until 12) {
                    val angleRad = Math.toRadians((i * 30).toDouble())
                    val dotX = (center.x + innerRadius * kotlin.math.cos(angleRad)).toFloat()
                    val dotY = (center.y + innerRadius * kotlin.math.sin(angleRad)).toFloat()
                    drawCircle(
                        color = Color(0xFFE63946),
                        radius = 3.5f,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // Center Album Art Label Core
            Box(
                modifier = Modifier
                    .fillMaxSize(0.42f)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFF00E5FF), CircleShape)
                    .background(Color(0xFFE63946)),
                contentAlignment = Alignment.Center
            ) {
                if (track?.albumArtDrawableRes != null) {
                    Image(
                        painter = painterResource(id = track.albumArtDrawableRes),
                        contentDescription = "Vinyl Label Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (track?.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = "Vinyl Label Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Center Spindle Hole
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF000000))
                        .border(1.dp, Color.Gray, CircleShape)
                )
            }
        }

        // Extended Tonearm Overlay (Pivot top-right, needle resting on record)
        val tonearmAngle = if (isPlaying) 28f else 12f
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("vinyl_tonearm_overlay")
        ) {
            val pivotX = size.width * 0.88f
            val pivotY = size.height * 0.10f

            // Tonearm Base Pivot
            drawCircle(
                color = Color(0xFF888888),
                radius = 16f,
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color(0xFF444444),
                radius = 8f,
                center = Offset(pivotX, pivotY)
            )

            // Tonearm Arm Line (Angled based on tonearmAngle)
            val rad = Math.toRadians(tonearmAngle.toDouble())
            val armLength = size.width * 0.52f
            val endX = pivotX - (armLength * Math.cos(rad)).toFloat()
            val endY = pivotY + (armLength * Math.sin(rad)).toFloat()

            drawLine(
                color = Color(0xFFDDDDDD),
                start = Offset(pivotX, pivotY),
                end = Offset(endX, endY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Tonearm Cartridge / Needle Head
            drawCircle(
                color = Color(0xFFFF2E93),
                radius = 10f,
                center = Offset(endX, endY)
            )
        }
    }
}
