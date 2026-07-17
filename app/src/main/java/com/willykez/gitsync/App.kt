package com.willykez.gitsync

import android.app.Application
import com.willykez.gitsync.data.db.AppDatabase
import com.willykez.gitsync.data.repository.CredentialRepository
import com.willykez.gitsync.data.repository.GpgHome
import com.willykez.gitsync.data.repository.RepoRepository
import com.willykez.gitsync.data.repository.SigningKeyRepository
import com.willykez.gitsync.data.repository.SshKeyRepository
import com.willykez.gitsync.sync.SyncScheduler

/**
 * Application class. Holds simple hand-rolled singletons for the database
 * and repositories — no DI framework needed for an app this small.
 */
class App : Application() {

    lateinit var repoRepository: RepoRepository
        private set

    lateinit var credentialRepository: CredentialRepository
        private set

    lateinit var sshKeyRepository: SshKeyRepository
        private set

    lateinit var signingKeyRepository: SigningKeyRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Must happen before any JGit FS/GpgSigner code runs anywhere in the process — JGit
        // caches the resolved "user home" the first time it's asked, so this has to win the
        // race. See SigningKeyRepository's class doc for why this exists at all.
        GpgHome.ensureUserHomeOverride(this)

        val db = AppDatabase.getDatabase(this)
        repoRepository = RepoRepository(db.repoDao())
        credentialRepository = CredentialRepository(db.credentialDao())
        sshKeyRepository = SshKeyRepository(db.sshKeyDao())
        signingKeyRepository = SigningKeyRepository(db.signingKeyDao(), this)

        // Re-applies whatever the user last set in Settings — WorkManager schedules don't
        // survive a full app data wipe/reinstall, but they do survive normal process death,
        // so this is mainly a safety net plus the thing that (re)schedules after a toggle.
        SyncScheduler.applyFromPrefs(this)
    }
}
