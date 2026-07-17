package com.willykez.gitsync.data.repository

import android.content.Context
import com.willykez.gitsync.data.db.dao.SigningKeyDao
import com.willykez.gitsync.data.db.entity.SigningKeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import java.io.ByteArrayInputStream
import java.io.File

data class DecryptedSigningKey(
    val id: Long,
    val name: String,
    val keyId: String,
    val userId: String,
)

/**
 * Handles GPG commit-signing keys. Two responsibilities that are easy to conflate but worth
 * keeping distinct:
 *
 *  1. **Storage** — the user's pasted armored private key is encrypted with TokenCrypto and
 *     kept in Room, same as every other secret in this app (HTTPS PATs, SSH private keys).
 *
 *  2. **Installation** — JGit's [org.eclipse.jgit.gpg.bc.internal.BouncyCastleGpgSigner]
 *     (via `BouncyCastleGpgKeyLocator`) doesn't take a key as a parameter — it searches a
 *     "GnuPG home directory" on disk (normally `~/.gnupg`) for `pubring.gpg`/`secring.gpg`
 *     files, the same way the real `gpg` CLI would. There's no meaningful `$HOME` on Android,
 *     so [installActiveKey] relocates that search by pointing the `user.home` system property
 *     at app-private storage (see [GNUPG_HOME_OVERRIDE]) and writes the key material there in
 *     the legacy keyring format the locator falls back to. This has to happen before JGit's
 *     `FS` class caches a user-home value, which is why `App.onCreate()` sets the system
 *     property immediately on startup rather than waiting for the first signed commit.
 *
 * This is the most technically uncertain of the app's new capabilities — it depends on JGit's
 * internal key-lookup fallback behavior rather than a stable public API, so treat it as
 * best-effort and verify signing actually works on a real device before relying on it.
 */
class SigningKeyRepository(private val dao: SigningKeyDao, private val appContext: Context) {

    val allKeys: Flow<List<DecryptedSigningKey>> =
        dao.getAllFlow().map { list -> list.map { it.toDecrypted() } }

    suspend fun getAll(): List<DecryptedSigningKey> = dao.getAll().map { it.toDecrypted() }

    /** Parses [armoredPrivateKey], stores it encrypted, and returns the new row's ID.
     * Throws with a user-readable message if the pasted text isn't a valid PGP private key. */
    suspend fun importKey(name: String, armoredPrivateKey: String): Long = withContext(Dispatchers.Default) {
        val (keyId, userId) = parseKeyIdAndUserId(armoredPrivateKey)
        val entity = SigningKeyEntity(
            name = name,
            armoredPrivateKey = TokenCrypto.encrypt(armoredPrivateKey.trim()),
            keyId = keyId,
            userId = userId,
        )
        dao.insert(entity)
    }

    suspend fun deleteKey(key: DecryptedSigningKey) {
        dao.delete(SigningKeyEntity(id = key.id, name = key.name, armoredPrivateKey = "", keyId = key.keyId, userId = key.userId))
    }

    /** Writes [keyId]'s key material into the relocated GnuPG home so JGit's signer can find
     * it, and returns the hex key ID to pass to `CommitCommand.setSigningKey()`. Call this
     * right before signing (SigningPrefs tracks which key is "active"; GitEngine.commit()
     * doesn't know about Room, so the ViewModel calls this first and passes the result down). */
    suspend fun installActiveKey(keyId: Long): String? = withContext(Dispatchers.IO) {
        val entity = dao.getById(keyId) ?: return@withContext null
        val armored = try { TokenCrypto.decrypt(entity.armoredPrivateKey) } catch (e: Exception) { return@withContext null }

        GpgHome.ensureUserHomeOverride(appContext)
        val gnupgDir = File(GpgHome.overrideHomeDir(appContext), ".gnupg").apply { mkdirs() }

        val collection = PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(ByteArrayInputStream(armored.toByteArray(Charsets.UTF_8))),
            JcaKeyFingerprintCalculator(),
        )
        val secringOut = java.io.ByteArrayOutputStream()
        val pubringOut = java.io.ByteArrayOutputStream()
        for (ring in collection.keyRings) {
            secringOut.write(ring.encoded)
            val publicKeys = ring.publicKeys.asSequence().toList()
            if (publicKeys.isNotEmpty()) {
                pubringOut.write(PGPPublicKeyRing(publicKeys).encoded)
            }
        }
        // Overwrite rather than append — this app only ever has one "active" signing key
        // installed at a time, selected via SigningPrefs.
        File(gnupgDir, "secring.gpg").writeBytes(secringOut.toByteArray())
        File(gnupgDir, "pubring.gpg").writeBytes(pubringOut.toByteArray())

        entity.keyId.ifBlank { null }
    }

    private fun SigningKeyEntity.toDecrypted() = DecryptedSigningKey(id, name, keyId, userId)

    private fun parseKeyIdAndUserId(armored: String): Pair<String, String> {
        val collection = try {
            PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(ByteArrayInputStream(armored.toByteArray(Charsets.UTF_8))),
                JcaKeyFingerprintCalculator(),
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("That doesn't look like a valid PGP private key block: ${e.message}")
        }
        val ring = collection.keyRings.asSequence().firstOrNull()
            ?: throw IllegalArgumentException("No private key found in the pasted text")
        val master = ring.secretKey
        val keyId = java.lang.Long.toHexString(master.keyID).uppercase()
        val userId = master.publicKey.userIDs.asSequence().firstOrNull() ?: ""
        return keyId to userId
    }
}

/** Where JGit's BouncyCastleGpgSigner looks for `~/.gnupg` once we've overridden `user.home`. */
object GpgHome {
    private const val DIR_NAME = "gnupg-home"
    private var installed = false

    fun overrideHomeDir(context: Context): File = File(context.filesDir, DIR_NAME)

    /** Idempotent — safe to call from GitEngine before every signed commit, but the real
     * guarantee only holds if it's also called once very early (App.onCreate), before JGit's
     * FS class has a chance to resolve/cache a user-home value from the *original* system
     * property. See SigningKeyRepository's class doc for the full explanation. */
    fun ensureUserHomeOverride(context: Context) {
        if (installed) return
        val dir = overrideHomeDir(context).apply { mkdirs() }
        System.setProperty("user.home", dir.absolutePath)
        installed = true
    }
}
