package com.sweetcode.lumi.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.data.model.MediaType
import com.sweetcode.lumi.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.update

enum class LibraryFilter(val label: String, val type: MediaType?) {
    ALL("Todos", null),
    MANGA("Manga", MediaType.MANGA),
    COMIC("Cómic", MediaType.COMIC),
    BOOK("Libros", MediaType.BOOK)
}

enum class LibrarySort(val label: String) {
    ALPHABETICAL("Alfabético"),
    RECENTLY_ADDED("Recién agregados"),
    RECENTLY_READ("Recién leídos")
}

sealed class LibraryEntry {
    data class Single(val item: MediaItem) : LibraryEntry()
    data class Series(
        val name: String,
        val type: MediaType,
        val items: List<MediaItem>
    ) : LibraryEntry() {
        val cover: MediaItem get() = items.first()
        val count: Int get() = items.size
    }
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val allItems: List<MediaItem> = emptyList(),
    val featured: MediaItem? = null,
    val continueReading: List<MediaItem> = emptyList(),
    val recentlyAdded: List<MediaItem> = emptyList(),
    val entries: List<LibraryEntry> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.ALPHABETICAL,
    val query: String = "",
    val isSearchOpen: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState(isLoading = true))
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        load(initial = true)
    }

    fun refresh() = load(initial = false)

    fun reloadProgress() {
        viewModelScope.launch {
            repository.reloadProgress()
            _state.value = _state.value.copy(allItems = repository.items.value)
            recomputeAll()
        }
    }

    fun setFilter(filter: LibraryFilter) {
        _state.value = _state.value.copy(filter = filter)
        recomputeEntries()
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
        recomputeEntries()
    }

    fun openSearch() {
        _state.value = _state.value.copy(isSearchOpen = true)
    }

    fun closeSearch() {
        _state.value = _state.value.copy(isSearchOpen = false, query = "")
        recomputeEntries()
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = initial,
                isRefreshing = !initial,
                error = null
            )
            try {
                if (initial) repository.loadIfNeeded() else repository.refresh()
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    allItems = repository.items.value
                )
                recomputeAll()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Error al escanear"
                )
            }
        }
    }

    private fun recomputeAll() {
        val current = _state.value
        val all = current.allItems

        val featured = all
            .filter { it.currentPage > 0 && it.pageCount > 0 && it.currentPage < it.pageCount - 1 }
            .maxByOrNull { it.currentPage }
            ?: all.firstOrNull()

        val continueReading = all
            .filter { it.currentPage > 0 && it.pageCount > 0 && it.currentPage < it.pageCount - 1 }
            .sortedByDescending { it.lastReadAt ?: 0L }
            .take(10)

        val recentlyAdded = all
            .sortedByDescending { it.addedAt }
            .take(10)

        val filtered = all
            .filter { current.filter.type == null || it.type == current.filter.type }
            .filter {
                current.query.isBlank() ||
                        it.title.contains(current.query, ignoreCase = true) ||
                        (it.collection?.contains(current.query, ignoreCase = true) == true)
            }

        val grouped = filtered.groupBy { it.collection }
        val entries = grouped.flatMap { (collection, items) ->
            if (collection != null && items.size > 1) {
                listOf(
                    LibraryEntry.Series(
                        name = collection,
                        type = items.first().type,
                        items = items.sortedBy { it.title }
                    )
                )
            } else {
                items.map { LibraryEntry.Single(it) }
            }
        }

        val sorted = when (current.sort) {
            LibrarySort.ALPHABETICAL -> entries.sortedBy {
                when (it) {
                    is LibraryEntry.Series -> it.name.lowercase()
                    is LibraryEntry.Single -> it.item.title.lowercase()
                }
            }
            LibrarySort.RECENTLY_ADDED -> entries.sortedByDescending {
                when (it) {
                    is LibraryEntry.Series -> it.items.maxOf { item -> item.addedAt }
                    is LibraryEntry.Single -> it.item.addedAt
                }
            }
            LibrarySort.RECENTLY_READ -> entries.sortedByDescending {
                when (it) {
                    is LibraryEntry.Series -> it.items.maxOfOrNull { item -> item.lastReadAt ?: 0L } ?: 0L
                    is LibraryEntry.Single -> it.item.lastReadAt ?: 0L
                }
            }
        }

        _state.update {
            it.copy(
                featured = featured,
                continueReading = continueReading,
                recentlyAdded = recentlyAdded,
                entries = sorted
            )
        }
    }

    private fun recomputeEntries() = recomputeAll()

    fun setSort(sort: LibrarySort) {
        _state.value = _state.value.copy(sort = sort)
        recomputeEntries()
    }
}