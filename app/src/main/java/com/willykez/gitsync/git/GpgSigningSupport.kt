package com.willykez.gitsync.git

import org.eclipse.jgit.gpg.bc.BouncyCastleGpgSigner
import org.eclipse.jgit.lib.GpgSigner

/**
 * Registers [BouncyCastleGpgSigner] as JGit's process-wide default signer the first time a
 * signed commit is attempted. GpgSigner.setDefault() is a static, so this only needs to
 * happen once — GitEngine.commit() calls [ensureRegistered] before every signed commit, which
 * is cheap after the first call.
 *
 * The signer itself finds key material via BouncyCastleGpgKeyLocator, which searches a
 * "GnuPG home" directory on disk — see SigningKeyRepository/GpgHome for how that's relocated
 * into app-private storage on Android instead of a real `~/.gnupg`.
 */
object GpgSigningSupport {
    private var registered = false

    fun ensureRegistered() {
        if (registered) return
        GpgSigner.setDefault(BouncyCastleGpgSigner())
        registered = true
    }
}
