package io.github.amiigood.lumi.lumi.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.amiigood.lumi.lumi.data.model.ReadingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amiigood.lumi.lumi.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lumi_prefs")

@Singleton
class LumiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootFolderKey = stringPreferencesKey("root_folder_uri")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val rootFolderUri: Flow<String?> = context.dataStore.data.map { it[rootFolderKey] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingDoneKey] ?: false }

    private val readerTutorialDoneKey = booleanPreferencesKey("reader_tutorial_done")

    val readerTutorialDone: Flow<Boolean> = context.dataStore.data.map { it[readerTutorialDoneKey] ?: false }

    private val readingModePrefix = "reading_mode_"

    private val progressPrefix = "progress_"
    private val totalPagesPrefix = "totalpages_"

    private val epubFontSizeKey = intPreferencesKey("epub_font_size")
    private val epubDarkModeKey = booleanPreferencesKey("epub_dark_mode")

    val epubFontSize: Flow<Int> = context.dataStore.data.map { it[epubFontSizeKey] ?: 18 }
    val epubDarkMode: Flow<Boolean> = context.dataStore.data.map { it[epubDarkModeKey] ?: false }

    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[dynamicColorKey] ?: false }

    private val lastReadPrefix = "lastread_"
    suspend fun setRootFolderUri(uri: String) {
        context.dataStore.edit { it[rootFolderKey] = uri }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingDoneKey] = done }
    }

    suspend fun setReaderTutorialDone(done: Boolean) {
        context.dataStore.edit { it[readerTutorialDoneKey] = done }
    }

    fun readingMode(itemId: String): Flow<ReadingMode> = context.dataStore.data.map {
        val raw = it[stringPreferencesKey("$readingModePrefix$itemId")] ?: "PAGED"
        runCatching { ReadingMode.valueOf(raw) }.getOrDefault(ReadingMode.PAGED)
    }

    suspend fun setReadingMode(itemId: String, mode: ReadingMode) {
        context.dataStore.edit { it[stringPreferencesKey("$readingModePrefix$itemId")] = mode.name }
    }

    fun progress(itemId: String): Flow<Int> = context.dataStore.data.map {
        it[intPreferencesKey("$progressPrefix$itemId")] ?: 0
    }

    fun totalPages(itemId: String): Flow<Int> = context.dataStore.data.map {
        it[intPreferencesKey("$totalPagesPrefix$itemId")] ?: 0
    }

    suspend fun setProgress(itemId: String, page: Int, totalPages: Int) {
        context.dataStore.edit {
            it[intPreferencesKey("$progressPrefix$itemId")] = page
            it[intPreferencesKey("$totalPagesPrefix$itemId")] = totalPages
            it[longPreferencesKey("$lastReadPrefix$itemId")] = System.currentTimeMillis()
        }
    }

    suspend fun setEpubFontSize(size: Int) {
        context.dataStore.edit { it[epubFontSizeKey] = size }
    }

    suspend fun setEpubDarkMode(dark: Boolean) {
        context.dataStore.edit { it[epubDarkModeKey] = dark }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = enabled }
    }

    suspend fun setThemeModeEnum(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }

    val themeModeEnum: Flow<ThemeMode> = context.dataStore.data.map {
        val raw = it[themeModeKey] ?: "SYSTEM"
        runCatching { ThemeMode.valueOf(raw) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun lastReadAt(itemId: String): Flow<Long> = context.dataStore.data.map {
        it[longPreferencesKey("$lastReadPrefix$itemId")] ?: 0L
    }
}