package io.github.amiigood.lumi.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.amiigood.lumi.lumi.data.model.MediaFormat
import io.github.amiigood.lumi.lumi.data.parser.CoverManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import java.io.File

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoverManagerEntryPoint {
    fun coverManager(): CoverManager
}

@Composable
fun CoverImage(
    itemId: String,
    fileUri: String,
    format: MediaFormat,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coverManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CoverManagerEntryPoint::class.java
        ).coverManager()
    }

    var coverFile by remember(itemId) { mutableStateOf<File?>(null) }
    var failed by remember(itemId) { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        coverFile = null
        failed = false
        val file = coverManager.getOrExtractCover(itemId, Uri.parse(fileUri), format)
        if (file != null) coverFile = file else failed = true
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverFile != null -> AsyncImage(
                model = coverFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            failed -> Icon(
                imageVector = Icons.Rounded.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            else -> Icon(
                imageVector = Icons.Rounded.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}