package com.sweetcode.lumi.data.parser

import android.content.Context
import android.net.Uri
import com.sweetcode.lumi.data.model.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cbzParser: CbzParser,
    private val cbrParser: CbrParser,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser
) {

    private val mutex = Mutex()
    private val coverDir: File by lazy {
        File(context.cacheDir, "covers").apply { mkdirs() }
    }

    suspend fun getOrExtractCover(itemId: String, uri: Uri, format: MediaFormat): File? = withContext(Dispatchers.IO) {
        val target = File(coverDir, "$itemId.jpg")
        if (target.exists() && target.length() > 0) return@withContext target

        mutex.withLock {
            if (target.exists() && target.length() > 0) return@withLock target
            val parser = when (format) {
                MediaFormat.CBZ -> cbzParser
                MediaFormat.CBR -> cbrParser
                MediaFormat.EPUB -> epubParser
                MediaFormat.PDF -> pdfParser
            }
            val ok = parser.extractCover(uri, target)
            if (ok && target.length() > 0) target else null
        }
    }
}