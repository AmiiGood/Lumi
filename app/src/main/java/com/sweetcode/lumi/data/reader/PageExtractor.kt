package com.sweetcode.lumi.data.reader

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import com.sweetcode.lumi.data.model.MediaFormat
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

    suspend fun extractPages(itemId: String, uri: Uri, format: MediaFormat): List<File> = withContext(Dispatchers.IO) {
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
                MediaFormat.PDF -> extractPdf(uri, target)
                else -> emptyList()
            }
        } catch (e: Exception) {
            target.deleteRecursively()
            emptyList()
        }
    }

    private fun extractPdf(uri: Uri, target: File): List<File> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        val files = mutableListOf<File>()
        pfd.use {
            android.graphics.pdf.PdfRenderer(it).use { renderer ->
                val total = renderer.pageCount
                val padLength = total.toString().length

                for (i in 0 until total) {
                    renderer.openPage(i).use { page ->
                        val scale = 2 // 2x para mejor calidad
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(
                            bitmap,
                            null,
                            null,
                            android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        val pageNum = (i + 1).toString().padStart(padLength, '0')
                        val out = File(target, "page_$pageNum.jpg")
                        out.outputStream().use { fos ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
                        }
                        bitmap.recycle()
                        files.add(out)
                    }
                }
            }
        }
        return files.sortedWith(naturalOrderFile())
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

    suspend fun clearCache(itemId: String) = withContext(Dispatchers.IO) {
        File(pagesRoot, itemId).deleteRecursively()
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