package com.sweetcode.lumi.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val name: String = "",
    val items: List<MediaItem> = emptyList()
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LibraryRepository
) : ViewModel() {

    private val name: String = java.net.URLDecoder.decode(
        savedStateHandle.get<String>("collectionName") ?: "",
        "UTF-8"
    )

    private val _state = MutableStateFlow(CollectionDetailUiState(name = name))
    val state: StateFlow<CollectionDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun reloadProgress() {
        viewModelScope.launch {
            repository.reloadProgress()
            load()
        }
    }

    private fun load() {
        _state.value = CollectionDetailUiState(
            name = name,
            items = repository.getCollection(name)
        )
    }
}