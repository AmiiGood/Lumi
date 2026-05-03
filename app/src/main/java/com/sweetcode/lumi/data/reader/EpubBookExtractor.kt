package com.sweetcode.lumi.data.reader

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedEpub(
    val rootDir: File,
    val htmlFile: File,
    val title: String?
)

@Singleton
class EpubBookExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val booksRoot: File by lazy {
        File(context.cacheDir, "epubs").apply { mkdirs() }
    }

    suspend fun extract(itemId: String, uri: Uri): ExtractedEpub? = withContext(Dispatchers.IO) {
        val target = File(booksRoot, itemId)
        val htmlFile = File(target, "_lumi_book.html")

        if (htmlFile.exists() && htmlFile.length() > 0) {
            return@withContext ExtractedEpub(target, htmlFile, null)
        }

        target.deleteRecursively()
        target.mkdirs()

        try {
            unzipAll(uri, target)

            val containerXml = File(target, "META-INF/container.xml")
            if (!containerXml.exists()) return@withContext null

            val opfPath = Regex("full-path=\"([^\"]+)\"")
                .find(containerXml.readText())?.groupValues?.get(1) ?: return@withContext null

            val opfFile = File(target, opfPath)
            if (!opfFile.exists()) return@withContext null

            val opfDir = opfFile.parentFile ?: target
            val opf = opfFile.readText()

            val title = Regex("<dc:title[^>]*>([^<]+)</dc:title>")
                .find(opf)?.groupValues?.get(1)?.trim()

            val manifest = parseManifest(opf)
            val spine = parseSpine(opf)

            val merged = StringBuilder()
            merged.append("""<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>""")

            for (idref in spine) {
                val href = manifest[idref] ?: continue
                val chapterFile = File(opfDir, href)
                if (!chapterFile.exists()) continue
                val chapterDir = chapterFile.parentFile ?: opfDir

                val raw = chapterFile.readText()
                val body = extractBody(raw)
                val resolved = resolveResourcePaths(body, chapterDir, target)

                merged.append("""<section class="lumi-chapter">""")
                merged.append(resolved)
                merged.append("""</section>""")
            }

            merged.append("""</body></html>""")
            htmlFile.writeText(merged.toString())

            ExtractedEpub(target, htmlFile, title)
        } catch (e: Exception) {
            target.deleteRecursively()
            null
        }
    }

    private fun unzipAll(uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val safePath = entry.name.replace("..", "")
                    val out = File(target, safePath)
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { fos -> zip.copyTo(fos) }
                    }
                    entry = zip.nextEntry
                }
            }
        }
    }

    private fun parseManifest(opf: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""<item[^>]*\bid="([^"]+)"[^>]*\bhref="([^"]+)"[^>]*>""")
        regex.findAll(opf).forEach {
            map[it.groupValues[1]] = it.groupValues[2]
        }
        // También intentar con el orden invertido (href antes que id)
        val regex2 = Regex("""<item[^>]*\bhref="([^"]+)"[^>]*\bid="([^"]+)"[^>]*>""")
        regex2.findAll(opf).forEach {
            map[it.groupValues[2]] = it.groupValues[1]
        }
        return map
    }

    private fun parseSpine(opf: String): List<String> {
        val spineBlock = Regex("""<spine[^>]*>([\s\S]*?)</spine>""")
            .find(opf)?.groupValues?.get(1) ?: return emptyList()
        return Regex("""<itemref[^>]*\bidref="([^"]+)"""")
            .findAll(spineBlock)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun extractBody(html: String): String {
        val match = Regex("""<body[^>]*>([\s\S]*?)</body>""", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.get(1) ?: html
    }

    private fun resolveResourcePaths(html: String, chapterDir: File, root: File): String {
        // Convierte src="../images/foo.jpg" en src="file:///path/absoluto"
        val srcRegex = Regex("""(src|href)="([^"]+)"""")
        return srcRegex.replace(html) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            if (path.startsWith("http") || path.startsWith("data:") || path.startsWith("#")) {
                match.value
            } else {
                val resolved = File(chapterDir, path).normalize()
                if (resolved.exists() && resolved.absolutePath.startsWith(root.absolutePath)) {
                    """$attr="file://${resolved.absolutePath}""""
                } else {
                    match.value
                }
            }
        }
    }
}