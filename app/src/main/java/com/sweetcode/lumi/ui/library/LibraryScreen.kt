package com.sweetcode.lumi.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sweetcode.lumi.ui.components.EmptyState
import com.sweetcode.lumi.ui.components.FilterChipsRow
import com.sweetcode.lumi.ui.components.LibraryEntryCard
import com.sweetcode.lumi.ui.library.components.FeaturedHero
import com.sweetcode.lumi.ui.library.components.HorizontalSection
import com.sweetcode.lumi.ui.library.components.SearchOverlay
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.runtime.setValue

@Composable
fun LibraryScreen(
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var sortSheetOpen by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.reloadProgress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Escaneando tu biblioteca…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.error != null -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "Algo salió mal",
                description = state.error!!
            )

            state.allItems.isEmpty() -> EmptyState(
                icon = Icons.Rounded.LibraryBooks,
                title = "Tu biblioteca está vacía",
                description = "Agrega CBZ, CBR, EPUB o PDF a la carpeta seleccionada."
            )

            else -> HomeContent(
                state = state,
                onItemClick = onItemClick,
                onCollectionClick = onCollectionClick,
                onSearch = viewModel::openSearch,
                onSettings = onSettingsClick,
                onSortClick = { sortSheetOpen = true },
                onFilterChange = viewModel::setFilter,
                onRefresh = viewModel::refresh
            )
        }

        AnimatedVisibility(
            visible = state.isSearchOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SearchOverlay(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onClose = viewModel::closeSearch,
                entries = state.entries,
                onCollectionClick = onCollectionClick,
                onItemClick = onItemClick
            )
        }
    }

    if (sortSheetOpen) {
        com.sweetcode.lumi.ui.library.components.SortBottomSheet(
            selected = state.sort,
            onSelect = viewModel::setSort,
            onDismiss = { sortSheetOpen = false }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: LibraryUiState,
    onItemClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onSortClick: () -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onRefresh: () -> Unit
) {
    val gridState = rememberLazyGridState()

    val showSolidBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 200
        }
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.featured != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FeaturedHero(
                        item = state.featured,
                        onContinue = { onItemClick(state.featured.id) },
                        onClick = { onItemClick(state.featured.id) }
                    )
                }
            }

            if (state.continueReading.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HorizontalSection(
                        title = "Continúa leyendo",
                        items = state.continueReading,
                        onItemClick = { onItemClick(it.id) },
                        showProgress = true,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (state.recentlyAdded.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HorizontalSection(
                        title = "Recién agregados",
                        items = state.recentlyAdded,
                        onItemClick = { onItemClick(it.id) },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tu biblioteca",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(onClick = onSortClick) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Sort,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = state.sort.label,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FilterChipsRow(
                        selected = state.filter,
                        onSelect = onFilterChange
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (state.entries.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin resultados para este filtro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(state.entries, key = { entryKey(it) }, span = { GridItemSpan(1) }) { entry ->
                    Box(
                        modifier = Modifier.padding(
                            start = if (state.entries.indexOf(entry) % 2 == 0) 16.dp else 0.dp,
                            end = 16.dp
                        )
                    ) {
                        LibraryEntryCard(
                            entry = entry,
                            onClick = {
                                when (entry) {
                                    is LibraryEntry.Single -> onItemClick(entry.item.id)
                                    is LibraryEntry.Series -> onCollectionClick(entry.name)
                                }
                            }
                        )
                    }
                }
            }
        }

        TopBar(
            showSolid = showSolidBar,
            onSearch = onSearch,
            onSettings = onSettings,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun TopBar(
    showSolid: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (showSolid) {
        MaterialTheme.colorScheme.background
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Lumi",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearch) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                androidx.compose.material.icons.Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun entryKey(entry: LibraryEntry): String = when (entry) {
    is LibraryEntry.Single -> "single_${entry.item.id}"
    is LibraryEntry.Series -> "series_${entry.name}"
}