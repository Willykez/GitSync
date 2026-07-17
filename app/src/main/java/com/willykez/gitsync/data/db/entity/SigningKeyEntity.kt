package com.willykez.gitsync.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A GPG private key imported for commit signing. Only one of these is ever "active"
 * at a time (tracked by [com.willykez.gitsync.sync.SyncPrefs]-style prefs in
 * SigningPrefs, not here) — this table just holds the imported key material.
 *
 * [armoredPrivateKey] is the encrypted ASCII-armored `-----BEGIN PGP PRIVATE KEY BLOCK-----`
 * text the user pasted in, encrypted at rest via TokenCrypto like every other secret in
 * this app. [keyId] is the short hex key ID (e.g. "A1B2C3D4E5F6A7B8") parsed out of the
 * key at import time — this is what gets passed to JGit's `setSigningKey()`.
 */
@Entity(tableName = "signing_keys")
data class SigningKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Friendly label, e.g. "Personal GPG key" */
    val name: String,

    /** Encrypted ASCII-armored private key block */
    val armoredPrivateKey: String,

    /** Short hex GPG key ID, parsed at import time. Empty if parsing failed. */
    val keyId: String,

    /** User ID string from the key (name/email), if parseable — shown in the list. */
    val userId: String = "",

    val addedAt: Long = System.currentTimeMillis()
)
