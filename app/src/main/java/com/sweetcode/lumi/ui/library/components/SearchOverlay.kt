package com.sweetcode.lumi.ui.library.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.sweetcode.lumi.ui.components.EmptyState
import com.sweetcode.lumi.ui.components.LibraryEntryCard
import com.sweetcode.lumi.ui.library.LibraryEntry

@Composable
fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    entries: List<LibraryEntry>,
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Cerrar búsqueda",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Buscar en tu biblioteca") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                query.isBlank() -> Text(
                    text = "Empieza a escribir para buscar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .padding(24.dp)
                )
                entries.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "Sin resultados",
                    description = "No encontramos nada para \"$query\""
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp, bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(entries, key = { entryKey(it) }) { entry ->
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
    }
}

private fun entryKey(entry: LibraryEntry): String = when (entry) {
    is LibraryEntry.Single -> "single_${entry.item.id}"
    is LibraryEntry.Series -> "series_${entry.name}"
}