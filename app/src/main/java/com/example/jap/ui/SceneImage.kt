package com.example.jap.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * The picture for a scenario, read from assets/scenes/<id>.webp, .png or .jpg.
 * Returns null when there is none, so callers can fall back to the kanji art.
 */
@Composable
fun rememberScene(id: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(id) {
        listOf("webp", "png", "jpg").firstNotNullOfOrNull { ext ->
            runCatching {
                context.assets.open("scenes/$id.$ext").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
}

@Composable
fun SceneImage(image: ImageBitmap, description: String, modifier: Modifier = Modifier, alignment: Alignment = Alignment.Center) {
    Image(image, contentDescription = description, contentScale = ContentScale.Crop, alignment = alignment, modifier = modifier)
}
