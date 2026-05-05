package io.github.amiigood.lumi.lumi.ui.reader

import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.amiigood.lumi.lumi.data.reader.PageRef
import java.io.File

@Composable
fun WebtoonReader(
    pages: List<PageRef>,
    initialPage: Int,
    listState: LazyListState = rememberLazyListState(initialPage),
    onCenterTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val targetWidthPx = with(density) { config.screenWidthDp.dp.roundToPx() }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onCenterTap() })
                }
        ) {
            items(pages, key = { it.key }) { page ->
                WebtoonPage(
                    page = page,
                    targetWidthPx = targetWidthPx,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun WebtoonPage(
    page: PageRef,
    targetWidthPx: Int,
    context: Context
) {
    var resolvedFile by remember(page.key) { mutableStateOf<File?>(null) }

    LaunchedEffect(page.key) {
        resolvedFile = when (page) {
            is PageRef.Direct -> page.file
            is PageRef.LazyPdf -> page.resolve()
        }
    }

    val file = resolvedFile
    if (file == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .size(targetWidthPx)
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