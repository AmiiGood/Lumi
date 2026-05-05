package io.github.amiigood.lumi.lumi.data.reader

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import io.github.amiigood.lumi.lumi.data.model.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

    private val pagesRoot: File by lazy {
        File(context.cacheDir, "pages").apply { mkdirs() }
    }

    suspend fun extractImagePages(itemId: String, uri: Uri, format: MediaFormat): List<File> = withContext(Dispatchers.IO) {
        val target = File(pagesRoot, itemId)

        if (target.exists() && target.listFiles()?.isNotEmpty() == true) {
            return@withContext target.listFiles()!!
                .filter { it.isFile }
                .sortedWith(naturalOrderFile())
        }

        target.mkdirs()
        try {
            when (format) {
                MediaFormat.CBZ -> extractCbz(uri, target)
                MediaFormat.CBR -> extractCbr(uri, target)
                else -> emptyList()
            }
        } catch (e: UnsupportedRarException) {
            target.deleteRecursively()
            throw e
        } catch (e: Exception) {
            target.deleteRecursively()
            emptyList()
        }
    }

    private fun extractCbz(uri: Uri, target: File): List<File> {
        val files = mutableListOf<File>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.isImage()) {
                        val safeName = entry.name.replace('/', '_').replace('\\', '_')
                        val out = File(target, safeName)
                        out.outputStream().use { fos -> zip.copyTo(fos) }
                        files.add(out)
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return files.sortedWith(naturalOrderFile())
    }

    private fun extractCbr(uri: Uri, target: File): List<File> {
        val temp = File.createTempFile("cbr_", ".rar", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { out -> input.copyTo(out) }
            } ?: return emptyList()

            if (isRar5(temp)) {
                throw UnsupportedRarException()
            }

            val files = mutableListOf<File>()
            Archive(temp).use { archive ->
                archive.fileHeaders
                    .filter { !it.isDirectory && it.fileName.isImage() }
                    .forEach { header ->
                        val safeName = header.fileName.replace('/', '_').replace('\\', '_')
                        val out = File(target, safeName)
                        out.outputStream().use { fos ->
                            archive.extractFile(header, fos)
                        }
                        files.add(out)
                    }
            }
            return files.sortedWith(naturalOrderFile())
        } finally {
            temp.delete()
        }
    }

    private fun isRar5(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val sig = ByteArray(8)
                if (input.read(sig) < 8) return false
                // RAR 5.0: 52 61 72 21 1A 07 01 00
                sig[0] == 0x52.toByte() && sig[1] == 0x61.toByte() &&
                        sig[2] == 0x72.toByte() && sig[3] == 0x21.toByte() &&
                        sig[4] == 0x1A.toByte() && sig[5] == 0x07.toByte() &&
                        sig[6] == 0x01.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearCache(itemId: String) = withContext(Dispatchers.IO) {
        File(pagesRoot, itemId).deleteRecursively()
        File(context.cacheDir, "pdf_pages/$itemId").deleteRecursively()
    }

    private fun String.isImage() = imageExtensions.any { this.lowercase().endsWith(it) }

    private fun naturalOrderFile(): Comparator<File> = Comparator { a, b ->
        naturalOrder().compare(a.name, b.name)
    }

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

class UnsupportedRarException : Exception("RAR5 no soportado. Convierte el archivo a CBZ o RAR4.")