package com.sweetcode.lumi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweetcode.lumi.data.local.LumiPreferences
import com.sweetcode.lumi.data.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class StartState {
    data object Loading : StartState()
    data object NeedsOnboarding : StartState()
    data object Ready : StartState()
}

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: LumiPreferences
) : ViewModel() {

    val startState: StateFlow<StartState> = preferences.onboardingDone
        .map { if (it) StartState.Ready else StartState.NeedsOnboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartState.Loading)

    val themeState: StateFlow<ThemeState> = combine(
        preferences.themeModeEnum,
        preferences.dynamicColor
    ) { mode, dynamic ->
        ThemeState(mode, dynamic)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())
}