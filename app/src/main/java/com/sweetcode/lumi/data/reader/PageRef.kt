package com.sweetcode.lumi.data.reader

import java.io.File

sealed interface PageRef {
    val key: String

    data class Direct(val file: File) : PageRef {
        override val key: String = file.absolutePath
    }

    data class LazyPdf(
        val itemId: String,
        val index: Int,
        val source: PdfPageSource
    ) : PageRef {
        override val key: String = "pdf_${itemId}_$index"
        suspend fun resolve(): File? = source.getPage(index)
    }
}