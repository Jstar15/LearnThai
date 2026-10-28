package com.example.learnthai.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/**
 * Loads story art from `assets/stories/...` with a small in-memory [LruCache].
 *
 * Art is bundled as assets rather than drawables so a new story needs no `R.drawable`
 * registry entry. If an image is missing or unreadable the card degrades to a deterministic
 * gradient placeholder instead of crashing or showing an empty box.
 */
@Composable
fun StoryImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackLabel: String = "",
    fallbackInitial: String = ""
) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf(StoryArtCache.peek(assetPath)) }

    LaunchedEffect(assetPath) {
        if (bitmap == null && assetPath.isNotBlank()) {
            bitmap = withContext(Dispatchers.IO) { StoryArtCache.load(context, assetPath) }
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        StoryArtPlaceholder(
            modifier = modifier.fillMaxSize(),
            label = fallbackLabel,
            initial = fallbackInitial.ifBlank { fallbackLabel.take(1).uppercase() },
            seed = assetPath
        )
    }
}

@Composable
private fun StoryArtPlaceholder(
    modifier: Modifier,
    label: String,
    initial: String,
    seed: String
) {
    val palette = listOf(
        Color(0xFF4E7A2E) to Color(0xFF1B3520),
        Color(0xFFC2622E) to Color(0xFF3A1F12),
        Color(0xFF6750A4) to Color(0xFF241546),
        Color(0xFF2E6E7A) to Color(0xFF102B30),
        Color(0xFF8A5A2E) to Color(0xFF2B1A0E)
    )
    val pair = palette[seed.hashCode().absoluteValue % palette.size]

    Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(pair.first, pair.second))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            if (label.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private object StoryArtCache {

    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun peek(path: String): Bitmap? = if (path.isBlank()) null else cache.get(path)

    fun load(context: Context, path: String): Bitmap? {
        if (path.isBlank()) return null
        cache.get(path)?.let { return it }

        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open("stories/$path").use { BitmapFactory.decodeStream(it, null, bounds) }

            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= TARGET_WIDTH) sampleSize *= 2

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.assets.open("stories/$path").use {
                BitmapFactory.decodeStream(it, null, options)
            }
            if (bitmap != null) cache.put(path, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private const val TARGET_WIDTH = 1080
}
