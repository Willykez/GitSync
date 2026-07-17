package com.willykez.gitsync.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved SSH keypair for authenticating with a git remote over `git@host:owner/repo.git`
 * style URLs — the alternative to a [CredentialEntity] (HTTPS + PAT).
 *
 * [privateKeyPem] is encrypted at rest before being written here (same TokenCrypto,
 * Android Keystore-backed, as CredentialEntity's token) — see SshKeyRepository.
 * [publicKeyLine] is stored in plain text since it's not sensitive — it's the thing
 * you paste into GitHub/GitLab's "Add SSH key" screen.
 */
@Entity(tableName = "ssh_keys")
data class SshKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Friendly label, e.g. "GitHub - Willykez (Ed25519)" */
    val name: String,

    /** Encrypted PEM-format private key (see TokenCrypto) */
    val privateKeyPem: String,

    /** OpenSSH "authorized_keys"-style public key line, e.g. "ssh-ed25519 AAAA... comment" */
    val publicKeyLine: String,

    /** "ed25519" or "rsa" — informational, shown in the list. */
    val keyType: String = "ed25519",

    /** Optional passphrase protecting the private key, encrypted the same way. Empty = none. */
    val passphrase: String = "",

    val addedAt: Long = System.currentTimeMillis()
)
