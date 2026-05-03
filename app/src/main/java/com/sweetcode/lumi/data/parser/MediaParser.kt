package com.sweetcode.lumi.data.parser

import android.net.Uri
import com.sweetcode.lumi.data.model.ItemMetadata
import java.io.File

data class ParsedMetadata(
    val title: String?,
    val author: String?,
    val pageCount: Int,
    val extra: ItemMetadata = ItemMetadata()
)

interface MediaParser {
    suspend fun extractCover(uri: Uri, outputFile: File): Boolean
    suspend fun extractMetadata(uri: Uri): ParsedMetadata
}