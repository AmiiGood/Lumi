package io.github.amiigood.lumi.lumi.data.parser

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amiigood.lumi.lumi.data.model.ItemMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubParser @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaParser {

    private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

    override suspend fun extractCover(uri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val opf = readOpfContent(uri) ?: return@withContext false
            val coverHref = findCoverHref(opf) ?: findFirstImage(uri) ?: return@withContext false
            extractEntry(uri, coverHref, outputFile)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun extractMetadata(uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        try {
            val opf = readOpfContent(uri) ?: return@withContext ParsedMetadata(null, null, 0)
            val title = Regex("<dc:title[^>]*>([^<]+)</dc:title>").find(opf)?.groupValues?.get(1)?.trim()
            val author = Regex("<dc:creator[^>]*>([^<]+)</dc:creator>").find(opf)?.groupValues?.get(1)?.trim()
            val publisher = Regex("<dc:publisher[^>]*>([^<]+)</dc:publisher>").find(opf)?.groupValues?.get(1)?.trim()
            val description = Regex("<dc:description[^>]*>([\\s\\S]*?)</dc:description>")
                .find(opf)?.groupValues?.get(1)?.trim()
                ?.replace(Regex("<[^>]+>"), "")
            val language = Regex("<dc:language[^>]*>([^<]+)</dc:language>").find(opf)?.groupValues?.get(1)?.trim()
            val date = Regex("<dc:date[^>]*>([^<]+)</dc:date>").find(opf)?.groupValues?.get(1)?.trim()
            val isbn = Regex("<dc:identifier[^>]*>([^<]*(?:isbn|ISBN)[^<]*)</dc:identifier>")
                .find(opf)?.groupValues?.get(1)
                ?.replace(Regex("[^0-9X]"), "")
                ?.takeIf { it.length in 10..13 }
            val genres = Regex("<dc:subject[^>]*>([^<]+)</dc:subject>")
                .findAll(opf)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotEmpty() }
                .toList()

            ParsedMetadata(
                title = title,
                author = author,
                pageCount = 0,
                extra = ItemMetadata(
                    summary = description,
                    publisher = publisher,
                    year = date?.take(4)?.toIntOrNull(),
                    genres = genres,
                    language = language,
                    isbn = isbn,
                    writer = author
                )
            )
        } catch (e: Exception) {
            ParsedMetadata(null, null, 0)
        }
    }

    private fun readOpfContent(uri: Uri): String? {
        var opfPath: String? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "META-INF/container.xml") {
                        val content = zip.readBytes().toString(Charsets.UTF_8)
                        opfPath = Regex("full-path=\"([^\"]+)\"").find(content)?.groupValues?.get(1)
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        }
        if (opfPath == null) return null

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == opfPath) {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    private fun findCoverHref(opf: String): String? {
        val coverId = Regex("<meta[^>]*name=\"cover\"[^>]*content=\"([^\"]+)\"").find(opf)?.groupValues?.get(1)
        if (coverId != null) {
            val href = Regex("<item[^>]*id=\"$coverId\"[^>]*href=\"([^\"]+)\"").find(opf)?.groupValues?.get(1)
            if (href != null) return href
        }
        return Regex("<item[^>]*href=\"([^\"]+\\.(?:jpg|jpeg|png))\"[^>]*properties=\"cover-image\"")
            .find(opf)?.groupValues?.get(1)
    }

    private fun findFirstImage(uri: Uri): String? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && imageExtensions.any { entry!!.name.lowercase().endsWith(it) }) {
                        return entry.name
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
                    val cleanName = entry.name.substringAfter('/', entry.name)
                    if (entry.name == entryName || entry.name.endsWith("/$entryName") || cleanName == entryName) {
                        outputFile.outputStream().use { out -> zip.copyTo(out) }
                        return true
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return false
    }
}