package com.willykez.gitsync.ui.screens.sshkeys

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willykez.gitsync.App
import com.willykez.gitsync.data.repository.DecryptedSshKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SshKeysUiState(
    val keys: List<DecryptedSshKey> = emptyList(),
    val showGenerator: Boolean = false,
    val showImporter: Boolean = false,
    val message: String? = null,
    val lastGeneratedPublicKey: String? = null, // shown once right after generating, so the user can copy it
)

class SshKeysViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as App).sshKeyRepository

    private val showGenerator = MutableStateFlow(false)
    private val showImporter = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val lastGenerated = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SshKeysUiState> = combine(
        repo.allKeys, showGenerator, showImporter, message, lastGenerated,
    ) { keys, gen, imp, msg, lastGen ->
        SshKeysUiState(keys, gen, imp, msg, lastGen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SshKeysUiState())

    fun openGenerator() { showGenerator.value = true }
    fun openImporter() { showImporter.value = true }
    fun dismissSheets() { showGenerator.value = false; showImporter.value = false }
    fun dismissMessage() { message.value = null }

    fun generate(name: String, comment: String, passphrase: String) {
        viewModelScope.launch {
            val id = repo.generateKey(name.ifBlank { "SSH Key" }, comment, passphrase)
            val key = repo.getById(id)
            lastGenerated.value = key?.publicKeyLine
            showGenerator.value = false
            message.value = "Key generated — copy the public key below into GitHub/GitLab"
        }
    }

    fun import(name: String, privateKeyPem: String, publicKeyLine: String, passphrase: String) {
        if (privateKeyPem.isBlank() || publicKeyLine.isBlank()) {
            message.value = "Both the private and public key are required"
            return
        }
        viewModelScope.launch {
            repo.importKey(name.ifBlank { "Imported Key" }, privateKeyPem, publicKeyLine, passphrase)
            showImporter.value = false
            message.value = "Key imported"
        }
    }

    fun delete(key: DecryptedSshKey) {
        viewModelScope.launch { repo.deleteKey(key) }
    }

    fun dismissGeneratedKeyBanner() { lastGenerated.value = null }
}
