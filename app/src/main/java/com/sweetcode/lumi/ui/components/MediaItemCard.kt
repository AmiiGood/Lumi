package com.sweetcode.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.ui.library.LibraryEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

@Composable
fun LibraryEntryCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (entry) {
        is LibraryEntry.Single -> SingleCard(entry.item, onClick, modifier)
        is LibraryEntry.Series -> SeriesCard(entry, onClick, modifier)
    }
}

@Composable
private fun SingleCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            CoverImage(
                itemId = item.id,
                fileUri = item.filePath,
                format = item.format,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
            )
            if (item.pageCount > 0 && item.currentPage > 0) {
                ProgressBar(
                    progress = (item.currentPage + 1).toFloat() / item.pageCount.toFloat(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeriesCard(
    series: LibraryEntry.Series,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val readCount = series.items.count { it.currentPage > 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            CoverImage(
                itemId = series.cover.id,
                fileUri = series.cover.filePath,
                format = series.cover.format,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${series.count}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (readCount > 0) {
                ProgressBar(
                    progress = readCount.toFloat() / series.count.toFloat(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = series.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (readCount > 0) "$readCount/${series.count} leídos" else "${series.count} tomos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}