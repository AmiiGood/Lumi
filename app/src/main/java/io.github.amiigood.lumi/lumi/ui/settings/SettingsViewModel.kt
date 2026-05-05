package io.github.amiigood.lumi.lumi.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.amiigood.lumi.lumi.data.CacheCleaner
import io.github.amiigood.lumi.lumi.data.local.LumiPreferences
import io.github.amiigood.lumi.lumi.data.local.MediaItemDao
import io.github.amiigood.lumi.lumi.data.model.ThemeMode
import io.github.amiigood.lumi.lumi.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val rootFolderUri: String? = null,
    val cacheSizeBytes: Long = 0,
    val isRefreshing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: LumiPreferences,
    private val repository: LibraryRepository,
    private val cacheCleaner: CacheCleaner,
    private val dao: MediaItemDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientState())

    val state: StateFlow<SettingsUiState> = combine(
        preferences.themeModeEnum,
        preferences.dynamicColor,
        preferences.rootFolderUri,
        transientState
    ) { theme, dynamic, uri, transient ->
        SettingsUiState(
            themeMode = theme,
            dynamicColor = dynamic,
            rootFolderUri = uri,
            cacheSizeBytes = transient.cacheSize,
            isRefreshing = transient.isRefreshing,
            message = transient.message
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        viewModelScope.launch { recalcCacheSize() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeModeEnum(mode) }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            val current = preferences.dynamicColor.firstOrNull() ?: false
            preferences.setDynamicColor(!current)
        }
    }

    fun changeFolder(uri: Uri) {
        viewModelScope.launch {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            dao.deleteAll()
            preferences.setRootFolderUri(uri.toString())
            transientState.value = transientState.value.copy(message = "Carpeta cambiada. Refrescando…")
            repository.refresh()
            transientState.value = transientState.value.copy(message = "Biblioteca actualizada")
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            transientState.value = transientState.value.copy(isRefreshing = true, message = null)
            try {
                repository.refresh()
                transientState.value = transientState.value.copy(message = "Biblioteca actualizada")
            } catch (e: Exception) {
                transientState.value = transientState.value.copy(message = "Error: ${e.message}")
            } finally {
                transientState.value = transientState.value.copy(isRefreshing = false)
            }
        }
    }

    fun clearCovers() {
        viewModelScope.launch {
            cacheCleaner.clearCovers()
            repository.reloadProgress()
            recalcCacheSize()
            transientState.value = transientState.value.copy(message = "Portadas limpiadas")
        }
    }

    fun clearReaderCache() {
        viewModelScope.launch {
            cacheCleaner.clearExtractedPages()
            recalcCacheSize()
            transientState.value = transientState.value.copy(message = "Cache del lector limpiado")
        }
    }

    fun consumeMessage() {
        transientState.value = transientState.value.copy(message = null)
    }

    private suspend fun recalcCacheSize() {
        transientState.value = transientState.value.copy(cacheSize = cacheCleaner.cacheSize())
    }

    private data class TransientState(
        val cacheSize: Long = 0,
        val isRefreshing: Boolean = false,
        val message: String? = null
    )
}