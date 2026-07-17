package com.willykez.gitsync.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.willykez.gitsync.data.db.dao.CredentialDao
import com.willykez.gitsync.data.db.dao.RepoDao
import com.willykez.gitsync.data.db.dao.SigningKeyDao
import com.willykez.gitsync.data.db.dao.SshKeyDao
import com.willykez.gitsync.data.db.entity.CredentialEntity
import com.willykez.gitsync.data.db.entity.RepoEntity
import com.willykez.gitsync.data.db.entity.SigningKeyEntity
import com.willykez.gitsync.data.db.entity.SshKeyEntity

@Database(
    entities = [
        RepoEntity::class, CredentialEntity::class,
        SshKeyEntity::class, SigningKeyEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun repoDao(): RepoDao
    abstract fun credentialDao(): CredentialDao
    abstract fun sshKeyDao(): SshKeyDao
    abstract fun signingKeyDao(): SigningKeyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gitsync.db"
                )
                    // Fine while the schema is still settling during early development.
                    // Once you ship a version people rely on, replace this with real
                    // Room migrations so upgrading the app doesn't wipe repo history.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
