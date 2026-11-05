package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppIconImage(
    drawable: Drawable?,
    contentDescription: String,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val bitmap: ImageBitmap? = remember(drawable) {
        drawable?.toImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap? {
    return runCatching {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap.asImageBitmap()
        }
        val width = intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = intrinsicHeight.takeIf { it > 0 } ?: 96
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bmp.asImageBitmap()
    }.getOrNull()
}
