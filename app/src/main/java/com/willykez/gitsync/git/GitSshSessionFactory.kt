package com.willykez.gitsync.git

import android.content.Context
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.util.security.SecurityUtils
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.sshd.JGitKeyCache
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.transport.SshTransport
import com.willykez.gitsync.data.repository.DecryptedSshKey
import java.io.ByteArrayInputStream
import java.io.File
import java.net.InetSocketAddress
import java.security.PublicKey
import java.util.Base64

/**
 * Builds a [TransportConfigCallback] that authenticates over SSH using an in-memory keypair
 * decrypted from a [DecryptedSshKey], instead of JGit's normal behavior of reading identity
 * files out of `~/.ssh` (which doesn't meaningfully exist as a concept on Android).
 *
 * Two things JGit's [SshdSessionFactoryBuilder] normally wants that this app doesn't have:
 *  - A "home"/"ssh" directory. There's no real `$HOME` here, so these point at a small
 *    directory under app-private storage instead — used only for the trusted-host-keys file
 *    below, nothing else is read from or written to it.
 *  - Host key verification. There's no `known_hosts` a user pre-populated by hand, so this
 *    implements trust-on-first-use: the first time you connect to a given host, its key is
 *    recorded; if that key ever changes on a later connection, the connection is rejected
 *    (rather than silently trusting a possibly-MITM'd new key). This is the same trust model
 *    SSH clients use by default the first time you `ssh` into a new host.
 */
object GitSshSessionFactory {

    /** One shared factory per app process — JGit recommends against creating a fresh
     * SshdSessionFactory (and its JGitKeyCache) per-operation. */
    private var cached: Pair<Long, SshdSessionFactory>? = null

    fun transportConfigCallbackFor(context: Context, key: DecryptedSshKey): TransportConfigCallback {
        val factory = factoryFor(context, key)
        return TransportConfigCallback { transport ->
            if (transport is SshTransport) {
                transport.sshSessionFactory = factory
            }
        }
    }

    private fun factoryFor(context: Context, key: DecryptedSshKey): SshdSessionFactory {
        cached?.let { (id, f) -> if (id == key.id) return f }

        val sshHome = File(context.filesDir, "ssh-runtime").apply { mkdirs() }
        val knownHostsFile = File(sshHome, "trusted_host_keys")

        val passphraseProvider = FilePasswordProvider { _, _, _ -> key.passphrase.ifBlank { null } }
        val keyPairs = SecurityUtils.loadKeyPairIdentities(
            null, null, ByteArrayInputStream(key.privateKeyPem.toByteArray(Charsets.UTF_8)),
            passphraseProvider,
        )

        val built = SshdSessionFactoryBuilder()
            .setPreferredAuthentications("publickey")
            .setDefaultKeysProvider { keyPairs }
            .setHomeDirectory(sshHome)
            .setSshDirectory(sshHome)
            .setConfigStoreFactory { _, _, _ -> null }
            .setServerKeyDatabase { _, _ -> TrustOnFirstUseKeyDatabase(knownHostsFile) }
            .build(JGitKeyCache())

        cached = key.id to built
        return built
    }

    /** Minimal file-backed [ServerKeyDatabase]: one "host base64(pubkey-encoding)" line per
     * trusted host. Not a full known_hosts parser (no hashing, no per-key-type namespacing
     * beyond the encoded bytes themselves) — deliberately simple since it only needs to serve
     * this app's own trust decisions, not be interoperable with OpenSSH's known_hosts file. */
    private class TrustOnFirstUseKeyDatabase(private val file: File) : ServerKeyDatabase {

        override fun lookup(
            connectAddress: String,
            remoteAddress: InetSocketAddress,
            config: ServerKeyDatabase.Configuration,
        ): List<PublicKey> = emptyList() // we don't pre-seed trusted keys; accept() below does TOFU verification instead

        override fun accept(
            connectAddress: String,
            remoteAddress: InetSocketAddress,
            serverKey: PublicKey,
            config: ServerKeyDatabase.Configuration,
            provider: CredentialsProvider?,
        ): Boolean {
            val encoded = Base64.getEncoder().encodeToString(serverKey.encoded)
            val entries = readEntries()
            val existing = entries[connectAddress]
            return when {
                existing == null -> {
                    // First time seeing this host — trust it and remember the key.
                    writeEntry(connectAddress, encoded)
                    true
                }
                existing == encoded -> true // matches what we trusted before
                else -> false // key changed since we last trusted it — refuse rather than silently re-trust
            }
        }

        private fun readEntries(): Map<String, String> {
            if (!file.exists()) return emptyMap()
            return file.readLines()
                .mapNotNull { line ->
                    val parts = line.split(" ", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        }

        private fun writeEntry(host: String, encodedKey: String) {
            file.parentFile?.mkdirs()
            file.appendText("$host $encodedKey\n")
        }
    }
}
