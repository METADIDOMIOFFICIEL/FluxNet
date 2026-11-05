package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SpeedSample
import com.example.ui.theme.DownloadCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.UploadEmerald
import com.example.util.Formatters

@Composable
fun LiveThroughputChart(
    samples: List<SpeedSample>,
    modifier: Modifier = Modifier,
    maxPoints: Int = 60,
    showTimeAxis: Boolean = false
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val maxBps = (samples.maxOfOrNull { maxOf(it.downloadBps, it.uploadBps) } ?: 1024L)
        .coerceAtLeast(1024L)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = DownloadCyan,
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Réception",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(14.dp))
                Surface(
                    color = UploadEmerald,
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Envoi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Pic : ${Formatters.formatSpeed(maxBps)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Horizontal dashed gridlines
                for (i in 1..3) {
                    val y = h * (i / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                if (samples.size >= 2) {
                    val padded = if (samples.size < maxPoints) {
                        List(maxPoints - samples.size) { SpeedSample(0L, 0L, 0L) } + samples
                    } else {
                        samples.takeLast(maxPoints)
                    }

                    val stepX = w / (padded.size - 1).coerceAtLeast(1)

                    // Download path & fill
                    val downPath = Path()
                    val downFill = Path()
                    padded.forEachIndexed { idx, sample ->
                        val x = idx * stepX
                        val ratio = (sample.downloadBps.toFloat() / maxBps.toFloat()).coerceIn(0f, 1f)
                        val y = h - (ratio * (h - 8.dp.toPx())) - 4.dp.toPx()
                        if (idx == 0) {
                            downPath.moveTo(x, y)
                            downFill.moveTo(x, h)
                            downFill.lineTo(x, y)
                        } else {
                            downPath.lineTo(x, y)
                            downFill.lineTo(x, y)
                        }
                    }
                    downFill.lineTo(w, h)
                    downFill.close()

                    drawPath(
                        path = downFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DownloadCyan.copy(alpha = 0.28f),
                                DownloadCyan.copy(alpha = 0.0f)
                            )
                        )
                    )
                    drawPath(
                        path = downPath,
                        color = DownloadCyan,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Upload path & fill
                    val upPath = Path()
                    val upFill = Path()
                    padded.forEachIndexed { idx, sample ->
                        val x = idx * stepX
                        val ratio = (sample.uploadBps.toFloat() / maxBps.toFloat()).coerceIn(0f, 1f)
                        val y = h - (ratio * (h - 8.dp.toPx())) - 4.dp.toPx()
                        if (idx == 0) {
                            upPath.moveTo(x, y)
                            upFill.moveTo(x, h)
                            upFill.lineTo(x, y)
                        } else {
                            upPath.lineTo(x, y)
                            upFill.lineTo(x, y)
                        }
                    }
                    upFill.lineTo(w, h)
                    upFill.close()

                    drawPath(
                        path = upFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                UploadEmerald.copy(alpha = 0.22f),
                                UploadEmerald.copy(alpha = 0.0f)
                            )
                        )
                    )
                    drawPath(
                        path = upPath,
                        color = UploadEmerald,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        if (showTimeAxis) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-45s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-30s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-15s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Maintenant",
                    style = MaterialTheme.typography.labelSmall,
                    color = DownloadCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MiniSparkline(
    samples: List<SpeedSample>,
    modifier: Modifier = Modifier
) {
    val maxBps = (samples.maxOfOrNull { it.downloadBps + it.uploadBps } ?: 1024L)
        .coerceAtLeast(1024L)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (samples.size < 2) {
            drawLine(
                color = DownloadCyan.copy(alpha = 0.3f),
                start = Offset(0f, h - 2.dp.toPx()),
                end = Offset(w, h - 2.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
            return@Canvas
        }

        val recent = samples.takeLast(18)
        val stepX = w / (recent.size - 1).coerceAtLeast(1)
        val path = Path()
        recent.forEachIndexed { idx, sample ->
            val x = idx * stepX
            val total = (sample.downloadBps + sample.uploadBps).toFloat()
            val ratio = (total / maxBps.toFloat()).coerceIn(0f, 1f)
            val y = h - (ratio * (h - 4.dp.toPx())) - 2.dp.toPx()
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = DownloadCyan,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
