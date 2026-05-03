package com.sweetcode.lumi.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.ui.components.CoverImage
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    onRead: (String) -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val item = viewModel.item
    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("No se encontró el item", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { HeroSection(item = item) }
            item { Spacer(Modifier.height(16.dp)) }
            item { ActionRow(onRead = { onRead(item.id) }) }
            item { Spacer(Modifier.height(24.dp)) }
            if (!item.metadata.summary.isNullOrBlank()) {
                item { SummarySection(item.metadata.summary) }
                item { Spacer(Modifier.height(24.dp)) }
            }
            if (item.metadata.genres.isNotEmpty()) {
                item { GenresSection(item.metadata.genres) }
                item { Spacer(Modifier.height(24.dp)) }
            }
            item { InfoSection(item) }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp)
                .size(40.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HeroSection(item: MediaItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        CoverImage(
            itemId = item.id,
            fileUri = item.filePath,
            format = item.format,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.0f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.author.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.author,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionRow(onRead: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onRead,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Leer", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SummarySection(summary: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Sinopsis",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (expanded) "Ver menos" else "Ver más",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun GenresSection(genres: List<String>) {
    Column {
        Text(
            text = "Géneros",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                AssistChip(
                    onClick = {},
                    label = { Text(genre) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoSection(item: MediaItem) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Detalles",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        InfoRow("Formato", item.format.name)
        if (item.pageCount > 0) InfoRow("Páginas", item.pageCount.toString())
        item.metadata.publisher?.let { InfoRow("Editorial", it) }
        item.metadata.year?.let { InfoRow("Año", it.toString()) }
        item.metadata.language?.let { InfoRow("Idioma", it.uppercase()) }
        item.metadata.volume?.let { InfoRow("Volumen", it) }
        item.metadata.number?.let { InfoRow("Número", it) }
        item.metadata.isbn?.let { InfoRow("ISBN", it) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}