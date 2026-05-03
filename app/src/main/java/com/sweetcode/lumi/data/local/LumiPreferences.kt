package com.sweetcode.lumi.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sweetcode.lumi.data.model.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lumi_prefs")

@Singleton
class LumiPreferences @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
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

    private val epubFontSizeKey = androidx.datastore.preferences.core.intPreferencesKey("epub_font_size")
    private val epubDarkModeKey = androidx.datastore.preferences.core.booleanPreferencesKey("epub_dark_mode")

    val epubFontSize: Flow<Int> = context.dataStore.data.map { it[epubFontSizeKey] ?: 18 }
    val epubDarkMode: Flow<Boolean> = context.dataStore.data.map { it[epubDarkModeKey] ?: false }

    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[dynamicColorKey] ?: false }


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
        it[androidx.datastore.preferences.core.intPreferencesKey("$progressPrefix$itemId")] ?: 0
    }

    fun totalPages(itemId: String): Flow<Int> = context.dataStore.data.map {
        it[androidx.datastore.preferences.core.intPreferencesKey("$totalPagesPrefix$itemId")] ?: 0
    }

    suspend fun setProgress(itemId: String, page: Int, totalPages: Int) {
        context.dataStore.edit {
            it[androidx.datastore.preferences.core.intPreferencesKey("$progressPrefix$itemId")] = page
            it[androidx.datastore.preferences.core.intPreferencesKey("$totalPagesPrefix$itemId")] = totalPages
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

    suspend fun setThemeModeEnum(mode: com.sweetcode.lumi.data.model.ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }

    val themeModeEnum: Flow<com.sweetcode.lumi.data.model.ThemeMode> = context.dataStore.data.map {
        val raw = it[themeModeKey] ?: "SYSTEM"
        runCatching { com.sweetcode.lumi.data.model.ThemeMode.valueOf(raw) }
            .getOrDefault(com.sweetcode.lumi.data.model.ThemeMode.SYSTEM)
    }
}