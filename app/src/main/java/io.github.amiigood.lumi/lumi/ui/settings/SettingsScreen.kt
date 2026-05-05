package io.github.amiigood.lumi.lumi.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.amiigood.lumi.lumi.data.model.ThemeMode
import androidx.compose.foundation.layout.size
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.ui.platform.LocalContext
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.changeFolder(uri)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { SectionTitle("Biblioteca") }

            item {
                SettingRow(
                    icon = Icons.Rounded.Folder,
                    title = "Carpeta de la biblioteca",
                    subtitle = state.rootFolderUri?.let { decodeUri(it) } ?: "No seleccionada",
                    onClick = { folderPicker.launch(null) }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Rounded.Refresh,
                    title = "Refrescar biblioteca",
                    subtitle = "Volver a escanear la carpeta",
                    trailing = {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    onClick = { viewModel.refreshLibrary() }
                )
            }

            item { Divider() }
            item { SectionTitle("Apariencia") }

            item {
                SettingRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "Tema",
                    subtitle = state.themeMode.label,
                    trailing = {}
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            AssistChip(
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.label) },
                                colors = if (state.themeMode == mode) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        labelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Material You",
                        subtitle = "Usar colores de tu fondo de pantalla",
                        trailing = {
                            Switch(
                                checked = state.dynamicColor,
                                onCheckedChange = { viewModel.toggleDynamicColor() }
                            )
                        },
                        onClick = { viewModel.toggleDynamicColor() }
                    )
                }
            }

            item { Divider() }
            item { SectionTitle("Almacenamiento") }

            item {
                SettingRow(
                    icon = Icons.Rounded.Settings,
                    title = "Espacio usado en cache",
                    subtitle = formatBytes(state.cacheSizeBytes),
                    onClick = null
                )
            }

            item {
                SettingRow(
                    icon = Icons.Rounded.Image,
                    title = "Limpiar portadas",
                    subtitle = "Las portadas se regenerarán al volver a la biblioteca",
                    onClick = { viewModel.clearCovers() }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Rounded.CleaningServices,
                    title = "Limpiar cache del lector",
                    subtitle = "Las páginas se extraerán de nuevo al abrir",
                    onClick = { viewModel.clearReaderCache() }
                )
            }

            item { Divider() }
            item { SectionTitle("Acerca de Lumi") }

            item {
                val context = LocalContext.current
                SettingRow(
                    icon = Icons.Rounded.Favorite,
                    title = "Apoyar el desarrollo",
                    subtitle = "Lumi es gratis y sin anuncios. Si te gusta, considera invitarme un café",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/amiigood"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                val context = LocalContext.current
                SettingRow(
                    icon = Icons.Rounded.Mail,
                    title = "Contacto",
                    subtitle = "alexisalvarez1234563@outlook.com",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:alexisalvarez1234563@outlook.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Lumi - ")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                val context = LocalContext.current
                SettingRow(
                    icon = Icons.Rounded.Code,
                    title = "Versión",
                    subtitle = "1.0.2",
                    onClick = null
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    val rowMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowMod)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            trailing?.invoke()
        }
        extraContent?.invoke()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024 * 1024)} MB"
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

private fun decodeUri(uri: String): String {
    val tree = uri.substringAfter("tree/", uri)
    val decoded = runCatching { URLDecoder.decode(tree, "UTF-8") }.getOrDefault(tree)
    return decoded.substringAfterLast(':').substringAfterLast('/')
}