package io.github.amiigood.lumi.lumi.data.parser

import android.content.Context
import android.net.Uri
import io.github.amiigood.lumi.lumi.data.model.ItemMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CbzParser @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaParser {

    private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

    override suspend fun extractCover(uri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val firstImageName = listImageNames(uri).firstOrNull() ?: return@withContext false
            extractEntry(uri, firstImageName, outputFile)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun extractMetadata(uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        val pages = listImageNames(uri).size
        val xml = readComicInfoXml(uri)
        val extra = if (xml != null) ComicInfoParser.parse(xml) else ItemMetadata()

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

    private fun listImageNames(uri: Uri): List<String> {
        val names = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.isImage()) {
                        names.add(entry.name)
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return names.sortedWith(naturalOrder())
    }

    private fun readComicInfoXml(uri: Uri): String? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.equals("ComicInfo.xml", ignoreCase = true)) {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    private fun extractEntry(uri: Uri, entryName: String, outputFile: File): Boolean {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == entryName) {
                        outputFile.outputStream().use { out -> zip.copyTo(out) }
                        return true
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return false
    }

    private fun String.isImage() = imageExtensions.any { this.lowercase().endsWith(it) }

    private fun naturalOrder(): Comparator<String> = Comparator { a, b ->
        val regex = Regex("(\\d+)|(\\D+)")
        val ta = regex.findAll(a).toList()
        val tb = regex.findAll(b).toList()
        val len = minOf(ta.size, tb.size)
        for (i in 0 until len) {
            val sa = ta[i].value
            val sb = tb[i].value
            val cmp = if (sa.first().isDigit() && sb.first().isDigit()) {
                sa.toLong().compareTo(sb.toLong())
            } else {
                sa.compareTo(sb)
            }
            if (cmp != 0) return@Comparator cmp
        }
        ta.size - tb.size
    }
}