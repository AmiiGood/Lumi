package com.sweetcode.lumi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sweetcode.lumi.data.model.ItemMetadata
import com.sweetcode.lumi.data.model.MediaFormat
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.data.model.MediaType

@Entity(tableName = "media_items")
@TypeConverters(MediaConverters::class)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val filePath: String,
    val format: MediaFormat,
    val type: MediaType,
    val collection: String?,
    val pageCount: Int,
    val addedAt: Long,
    // Metadata aplanada
    val series: String?,
    val number: String?,
    val volume: String?,
    val summary: String?,
    val publisher: String?,
    val year: Int?,
    val genres: String,
    val language: String?,
    val isbn: String?,
    val writer: String?,
    val artist: String?
)

fun MediaItem.toEntity(): MediaItemEntity = MediaItemEntity(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    filePath = filePath,
    format = format,
    type = type,
    collection = collection,
    pageCount = pageCount,
    addedAt = addedAt,
    series = metadata.series,
    number = metadata.number,
    volume = metadata.volume,
    summary = metadata.summary,
    publisher = metadata.publisher,
    year = metadata.year,
    genres = metadata.genres.joinToString("|"),
    language = metadata.language,
    isbn = metadata.isbn,
    writer = metadata.writer,
    artist = metadata.artist
)

fun MediaItemEntity.toDomain(currentPage: Int = 0, lastReadAt: Long? = null): MediaItem = MediaItem(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    filePath = filePath,
    format = format,
    type = type,
    collection = collection,
    pageCount = pageCount,
    currentPage = currentPage,
    lastReadAt = lastReadAt,
    addedAt = addedAt,
    metadata = ItemMetadata(
        series = series,
        number = number,
        volume = volume,
        summary = summary,
        publisher = publisher,
        year = year,
        genres = if (genres.isEmpty()) emptyList() else genres.split("|"),
        language = language,
        isbn = isbn,
        writer = writer,
        artist = artist
    )
)

class MediaConverters {
    @TypeConverter
    fun fromFormat(value: MediaFormat): String = value.name

    @TypeConverter
    fun toFormat(value: String): MediaFormat = MediaFormat.valueOf(value)

    @TypeConverter
    fun fromType(value: MediaType): String = value.name

    @TypeConverter
    fun toType(value: String): MediaType = MediaType.valueOf(value)
}