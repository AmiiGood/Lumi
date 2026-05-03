package com.sweetcode.lumi.util

import com.sweetcode.lumi.data.model.MediaFormat
import com.sweetcode.lumi.data.model.MediaType

object FormatDetector {

    fun detectFormat(fileName: String): MediaFormat? {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".cbz") -> MediaFormat.CBZ
            lower.endsWith(".cbr") -> MediaFormat.CBR
            lower.endsWith(".epub") -> MediaFormat.EPUB
            lower.endsWith(".pdf") -> MediaFormat.PDF
            else -> null
        }
    }

    fun inferTypeFromPath(parentFolders: List<String>, format: MediaFormat): MediaType {
        val joined = parentFolders.joinToString("/").lowercase()
        return when {
            joined.contains("manga") -> MediaType.MANGA
            joined.contains("comic") -> MediaType.COMIC
            joined.contains("libro") || joined.contains("book") -> MediaType.BOOK
            format == MediaFormat.EPUB -> MediaType.BOOK
            format == MediaFormat.CBZ || format == MediaFormat.CBR -> MediaType.COMIC
            else -> MediaType.UNKNOWN
        }
    }
}