package com.sweetcode.lumi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sweetcode.lumi.data.model.ThemeMode
import com.sweetcode.lumi.ui.MainViewModel
import com.sweetcode.lumi.ui.StartState
import com.sweetcode.lumi.ui.navigation.LumiDestination
import com.sweetcode.lumi.ui.navigation.LumiNavHost
import com.sweetcode.lumi.ui.theme.LumiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by viewModel.themeState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeState.mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            LumiTheme(
                darkTheme = darkTheme,
                dynamicColor = themeState.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.startState.collectAsState()
                    when (state) {
                        StartState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        StartState.NeedsOnboarding -> LumiNavHost(
                            startDestination = LumiDestination.Onboarding.route
                        )
                        StartState.Ready -> LumiNavHost(
                            startDestination = LumiDestination.Library.route
                        )
                    }
                }
            }
        }
    }
}