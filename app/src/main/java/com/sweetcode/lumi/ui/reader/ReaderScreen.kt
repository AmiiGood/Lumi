package com.sweetcode.lumi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewDay

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val s = state) {
            ReaderUiState.Loading -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Preparando páginas…",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is ReaderUiState.Error -> Text(
                text = s.message,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            is ReaderUiState.Unsupported -> Text(
                text = "Lector ${s.format.name} próximamente",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            is ReaderUiState.ImageReady -> ImageReader(
                state = s,
                onBack = onBack,
                onTutorialSeen = viewModel::markTutorialSeen,
                onToggleMode = viewModel::toggleMode,
                onPageChanged = { page -> viewModel.saveProgress(page, s.pages.size) }
            )
            is ReaderUiState.EpubReady -> EpubReader(
                htmlFile = s.htmlFile,
                title = s.item.title,
                initialProgress = s.initialProgress,
                onBack = onBack,
                onProgressChanged = { current, total -> viewModel.saveProgress(current, total) }
            )
        }
    }
}

@Composable
private fun ImageReader(
    state: ReaderUiState.ImageReady,
    onBack: () -> Unit,
    onTutorialSeen: () -> Unit,
    onToggleMode: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = state.initialPage,
        pageCount = { state.pages.size }
    )
    val webtoonState = androidx.compose.foundation.lazy.rememberLazyListState(state.initialPage)
    val scope = rememberCoroutineScope()
    var showOverlay by remember { mutableStateOf(true) }
    var showTutorial by remember { mutableStateOf(state.showTutorial) }

    LaunchedEffect(state.mode, pagerState, webtoonState) {
        androidx.compose.runtime.snapshotFlow {
            when (state.mode) {
                com.sweetcode.lumi.data.model.ReadingMode.PAGED -> pagerState.currentPage
                com.sweetcode.lumi.data.model.ReadingMode.WEBTOON -> webtoonState.firstVisibleItemIndex
            }
        }.collect { page -> onPageChanged(page) }
    }

    LaunchedEffect(showOverlay) {
        if (showOverlay && !showTutorial) {
            kotlinx.coroutines.delay(3000)
            showOverlay = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.mode) {
            com.sweetcode.lumi.data.model.ReadingMode.PAGED -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZoomablePage(
                        file = state.pages[page],
                        onTap = { zone ->
                            when (zone) {
                                TapZone.Left -> scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                                TapZone.Right -> scope.launch {
                                    if (pagerState.currentPage < state.pages.lastIndex) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                                TapZone.Center -> showOverlay = !showOverlay
                            }
                        }
                    )
                }
            }
            com.sweetcode.lumi.data.model.ReadingMode.WEBTOON -> {
                WebtoonReader(
                    pages = state.pages,
                    initialPage = state.initialPage,
                    listState = webtoonState,
                    onCenterTap = { showOverlay = !showOverlay },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopBar(
                title = state.item.title,
                mode = state.mode,
                onBack = onBack,
                onToggleMode = onToggleMode
            )
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val currentPage = when (state.mode) {
                com.sweetcode.lumi.data.model.ReadingMode.PAGED -> pagerState.currentPage + 1
                com.sweetcode.lumi.data.model.ReadingMode.WEBTOON -> webtoonState.firstVisibleItemIndex + 1
            }
            BottomBar(
                currentPage = currentPage,
                totalPages = state.pages.size
            )
        }

        if (showTutorial) {
            ReaderTutorial(
                onDismiss = {
                    showTutorial = false
                    showOverlay = true
                    onTutorialSeen()
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    mode: com.sweetcode.lumi.data.model.ReadingMode,
    onBack: () -> Unit,
    onToggleMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Atrás",
                tint = Color.White
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            maxLines = 1
        )
        IconButton(onClick = onToggleMode) {
            Icon(
                imageVector = if (mode == com.sweetcode.lumi.data.model.ReadingMode.PAGED) {
                    androidx.compose.material.icons.Icons.Rounded.ViewDay
                } else {
                    androidx.compose.material.icons.Icons.Rounded.ViewCarousel
                },
                contentDescription = "Cambiar modo",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun BottomBar(
    currentPage: Int,
    totalPages: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$currentPage / $totalPages",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        LinearProgressIndicator(
            progress = { currentPage.toFloat() / totalPages.toFloat() },
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}