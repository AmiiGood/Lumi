package io.github.amiigood.lumi.lumi.ui.reader

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.amiigood.lumi.lumi.data.local.LumiPreferences
import io.github.amiigood.lumi.lumi.data.model.MediaFormat
import io.github.amiigood.lumi.lumi.data.model.MediaItem
import io.github.amiigood.lumi.lumi.data.model.ReadingMode
import io.github.amiigood.lumi.lumi.data.reader.EpubBookExtractor
import io.github.amiigood.lumi.lumi.data.reader.PageExtractor
import io.github.amiigood.lumi.lumi.data.reader.PageRef
import io.github.amiigood.lumi.lumi.data.reader.PdfPageSource
import io.github.amiigood.lumi.lumi.data.reader.PdfPageSourceFactory
import io.github.amiigood.lumi.lumi.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amiigood.lumi.lumi.data.reader.UnsupportedRarException
import kotlinx.coroutines.GlobalScope
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
        val pages: List<PageRef>,
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
    private val pdfPageSourceFactory: PdfPageSourceFactory,
    private val preferences: LumiPreferences
) : ViewModel() {

    private val itemId: String = savedStateHandle.get<String>("itemId") ?: ""

    private val savedStateKey = "current_page_$itemId"
    private var savedStateHandleRef = savedStateHandle

    private fun getSavedPage(): Int? = savedStateHandleRef.get<Int>(savedStateKey)
    private fun setSavedPage(page: Int) {
        savedStateHandleRef[savedStateKey] = page
    }

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var pdfSource: PdfPageSource? = null

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
                MediaFormat.CBZ, MediaFormat.CBR -> loadImagePages(item)
                MediaFormat.PDF -> loadPdf(item)
                MediaFormat.EPUB -> loadEpub(item)
            }
        }
    }

    private suspend fun loadImagePages(item: MediaItem) {
        try {
            val files = pageExtractor.extractImagePages(item.id, Uri.parse(item.filePath), item.format)
            if (files.isEmpty()) {
                _state.value = ReaderUiState.Error("No se pudieron extraer las páginas")
                return
            }
            val pages = files.map { PageRef.Direct(it) }
            emitImageReady(item, pages)
        } catch (e: UnsupportedRarException) {
            _state.value = ReaderUiState.Error(e.message ?: "Formato no soportado")
        } catch (e: Exception) {
            _state.value = ReaderUiState.Error("Error al abrir: ${e.message}")
        }
    }

    private suspend fun loadPdf(item: MediaItem) {
        val source = pdfPageSourceFactory.create(item.id, Uri.parse(item.filePath))
        pdfSource = source
        val total = source.open()
        if (total == 0) {
            _state.value = ReaderUiState.Error("PDF vacío o inválido")
            return
        }
        val pages = (0 until total).map { idx ->
            PageRef.LazyPdf(item.id, idx, source)
        }
        emitImageReady(item, pages)
    }

    private suspend fun emitImageReady(item: MediaItem, pages: List<PageRef>) {
        val tutorialSeen = preferences.readerTutorialDone.firstOrNull() ?: false
        val mode = preferences.readingMode(item.id).firstOrNull() ?: ReadingMode.PAGED
        val savedPage = getSavedPage()
            ?: preferences.progress(item.id).firstOrNull()
            ?: 0
        _state.value = ReaderUiState.ImageReady(
            item = item,
            pages = pages,
            initialPage = savedPage.coerceIn(0, pages.lastIndex),
            showTutorial = !tutorialSeen,
            mode = mode
        )
    }

    private suspend fun loadEpub(item: MediaItem) {
        val extracted = epubExtractor.extract(item.id, Uri.parse(item.filePath))
        if (extracted == null) {
            _state.value = ReaderUiState.Error("No se pudo abrir el EPUB")
        } else {
            val savedPage = getSavedPage()
                ?: preferences.progress(item.id).firstOrNull()
                ?: 0
            _state.value = ReaderUiState.EpubReady(
                item = item,
                htmlFile = extracted.htmlFile,
                initialProgress = savedPage
            )
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
        setSavedPage(currentPage)
        viewModelScope.launch {
            preferences.setProgress(itemId, currentPage, totalPages)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val source = pdfSource
        if (source != null) {
            GlobalScope.launch { source.close() }
            pdfSource = null
        }
    }
}