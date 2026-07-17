package com.willykez.gitsync.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willykez.gitsync.App
import com.willykez.gitsync.data.repository.DecryptedSigningKey
import com.willykez.gitsync.data.repository.SigningPrefs
import com.willykez.gitsync.sync.SyncPrefs
import com.willykez.gitsync.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val backgroundSyncEnabled: Boolean = false,
    val intervalHours: Long = SyncPrefs.DEFAULT_INTERVAL_HOURS,
    val signingEnabled: Boolean = false,
    val signingKeys: List<DecryptedSigningKey> = emptyList(),
    val activeSigningKeyId: Long = 0L,
    val showKeyImporter: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val signingKeyRepository = (app as App).signingKeyRepository

    private val _state = MutableStateFlow(
        SettingsUiState(
            backgroundSyncEnabled = SyncPrefs.isEnabled(app),
            intervalHours = SyncPrefs.intervalHours(app),
            signingEnabled = SigningPrefs.isEnabled(app),
            activeSigningKeyId = SigningPrefs.activeKeyId(app),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            signingKeyRepository.allKeys.collect { keys ->
                _state.value = _state.value.copy(signingKeys = keys)
            }
        }
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        val app = getApplication<android.app.Application>()
        SyncPrefs.setEnabled(app, enabled)
        SyncScheduler.applyFromPrefs(app)
        _state.value = _state.value.copy(backgroundSyncEnabled = enabled)
    }

    fun setIntervalHours(hours: Long) {
        val app = getApplication<android.app.Application>()
        SyncPrefs.setIntervalHours(app, hours)
        if (_state.value.backgroundSyncEnabled) SyncScheduler.applyFromPrefs(app)
        _state.value = _state.value.copy(intervalHours = hours.coerceAtLeast(SyncPrefs.MIN_INTERVAL_HOURS))
    }

    fun setSigningEnabled(enabled: Boolean) {
        val app = getApplication<android.app.Application>()
        if (enabled && _state.value.activeSigningKeyId == 0L && _state.value.signingKeys.isNotEmpty()) {
            // Picking "enable" with keys available but none chosen yet — default to the first
            // one rather than silently no-op'ing (setSigningKey would be null and JGit would
            // fall back to whatever key is "first" in the keyring anyway, so this just makes
            // that visible in the UI instead of leaving the picker looking unset).
            setActiveKey(_state.value.signingKeys.first().id)
        }
        SigningPrefs.setEnabled(app, enabled)
        _state.value = _state.value.copy(signingEnabled = enabled)
    }

    fun setActiveKey(id: Long) {
        val app = getApplication<android.app.Application>()
        SigningPrefs.setActiveKeyId(app, id)
        _state.value = _state.value.copy(activeSigningKeyId = id)
    }

    fun openKeyImporter() { _state.value = _state.value.copy(showKeyImporter = true) }
    fun dismissKeyImporter() { _state.value = _state.value.copy(showKeyImporter = false) }
    fun dismissMessage() { _state.value = _state.value.copy(message = null) }

    fun importSigningKey(name: String, armoredPrivateKey: String) {
        viewModelScope.launch {
            try {
                signingKeyRepository.importKey(name.ifBlank { "GPG Key" }, armoredPrivateKey)
                _state.value = _state.value.copy(showKeyImporter = false, message = "Key imported")
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = e.message ?: "Couldn't parse that key")
            }
        }
    }

    fun deleteSigningKey(key: DecryptedSigningKey) {
        viewModelScope.launch {
            signingKeyRepository.deleteKey(key)
            if (_state.value.activeSigningKeyId == key.id) setActiveKey(0L)
        }
    }
}
