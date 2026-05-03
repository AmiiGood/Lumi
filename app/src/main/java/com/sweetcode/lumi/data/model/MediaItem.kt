package com.sweetcode.lumi.data.model

enum class MediaType {
    MANGA,
    COMIC,
    BOOK,
    UNKNOWN
}

enum class MediaFormat {
    CBZ,
    CBR,
    EPUB,
    PDF
}

data class MediaItem(
    val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val filePath: String,
    val format: MediaFormat,
    val type: MediaType,
    val collection: String?,
    val pageCount: Int,
    val currentPage: Int,
    val lastReadAt: Long?,
    val addedAt: Long,
    val metadata: ItemMetadata = ItemMetadata()
)

data class ItemMetadata(
    val series: String? = null,
    val number: String? = null,
    val volume: String? = null,
    val summary: String? = null,
    val publisher: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val language: String? = null,
    val isbn: String? = null,
    val writer: String? = null,
    val artist: String? = null
)