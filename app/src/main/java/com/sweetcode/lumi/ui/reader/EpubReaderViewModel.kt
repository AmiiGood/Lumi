package com.sweetcode.lumi.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweetcode.lumi.data.local.LumiPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpubSettings(
    val fontSize: Int = 18,
    val darkMode: Boolean = false
)

@HiltViewModel
class EpubReaderViewModel @Inject constructor(
    private val preferences: LumiPreferences
) : ViewModel() {

    private val _settings = MutableStateFlow(EpubSettings())
    val settings: StateFlow<EpubSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            val size = preferences.epubFontSize.firstOrNull() ?: 18
            val dark = preferences.epubDarkMode.firstOrNull() ?: false
            _settings.value = EpubSettings(size, dark)
        }
    }

    fun setFontSize(size: Int) {
        val clamped = size.coerceIn(12, 32)
        _settings.value = _settings.value.copy(fontSize = clamped)
        viewModelScope.launch { preferences.setEpubFontSize(clamped) }
    }

    fun toggleDarkMode() {
        val newValue = !_settings.value.darkMode
        _settings.value = _settings.value.copy(darkMode = newValue)
        viewModelScope.launch { preferences.setEpubDarkMode(newValue) }
    }
}