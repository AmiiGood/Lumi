package com.sweetcode.lumi.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun WebtoonReader(
    pages: List<File>,
    initialPage: Int,
    listState: LazyListState = rememberLazyListState(initialPage),
    onCenterTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onCenterTap() })
                }
        ) {
            items(pages, key = { it.absolutePath }) { file ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 1.dp)
                )
            }
        }
    }
}