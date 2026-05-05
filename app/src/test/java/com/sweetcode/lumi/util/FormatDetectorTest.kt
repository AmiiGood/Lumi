package com.sweetcode.lumi.util

import com.sweetcode.lumi.data.model.MediaFormat
import com.sweetcode.lumi.data.model.MediaType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class FormatDetectorTest {

    @Test
    fun `detecta CBZ por extension`() {
        assertEquals(MediaFormat.CBZ, FormatDetector.detectFormat("manga.cbz"))
        assertEquals(MediaFormat.CBZ, FormatDetector.detectFormat("MANGA.CBZ"))
    }

    @Test
    fun `detecta CBR por extension`() {
        assertEquals(MediaFormat.CBR, FormatDetector.detectFormat("comic.cbr"))
    }

    @Test
    fun `detecta EPUB por extension`() {
        assertEquals(MediaFormat.EPUB, FormatDetector.detectFormat("libro.epub"))
    }

    @Test
    fun `detecta PDF por extension`() {
        assertEquals(MediaFormat.PDF, FormatDetector.detectFormat("doc.pdf"))
    }

    @Test
    fun `formato desconocido retorna null`() {
        assertNull(FormatDetector.detectFormat("archivo.txt"))
        assertNull(FormatDetector.detectFormat("imagen.jpg"))
        assertNull(FormatDetector.detectFormat("sinExtension"))
    }

    @Test
    fun `infiere MANGA por carpeta padre`() {
        val type = FormatDetector.inferTypeFromPath(listOf("Manga", "Berserk"), MediaFormat.CBZ)
        assertEquals(MediaType.MANGA, type)
    }

    @Test
    fun `infiere COMIC por carpeta padre`() {
        val type = FormatDetector.inferTypeFromPath(listOf("Comics", "Marvel"), MediaFormat.CBZ)
        assertEquals(MediaType.COMIC, type)
    }

    @Test
    fun `infiere BOOK por carpeta libro o book`() {
        assertEquals(MediaType.BOOK, FormatDetector.inferTypeFromPath(listOf("Libros"), MediaFormat.EPUB))
        assertEquals(MediaType.BOOK, FormatDetector.inferTypeFromPath(listOf("Books"), MediaFormat.EPUB))
    }

    @Test
    fun `EPUB sin pista de carpeta es BOOK`() {
        val type = FormatDetector.inferTypeFromPath(listOf("Random"), MediaFormat.EPUB)
        assertEquals(MediaType.BOOK, type)
    }

    @Test
    fun `CBZ sin pista de carpeta es COMIC`() {
        val type = FormatDetector.inferTypeFromPath(emptyList(), MediaFormat.CBZ)
        assertEquals(MediaType.COMIC, type)
    }

    @Test
    fun `inferencia es case insensitive`() {
        val type = FormatDetector.inferTypeFromPath(listOf("MANGA"), MediaFormat.CBZ)
        assertEquals(MediaType.MANGA, type)
    }
}