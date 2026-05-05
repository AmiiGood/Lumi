package com.sweetcode.lumi.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sweetcode.lumi.data.model.MediaFormat
import com.sweetcode.lumi.data.model.MediaItem
import com.sweetcode.lumi.data.parser.CbrParser
import com.sweetcode.lumi.data.parser.CbzParser
import com.sweetcode.lumi.data.parser.EpubParser
import com.sweetcode.lumi.data.parser.MediaParser
import com.sweetcode.lumi.data.parser.PdfParser
import com.sweetcode.lumi.util.FormatDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

private data class ScannedFile(
    val docFile: DocumentFile,
    val format: MediaFormat,
    val parentFolders: List<String>
)

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cbzParser: CbzParser,
    private val cbrParser: CbrParser,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser
) {

    suspend fun scan(rootUri: Uri): List<MediaItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        val scanned = mutableListOf<ScannedFile>()
        walk(root, emptyList(), scanned)

        scanned.map { it.toMediaItem() }
    }

    private suspend fun walk(
        dir: DocumentFile,
        parents: List<String>,
        out: MutableList<ScannedFile>
    ) {
        coroutineContext.ensureActive()
        for (child in dir.listFiles()) {
            coroutineContext.ensureActive()
            when {
                child.isDirectory -> {
                    val newParents = parents + (child.name ?: "")
                    walk(child, newParents, out)
                }
                child.isFile -> {
                    val name = child.name ?: continue
                    val format = FormatDetector.detectFormat(name) ?: continue
                    out.add(ScannedFile(child, format, parents))
                }
            }
        }
    }

    private suspend fun ScannedFile.toMediaItem(): MediaItem {
        coroutineContext.ensureActive()
        val name = docFile.name ?: "Untitled"
        val fallbackTitle = name.substringBeforeLast('.')
        val type = FormatDetector.inferTypeFromPath(parentFolders, format)
        val parser: MediaParser = when (format) {
            MediaFormat.CBZ -> cbzParser
            MediaFormat.CBR -> cbrParser
            MediaFormat.EPUB -> epubParser
            MediaFormat.PDF -> pdfParser
        }

        val parsed = runCatching { parser.extractMetadata(docFile.uri) }
            .getOrNull()

        val collection = parsed?.extra?.series ?: parentFolders.lastOrNull()

        return MediaItem(
            id = hashId(docFile.uri.toString()),
            title = parsed?.title ?: fallbackTitle,
            author = parsed?.author,
            coverPath = null,
            filePath = docFile.uri.toString(),
            format = format,
            type = type,
            collection = collection,
            pageCount = parsed?.pageCount ?: 0,
            currentPage = 0,
            lastReadAt = null,
            addedAt = System.currentTimeMillis(),
            metadata = parsed?.extra ?: com.sweetcode.lumi.data.model.ItemMetadata()
        )
    }

    private fun hashId(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}