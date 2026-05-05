package io.github.amiigood.lumi.lumi.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.amiigood.lumi.lumi.data.model.MediaItem
import io.github.amiigood.lumi.lumi.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: LibraryRepository
) : ViewModel() {

    val item: MediaItem? = savedStateHandle.get<String>("itemId")?.let { repository.getItem(it) }
}