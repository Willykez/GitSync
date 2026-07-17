package com.willykez.gitsync.ui.screens.clone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willykez.gitsync.App
import com.willykez.gitsync.data.db.entity.RepoEntity
import com.willykez.gitsync.data.repository.DecryptedCredential
import com.willykez.gitsync.data.repository.DecryptedSshKey
import com.willykez.gitsync.git.GitEngine
import com.willykez.gitsync.git.GitResult
import com.willykez.gitsync.git.GitSshSessionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CloneUiState(
    val url: String = "",
    val branch: String = "",
    val repoName: String = "",
    val credentials: List<DecryptedCredential> = emptyList(),
    val selectedCredentialId: Long? = null,
    val sshKeys: List<DecryptedSshKey> = emptyList(),
    val selectedSshKeyId: Long? = null,
    val isCloning: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false
) {
    /** git@host:owner/repo.git and ssh://... URLs authenticate with an SSH key, not a
     * username+token — the credential picker and SSH key picker are mutually exclusive
     * in the UI depending on which kind of URL is entered. */
    val isSshUrl: Boolean get() = url.trimStart().let { it.startsWith("git@") || it.startsWith("ssh://") }
}

class CloneViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private val repoRepository = app.repoRepository
    private val credentialRepository = app.credentialRepository
    private val sshKeyRepository = app.sshKeyRepository

    private val _uiState = MutableStateFlow(CloneUiState())
    val uiState: StateFlow<CloneUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            credentialRepository.allCredentials.collect { list ->
                _uiState.value = _uiState.value.copy(credentials = list)
            }
        }
        viewModelScope.launch {
            sshKeyRepository.allKeys.collect { list ->
                _uiState.value = _uiState.value.copy(sshKeys = list)
            }
        }
    }

    fun onUrlChanged(url: String) {
        val guessedName = url.trimEnd('/').substringAfterLast('/').removeSuffix(".git")
        _uiState.value = _uiState.value.copy(
            url = url,
            repoName = if (_uiState.value.repoName.isBlank() || _uiState.value.repoName == guessNameFor(_uiState.value.url)) {
                guessedName
            } else {
                _uiState.value.repoName
            }
        )
    }

    private fun guessNameFor(url: String) =
        url.trimEnd('/').substringAfterLast('/').removeSuffix(".git")

    fun onBranchChanged(branch: String) {
        _uiState.value = _uiState.value.copy(branch = branch)
    }

    fun onRepoNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(repoName = name)
    }

    fun onCredentialSelected(id: Long?) {
        _uiState.value = _uiState.value.copy(selectedCredentialId = id)
    }

    fun onSshKeySelected(id: Long?) {
        _uiState.value = _uiState.value.copy(selectedSshKeyId = id)
    }

    fun startClone() {
        val state = _uiState.value
        if (state.url.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Enter a repo URL first.")
            return
        }
        val name = state.repoName.ifBlank { guessNameFor(state.url) }
        val context = getApplication<App>()

        if (!com.willykez.gitsync.data.PublicStorage.hasStorageAccess(context)) {
            _uiState.value = state.copy(
                errorMessage = "Git Sync needs storage access to save repos in a public folder. " +
                    "Grant it from the banner on the repo list, then try again."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloning = true, errorMessage = null)

            val reposDir = com.willykez.gitsync.data.PublicStorage.reposRootDir()
            val destination = File(reposDir, name)

            if (destination.exists()) {
                _uiState.value = _uiState.value.copy(
                    isCloning = false,
                    errorMessage = "A folder named \"$name\" already exists. Pick a different name."
                )
                return@launch
            }

            val credential = if (!state.isSshUrl) state.selectedCredentialId?.let { credentialRepository.getById(it) } else null
            val sshKey = if (state.isSshUrl) state.selectedSshKeyId?.let { sshKeyRepository.getById(it) } else null
            val sshTransport = sshKey?.let { GitSshSessionFactory.transportConfigCallbackFor(context, it) }

            when (val result = GitEngine.cloneRepo(
                url = state.url,
                localPath = destination.absolutePath,
                branch = state.branch,
                credential = credential,
                sshTransport = sshTransport,
            )) {
                is GitResult.Success -> {
                    result.data.close()
                    repoRepository.addRepo(
                        RepoEntity(
                            name = name,
                            fullSavePath = destination.absolutePath,
                            cloneUrl = state.url,
                            branch = state.branch,
                            credentialId = credential?.id ?: 0L,
                            sshKeyId = sshKey?.id ?: 0L,
                        )
                    )
                    _uiState.value = _uiState.value.copy(isCloning = false, done = true)
                }
                is GitResult.Error -> {
                    destination.deleteRecursively()
                    _uiState.value = _uiState.value.copy(
                        isCloning = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** Clears the form and the one-shot `done` flag after a successful clone.
     *  Called once the sheet has told the caller it's done — without this, since
     *  this ViewModel is scoped to the screen behind the sheet (not to the sheet's
     *  own visibility), the next time the sheet opens it would immediately see
     *  the previous `done = true` and close itself before appearing. */
    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            url = "",
            branch = "",
            repoName = "",
            selectedCredentialId = null,
            selectedSshKeyId = null,
            isCloning = false,
            errorMessage = null,
            done = false,
        )
    }
}
