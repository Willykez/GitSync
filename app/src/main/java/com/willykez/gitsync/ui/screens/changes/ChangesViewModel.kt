package com.willykez.gitsync.ui.screens.changes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willykez.gitsync.App
import com.willykez.gitsync.data.db.entity.RepoEntity
import com.willykez.gitsync.data.repository.SigningPrefs
import com.willykez.gitsync.git.GitEngine
import com.willykez.gitsync.git.GitFileEntry
import com.willykez.gitsync.git.GitResult
import com.willykez.gitsync.git.GitSshSessionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback

data class ChangesUiState(
    val repo: RepoEntity? = null,
    val currentBranch: String = "",
    val staged: List<GitFileEntry> = emptyList(),
    val unstaged: List<GitFileEntry> = emptyList(),
    val commitMessage: String = "",
    val ahead: Int = 0,
    val behind: Int = 0,
    val statusLine: String = "",
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val message: String? = null,
    val hasConflicts: Boolean = false,
)

class ChangesViewModel(app: Application) : AndroidViewModel(app) {
    private val appRef = app as App
    private val repoRepo = appRef.repoRepository
    private val credRepo = appRef.credentialRepository

    private val _state = MutableStateFlow(ChangesUiState())
    val uiState: StateFlow<ChangesUiState> = _state.asStateFlow()

    private var git: Git? = null
    private var repoId: Long = -1

    fun load(id: Long) {
        repoId = id
        viewModelScope.launch {
            val repo = repoRepo.getById(id) ?: return@launch
            _state.value = _state.value.copy(repo = repo, isLoading = true)
            openAndRefresh(repo)
        }
    }

    fun refresh() { viewModelScope.launch { _state.value.repo?.let { openAndRefresh(it) } } }

    private suspend fun openAndRefresh(repo: RepoEntity) {
        val result = GitEngine.openRepo(repo.fullSavePath)
        if (result is GitResult.Error) { err("Open failed: ${result.message}"); return }
        val g = (result as GitResult.Success).data
        git = g

        val branch = GitEngine.getCurrentBranch(g).getOrNull() ?: ""
        val ab = GitEngine.getAheadBehind(g).getOrNull() ?: Pair(0, 0)
        val statusLine = buildStatusLine(g)

        when (val sr = GitEngine.getStatus(g)) {
            is GitResult.Error -> err("Status failed: ${sr.message}")
            is GitResult.Success -> {
                val (stg, unstg) = sr.data.partition { it.staged }
                _state.value = _state.value.copy(
                    staged = stg, unstaged = unstg, isLoading = false,
                    currentBranch = branch, ahead = ab.first, behind = ab.second,
                    statusLine = statusLine,
                    hasConflicts = unstg.any { it.status == com.willykez.gitsync.git.GitFileStatus.CONFLICTED },
                )
            }
        }
    }

    private suspend fun buildStatusLine(g: Git): String {
        val head = GitEngine.getHeadCommit(g).getOrNull()
        return if (head != null) "${head.shortSha} ${head.message}" else "No commits yet"
    }

    fun onCommitMessageChanged(v: String) { _state.value = _state.value.copy(commitMessage = v) }

    fun stageFile(e: GitFileEntry) = gitOp { g -> GitEngine.stageFile(g, e.path) }
    fun unstageFile(e: GitFileEntry) = gitOp { g -> GitEngine.unstageFile(g, e.path) }
    fun stageAll() = gitOp { g -> GitEngine.stageAll(g) }
    fun unstageAll() = gitOp { g ->
        _state.value.staged.forEach { GitEngine.unstageFile(g, it.path) }
        GitResult.Success(Unit)
    }
    fun discardFile(e: GitFileEntry) = gitOp { g -> GitEngine.discardFile(g, e.path) }
    fun discardAll() = gitOp { g -> GitEngine.discardAllChanges(g) }

    fun stageAllAndCommit() {
        val msg = _state.value.commitMessage
        if (msg.isBlank()) { err("Write a commit message first"); return }
        gitOp { g ->
            val r1 = GitEngine.stageAll(g)
            if (r1 is GitResult.Error) return@gitOp r1
            val (sign, keyId) = resolveSigning()
            GitEngine.commit(g, msg, authorName(), "gitsync@local", sign = sign, signingKeyId = keyId)
        }
    }

    fun commit() {
        val msg = _state.value.commitMessage
        if (msg.isBlank()) { err("Write a commit message first"); return }
        if (_state.value.staged.isEmpty()) { err("Stage at least one file first"); return }
        gitOp { g ->
            val (sign, keyId) = resolveSigning()
            val r = GitEngine.commit(g, msg, authorName(), "gitsync@local", sign = sign, signingKeyId = keyId)
            if (r is GitResult.Success) _state.value = _state.value.copy(commitMessage = "")
            r
        }
    }

    fun amendCommit(newMessage: String) {
        if (newMessage.isBlank()) { err("Message required"); return }
        gitOp { g ->
            val (sign, keyId) = resolveSigning()
            GitEngine.commit(g, newMessage, authorName(), "gitsync@local", amend = true, sign = sign, signingKeyId = keyId)
        }
    }

    /** Installs the active signing key (if commit signing is turned on in Settings) into
     * the relocated GnuPG home so JGit's signer can find it, and returns whether to sign
     * plus the hex key ID to pass to CommitCommand.setSigningKey(). */
    private suspend fun resolveSigning(): Pair<Boolean, String?> {
        val context = getApplication<App>()
        if (!SigningPrefs.isEnabled(context)) return false to null
        val activeId = SigningPrefs.activeKeyId(context)
        if (activeId == 0L) return false to null
        val keyId = appRef.signingKeyRepository.installActiveKey(activeId)
        return true to keyId
    }

    fun squash(n: Int, message: String) {
        if (message.isBlank()) { err("Write a commit message first"); return }
        gitOp { g -> GitEngine.squashLastN(g, n, message, authorName(), "gitsync@local") }
    }

    fun cherryPick(sha: String) {
        if (sha.isBlank()) { err("Enter a commit SHA"); return }
        gitOp { g -> GitEngine.cherryPick(g, sha) }
    }

    fun fetch() = remoteOp { g, cred, ssh -> GitEngine.fetch(g, credential = cred, sshTransport = ssh) }
    fun pullMerge() = remoteOp { g, cred, ssh -> GitEngine.pullMerge(g, cred, ssh) }
    fun pullRebase() = remoteOp { g, cred, ssh -> GitEngine.pullRebase(g, cred, ssh) }
    fun pullForce() = remoteOp { g, cred, ssh -> GitEngine.pullForce(g, cred, ssh) }
    fun push() = remoteOp { g, cred, ssh -> GitEngine.push(g, credential = cred, sshTransport = ssh) }
    fun pushForce() = remoteOp { g, cred, ssh -> GitEngine.push(g, force = true, credential = cred, sshTransport = ssh) }
    fun syncMerge() = remoteOp { g, cred, ssh -> GitEngine.syncMerge(g, cred, ssh) }
    fun syncRebase() = remoteOp { g, cred, ssh -> GitEngine.syncRebase(g, cred, ssh) }

    fun resetSoft() = gitOp { g -> GitEngine.resetSoft(g) }
    fun resetMixed() = gitOp { g -> GitEngine.resetMixed(g) }
    fun resetHard() = gitOp { g -> GitEngine.resetHard(g) }

    fun dismissMessage() { _state.value = _state.value.copy(message = null) }

    /** For snackbar messages triggered purely from the UI layer (e.g. "Branch name copied"
     * after a clipboard action) that don't come from a git operation result. */
    fun showTransientMessage(msg: String) { _state.value = _state.value.copy(message = msg) }

    private fun authorName(): String {
        return _state.value.repo?.let { r ->
            if (r.credentialId != 0L) null else null
        } ?: "Git Sync"
    }

    private fun gitOp(block: suspend (Git) -> GitResult<*>) {
        val g = git ?: run { err("Repo not open"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true)
            when (val r = block(g)) {
                is GitResult.Error -> err(r.message)
                is GitResult.Success -> {
                    val msg = when (val d = r.data) {
                        is String -> d
                        is Unit -> "Done"
                        else -> "Done"
                    }
                    ok(msg)
                    _state.value.repo?.let { openAndRefresh(it) }
                }
            }
            _state.value = _state.value.copy(isWorking = false)
        }
    }

    private fun remoteOp(
        block: suspend (Git, com.willykez.gitsync.data.repository.DecryptedCredential?, TransportConfigCallback?) -> GitResult<*>,
    ) {
        val repo = _state.value.repo ?: run { err("Repo not loaded"); return }
        val g = git ?: run { err("Repo not open"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true)
            val cred = if (repo.sshKeyId == 0L && repo.credentialId != 0L) credRepo.getById(repo.credentialId) else null
            val sshTransport = if (repo.sshKeyId != 0L) {
                appRef.sshKeyRepository.getById(repo.sshKeyId)?.let {
                    GitSshSessionFactory.transportConfigCallbackFor(getApplication(), it)
                }
            } else null
            when (val r = block(g, cred, sshTransport)) {
                is GitResult.Error -> {
                    repoRepo.markError(repo.id, r.message)
                    err(r.message)
                }
                is GitResult.Success -> {
                    repoRepo.markSyncSuccess(repo.id)
                    val msg = when (val d = r.data) { is String -> d; else -> "Done" }
                    ok(msg)
                    openAndRefresh(repo)
                }
            }
            _state.value = _state.value.copy(isWorking = false)
        }
    }

    private fun err(msg: String) { _state.value = _state.value.copy(message = msg, isWorking = false) }
    private fun ok(msg: String) { _state.value = _state.value.copy(message = msg) }

    // squash helper exposed from GitEngine
    private suspend fun GitEngine.squashLastN(git: Git, n: Int, msg: String, name: String, email: String): GitResult<String> {
        val reset = resetSoft(git, "HEAD~$n")
        if (reset is GitResult.Error) return GitResult.Error(reset.message)
        return commit(git, msg, name, email)
    }

    override fun onCleared() { super.onCleared(); git?.close() }
}
