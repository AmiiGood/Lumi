package com.sweetcode.lumi.data.parser

import io.github.amiigood.lumi.lumi.data.parser.ComicInfoParser
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class ComicInfoParserTest {

    @Test
    fun `parsea Series Number y Volume`() {
        val xml = """
            <ComicInfo>
                <Series>Berserk</Series>
                <Number>5</Number>
                <Volume>1990</Volume>
            </ComicInfo>
        """.trimIndent()

        val result = ComicInfoParser.parse(xml)
        assertEquals("Berserk", result.series)
        assertEquals("5", result.number)
        assertEquals("1990", result.volume)
    }

    @Test
    fun `parsea Year como Int`() {
        val xml = "<Year>2023</Year>"
        assertEquals(2023, ComicInfoParser.parse(xml).year)
    }

    @Test
    fun `Year invalido retorna null`() {
        val xml = "<Year>no-es-numero</Year>"
        assertNull(ComicInfoParser.parse(xml).year)
    }

    @Test
    fun `parsea generos separados por coma`() {
        val xml = "<Genre>Action, Drama, Fantasy</Genre>"
        val result = ComicInfoParser.parse(xml)
        assertEquals(listOf("Action", "Drama", "Fantasy"), result.genres)
    }

    @Test
    fun `genero vacio retorna lista vacia`() {
        val xml = "<ComicInfo></ComicInfo>"
        assertEquals(emptyList<String>(), ComicInfoParser.parse(xml).genres)
    }

    @Test
    fun `prefiere Penciller sobre Inker para artist`() {
        val xml = """
            <ComicInfo>
                <Penciller>Artista A</Penciller>
                <Inker>Artista B</Inker>
            </ComicInfo>
        """.trimIndent()
        assertEquals("Artista A", ComicInfoParser.parse(xml).artist)
    }

    @Test
    fun `usa Inker si no hay Penciller`() {
        val xml = "<Inker>Artista B</Inker>"
        assertEquals("Artista B", ComicInfoParser.parse(xml).artist)
    }

    @Test
    fun `tags vacios retornan null`() {
        val xml = "<Series></Series>"
        assertNull(ComicInfoParser.parse(xml).series)
    }

    @Test
    fun `xml sin tags conocidos retorna metadata vacia`() {
        val xml = "<Otro>valor</Otro>"
        val result = ComicInfoParser.parse(xml)
        assertNull(result.series)
        assertNull(result.year)
        assertEquals(emptyList<String>(), result.genres)
    }

    @Test
    fun `case insensitive en tags`() {
        val xml = "<series>Naruto</series>"
        assertEquals("Naruto", ComicInfoParser.parse(xml).series)
    }
}