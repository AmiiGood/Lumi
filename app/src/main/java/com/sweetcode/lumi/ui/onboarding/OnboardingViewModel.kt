package com.sweetcode.lumi.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweetcode.lumi.data.local.LumiPreferences
import com.sweetcode.lumi.data.local.MediaItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: LumiPreferences,
    private val dao: MediaItemDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    fun onFolderSelected(uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            dao.deleteAll()
            preferences.setRootFolderUri(uri.toString())
            preferences.setOnboardingDone(true)
            onComplete()
        }
    }
}