package com.sweetcode.lumi.data.repository

import android.net.Uri
import com.sweetcode.lumi.data.local.LumiPreferences
import com.sweetcode.lumi.data.local.MediaItemDao
import com.sweetcode.lumi.data.local.toDomain
import com.sweetcode.lumi.data.local.toEntity
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.data.scanner.LibraryScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val scanner: LibraryScanner,
    private val preferences: LumiPreferences,
    private val dao: MediaItemDao
) {
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    suspend fun loadIfNeeded() {
        if (_items.value.isNotEmpty()) return

        val cached = dao.getAll()
        if (cached.isNotEmpty()) {
            _items.value = cached.map { entity ->
                val savedPage = preferences.progress(entity.id).firstOrNull() ?: 0
                val savedTotal = preferences.totalPages(entity.id).firstOrNull() ?: 0
                val item = entity.toDomain(currentPage = savedPage)
                if (savedTotal > 0) item.copy(pageCount = savedTotal) else item
            }
        } else {
            refresh()
        }
    }

    suspend fun refresh() {
        val uriString = preferences.rootFolderUri.firstOrNull() ?: return
        val scanned = scanner.scan(Uri.parse(uriString))

        dao.replaceAll(scanned.map { it.toEntity() })

        val withProgress = scanned.map { item ->
            val savedPage = preferences.progress(item.id).firstOrNull() ?: 0
            val savedTotal = preferences.totalPages(item.id).firstOrNull() ?: 0
            item.copy(
                currentPage = savedPage,
                pageCount = if (savedTotal > 0) savedTotal else item.pageCount
            )
        }
        _items.value = withProgress
    }

    suspend fun reloadProgress() {
        val current = _items.value
        if (current.isEmpty()) return
        val updated = current.map { item ->
            val savedPage = preferences.progress(item.id).firstOrNull() ?: 0
            val savedTotal = preferences.totalPages(item.id).firstOrNull() ?: 0
            item.copy(
                currentPage = savedPage,
                pageCount = if (savedTotal > 0) savedTotal else item.pageCount
            )
        }
        _items.value = updated
    }

    fun getItem(id: String): MediaItem? = _items.value.find { it.id == id }

    fun getCollection(name: String): List<MediaItem> =
        _items.value.filter { it.collection == name }.sortedBy { it.title }
}