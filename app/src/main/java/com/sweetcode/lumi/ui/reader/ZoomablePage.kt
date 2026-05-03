package com.sweetcode.lumi.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import kotlin.math.abs

@Composable
fun ZoomablePage(
    file: File,
    onTap: (TapZone) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // Solo consumimos pinch/pan si hay zoom activo o se inicia pinch (2 dedos)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            // Pinch zoom
                            var zoom = 1f
                            var pan = Offset.Zero
                            event.changes.forEach { change ->
                                pan += change.positionChange()
                            }
                            // Calcular zoom basado en distancia entre dedos
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
                            // Pan con un dedo solo si está en zoom
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
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
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

enum class TapZone { Left, Center, Right }