package io.github.amiigood.lumi.lumi.data.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class PdfPageSource(
    private val context: Context,
    val itemId: String,
    private val uri: Uri
) {
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()

    var pageCount: Int = 0
        private set

    private val cacheDir: File by lazy {
        File(context.cacheDir, "pdf_pages/$itemId").apply { mkdirs() }
    }

    suspend fun open(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (renderer == null) {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                renderer = pfd?.let { PdfRenderer(it) }
                pageCount = renderer?.pageCount ?: 0
            }
            pageCount
        }
    }

    suspend fun getPage(index: Int): File? = withContext(Dispatchers.IO) {
        val cached = File(cacheDir, "page_$index.jpg")
        if (cached.exists() && cached.length() > 0) return@withContext cached

        mutex.withLock {
            if (cached.exists() && cached.length() > 0) return@withLock cached
            val r = renderer ?: return@withLock null
            if (index !in 0 until r.pageCount) return@withLock null

            try {
                r.openPage(index).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cached.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    bitmap.recycle()
                    cached
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            renderer?.close()
            pfd?.close()
            renderer = null
            pfd = null
        }
    }
}

@Singleton
class PdfPageSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(itemId: String, uri: Uri) = PdfPageSource(context, itemId, uri)
}