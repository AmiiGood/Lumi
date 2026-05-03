package com.sweetcode.lumi.ui.reader

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweetcode.lumi.data.local.LumiPreferences
import com.sweetcode.lumi.data.model.MediaFormat
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.data.model.ReadingMode
import com.sweetcode.lumi.data.reader.EpubBookExtractor
import com.sweetcode.lumi.data.reader.PageExtractor
import com.sweetcode.lumi.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ReaderUiState {
    data object Loading : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
    data class ImageReady(
        val item: MediaItem,
        val pages: List<File>,
        val initialPage: Int,
        val showTutorial: Boolean,
        val mode: ReadingMode
    ) : ReaderUiState()
    data class EpubReady(
        val item: MediaItem,
        val htmlFile: File,
        val initialProgress: Int
    ) : ReaderUiState()
    data class Unsupported(val format: MediaFormat) : ReaderUiState()
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LibraryRepository,
    private val pageExtractor: PageExtractor,
    private val epubExtractor: EpubBookExtractor,
    private val preferences: LumiPreferences
) : ViewModel() {

    private val itemId: String = savedStateHandle.get<String>("itemId") ?: ""

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val item = repository.getItem(itemId)
            if (item == null) {
                _state.value = ReaderUiState.Error("Item no encontrado")
                return@launch
            }

            when (item.format) {
                MediaFormat.CBZ, MediaFormat.CBR, MediaFormat.PDF -> {
                    val pages = pageExtractor.extractPages(item.id, Uri.parse(item.filePath), item.format)
                    if (pages.isEmpty()) {
                        _state.value = ReaderUiState.Error("No se pudieron extraer las páginas")
                    } else {
                        val tutorialSeen = preferences.readerTutorialDone.firstOrNull() ?: false
                        val mode = preferences.readingMode(item.id).firstOrNull() ?: ReadingMode.PAGED
                        _state.value = ReaderUiState.ImageReady(
                            item = item,
                            pages = pages,
                            initialPage = (preferences.progress(item.id).firstOrNull() ?: 0).coerceIn(0, pages.lastIndex),
                            showTutorial = !tutorialSeen,
                            mode = mode
                        )
                    }
                }
                MediaFormat.EPUB -> {
                    val extracted = epubExtractor.extract(item.id, Uri.parse(item.filePath))
                    if (extracted == null) {
                        _state.value = ReaderUiState.Error("No se pudo abrir el EPUB")
                    } else {
                        _state.value = ReaderUiState.EpubReady(
                            item = item,
                            htmlFile = extracted.htmlFile,
                            initialProgress = preferences.progress(item.id).firstOrNull() ?: 0
                        )
                    }
                }
            }
        }
    }

    fun markTutorialSeen() {
        viewModelScope.launch {
            preferences.setReaderTutorialDone(true)
        }
    }

    fun toggleMode() {
        val current = _state.value
        if (current is ReaderUiState.ImageReady) {
            val newMode = if (current.mode == ReadingMode.PAGED) ReadingMode.WEBTOON else ReadingMode.PAGED
            _state.update { current.copy(mode = newMode) }
            viewModelScope.launch {
                preferences.setReadingMode(current.item.id, newMode)
            }
        }
    }

    fun saveProgress(currentPage: Int, totalPages: Int) {
        viewModelScope.launch {
            preferences.setProgress(itemId, currentPage, totalPages)
        }
    }
}