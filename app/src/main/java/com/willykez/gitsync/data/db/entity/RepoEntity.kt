package com.willykez.gitsync.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for one tracked git repository. The actual repo (working files,
 * .git folder, history) lives on disk at [fullSavePath] — JGit manages that
 * content directly. This row is only what the app needs to list and
 * operate on the repo.
 */
@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name shown in the repo list — usually the folder name. */
    val name: String,

    /** Absolute path on device storage, e.g. .../files/repos/my-repo */
    val fullSavePath: String,

    /** The URL this repo was cloned from, e.g. https://github.com/you/repo.git */
    val cloneUrl: String,

    /** Branch checked out. Empty until first successful clone/fetch resolves it. */
    val branch: String = "",

    /** FK -> CredentialEntity.id. 0 = no HTTPS credential attached (public repo, or SSH used instead). */
    val credentialId: Long = 0,

    /** FK -> SshKeyEntity.id. 0 = no SSH key attached. A repo uses at most one auth method —
     * if both this and [credentialId] are non-zero (shouldn't normally happen via the UI),
     * GitEngine prefers the SSH key since an ssh:// / git@ URL can't authenticate over HTTPS anyway. */
    val sshKeyId: Long = 0,

    /** Epoch millis of the last successful pull or push. 0 = never synced. */
    val lastSyncTime: Long = 0L,

    /** Message from the most recent failed operation. Empty = no error. */
    val lastError: String = "",

    /** Epoch millis when this repo was added to the app, for list ordering. */
    val addedAt: Long = System.currentTimeMillis()
)
