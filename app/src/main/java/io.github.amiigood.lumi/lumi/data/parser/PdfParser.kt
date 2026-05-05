package io.github.amiigood.lumi.lumi.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfParser @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaParser {

    override suspend fun extractCover(uri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            openRenderer(uri)?.use { renderer ->
                if (renderer.pageCount == 0) return@withContext false
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    outputFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    bitmap.recycle()
                    true
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun extractMetadata(uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        try {
            openRenderer(uri)?.use { renderer ->
                ParsedMetadata(title = null, author = null, pageCount = renderer.pageCount)
            } ?: ParsedMetadata(null, null, 0)
        } catch (e: Exception) {
            ParsedMetadata(null, null, 0)
        }
    }

    private fun openRenderer(uri: Uri): PdfRenderer? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return PdfRenderer(pfd)
    }
}