package com.sweetcode.lumi.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun clearCovers() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "covers").deleteRecursively()
    }

    suspend fun clearExtractedPages() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "pages").deleteRecursively()
        File(context.cacheDir, "epubs").deleteRecursively()
        File(context.cacheDir, "pdf_pages").deleteRecursively()
    }

    suspend fun cacheSize(): Long = withContext(Dispatchers.IO) {
        val dirs = listOf("covers", "pages", "epubs", "pdf_pages").map { File(context.cacheDir, it) }
        dirs.sumOf { dir -> if (dir.exists()) dir.walk().filter { it.isFile }.sumOf { it.length() } else 0L }
    }
}