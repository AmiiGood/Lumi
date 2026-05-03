package com.sweetcode.lumi.data.parser

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import com.sweetcode.lumi.data.model.ItemMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CbrParser @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaParser {

    private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

    override suspend fun extractCover(uri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyUriToTempFile(uri) ?: return@withContext false
            try {
                Archive(tempFile).use { archive ->
                    val firstImage = archive.fileHeaders
                        .filter { !it.isDirectory && it.fileName.isImage() }
                        .sortedWith(compareBy { it.fileName })
                        .firstOrNull() ?: return@withContext false

                    outputFile.outputStream().use { out ->
                        archive.extractFile(firstImage, out)
                    }
                    true
                }
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun extractMetadata(uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        val tempFile = copyUriToTempFile(uri) ?: return@withContext ParsedMetadata(null, null, 0)
        try {
            Archive(tempFile).use { archive ->
                val pages = archive.fileHeaders.count { !it.isDirectory && it.fileName.isImage() }
                val xmlHeader = archive.fileHeaders.firstOrNull {
                    !it.isDirectory && it.fileName.equals("ComicInfo.xml", ignoreCase = true)
                }
                val extra = if (xmlHeader != null) {
                    val out = ByteArrayOutputStream()
                    archive.extractFile(xmlHeader, out)
                    ComicInfoParser.parse(out.toString("UTF-8"))
                } else ItemMetadata()

                val title = listOfNotNull(extra.series, extra.number?.let { "#$it" })
                    .joinToString(" ")
                    .ifBlank { null }

                ParsedMetadata(
                    title = title,
                    author = extra.writer ?: extra.artist,
                    pageCount = pages,
                    extra = extra
                )
            }
        } catch (e: Exception) {
            ParsedMetadata(null, null, 0)
        } finally {
            tempFile.delete()
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val temp = File.createTempFile("cbr_", ".rar", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { out -> input.copyTo(out) }
            } ?: return null
            temp
        } catch (e: Exception) {
            null
        }
    }

    private fun String.isImage() = imageExtensions.any { this.lowercase().endsWith(it) }
}