package com.sweetcode.lumi.data.parser

import com.sweetcode.lumi.data.model.ItemMetadata

object ComicInfoParser {

    fun parse(xml: String): ItemMetadata {
        return ItemMetadata(
            series = tag(xml, "Series"),
            number = tag(xml, "Number"),
            volume = tag(xml, "Volume"),
            summary = tag(xml, "Summary"),
            publisher = tag(xml, "Publisher"),
            year = tag(xml, "Year")?.toIntOrNull(),
            genres = tag(xml, "Genre")?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            language = tag(xml, "LanguageISO"),
            writer = tag(xml, "Writer"),
            artist = tag(xml, "Penciller") ?: tag(xml, "Inker") ?: tag(xml, "CoverArtist")
        )
    }

    private fun tag(xml: String, name: String): String? {
        val regex = Regex("<$name>([^<]*)</$name>", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}