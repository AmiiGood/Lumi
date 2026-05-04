package com.sweetcode.lumi.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sweetcode.lumi.data.reader.PageRef
import java.io.File
import kotlin.math.abs
import androidx.compose.ui.unit.dp

@Composable
fun ZoomablePage(
    page: PageRef,
    onTap: (TapZone) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var resolvedFile by remember(page.key) { mutableStateOf<File?>(null) }

    LaunchedEffect(page.key) {
        resolvedFile = when (page) {
            is PageRef.Direct -> page.file
            is PageRef.LazyPdf -> page.resolve()
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val targetWidthPx = with(density) { config.screenWidthDp.dp.roundToPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            var zoom = 1f
                            var pan = Offset.Zero
                            event.changes.forEach { change ->
                                pan += change.positionChange()
                            }
                            val pressedChanges = event.changes.filter { it.pressed }
                            if (pressedChanges.size >= 2) {
                                val curr = (pressedChanges[0].position - pressedChanges[1].position).getDistance()
                                val prev = (
                                        (pressedChanges[0].position - pressedChanges[0].positionChange()) -
                                                (pressedChanges[1].position - pressedChanges[1].positionChange())
                                        ).getDistance()
                                if (prev > 0) zoom = curr / prev
                            }
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan / 2f
                                event.changes.forEach { it.consume() }
                            } else {
                                offset = Offset.Zero
                            }
                        } else if (pointerCount == 1 && scale > 1f) {
                            event.changes.forEach { change ->
                                val delta = change.positionChange()
                                if (abs(delta.x) > 0 || abs(delta.y) > 0) {
                                    offset += delta
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { tapOffset ->
                        if (scale <= 1.01f) {
                            val width = size.width
                            val zone = when {
                                tapOffset.x < width * 0.3f -> TapZone.Left
                                tapOffset.x > width * 0.7f -> TapZone.Right
                                else -> TapZone.Center
                            }
                            onTap(zone)
                        }
                    }
                )
            }
    ) {
        val file = resolvedFile
        if (file == null) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .size(targetWidthPx * 2)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }
    }
}

enum class TapZone { Left, Center, Right }